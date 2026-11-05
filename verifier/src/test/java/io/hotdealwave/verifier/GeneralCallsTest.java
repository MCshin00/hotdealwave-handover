package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

/** Representative Spring declarations in a temporary target, without building that target. */
class GeneralCallsTest {
    static final ObjectMapper JSON = new ObjectMapper();
    static final String FILE = "shop/src/main/java/shop/Calls.java";
    @TempDir Path target;
    ObjectNode input;

    void setup(String source) throws Exception {
        Files.createDirectories(target.resolve(FILE).getParent());
        Files.writeString(target.resolve(FILE), source);
        Files.writeString(target.resolve("shop/pom.xml"), "<project/>");
        input = JSON.createObjectNode();
        for (String key : List.of("services", "endpoints", "httpClients", "httpCalls", "declarations",
                "messageDeclarations", "messagePublications", "messageSubscriptions", "messageLinks", "calls")) input.putArray(key);
    }
    ObjectNode item(int... lines) {
        var node = JSON.createObjectNode();
        var ev = node.putArray("evidence");
        for (int line : lines) ev.add(FILE + ":" + line);
        return node;
    }
    ObjectNode fact(String value, int... lines) { return item(lines).put("value", value); }
    JsonNode verify() throws Exception { return new Verifier().verify(input, target); }
    ObjectNode call(String kind, String path, int line) {
        var call = item(line).put("id", "shop.Calls.get").put("service", "shop").put("kind", kind)
                .put("clientId", "").put("handler", "get").put("external", false);
        call.set("httpMethod", fact("GET", line));
        call.set("path", fact(path, line));
        call.set("target", fact("stock.Stock.get", line, 1));
        input.withArray("httpCalls").add(call);
        return call;
    }
    void endpoint() {
        var endpoint = item(1).put("id", "stock.Stock.get").put("service", "stock");
        endpoint.set("httpMethod", fact("GET", 1));
        endpoint.set("path", fact("/items", 1));
        input.withArray("endpoints").add(endpoint);
        input.withArray("declarations").add(item(1).put("kind", "GetMapping"));

    }
    void client(String kind, String url, String path, int line) {
        var client = item(line).put("id", "shop.Calls").put("service", "shop").put("kind", kind).put("type", "Calls");
        client.set("name", fact("stock", line));
        client.set("url", fact(url, line));
        client.set("path", fact(path, line));
        input.withArray("httpClients").add(client);
    }

    @ParameterizedTest @CsvSource({"feign", "restTemplate", "restClient", "webClient", "httpExchange"})
    void httpKindsResolveAgainstTheSameEndpoint(String kind) throws Exception {
        String declaration = switch (kind) {
            case "feign" -> "@FeignClient(name = \"stock\") interface Calls {";
            case "httpExchange" -> "@HttpExchange(url = \"http://stock\") interface Calls {";
            case "restClient" -> "RestClient client = RestClient.builder().baseUrl(\"http://stock\").build();";
            case "webClient" -> "WebClient client = WebClient.builder().baseUrl(\"http://stock\").build();";
            default -> "RestTemplate client = new RestTemplate();";
        };
        String invocation = switch (kind) {
            case "feign" -> "@GetMapping(\"/items\") String get();";
            case "httpExchange" -> "@GetExchange(\"/items\") String get();";
            case "restTemplate" -> "String get() { return client.getForObject(\"http://stock/items\", String.class); }";
            default -> "String get() { return client.get().uri(\"/items\").retrieve().body(String.class); }";
        };
        setup("@GetMapping(\"/items\") String stock();\n" + declaration + "\n" + invocation + "\nvoid caller() { calls.get(); }");
        if (kind.equals("restClient") || kind.equals("webClient")) Files.writeString(target.resolve(FILE),
                Files.readString(target.resolve(FILE)) + "\n" + (kind.equals("restClient") ? "WebClient" : "RestClient") + " other;");
        endpoint();
        String path = kind.equals("restTemplate") ? "http://stock/items" : "/items";
        var call = call(kind, path, 3);
        if (!kind.equals("restTemplate")) {
            client(kind, kind.equals("feign") ? "" : "http://stock", "", 2);
            call.put("clientId", "shop.Calls");
        }
        if (kind.equals("feign")) input.withArray("declarations").add(item(3).put("kind", "GetMapping"));
        input.withArray("calls").add(item(1, kind.equals("feign") || kind.equals("httpExchange") ? 4 : 3)
                .put("fromId", "stock.Stock.get").put("toId", "shop.Calls.get"));
        // Use a same-service handler for the call-chain check.
        ((ObjectNode) input.path("endpoints").get(0)).put("service", "shop");
        var origin = input.path("endpoints").get(0).deepCopy();
        ((ObjectNode) origin).put("id", "shop.Origin.get");
        ((ObjectNode) input.path("endpoints").get(0)).put("service", "stock");
        input.withArray("endpoints").add(origin);
        ((ObjectNode) input.path("calls").get(0)).put("fromId", "shop.Origin.get");
        var output = verify();
        assertEquals(Checks.CONFIRMED, output.at("/httpCalls/0/target/status").asText(), output.path("warnings").toPrettyString());
        assertEquals(Checks.CONFIRMED, output.at("/calls/0/status").asText());
        assertFalse(Checks.items(output.path("warnings")).stream().anyMatch(w -> w.path("code").asText().equals("inventory-mismatch")), output.path("warnings").toPrettyString());
        ((ObjectNode)call.path("target")).put("value", "stock.Wrong.get");
        assertTrue(Checks.items(verify().path("warnings")).stream().anyMatch(w -> w.path("code").asText().equals("target-mismatch")));
    }

