package io.hotdealwave.server;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The query layer as MCP tools at /mcp. Results are the page body text, so status and evidence travel with each value. */
@Configuration
class Mcp {
    private final Analysis analysis;
    private final Queries queries;
    private final Pages pages;
    private final Search search;
    private final View view;
    private final Legend legend;

    Mcp(Analysis analysis, Queries queries, Pages pages, Search search, View view, Legend legend) {
        this.analysis = analysis; this.queries = queries; this.pages = pages; this.search = search; this.view = view; this.legend = legend;
    }

    @Bean HttpServletStatelessServerTransport mcpTransport() {
        return HttpServletStatelessServerTransport.builder().messageEndpoint("/mcp").build();
    }

    @Bean ServletRegistrationBean<HttpServletStatelessServerTransport> mcpServlet(HttpServletStatelessServerTransport transport) {
        return new ServletRegistrationBean<>(transport, "/mcp");
    }

    @Bean(destroyMethod = "close") McpStatelessSyncServer mcpServer(HttpServletStatelessServerTransport transport) {
        var target = analysis.target();
        return McpServer.sync(transport).serverInfo("hotdealwave-handover", "1")
                .instructions(target.name() + " 저장소의 고정 커밋 " + target.shortCommit() + "을 분석한 문서다. "
                        + "도구 결과의 값마다 확인 상태(확인됨, 확인 안 됨, 저장소에 근거 없음)와 이유, 근거 파일:줄이 붙는다. "
                        + "결과 머리의 \"확인됨의 뜻\"은 확인됨이 무엇을 확인했는지 밝힌다. "
                        + "답할 때 확인되지 않은 값은 그 상태와 이유를 함께 밝힌다.")
                .capabilities(ServerCapabilities.builder().tools(false).build())
                .tools(
                        tool("find_endpoint", "경로(\"POST /api/v1/orders\"), ID, 핸들러 이름으로 엔드포인트를 찾아 본문(요청·응답 필드, 호출, 검증 범위, 업무 규칙·함정)을 낸다.",
                                "query", "엔드포인트 ID, \"메서드 경로\", 경로 또는 핸들러 이름", this::findEndpoint),
                        tool("trace_flow", "엔드포인트·Kafka 발행·구독 ID에서 Feign 호출과 Kafka 발행·구독을 따라간 단계와 부르는 곳을 낸다. 이어지지 않는 곳은 상태와 이유에서 멈춘다.",
                                "id", "엔드포인트, Kafka 발행 또는 구독 ID", this::traceFlow),
                        tool("find_subscribers", "토픽의 발행·구독과 그 연결을 담은 토픽 본문을 낸다.",
                                "topic", "토픽 환경변수 이름 또는 코드에 적힌 토픽 이름", name -> page("/topics/" + name)),
                        tool("search_docs", "엔드포인트·토픽·서비스·배포·모니터링 본문을 키워드로 찾아 상위 5개 절(페이지 주소, 절 제목, 점수, 미리보기)을 낸다. 본문 전체는 get_page로 읽는다.",
                                "query", "찾을 말. 서비스 이름(order-service 등)이나 API·토픽·설정 같은 말이 있으면 그 범위를 먼저 찾는다", this::searchDocs),
                        tool("get_page", "페이지 주소(/endpoints/ID, /topics/이름, /services/이름, /repository)로 본문 전체를 낸다.",
                                "page", "search_docs가 낸 페이지 주소", this::page))
                .build();
    }

    private SyncToolSpecification tool(String name, String description, String argument, String argumentDescription, Function<String, String> body) {
        var schema = Map.<String, Object>of("type", "object", "required", List.of(argument),
                "properties", Map.of(argument, Map.of("type", "string", "description", argumentDescription)));
        return new SyncToolSpecification(Tool.builder().name(name).description(description).inputSchema(schema).build(), (context, request) -> {
            String text = body.apply(String.valueOf(request.arguments().get(argument)));
            if (text == null) return CallToolResult.builder().addTextContent("찾지 못함: " + request.arguments().get(argument)).isError(true).build();
            return CallToolResult.builder().addTextContent(text).build();
        });
    }

    private String page(String path) {
        String text = pages.text(path);
        var page = pages.page(path);
        return text == null ? null : legend.text(page.kind()) + text;
    }

    private String findEndpoint(String query) {
        var found = queries.findEndpoint(query);
        return found.isEmpty() ? null : legend.text("엔드포인트")
                + String.join("\n\n---\n\n", found.stream().map(e -> pages.text("/endpoints/" + e.path("id").asText())).toList());
    }

    private String traceFlow(String id) {
        if (Stream.of("endpoints", "kafkaPublications", "kafkaSubscriptions").allMatch(key -> analysis.find(key, id).isMissingNode())) return null;
        var flow = queries.traceFlow(id);
        var lines = new ArrayList<String>();
        lines.add("나가는 호출과 발행");
        lines.addAll(flow.forward().isEmpty() ? List.of("확인된 호출 없음") : flow.forward().stream().map(view::step).toList());
        lines.add("부르는 곳");
        lines.addAll(flow.callers().isEmpty() ? List.of("확인된 호출 없음") : flow.callers().stream().map(view::step).toList());
        var services = new TreeSet<String>();
        for (var step : Stream.concat(flow.forward().stream(), flow.callers().stream()).toList())
            for (var node : Arrays.asList(step.from(), step.to())) if (node != null && node.contains(".")) services.add(node.substring(0, node.indexOf('.')));
        if (services.isEmpty()) services.add(id.substring(0, Math.max(0, id.indexOf('.'))));
        var unsupported = analysis.list("unsupported").stream().filter(u -> services.contains(u.path("service").asText())).toList();
        if (!unsupported.isEmpty()) lines.add("도구가 다루지 않는 방식 " + unsupported.size() + "곳: " + String.join(", ", unsupported.stream()
                .map(u -> u.path("service").asText() + " " + u.path("mechanism").asText() + " " + view.evidence(u)).toList())
                + ". 이 흐름에는 그 호출이 빠져 있을 수 있다.");
        return legend.text("흐름") + String.join("\n", lines);
    }

    private String searchDocs(String query) {
        var results = search.search(query);
        if (results.isEmpty()) return null;
        var text = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            var r = results.get(i);
            text.append(i + 1).append(". ").append(r.title()).append(r.section().isEmpty() ? "" : " — " + r.section()).append('\n')
                    .append("페이지: ").append(r.page()).append(" · 점수 ").append(r.score()).append('\n')
                    .append(r.preview()).append("\n\n");
        }
        String[] kinds = results.stream().map(r -> pages.page(r.page()).kind()).distinct().toArray(String[]::new);
        return legend.text(kinds) + text.toString().strip();
    }
}
