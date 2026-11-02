package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VerifierTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String ORDER = "order/src/main/java/com/sparta/hotdeal/order/";
    private static final String COUPON = "coupon/src/main/java/com/sparta/hotdeal/coupon/";

    private ObjectNode document() {
        var root = JSON.createObjectNode().put("schemaVersion", 1);
        for (String key : new String[]{"services", "endpoints", "feignClients", "feignCalls", "gatewayRoutes",
                "allowedPaths", "declarations", "kafkaDeclarations", "kafkaPublications", "kafkaSubscriptions",
                "kafkaLinks", "environmentVariables", "externalResources", "warnings"}) root.putArray(key);
        root.putObject("checks").putObject("obsolete").put("warningCount", 123);
        root.withArray("warnings").addObject().put("reason", "입력 경고는 버린다");
        return root;
    }

    private ObjectNode item(String... evidence) {
        var node = JSON.createObjectNode().put("status", "입력 상태는 버린다").put("reason", "입력 이유는 버린다");
        var array = node.putArray("evidence");
        for (String line : evidence) array.add(line);
        return node;
    }

    private ObjectNode fact(String value, String... evidence) {
        return item(evidence).put("value", value);
    }

    private ObjectNode endpoint(String id, String service, String path, String parent, String mapping) {
        var node = item(mapping, parent).put("id", id).put("service", service);
        node.set("path", fact(path, parent, mapping));
        node.set("httpMethod", fact("POST", mapping));
        node.putArray("externalPaths");
        return node;
    }

    private JsonNode verify(ObjectNode input) throws Exception {
        return new Verifier().verify(input, Path.of("target/HotDealWave"));
    }

    @Test void orderMappingEvidenceIsCheckedAndInputVerdictsAreDiscarded() throws Exception {
        var input = document();
        String file = ORDER + "presentation/controller/OrderController.java";
        input.withArray("endpoints").add(endpoint("order", "order-service", "/api/v1/orders", file + ":35", file + ":40"));
        var output = verify(input);
        assertEquals("확인됨", output.at("/endpoints/0/path/status").asText());
        assertEquals("확인됨", output.at("/endpoints/0/httpMethod/status").asText());
        assertFalse(output.path("checks").has("obsolete"));
        assertFalse(output.toString().contains("입력"));
        assertTrue(input.toString().contains("입력"));
    }

    @Test void changingOnlyOneEvidenceLineRejectsTheItem() throws Exception {
        var input = document();
        String file = ORDER + "presentation/controller/OrderController.java";
        var mapping = item(file + ":40").put("kind", "PostMapping");
        input.withArray("declarations").add(mapping);
        assertEquals("확인됨", verify(input).at("/declarations/0/status").asText());
        mapping.withArray("evidence").set(0, JSON.getNodeFactory().textNode(file + ":41"));
        var output = verify(input);
        assertEquals("확인 안 됨", output.at("/declarations/0/status").asText());
        assertTrue(output.path("warnings").toString().contains(file + ":41"));
        assertEquals(1, output.at("/checks/evidence-lines/warningCount").asInt());
    }

    @Test void pathRejectsNonMappingEvidenceEvenWhenTheClassPathStillMatches() throws Exception {
        var input = document();
        String file = ORDER + "presentation/controller/OrderController.java";
        var endpoint = endpoint("order", "order-service", "/api/v1/orders", file + ":35", file + ":40");
        input.withArray("endpoints").add(endpoint);
        var output = verify(input);
        assertEquals("확인됨", output.at("/endpoints/0/path/status").asText());
        assertEquals(0, output.at("/checks/evidence-lines/warningCount").asInt());

        ((ObjectNode) endpoint.path("path")).withArray("evidence")
                .set(1, JSON.getNodeFactory().textNode(file + ":41"));
        output = verify(input);
        assertEquals("확인 안 됨", output.at("/endpoints/0/path/status").asText());
        assertEquals("확인 안 됨", output.at("/endpoints/0/status").asText());
        assertEquals(1, output.at("/checks/evidence-lines/warningCount").asInt());
        assertTrue(output.path("warnings").toString().contains(file + ":41"));
    }

    @Test void inventoryIncludesCommentedMapping() throws Exception {
        var output = verify(document());
        assertEquals(103, output.at("/checks/declarations/mappingCount").asInt());
        assertEquals(9, output.at("/checks/declarations/feignClientCount").asInt());
        assertTrue(output.path("warnings").toString().contains("PaymentViewController.java:12"));
        assertEquals(9, output.at("/checks/kafka-declarations/listenerCount").asInt());
        assertEquals(7, output.at("/checks/kafka-declarations/sendCount").asInt());
        assertEquals(3, output.at("/checks/kafka-declarations/dlqCount").asInt());
    }

    @Test void feignTargetIsCheckedByServiceMethodAndPath() throws Exception {
        var input = document();
        String clientLine = ORDER + "infrastructure/client/CouponClient.java:13";
        String callLine = ORDER + "infrastructure/client/CouponClient.java:15";
        String controller = COUPON + "presentation/controller/CouponController.java";
        var client = item(clientLine).put("id", "client").put("type", "CouponClient");
        client.set("name", fact("coupon-service", clientLine));
        client.set("url", fact("", clientLine));
        client.set("path", fact("", clientLine));
        input.withArray("feignClients").add(client);
        var endpoint = endpoint("coupon", "coupon-service", "/api/v1/coupons/{couponId}/validate", controller + ":23", controller + ":63");
        input.withArray("endpoints").add(endpoint);
        var call = item(callLine, clientLine).put("id", "call").put("clientId", "client").put("handler", "validateCoupon").put("external", false);
        call.set("path", fact("/api/v1/coupons/{couponId}/validate", callLine));
        call.set("httpMethod", fact("POST", callLine));
        call.set("target", fact("coupon", callLine, controller + ":63"));
        input.withArray("feignCalls").add(call);
        assertEquals("확인됨", verify(input).at("/feignCalls/0/target/status").asText());
        ((ObjectNode) call.path("target")).put("value", "missing");
        var rejected = verify(input);
        assertEquals("확인 안 됨", rejected.at("/feignCalls/0/target/status").asText());
        assertEquals(1, rejected.at("/checks/feign-targets/warningCount").asInt());
    }

    private ObjectNode kafka(String id, String service, String kind, String key, String env, String... lines) {
        var node = item(lines).put("id", id).put("service", service).put("kind", kind).put("profile", "base");
        node.set("topicKey", fact(key, lines));
        node.set("topicName", fact(null, lines));
        node.set("environment", fact(env, lines));
        return node;
    }

    @Test void kafkaRelationsRequireEqualEnvironmentNamesAndBothComposeLines() throws Exception {
        var input = document();
        String payment = "payment/src/main/java/com/sparta/hotdeal/payment/event/producer/PaymentEventProducer.java";
        String consumer = ORDER + "event/consumer/OrderStatusUpdateEventConsumer.java:25";
        String env = "KAFKA_UPDATE_ORDER_STATUS_TOPIC";
        var pub = kafka("pub", "payment-service", "kafkaTemplate.send", "spring.kafka.topics.update-order-status", env,
                payment + ":52", payment + ":23", "payment/src/main/resources/application.yml:18", "docker-compose.yml:196");
        var sub = kafka("sub", "order-service", "KafkaListener", "spring.kafka.topics.update-order-status", env,
                consumer, "order/src/main/resources/application.yml:17", "docker-compose.yml:157");
        input.withArray("kafkaPublications").add(pub);
        input.withArray("kafkaSubscriptions").add(sub);
        input.withArray("kafkaLinks").add(item("docker-compose.yml:196", "docker-compose.yml:157")
                .put("publicationId", "pub").put("subscriptionId", "sub").put("environment", env));
        var output = verify(input);
        assertEquals("확인됨", output.at("/kafkaLinks/0/status").asText());
        assertEquals(0, output.at("/checks/kafka-topics/warningCount").asInt());

        input.withArray("kafkaPublications").add(kafka("request", "order-service", "kafkaTemplate.send",
                "spring.kafka.topics.request-product", "KAFKA_REQUEST_PRODUCT_TOPIC", ORDER + "event/producer/OrderEventProducer.java:62",
                ORDER + "event/producer/OrderEventProducer.java:24", "order/src/main/resources/application.yml:15", "docker-compose.yml:156"));
        input.withArray("kafkaSubscriptions").add(kafka("reduce", "product-service", "KafkaListener",
                "spring.kafka.topics.reduce-quantity", "KAFKA_REDUCE_QUANTITY_TOPIC",
                "product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java:29",
                "product/src/main/resources/application.yml:14", "docker-compose.yml:90"));
        output = verify(input);
        assertEquals(1, output.path("kafkaLinks").size());
        assertEquals(2, output.at("/checks/kafka-topics/warningCount").asInt());
        assertEquals("확인됨", output.at("/kafkaPublications/1/status").asText());
        assertEquals("확인됨", output.at("/kafkaSubscriptions/1/status").asText());
    }

    @Test void environmentOnlyValueIsAbsentButPlainNullIsUnknown() throws Exception {
        var input = document();
        String line = "coupon/src/main/resources/application-prod.yml:16";
        var env = item(line).put("name", "REDIS_PROD_HOST").put("location", "spring.data.redis.host");
        env.set("value", fact("${REDIS_PROD_HOST}", line));
        input.withArray("environmentVariables").add(env);
        var output = verify(input);
        assertEquals("저장소에 근거 없음", output.at("/environmentVariables/0/value/status").asText());
        assertTrue(output.at("/environmentVariables/0/value/value").isNull());
        var client = item(ORDER + "infrastructure/client/CouponClient.java:13").put("id", "client");
        client.set("name", fact(null, ORDER + "infrastructure/client/CouponClient.java:13"));
        input.withArray("feignClients").add(client);
        output = verify(input);
        assertEquals("확인 안 됨", output.at("/feignClients/0/name/status").asText());
        assertEquals("확인 안 됨", output.at("/feignClients/0/status").asText());
        assertEquals(0, output.at("/checks/evidence-lines/warningCount").asInt());
    }

    @Test void unresolvedTypeWithValidEvidenceRemainsUnknownWithoutEvidenceWarning() throws Exception {
        var input = document();
        String file = ORDER + "presentation/controller/OrderController.java";
        var endpoint = endpoint("orders", "order-service", "/api/v1/orders", file + ":35", file + ":49");
        endpoint.set("httpMethod", fact("GET", file + ":49"));
        var parameter = item(file + ":52").put("name", "pageable").put("binding", "PageableDefault");
        parameter.set("schema", item(file + ":52").put("type", "Pageable").put("kind", "unresolved"));
        endpoint.putArray("parameters").add(parameter);
        input.withArray("endpoints").add(endpoint);
        var output = verify(input);
        assertEquals("확인 안 됨", output.at("/endpoints/0/parameters/0/schema/status").asText());
        assertEquals(0, output.at("/checks/evidence-lines/warningCount").asInt());
    }

    @Test void jsonNameUsesBooleanFieldAndLombokEvidence() throws Exception {
        var input = document();
        String dto = COUPON + "application/dto/res/ResGetUserCouponsDto.java";
        var response = item(dto + ":15").put("type", "ResGetUserCouponsDto").put("kind", "object");
        var field = item(dto + ":21").put("name", "isUsed");
        field.set("schema", item(dto + ":21").put("type", "boolean").put("kind", "scalar"));
        field.set("jsonName", fact("used", dto + ":21", dto + ":11"));
        response.putArray("fields").add(field);
        String controller = COUPON + "presentation/controller/CouponController.java";
        var endpoint = endpoint("coupon", "coupon-service", "/api/v1/coupons/{couponId}/validate", controller + ":23", controller + ":63");
        endpoint.set("response", response);
        input.withArray("endpoints").add(endpoint);
        assertEquals("확인됨", verify(input).at("/endpoints/0/response/fields/0/jsonName/status").asText());
        ((ObjectNode) field.path("jsonName")).put("value", "isUsed");
        assertEquals("확인 안 됨", verify(input).at("/endpoints/0/response/fields/0/jsonName/status").asText());
    }
}
