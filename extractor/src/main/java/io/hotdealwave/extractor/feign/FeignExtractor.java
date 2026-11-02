package io.hotdealwave.extractor.feign;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import io.hotdealwave.extractor.model.Analysis;
import io.hotdealwave.extractor.model.Analysis.*;
import io.hotdealwave.extractor.rest.Mappings;
import io.hotdealwave.extractor.source.SourceContext;
import io.hotdealwave.extractor.source.SourceValues;

public final class FeignExtractor {
    public void extract(SourceContext source, Analysis result) {
        var values = new SourceValues(source);
        var mappings = new Mappings(source);
        for (var unit : source.units) for (var type : unit.findAll(ClassOrInterfaceDeclaration.class)) {
            var annotation = type.getAnnotations().stream().filter(a -> SourceValues.name(a).equals("FeignClient")).findFirst();
            if (annotation.isEmpty()) continue;
            var a = annotation.get();
            var name = SourceValues.argument(a, "name").or(() -> SourceValues.argument(a, "value"))
                    .map(values::value).orElseGet(() -> Fact.<String>unknown(source.evidence(a), "Feign 서비스 이름 없음"));
            var url = values.attribute(a, "url", "");
            var path = values.attribute(a, "path", "");
            var client = new FeignClient(source.evidence(a).get(0), source.serviceName(), type.getNameAsString(),
                    name, url, path, name.known() && url.known() ? Analysis.CONFIRMED : Analysis.UNKNOWN, source.evidence(a));
            result.feignClients.add(client);
            type.getAnnotations().stream().filter(Mappings::isMapping).forEach(m -> result.declarations.add(
                    new Declaration(SourceValues.name(m), Analysis.CONFIRMED, source.evidence(m))));
            for (var method : type.getMethods()) {
                var combined = mappings.combine(type, method);
                if (!combined.isEmpty()) method.getAnnotations().stream().filter(Mappings::isMapping).forEach(m -> result.declarations.add(
                        new Declaration(SourceValues.name(m), Analysis.CONFIRMED, source.evidence(m))));
                for (var mapping : combined) {
                    var call = new FeignCall();
                    call.id = mapping.evidence().get(0) + "#" + result.feignCalls.size();
                    call.clientId = client.id();
                    call.service = source.serviceName();
                    call.handler = method.getNameAsString();
                    call.httpMethod = mapping.method();
                    call.path = Mappings.join(path, mapping.path());
                    call.evidence = Analysis.evidence(mapping.evidence(), client.evidence());
                    call.external = url.known() && !url.value().isEmpty();
                    call.status = call.path.known() && call.httpMethod.known() && url.known()
                            && (call.external || name.known()) ? Analysis.CONFIRMED : Analysis.UNKNOWN;
                    result.feignCalls.add(call);
                }
            }
        }
    }
}
