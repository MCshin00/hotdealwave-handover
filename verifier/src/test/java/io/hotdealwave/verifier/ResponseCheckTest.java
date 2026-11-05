package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ResponseCheckTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    @TempDir Path temporary;

    private JsonNode run(Path input) throws Exception {
        Path output = temporary.resolve("analysis.json");
        Main.main(new String[]{input.toString(), "target/HotDealWave", output.toString()});
        return JSON.readTree(output.toFile());
    }

    private JsonNode check(JsonNode analysis, String id) {
        return Checks.items(analysis.path("endpoints")).stream()
                .filter(e -> e.path("id").asText().equals(id)).findFirst().orElseThrow().path("responseCheck");
    }

    @Test void endpointCarriesStoredResponseStatusAndLines() throws Exception {
        var analysis = run(Path.of("analysis/hotdealwave"));
        String file = "analysis/hotdealwave/responses/coupon-service.CouponController.getUserCouponDetail.json";
        var recorded = check(analysis, "coupon-service.CouponController.getUserCouponDetail");
        assertEquals("응답 기록 있음", recorded.path("status").asText());
        assertEquals(200, recorded.path("statusCode").asInt());
        assertEquals(file, recorded.path("file").asText());
        assertEquals(Files.readAllLines(Path.of(file)).get(17), recorded.path("lines").get(17).asText());

        var failed = check(analysis, "product-service.ReviewController.getReviewList");
        assertEquals("응답 기록 있음", failed.path("status").asText());
        assertEquals(500, failed.path("statusCode").asInt());

        var notCalled = check(analysis, "payment-service.PaymentController.readyPayment");
        assertEquals("호출 못 함", notCalled.path("status").asText());
        assertTrue(notCalled.path("statusCode").isNull());
        assertFalse(notCalled.path("reason").asText().isEmpty());
        assertEquals(11, analysis.at("/checks/kafka-topics/warningCount").asInt());
    }

    @Test void endpointWithoutStoredResponseIsCodeOnly() throws Exception {
        Path services = Files.createDirectories(temporary.resolve("hotdealwave/services"));
        Files.copy(Path.of("analysis/hotdealwave/services/order-service.json"), services.resolve("order-service.json"));
        Files.writeString(services.resolveSibling("relations.json"), "{\"schemaVersion\":1,\"kafkaLinks\":[],\"calls\":[],\"throws\":[]}");
        var analysis = run(services.getParent());
        assertFalse(analysis.path("endpoints").isEmpty());
        for (var endpoint : analysis.path("endpoints")) {
            assertEquals("코드만", endpoint.path("responseCheck").path("status").asText());
            assertTrue(endpoint.path("responseCheck").path("file").isNull());
            assertTrue(endpoint.path("responseCheck").path("statusCode").isNull());
            assertTrue(endpoint.path("responseCheck").path("reason").isNull());
            assertTrue(endpoint.path("responseCheck").path("lines").isArray());
            assertTrue(endpoint.path("responseCheck").path("lines").isEmpty());
        }
    }

    @Test void responseOutsideResponsesDirectoryIsNotRead() throws Exception {
        Path services = Files.createDirectories(temporary.resolve("hotdealwave/services"));
        Files.createDirectories(temporary.resolve("hotdealwave/responses"));
        var service = (ObjectNode) JSON.readTree(Path.of("analysis/hotdealwave/services/order-service.json").toFile());
        ((ObjectNode) service.path("endpoints").get(0)).put("id", "..\\..\\outside");
        JSON.writeValue(services.resolve("order-service.json").toFile(), service);
        Files.writeString(services.resolveSibling("relations.json"), "{\"schemaVersion\":1,\"kafkaLinks\":[],\"calls\":[],\"throws\":[]}");
        Files.writeString(temporary.resolve("outside.json"), "{\"statusCode\":200,\"reason\":null}");
        var analysis = run(services.getParent());
        assertEquals("코드만", check(analysis, "..\\..\\outside").path("status").asText());
    }
}
