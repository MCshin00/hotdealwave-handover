package io.hotdealwave.server;

import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** BM25 over the h2 sections of page bodies, with service and kind filters taken from the question. */
@Component
class Search {
    record Result(String page, String title, String section, double score, String preview, Set<String> services) {}
    record Route(String service, String kind) {}
    private record Doc(Pages.Section section, Map<String, Integer> tf, int length) {}

    private static final Pattern WORD = Pattern.compile("[A-Za-z0-9_.\\-/]+|[가-힣]+");
    private static final Map<String, String> KIND_WORDS = new LinkedHashMap<>();
    static {
        for (var w : List.of("api", "엔드포인트")) KIND_WORDS.put(w, "엔드포인트");
        for (var w : List.of("토픽", "kafka")) KIND_WORDS.put(w, "토픽");
        for (var w : List.of("서비스", "설정", "환경변수")) KIND_WORDS.put(w, "서비스");
    }
    private static final double K1 = 1.2, B = 0.75;
    static final int LIMIT = 5;

    private final Pages pages;
    private final Map<String, String> serviceNames = new HashMap<>();
    private List<Doc> docs;
    private final Map<String, Integer> df = new HashMap<>();
    private double averageLength;

    Search(Pages pages, Analysis analysis) {
        this.pages = pages;
        for (var service : analysis.list("services")) {
            String name = Analysis.value(service.path("name"));
            serviceNames.put(name.toLowerCase(), name);
            String directory = service.path("directory").asText();
            serviceNames.put(directory.substring(directory.lastIndexOf('/') + 1).toLowerCase(), name);
        }
    }

    /** Identifiers whole and by camelCase/snake_case parts, lowercased and once each per word; Korean in two-letter pieces. */
    static List<String> tokens(String text) {
        var tokens = new ArrayList<String>();
        var m = WORD.matcher(text);
        while (m.find()) {
            String word = m.group();
            if (word.charAt(0) >= '가' && word.charAt(0) <= '힣') {
                if (word.length() == 1) tokens.add(word);
                for (int i = 0; i + 2 <= word.length(); i++) tokens.add(word.substring(i, i + 2));
                continue;
            }
            word = word.replaceAll("^[._\\-/]+|[._\\-/]+$", "");
            if (word.isEmpty()) continue;
            // A dotted or hyphenated name also yields each identifier in it whole, then by case parts.
            var pieces = new LinkedHashSet<String>();
            pieces.add(word.toLowerCase());
            for (var segment : word.split("[./:-]+")) pieces.add(segment.toLowerCase());
            for (var part : word.replaceAll("([a-z0-9])([A-Z])", "$1 $2").split("[^A-Za-z0-9]+")) pieces.add(part.toLowerCase());
            pieces.remove("");
            tokens.addAll(pieces);
        }
        return tokens;
    }

    /** A service named as in the data (order-service, order) and a kind word, when the question has them. */
    Route route(String query) {
        var words = new HashSet<String>();
        var m = WORD.matcher(query.toLowerCase());
        while (m.find()) words.add(m.group());
        String service = words.stream().map(serviceNames::get).filter(Objects::nonNull).findFirst().orElse(null);
        String kind = KIND_WORDS.entrySet().stream().filter(e -> words.contains(e.getKey()) || (e.getKey().charAt(0) >= '가' && query.contains(e.getKey())))
                .map(Map.Entry::getValue).findFirst().orElse(null);
        return new Route(service, kind);
    }

    /** Top sections, one per page. Filters are dropped one at a time, kind first, while nothing matches. */
    List<Result> search(String query) {
        var route = route(query);
        var tried = new LinkedHashSet<>(List.of(route, new Route(route.service(), null), new Route(null, null)));
        for (var filter : tried) {
            var results = rank(query, filter);
            if (!results.isEmpty()) return results;
        }
        return List.of();
    }

    /** Full text of a result's section. */
    String text(Result result) {
        return docs().stream().map(Doc::section).filter(s -> s.page().equals(result.page()) && s.heading().equals(result.section()))
                .map(Pages.Section::text).findFirst().orElse("");
    }

    private List<Result> rank(String query, Route filter) {
        var terms = tokens(query);
        var scored = new ArrayList<Result>();
        for (var doc : docs()) {
            var section = doc.section();
            if (filter.service() != null && !section.services().contains(filter.service())) continue;
            if (filter.kind() != null && !section.kind().equals(filter.kind())) continue;
            double score = 0;
            for (var term : terms) {
                int tf = doc.tf().getOrDefault(term, 0);
                if (tf == 0) continue;
                double idf = Math.log(1 + (docs.size() - df.get(term) + 0.5) / (df.get(term) + 0.5));
                score += idf * tf * (K1 + 1) / (tf + K1 * (1 - B + B * doc.length() / averageLength));
            }
            if (score > 0) scored.add(new Result(section.page(), section.title(), section.heading(), Math.round(score * 1000) / 1000.0,
                    preview(section.text(), terms), section.services()));
        }
        scored.sort(Comparator.comparingDouble(Result::score).reversed());
        var seen = new HashSet<String>();
        return scored.stream().filter(r -> seen.add(r.page())).limit(LIMIT).toList();
    }

    private static String preview(String text, List<String> terms) {
        String lower = text.toLowerCase();
        int at = terms.stream().sorted(Comparator.comparingInt(String::length).reversed()).mapToInt(lower::indexOf)
                .filter(i -> i >= 0).findFirst().orElse(0);
        int start = Math.max(0, at - 60), end = Math.min(text.length(), start + 200);
        return (start > 0 ? "…" : "") + text.substring(start, end).replace('\n', ' ') + (end < text.length() ? "…" : "");
    }

    private synchronized List<Doc> docs() {
        if (docs == null) {
            var built = new ArrayList<Doc>();
            for (var page : pages.sections().values()) {
                for (var section : page) {
                    var tf = new HashMap<String, Integer>();
                    var tokens = tokens(section.title() + " " + section.heading() + " " + section.text());
                    tokens.forEach(t -> tf.merge(t, 1, Integer::sum));
                    tf.keySet().forEach(t -> df.merge(t, 1, Integer::sum));
                    built.add(new Doc(section, tf, tokens.size()));
                }
            }
            averageLength = built.stream().mapToInt(Doc::length).average().orElse(1);
            docs = built;
        }
        return docs;
    }
}
