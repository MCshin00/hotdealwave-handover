package io.hotdealwave.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import io.hotdealwave.extractor.rest.DtoExtractor;
import io.hotdealwave.extractor.scan.ServiceScanner;
import io.hotdealwave.extractor.source.SourceContext;
import java.nio.file.Path;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExtractorTest {
    private static JsonNode analysis;
    private static final String ORDER = "order/src/main/java/com/sparta/hotdeal/order/";
    private static final String GATEWAY = "gateway/src/main/";

    @BeforeAll static void extract() throws Exception {
        analysis = new Extractor().analyze(Path.of("target/HotDealWave"));
    }

    static JsonNode find(JsonNode nodes, String key, String value) {
        return StreamSupport.stream(nodes.spliterator(), false)
                .filter(n -> n.path(key).asText().equals(value)).findFirst().orElseThrow();
    }

    static JsonNode endpoint(String service, String method, String path) {
        return StreamSupport.stream(analysis.path("endpoints").spliterator(), false)
                .filter(n -> n.path("service").asText().equals(service)
                        && n.path("httpMethod").path("value").asText().equals(method)
                        && n.path("path").path("value").asText().equals(path))
                .findFirst().orElseThrow();
    }

    @Test void orderRequestAndGenericResponseComeFromSource() {
        JsonNode e = endpoint("order-service", "POST", "/api/v1/orders");
        assertTrue(e.path("evidence").toString().contains("OrderController.java:40"));
        assertEquals(1, e.path("parameters").size()); // AuthenticationPrincipal is not client input.
        JsonNode p = e.path("parameters").get(0);
        assertEquals("본문", p.path("kind").asText());
        JsonNode request = p.path("schema");
        assertTrue(request.path("evidence").toString().contains(ORDER + "application/dtos/order/req/ReqPostOrderDto.java:15"));
        assertEquals(3, request.path("fields").size());
        JsonNode baskets = find(request.path("fields"), "name", "basketList");
        assertEquals("List<UUID>", baskets.path("schema").path("type").asText());
        assertEquals("NotNull", baskets.path("annotations").get(0).path("name").asText());
        JsonNode response = e.path("response");
        assertEquals("ResponseDto<ResPostOrderDto>", response.path("type").asText());
        JsonNode data = find(response.path("fields"), "name", "data").path("schema");
        assertEquals("ResPostOrderDto", data.path("type").asText());
        assertEquals("orderId", data.path("fields").get(0).path("name").asText());
        JsonNode companyQuery = endpoint("company-service", "GET", "/api/v1/companies");
        assertEquals("쿼리", find(companyQuery.path("parameters"), "name", "pageable").path("kind").asText());
        JsonNode signup = endpoint("user-service", "POST", "/api/v1/auth/signup");
        JsonNode password = find(signup.path("parameters").get(0).path("schema").path("fields"), "name", "password");
        JsonNode size = find(password.path("annotations"), "name", "Size");
        assertEquals("8", size.path("arguments").path("min").path("value").asText());
        assertEquals("15", size.path("arguments").path("max").path("value").asText());
    }

    @Test void couponBooleanJsonNameComesFromLombokGetter() {
        JsonNode response = endpoint("coupon-service", "GET", "/api/v1/coupons/{couponId}").path("response");
        JsonNode dto = find(response.path("fields"), "name", "data").path("schema");
        assertEquals("ResGetUserCouponsDto", dto.path("type").asText());
        JsonNode field = find(dto.path("fields"), "name", "isUsed");
        assertEquals("isUsed", field.path("name").asText());
        JsonNode jsonName = field.path("jsonName");
        assertEquals("used", jsonName.path("value").asText());
        assertEquals("확인됨", jsonName.path("status").asText());
        String file = "coupon/src/main/java/com/sparta/hotdeal/coupon/application/dto/res/ResGetUserCouponsDto.java";
        assertTrue(jsonName.path("evidence").toString().contains(file + ":11"));
        assertTrue(jsonName.path("evidence").toString().contains(file + ":21"));
    }

    @Test void feignUsesHttpPathInsteadOfJavaMethodName() {
        JsonNode call = StreamSupport.stream(analysis.path("feignCalls").spliterator(), false)
                .filter(n -> n.path("evidence").toString().contains(ORDER + "infrastructure/client/ProductClient.java:26"))
                .findFirst().orElseThrow();
        assertEquals("reduceQuantity", call.path("handler").asText());
        assertEquals("확인됨", call.path("target").path("status").asText());
        JsonNode target = find(analysis.path("endpoints"), "id", call.path("target").path("value").asText());
        assertEquals("restoreQuantity", target.path("handler").asText());
        assertEquals("PUT", target.path("httpMethod").path("value").asText());
        assertEquals("/api/v1/products/restore-quantity", target.path("path").path("value").asText());
        assertTrue(call.path("target").path("evidence").toString().contains("ProductController.java:112"));
    }

    @Test void paymentJsonNamingRemainsUnknownDespiteLombokGetter() throws Exception {
        Path root = Path.of("target/HotDealWave").toAbsolutePath().normalize();
        var payment = new ServiceScanner().scan(root).stream()
                .filter(s -> s.model().directory().equals("payment")).findFirst().orElseThrow();
        var source = new SourceContext(root, payment);
        var client = source.types.get("com.sparta.hotdeal.payment.infrastructure.client.KakaoPayClient");
        var response = new DtoExtractor(source).extract(client.getMethodsByName("cancel").get(0).getType());
        var jsonName = response.fields.stream().filter(f -> f.name().equals("partnerOrderId"))
                .findFirst().orElseThrow().jsonName();
        assertEquals("확인 안 됨", jsonName.status());
        assertNull(jsonName.value());
        assertTrue(jsonName.reason().contains("@JsonNaming"));
        String file = "payment/src/main/java/com/sparta/hotdeal/payment/infrastructure/dto/kakaopay/res/ResPostKakaoPayCancelDto.java";
        assertTrue(jsonName.evidence().contains(file + ":11"));
        assertTrue(jsonName.evidence().contains(file + ":17"));
    }

    @Test void explicitFeignUrlHasNoRepositoryTarget() {
        JsonNode client = find(analysis.path("feignClients"), "type", "KakaoPayClient");
        assertEquals("https://open-api.kakaopay.com/online/v1/payment", client.path("url").path("value").asText());
        assertTrue(client.path("evidence").toString().contains("KakaoPayClient.java:14"));
        var calls = StreamSupport.stream(analysis.path("feignCalls").spliterator(), false)
                .filter(n -> n.path("clientId").asText().equals(client.path("id").asText())).toList();
        assertEquals(3, calls.size());
        calls.forEach(n -> {
            assertTrue(n.path("external").asBoolean());
            assertEquals("저장소에 근거 없음", n.path("target").path("status").asText());
        });
    }

    @Test void gatewayOrderRuleAndTokenEvidenceArePreserved() {
        JsonNode route = find(analysis.path("gatewayRoutes"), "id", "order-service");
        assertEquals("order-service", route.path("targetService").path("value").asText());
        assertEquals(3, route.path("paths").size());
        assertEquals("/api/v1/orders/**", route.path("paths").get(0).path("value").asText());
        assertEquals("/order-service/(?<segment>.*), /$\\{segment}", route.path("rewritePath").path("value").asText());
        assertTrue(route.path("rewritePath").path("evidence").toString().contains(GATEWAY + "resources/application.yml:47"));
        JsonNode order = endpoint("order-service", "POST", "/api/v1/orders");
        assertTrue(order.path("tokenRequired").path("value").asBoolean());
        assertTrue(order.path("tokenRequired").path("evidence").toString().contains("AllowedPath.java:"));
        assertEquals("/api/v1/orders", order.path("externalPaths").get(0).path("path").asText());
        assertEquals("/order-service/api/v1/orders", order.path("externalPaths").get(1).path("path").asText());
        JsonNode product = endpoint("product-service", "GET", "/api/v1/products/{productId}");
        assertFalse(product.path("tokenRequired").path("value").asBoolean());
        assertTrue(product.path("tokenRequired").path("evidence").toString().contains("AllowedPath.java:10"));
        assertEquals("확인 안 됨", product.path("externalPaths").get(1).path("tokenRequired").path("status").asText());
    }

    @Test void independentDeclarationInventoryAndThreeChecksAreWritten() {
        assertEquals(103, analysis.path("checks").path("declarations").path("mappingCount").asInt());
        assertEquals(9, analysis.path("checks").path("declarations").path("feignClientCount").asInt());
        assertEquals(6, analysis.path("gatewayRoutes").size());
        assertTrue(analysis.path("checks").has("feign-targets"));
        assertTrue(analysis.path("checks").has("gateway-routes"));
        assertTrue(analysis.path("warnings").isArray());
    }
}