    @Test void fullUrlExternalEnvironmentAndDynamicTargetsAreNotInternalMatches() throws Exception {
        setup("@GetMapping(\"/items\") String get();\nRestTemplate client;\nclient.getForObject(\"https://outside.example/items\", String.class);\n"
                + "client.getForObject(\"${STOCK_URL}\", String.class);\nclient.getForObject(base + suffix, String.class);\n"
                + "@Value(\"${stock.url}\") String configuredUrl;\nclient.getForObject(configuredUrl, String.class);");
        endpoint();
        call("restTemplate", "https://outside.example/items", 3).put("external", true);
        call("restTemplate", "${STOCK_URL}", 4).put("id", "shop.Calls.env");
        call("restTemplate", null, 5).put("id", "shop.Calls.dynamic").put("external", true);
        var configured = call("restTemplate", null, 7).put("id", "shop.Calls.configured");
        ((ObjectNode)configured.path("path")).withArray("evidence").add(FILE + ":6");
        var output = verify();
        assertEquals(Checks.ABSENT, output.at("/httpCalls/3/target/status").asText());
        assertTrue(output.at("/httpCalls/0/external").asBoolean());
        assertEquals(Checks.ABSENT, output.at("/httpCalls/0/target/status").asText());
        assertEquals(Checks.ABSENT, output.at("/httpCalls/1/target/status").asText());
        assertEquals(Checks.UNKNOWN, output.at("/httpCalls/2/target/status").asText());
        assertTrue(output.at("/httpCalls/2/external").asBoolean());
        assertFalse(Checks.items(output.path("warnings")).stream().anyMatch(w -> w.path("code").asText().equals("target-mismatch")));
    }

    @Test void mappingPathsUseOnlyThePathArgument() throws Exception {
        setup("""
                @GetMapping("/x/{id}") String stock();
                @FeignClient(name = "stock") interface Calls {
                @GetMapping(value = "/x/{id}", headers = {"A=B"}, params = {"q=yes"}, produces = "application/json", consumes = "application/json", name = "lookup") String get();
                }
                """);
        endpoint();
        ((ObjectNode) input.at("/endpoints/0/path")).put("value", "/x/{id}");
        client("feign", "", "", 2);
        var call = call("feign", "/x/{id}", 3).put("clientId", "shop.Calls");
        input.withArray("declarations").add(item(3).put("kind", "GetMapping"));
        var output = verify();
        assertEquals(Checks.CONFIRMED, output.at("/httpCalls/0/path/status").asText());
        assertEquals(Checks.CONFIRMED, output.at("/httpCalls/0/target/status").asText());
        assertEquals(0, output.at("/checks/http-declarations/warningCount").asInt());
        assertEquals(0, output.at("/checks/declarations/warningCount").asInt());
        Files.writeString(target.resolve(FILE), Files.readString(target.resolve(FILE)).replace("value =", "path ="));
        assertEquals(Checks.CONFIRMED, verify().at("/httpCalls/0/path/status").asText());
        ((ObjectNode) call.path("path")).put("value", "/x/{id}/A=B");
        assertEquals(Checks.UNKNOWN, verify().at("/httpCalls/0/path/status").asText());
    }

    @Test void configuredFeignUrlPreservesExternalAndWarnsOnFalse() throws Exception {
        setup("""
                @FeignClient(name = "gemini", url = "${gemini.url}") interface Calls {
                @GetMapping("/items") String get();
                }
                """);
        client("feign", "${gemini.url}", "", 1);
        ((ObjectNode) input.at("/httpClients/0/name")).put("value", "gemini");
        var call = call("feign", "/items", 2).put("clientId", "shop.Calls").put("external", true);
        ((ObjectNode) call.path("target")).putNull("value");
        var output = verify();
        assertTrue(output.at("/httpCalls/0/external").asBoolean());
        assertEquals(Checks.ABSENT, output.at("/httpCalls/0/target/status").asText());
        assertFalse(Checks.items(output.path("warnings")).stream().anyMatch(w -> w.path("check").asText().equals("http-targets")));

        call.put("external", false);
        output = verify();
        assertFalse(output.at("/httpCalls/0/external").asBoolean());
        assertEquals(Checks.UNKNOWN, output.at("/httpCalls/0/status").asText());
        assertTrue(Checks.items(output.path("warnings")).stream().anyMatch(w ->
                w.path("check").asText().equals("http-targets") && w.path("code").asText().equals("extraction-mismatch")));
    }

