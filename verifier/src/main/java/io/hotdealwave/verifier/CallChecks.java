package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import static io.hotdealwave.verifier.Checks.*;

/** Handler -> HTTP call or directly invoked message publication links; only the ends of each chain are checked. */
final class CallChecks {
    void run(SourceText source, ObjectNode analysis) throws IOException {
        var sends = new java.util.ArrayList<com.fasterxml.jackson.databind.JsonNode>();
        for (var publication : analysis.path("messagePublications"))
            if (CommunicationRules.callable(source, publication)) sends.add(publication);
        for (var call : analysis.path("calls")) {
            String fromId = call.path("fromId").asText(), toId = call.path("toId").asText();
            var from = find(analysis.path("endpoints"), "id", fromId);
            if (from.isMissingNode()) from = find(analysis.path("messageSubscriptions"), "id", fromId);
            var http = find(analysis.path("httpCalls"), "id", toId);
            var publication = find(analysis.path("messagePublications"), "id", toId);
            if (UNKNOWN.equals(call.path("status").asText())) continue; // evidence-lines already warned
            var evidence = items(call.path("evidence"));
            String reason = null;
            if (from.isMissingNode() || (http.isMissingNode() && !sends.contains(publication)))
                reason = "출발·도착 ID가 추출 결과에 없음";
            else if (!(http.isMissingNode() ? publication : http).path("service").asText().isEmpty()
                    && !from.path("service").equals((http.isMissingNode() ? publication : http).path("service")))
                reason = "출발·도착이 같은 서비스가 아님";
            else if (!evidence.get(0).equals(from.path("evidence").path(0)))
                reason = "첫 근거가 출발 항목의 첫 근거와 다름";
            else {
                var last = evidence.get(evidence.size() - 1);
                // Publications may share a wrapper's send line, so the chain must also hold a line only this publication has.
                boolean reaches = http.isMissingNode() ? last.equals(publication.path("evidence").path(0))
                        && evidence.stream().anyMatch(line -> items(publication.path("evidence")).contains(line)
                                && sends.stream().noneMatch(p -> p != publication && items(p.path("evidence")).contains(line)))
                        : !CommunicationRules.interfaceCall(http) ? last.equals(http.path("evidence").path(0))
                        : Pattern.compile("\\.\\s*" + Pattern.quote(http.path("handler").asText()) + "\\s*\\(")
                                .matcher(Objects.toString(source.line(last.asText()), "")).find();
                if (!reaches) reason = "마지막 근거가 도착 지점 줄이 아님";
            }
            verdict(call, reason == null ? CONFIRMED : UNKNOWN, reason);
            if (reason != null) warn(analysis, "handler-calls", "extraction-mismatch", call.path("evidence"), reason + ": " + fromId + " -> " + toId);
        }
        var targets = Stream.concat(items(analysis.path("httpCalls")).stream(), sends.stream()).toList();
        for (var target : targets) {
            String id = target.path("id").asText();
            if (items(analysis.path("calls")).stream().noneMatch(c -> known(c) && c.path("toId").asText().equals(id)))
                warn(analysis, "handler-calls", "unlinked", target.path("evidence"), "호출하는 곳 확인 안 됨: " + id);
        }
        count(analysis, "handler-calls");
    }
}
