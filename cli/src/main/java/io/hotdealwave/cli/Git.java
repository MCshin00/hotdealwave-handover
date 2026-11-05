package io.hotdealwave.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** The git command the user already has; the tool never stores credentials of its own. */
final class Git {
    private Git() {}

    static String run(Path directory, String... arguments) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("git", "-C", directory.toString()));
        command.addAll(List.of(arguments));
        Process process;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
        } catch (IOException e) {
            throw new Handover.Stop("git을 실행하지 못함. Git이 설치되어 PATH에 있어야 한다.");
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
        if (process.waitFor() != 0) throw new Handover.Stop("git " + String.join(" ", arguments) + " 실패:\n" + output);
        return output;
    }
}
