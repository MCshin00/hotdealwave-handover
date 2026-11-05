package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;

/** Stored responses are observations; they never change an endpoint's verdict. */
final class ResponseChecks {
    void run(Path input, ObjectNode analysis) throws Exception {
        Path extraction = input.toAbsolutePath().normalize();
        String prefix = "responses/";
        var json = new ObjectMapper();
        for (var endpoint : analysis.path("endpoints")) {
            var check = ((ObjectNode) endpoint).putObject("responseCheck");
            String reference = prefix + endpoint.path("id").asText() + ".json";
            Path file = SourceText.isResponseEvidence(reference)
                    ? SourceText.fileUnder(extraction.resolve("responses"), endpoint.path("id").asText() + ".json") : null;
            if (file == null) {
                check.put("status", "코드만").putNull("statusCode").putNull("reason").putNull("file");
                check.putArray("lines");
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
        }
    }
}
