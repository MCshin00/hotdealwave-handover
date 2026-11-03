package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import static io.hotdealwave.verifier.Checks.*;

/** Handler → Feign call or kafkaTemplate.send links; only the ends of each chain are checked. */
final class CallChecks {
    void run(SourceText source, ObjectNode analysis) throws IOException {
        for (var call : analysis.path("calls")) {
            String fromId = call.path("fromId").asText(), toId = call.path("toId").asText();
            var from = find(analysis.path("endpoints"), "id", fromId);
            if (from.isMissingNode()) from = find(analysis.path("kafkaSubscriptions"), "id", fromId);
            var feign = find(analysis.path("feignCalls"), "id", toId);
            var publication = find(analysis.path("kafkaPublications"), "id", toId);
            if (UNKNOWN.equals(call.path("status").asText())) continue; // evidence-lines already warned
            var evidence = items(call.path("evidence"));
            String reason = null;
            if (from.isMissingNode() || (feign.isMissingNode() && !"kafkaTemplate.send".equals(publication.path("kind").asText())))
                reason = "출발·도착 ID가 추출 결과에 없음";
            else if (!evidence.get(0).equals(from.path("evidence").path(0)))
                reason = "첫 근거가 출발 항목의 첫 근거와 다름";
            else {
                var last = evidence.get(evidence.size() - 1);
                boolean reaches = feign.isMissingNode() ? items(publication.path("evidence")).contains(last)
                        : Pattern.compile("\\.\\s*" + Pattern.quote(feign.path("handler").asText()) + "\\s*\\(")
                                .matcher(Objects.toString(source.line(last.asText()), "")).find();
                if (!reaches) reason = "마지막 근거가 도착 지점 줄이 아님";
            }
            verdict(call, reason == null ? CONFIRMED : UNKNOWN, reason);
            if (reason != null) warn(analysis, "handler-calls", call.path("evidence"), reason + ": " + fromId + " -> " + toId);
        }
        var targets = Stream.concat(items(analysis.path("feignCalls")).stream(), items(analysis.path("kafkaPublications")).stream()
                .filter(p -> "kafkaTemplate.send".equals(p.path("kind").asText()))).toList();
        for (var target : targets) {
            String id = target.path("id").asText();
            if (items(analysis.path("calls")).stream().noneMatch(c -> known(c) && c.path("toId").asText().equals(id)))
                warn(analysis, "handler-calls", target.path("evidence"), "호출하는 곳 확인 안 됨: " + id);
        }
        count(analysis, "handler-calls");
    }
}
