package io.hotdealwave.cli;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hotdealwave.server.ServerApplication;
import io.hotdealwave.verifier.Verifier;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.SpringApplication;

/** The three commands. A person runs init and serve; the AI session runs verify when it finishes extracting. */
public final class Handover {
    private static final String USAGE = """
            사용법:
              java -jar <도구>/build/handover.jar init --repo <경로 또는 주소> --commit <커밋> --workspace <폴더>
              java -jar <도구>/build/handover.jar verify [--workspace <폴더>]
              java -jar <도구>/build/handover.jar serve [--workspace <폴더>] [--port <번호>]
            --workspace를 생략하면 현재 폴더다.""";
    private static final String GITHUB = "^https://github[.]com/[^/]+/[^/]+$";
    private static final ObjectMapper JSON = new ObjectMapper();

    /** A stop the user can act on: printed without a stack trace. */
    static final class Stop extends RuntimeException {
        Stop(String message) { super(message); }
    }

    private Handover() {}

    public static void main(String[] args) throws Exception {
        try {
            run(tool(), args);
        } catch (Stop stop) {
            System.err.println(stop.getMessage());
            System.exit(1);
        }
    }

    /** The tool folder: handover.jar sits in its build folder. */
    private static Path tool() {
        String jar = System.getProperty("java.class.path").split(File.pathSeparator)[0];
        return Path.of(jar).toAbsolutePath().normalize().getParent().getParent();
    }

    static void run(Path tool, String... args) throws Exception {
        if (args.length == 0) throw new Stop(USAGE);
        var options = options(args);
        Path workspace = Path.of(options.getOrDefault("--workspace", "")).toAbsolutePath().normalize();
        switch (args[0]) {
            case "init" -> init(tool, workspace, required(options, "--repo"), required(options, "--commit"), options);
            case "verify" -> verify(workspace);
            case "serve" -> serve(workspace, options.getOrDefault("--port", "8080"));
            default -> throw new Stop(USAGE);
        }
    }

    private static Map<String, String> options(String[] args) {
        var options = new HashMap<String, String>();
        for (int i = 1; i < args.length; i += 2) {
            if (!Set.of("--repo", "--commit", "--workspace", "--port").contains(args[i]) || i + 1 >= args.length)
                throw new Stop(USAGE);
            options.put(args[i], args[i + 1]);
        }
        return options;
    }

    private static String required(Map<String, String> options, String name) {
        if (!options.containsKey(name)) throw new Stop(name + "가 필요하다.\n" + USAGE);
        return options.get(name);
    }

    private static void init(Path tool, Path workspace, String repo, String commit, Map<String, String> options) throws Exception {
        if (!options.containsKey("--workspace")) throw new Stop("--workspace가 필요하다.\n" + USAGE);
        Path source = workspace.resolve("source");
        if (Files.exists(source)) throw new Stop(source + "가 이미 있다. 다른 작업 폴더를 준다.");
        Path local = localDirectory(repo);
        boolean isLocal = local != null;
        String repository = githubUrl(isLocal ? Git.run(local, "remote", "get-url", "origin") : repo);
        Path targetFile = workspace.resolve("target.json");
        JsonNode existing = Files.exists(targetFile) ? JSON.readTree(targetFile.toFile()) : null;
        if (existing != null && !(existing.path("repository").asText().equals(repository)
                && existing.path("commit").asText().startsWith(commit)))
            throw new Stop(targetFile + "의 대상(" + existing.path("repository").asText() + " @"
                    + existing.path("commit").asText() + ")과 준 대상이 다르다.");

        Files.createDirectories(workspace);
        Git.run(workspace, "clone", "--quiet", "--no-checkout", isLocal ? local.toString() : repo, "source");
        String pinned;
        try {
            Git.run(source, "-c", "advice.detachedHead=false", "checkout", "--quiet", "--detach", commit);
            pinned = Git.run(source, "rev-parse", "HEAD");
        } catch (Stop stop) {
            delete(source);
            throw stop;
        }
        String name = repository.substring(repository.lastIndexOf('/') + 1);
        if (existing == null) {
            var target = JSON.createObjectNode().put("schemaVersion", 1).put("name", name)
                    .put("repository", repository).put("commit", pinned);
            write(targetFile, target);
        }
        String toolPath = relative(workspace, tool);
        Files.writeString(workspace.resolve("AGENTS.md"), instructions(name, repository, pinned, toolPath));
        Files.writeString(workspace.resolve("CLAUDE.md"), "@AGENTS.md\n");
        System.out.println("준비됨: " + workspace + "\n"
                + "이 폴더에서 AI 세션(Claude Code 또는 Codex)을 열고 \"추출해 줘\"라고 한다.\n"
                + "Claude Code에서 Skill을 슬래시 명령으로도 쓰려면 도구 폴더(" + tool + ")를 세션에 더한다.");
    }

    /** The checkout folder --repo names, or null for an address (Windows paths cannot hold "https:"). */
    private static Path localDirectory(String repo) {
        try {
            Path path = Path.of(repo).toAbsolutePath().normalize();
            return Files.isDirectory(path) ? path : null;
        } catch (InvalidPathException address) {
            return null;
        }
    }

