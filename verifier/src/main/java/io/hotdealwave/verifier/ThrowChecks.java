package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.regex.Pattern;
import static io.hotdealwave.verifier.Checks.*;

/** Endpoint → exception creation links; the chain's ends, the error code and the receiving handler are checked. */
final class ThrowChecks {
    void run(SourceText source, ObjectNode analysis) throws IOException {
        for (var link : analysis.path("throws")) {
            if (UNKNOWN.equals(link.path("status").asText())) continue; // evidence-lines already warned
            String fromId = link.path("fromId").asText(), exception = link.path("exception").asText();
            String errorCode = link.path("errorCode").asText(), message = link.path("message").asText();
            var from = find(analysis.path("endpoints"), "id", fromId);
            var evidence = items(link.path("evidence"));
            String service = from.path("service").asText();
            String lastEvidence = evidence.get(evidence.size() - 1).asText();
            String last = statement(source, lastEvidence);
            String reason = null;
            if (from.isMissingNode()) reason = "출발 ID가 추출 결과에 없음";
            else if (!evidence.get(0).equals(from.path("evidence").path(0))) reason = "첫 근거가 출발 항목의 첫 근거와 다름";
            else if (!Pattern.compile("\\bnew\\s+" + Pattern.quote(exception) + "\\s*\\(").matcher(String.valueOf(source.line(lastEvidence))).find()
                    || !errorCode.isEmpty() && !last.contains(errorCode) || !message.isEmpty() && !last.contains(message))
                reason = "마지막 근거가 예외 생성 줄이 아님";
            else if (!errorCode.isEmpty() && items(analysis.path("errorCodes")).stream()
                    .noneMatch(c -> c.path("service").asText().equals(service) && c.path("name").asText().equals(errorCode)))
                reason = "오류 코드가 추출 결과에 없음";
            else if (link.path("handlerId").isNull()) reason = "받는 처리기 없음";
            else if (find(analysis.path("exceptionHandlers"), "id", link.path("handlerId").asText()).isMissingNode())
                reason = "처리기 ID가 추출 결과에 없음";
            verdict(link, reason == null ? CONFIRMED : UNKNOWN, reason);
            if (reason != null) warn(analysis, "error-throws", link.path("evidence"),
                    reason + ": " + fromId + " -> " + exception + (errorCode.isEmpty() ? "" : " " + errorCode));
        }
        count(analysis, "error-throws");
    }

    /** The creation line joined with the following lines up to the end of its statement, for arguments on the next lines. */
    private static String statement(SourceText source, String evidence) throws IOException {
        int colon = evidence.lastIndexOf(':');
        String file = evidence.substring(0, colon);
        int number = Integer.parseInt(evidence.substring(colon + 1));
        var text = new StringBuilder();
        for (int i = number; ; i++) {
            String line = source.line(file + ":" + i);
            if (line == null) break;
            text.append(line).append(' ');
            if (line.contains(";")) break;
        }
        return text.toString();
    }
}
