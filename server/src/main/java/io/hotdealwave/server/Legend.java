package io.hotdealwave.server;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/** What "확인됨" checked for the values a page or tool result shows, from the verifier output. */
@Component
class Legend {
    private static final Map<String, List<String>> ARRAYS = Map.of(
            "엔드포인트", List.of("endpoints", "fields", "externalPaths", "tokenRequired", "httpCalls", "calls", "throws", "errors"),
            "토픽", List.of("messagePublications", "messageSubscriptions", "messageLinks", "calls"),
            "서비스", List.of("services", "endpoints", "httpCalls", "messagePublications", "messageSubscriptions", "environmentVariables", "errors", "unsupported"),
            "배포·모니터링", List.of("repository"),
            "되살리기", List.of("environmentVariables", "repository", "unsupported"),
            "흐름", List.of("calls", "httpCalls", "messageLinks"));

    private final Analysis analysis;

    Legend(Analysis analysis) { this.analysis = analysis; }

    List<String> lines(String... kinds) {
        var confirmed = analysis.get("confirmed");
        return Arrays.stream(kinds).flatMap(kind -> ARRAYS.getOrDefault(kind, List.of()).stream()).distinct()
                .map(k -> confirmed.path(k).asText("")).filter(l -> !l.isEmpty()).toList();
    }

    /** Head of a tool result; a result over several kinds of page gets the arrays of each once. */
    String text(String... kinds) {
        var lines = lines(kinds);
        return lines.isEmpty() ? "" : "확인됨의 뜻\n" + String.join("\n", lines.stream().map(l -> "- " + l).toList()) + "\n\n";
    }

    /** Folded legend under a page title. Pages do not mark confirmed values, so the legend says so. */
    String html(String kind) {
        var lines = lines(kind);
        if (lines.isEmpty()) return "";
        return "<details class=\"legend\"><summary>확인됨의 뜻</summary><p>상태가 붙지 않은 값은 확인됨이다. 확인됨은 다음만 확인했다.</p><ul>"
                + String.join("", lines.stream().map(l -> "<li>" + HtmlUtils.htmlEscape(l) + "</li>").toList()) + "</ul></details>";
    }
}
