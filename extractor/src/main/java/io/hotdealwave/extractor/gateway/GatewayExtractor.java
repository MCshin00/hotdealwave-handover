package io.hotdealwave.extractor.gateway;

import com.github.javaparser.ast.body.EnumDeclaration;
import io.hotdealwave.extractor.model.Analysis;
import io.hotdealwave.extractor.model.Analysis.*;
import io.hotdealwave.extractor.source.SourceContext;
import io.hotdealwave.extractor.source.SourceValues;
import io.hotdealwave.extractor.source.YamlSource;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import static io.hotdealwave.extractor.source.YamlSource.*;

public final class GatewayExtractor {
    public void extract(Path root, SourceContext source, Analysis analysis) throws IOException {
        var yaml = new YamlSource(root, source.service.directory().resolve("src/main/resources/application.yml"));
        for (Node node : items(get(yaml.root, "spring", "cloud", "gateway", "routes"))) {
            var id = yaml.text(get(node, "id"));
            var uri = yaml.text(get(node, "uri"));
            Fact<String> service = uri.known() && uri.value().startsWith("lb://")
                    ? Fact.known(uri.value().substring(5), uri.evidence())
                    : Fact.unknown(uri.evidence(), "lb:// 서비스 대상으로 확정할 수 없음");
            var paths = new ArrayList<Fact<String>>();
            for (Node predicate : items(get(node, "predicates"))) {
                if (predicate instanceof ScalarNode scalar && scalar.getValue().startsWith("Path=")) {
                    for (String pattern : scalar.getValue().substring(5).split(",")) {
                        paths.add(pattern.contains("${") ? Fact.unknown(yaml.evidence(predicate), "환경에서 정해지는 경로")
                                : Fact.known(pattern.trim(), yaml.evidence(predicate)));
                    }
                }
            }
            Fact<String> rewrite = Fact.known("", yaml.evidence(node));
            for (Node filter : items(get(node, "filters"))) {
                if (filter instanceof ScalarNode scalar && scalar.getValue().startsWith("RewritePath="))
                    rewrite = Fact.known(scalar.getValue().substring("RewritePath=".length()), yaml.evidence(filter));
            }
            analysis.gatewayRoutes.add(new Route(id.value(), uri, service, paths, rewrite,
                    id.known() && service.known() && paths.stream().allMatch(Fact::known) ? Analysis.CONFIRMED : Analysis.UNKNOWN,
                    yaml.evidence(node)));
        }
        var values = new SourceValues(source);
        for (var unit : source.units) for (var type : unit.findAll(EnumDeclaration.class)) {
            if (!type.getNameAsString().equals("AllowedPath")) continue;
            for (var entry : type.getEntries()) {
                var args = entry.getArguments();
                Fact<String> path = args.size() > 0 ? values.value(args.get(0))
                        : Fact.unknown(source.evidence(entry), "경로 인자 없음");
                Fact<String> method = args.size() > 1 ? values.value(args.get(1))
                        : Fact.unknown(source.evidence(entry), "메서드 인자 없음");
                analysis.allowedPaths.add(new AllowedPath(entry.getNameAsString(), path, method,
                        path.known() && method.known() ? Analysis.CONFIRMED : Analysis.UNKNOWN, source.evidence(entry)));
            }
        }
    }
}
