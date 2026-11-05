package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import static io.hotdealwave.verifier.Checks.*;

/** Stored responses are observations: each endpoint carries its record and how the observed keys compare with the code. */
final class ResponseChecks {
    private static final String PREFIX = "responses/";
    private final Map<String, List<String>> files = new HashMap<>();
    private Path responses;

    void run(Path input, ObjectNode analysis) throws Exception {
        responses = input.toAbsolutePath().normalize().resolve("responses");
        var json = new ObjectMapper();
        for (var endpoint : analysis.path("endpoints")) {
            fields(analysis, endpoint.path("response"));
            for (var parameter : endpoint.path("parameters")) fields(analysis, parameter.path("schema"));
            var check = ((ObjectNode) endpoint).putObject("responseCheck");
            String reference = PREFIX + endpoint.path("id").asText() + ".json";
            Path file = SourceText.isResponseEvidence(reference) ? SourceText.fileUnder(responses, endpoint.path("id").asText() + ".json") : null;
            if (file == null) {
                check.put("status", "코드만").putNull("statusCode").putNull("reason").putNull("file");
                check.putArray("lines");
                check.putArray("comparison");
                continue;
            }
            var lines = Files.readAllLines(file);
            var response = json.readTree(String.join("\n", lines));
            check.put("status", response.path("statusCode").isNull() ? "호출 못 함" : "응답 기록 있음");
            check.set("statusCode", response.path("statusCode"));
            check.set("reason", response.path("reason"));
            check.put("file", reference);
            var array = check.putArray("lines");
            lines.forEach(array::add);
            var comparison = check.putArray("comparison");
            ResponseComparison.compare(check, endpoint.path("response"), response.path("body")).forEach(comparison::add);
        }
    }

    /** A field confirmed from a stored response must have its JSON key on each response line it cites. */
    private void fields(ObjectNode analysis, JsonNode schema) throws IOException {
        String reason = "jsonName: 응답 근거 줄에서 JSON 키를 확인하지 못함";
        for (var field : schema.path("fields")) {
            String key = field.path("jsonName").path("value").asText(null);
            String failed = null;
            for (var node : List.of(field, field.path("jsonName"), field.path("schema")))
                for (var evidence : node.path("evidence")) {
                    if (!SourceText.isResponseEvidence(evidence.asText())) continue;
                    String line = line(evidence.asText());
                    if (key != null && line != null && line.contains("\"" + key + "\"")) continue;
                    verdict(node, UNKNOWN, reason);
                    if (failed == null) failed = evidence.asText();
                }
            if (failed != null) {
                verdict(field.path("jsonName"), UNKNOWN, reason);
                verdict(field, UNKNOWN, reason);
                warn(analysis, "evidence-lines", "extraction-mismatch", analysis.arrayNode().add(failed), reason);
            }
            fields(analysis, field.path("schema"));
        }
        for (var argument : schema.path("arguments")) fields(analysis, argument);
    }

    private String line(String evidence) throws IOException {
        int colon = evidence.lastIndexOf(':');
        if (colon < 0) return null;
        String name = evidence.substring(PREFIX.length(), colon);
        if (!files.containsKey(name)) {
            Path file = SourceText.fileUnder(responses, name);
            files.put(name, file == null ? List.of() : Files.readAllLines(file));
        }
        var lines = files.get(name);
        int number = Integer.parseInt(evidence.substring(colon + 1));
        return number > 0 && number <= lines.size() ? lines.get(number - 1) : null;
    }
}
