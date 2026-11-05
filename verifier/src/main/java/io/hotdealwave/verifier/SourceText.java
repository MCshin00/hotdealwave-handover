package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/** Text only; evidence cannot select environment files or leave the target. */
final class SourceText {
    private static final Pattern RESPONSE_EVIDENCE = Pattern.compile(
            "responses/(?:[^/:]+/)*[^/:]+\\.json(?::[1-9][0-9]*)?");
    private final Path root;
    private final Map<Path, List<String>> cache = new HashMap<>();
    SourceText(Path root) throws IOException { this.root = root.toRealPath(); }

    static boolean isResponseEvidence(String evidence) {
        return RESPONSE_EVIDENCE.matcher(evidence).matches()
                && Arrays.stream(evidence.split("/")).noneMatch(part -> part.equals(".") || part.equals(".."));
    }

    static boolean isExcluded(Path relative) {
        for (Path part : relative) {
            String name = part.toString().toLowerCase(Locale.ROOT);
            if (name.equals(".env") || name.startsWith(".env.") || name.equals(".local") || name.equals(".git")) return true;
        }
        return false;
    }

    /** The regular file at relative under root, or null when its real path leaves root or is excluded. */
    static Path fileUnder(Path root, String relative) throws IOException {
        if (relative.contains("\\") || isExcluded(Path.of(relative))) return null;
        Path file = root.resolve(relative);
        if (!Files.isRegularFile(file)) return null;
        Path real = file.toRealPath(), realRoot = root.toRealPath();
        return real.startsWith(realRoot) && !isExcluded(realRoot.relativize(real)) ? file : null;
    }

    private Path path(String relative) throws IOException {
        var candidate = root.resolve(relative).normalize();
        if (!candidate.startsWith(root) || isExcluded(root.relativize(candidate))) return null;
        String name = candidate.getFileName().toString();
        if (!(name.endsWith(".java") || name.endsWith(".yml") || name.equals("build.gradle")
                || name.equals("Dockerfile") || name.endsWith(".xml") || name.endsWith(".md"))) return null;
        if (!Files.isRegularFile(candidate)) return null;
        Path real = candidate.toRealPath();
        if (!real.startsWith(root) || isExcluded(root.relativize(real))) return null;
        return candidate;
    }

    List<String> lines(String relative) throws IOException {
        Path file = path(relative);
        if (file == null) return List.of();
        if (!cache.containsKey(file)) cache.put(file, Files.readAllLines(file));
        return cache.get(file);
    }

    String line(String evidence) throws IOException {
        int colon = evidence.lastIndexOf(':');
        if (colon < 1) return null;
        try {
            int number = Integer.parseInt(evidence.substring(colon + 1));
            var lines = lines(evidence.substring(0, colon));
            return number > 0 && number <= lines.size() ? lines.get(number - 1) : null;
        } catch (NumberFormatException e) { return null; }
    }

    List<String> evidenceLines(JsonNode node) throws IOException {
        var result = new ArrayList<String>();
        for (JsonNode evidence : node.path("evidence")) {
            if (isResponseEvidence(evidence.asText())) continue;
            String line = line(evidence.asText());
            if (line == null) return List.of();
            result.add(line);
        }
        return result;
    }

    /** Java lines with comments removed, for the declaration inventories only. */
    Map<String, String> javaLines() throws IOException {
        var result = new LinkedHashMap<String, String>();
        try (var directories = Files.list(root)) {
            for (var directory : directories.sorted().toList()) {
                String name = directory.getFileName().toString();
                if (name.startsWith(".")) continue;
                Path javaRoot = directory.resolve("src/main/java");
                if (!Files.isRegularFile(directory.resolve("build.gradle")) || !Files.isDirectory(javaRoot)) continue;
                try (var files = Files.walk(javaRoot)) {
                    for (var file : files.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
                        String relative = root.relativize(file).toString().replace('\\', '/');
                        var lines = withoutComments(lines(relative));
                        for (int i = 0; i < lines.size(); i++) result.put(relative + ":" + (i + 1), lines.get(i));
                    }
                }
            }
        }
        return result;
    }

    private static List<String> withoutComments(List<String> lines) {
        var result = new ArrayList<String>();
        boolean block = false;
        for (String line : lines) {
            var code = new StringBuilder();
            boolean string = false;
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i), next = i + 1 < line.length() ? line.charAt(i + 1) : 0;
                if (block) {
                    if (c == '*' && next == '/') { block = false; i++; }
                } else if (string) {
                    code.append(c);
                    if (c == '\\' && next != 0) code.append(line.charAt(++i));
                    else if (c == '"') string = false;
                } else if (c == '/' && next == '/') {
                    break;
                } else if (c == '/' && next == '*') {
                    block = true;
                    i++;
                } else {
                    if (c == '"') string = true;
                    code.append(c);
                }
            }
            result.add(code.toString());
        }
        return result;
    }

    String yamlKey(String evidence) throws IOException {
        int colon = evidence.lastIndexOf(':');
        String file = evidence.substring(0, colon);
        if (!file.endsWith(".yml")) return "";
        int number = Integer.parseInt(evidence.substring(colon + 1));
        var parents = new TreeMap<Integer, String>();
        var pattern = Pattern.compile("^(\\s*)([\\w-]+):(?:\\s|$)");
        var lines = lines(file);
        for (int i = 0; i < Math.min(number, lines.size()); i++) {
            var match = pattern.matcher(lines.get(i));
            if (!match.find()) continue;
            int indent = match.group(1).length();
            parents.tailMap(indent).clear();
            parents.put(indent, match.group(2));
        }
        return String.join(".", parents.values());
    }
}
