package io.hotdealwave.server;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.springframework.stereotype.Component;
import static io.hotdealwave.server.Analysis.known;
import static io.hotdealwave.server.Analysis.value;

/** Relation lookups shared by pages, APIs and MCP. Never calls a model. */
@Component
class Queries {
    /** One hop. {@code to} is null when the next hop could not be confirmed; then status and reason say why. */
    public record Step(String from, String to, String kind, String status, String reason, List<String> evidence) {}
    public record Flow(List<Step> forward, List<Step> callers) {}
    public record Topic(String name, List<JsonNode> publications, List<JsonNode> subscriptions) {}

    private final Analysis analysis;

    Queries(Analysis analysis) { this.analysis = analysis; }

    List<JsonNode> findEndpoint(String query) {
        String q = query.trim();
        return analysis.list("endpoints").stream().filter(e -> e.path("id").asText().equals(q)
                || q.equalsIgnoreCase(value(e.path("httpMethod")) + " " + value(e.path("path")))
                || q.equals(value(e.path("path"))) || q.equals(e.path("handler").asText())).toList();
    }

    Flow traceFlow(String id) {
        var forward = new ArrayList<Step>();
        var queue = new ArrayDeque<>(List.of(id));
        var seen = new HashSet<>(queue);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            var next = new ArrayList<Step>();
            for (var call : analysis.list("calls"))
                if (call.path("fromId").asText().equals(current)) next.add(step(current, call.path("toId").asText(), "호출", call));
            var feign = analysis.find("feignCalls", current);
            if (!feign.isMissingNode()) next.add(step(current, value(feign.path("target")), "Feign 대상", feign.path("target")));
            var publication = analysis.find("kafkaPublications", current);
            if (!publication.isMissingNode()) next.addAll(subscribers(publication));
            for (var step : next) {
                forward.add(step);
                if (step.to() != null && confirmed(step) && seen.add(step.to())) queue.add(step.to());
            }
        }
        return new Flow(forward, callers(id));
    }

    Topic findSubscribers(String topic) {
        return new Topic(topic, analysis.list("kafkaPublications").stream().filter(p -> topic.equals(topicName(p))).toList(),
                analysis.list("kafkaSubscriptions").stream().filter(s -> topic.equals(topicName(s))).toList());
    }

    /** Environment variable name, or the literal topic for the string DLQ topics. */
    static String topicName(JsonNode kafka) {
        String environment = known(kafka.path("environment")) ? value(kafka.path("environment")) : null;
        if (environment != null && !environment.isEmpty()) return environment;
        return known(kafka.path("topicName")) ? value(kafka.path("topicName")) : null;
    }

    /** Items whose affected IDs include any of the given IDs, traps first. */
    List<JsonNode> knowledgeFor(Collection<String> ids) {
        return analysis.list("knowledge").stream()
                .filter(k -> Analysis.listOf(k.path("affectedIds")).stream().anyMatch(a -> ids.contains(a.asText())))
                .sorted(Comparator.comparing(k -> !"함정".equals(k.path("kind").asText()))).toList();
    }

    /** kafka-topics warning reason for an unlinked publication or subscription, if any. */
    String warning(JsonNode kafka) {
        return analysis.list("warnings").stream()
                .filter(w -> w.path("check").asText().equals("kafka-topics") && w.path("evidence").equals(kafka.path("evidence")))
                .map(w -> w.path("reason").asText()).findFirst().orElse(null);
    }

    private List<Step> subscribers(JsonNode publication) {
        String id = publication.path("id").asText();
        var steps = analysis.list("kafkaLinks").stream().filter(l -> l.path("publicationId").asText().equals(id))
                .map(l -> step(id, l.path("subscriptionId").asText(), "구독", l)).toList();
        if (steps.stream().anyMatch(this::confirmed)) return steps;
        var unlinked = new ArrayList<>(steps);
        unlinked.add(new Step(id, null, "구독", "확인 안 됨", warning(publication), texts(publication.path("evidence"))));
        return unlinked;
    }

    private List<Step> callers(String id) {
        var steps = new ArrayList<Step>();
        for (var feign : analysis.list("feignCalls"))
            if (id.equals(value(feign.path("target")))) steps.add(step(feign.path("id").asText(), id, "Feign 대상", feign.path("target")));
        for (var link : analysis.list("kafkaLinks"))
            if (link.path("subscriptionId").asText().equals(id)) steps.add(step(link.path("publicationId").asText(), id, "구독", link));
        var targets = steps.stream().map(Step::from).toList();
        for (var call : analysis.list("calls"))
            if (targets.contains(call.path("toId").asText()))
                steps.add(step(call.path("fromId").asText(), call.path("toId").asText(), "호출", call));
        return steps;
    }

    private boolean confirmed(Step step) { return Analysis.CONFIRMED.equals(step.status()); }

    private static Step step(String from, String to, String kind, JsonNode checked) {
        String reason = checked.path("reason").isTextual() ? checked.path("reason").asText() : null;
        return new Step(from, known(checked) ? to : null, kind, checked.path("status").asText(), reason, texts(checked.path("evidence")));
    }

    private static List<String> texts(JsonNode array) {
        return Analysis.listOf(array).stream().map(JsonNode::asText).toList();
    }
}
