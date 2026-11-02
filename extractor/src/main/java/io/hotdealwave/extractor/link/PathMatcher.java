package io.hotdealwave.extractor.link;

import java.util.regex.Pattern;

public final class PathMatcher {
    private PathMatcher() {}
    /** Compare URI templates by complete segments; Java parameter names are irrelevant. */
    public static boolean sameEndpoint(String first, String second) {
        return first != null && second != null && normalizeVariables(first).equals(normalizeVariables(second));
    }
    private static String normalizeVariables(String path) {
        return path.replaceAll("\\{[^/{}]+}", "{}");
    }
    public static boolean matches(String pattern, String path) {
        if (pattern == null || path == null) return false;
        // /** also matches the prefix itself, including an empty remaining path.
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            if (matches(prefix, path)) return true;
        }
        var regex = new StringBuilder("^");
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*') {
                if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '*') { regex.append(".*"); i++; }
                else regex.append("[^/]*");
            } else if (c == '?') regex.append("[^/]");
            else if (c == '{' && pattern.indexOf('}', i) > i) {
                regex.append("[^/]+"); i = pattern.indexOf('}', i);
            } else regex.append(Pattern.quote(String.valueOf(c)));
        }
        return path.matches(regex.append('$').toString());
    }
}