    ObjectNode message(String array, String kind, String name, int line) {
        var node = item(line).put("id", array).put("service", "shop").put("kind", kind).put("profile", "base");
        node.set("topicKey", fact("", line));
        node.set("topicName", fact(name, line));
        node.set("environment", fact("", line));
        input.withArray(array).add(node);
        input.withArray("messageDeclarations").add(item(line).put("kind", kind));
        return node;
    }
    void link(int... lines) {
        input.withArray("messageLinks").add(item(lines).put("publicationId", "messagePublications")
                .put("subscriptionId", "messageSubscriptions").put("environment", ""));
    }
    @ParameterizedTest @CsvSource({"kafka", "rabbit", "stream"})
    void messageKindsHaveDeclarationInventoriesAndDestinationLinks(String kind) throws Exception {
        String source = switch (kind) {
            case "kafka" -> "KafkaTemplate<String,String> bus;\nbus.send(\"orders\", value);\n@KafkaListener(topics = \"orders\") void receive(String value) {}";
            case "rabbit" -> "RabbitTemplate bus;\nbus.convertAndSend(\"orders\", value);\n@RabbitListener(queues = \"orders\") void receive(String value) {}";
            default -> "StreamBridge bus;\nbus.send(\"orders-out-0\", value);\n@Bean Consumer<String> receive() { return value -> {}; }";
        };
        setup(source);
        var pub = message("messagePublications", kind, kind.equals("rabbit") ? "/orders" : "orders", 2);
        var sub = message("messageSubscriptions", kind, "orders", 3);
        if (kind.equals("stream")) {
            Path settings = target.resolve("shop/src/main/resources/application.properties");
            Files.createDirectories(settings.getParent());
            Files.writeString(settings, "spring.cloud.stream.bindings.orders-out-0.destination=orders\nspring.cloud.stream.bindings.receive-in-0.destination=orders\n");
            int number = 1;
            for (var node : List.of(pub, sub)) {
                String key = "spring.cloud.stream.bindings." + (number == 1 ? "orders-out-0" : "receive-in-0") + ".destination";
                String ev = "shop/src/main/resources/application.properties:" + number++;
                for (String field : List.of("topicKey", "topicName", "environment")) {
                    ((ObjectNode)node.path(field)).put("value", field.equals("topicKey") ? key : field.equals("topicName") ? "orders" : "");
                    ((ObjectNode)node.path(field)).withArray("evidence").add(ev);
                }
                node.withArray("evidence").add(ev);
            }
        }
        link(2, 3);
        for (var node : List.of(pub, sub)) for (var ev : node.path("topicName").path("evidence"))
            ((ObjectNode)input.path("messageLinks").get(0)).withArray("evidence").add(ev);
        var output = verify();
        assertEquals(Checks.CONFIRMED, output.at("/messageLinks/0/status").asText(), output.path("warnings").toPrettyString());
        assertFalse(Checks.items(output.path("warnings")).stream().anyMatch(w -> w.path("code").asText().equals("inventory-mismatch")), output.path("warnings").toPrettyString());
        if (kind.equals("rabbit")) {
            Files.writeString(target.resolve(FILE), source + "\nbus.setExchange(\"events\");");
            assertEquals(Checks.UNKNOWN, verify().at("/messageLinks/0/status").asText());
            Files.writeString(target.resolve(FILE), source);
        }
        // A matching spelling alone must never connect different transports.
        sub.put("kind", kind.equals("kafka") ? "rabbit" : "kafka");
        assertEquals(Checks.UNKNOWN, verify().at("/messageLinks/0/status").asText());
    }

    @ParameterizedTest @CsvSource({"Supplier<String>", "'Function<String,String>'"})
    void streamFunctionDeclarationsAreInventoriedOnce(String type) throws Exception {
        setup("@Bean " + type + " orders() { return null; }");
        input.withArray("messageDeclarations").add(item(1).put("kind", "stream"));
        var output = verify();
        assertEquals(Checks.CONFIRMED, output.at("/messageDeclarations/0/status").asText());
        assertEquals(1, output.at("/checks/message-declarations/declarationCount").asInt());
        assertEquals(0, output.at("/checks/message-declarations/warningCount").asInt());
    }

    @Test void namedRabbitExchangeRequiresTheBindingEvidence() throws Exception {
        setup("RabbitTemplate bus;\nbus.convertAndSend(\"events\", \"created\", value);\n@RabbitListener(queues = \"orders\") void receive(String value) {}\n"
                + "Binding binding() { return new Binding(\"orders\", Binding.DestinationType.QUEUE, \"events\", \"created\", null); }");
        message("messagePublications", "rabbit", "events/created", 2);
        message("messageSubscriptions", "rabbit", "orders", 3);
        link(2, 3);
        assertEquals(Checks.UNKNOWN, verify().at("/messageLinks/0/status").asText());
        ((ObjectNode)input.path("messageLinks").get(0)).withArray("evidence").add(FILE + ":4");
        assertEquals(Checks.CONFIRMED, verify().at("/messageLinks/0/status").asText());
    }
}
