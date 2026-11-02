package io.hotdealwave.extractor.link;

import io.hotdealwave.extractor.model.Analysis;
import io.hotdealwave.extractor.model.Analysis.*;
import java.util.*;
import java.util.regex.Pattern;

public final class RestLinker {
    // Invert only the prefix-stripping rewrite present in the pinned repository.
    private static final Pattern PREFIX_REWRITE = Pattern.compile("^(/[^\\s(),?*]+/)\\(\\?<segment>\\.\\*\\),\\s*/\\$\\\\?\\{segment}$");

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
                var candidates = new LinkedHashSet<String>();
                candidates.add(endpoint.path.value());
                var inverse = PREFIX_REWRITE.matcher(route.rewritePath().value() == null ? "" : route.rewritePath().value());
                if (inverse.matches()) candidates.add(inverse.group(1) + endpoint.path.value().substring(1));
                for (String candidate : candidates) for (var pattern : route.paths()) {
                    if (!pattern.known() || !PathMatcher.matches(pattern.value(), candidate)) continue;
                    var forward = rewritten(route, candidate);
                    if (!Objects.equals(endpoint.path.value(), forward)) continue;
                    var evidence = Analysis.evidence(endpoint.evidence, route.evidence(), pattern.evidence(), route.rewritePath().evidence());
                    var before = token(analysis, candidate, endpoint.httpMethod.value(), evidence);
                    var after = token(analysis, forward, endpoint.httpMethod.value(), evidence);
                    Fact<Boolean> required = before;
                    if (!candidate.equals(forward) && (!before.known() || !after.known() || !Objects.equals(before.value(), after.value())))
                        required = Fact.unknown(Analysis.evidence(before.evidence(), after.evidence()),
                                "재작성 전·후의 토큰 판정이 다름; 필터 실행 순서는 추출 범위에서 확정하지 않음"
                                        + " (재작성 전: " + before.value() + ", 후: " + after.value() + ")");
                    if (endpoint.externalPaths.stream().noneMatch(p -> p.path().equals(candidate)))
                        endpoint.externalPaths.add(new ExternalPath(route.id(), candidate, required, Analysis.CONFIRMED, evidence));
                }
            }
            // The direct API path is the representative path; retain per-alias token decisions too.
            endpoint.tokenRequired = endpoint.externalPaths.stream()
                    .filter(p -> p.path().equals(endpoint.path.value())).findFirst()
                    .or(() -> endpoint.externalPaths.stream().findFirst()).map(ExternalPath::tokenRequired)
                    .orElseGet(() -> Fact.unknown(endpoint.evidence, "확인된 외부 경로 없음"));
        }
    }

    private String rewritten(Route route, String path) {
        if (!route.rewritePath().known()) return null;
        String value = route.rewritePath().value();
        if (value.isEmpty()) return path;
        var rewrite = PREFIX_REWRITE.matcher(value);
        if (!rewrite.matches()) return null;
        return path.startsWith(rewrite.group(1)) ? "/" + path.substring(rewrite.group(1).length()) : path;
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
