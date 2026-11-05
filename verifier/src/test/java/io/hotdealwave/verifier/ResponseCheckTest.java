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
        Example.verify(input, output);
        return JSON.readTree(output.toFile());
    }

    private JsonNode check(JsonNode analysis, String id) {
        return Checks.items(analysis.path("endpoints")).stream()
                .filter(e -> e.path("id").asText().equals(id)).findFirst().orElseThrow().path("responseCheck");
    }

    @Test void endpointCarriesStoredResponseStatusAndLines() throws Exception {
        var analysis = run(Example.WORKSPACE);
        String file = "responses/coupon-service.CouponController.getUserCouponDetail.json";
        var recorded = check(analysis, "coupon-service.CouponController.getUserCouponDetail");
        assertEquals("응답 기록 있음", recorded.path("status").asText());
        assertEquals(200, recorded.path("statusCode").asInt());
        assertEquals(file, recorded.path("file").asText());
        assertEquals(Files.readAllLines(Example.WORKSPACE.resolve(file)).get(17), recorded.path("lines").get(17).asText());

        var failed = check(analysis, "product-service.ReviewController.getReviewList");
        assertEquals("응답 기록 있음", failed.path("status").asText());
        assertEquals(500, failed.path("statusCode").asInt());

        var notCalled = check(analysis, "payment-service.PaymentController.readyPayment");
        assertEquals("호출 못 함", notCalled.path("status").asText());
        assertTrue(notCalled.path("statusCode").isNull());
        assertFalse(notCalled.path("reason").asText().isEmpty());
        assertEquals(11, analysis.at("/checks/kafka-topics/warningCount").asInt());
    }

    @Test void exampleWarningsSplitIntoValuesOutsideTheRepositoryAndLinksMissingInside() throws Exception {
        var analysis = run(Example.WORKSPACE);
        var codes = new java.util.TreeMap<String, Integer>();
        for (var warning : analysis.path("warnings")) codes.merge(warning.path("code").asText(), 1, Integer::sum);
        assertEquals(java.util.Map.of("outside-value", 9, "unlinked", 4), codes);
        assertFalse(analysis.at("/warningCodes/outside-value/action").asText().isEmpty());
        assertFalse(analysis.at("/confirmed/calls").asText().isEmpty());
    }

    @Test void storedResponsesAreComparedWithTheExtractedFields() throws Exception {
        var analysis = run(Example.WORKSPACE);
        var coupons = check(analysis, "coupon-service.CouponController.getUserCoupons");
        assertTrue(coupons.path("comparison").isArray());
        assertTrue(coupons.path("comparison").isEmpty(), coupons.path("comparison").toString());
        assertEquals("비교 보류: HTTP 500 (정상 응답과 비교하지 않음)",
                check(analysis, "product-service.ReviewController.getReviewList").path("comparison").get(0).asText());

        var service = JSON.readTree(Path.of("examples/hotdealwave/services/coupon-service.json").toFile());
        var endpoint = Checks.find(service.path("endpoints"), "id", "coupon-service.CouponController.getUserCoupons");
        var body = (ObjectNode) JSON.readTree(Example.WORKSPACE.resolve("responses/coupon-service.CouponController.getUserCoupons.json").toFile()).path("body");
        var coupon = (ObjectNode) body.path("data").get(0);
        coupon.set("isUsed", coupon.remove("used"));
        assertEquals(java.util.List.of("이름 다름: $.data[].isUsed ↔ $.data[].used"),
                ResponseComparison.compare(endpoint.path("response"), java.util.List.of(body), "$"));

        service = JSON.readTree(Path.of("examples/hotdealwave/services/company-service.json").toFile());
        endpoint = Checks.find(service.path("endpoints"), "id", "company-service.CompanyController.getCompanyList");
        body = (ObjectNode) JSON.readTree(Example.WORKSPACE.resolve("responses/company-service.CompanyController.getCompanyList.json").toFile()).path("body");
        var company = (ObjectNode) body.path("data").path("content").get(0);
        company.set("brand_name", company.remove("brandName"));
        company.put("logo", "x");
        assertEquals(java.util.List.of("이름 다름: $.data.content[].brand_name ↔ $.data.content[].brandName", "응답에만 있음: $.data.content[].logo"),
                ResponseComparison.compare(endpoint.path("response"), java.util.List.of(body), "$"));
    }

    @Test void responseEvidenceMustHoldTheFieldNameOnItsLine() throws Exception {
        Path workspace = temporary.resolve("workspace");
        Path services = Files.createDirectories(workspace.resolve("services"));
        Files.createDirectories(workspace.resolve("responses"));
        String id = "coupon-service.CouponController.getUserCoupons";
        Files.copy(Example.WORKSPACE.resolve("responses/" + id + ".json"), workspace.resolve("responses/" + id + ".json"));
        var service = (ObjectNode) JSON.readTree(Path.of("examples/hotdealwave/services/coupon-service.json").toFile());
        var endpoint = Checks.find(service.path("endpoints"), "id", id);
        var used = Checks.items(endpoint.at("/response/fields/1/schema/arguments/0/fields")).stream()
                .filter(f -> f.path("name").asText().equals("isUsed")).map(f -> (ObjectNode) f).findFirst().orElseThrow();
        String usedLine = "responses/" + id + ".json:19", otherLine = "responses/" + id + ".json:12";
        for (var node : new ObjectNode[]{used, (ObjectNode) used.path("jsonName"), (ObjectNode) used.path("schema")})
            node.putArray("evidence").add(usedLine);
        JSON.writeValue(services.resolve("coupon-service.json").toFile(), service);
        Files.writeString(workspace.resolve("relations.json"), "{\"schemaVersion\":1,\"kafkaLinks\":[],\"calls\":[],\"throws\":[]}");
        Files.copy(Example.WORKSPACE.resolve("target.json"), workspace.resolve("target.json"));
        var field = Checks.find(run(workspace).path("endpoints"), "id", id).at("/response/fields/1/schema/arguments/0/fields/5");
        assertEquals(Checks.CONFIRMED, field.path("jsonName").path("status").asText());

        for (var node : new ObjectNode[]{used, (ObjectNode) used.path("jsonName"), (ObjectNode) used.path("schema")})
            node.putArray("evidence").add(otherLine);
        JSON.writeValue(services.resolve("coupon-service.json").toFile(), service);
        var analysis = run(workspace);
        field = Checks.find(analysis.path("endpoints"), "id", id).at("/response/fields/1/schema/arguments/0/fields/5");
        assertEquals(Checks.UNKNOWN, field.path("jsonName").path("status").asText());
        assertEquals(Checks.UNKNOWN, field.path("schema").path("status").asText());
        assertTrue(analysis.path("warnings").toString().contains(otherLine));

        ((ObjectNode) used.path("jsonName")).putNull("value");
        JSON.writeValue(services.resolve("coupon-service.json").toFile(), service);
        field = Checks.find(run(workspace).path("endpoints"), "id", id).at("/response/fields/1/schema/arguments/0/fields/5");
        assertEquals(Checks.UNKNOWN, field.path("status").asText());
    }

    @Test void endpointWithoutStoredResponseIsCodeOnly() throws Exception {
        Path services = Files.createDirectories(temporary.resolve("hotdealwave/services"));
        Files.copy(Path.of("examples/hotdealwave/services/order-service.json"), services.resolve("order-service.json"));
        Files.writeString(services.resolveSibling("relations.json"), "{\"schemaVersion\":1,\"kafkaLinks\":[],\"calls\":[],\"throws\":[]}");
        Files.copy(Path.of("examples/hotdealwave/target.json"), services.resolveSibling("target.json"));
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
        var service = (ObjectNode) JSON.readTree(Path.of("examples/hotdealwave/services/order-service.json").toFile());
        ((ObjectNode) service.path("endpoints").get(0)).put("id", "..\\..\\outside");
        JSON.writeValue(services.resolve("order-service.json").toFile(), service);
        Files.writeString(services.resolveSibling("relations.json"), "{\"schemaVersion\":1,\"kafkaLinks\":[],\"calls\":[],\"throws\":[]}");
        Files.copy(Path.of("examples/hotdealwave/target.json"), services.resolveSibling("target.json"));
        Files.writeString(temporary.resolve("outside.json"), "{\"statusCode\":200,\"reason\":null}");
        var analysis = run(services.getParent());
        assertEquals("코드만", check(analysis, "..\\..\\outside").path("status").asText());
    }
}
