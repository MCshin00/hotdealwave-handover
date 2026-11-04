package io.hotdealwave.server;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Component;
import static io.hotdealwave.server.Analysis.known;
import static io.hotdealwave.server.Analysis.value;

/** Text for page bodies: a confirmed value with its evidence, otherwise the status and reason in its place. */
@Component("view")
public class View {
    public String fact(JsonNode fact) {
        if (fact.isMissingNode()) return "";
        String shown = known(fact) ? value(fact) : fact.path("status").asText() + " — " + fact.path("reason").asText("");
        return shown + " " + evidence(fact);
    }

    public String status(JsonNode node) {
        if (known(node)) return node.path("status").asText();
        return node.path("status").asText() + (node.path("reason").isTextual() ? " — " + node.path("reason").asText() : "");
    }

    /** The value alone, for link text; the status when the value is not confirmed. */
    public String plain(JsonNode fact) {
        return known(fact) ? value(fact) : fact.path("status").asText();
    }

    public String evidence(JsonNode node) {
        return texts(node.path("evidence"));
    }

    public String texts(JsonNode list) {
        return "[" + String.join(", ", Analysis.listOf(list).stream().map(JsonNode::asText).toList()) + "]";
    }

    public String topic(JsonNode kafka) {
        return Queries.topicName(kafka);
    }

    public String step(Queries.Step step) {
        String to = step.to() != null ? step.to() : step.status() + (step.reason() != null ? " — " + step.reason() : "");
        return step.from() + " → (" + step.kind() + ") " + to + " [" + String.join(", ", step.evidence()) + "]";
    }

    /** How far an endpoint was checked: code alone, or code plus a recorded live response. */
    public String verification(JsonNode endpoint) {
        var check = endpoint.path("responseCheck");
        if (!"응답 기록 있음".equals(check.path("status").asText())) return "코드만";
        int code = check.path("statusCode").asInt();
        return code / 100 == 2 ? "코드 + 실제 응답" : "코드 + 실제 응답(오류 " + code + ")";
    }

    /** Endpoint count per verification scope, error codes merged. */
    public String verificationSummary(List<JsonNode> endpoints) {
        if (endpoints.isEmpty()) return "코드만(엔드포인트 없음)";
        var counts = new LinkedHashMap<String, Integer>();
        for (var scope : List.of("코드 + 실제 응답", "코드 + 실제 응답(오류)", "코드만")) counts.put(scope, 0);
        for (var endpoint : endpoints) counts.merge(verification(endpoint).replaceFirst(" \\d+\\)", ")"), 1, Integer::sum);
        var parts = counts.entrySet().stream().filter(e -> e.getValue() > 0).map(e -> e.getKey() + " " + e.getValue() + "개").toList();
        return "엔드포인트 " + endpoints.size() + "개 중 " + String.join(", ", parts);
    }

    public String responseCheck(JsonNode check) {
        String status = check.path("status").asText();
        if (check.path("statusCode").isNumber()) status += " " + check.path("statusCode").asInt();
        if (check.path("reason").isTextual()) status += " (" + check.path("reason").asText() + ")";
        return status;
    }

    /** Kind and source; the confirmation status only once the developer has set it. */
    public String item(JsonNode item) {
        String status = item.path("confirmationStatus").asText();
        return "[" + item.path("kind").asText() + " · " + item.path("source").asText() + ("미확인".equals(status) ? "" : " · " + status) + "] "
                + item.path("title").asText();
    }

    /** Field tree lines of a request or response type, one field per line. */
    public List<String> fields(JsonNode schema) {
        var lines = new ArrayList<String>();
        fields(schema, "", lines);
        return lines;
    }

    private void fields(JsonNode schema, String indent, List<String> lines) {
        for (var field : Analysis.listOf(schema.path("fields"))) {
            var annotations = Analysis.listOf(field.path("annotations")).stream().map(a -> "@" + a.path("name").asText()).toList();
            lines.add(indent + fact(field.path("jsonName")).replaceFirst(" \\[.*", "") + "  " + field.path("schema").path("type").asText()
                    + "  " + (annotations.isEmpty() ? "어노테이션 없음" : String.join(" ", annotations)) + "  " + status(field) + " " + evidence(field)
                    + (known(field.path("schema")) ? "" : "  타입 구조: " + status(field.path("schema"))));
            if (!field.path("schema").path("recursive").asBoolean()) nested(field.path("schema"), indent + "    ", lines);
        }
    }

    private void nested(JsonNode schema, String indent, List<String> lines) {
        fields(schema, indent, lines);
        for (var argument : Analysis.listOf(schema.path("arguments"))) nested(argument, indent, lines);
    }
}
