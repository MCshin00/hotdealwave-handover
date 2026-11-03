package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import static io.hotdealwave.verifier.Checks.*;

/** Knowledge keeps the developer's confirmation; checks only add warnings. */
final class KnowledgeChecks {
    void run(Path input, Path target, ObjectNode analysis) throws Exception {
        Path extraction = input.toAbsolutePath().normalize();
        String name = extraction.getFileName().toString();
        Path directory = extraction.getParent().resolveSibling("knowledge").resolve(name);
        if (!Files.isDirectory(directory)) return;

        var json = new ObjectMapper();
        var schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                .getSchema(Path.of("docs/format/knowledge.schema.json").toUri());
        var ids = new HashSet<String>();
        for (String key : List.of("endpoints", "feignCalls", "kafkaPublications", "kafkaSubscriptions"))
            for (var item : analysis.path(key)) ids.add(item.path("id").asText());
        var knowledge = analysis.putArray("knowledge");
        try (var files = Files.list(directory)) {
            for (var file : files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList()) {
                var document = json.readTree(file.toFile());
                String location = "knowledge/" + name + "/" + file.getFileName();
                schema.validate(document).stream().sorted(Comparator.comparing(message -> message.toString()))
                        .forEach(message -> warn(analysis, "schema", json.createArrayNode(), location + ": " + message));
                for (var item : document.path("items")) {
                    String label = location + ": " + item.path("title").asText();
                    for (var evidence : item.path("evidence")) {
                        String reference = evidence.asText();
                        String prefix = "analysis/" + name + "/responses/";
                        boolean exists = reference.startsWith("analysis/")
                                ? reference.startsWith(prefix) && SourceText.isResponseEvidence(reference)
                                    && lineExists(extraction.resolve("responses"), reference.substring(prefix.length()))
                                : lineExists(target, reference);
                        if (!exists) warn(analysis, "knowledge-evidence", json.createArrayNode().add(reference),
                                label + ": 근거 파일·줄을 확인하지 못함");
                    }
                    for (var id : item.path("affectedIds")) {
                        if (!ids.contains(id.asText())) warn(analysis, "knowledge-affected-ids", item.path("evidence"),
                                label + ": 추출 결과에 영향 ID 없음: " + id.asText());
                    }
                    if (!"틀림".equals(item.path("confirmationStatus").asText())) knowledge.add(item);
                }
            }
        }
        count(analysis, "knowledge-evidence");
        count(analysis, "knowledge-affected-ids");
    }

    private boolean lineExists(Path root, String evidence) throws IOException {
        if (!evidence.matches("^(?!/)[^\\\\:\\r\\n]+:[1-9][0-9]*$")) return false;
        int colon = evidence.lastIndexOf(':');
        String relative = evidence.substring(0, colon);
        Path file = root.resolve(relative);
        if (SourceText.isExcluded(Path.of(relative)) || !Files.isRegularFile(file) || !file.toRealPath().startsWith(root.toRealPath())) return false;
        try {
            int number = Integer.parseInt(evidence.substring(colon + 1));
            try (var lines = Files.lines(file)) { return lines.limit(number).count() == number; }
        } catch (NumberFormatException e) { return false; }
    }
}
