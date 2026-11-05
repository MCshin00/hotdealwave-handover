package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExampleOutputTest {
    /** The committed example output is what verify writes now; regenerate it with verify when this fails. */
    @Test void committedExampleOutputIsCurrent() throws Exception {
        var committed = new ObjectMapper().readTree(Example.WORKSPACE.resolve("out/analysis.json").toFile());

        assertEquals(committed, new Verifier().verify(Example.WORKSPACE, Example.SOURCE));
    }
}
