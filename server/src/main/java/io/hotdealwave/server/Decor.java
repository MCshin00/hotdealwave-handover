package io.hotdealwave.server;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Display-only markup over a rendered body: evidence lists become folded links, statuses become badges,
 * a leading "key: " becomes a label, and lines get a status class.
 * Search, MCP and the model read the body before this, so their text does not change.
 */
final class Decor {
    /** A recorded response file of any target; its lines open on the response page. */
    static final Pattern RESPONSE_FILE = Pattern.compile("responses/(.+)");

    private static final Pattern LINE = Pattern.compile("<p class=\"((?:line|meta)[^\"]*)\">(.*?)</p>", Pattern.DOTALL);
    private static final Pattern EVIDENCE = Pattern.compile("\\[([^\\[\\]<>]+?:\\d+(?:, [^\\[\\]<>]+?:\\d+)*)]");
    private static final Pattern KEY = Pattern.compile("^([가-힣A-Za-z()· ]{1,16}): ");
    /** A status standing on its own, not part of a sentence such as "구독자 확인 안 됨:". */
    private static final Pattern STATUS = Pattern.compile("(?<=^|[\\s>(])(확인됨|확인 안 됨|저장소에 근거 없음)(?=$|[\\s<\\[])");
    /** A status the page does not mark: the value standing alone is confirmed, and the legend says so. */
    private static final Pattern CONFIRMED = Pattern.compile("(?:\\s·)?\\s?(?<=^|[\\s>(])확인됨(?=$|[\\s<\\[])");
    /** The reason after an unconfirmed status, up to its evidence or the end of the line. */
    private static final Pattern WHY = Pattern.compile("(확인 안 됨|저장소에 근거 없음) — (.+?)(?=\\s\\[|\\s·\\s|<|$)");
    private static final Pattern METHOD = Pattern.compile("(?<=^|<span class=\"k\">HTTP 메서드:</span> |>)(GET|POST|PUT|PATCH|DELETE)(?= |<|$)");
    /** The reason after a response check in a service page row. The endpoint page shows it whole. */
    private static final Pattern REASON = Pattern.compile("(응답 확인: [^(<]*?) \\(([^<]+)\\)(?=</span>)");
    private static final Pattern TARGET_LINE = Pattern.compile("target/[^/\\s;:]+/([^\\s;:]+):(\\d+)");
    /** An item tag "[kind · source (· status)]" split into a kind tag and a source tag. */
    private static final Pattern TAG = Pattern.compile("<span class=\"tag (\\w+)\">\\[([^·\\]]+) · ([^\\]]+)]</span>");

    private Decor() {}

    static String html(String html, Analysis.Target target) {
        String lines = LINE.matcher(html).replaceAll(m -> {
            String inner = m.group(2).contains("\n") ? m.group(2).strip().replaceAll("\\s*\\n\\s*", " ") : m.group(2);
            return Matcher.quoteReplacement("<div class=\"" + m.group(1) + status(inner) + "\">" + mark(inner, target.source()) + "</div>");
        });
        return TAG.matcher(lines).replaceAll(m -> Matcher.quoteReplacement("<span class=\"tag " + m.group(1) + "\">" + m.group(2)
                + "</span> <span class=\"tag " + (m.group(3).endsWith("확인함") ? "checked" : "ai") + "\">" + m.group(3) + "</span>"));
    }

    /** The weakest status on the line decides its stripe. */
    private static String status(String inner) {
        String text = inner.replaceAll("<[^>]+>", "");
        if (text.contains("저장소에 근거 없음")) return " none";
        if (text.contains("확인 안 됨")) return " unknown";
        return "";
    }

    private static String mark(String inner, String source) {
        String marked = REASON.matcher(inner).replaceFirst(m -> Matcher.quoteReplacement(m.group(1)
                + " <details class=\"why\"><summary>이유</summary>(" + TARGET_LINE.matcher(m.group(2)).replaceAll(t -> Matcher.quoteReplacement(
                        "<a href=\"" + source + t.group(1) + "#L" + t.group(2) + "\">" + t.group() + "</a>")) + ")</details>"));
        marked = KEY.matcher(marked).replaceFirst(m -> Matcher.quoteReplacement("<span class=\"k\">" + m.group(1) + ":</span> "));
        marked = CONFIRMED.matcher(marked).replaceAll("");
        marked = WHY.matcher(marked).replaceAll(m -> Matcher.quoteReplacement(m.group(1)
                + " <details class=\"why\"><summary>이유</summary>" + m.group(2) + "</details>"));
        marked = STATUS.matcher(marked).replaceAll(m -> "<span class=\"pill " + (m.group(1).equals("확인 안 됨") ? "unknown" : "none")
                + "\">" + m.group(1) + "</span>");
        marked = METHOD.matcher(marked).replaceAll(m -> "<span class=\"method " + m.group(1).toLowerCase() + "\">" + m.group(1) + "</span>");
        return evidence(marked, source);
    }

    private static String evidence(String inner, String source) {
        return EVIDENCE.matcher(inner).replaceAll(m -> {
            var parts = m.group(1).split(", ");
            String links = Arrays.stream(parts).map(part -> link(part, source)).collect(Collectors.joining(", "));
            return Matcher.quoteReplacement("<details class=\"ev\"><summary>근거 " + parts.length + "개</summary>["+ links + "]</details>");
        });
    }

    private static String link(String evidence, String source) {
        int colon = evidence.lastIndexOf(':');
        String file = evidence.substring(0, colon), line = evidence.substring(colon + 1);
        var response = RESPONSE_FILE.matcher(file);
        String href = response.matches() ? "/responses/" + response.group(1) : source + file;
        return "<a href=\"" + href + "#L" + line + "\">" + evidence + "</a>";
    }
}
