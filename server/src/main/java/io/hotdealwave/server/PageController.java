package io.hotdealwave.server;

import java.util.*;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

/** Endpoint, topic and service bodies as HTML. Search and MCP read the same bodies as text. */
@Controller
class PageController {
    record ServiceCard(String name, long endpoints, long feignCalls, long publications, long subscriptions, long responded) {}
    record TopicCard(String name, int publications, int subscriptions) {}
    /** A verifier warning: a connection the repository alone cannot make, with the page to look at. */
    record Gap(String kind, String reason, String evidence, String page, String target) {
        /** The status part of the reason, such as "구독자 확인 안 됨". */
        public String title() { int i = reason.indexOf(": "); return i < 0 ? reason : reason.substring(0, i); }
        public String detail() { int i = reason.indexOf(": "); return i < 0 ? "" : reason.substring(i + 2); }
    }

    private static final Pattern TOPIC = Pattern.compile("같은 (?:환경변수|토픽 이름)\\(([^)]+)\\)");
    private static final Pattern CALLER = Pattern.compile("확인 안 됨: ([^.\\s]+)\\.");

    private final Analysis analysis;
    private final Pages pages;
    private final Queries queries;
    private final View view;

    PageController(Analysis analysis, Pages pages, Queries queries, View view) {
        this.analysis = analysis; this.pages = pages; this.queries = queries; this.view = view;
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody String index() {
        var cards = pages.services().stream().map(name -> new ServiceCard(name,
                count("endpoints", name), count("feignCalls", name), count("kafkaPublications", name), count("kafkaSubscriptions", name),
                analysis.list("endpoints").stream().filter(e -> e.path("service").asText().equals(name))
                        .filter(e -> "응답 기록 있음".equals(e.path("responseCheck").path("status").asText())).count())).toList();
        var topics = pages.topics().stream().map(name -> {
            var topic = queries.findSubscribers(name);
            return new TopicCard(name, topic.publications().size(), topic.subscriptions().size());
        }).toList();
        var gaps = analysis.list("warnings").stream().map(this::gap).toList();
        return pages.html("index", Map.of("cards", cards, "topics", topics, "gaps", gaps,
                "endpointCount", analysis.list("endpoints").size(), "responded", cards.stream().mapToLong(ServiceCard::responded).sum()));
    }

    private long count(String key, String service) {
        return analysis.list(key).stream().filter(n -> n.path("service").asText().equals(service)).count();
    }

    private Gap gap(com.fasterxml.jackson.databind.JsonNode warning) {
        String reason = warning.path("reason").asText(), check = warning.path("check").asText();
        var topic = TOPIC.matcher(reason);
        var caller = CALLER.matcher(reason);
        String kind = switch (check) { case "kafka-topics" -> "Kafka 연결"; case "handler-calls" -> "Feign 호출"; default -> check; };
        if (topic.find()) return new Gap(kind, reason, view.evidence(warning), "/topics/" + topic.group(1), topic.group(1));
        if (caller.find()) return new Gap(kind, reason, view.evidence(warning), "/services/" + caller.group(1), caller.group(1));
        return new Gap(kind, reason, view.evidence(warning), null, null);
    }

    @GetMapping(value = "/endpoints/{id}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody String endpoint(@PathVariable String id) { return show("/endpoints/" + id); }

    @GetMapping(value = "/topics/{name}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody String topic(@PathVariable String name) { return show("/topics/" + name); }

    @GetMapping(value = "/services/{name}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody String service(@PathVariable String name) { return show("/services/" + name); }

    @GetMapping(value = "/repository", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody String repository() { return show("/repository"); }

    /** A recorded response file with numbered lines, which response evidence links to. */
    @GetMapping("/responses/{file:.+}")
    String response(@PathVariable String file, Model model) {
        var endpoint = analysis.list("endpoints").stream()
                .filter(e -> { var m = Decor.RESPONSE_FILE.matcher(e.path("responseCheck").path("file").asText()); return m.matches() && m.group(1).equals(file); }).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("file", file);
        model.addAttribute("endpoint", endpoint.path("id").asText());
        model.addAttribute("lines", Analysis.listOf(endpoint.path("responseCheck").path("lines")).stream().map(l -> l.asText()).toList());
        return "response";
    }

    private String show(String path) {
        String html = pages.html(path);
        if (html == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return html;
    }
}
