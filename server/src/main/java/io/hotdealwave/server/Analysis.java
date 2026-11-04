package io.hotdealwave.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** The verifier output, the only input of the server. */
@Component
class Analysis {
    static final String CONFIRMED = "확인됨";
    private final JsonNode root;

    Analysis(@Value("${analysis.path:out/analysis.json}") String path) throws IOException {
        root = new ObjectMapper().readTree(Path.of(path).toFile());
    }

    List<JsonNode> list(String key) { return listOf(root.path(key)); }

    static List<JsonNode> listOf(JsonNode array) { return StreamSupport.stream(array.spliterator(), false).toList(); }

    JsonNode find(String key, String id) {
        return list(key).stream().filter(n -> n.path("id").asText().equals(id)).findFirst()
                .orElse(MissingNode.getInstance());
    }

    JsonNode checks() { return root.path("checks"); }

    static boolean known(JsonNode node) { return CONFIRMED.equals(node.path("status").asText()); }

    static String value(JsonNode fact) { return fact.path("value").isValueNode() ? fact.path("value").asText() : null; }
}
