package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;

/** The committed example workspace. Its source/ comes from init (README "검사"). */
final class Example {
    static final Path WORKSPACE = Path.of("examples/hotdealwave");
    static final Path SOURCE = WORKSPACE.resolve("source");

    private Example() {}

    /** What the verify command writes, for an extraction directory read against the example source. */
    static void verify(Path input, Path output) throws Exception {
        new ObjectMapper().writeValue(output.toFile(), new Verifier().verify(input, SOURCE));
    }
}
