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

    /** The first line of an evidence list and how many more there are. */
    public String brief(JsonNode list) {
        var lines = Analysis.listOf(list);
        if (lines.isEmpty()) return "[]";
        return "[" + lines.get(0).asText() + (lines.size() > 1 ? " 외 " + (lines.size() - 1) + "줄" : "") + "]";
    }

    public String texts(JsonNode list) {
        return "[" + String.join(", ", Analysis.listOf(list).stream().map(JsonNode::asText).toList()) + "]";
    }

    public String topic(JsonNode kafka) {
        return Queries.topicName(kafka);
    }

    public String step(Queries.Step step) {
        return step.from() + " → (" + step.kind() + ") " + target(step) + " " + stepEvidence(step);
    }

    /** Where a step goes, or its status and reason when the target is not confirmed. */
    public String target(Queries.Step step) {
        return step.to() != null ? step.to() : step.status() + (step.reason() != null ? " — " + step.reason() : "");
    }

    public String stepEvidence(Queries.Step step) {
        return "[" + String.join(", ", step.evidence()) + "]";
    }

    /** File name of a recorded response, or null when none was recorded. */
    public String responseFile(JsonNode check) {
        String file = check.path("file").asText(null);
        return file == null ? null : file.substring(file.lastIndexOf('/') + 1);
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

    /** How the observed keys compare with the code: the differences, "차이 없음", or nothing when no response was observed. */
    public List<String> comparison(JsonNode check) {
        if (!"응답 기록 있음".equals(check.path("status").asText())) return List.of();
        var differences = Analysis.listOf(check.path("comparison")).stream().map(JsonNode::asText).toList();
        return differences.isEmpty() ? List.of("차이 없음") : differences;
    }

    public String responseCheck(JsonNode check) {
        String status = check.path("status").asText();
        if (check.path("statusCode").isNumber()) status += " " + check.path("statusCode").asInt();
        if (check.path("reason").isTextual()) status += " (" + check.path("reason").asText() + ")";
        return status;
    }

    /** An error code constant: code, HTTP status and message as declared. */
    public String errorCode(JsonNode code) {
        return code.path("name").asText() + " (" + code.path("enumType").asText() + ") 코드 " + shown(code.path("code"))
                + " · 상태 " + shown(code.path("httpStatus")) + " · 메시지 " + shown(code.path("message")) + " " + status(code) + " " + evidence(code);
    }

    /** An exception handler: what it catches and where status, error code and message come from. */
    public String handler(JsonNode handler) {
        return handler.path("id").asText() + ": " + plain(handler.path("exception")) + " → 상태 " + sourced(handler.path("httpStatus"))
                + " · 본문 " + handler.path("body").path("type").asText() + " 오류 코드 " + sourced(handler.path("errorCode"))
                + " · 메시지 " + sourced(handler.path("message")) + " " + status(handler) + " " + evidence(handler);
    }

    /** An exception an endpoint can raise, then the error code it carries and the handler that answers it. */
    public String thrown(Queries.Thrown thrown) {
        var link = thrown.link();
        String head = link.path("exception").asText() + (link.path("errorCode").asText().isEmpty() ? "" : " " + link.path("errorCode").asText())
                + (link.path("message").asText().isEmpty() ? "" : " \"" + link.path("message").asText() + "\"");
        String code = thrown.errorCode().isMissingNode() ? "" : " · 오류 코드 " + shown(thrown.errorCode().path("code"))
                + ", 상태 " + shown(thrown.errorCode().path("httpStatus")) + ", 메시지 " + shown(thrown.errorCode().path("message"));
        var h = thrown.handler();
        String handler = h.isMissingNode() ? "" : " · 처리기 " + h.path("id").asText() + ": 상태 " + sourced(h.path("httpStatus"))
                + ", 본문 오류 코드 " + sourced(h.path("errorCode")) + ", 메시지 " + sourced(h.path("message"));
        return head + code + handler + " · " + status(link) + " " + evidence(link);
    }

    /** A gateway rejection: condition, status and body. */
    public String rejection(JsonNode rejection) {
        return rejection.path("filter").asText() + ": 조건 " + shown(rejection.path("condition")) + " → 상태 " + shown(rejection.path("httpStatus"))
                + " · 본문 " + (known(rejection.path("body")) && value(rejection.path("body")).isEmpty() ? "없음" : shown(rejection.path("body")))
                + " " + status(rejection) + " " + evidence(rejection);
    }

    /** A value without its evidence: empty when the declaration has none, the status and reason when not confirmed. */
    private String shown(JsonNode fact) {
        if (!known(fact)) return status(fact);
        String value = value(fact);
        return value == null || value.isEmpty() ? "없음" : value;
    }

    /** A handler value by where it comes from. */
    private String sourced(JsonNode fact) {
        return switch (fact.path("source").asText()) {
            case "고정" -> shown(fact);
            case "던진 오류 코드" -> known(fact) ? "던진 오류 코드의 값" : status(fact);
            default -> status(fact);
        };
    }

    /** Plain strings of a list, comma separated. */
    public String joined(JsonNode list) {
        return String.join(", ", Analysis.listOf(list).stream().map(JsonNode::asText).toList());
    }

    /** Values of a fact list, each its value or status. */
    public String plains(JsonNode facts) {
        return String.join(", ", Analysis.listOf(facts).stream().map(this::plain).toList());
    }

    /** Kind and source; the confirmation status only once the developer has set it. */
    public String item(JsonNode item) {
        return itemTag(item) + " " + item.path("title").asText();
    }

    public String itemTag(JsonNode item) {
        String status = item.path("confirmationStatus").asText();
        return "[" + item.path("kind").asText() + " · " + item.path("source").asText() + ("미확인".equals(status) ? "" : " · " + status) + "]";
    }

    /** Style class for an item's kind tag. */
    public String kindClass(JsonNode item) {
        return switch (item.path("kind").asText()) {
            case "함정" -> "trap";
            case "업무 규칙" -> "rule";
            default -> "about";
        };
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
