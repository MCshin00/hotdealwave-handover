package io.hotdealwave.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static io.hotdealwave.extractor.ExtractorTest.find;

/** Only patterns absent from the pinned repository use a small independent input. */
class UnresolvedSourceTest {
    @TempDir Path root;

    @Test void localConstantsJsonAnnotationsCyclesAndUnresolvedMappings() throws Exception {
        Path service = root.resolve("sample");
        Files.createDirectories(service.resolve("src/main/java/sample"));
        Files.createDirectories(service.resolve("src/main/resources"));
        Files.writeString(service.resolve("build.gradle"), "");
        Files.writeString(service.resolve("src/main/resources/application.yml"), "spring:\n  application:\n    name: sample-service\n");
        Files.writeString(service.resolve("src/main/java/sample/Api.java"), """
                package sample;
                import missing.OtherModule;
                @RestController
                @RequestMapping(Paths.BASE)
                class Api {
                    @GetMapping(Paths.ITEM)
                    Wrapper<Item> get() { return null; }
                    @GetMapping(Paths.BASE + "/computed")
                    void computed() {}
                    @GetMapping(OtherModule.PATH)
                    void externalConstant() {}
                    @CustomMapping("/custom")
                    void custom() {}
                }
                class Paths {
                    static final String BASE = "/local";
                    static final String ITEM = "/item";
                }
                @Data class Wrapper<T> { T data; }
                class Item {
                    @JsonProperty("wire_name") @JsonNaming(Strategy.class) @Size(min = 2, max = 8) String name;
                    @JsonIgnore String secret;
                    Item next;
                    @Getter private Boolean isUsed;
                    @Getter private boolean isReady;
                    private boolean isValid;
                    public boolean isValid() { return isValid; }
                    public String visible;
                    @Getter @JsonNaming(Strategy.class) private String renamed;
                }
                @FeignClient(name = "sample-service")
                interface Client {
                    @GetMapping("/missing") void missing();
                }
                """);
        JsonNode analysis = new Extractor().analyze(root);
        JsonNode endpoint = find(analysis.path("endpoints"), "handler", "get");
        assertEquals("/local/item", endpoint.path("path").path("value").asText());
        assertEquals("확인됨", endpoint.path("path").path("status").asText());
        JsonNode item = find(endpoint.path("response").path("fields"), "name", "data").path("schema");
        assertEquals("wire_name", find(item.path("fields"), "name", "name").path("jsonName").path("value").asText());
        assertTrue(find(item.path("fields"), "name", "secret").path("jsonIgnored").path("value").asBoolean());
        assertTrue(find(item.path("fields"), "name", "next").path("schema").path("recursive").asBoolean());
        for (String name : new String[] {"isUsed", "isReady", "isValid", "visible"}) {
            JsonNode jsonName = find(item.path("fields"), "name", name).path("jsonName");
            String expected = switch (name) {
                case "isReady" -> "ready";
                case "isValid" -> "valid";
                default -> name;
            };
            assertEquals(expected, jsonName.path("value").asText());
            assertEquals("확인됨", jsonName.path("status").asText());
        }
        assertTrue(find(item.path("fields"), "name", "isValid").path("jsonName").path("evidence")
                .toString().contains("Api.java:27"));
        for (String name : new String[] {"next", "renamed"}) {
            JsonNode jsonName = find(item.path("fields"), "name", name).path("jsonName");
            assertEquals("확인 안 됨", jsonName.path("status").asText());
            assertTrue(jsonName.path("value").isNull());
            assertFalse(jsonName.path("reason").asText().isEmpty());
            assertFalse(jsonName.path("evidence").isEmpty());
        }
        for (String handler : new String[] {"computed", "externalConstant"}) {
            JsonNode path = find(analysis.path("endpoints"), "handler", handler).path("path");
            assertEquals("확인 안 됨", path.path("status").asText());
            assertTrue(path.path("value").isNull());
            assertFalse(path.path("evidence").isEmpty());
        }
        assertEquals(1, analysis.path("checks").path("declarations").path("warningCount").asInt());
        assertEquals(1, analysis.path("checks").path("feign-targets").path("warningCount").asInt());
        assertEquals(3, analysis.path("checks").path("gateway-routes").path("warningCount").asInt());
        assertTrue(StreamSupport.stream(analysis.path("warnings").spliterator(), false)
                .anyMatch(w -> w.path("reason").asText().contains("CustomMapping")));
    }
}