    private static String githubUrl(String url) {
        String normalized = url.strip()
                .replaceFirst("^git@github[.]com:", "https://github.com/")
                .replaceFirst("^ssh://git@github[.]com/", "https://github.com/")
                .replaceFirst("/+$", "").replaceFirst("[.]git$", "");
        if (!normalized.matches(GITHUB)) throw new Stop("근거 링크를 만들려면 GitHub 저장소여야 한다: " + url);
        return normalized;
    }

    private static String relative(Path from, Path to) {
        try {
            String path = from.relativize(to.toAbsolutePath().normalize()).toString().replace('\\', '/');
            return path.isEmpty() ? "." : path;
        } catch (IllegalArgumentException differentDrive) {
            return to.toAbsolutePath().normalize().toString().replace('\\', '/');
        }
    }

    private static String instructions(String name, String repository, String commit, String tool) {
        return """
                # %1$s 분석 작업 폴더

                `source/`는 %2$s의 고정 커밋 `%3$s`이다. 읽기만 하고 고치지 않는다. 결과는 모두 이 폴더에 쓴다. `.env`, `.env.*`는 열지 않는다.

                ## 순서

                어디까지 했는지는 이 폴더의 파일을 보고 정한다.

                1. 서비스: `%4$s/.claude/skills/extract-msa-java-spring/SKILL.md`를 따라 대상 `source`, 결과 디렉터리 `.`로 `services/`에 서비스 파일을 쓴다. 서비스끼리는 독립이라 세션을 나눠 동시에 돌려도 된다.
                2. 관계: 모든 서비스 파일이 있으면 같은 Skill의 관계 단계로 `relations.json`을 쓴다. 서비스를 하나라도 다시 추출했으면 다시 쓴다.
                3. 저장소 수준: 같은 Skill의 저장소 수준 단계로 `repository.json`을 쓴다.
                4. 응답 확인(테스트 서버 주소와 테스트 계정을 받았을 때만): `%4$s/.claude/skills/probe-msa-java-spring/SKILL.md`를 따라 `responses/`에 응답을 저장한다.
                5. 검증: `java -jar %4$s/build/handover.jar verify`를 부른다. `schema` 경고는 그 파일의 형식 오류라 고치고 다시 부른다. 다른 경고는 결과에 "확인 안 됨"과 이유로 나오므로 그대로 둔다.
                6. 업무 규칙·함정: `%4$s/.claude/skills/rules-msa-java-spring/SKILL.md`를 따라 `knowledge/`에 쓰고 5를 다시 한다.

                결과는 사람이 `java -jar %4$s/build/handover.jar serve`로 띄워 본다.
                """.formatted(name, repository, commit, tool);
    }

    private static void verify(Path workspace) throws Exception {
        Path targetFile = workspace.resolve("target.json");
        if (!Files.exists(targetFile)) throw new Stop(targetFile + "가 없다. 먼저 init으로 작업 폴더를 만든다.");
        Path source = workspace.resolve("source");
        if (!Files.isDirectory(source)) throw new Stop(source + "가 없다. 같은 --repo, --commit으로 init을 다시 부른다.");
        String commit = JSON.readTree(targetFile.toFile()).path("commit").asText();
        String head = Git.run(source, "rev-parse", "HEAD");
        if (!head.equals(commit)) throw new Stop("source/가 고정 커밋 " + commit + "이 아니라 " + head + "이다. 고정 커밋으로 되돌린 뒤 다시 부른다.");
        String changes = Git.run(source, "status", "--porcelain");
        if (!changes.isEmpty()) throw new Stop("source/에 바뀐 파일이 있다. 대상 코드는 읽기만 한다. 되돌린 뒤 다시 부른다.\n" + changes);

        var analysis = new Verifier().verify(workspace, source);
        Path output = workspace.resolve("out/analysis.json");
        Files.createDirectories(output.getParent());
        write(output, analysis);
        analysis.path("checks").properties().forEach(e -> System.out.println(e.getKey() + ": 경고 "
                + e.getValue().path("warningCount").asInt() + "개"));
        System.out.println("썼다: " + output);
    }

    private static void serve(Path workspace, String port) {
        Path output = workspace.resolve("out/analysis.json");
        if (!Files.exists(output)) throw new Stop(output + "가 없다. 먼저 이 작업 폴더에서 verify를 부른다.");
        SpringApplication.run(ServerApplication.class, "--analysis.path=" + output, "--server.port=" + port);
    }

    /** The same bytes on every OS, so the committed example output does not change with the machine. */
    private static void write(Path file, JsonNode node) throws Exception {
        var printer = new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter("  ", "\n"));
        Files.writeString(file, JSON.writer(printer).writeValueAsString(node) + "\n");
    }

    private static void delete(Path directory) throws Exception {
        if (!Files.exists(directory)) return;
        try (var paths = Files.walk(directory)) {
            for (var path : paths.sorted(Comparator.reverseOrder()).toList()) {
                path.toFile().setWritable(true);
                Files.delete(path);
            }
        }
    }
}
