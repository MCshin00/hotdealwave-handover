package io.hotdealwave.extractor.check;

import io.hotdealwave.extractor.link.PathMatcher;
import io.hotdealwave.extractor.model.Analysis;
import io.hotdealwave.extractor.model.Analysis.*;
import io.hotdealwave.extractor.source.SourceContext;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;

public final class CrossChecks {
    // Intentionally independent from JavaParser. Comments are not silently filtered out.
    private static final Pattern DECLARATION = Pattern.compile("@(?:[\\w]+\\.)*([\\w$]*Mapping|FeignClient)\\b");
    // Alias reachability is checked without publishing aliases or their token decisions.
    private static final Pattern PREFIX_REWRITE = Pattern.compile("^(/[^\\s(),?*]+/)\\(\\?<segment>\\.\\*\\),\\s*/\\$\\\\?\\{segment}$");

    public void run(List<SourceContext> sources, Analysis analysis) throws IOException {
        var raw = new LinkedHashMap<String, Declaration>();
        for (var source : sources) for (var file : source.files) {
            var lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                var matcher = DECLARATION.matcher(lines.get(i));
                while (matcher.find()) {
                    var declaration = new Declaration(matcher.group(1), Analysis.CONFIRMED,
                            List.of(source.relative(file) + ":" + (i + 1)));
                    raw.put(key(declaration), declaration);
                }
            }
        }
        var extracted = new LinkedHashMap<String, Declaration>();
        for (var declaration : analysis.declarations) extracted.put(key(declaration), declaration);
        for (var client : analysis.feignClients) {
            var declaration = new Declaration("FeignClient", client.status(), client.evidence());
            extracted.put(key(declaration), declaration);
        }
        for (var entry : raw.entrySet()) if (!extracted.containsKey(entry.getKey()))
            warn(analysis, "declarations", entry.getValue().evidence(), "단순 검색에서 찾은 선언이 추출 결과에 없음: " + entry.getValue().kind());
        for (var entry : extracted.entrySet()) if (!raw.containsKey(entry.getKey()))
            warn(analysis, "declarations", entry.getValue().evidence(), "추출한 선언을 단순 검색에서 찾지 못함: " + entry.getValue().kind());

        for (var call : analysis.feignCalls) if (!call.external && !call.target.known())
            warn(analysis, "feign-targets", call.target.evidence(), call.target.reason());

        for (var endpoint : analysis.endpoints) if (endpoint.externalPaths.isEmpty())
            warn(analysis, "gateway-routes", endpoint.evidence, "확인된 게이트웨이 외부 경로 없음: "
                    + endpoint.httpMethod.value() + " " + endpoint.path.value());
        for (var route : analysis.gatewayRoutes) {
            var inverse = PREFIX_REWRITE.matcher(route.rewritePath().value() == null ? "" : route.rewritePath().value());
            String prefix = inverse.matches() ? inverse.group(1) : "";
            for (var path : route.paths()) {
                boolean connected = path.known() && route.targetService().known() && analysis.endpoints.stream()
                        .filter(e -> e.path.known() && Objects.equals(e.service, route.targetService().value()))
                        .anyMatch(e -> {
                            var candidates = new LinkedHashSet<String>();
                            candidates.add(e.path.value());
                            if (!prefix.isEmpty()) candidates.add(prefix + e.path.value().substring(1));
                            return candidates.stream().anyMatch(candidate -> PathMatcher.matches(path.value(), candidate)
                                    && Objects.equals(e.path.value(), rewritten(route, candidate)));
                        });
                if (!connected) warn(analysis, "gateway-routes", Analysis.evidence(route.evidence(), path.evidence()),
                        "컨트롤러에 닿는 외부 경로를 확인하지 못함: " + path.value());
            }
        }
        for (String name : List.of("declarations", "feign-targets", "gateway-routes")) {
            Map<String, Integer> counts = new LinkedHashMap<>();
            counts.put("warningCount", (int) analysis.warnings.stream().filter(w -> w.check().equals(name)).count());
            if (name.equals("declarations")) {
                counts.put("mappingCount", (int) raw.values().stream().filter(d -> d.kind().endsWith("Mapping")).count());
                counts.put("feignClientCount", (int) raw.values().stream().filter(d -> d.kind().equals("FeignClient")).count());
                counts.put("extractedMappingCount", (int) extracted.values().stream().filter(d -> d.kind().endsWith("Mapping")).count());
                counts.put("extractedFeignClientCount", analysis.feignClients.size());
            }
            analysis.checks.put(name, counts);
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
    private static String key(Declaration d) { return d.kind() + "@" + d.evidence().get(0); }
    private static void warn(Analysis analysis, String check, List<String> evidence, String reason) {
        analysis.warnings.add(new Warning(check, Analysis.UNKNOWN, evidence, reason));
    }
}
