package io.hotdealwave.server;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpTest {
    @LocalServerPort int port;
    McpSyncClient client;

    @BeforeEach void connect() {
        client = McpClient.sync(HttpClientStreamableHttpTransport.builder("http://localhost:" + port).endpoint("/mcp").build()).build();
        client.initialize();
    }

    @AfterEach void close() { client.closeGracefully(); }

    private McpSchema.CallToolResult call(String tool, String key, String value) {
        return client.callTool(new McpSchema.CallToolRequest(tool, Map.of(key, value)));
    }

    private static String text(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }

    @Test void listsFiveTools() {
        var names = client.listTools().tools().stream().map(McpSchema.Tool::name).sorted().toList();
        assertEquals(java.util.List.of("find_endpoint", "find_subscribers", "get_page", "search_docs", "trace_flow"), names);
    }

    @Test void toolsReturnBodyTextWithStatusAndEvidence() {
        String endpoint = text(call("find_endpoint", "query", "POST /api/v1/orders"));
        assertTrue(endpoint.contains("order-service.OrderController.createOrder"));
        assertTrue(endpoint.contains("order/src/main/java/com/sparta/hotdeal/order/presentation/controller/OrderController.java:40"));

        String flow = text(call("trace_flow", "id", "order-service.OrderController.createOrder"));
        assertTrue(flow.contains("order-service.requestProduct → (구독) 확인 안 됨 — 구독자 확인 안 됨: 같은 환경변수(KAFKA_REQUEST_PRODUCT_TOPIC)를 쓰는 구독을 찾지 못함. 토픽 실제 값은 저장소 밖 환경에서 정해짐"));

        assertTrue(text(call("find_subscribers", "topic", "KAFKA_DLQ_TOPIC")).contains("토픽 실제 값: 저장소에 근거 없음"));
        assertTrue(text(call("search_docs", "query", "createOrder")).contains("/endpoints/order-service.OrderController.createOrder"));
        assertTrue(text(call("get_page", "page", "/services/payment-service")).contains("검증 범위: 엔드포인트 9개 중"));
    }

    @Test void unknownInputIsAnError() {
        var result = call("find_endpoint", "query", "GET /api/v1/none");
        assertTrue(result.isError());
        assertTrue(text(result).contains("찾지 못함"));
    }
}
