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
    static final String SOURCE = "https://github.com/sparta-final-12th-9700/HotDealWave/blob/cc43016b907331a3ce8471475cbf44412e8a6954/";
    static final String RESPONSES = "analysis/hotdealwave/responses/";

    private static final Pattern LINE = Pattern.compile("<p class=\"((?:line|meta)[^\"]*)\">(.*?)</p>", Pattern.DOTALL);
    private static final Pattern EVIDENCE = Pattern.compile("\\[([^\\[\\]<>]+?:\\d+(?:, [^\\[\\]<>]+?:\\d+)*)]");
    private static final Pattern KEY = Pattern.compile("^([가-힣A-Za-z()· ]{1,16}): ");
    /** A status standing on its own, not part of a sentence such as "구독자 확인 안 됨:". */
    private static final Pattern STATUS = Pattern.compile("(?<=^|[\\s>(])(확인됨|확인 안 됨|저장소에 근거 없음)(?=$|[\\s<\\[])");
    private static final Pattern METHOD = Pattern.compile("(?<=^|<span class=\"k\">HTTP 메서드:</span> |>)(GET|POST|PUT|PATCH|DELETE)(?= |<|$)");
    /** The reason after a response check in a service page row. The endpoint page shows it whole. */
    private static final Pattern REASON = Pattern.compile("(응답 확인: [^(<]*?) \\(([^<]+)\\)(?=</span>)");
    private static final Pattern TARGET_LINE = Pattern.compile("target/HotDealWave/([^\\s;:]+):(\\d+)");
    /** An item tag "[kind · source (· status)]" split into a kind tag and a source tag. */
    private static final Pattern TAG = Pattern.compile("<span class=\"tag (\\w+)\">\\[([^·\\]]+) · ([^\\]]+)]</span>");

    private Decor() {}

    static String html(String html) {
        String lines = LINE.matcher(html).replaceAll(m -> {
            String inner = m.group(2).contains("\n") ? m.group(2).strip().replaceAll("\\s*\\n\\s*", " ") : m.group(2);
            return Matcher.quoteReplacement("<div class=\"" + m.group(1) + status(inner) + "\">" + mark(inner) + "</div>");
        });
        return TAG.matcher(lines).replaceAll(m -> Matcher.quoteReplacement("<span class=\"tag " + m.group(1) + "\">" + m.group(2)
                + "</span> <span class=\"tag " + (m.group(3).endsWith("확인함") ? "checked" : "ai") + "\">" + m.group(3) + "</span>"));
    }

    /** The weakest status on the line decides its stripe. */
    private static String status(String inner) {
        String text = inner.replaceAll("<[^>]+>", "");
        if (text.contains("저장소에 근거 없음")) return " none";
        if (text.contains("확인 안 됨")) return " unknown";
        if (text.contains("확인됨")) return " ok";
        return "";
    }

    private static String mark(String inner) {
        String marked = REASON.matcher(inner).replaceFirst(m -> Matcher.quoteReplacement(m.group(1)
                + " <details class=\"why\"><summary>이유</summary>(" + TARGET_LINE.matcher(m.group(2)).replaceAll(t -> Matcher.quoteReplacement(
                        "<a href=\"" + SOURCE + t.group(1) + "#L" + t.group(2) + "\">" + t.group() + "</a>")) + ")</details>"));
        marked = KEY.matcher(marked).replaceFirst(m -> Matcher.quoteReplacement("<span class=\"k\">" + m.group(1) + ":</span> "));
        marked = STATUS.matcher(marked).replaceAll(m -> "<span class=\"pill " + switch (m.group(1)) {
            case "확인됨" -> "ok";
            case "확인 안 됨" -> "unknown";
            default -> "none";
        } + "\">" + m.group(1) + "</span>");
        marked = METHOD.matcher(marked).replaceAll(m -> "<span class=\"method " + m.group(1).toLowerCase() + "\">" + m.group(1) + "</span>");
        return evidence(marked);
    }

    private static String evidence(String inner) {
        return EVIDENCE.matcher(inner).replaceAll(m -> {
            var parts = m.group(1).split(", ");
            String links = Arrays.stream(parts).map(Decor::link).collect(Collectors.joining(", "));
            return Matcher.quoteReplacement("<details class=\"ev\"><summary>근거 " + parts.length + "개</summary>["+ links + "]</details>");
        });
    }

    private static String link(String evidence) {
        int colon = evidence.lastIndexOf(':');
        String file = evidence.substring(0, colon), line = evidence.substring(colon + 1);
        String href = file.startsWith(RESPONSES) ? "/responses/" + file.substring(RESPONSES.length()) : SOURCE + file;
        return "<a href=\"" + href + "#L" + line + "\">" + evidence + "</a>";
    }
}
