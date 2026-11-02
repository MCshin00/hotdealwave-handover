package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.regex.Pattern;
import static io.hotdealwave.verifier.Checks.*;

final class CrossChecks {
    // Comments remain part of the independent declaration inventory.
    private static final Pattern DECLARATION = Pattern.compile("@(?:[\\w]+\\.)*([\\w$]*Mapping|FeignClient)\\b");
    private static final Pattern PREFIX_REWRITE = Pattern.compile("^(/[^\\s(),?*]+/)\\(\\?<segment>\\.\\*\\),\\s*/\\$\\\\?\\{segment}$");

    void run(Map<String, String> lines, ObjectNode analysis) {
        var raw = new LinkedHashMap<String, String>();
        for (var line : lines.entrySet()) {
            var matcher = DECLARATION.matcher(line.getValue());
            while (matcher.find()) raw.put(matcher.group(1) + "@" + line.getKey(), matcher.group(1));
        }
        var extracted = new LinkedHashMap<String, String>();
        for (var declaration : analysis.path("declarations"))
            extracted.put(declaration.path("kind").asText() + "@" + declaration.path("evidence").path(0).asText(), declaration.path("kind").asText());
        for (var client : analysis.path("feignClients"))
            extracted.put("FeignClient@" + client.path("evidence").path(0).asText(), "FeignClient");
        inventory(analysis, "declarations", raw, extracted);
        var counts = count(analysis, "declarations");
        counts.put("mappingCount", raw.size() - Collections.frequency(new ArrayList<>(raw.values()), "FeignClient"));
        counts.put("feignClientCount", Collections.frequency(new ArrayList<>(raw.values()), "FeignClient"));
        counts.put("extractedMappingCount", extracted.size() - Collections.frequency(new ArrayList<>(extracted.values()), "FeignClient"));
        counts.put("extractedFeignClientCount", analysis.path("feignClients").size());

        for (var call : analysis.path("feignCalls")) {
            var client = find(analysis.path("feignClients"), "id", call.path("clientId").asText());
            var target = call.path("target");
            String url = value(client.path("url"));
            boolean external = known(client.path("url")) && url != null && !url.isEmpty();
            ((ObjectNode) call).put("external", external);
            if (external) {
                if (!UNKNOWN.equals(target.path("status").asText()))
                    verdict(target, ABSENT, "명시된 외부 URL의 컨트롤러는 분석 저장소에 없음");
                continue;
            }
            var candidates = items(analysis.path("endpoints")).stream()
                    .filter(e -> known(e) && known(call) && known(e.path("path")) && known(e.path("httpMethod"))
                            && known(client.path("name")) && known(client.path("url"))
                            && known(call.path("path")) && known(call.path("httpMethod"))
                            && Objects.equals(e.path("service").asText(), value(client.path("name")))
                            && (Objects.equals(value(e.path("httpMethod")), value(call.path("httpMethod"))) || "*".equals(value(e.path("httpMethod"))))
                            && PathMatcher.sameEndpoint(value(e.path("path")), value(call.path("path"))))
                    .toList();
            if (candidates.size() == 1 && Objects.equals(value(target), candidates.get(0).path("id").asText())
                    && items(target.path("evidence")).contains(call.path("evidence").path(0))
                    && items(target.path("evidence")).contains(candidates.get(0).path("evidence").path(0))
                    && !UNKNOWN.equals(target.path("status").asText())) verdict(target, CONFIRMED, null);
            else {
                String reason = candidates.isEmpty() ? "같은 서비스 이름·HTTP 메서드·경로의 엔드포인트 없음"
                        : candidates.size() > 1 ? "동일한 대상 엔드포인트가 여러 개임" : "추출한 대상과 서비스·HTTP 메서드·경로가 일치하지 않음";
                verdict(target, UNKNOWN, reason);
                verdict(call, UNKNOWN, reason);
                warn(analysis, "feign-targets", target.path("evidence"), reason);
            }
        }
        count(analysis, "feign-targets");

        for (var endpoint : analysis.path("endpoints")) {
            for (var external : endpoint.path("externalPaths")) {
                var route = find(analysis.path("gatewayRoutes"), "id", external.path("routeId").asText());
                String path = external.path("path").asText();
                boolean connected = known(endpoint) && known(route) && known(endpoint.path("path")) && known(route.path("targetService"))
                        && items(external.path("evidence")).contains(endpoint.path("evidence").path(0))
                        && items(external.path("evidence")).contains(route.path("evidence").path(0))
                        && Objects.equals(endpoint.path("service").asText(), value(route.path("targetService")))
                        && items(route.path("paths")).stream().anyMatch(p -> known(p) && PathMatcher.matches(value(p), path))
                        && Objects.equals(value(endpoint.path("path")), rewritten(route, path))
                        && !UNKNOWN.equals(external.path("status").asText());
                verdict(external, connected ? CONFIRMED : UNKNOWN, connected ? null : "외부 경로의 대상·Path·RewritePath가 일치하지 않음");
                token(analysis, external.path("tokenRequired"), path, value(endpoint.path("httpMethod")), connected);
            }
            boolean connected = items(endpoint.path("externalPaths")).stream().anyMatch(Checks::known);
            token(analysis, endpoint.path("tokenRequired"), value(endpoint.path("path")), value(endpoint.path("httpMethod")), connected);
            if (!connected) {
                warn(analysis, "gateway-routes", endpoint.path("evidence"), "확인된 게이트웨이 외부 경로 없음: "
                        + value(endpoint.path("httpMethod")) + " " + value(endpoint.path("path")));
                verdict(endpoint, UNKNOWN, "확인된 게이트웨이 외부 경로 없음");
            }
        }
        for (var route : analysis.path("gatewayRoutes")) {
            var inverse = PREFIX_REWRITE.matcher(Objects.toString(value(route.path("rewritePath")), ""));
            String prefix = inverse.matches() ? inverse.group(1) : "";
            for (var path : route.path("paths")) {
                boolean connected = known(route) && known(path) && known(route.path("targetService")) && items(analysis.path("endpoints")).stream()
                        .filter(e -> known(e.path("path")) && Objects.equals(e.path("service").asText(), value(route.path("targetService"))))
                        .anyMatch(e -> {
                            String endpointPath = value(e.path("path"));
                            var candidates = new LinkedHashSet<String>();
                            candidates.add(endpointPath);
                            if (!prefix.isEmpty()) candidates.add(prefix + endpointPath.substring(1));
                            return candidates.stream().anyMatch(candidate -> PathMatcher.matches(value(path), candidate)
                                    && Objects.equals(endpointPath, rewritten(route, candidate)));
                        });
                if (!connected) {
                    var evidence = route.path("evidence").deepCopy();
                    for (var ev : path.path("evidence")) if (!items(evidence).contains(ev)) ((com.fasterxml.jackson.databind.node.ArrayNode) evidence).add(ev);
                    warn(analysis, "gateway-routes", evidence, "컨트롤러에 닿는 외부 경로를 확인하지 못함: " + value(path));
                    verdict(route, UNKNOWN, "컨트롤러에 닿는 외부 경로를 확인하지 못함");
                }
            }
        }
        count(analysis, "gateway-routes");
    }

