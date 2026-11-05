package io.hotdealwave.server;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QueriesTest {
    private static Queries queries;

    @BeforeAll static void load() throws Exception {
        queries = new Queries(new Analysis("examples/hotdealwave/out/analysis.json"));
    }

    private static boolean has(List<Queries.Step> steps, String from, String to, String status) {
        return steps.stream().anyMatch(s -> s.from().equals(from) && java.util.Objects.equals(s.to(), to) && s.status().equals(status));
    }

    @Test void findEndpointByMethodAndPath() {
        var found = queries.findEndpoint("POST /api/v1/orders");
        assertEquals(1, found.size());
        var endpoint = found.get(0);
        assertEquals("order-service.OrderController.createOrder", endpoint.path("id").asText());
        assertEquals("basketList", endpoint.at("/parameters/0/schema/fields/0/name").asText());
        assertEquals("ResponseDto<ResPostOrderDto>", endpoint.at("/response/type").asText());
    }

    @Test void traceFlowFollowsCallsTargetsAndStopsAtUnlinkedPublication() {
        String createOrder = "order-service.OrderController.createOrder";
        var flow = queries.traceFlow(createOrder);
        assertTrue(has(flow.forward(), createOrder, "order-service.UserClient.getAddress", "확인됨"));
        assertTrue(has(flow.forward(), "order-service.UserClient.getAddress", "user-service.AddressController.getAddress", "확인됨"));
        assertTrue(has(flow.forward(), "order-service.CouponClient.useCoupon", "coupon-service.CouponController.useCoupon", "확인됨"));
        assertTrue(has(flow.forward(), createOrder, "order-service.requestProduct", "확인됨"));
        var stop = flow.forward().stream().filter(s -> s.from().equals("order-service.requestProduct")).findFirst().orElseThrow();
        assertNull(stop.to());
        assertEquals("확인 안 됨", stop.status());
        assertTrue(stop.reason().contains("구독자 확인 안 됨"));

        var cancel = queries.traceFlow("order-service.OrderController.cancelOrder");
        assertTrue(has(cancel.forward(), "order-service.cancelPayment", "payment-service.cancel", "확인됨"));

        var callers = queries.traceFlow("user-service.AddressController.getAddress").callers();
        assertTrue(has(callers, createOrder, "order-service.UserClient.getAddress", "확인됨"));
    }

    @Test void findSubscribersByEnvironmentName() {
        var topic = queries.findSubscribers("KAFKA_DLQ_TOPIC");
        assertEquals(List.of("order-service.publishDlq", "order-service.recoverDlq", "payment-service.publishDlq"),
                topic.publications().stream().map(p -> p.path("id").asText()).toList());
        assertEquals(List.of("order-service.dlq", "payment-service.dlq"),
                topic.subscriptions().stream().map(s -> s.path("id").asText()).toList());
    }
}
