package io.hotdealwave.server;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
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

    public String responseCheck(JsonNode check) {
        String status = check.path("status").asText();
        if (check.path("statusCode").isNumber()) status += " " + check.path("statusCode").asInt();
        if (check.path("reason").isTextual()) status += " (" + check.path("reason").asText() + ")";
        return status;
    }

    public String item(JsonNode item) {
        return "[" + item.path("kind").asText() + " · " + item.path("confirmationStatus").asText() + "] " + item.path("title").asText();
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
                    + (annotations.isEmpty() ? "" : "  " + String.join(" ", annotations)) + "  " + status(field) + " " + evidence(field));
            if (!field.path("schema").path("recursive").asBoolean()) nested(field.path("schema"), indent + "    ", lines);
        }
    }

    private void nested(JsonNode schema, String indent, List<String> lines) {
        fields(schema, indent, lines);
        for (var argument : Analysis.listOf(schema.path("arguments"))) nested(argument, indent, lines);
    }
}
