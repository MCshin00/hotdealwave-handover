package io.hotdealwave.extractor.source;

import io.hotdealwave.extractor.model.Analysis.Fact;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

/** Compose nodes rather than loading arbitrary Java objects; keep YAML line marks. */
public final class YamlSource {
    private final String file;
    public final Node root;

    public YamlSource(Path repository, Path path) throws IOException {
        file = repository.relativize(path).toString().replace('\\', '/');
        try (var reader = Files.newBufferedReader(path)) {
            root = new Yaml().compose(reader);
        }
    }
    public static Node get(Node node, String... keys) {
        for (String key : keys) {
            if (!(node instanceof MappingNode mapping)) return null;
            node = mapping.getValue().stream()
                    .filter(t -> t.getKeyNode() instanceof ScalarNode s && s.getValue().equals(key))
                    .map(t -> t.getValueNode()).findFirst().orElse(null);
        }
        return node;
    }
    public static List<Node> items(Node node) {
        return node instanceof SequenceNode s ? s.getValue() : List.of();
    }
    public List<String> evidence(Node node) {
        return List.of(file + ":" + (node == null ? 1 : node.getStartMark().getLine() + 1));
    }
    public Fact<String> text(Node node) {
        if (!(node instanceof ScalarNode scalar)) return Fact.unknown(evidence(node), "문자열 값 없음");
        String value = scalar.getValue();
        if (value.contains("${")) return Fact.unknown(evidence(node), "환경에서 정해지는 값");
        return Fact.known(value, evidence(node));
    }
}
