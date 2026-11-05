package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.util.*;
import static io.hotdealwave.verifier.Checks.*;

/** All HTTP kinds use the same service, method, path and target-evidence comparison. */
final class HttpChecks {
    static String host(String url) {
        try { return URI.create(url).getHost(); } catch (IllegalArgumentException e) { return null; }
    }
    private static String path(String url) {
        try { String path = URI.create(url).getPath(); return path == null || path.isEmpty() ? "/" : path; }
        catch (IllegalArgumentException e) { return null; }
    }
    void run(ObjectNode analysis) {
        var services = new HashSet<String>();
        for (var service : analysis.path("services")) if (known(service.path("name"))) services.add(value(service.path("name")));
        for (var endpoint : analysis.path("endpoints")) services.add(endpoint.path("service").asText());
        for (var call : analysis.path("httpCalls")) {
            var client = find(analysis.path("httpClients"), "id", call.path("clientId").asText());
            var target = call.path("target");
            String url = value(client.path("url")), callPath = value(call.path("path"));
            boolean hasClient = !call.path("clientId").asText().isEmpty();
            boolean absent = ABSENT.equals(call.path("path").path("status").asText())
                    || hasClient && ABSENT.equals(client.path("url").path("status").asText());
            ((ObjectNode)call).put("external", false);
            if (absent) { verdict(target, ABSENT, "실제 주소는 저장소 밖 설정값에서 정해짐"); continue; }
            if (callPath == null || !known(call.path("httpMethod")) || hasClient && (!known(client.path("url")) || !known(client))) {
                verdict(target, UNKNOWN, "클라이언트 주소·호출 경로·메서드를 확정할 수 없음");
                verdict(call, UNKNOWN, "하위 값을 확정할 수 없음");
                continue;
            }
            String address = callPath.contains("://") ? callPath : hasClient && url != null && !url.isEmpty()
                    ? url.replaceFirst("/$", "") + "/" + callPath.replaceFirst("^/", "") : callPath;
            String service = address.contains("://") ? host(address) : value(client.path("name"));
            String endpointPath = address.contains("://") ? path(address) : callPath;
            if (address.contains("://") && service != null && !services.contains(service) && !address.startsWith("lb://")) {
                ((ObjectNode)call).put("external", true);
                if (!UNKNOWN.equals(target.path("status").asText())) verdict(target, ABSENT, "명시된 외부 URL의 컨트롤러는 분석 저장소에 없음");
                continue;
            }
            var candidates = items(analysis.path("endpoints")).stream()
                    .filter(e -> known(e) && known(call) && known(e.path("path")) && known(e.path("httpMethod"))
                            && Objects.equals(e.path("service").asText(), service)
                            && (Objects.equals(value(e.path("httpMethod")), value(call.path("httpMethod"))) || "*".equals(value(e.path("httpMethod"))))
                            && PathMatcher.sameEndpoint(value(e.path("path")), endpointPath)).toList();
            if (candidates.size() == 1 && Objects.equals(value(target), candidates.get(0).path("id").asText())
                    && items(target.path("evidence")).contains(call.path("evidence").path(0))
                    && items(target.path("evidence")).contains(candidates.get(0).path("evidence").path(0))
                    && !UNKNOWN.equals(target.path("status").asText())) verdict(target, CONFIRMED, null);
            else {
                String reason = candidates.isEmpty() ? "같은 서비스 이름·HTTP 메서드·경로의 엔드포인트 없음"
                        : candidates.size() > 1 ? "동일한 대상 엔드포인트가 여러 개임" : "추출한 대상과 서비스·HTTP 메서드·경로가 일치하지 않음";
                verdict(target, UNKNOWN, reason);
                verdict(call, UNKNOWN, reason);
                warn(analysis, "http-targets", "target-mismatch", target.path("evidence"), reason);
            }
        }
        count(analysis, "http-targets");
    }
}
