package io.hotdealwave.extractor.link;

import io.hotdealwave.extractor.model.Analysis;
import io.hotdealwave.extractor.model.Analysis.*;
import java.util.*;

public final class RestLinker {
    public void link(Analysis analysis) {
        for (var call : analysis.feignCalls) {
            var client = analysis.feignClients.stream().filter(c -> c.id().equals(call.clientId)).findFirst().orElseThrow();
            if (call.external) {
                call.target = new Fact<>(null, Analysis.ABSENT, call.evidence, "명시된 외부 URL의 컨트롤러는 분석 저장소에 없음");
                continue;
            }
            if (!client.url().known() || !client.name().known() || !call.path.known() || !call.httpMethod.known()) {
                call.target = Fact.unknown(call.evidence, "호출의 서비스·경로·HTTP 메서드를 확정할 수 없음");
                continue;
            }
            var targets = analysis.endpoints.stream().filter(e -> Objects.equals(e.service, client.name().value())
                    && e.httpMethod.known() && e.path.known()
                    && (Objects.equals(e.httpMethod.value(), call.httpMethod.value()) || "*".equals(e.httpMethod.value()))
                    && PathMatcher.sameEndpoint(e.path.value(), call.path.value())).toList();
            if (targets.size() == 1) {
                var target = targets.get(0);
                var service = analysis.services.stream().filter(s -> Objects.equals(s.name().value(), target.service)).findFirst().orElseThrow();
                call.target = Fact.known(target.id, Analysis.evidence(call.evidence, client.name().evidence(),
                        service.evidence(), target.evidence));
            } else call.target = Fact.unknown(call.evidence, targets.isEmpty()
                    ? "같은 서비스 이름·HTTP 메서드·경로의 엔드포인트 없음" : "동일한 대상 엔드포인트가 여러 개임");
        }
        for (var endpoint : analysis.endpoints) {
            if (endpoint.path.known()) for (var route : analysis.gatewayRoutes) {
                if (!route.targetService().known() || !Objects.equals(route.targetService().value(), endpoint.service)) continue;
                for (var pattern : route.paths()) {
                    if (!pattern.known() || !PathMatcher.matches(pattern.value(), endpoint.path.value())) continue;
                    var evidence = Analysis.evidence(endpoint.evidence, route.evidence(), pattern.evidence(), route.rewritePath().evidence());
                    var required = token(analysis, endpoint.path.value(), endpoint.httpMethod.value(), evidence);
                    if (endpoint.externalPaths.stream().noneMatch(p -> p.path().equals(endpoint.path.value())))
                        endpoint.externalPaths.add(new ExternalPath(route.id(), endpoint.path.value(), required, Analysis.CONFIRMED, evidence));
                }
            }
            endpoint.tokenRequired = endpoint.externalPaths.stream()
                    .findFirst().map(ExternalPath::tokenRequired)
                    .orElseGet(() -> Fact.unknown(endpoint.evidence, "확인된 외부 경로 없음"));
        }
    }

    private Fact<Boolean> token(Analysis analysis, String path, String method, List<String> routeEvidence) {
        if (method == null || "*".equals(method) || analysis.allowedPaths.isEmpty())
            return Fact.unknown(routeEvidence, "토큰 허용 목록 또는 HTTP 메서드를 확정할 수 없음");
        var evidence = new ArrayList<>(routeEvidence);
        for (var allowed : analysis.allowedPaths) {
            // AllowedPath.matches uses startsWith, not an Ant pattern or segment boundary.
            if (allowed.path().known() && allowed.httpMethod().known()
                    && path.startsWith(allowed.path().value())
                    && ("*".equals(allowed.httpMethod().value()) || method.equalsIgnoreCase(allowed.httpMethod().value())))
                return Fact.known(false, Analysis.evidence(routeEvidence, allowed.evidence()));
            evidence.addAll(allowed.evidence());
        }
        if (analysis.allowedPaths.stream().anyMatch(p -> !p.path().known() || !p.httpMethod().known()))
            return Fact.unknown(evidence, "허용 목록에 해석되지 않은 값 있음");
        return Fact.known(true, evidence);
    }
}
