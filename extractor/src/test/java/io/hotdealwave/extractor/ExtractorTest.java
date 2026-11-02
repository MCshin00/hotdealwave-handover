package io.hotdealwave.extractor;

import com.fasterxml.jackson.databind.JsonNode;
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
        JsonNode product = endpoint("product-service", "GET", "/api/v1/products/{productId}");
        assertFalse(product.path("tokenRequired").path("value").asBoolean());
        assertTrue(product.path("tokenRequired").path("evidence").toString().contains("AllowedPath.java:10"));
    }

    @Test void parserWritesExtractedDeclarationsWithoutCrossChecks() {
        assertEquals(102, analysis.path("declarations").size());
        assertEquals(9, analysis.path("feignClients").size());
        assertEquals(6, analysis.path("gatewayRoutes").size());
        assertTrue(analysis.path("checks").isEmpty());
        assertTrue(analysis.path("warnings").isEmpty());
    }

    static JsonNode withEvidence(JsonNode nodes, String suffix) {
        return StreamSupport.stream(nodes.spliterator(), false)
                .filter(n -> n.path("evidence").toString().contains(suffix)).findFirst().orElseThrow();
    }

    @Test void kafkaLinksRequireTheSameComposeEnvironmentName() {
        JsonNode pub = withEvidence(analysis.path("kafkaPublications"), "PaymentEventProducer.java:23");
        JsonNode sub = withEvidence(analysis.path("kafkaSubscriptions"), "OrderStatusUpdateEventConsumer.java:25");
        assertEquals("spring.kafka.topics.update-order-status", pub.path("topicKey").path("value").asText());
        assertEquals("KAFKA_UPDATE_ORDER_STATUS_TOPIC", pub.path("environment").path("value").asText());
        JsonNode link = StreamSupport.stream(analysis.path("kafkaLinks").spliterator(), false)
                .filter(n -> n.path("publicationId").asText().equals(pub.path("id").asText())
                        && n.path("subscriptionId").asText().equals(sub.path("id").asText())).findFirst().orElseThrow();
        assertEquals("확인됨", link.path("status").asText());
        for (String line : new String[] {"docker-compose.yml:196", "docker-compose.yml:157"})
            assertTrue(link.path("evidence").toString().contains(line));
        assertTrue(pub.path("evidence").toString().contains("payment/src/main/resources/application.yml:18"));
        assertTrue(sub.path("evidence").toString().contains("order/src/main/resources/application.yml:17"));

        JsonNode request = withEvidence(analysis.path("kafkaPublications"), "OrderEventProducer.java:37");
        JsonNode reduce = withEvidence(analysis.path("kafkaSubscriptions"), "ProductKafkaConsumer.java:29");
        assertEquals("KAFKA_REQUEST_PRODUCT_TOPIC", request.path("environment").path("value").asText());
        assertEquals("KAFKA_REDUCE_QUANTITY_TOPIC", reduce.path("environment").path("value").asText());
        assertFalse(StreamSupport.stream(analysis.path("kafkaLinks").spliterator(), false)
                .anyMatch(n -> n.path("publicationId").asText().equals(request.path("id").asText())
                        || n.path("subscriptionId").asText().equals(reduce.path("id").asText())));
    }

    @Test void kafkaArgumentsWrappersAndDlqKeepTheWholeEvidenceChain() {
        JsonNode order = withEvidence(analysis.path("kafkaPublications"), "OrderEventProducer.java:37");
        assertTrue(order.path("evidence").toString().contains("OrderEventProducer.java:62"));
        JsonNode product = withEvidence(analysis.path("kafkaPublications"), "ProductKafkaConsumer.java:48");
        for (String line : new String[] {"ProductKafkaConsumer.java:85", "ProductInventoryService.java:125",
                "ProductKafkaProducer.java:14", "product/src/main/resources/application.yml:17", "docker-compose.yml:89"})
            assertTrue(product.path("evidence").toString().contains(line), line);
        assertEquals("KAFKA_REQUEST_ORDER_TOPIC", product.path("environment").path("value").asText());
        JsonNode dlq = withEvidence(analysis.path("kafkaPublications"), ORDER + "infrastructure/config/ConsumerApplicationKafkaConfig.java:61");
        assertEquals("DeadLetterPublishingRecoverer", dlq.path("kind").asText());
        assertEquals("spring.kafka.topics.dlq", dlq.path("topicKey").path("value").asText());
        assertTrue(dlq.path("evidence").toString().contains("ConsumerApplicationKafkaConfig.java:30"));
        for (String service : new String[] {"payment", "product"}) {
            String line = service + "/src/main/java/com/sparta/hotdeal/" + service
                    + "/infrastructure/config/ConsumerApplicationKafkaConfig.java:61";
            JsonNode literalDlq = withEvidence(analysis.path("kafkaPublications"), line);
            assertEquals(service + "-dlq", literalDlq.path("topicName").path("value").asText());
            assertEquals("확인됨", literalDlq.path("topicName").path("status").asText());
            assertTrue(literalDlq.path("topicName").path("evidence").toString().contains(line));
            assertEquals("확인됨", literalDlq.path("status").asText());
            for (String field : new String[] {"topicKey", "environment"}) {
                assertEquals("확인됨", literalDlq.path(field).path("status").asText());
                assertEquals("", literalDlq.path(field).path("value").asText());
                assertTrue(literalDlq.path(field).path("evidence").toString().contains(line));
            }
            assertFalse(StreamSupport.stream(analysis.path("kafkaLinks").spliterator(), false)
                    .anyMatch(n -> n.path("publicationId").asText().equals(literalDlq.path("id").asText())));
        }
        assertEquals(19, analysis.path("kafkaDeclarations").size());
        assertFalse(analysis.path("kafkaDeclarations").toString().contains("MailServiceImpl.java:21"));
    }

    @Test void profilesAndResourcesExposeEvidenceButNotValues() {
        JsonNode env = withEvidence(analysis.path("environmentVariables"), "coupon/src/main/resources/application-prod.yml:16");
        assertEquals("REDIS_PROD_HOST", env.path("name").asText());
        assertEquals("prod", env.path("profile").asText());
        assertEquals("저장소에 근거 없음", env.path("value").path("status").asText());
        assertTrue(env.path("value").path("value").isNull());
        JsonNode db = withEvidence(analysis.path("externalResources"), "order/src/main/resources/application-prod.yml:3");
        assertEquals("DB", db.path("kind").asText());
        for (String kind : new String[] {"DB", "Redis", "Kafka", "Elasticsearch", "S3", "KakaoPay"}) {
            JsonNode resource = find(analysis.path("externalResources"), "kind", kind);
            assertFalse(resource.path("configurationEvidence").isEmpty(), kind);
            assertFalse(resource.path("codeEvidence").isEmpty(), kind);
        }
        assertTrue(find(analysis.path("externalResources"), "kind", "KakaoPay")
                .path("codeEvidence").toString().contains("KakaoPayClient.java:14"));
        for (JsonNode variable : analysis.path("environmentVariables")) {
            assertEquals("저장소에 근거 없음", variable.path("value").path("status").asText());
            assertTrue(variable.path("value").path("value").isNull());
        }
    }
}
