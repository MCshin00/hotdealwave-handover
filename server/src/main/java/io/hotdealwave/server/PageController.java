package io.hotdealwave.server;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

/** Endpoint, topic and service bodies. The same bodies become search chunks in stage 7. */
@Controller
class PageController {
    private final Analysis analysis;
    private final Queries queries;

    PageController(Analysis analysis, Queries queries) { this.analysis = analysis; this.queries = queries; }

    @GetMapping("/")
    String index(Model model) {
        model.addAttribute("services", analysis.list("services").stream().map(s -> Analysis.value(s.path("name"))).toList());
        model.addAttribute("topics", topics());
        var counts = new LinkedHashMap<String, Integer>();
        analysis.checks().properties().forEach(e -> counts.put(e.getKey(), e.getValue().path("warningCount").asInt()));
        model.addAttribute("warnings", counts);
        return "index";
    }

    @GetMapping("/endpoints/{id}")
    String endpoint(@PathVariable String id, Model model) {
        var endpoint = found(analysis.find("endpoints", id));
        model.addAttribute("endpoint", endpoint);
        model.addAttribute("flow", queries.traceFlow(id));
        model.addAttribute("items", queries.knowledgeFor(List.of(id)));
        return "endpoint";
    }

    @GetMapping("/topics/{name}")
    String topic(@PathVariable String name, Model model) {
        var topic = queries.findSubscribers(name);
        if (topic.publications().isEmpty() && topic.subscriptions().isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        var ids = Stream.concat(topic.publications().stream(), topic.subscriptions().stream()).map(n -> n.path("id").asText()).toList();
        model.addAttribute("topic", topic);
        model.addAttribute("links", topic.publications().stream()
                .flatMap(p -> queries.traceFlow(p.path("id").asText()).forward().stream().filter(s -> s.from().equals(p.path("id").asText())))
                .filter(s -> s.kind().equals("구독")).toList());
        model.addAttribute("unpublished", topic.subscriptions().stream().map(queries::warning).filter(Objects::nonNull).toList());
        model.addAttribute("items", queries.knowledgeFor(ids));
        return "topic";
    }

    @GetMapping("/services/{name}")
    String service(@PathVariable String name, Model model) {
        var service = found(analysis.list("services").stream().filter(s -> name.equals(Analysis.value(s.path("name")))).findFirst()
                .orElse(com.fasterxml.jackson.databind.node.MissingNode.getInstance()));
        model.addAttribute("service", service);
        model.addAttribute("name", name);
        var ids = new ArrayList<String>();
        for (var key : List.of("endpoints", "feignCalls", "kafkaPublications", "kafkaSubscriptions", "environmentVariables", "externalResources")) {
            var owned = analysis.list(key).stream().filter(n -> n.path("service").asText().equals(name)).toList();
            model.addAttribute(key, owned);
            owned.forEach(n -> { if (n.has("id")) ids.add(n.path("id").asText()); });
        }
        model.addAttribute("items", queries.knowledgeFor(ids));
        return "service";
    }

    private List<String> topics() {
        return Stream.concat(analysis.list("kafkaPublications").stream(), analysis.list("kafkaSubscriptions").stream())
                .map(Queries::topicName).filter(Objects::nonNull).distinct().sorted().toList();
    }

    private static JsonNode found(JsonNode node) {
        if (node.isMissingNode()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return node;
    }
}
