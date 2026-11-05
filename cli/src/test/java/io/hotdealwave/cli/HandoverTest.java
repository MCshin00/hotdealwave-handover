package io.hotdealwave.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class HandoverTest {
    private static final Path TOOL = Path.of("").toAbsolutePath();
    @TempDir Path temporary;
    private Path checkout;
    private String commit;

    /** A checkout the user already has: one commit, origin on GitHub, and a file changed after that commit. */
    @BeforeEach void checkout() throws Exception {
        checkout = Files.createDirectories(temporary.resolve("shop"));
        Git.run(checkout, "init", "-q");
        Files.writeString(checkout.resolve("README.md"), "shop\n");
        Git.run(checkout, "add", "README.md");
        Git.run(checkout, "-c", "user.name=t", "-c", "user.email=t@t", "commit", "-q", "-m", "first");
        Git.run(checkout, "remote", "add", "origin", "git@github.com:acme/shop.git");
        commit = Git.run(checkout, "rev-parse", "HEAD");
        Files.writeString(checkout.resolve("README.md"), "changed by the user\n");
    }

    @Test void initClonesThePinnedCommitAndWritesTargetAndInstructions() throws Exception {
        Path workspace = temporary.resolve("shop-work");

        Handover.run(TOOL, "init", "--repo", checkout.toString(), "--commit", commit.substring(0, 7), "--workspace", workspace.toString());

        assertEquals(commit, Git.run(workspace.resolve("source"), "rev-parse", "HEAD"));
        assertEquals("shop", Files.readString(workspace.resolve("source/README.md")).strip());
        var target = new ObjectMapper().readTree(workspace.resolve("target.json").toFile());
        assertEquals("shop", target.path("name").asText());
        assertEquals("https://github.com/acme/shop", target.path("repository").asText());
        assertEquals(commit, target.path("commit").asText());
        String instructions = Files.readString(workspace.resolve("AGENTS.md"));
        assertTrue(instructions.contains(".claude/skills/extract-msa-java-spring/SKILL.md"));
        assertTrue(instructions.contains("build/handover.jar verify"));
        assertEquals("@AGENTS.md\n", Files.readString(workspace.resolve("CLAUDE.md")));
        assertEquals("changed by the user\n", Files.readString(checkout.resolve("README.md")));
    }

    @Test void instructionsAskHowToSplitServiceExtraction() throws Exception {
        Path workspace = temporary.resolve("shop-work");
        Handover.run(TOOL, "init", "--repo", checkout.toString(), "--commit", commit, "--workspace", workspace.toString());
        String instructions = Files.readString(workspace.resolve("AGENTS.md"));
        assertAll(
                () -> assertTrue(instructions.contains("서비스 수")),
                () -> assertTrue(instructions.contains("서비스별 하위 에이전트로 동시에")),
                () -> assertTrue(instructions.contains("한 세션에서 차례로")),
                () -> assertTrue(instructions.contains("사용자에게 묻고")),
                () -> assertTrue(instructions.contains("고른 대로")),
                () -> assertTrue(instructions.contains("하위 에이전트가 없는 도구")),
                () -> assertTrue(instructions.contains("서비스 목록을 나눠 세션을 여러 개")));
    }

    @Test void initTakesAnAddressAndStopsWhenItIsNotGitHub() {
        Path workspace = temporary.resolve("shop-work");

        var stop = assertThrows(Handover.Stop.class, () -> Handover.run(TOOL, "init", "--repo", "https://gitlab.com/acme/shop",
                "--commit", commit, "--workspace", workspace.toString()));

        assertTrue(stop.getMessage().contains("GitHub"));
        assertFalse(Files.exists(workspace.resolve("source")));
    }

    @Test void initOnACommittedExampleKeepsItsResultsAndOnlyFetchesTheSource() throws Exception {
        Path workspace = temporary.resolve("example");
        Handover.run(TOOL, "init", "--repo", checkout.toString(), "--commit", commit, "--workspace", workspace.toString());
        Files.writeString(Files.createDirectories(workspace.resolve("services")).resolve("a.json"), "{}");
        delete(workspace.resolve("source"));

        Handover.run(TOOL, "init", "--repo", checkout.toString(), "--commit", commit, "--workspace", workspace.toString());

        assertEquals(commit, Git.run(workspace.resolve("source"), "rev-parse", "HEAD"));
        assertEquals("{}", Files.readString(workspace.resolve("services/a.json")));
    }

    @Test void verifyStopsWhenTheSourceChanged() throws Exception {
        Path workspace = temporary.resolve("shop-work");
        Handover.run(TOOL, "init", "--repo", checkout.toString(), "--commit", commit, "--workspace", workspace.toString());
        Files.writeString(workspace.resolve("source/README.md"), "edited during extraction\n");

        var stop = assertThrows(Handover.Stop.class, () -> Handover.run(TOOL, "verify", "--workspace", workspace.toString()));

        assertTrue(stop.getMessage().contains("README.md"));
        assertFalse(Files.exists(workspace.resolve("out/analysis.json")));
    }

    private static void delete(Path directory) throws Exception {
        try (var paths = Files.walk(directory)) {
            for (var path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                path.toFile().setWritable(true);
                Files.delete(path);
            }
        }
    }

    @Test void serveWithoutVerifierOutputAsksForVerifyFirst() {
        var stop = assertThrows(Handover.Stop.class, () -> Handover.run(TOOL, "serve", "--workspace", temporary.toString()));

        assertTrue(stop.getMessage().contains("verify"));
    }
}
