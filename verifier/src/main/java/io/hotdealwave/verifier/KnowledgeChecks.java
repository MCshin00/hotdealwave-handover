package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import static io.hotdealwave.verifier.Checks.*;

/** Knowledge keeps the developer's confirmation; checks only add warnings. */
final class KnowledgeChecks {
    void run(Path input, Path target, ObjectNode analysis) throws Exception {
        Path extraction = input.toAbsolutePath().normalize();
        Path directory = extraction.resolve("knowledge");
        if (!Files.isDirectory(directory)) return;

        var json = new ObjectMapper();
        var schema = ExtractionInput.registry().getSchema(ExtractionInput.schema("knowledge"));
        var ids = new HashSet<String>();
        for (String key : List.of("endpoints", "httpCalls", "messagePublications", "messageSubscriptions"))
            for (var item : analysis.path(key)) ids.add(item.path("id").asText());
        var knowledge = analysis.putArray("knowledge");
        var interpreted = new ArrayList<Map.Entry<String, JsonNode>>();
        try (var files = Files.list(directory)) {
            for (var file : files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList()) {
                var document = json.readTree(file.toFile());
                String location = "knowledge/" + file.getFileName();
                schema.validate(document).stream().sorted(Comparator.comparing(message -> message.toString()))
                        .forEach(message -> warn(analysis, "schema", "schema-invalid", json.createArrayNode(), location + ": " + message));
                for (var item : document.path("items")) {
                    String label = location + ": " + item.path("title").asText();
                    for (var evidence : item.path("evidence")) {
                        String reference = evidence.asText();
                        String prefix = "responses/";
                        boolean exists = reference.startsWith(prefix)
                                ? SourceText.isResponseEvidence(reference)
                                    && lineExists(extraction.resolve("responses"), reference.substring(prefix.length()))
                                : lineExists(target, reference);
                        if (!exists) warn(analysis, "knowledge-evidence", fileExists(reference.startsWith(prefix) ? extraction : target, reference)
                                        ? "line-missing" : "file-missing", json.createArrayNode().add(reference),
                                label + ": 근거 파일·줄을 확인하지 못함");
                    }
                    for (var id : item.path("affectedIds")) {
                        if (!ids.contains(id.asText())) warn(analysis, "knowledge-affected-ids", "unknown-id", item.path("evidence"),
                                label + ": 추출 결과에 영향 ID 없음: " + id.asText());
                    }
                    if (!"틀림".equals(item.path("confirmationStatus").asText())) knowledge.add(item);
                    if ("AI 해석".equals(item.path("source").asText())) interpreted.add(Map.entry(label, item));
                }
            }
        }
        for (int i = 0; i < interpreted.size(); i++) for (int j = i + 1; j < interpreted.size(); j++) {
            var a = interpreted.get(i).getValue();
            var b = interpreted.get(j).getValue();
            if (!a.path("service").equals(b.path("service")) && affectedIds(a).equals(affectedIds(b)))
                warn(analysis, "knowledge-duplicates", "duplicate-item", json.createArrayNode(),
                        "서비스가 다른 항목의 영향 ID 목록이 같음: " + interpreted.get(i).getKey() + " / " + interpreted.get(j).getKey());
        }
        count(analysis, "knowledge-evidence");
        count(analysis, "knowledge-affected-ids");
        count(analysis, "knowledge-duplicates");
    }

    private static Set<String> affectedIds(JsonNode item) {
        var ids = new TreeSet<String>();
        for (var id : item.path("affectedIds")) ids.add(id.asText());
        return ids;
    }

    private static boolean fileExists(Path root, String evidence) throws IOException {
        int colon = evidence.lastIndexOf(':');
        return colon > 0 && !evidence.contains("\\") && SourceText.fileUnder(root, evidence.substring(0, colon)) != null;
    }

    private boolean lineExists(Path root, String evidence) throws IOException {
        if (!evidence.matches("^(?!/)[^\\\\:\\r\\n]+:[1-9][0-9]*$")) return false;
        int colon = evidence.lastIndexOf(':');
        Path file = SourceText.fileUnder(root, evidence.substring(0, colon));
        if (file == null) return false;
        try {
            int number = Integer.parseInt(evidence.substring(colon + 1));
            try (var lines = Files.lines(file)) { return lines.limit(number).count() == number; }
        } catch (NumberFormatException e) { return false; }
    }
}
