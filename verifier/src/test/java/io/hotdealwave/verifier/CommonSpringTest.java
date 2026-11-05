package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** A small Spring MSA that is not shaped like HotDealWave: Maven and Kotlin DSL builds under services/, properties and yaml, Java DSL routes. */
class CommonSpringTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String ORDER = "services/order/src/main/java/shop/order/";
    private static final String PROPERTIES = "services/order/src/main/resources/application.properties";
    private static final String DELIVERY_YAML = "services/delivery/src/main/resources/application.yaml";
    private static final String GATEWAY = "gateway/src/main/java/shop/gateway/";
    private static final String COMPOSE = "docker/compose.yaml";
    @TempDir Path repository;

    @BeforeEach void repository() throws Exception {
        write("services/order/pom.xml", "<project/>");
        write(PROPERTIES, """
                spring.application.name=order-service
                spring.kafka.topics.order=${ORDER_TOPIC}
                spring.kafka.topics.paid=${PAID_TOPIC:order-paid}
                """);
        write(ORDER + "OrderController.java", """
                package shop.order;

                @RestController
                @RequestMapping("/orders")
                public class OrderController {
                    @GetMapping("/{id}")
                    public OrderResponse get(@PathVariable Long id) { return null; }
                }
                """);
        write(ORDER + "OrderEvents.java", """
                package shop.order;

                public class OrderEvents {
                    private final KafkaTemplate<String, String> orderTemplate;
                    @Value("${spring.kafka.topics.order}") private String orderTopic;
                    void created() { orderTemplate.send(orderTopic, "x"); }
                    void literal() { orderTemplate.send("order-created", "x"); }
                }
                """);
        write(ORDER + "OrderResponse.java", """
                package shop.order;

                public record OrderResponse(Long orderId, @JsonProperty("order_status") String status) {}
                """);
        write(ORDER + "Item.java", """
                package shop.order;

                @Getter
                public class Item {
                    @JsonProperty("item_name")
                    private String name;
                }
                """);
        write(ORDER + "Snake.java", """
                package shop.order;

                @Getter
                @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
                public class Snake {
                    private String itemName;
                }
                """);
        write(ORDER + "ErrorAdvice.java", """
                package shop.order;

                @RestControllerAdvice
                public class ErrorAdvice {
                    @ExceptionHandler(NotFound.class)
                    public ProblemDetail notFound(NotFound e) {
                        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
                    }
                }
                """);
        write("services/delivery/build.gradle.kts", "plugins { java }");
        write(DELIVERY_YAML, """
                spring:
                  application:
                    name: delivery-service
                  kafka:
                    topics:
                      order: order-created
                """);
        write("services/delivery/src/main/java/shop/delivery/OrderListener.java", """
                package shop.delivery;

                public class OrderListener {
                    @KafkaListener(topics = "${spring.kafka.topics.order}")
                    void on(String message) {}
                }
                """);
        write("services/delivery/build/generated/build.gradle", "");
        write("services/delivery/build/generated/src/main/java/Generated.java", "@GetMapping(\"/generated\")");
        write("common/build.gradle.kts", "plugins { java }");
        write("common/src/main/java/shop/common/HealthController.java", """
                package shop.common;

                public class HealthController {
                    @GetMapping("/health")
                    public String health() { return "ok"; }
                }
                """);
        write("gateway/build.gradle", "");
        write(GATEWAY + "Routes.java", """
                package shop.gateway;

                public class Routes {
                    @Bean
                    RouteLocator routes(RouteLocatorBuilder builder) {
                        return builder.routes()
                                .route("order", r -> r.path("/order/**")
                                        .filters(f -> f.stripPrefix(1))
                                        .uri("lb://order-service"))
                                .route("delivery", r -> r.path("/api/deliveries/**")
                                        .filters(f -> f.rewritePath("/api/(?<segment>.*)", "/${segment}"))
                                        .uri("lb://delivery-service"))
                                .build();
                    }
                }
                """);
        write(GATEWAY + "Security.java", """
                package shop.gateway;

                public class Security {
                    SecurityWebFilterChain chain(ServerHttpSecurity http) {
                        return http.authorizeExchange(exchanges -> exchanges
                                .pathMatchers(HttpMethod.GET, "/order/**").permitAll()
                                .pathMatchers("/order/{id}/cancel").permitAll()
                                .anyExchange().authenticated())
                                .build();
                    }
                }
                """);
        write(GATEWAY + "OpenPaths.java", """
                package shop.gateway;

                public class OpenPaths {
                    static final String ORDERS = "/order/", ANY_METHOD = "*";
                    static boolean open(String path) { return path.equals(ORDERS); }
                }
                """);
        write(COMPOSE, """
                services:
                  order:
                    build: ../services/order
                    environment:
                      ORDER_TOPIC: ${ORDER_TOPIC}
                """);
        write(ORDER + "StockClient.java", """
                package shop.order;

                public class StockClient {
                    private final RestTemplate restTemplate = new RestTemplate();
                    int stock(Long id) { return restTemplate.getForObject("http://stock-service/stocks/" + id, Integer.class); }
                }
                """);
        write("services/order/src/main/kotlin/shop/order/Legacy.kt", "class Legacy(val name: String)");
    }

    private void write(String file, String text) throws Exception {
        Files.writeString(Files.createDirectories(repository.resolve(file).getParent()).resolve(Path.of(file).getFileName()), text);
    }

    private ObjectNode document() {
        var root = JSON.createObjectNode().put("schemaVersion", 1);
        for (String key : new String[]{"services", "endpoints", "feignClients", "feignCalls", "gatewayRoutes", "allowedPaths",
                "declarations", "kafkaDeclarations", "kafkaPublications", "kafkaSubscriptions", "kafkaLinks",
                "environmentVariables", "externalResources", "exceptionHandlers", "unsupported"}) root.putArray(key);
        return root;
    }

    private ObjectNode item(String... evidence) {
        var node = JSON.createObjectNode();
        var array = node.putArray("evidence");
        for (String line : evidence) array.add(line);
        return node;
    }

    private ObjectNode fact(String value, String... evidence) {
        return item(evidence).put("value", value);
    }

    private JsonNode verify(ObjectNode input) throws Exception {
        return new Verifier().verify(input, repository);
    }

    private ObjectNode orderService() {
        var service = item(PROPERTIES + ":1").put("directory", "services/order");
        service.set("name", fact("order-service", PROPERTIES + ":1"));
        service.set("composeService", fact("order", COMPOSE + ":2", COMPOSE + ":3"));
        return service;
    }

    private ObjectNode kafka(String id, String service, String kind, String key, String name, String environment, String... lines) {
        var node = item(lines).put("id", id).put("service", service).put("kind", kind).put("profile", "base");
        node.set("topicKey", fact(key, lines));
        node.set("topicName", fact(name, lines));
        node.set("environment", fact(environment, lines));
        return node;
    }

    @Test void servicesInSubfoldersAndCommonModulesAreInventoriedButBuildOutputIsNot() throws Exception {
        var output = verify(document());
        String warnings = output.path("warnings").toString();
        assertTrue(warnings.contains(ORDER + "OrderController.java:6"));
        assertTrue(warnings.contains("common/src/main/java/shop/common/HealthController.java:4"));
        assertFalse(warnings.contains("Generated.java"));
        assertEquals(2, output.at("/checks/kafka-declarations/sendCount").asInt());
        assertEquals(1, output.at("/checks/kafka-declarations/listenerCount").asInt());
    }

    private String code(JsonNode output, String evidence) {
        return Checks.items(output.path("warnings")).stream().filter(w -> w.path("evidence").toString().contains(evidence))
                .map(w -> w.path("code").asText()).findFirst().orElse("");
    }

    @Test void evidenceWarningsNameWhyTheLineCouldNotBeRead() throws Exception {
        var input = document();
        for (String evidence : new String[]{"services/order/src/main/kotlin/shop/order/Legacy.kt:1", ORDER + "Missing.java:1",
                PROPERTIES + ":99", PROPERTIES + ":2"}) {
            var service = item(evidence).put("directory", "services/order");
            service.set("name", fact("order-service", evidence));
            input.withArray("services").add(service);
        }
        var output = verify(input);
        assertEquals("file-type", code(output, "Legacy.kt:1"));
        assertEquals("file-missing", code(output, "Missing.java:1"));
        assertEquals("line-missing", code(output, "application.properties:99"));
        assertEquals("extraction-mismatch", code(output, "application.properties:2"));
        for (String code : new String[]{"file-type", "file-missing", "line-missing", "extraction-mismatch"})
            assertFalse(output.at("/warningCodes/" + code + "/action").asText().isEmpty(), code);
        assertFalse(output.at("/confirmed/services").asText().isEmpty());
    }

    @Test void unsupportedMechanismIsKeptWithItsLineAndCarriedThroughTheWorkspace() throws Exception {
        String line = ORDER + "StockClient.java:5";
        var unsupported = item(line).put("service", "order-service").put("mechanism", "RestTemplate")
                .put("description", "stock-service의 재고를 HTTP로 조회한다");
        var input = document();
        input.withArray("unsupported").add(unsupported);
        assertEquals(Checks.CONFIRMED, verify(input).at("/unsupported/0/status").asText());
        unsupported.put("mechanism", "WebClient");
        assertEquals("extraction-mismatch", code(verify(input), line));

        Path workspace = Files.createDirectories(repository.resolveSibling("workspace").resolve("services"));
        var file = JSON.createObjectNode().put("schemaVersion", 1);
        file.set("service", orderService());
        for (String key : new String[]{"endpoints", "feignClients", "feignCalls", "gatewayRoutes", "allowedPaths", "declarations",
                "kafkaDeclarations", "kafkaPublications", "kafkaSubscriptions", "environmentVariables", "externalResources",
                "errorCodes", "exceptionHandlers", "gatewayRejections", "pageSerialization"}) file.putArray(key);
        file.putArray("unsupported").add(unsupported.put("mechanism", "RestTemplate"));
        JSON.writeValue(workspace.resolve("order-service.json").toFile(), file);
        Files.writeString(workspace.resolveSibling("relations.json"), "{\"schemaVersion\":1,\"kafkaLinks\":[],\"calls\":[],\"throws\":[]}");
        Files.writeString(workspace.resolveSibling("target.json"),
                "{\"schemaVersion\":1,\"name\":\"shop\",\"repository\":\"https://github.com/acme/shop\",\"commit\":\"0123456789abcdef0123456789abcdef01234567\"}");
        var output = new Verifier().verify(workspace.getParent(), repository);
        assertEquals(0, output.at("/checks/schema/warningCount").asInt(), output.path("warnings").toString());
        assertEquals(Checks.CONFIRMED, output.at("/unsupported/0/status").asText());
    }

    @Test void propertiesAndYamlNamesAreConfirmed() throws Exception {
        var input = document();
        input.withArray("services").add(orderService());
        var delivery = item(DELIVERY_YAML + ":3").put("directory", "services/delivery");
        delivery.set("name", fact("delivery-service", DELIVERY_YAML + ":3"));
        input.withArray("services").add(delivery);
        var output = verify(input);
        assertEquals(Checks.CONFIRMED, output.at("/services/0/name/status").asText());
        assertEquals(Checks.CONFIRMED, output.at("/services/0/composeService/status").asText());
        assertEquals(Checks.CONFIRMED, output.at("/services/1/name/status").asText());
        assertEquals(0, output.at("/checks/evidence-lines/warningCount").asInt());
    }

    @Test void topicChainRunsFromPropertiesToComposeMapEntry() throws Exception {
        var input = document();
        input.withArray("services").add(orderService());
        var publication = kafka("created", "order-service", "kafkaTemplate.send", "spring.kafka.topics.order", null, "ORDER_TOPIC",
                ORDER + "OrderEvents.java:6", ORDER + "OrderEvents.java:5", PROPERTIES + ":2", COMPOSE + ":5");
        input.withArray("kafkaPublications").add(publication);
        var output = verify(input);
        assertEquals(Checks.CONFIRMED, output.at("/kafkaPublications/0/environment/status").asText());
        assertEquals(Checks.ABSENT, output.at("/kafkaPublications/0/topicName/status").asText());
        assertEquals(0, output.at("/checks/evidence-lines/warningCount").asInt());

        ((ObjectNode) publication.path("environment")).put("value", "OTHER_TOPIC");
        assertEquals(Checks.UNKNOWN, verify(input).at("/kafkaPublications/0/environment/status").asText());
    }

    @Test void variableWithDefaultIsNamedAndItsValueIsOutsideTheRepository() throws Exception {
        var input = document();
        var variable = item(PROPERTIES + ":3").put("name", "PAID_TOPIC").put("location", "spring.kafka.topics.paid");
        variable.set("value", fact("${PAID_TOPIC:order-paid}", PROPERTIES + ":3"));
        input.withArray("environmentVariables").add(variable);
        var output = verify(input);
        assertEquals(Checks.CONFIRMED, output.at("/environmentVariables/0/status").asText());
        assertEquals(Checks.ABSENT, output.at("/environmentVariables/0/value/status").asText());
        assertTrue(output.at("/environmentVariables/0/value/reason").asText().contains("order-paid"));
    }

    @Test void publicationAndSubscriptionWithTheSameTopicNameAreLinked() throws Exception {
        var input = document();
        String send = ORDER + "OrderEvents.java:7", value = DELIVERY_YAML + ":6";
        input.withArray("kafkaPublications").add(kafka("literal", "order-service", "kafkaTemplate.send", "", "order-created", "", send));
        var subscription = kafka("delivery", "delivery-service", "KafkaListener", "spring.kafka.topics.order", "order-created", "",
                "services/delivery/src/main/java/shop/delivery/OrderListener.java:4", value);
        subscription.set("topicKey", fact("spring.kafka.topics.order", "services/delivery/src/main/java/shop/delivery/OrderListener.java:4"));
        subscription.set("topicName", fact("order-created", value));
        subscription.set("environment", fact("", value));
        input.withArray("kafkaSubscriptions").add(subscription);
        input.withArray("kafkaLinks").add(item(send, value).put("publicationId", "literal").put("subscriptionId", "delivery").put("environment", ""));
        var output = verify(input);
        assertEquals(Checks.CONFIRMED, output.at("/kafkaSubscriptions/0/topicName/status").asText());
        assertEquals(Checks.CONFIRMED, output.at("/kafkaLinks/0/status").asText());
        assertEquals(0, output.at("/checks/kafka-topics/warningCount").asInt());

        ((ObjectNode) subscription.path("topicName")).put("value", "order-shipped");
        output = verify(input);
        assertEquals(Checks.UNKNOWN, output.at("/kafkaLinks/0/status").asText());
        assertTrue(output.path("warnings").toString().contains("order-created"));
    }

    private ObjectNode orderRoute() {
        var route = item(GATEWAY + "Routes.java:7").put("id", "order");
        route.set("uri", fact("lb://order-service", GATEWAY + "Routes.java:9"));
        route.set("targetService", fact("order-service", GATEWAY + "Routes.java:9"));
        route.set("rewritePath", fact("StripPrefix=1", GATEWAY + "Routes.java:8"));
        route.putArray("paths").add(fact("/order/**", GATEWAY + "Routes.java:7"));
        return route;
    }

    private ObjectNode orderEndpoint(String... tokenEvidence) {
        String controller = ORDER + "OrderController.java";
        var endpoint = item(controller + ":6", controller + ":4").put("id", "order-service.OrderController.get").put("service", "order-service");
        endpoint.set("path", fact("/orders/{id}", controller + ":4", controller + ":6"));
        endpoint.set("httpMethod", fact("GET", controller + ":6"));
        var external = item(controller + ":6", GATEWAY + "Routes.java:7").put("routeId", "order").put("path", "/order/orders/{id}");
        external.set("tokenRequired", JSON.createObjectNode().put("value", false).set("evidence", item(tokenEvidence).path("evidence")));
        endpoint.putArray("externalPaths").add(external);
        return endpoint;
    }

    private ObjectNode allowed(String pattern, String method, String line) {
        var allowed = item(line).put("name", pattern);
        allowed.set("path", fact(pattern, line));
        allowed.set("httpMethod", fact(method, line));
        return allowed;
    }

    @Test void javaDslRoutesWithStripPrefixAndRewriteReachTheController() throws Exception {
        var input = document();
        input.withArray("gatewayRoutes").add(orderRoute());
        var delivery = item(GATEWAY + "Routes.java:10").put("id", "delivery");
        delivery.set("uri", fact("lb://delivery-service", GATEWAY + "Routes.java:12"));
        delivery.set("targetService", fact("delivery-service", GATEWAY + "Routes.java:12"));
        delivery.set("rewritePath", fact("/api/(?<segment>.*), /${segment}", GATEWAY + "Routes.java:11"));
        delivery.putArray("paths").add(fact("/api/deliveries/**", GATEWAY + "Routes.java:10"));
        input.withArray("gatewayRoutes").add(delivery);
        input.withArray("endpoints").add(orderEndpoint(GATEWAY + "Security.java:6", GATEWAY + "Security.java:8"));
        var output = verify(input);
        assertEquals(Checks.CONFIRMED, output.at("/gatewayRoutes/0/rewritePath/status").asText());
        assertEquals(Checks.CONFIRMED, output.at("/gatewayRoutes/1/rewritePath/status").asText());
        assertEquals(Checks.CONFIRMED, output.at("/endpoints/0/externalPaths/0/status").asText());
        assertEquals(0, output.at("/checks/evidence-lines/warningCount").asInt());
    }

    @Test void securityPermitAllDecidesTheTokenOnlyForPrefixPatterns() throws Exception {
        var input = document();
        input.withArray("gatewayRoutes").add(orderRoute());
        input.withArray("endpoints").add(orderEndpoint(GATEWAY + "Security.java:6", GATEWAY + "Security.java:8"));
        input.withArray("allowedPaths").add(allowed("/order/**", "GET", GATEWAY + "Security.java:6"));
        var output = verify(input);
        assertEquals(Checks.CONFIRMED, output.at("/allowedPaths/0/httpMethod/status").asText());
        assertEquals(Checks.CONFIRMED, output.at("/endpoints/0/externalPaths/0/tokenRequired/status").asText());

        input.withArray("allowedPaths").add(allowed("/order/{id}/cancel", "*", GATEWAY + "Security.java:7"));
        output = verify(input);
        assertEquals(Checks.UNKNOWN, output.at("/endpoints/0/externalPaths/0/tokenRequired/status").asText());
        assertTrue(output.at("/endpoints/0/externalPaths/0/tokenRequired/reason").asText().contains("/**"));
    }

    @Test void customFilterThatDoesNotCompareByPrefixLeavesTheTokenUnknown() throws Exception {
        var input = document();
        input.withArray("gatewayRoutes").add(orderRoute());
        input.withArray("endpoints").add(orderEndpoint(GATEWAY + "OpenPaths.java:4", GATEWAY + "OpenPaths.java:5"));
        input.withArray("allowedPaths").add(allowed("/order/", "*", GATEWAY + "OpenPaths.java:4"));
        ((ObjectNode) input.at("/allowedPaths/0")).put("name", "ORDERS");
        var output = verify(input);
        assertEquals(Checks.CONFIRMED, output.at("/endpoints/0/externalPaths/0/status").asText());
        assertEquals(Checks.UNKNOWN, output.at("/endpoints/0/externalPaths/0/tokenRequired/status").asText());
        assertTrue(output.at("/endpoints/0/externalPaths/0/tokenRequired/reason").asText().contains("startsWith"));
    }

    private ObjectNode field(String name, String jsonName, String... evidence) {
        var field = item(evidence).put("name", name);
        field.set("schema", item(evidence).put("type", "String").put("kind", "scalar"));
        field.set("jsonName", fact(jsonName, evidence));
        return field;
    }

    @Test void jsonNamesFollowJsonPropertyAndRecordsAndStopAtNamingStrategies() throws Exception {
        var input = document();
        var endpoint = orderEndpoint();
        ((ObjectNode) endpoint.path("externalPaths").get(0)).remove("tokenRequired");
        var response = item(ORDER + "OrderResponse.java:3").put("type", "OrderResponse").put("kind", "object");
        response.putArray("fields")
                .add(field("orderId", "orderId", ORDER + "OrderResponse.java:3"))
                .add(field("status", "order_status", ORDER + "OrderResponse.java:3"))
                .add(field("name", "item_name", ORDER + "Item.java:6"))
                .add(field("itemName", "itemName", ORDER + "Snake.java:6", ORDER + "Snake.java:3"));
        endpoint.set("response", response);
        input.withArray("endpoints").add(endpoint);
        var output = verify(input);
        assertEquals(Checks.CONFIRMED, output.at("/endpoints/0/response/fields/0/jsonName/status").asText());
        assertEquals(Checks.CONFIRMED, output.at("/endpoints/0/response/fields/1/jsonName/status").asText());
        assertEquals(Checks.CONFIRMED, output.at("/endpoints/0/response/fields/2/jsonName/status").asText());
        assertEquals(Checks.UNKNOWN, output.at("/endpoints/0/response/fields/3/jsonName/status").asText());
        assertTrue(output.at("/endpoints/0/response/fields/3/jsonName/reason").asText().contains("JsonNaming"));
    }

    @Test void problemDetailHandlerFixesTheStatusAndPassesTheMessageThrough() throws Exception {
        var input = document();
        String advice = ORDER + "ErrorAdvice.java";
        var handler = item(advice + ":5", advice + ":7").put("id", "order-service.ErrorAdvice.notFound");
        handler.set("exception", fact("NotFound", advice + ":5"));
        handler.set("httpStatus", fact("NOT_FOUND", advice + ":7").put("source", "고정"));
        handler.set("errorCode", fact("", advice + ":7").put("source", "고정"));
        handler.set("message", fact(null, advice + ":7").put("source", "예외 메시지"));
        handler.set("body", item(advice + ":6").put("type", "ProblemDetail").put("kind", "unresolved"));
        input.withArray("exceptionHandlers").add(handler);
        var output = verify(input);
        assertEquals(Checks.CONFIRMED, output.at("/exceptionHandlers/0/httpStatus/status").asText());
        assertEquals(Checks.CONFIRMED, output.at("/exceptionHandlers/0/errorCode/status").asText());
        assertEquals(Checks.UNKNOWN, output.at("/exceptionHandlers/0/message/status").asText());
        assertEquals(Checks.UNKNOWN, output.at("/exceptionHandlers/0/body/status").asText());
        assertEquals(0, output.at("/checks/evidence-lines/warningCount").asInt());
    }
}
