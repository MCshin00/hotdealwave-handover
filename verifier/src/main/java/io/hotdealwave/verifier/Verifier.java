package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;

public final class Verifier {
    public JsonNode verify(JsonNode input, Path target) throws Exception {
        var analysis = (ObjectNode) input.deepCopy();
        analysis.putObject("checks");
        analysis.putArray("warnings");
        Checks.clearVerdicts(analysis);
        var source = new SourceText(target);
        new EvidenceChecks(source, analysis).run();
        var lines = source.javaLines();
        new CrossChecks().run(lines, analysis);
        new KafkaChecks().run(lines, analysis);
        return analysis;
    }
}
