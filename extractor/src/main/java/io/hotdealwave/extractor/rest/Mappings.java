package io.hotdealwave.extractor.rest;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import io.hotdealwave.extractor.model.Analysis;
import io.hotdealwave.extractor.model.Analysis.Fact;
import io.hotdealwave.extractor.source.SourceContext;
import io.hotdealwave.extractor.source.SourceValues;
import java.util.*;

public final class Mappings {
    private static final Map<String, String> METHODS = Map.of("GetMapping", "GET", "PostMapping", "POST",
            "PutMapping", "PUT", "PatchMapping", "PATCH", "DeleteMapping", "DELETE",
            "RequestMapping", "*");
    public record Mapping(Fact<String> method, Fact<String> path, List<String> evidence) {}
    private final SourceContext source;
    private final SourceValues values;
    public Mappings(SourceContext source) { this.source = source; values = new SourceValues(source); }
    public static boolean isMapping(AnnotationExpr a) { return METHODS.containsKey(SourceValues.name(a)); }

    public List<Mapping> read(NodeWithAnnotations<?> node) {
        var result = new ArrayList<Mapping>();
        for (var a : node.getAnnotations()) {
            if (!isMapping(a)) continue;
            var paths = SourceValues.argument(a, "path").or(() -> SourceValues.argument(a, "value"))
                    .map(values::values).orElseGet(() -> List.of(Fact.known("", source.evidence(a))));
            List<Fact<String>> methods = List.of(Fact.known(METHODS.get(SourceValues.name(a)), source.evidence(a)));
            var methodArg = SourceValues.argument(a, "method");
            if (methodArg.isPresent()) {
                var expr = methodArg.get();
                var expressions = expr.isArrayInitializerExpr() ? expr.asArrayInitializerExpr().getValues()
                        : new com.github.javaparser.ast.NodeList<>(expr);
                methods = expressions.stream().map(e -> {
                    String raw = e.toString();
                    String method = raw.substring(raw.lastIndexOf('.') + 1);
                    return Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE").contains(method)
                            ? Fact.known(method, source.evidence(e))
                            : Fact.<String>unknown(source.evidence(e), "HTTP 메서드를 확정할 수 없음: " + raw);
                }).toList();
                if (methods.isEmpty()) methods = List.of(Fact.known("*", source.evidence(a)));
            }
            for (var method : methods) for (var path : paths)
                result.add(new Mapping(method, path, source.evidence(a)));
        }
        return result;
    }
    public List<Mapping> combine(NodeWithAnnotations<?> type, NodeWithAnnotations<?> method) {
        var parents = read(type);
        if (parents.isEmpty()) parents = List.of(new Mapping(Fact.known("*", source.evidence((Node) type)),
                Fact.known("", source.evidence((Node) type)), List.of()));
        var result = new ArrayList<Mapping>();
        for (var parent : parents) for (var child : read(method)) {
            Fact<String> http = "*".equals(child.method().value()) ? parent.method() : child.method();
            result.add(new Mapping(http, join(parent.path(), child.path()),
                    Analysis.evidence(child.evidence(), parent.evidence())));
        }
        return result;
    }
    public static Fact<String> join(Fact<String> prefix, Fact<String> suffix) {
        var evidence = Analysis.evidence(prefix.evidence(), suffix.evidence());
        if (!prefix.known() || !suffix.known()) return Fact.unknown(evidence, "경로 일부를 확정할 수 없음");
        return Fact.known(normalize(prefix.value() + "/" + suffix.value()), evidence);
    }
    public static String normalize(String path) {
        String result = ("/" + path).replaceAll("/{2,}", "/");
        return result.length() > 1 && result.endsWith("/") ? result.substring(0, result.length() - 1) : result;
    }
}
