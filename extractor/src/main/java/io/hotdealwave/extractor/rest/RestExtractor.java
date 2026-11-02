package io.hotdealwave.extractor.rest;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import io.hotdealwave.extractor.model.Analysis;
import io.hotdealwave.extractor.model.Analysis.*;
import io.hotdealwave.extractor.source.SourceContext;
import io.hotdealwave.extractor.source.SourceValues;
import java.util.*;

public final class RestExtractor {
    public void extract(SourceContext source, Analysis result) {
        var mappings = new Mappings(source);
        var dtos = new DtoExtractor(source);
        var values = new SourceValues(source);
        for (var unit : source.units) for (var type : unit.findAll(ClassOrInterfaceDeclaration.class)) {
            if (type.getAnnotations().stream().noneMatch(a -> Set.of("RestController", "Controller").contains(SourceValues.name(a)))) continue;
            type.getAnnotations().stream().filter(Mappings::isMapping).forEach(a -> result.declarations.add(
                    new Declaration(SourceValues.name(a), Analysis.CONFIRMED, source.evidence(a))));
            for (var method : type.getMethods()) {
                var combined = mappings.combine(type, method);
                if (!combined.isEmpty()) method.getAnnotations().stream().filter(Mappings::isMapping).forEach(a -> result.declarations.add(
                        new Declaration(SourceValues.name(a), Analysis.CONFIRMED, source.evidence(a))));
                for (var mapping : combined) {
                    var endpoint = new Endpoint();
                    endpoint.id = mapping.evidence().get(0) + "#" + result.endpoints.size();
                    endpoint.service = source.serviceName();
                    endpoint.controller = type.getNameAsString();
                    endpoint.handler = method.getNameAsString();
                    endpoint.httpMethod = mapping.method();
                    endpoint.path = mapping.path();
                    endpoint.evidence = mapping.evidence();
                    endpoint.status = endpoint.path.known() && endpoint.httpMethod.known() ? Analysis.CONFIRMED : Analysis.UNKNOWN;
                    endpoint.response = dtos.extract(method.getType());
                    for (var parameter : method.getParameters()) {
                        if (parameter.getAnnotations().stream().anyMatch(a -> SourceValues.name(a).equals("AuthenticationPrincipal"))) continue;
                        String kind = null;
                        String binding = null;
                        String name = parameter.getNameAsString();
                        String status = Analysis.CONFIRMED;
                        for (var a : parameter.getAnnotations()) {
                            String annotation = SourceValues.name(a);
                            String candidate = switch (annotation) {
                                case "PathVariable" -> "경로";
                                case "RequestParam", "ModelAttribute", "PageableDefault" -> "쿼리";
                                case "RequestBody", "RequestPart" -> "본문";
                                case "RequestHeader" -> "헤더";
                                default -> null;
                            };
                            if (candidate == null) continue;
                            kind = candidate;
                            binding = annotation;
                            var declared = SourceValues.argument(a, "name").or(() -> SourceValues.argument(a, "value"));
                            if (declared.isPresent()) {
                                var resolved = values.value(declared.get());
                                if (!resolved.known()) { name = null; status = Analysis.UNKNOWN; }
                                else if (!resolved.value().isEmpty()) name = resolved.value();
                            }
                        }
                        if (kind == null && parameter.getType().asString().equals("Pageable")
                                && unit.getImports().stream().anyMatch(i -> i.getNameAsString().equals("org.springframework.data.domain.Pageable"))) {
                            kind = "쿼리";
                            binding = "Pageable";
                        }
                        // Keep the declared Pageable type; do not invent framework default fields.
                        if (kind == null) continue;
                        endpoint.parameters.add(new Parameter(name, kind, binding, dtos.extract(parameter.getType()),
                                parameter.getAnnotations().stream().map(values::annotation).toList(), status, source.evidence(parameter)));
                    }
                    result.endpoints.add(endpoint);
                }
            }
        }
    }
}
