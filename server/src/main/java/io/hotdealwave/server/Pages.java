package io.hotdealwave.server;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.expression.ThymeleafEvaluationContext;

/** Endpoint, topic and service bodies. Pages show them as HTML; search and MCP read the same render as text. */
@Component
class Pages {
    record Page(String path, String template, Map<String, Object> model, String kind, Set<String> services) {}
    /** One h2 section of a body as text. The part above the first h2 has an empty heading. */
    record Section(String page, String title, String heading, String text, String kind, Set<String> services) {}
    /** An environment variable name with every place that reads it, when its value is not confirmed. */
    record Variable(String name, String status, String services, String profiles, String locations, String evidence) {}
    /** Warnings of one reason code as lines, with what to do about them. */
    record WarningGroup(String title, String action, List<String> lines) {}

    /** Gathers what starting the services again needs. Search leaves it out because every part repeats another page. */
    static final String REVIVAL = "/revival";

    private static final Pattern H2 = Pattern.compile("<h2[^>]*>(.*?)</h2>", Pattern.DOTALL);

    private final Analysis analysis;
    private final Queries queries;
    private final SpringTemplateEngine engine;
    private final Legend legend;
    private final View view;
    private final ThymeleafEvaluationContext evaluation;
    private Map<String, List<Section>> sections;

    Pages(Analysis analysis, Queries queries, SpringTemplateEngine engine, ApplicationContext context, Legend legend, View view) {
        this.analysis = analysis;
        this.view = view;
        this.legend = legend;
        this.queries = queries;
        this.engine = engine;
        this.evaluation = new ThymeleafEvaluationContext(context, null);
    }

    /** The page at a path such as /endpoints/{id}, or null. */
    Page page(String path) {
        if (path.equals("/repository")) return repository();
        if (path.equals(REVIVAL)) return revival();
        String[] parts = path.split("/", 3);
        if (parts.length != 3 || !parts[0].isEmpty()) return null;
        return switch (parts[1]) {
            case "endpoints" -> endpoint(parts[2]);
            case "topics" -> topic(parts[2]);
            case "services" -> service(parts[2]);
            default -> null;
        };
    }

    List<String> topics() {
        return Stream.concat(analysis.list("messagePublications").stream(), analysis.list("messageSubscriptions").stream())
                .map(Queries::topicName).filter(Objects::nonNull).distinct().sorted().toList();
    }

    List<String> services() {
        return analysis.list("services").stream().map(s -> Analysis.value(s.path("name"))).toList();
    }

    /** Sections of every page, rendered once. */
    synchronized Map<String, List<Section>> sections() {
        if (sections == null) {
            var paths = Stream.of(
                    analysis.list("endpoints").stream().map(e -> "/endpoints/" + e.path("id").asText()),
                    topics().stream().map(t -> "/topics/" + t),
                    services().stream().map(s -> "/services/" + s),
                    Stream.of("/repository").filter(p -> page(p) != null), Stream.of(REVIVAL)).flatMap(s -> s).toList();
            var all = new LinkedHashMap<String, List<Section>>();
            for (var path : paths) all.put(path, render(page(path)));
            sections = all;
        }
        return sections;
    }

    /** The whole body of a page as text, or null. */
    String text(String path) {
        var page = sections().get(path);
        if (page == null) return null;
        return String.join("\n\n", page.stream().map(s -> s.heading().isEmpty() ? s.text() : s.heading() + "\n" + s.text()).toList());
    }

    /** The page as HTML for the browser, with display-only markup, or null. */
    String html(String path) {
        var page = page(path);
        if (page == null) return null;
        String html = Decor.html(process(page.template(), page.model()), analysis.target());
        int title = html.indexOf("</h1>") + "</h1>".length();
        return html.substring(0, title) + legend.html(page.kind()) + html.substring(title);
    }

    /** Any other template as HTML for the browser, with display-only markup. */
    String html(String template, Map<String, Object> model) {
        return Decor.html(process(template, model), analysis.target());
    }

    private Page endpoint(String id) {
        var endpoint = analysis.find("endpoints", id);
        if (endpoint.isMissingNode()) return null;
        return new Page("/endpoints/" + id, "endpoint", Map.of("endpoint", endpoint, "flow", queries.traceFlow(id),
                "items", queries.knowledgeFor(List.of(id)), "descriptions", queries.descriptionsFor(List.of(id)),
                "errors", queries.errorsFor(id), "rejections", analysis.list("gatewayRejections")), "엔드포인트", Set.of(endpoint.path("service").asText()));
    }

    private Page topic(String name) {
        var topic = queries.findSubscribers(name);
        if (topic.publications().isEmpty() && topic.subscriptions().isEmpty()) return null;
        var nodes = Stream.concat(topic.publications().stream(), topic.subscriptions().stream()).toList();
        var links = topic.publications().stream()
                .flatMap(p -> queries.traceFlow(p.path("id").asText()).forward().stream().filter(s -> s.from().equals(p.path("id").asText())))
                .filter(s -> s.kind().equals("구독")).toList();
        var ids = nodes.stream().map(n -> n.path("id").asText()).toList();
        return new Page("/topics/" + name, "topic", Map.of("topic", topic, "links", links,
                "unpublished", topic.subscriptions().stream().map(queries::warning).filter(Objects::nonNull).toList(),
                "items", queries.knowledgeFor(ids), "descriptions", queries.descriptionsFor(ids)),
                "토픽", new TreeSet<>(nodes.stream().map(n -> n.path("service").asText()).toList()));
    }

