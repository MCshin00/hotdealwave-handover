package io.hotdealwave.server;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.stream.Stream;
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
    /** An exception an endpoint can raise, with its error code and receiving handler when they were extracted. */
    public record Thrown(JsonNode link, JsonNode errorCode, JsonNode handler) {}

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
            var feign = analysis.find("httpCalls", current);
            if (!feign.isMissingNode()) next.add(step(current, value(feign.path("target")), feign.path("kind").asText() + " 대상", feign.path("target")));
            var publication = analysis.find("messagePublications", current);
            if (!publication.isMissingNode()) next.addAll(subscribers(publication));
            for (var step : next) {
                forward.add(step);
                if (step.to() != null && confirmed(step) && seen.add(step.to())) queue.add(step.to());
            }
        }
        return new Flow(forward, callers(id));
    }

    Topic findSubscribers(String topic) {
        var publications = analysis.list("messagePublications");
        var subscriptions = analysis.list("messageSubscriptions");
        var matched = Stream.concat(publications.stream(), subscriptions.stream()).filter(n -> topic.equals(topicName(n)))
                .map(n -> n.path("id").asText()).collect(java.util.stream.Collectors.toSet());
        var related = new HashSet<>(matched);
        for (var link : analysis.list("messageLinks"))
            if (known(link) && (matched.contains(link.path("publicationId").asText()) || matched.contains(link.path("subscriptionId").asText()))) {
                related.add(link.path("publicationId").asText());
                related.add(link.path("subscriptionId").asText());
            }
        return new Topic(topic, publications.stream().filter(p -> related.contains(p.path("id").asText())).toList(),
                subscriptions.stream().filter(p -> related.contains(p.path("id").asText())).toList());
    }

    /** Environment variable name, or the literal topic for the string DLQ topics. */
    static String topicName(JsonNode kafka) {
        String environment = known(kafka.path("environment")) ? value(kafka.path("environment")) : null;
        if (environment != null && !environment.isEmpty()) return environment;
        return known(kafka.path("topicName")) ? value(kafka.path("topicName")) : null;
    }

    /** Rules and traps whose affected IDs include any of the given IDs, traps first. */
    List<JsonNode> knowledgeFor(Collection<String> ids) {
        return affecting(ids).filter(k -> !"설명".equals(k.path("kind").asText()))
                .sorted(Comparator.comparing(k -> !"함정".equals(k.path("kind").asText()))).toList();
    }

    /** Descriptions of the given IDs. */
    List<JsonNode> descriptionsFor(Collection<String> ids) {
        return affecting(ids).filter(k -> "설명".equals(k.path("kind").asText())).toList();
    }

    private Stream<JsonNode> affecting(Collection<String> ids) {
        return analysis.list("knowledge").stream()
                .filter(k -> Analysis.listOf(k.path("affectedIds")).stream().anyMatch(a -> ids.contains(a.asText())));
    }

    /** message-destinations warning reason for an unlinked publication or subscription, if any. */
    String warning(JsonNode kafka) {
        return analysis.list("warnings").stream()
                .filter(w -> w.path("check").asText().equals("message-destinations") && w.path("evidence").equals(kafka.path("evidence")))
                .map(w -> w.path("reason").asText()).findFirst().orElse(null);
    }

    private List<Step> subscribers(JsonNode publication) {
        String id = publication.path("id").asText();
        var steps = analysis.list("messageLinks").stream().filter(l -> l.path("publicationId").asText().equals(id))
                .map(l -> step(id, l.path("subscriptionId").asText(), "구독", l)).toList();
        if (steps.stream().anyMatch(this::confirmed)) return steps;
        var unlinked = new ArrayList<>(steps);
        unlinked.add(new Step(id, null, "구독", "확인 안 됨", warning(publication), texts(publication.path("evidence"))));
        return unlinked;
    }

    private List<Step> callers(String id) {
        var steps = new ArrayList<Step>();
        for (var feign : analysis.list("httpCalls"))
            if (id.equals(value(feign.path("target")))) steps.add(step(feign.path("id").asText(), id, feign.path("kind").asText() + " 대상", feign.path("target")));
        for (var link : analysis.list("messageLinks"))
            if (link.path("subscriptionId").asText().equals(id)) steps.add(step(link.path("publicationId").asText(), id, "구독", link));
        var targets = steps.stream().map(Step::from).toList();
        for (var call : analysis.list("calls"))
            if (targets.contains(call.path("toId").asText()))
                steps.add(step(call.path("fromId").asText(), call.path("toId").asText(), "호출", call));
        return steps;
    }

    /** Exceptions the endpoint's handler reaches, in extraction order. */
    List<Thrown> errorsFor(String endpointId) {
        String service = analysis.find("endpoints", endpointId).path("service").asText();
        return analysis.list("throws").stream().filter(t -> t.path("fromId").asText().equals(endpointId)).map(t -> new Thrown(t,
                analysis.list("errorCodes").stream().filter(c -> c.path("service").asText().equals(service)
                        && c.path("name").asText().equals(t.path("errorCode").asText())).findFirst().orElse(com.fasterxml.jackson.databind.node.MissingNode.getInstance()),
                analysis.find("exceptionHandlers", t.path("handlerId").asText()))).toList();
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
