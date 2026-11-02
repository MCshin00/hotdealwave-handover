package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.stream.StreamSupport;

final class Checks {
    static final String CONFIRMED = "확인됨", UNKNOWN = "확인 안 됨", ABSENT = "저장소에 근거 없음";
    private Checks() {}
    static List<JsonNode> items(JsonNode node) { return StreamSupport.stream(node.spliterator(), false).toList(); }
    static String value(JsonNode node) { return node.path("value").isTextual() ? node.path("value").asText() : null; }
    static boolean known(JsonNode node) { return CONFIRMED.equals(node.path("status").asText()); }
    static JsonNode find(JsonNode nodes, String key, String value) {
        return items(nodes).stream().filter(n -> n.path(key).asText().equals(value)).findFirst()
                .orElse(com.fasterxml.jackson.databind.node.MissingNode.getInstance());
    }
    static void verdict(JsonNode node, String status, String reason) {
        if (!(node instanceof ObjectNode object)) return;
        object.put("status", status);
        boolean fact = object.has("value") && !object.path("value").isObject();
        if (reason != null || fact || object.has("type")) object.put("reason", reason);
        if (!CONFIRMED.equals(status) && fact) object.putNull("value");
    }
    static void warn(ObjectNode analysis, String check, JsonNode evidence, String reason) {
        var warning = analysis.withArray("warnings").addObject().put("check", check).put("status", UNKNOWN);
        warning.set("evidence", evidence.deepCopy());
        warning.put("reason", reason);
    }
    static ObjectNode count(ObjectNode analysis, String check) {
        int count = (int) items(analysis.path("warnings")).stream().filter(w -> check.equals(w.path("check").asText())).count();
        return ((ObjectNode) analysis.path("checks")).putObject(check).put("warningCount", count);
    }
    static void clearVerdicts(JsonNode node) {
        if (node instanceof ObjectNode object) object.remove(List.of("status", "reason"));
        node.forEach(Checks::clearVerdicts);
    }
}