    private Page service(String name) {
        var service = analysis.list("services").stream().filter(s -> name.equals(Analysis.value(s.path("name")))).findFirst().orElse(null);
        if (service == null) return null;
        var model = new HashMap<String, Object>(Map.of("service", service, "name", name));
        var ids = new ArrayList<String>();
        for (var key : List.of("endpoints", "httpCalls", "messagePublications", "messageSubscriptions", "environmentVariables", "externalResources",
                "errorCodes", "exceptionHandlers", "gatewayRejections", "pageSerialization", "unsupported")) {
            var owned = analysis.list(key).stream().filter(n -> n.path("service").asText().equals(name)).toList();
            model.put(key, owned);
            owned.forEach(n -> { if (n.has("id")) ids.add(n.path("id").asText()); });
        }
        model.put("items", queries.knowledgeFor(ids));
        return new Page("/services/" + name, "service", model, "서비스", Set.of(name));
    }

    private Page repository() {
        var repository = analysis.repository();
        if (repository.isMissingNode()) return null;
        return new Page("/repository", "repository", Map.of("repository", repository), "배포·모니터링", Set.of());
    }

    private Page revival() {
        var variables = new LinkedHashMap<String, List<JsonNode>>();
        analysis.list("environmentVariables").stream().filter(v -> !Analysis.known(v.path("value")))
                .forEach(v -> variables.computeIfAbsent(v.path("name").asText(), k -> new ArrayList<>()).add(v));
        var warnings = new ArrayList<WarningGroup>();
        analysis.get("warningCodes").properties().forEach(code -> warnings.add(new WarningGroup(code.getValue().path("title").asText(),
                code.getValue().path("action").asText(), analysis.list("warnings").stream().filter(w -> w.path("code").asText().equals(code.getKey()))
                .map(w -> w.path("reason").asText() + " " + view.evidence(w)).toList())));
        var repository = analysis.repository();
        return new Page(REVIVAL, "revival", Map.of(
                "resources", analysis.list("externalResources"),
                "variables", variables.entrySet().stream().map(e -> variable(e.getKey(), e.getValue())).toList(),
                "secrets", Analysis.listOf(repository.path("pipelines")).stream().flatMap(p -> Analysis.listOf(p.path("secrets")).stream()).toList(),
                "containers", Analysis.listOf(repository.path("containers")),
                "warnings", warnings,
                "traps", analysis.list("knowledge").stream().filter(k -> k.path("kind").asText().equals("함정")).toList(),
                "unsupported", analysis.list("unsupported")), "되살리기", Set.of());
    }

    private Variable variable(String name, List<JsonNode> uses) {
        return new Variable(name, distinct(uses, v -> view.status(v.path("value"))), distinct(uses, v -> v.path("service").asText()),
                distinct(uses, v -> v.path("profile").asText()), distinct(uses, v -> v.path("location").asText()),
                "[" + distinct(uses.stream().flatMap(v -> Analysis.listOf(v.path("value").path("evidence")).stream()).toList(), JsonNode::asText) + "]");
    }

    private static String distinct(List<JsonNode> nodes, java.util.function.Function<JsonNode, String> part) {
        return String.join(", ", nodes.stream().map(part).distinct().toList());
    }

    private String process(String template, Map<String, Object> model) {
        var context = new Context(Locale.KOREAN, model);
        context.setVariable("target", analysis.target());
        context.setVariable(ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME, evaluation);
        return engine.process(template, context);
    }

    private List<Section> render(Page page) {
        String html = process(page.template(), page.model());
        html = html.substring(html.indexOf("<h1"), html.lastIndexOf("</body>"));
        String title = plain(html.substring(0, html.indexOf("</h1>")));
        var sections = new ArrayList<Section>();
        var m = H2.matcher(html);
        int start = 0;
        String heading = "";
        while (m.find()) {
            sections.add(new Section(page.path(), title, heading, plain(html.substring(start, m.start())), page.kind(), page.services()));
            heading = plain(m.group(1));
            start = m.end();
        }
        sections.add(new Section(page.path(), title, heading, plain(html.substring(start)), page.kind(), page.services()));
        return sections;
    }

    /** Block ends become line breaks; a template line break is dropped at a line start and is a space between inline elements. */
    private static String plain(String html) {
        String text = html.replaceAll("\\s*\\n\\s*", "\u0001")
                .replaceAll("(?i)</(p|div|h1|h2|h3|li|ul|section)>", "\n")
                .replaceAll("<[^>]+>", "");
        return HtmlUtils.htmlUnescape(text).lines().map(l -> l.replaceFirst("^\u0001+", "").replace('\u0001', ' ').stripTrailing())
                .filter(l -> !l.isBlank()).reduce((x, y) -> x + "\n" + y).orElse("");
    }
}
