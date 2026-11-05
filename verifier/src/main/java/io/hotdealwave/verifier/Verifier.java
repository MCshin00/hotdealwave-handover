package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;

public final class Verifier {
    public JsonNode verify(Path input, Path target) throws Exception {
        var extracted = new ExtractionInput().read(input);
        var analysis = (ObjectNode) verify(extracted, target);
        analysis.withArray("warnings").addAll(extracted.withArray("warnings"));
        new KnowledgeChecks().run(input, target, analysis);
        new ResponseChecks().run(input, analysis);
        Checks.count(analysis, "schema");
        Checks.count(analysis, "evidence-lines");
        Checks.legend(analysis);
        return analysis;
    }

    public JsonNode verify(JsonNode input, Path target) throws Exception {
        var analysis = (ObjectNode) input.deepCopy();
        analysis.putObject("checks");
        analysis.putArray("warnings");
        Checks.clearVerdicts(analysis);
        var source = new SourceText(target);
        new EvidenceChecks(source, analysis).run();
        var lines = source.javaLines();
        CommunicationRules.inventory(lines, analysis);
        new HttpChecks().run(analysis);
        new CrossChecks().run(lines, analysis, source);
        new MessageChecks().run(analysis, source);
        new CallChecks().run(source, analysis);
        new ThrowChecks().run(source, analysis);
        Checks.legend(analysis);
        return analysis;
    }
}
