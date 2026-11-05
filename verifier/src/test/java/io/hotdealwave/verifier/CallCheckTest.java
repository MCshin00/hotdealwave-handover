package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CallCheckTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    @TempDir Path temporary;

    private JsonNode run(Path input) throws Exception {
        Path output = temporary.resolve("analysis.json");
        Example.verify(input, output);
        return JSON.readTree(output.toFile());
    }

    private static List<String> confirmedTargets(JsonNode analysis, String fromId) {
        return Checks.items(analysis.path("calls")).stream()
                .filter(c -> c.path("fromId").asText().equals(fromId) && Checks.known(c))
                .map(c -> c.path("toId").asText()).toList();
    }

    private static List<String> warnings(JsonNode analysis) {
        return Checks.items(analysis.path("warnings")).stream()
                .filter(w -> w.path("check").asText().equals("handler-calls"))
                .map(w -> w.path("reason").asText()).toList();
    }

    @Test void handlerCallsReachFeignCallsAndPublications() throws Exception {
        var analysis = run(Example.WORKSPACE);
        assertTrue(confirmedTargets(analysis, "order-service.OrderController.createOrder").containsAll(List.of(
                "order-service.UserClient.getAddress", "order-service.ProductClient.getProductList",
                "order-service.CouponClient.validateCoupon", "order-service.CouponClient.useCoupon",
                "order-service.requestProduct")));
        assertTrue(confirmedTargets(analysis, "order-service.OrderController.cancelOrder").containsAll(List.of(
                "order-service.ProductClient.reduceQuantity", "order-service.cancelPayment")));
        assertTrue(confirmedTargets(analysis, "product-service.reduce").contains("product-service.requestPayment"));

        var reasons = warnings(analysis);
        assertTrue(reasons.stream().anyMatch(r -> r.contains("order-service.CouponClient.recoverCoupon")));
        assertTrue(reasons.stream().anyMatch(r -> r.contains("payment-service.OrderClient.updateOrderStatus")));
        assertEquals(9, analysis.path("messageLinks").size());
    }

    @Test void callsReachingAnotherTargetAreUnknown() throws Exception {
        Path input = Files.createDirectories(temporary.resolve("hotdealwave"));
        Path services = Files.createDirectories(input.resolve("services"));
        try (var files = Files.list(Path.of("examples/hotdealwave/services"))) {
            for (var file : files.toList()) Files.copy(file, services.resolve(file.getFileName()));
        }
        var relations = (ObjectNode) JSON.readTree(Path.of("examples/hotdealwave/relations.json").toFile());
        var call = (ObjectNode) Checks.items(relations.path("calls")).stream()
                .filter(c -> c.path("fromId").asText().equals("order-service.OrderController.createOrder")
                        && c.path("toId").asText().equals("order-service.CouponClient.useCoupon"))
                .findFirst().orElseThrow();
        var evidence = call.withArray("evidence");
        evidence.set(evidence.size() - 1, JSON.getNodeFactory().textNode(
                "order/src/main/java/com/sparta/hotdeal/order/infrastructure/adapter/UserClientAdapter.java:19"));
        // The order publications share one send line; only the per-publication call line tells them apart.
        Checks.items(relations.path("calls")).stream()
                .filter(c -> c.path("fromId").asText().equals("order-service.OrderController.createOrder")
                        && c.path("toId").asText().equals("order-service.requestProduct"))
                .forEach(c -> ((ObjectNode) c).put("toId", "order-service.cancelPayment"));
        // A publication chain must end at the send line, not at another line of that publication's evidence.
        Checks.items(relations.path("calls")).stream()
                .filter(c -> c.path("fromId").asText().equals("order-service.OrderController.cancelOrder")
                        && c.path("toId").asText().equals("order-service.cancelPayment"))
                .forEach(c -> {
                    var chain = ((ObjectNode) c).withArray("evidence");
                    chain.set(chain.size() - 1, JSON.getNodeFactory().textNode(
                            "order/src/main/java/com/sparta/hotdeal/order/event/producer/OrderEventProducer.java:26"));
                });
        JSON.writeValue(input.resolve("relations.json").toFile(), relations);
        Files.copy(Path.of("examples/hotdealwave/target.json"), input.resolve("target.json"));

        var analysis = run(input);
        var checked = Checks.items(analysis.path("calls")).stream()
                .filter(c -> c.path("toId").asText().equals("order-service.CouponClient.useCoupon")
                        && c.path("fromId").asText().equals("order-service.OrderController.createOrder"))
                .findFirst().orElseThrow();
        assertEquals(Checks.UNKNOWN, checked.path("status").asText());
        assertTrue(warnings(analysis).stream().anyMatch(r -> r.contains("order-service.CouponClient.useCoupon")));
        assertFalse(confirmedTargets(analysis, "order-service.OrderController.createOrder").contains("order-service.cancelPayment"));
        assertFalse(confirmedTargets(analysis, "order-service.OrderController.cancelOrder").contains("order-service.cancelPayment"));
    }
}