    private static String rewritten(JsonNode route, String path) {
        if (!known(route.path("rewritePath"))) return null;
        String value = value(route.path("rewritePath"));
        if (value.isEmpty()) return path;
        var rewrite = PREFIX_REWRITE.matcher(value);
        if (!rewrite.matches()) return null;
        return path.startsWith(rewrite.group(1)) ? "/" + path.substring(rewrite.group(1).length()) : path;
    }

    private static void token(ObjectNode analysis, JsonNode fact, String path, String method, boolean connected) {
        if (fact.isMissingNode()) return;
        var allowedPaths = items(analysis.path("allowedPaths"));
        if (!connected || path == null || method == null || method.equals("*") || allowedPaths.isEmpty()
                || allowedPaths.stream().anyMatch(a -> !known(a.path("path")) || !known(a.path("httpMethod")))
                || UNKNOWN.equals(fact.path("status").asText())) {
            verdict(fact, UNKNOWN, "외부 경로·허용 목록·HTTP 메서드를 확인하지 못함");
            return;
        }
        boolean required = allowedPaths.stream().noneMatch(a -> path.startsWith(value(a.path("path")))
                && ("*".equals(value(a.path("httpMethod"))) || method.equalsIgnoreCase(value(a.path("httpMethod")))));
        if (fact.path("value").isBoolean() && fact.path("value").asBoolean() == required) verdict(fact, CONFIRMED, null);
        else {
            verdict(fact, UNKNOWN, "토큰 허용 목록과 값이 일치하지 않음");
            warn(analysis, "gateway-routes", fact.path("evidence"), "토큰 허용 목록과 값이 일치하지 않음");
        }
    }

    static void inventory(ObjectNode analysis, String check, Map<String, String> raw, Map<String, String> extracted) {
        for (var entry : raw.entrySet()) if (!extracted.containsKey(entry.getKey()))
            inventoryWarning(analysis, check, entry, "단순 검색에서 찾은 선언이 추출 결과에 없음: ");
        for (var entry : extracted.entrySet()) if (!raw.containsKey(entry.getKey()))
            inventoryWarning(analysis, check, entry, "추출한 선언을 단순 검색에서 찾지 못함: ");
    }
    private static void inventoryWarning(ObjectNode analysis, String check, Map.Entry<String, String> entry, String reason) {
        var evidence = analysis.arrayNode().add(entry.getKey().substring(entry.getKey().indexOf('@') + 1));
        warn(analysis, check, evidence, reason + entry.getValue());
    }
}
