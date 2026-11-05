package io.hotdealwave.server;

import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** BM25 over the h2 sections of page bodies, with service and kind filters taken from the question. The answer path fuses BM25 and embedding ranks. */
@Component
class Search {
    record Result(String page, String title, String section, double score, String preview, Set<String> services) {}
    record Route(String service, String kind) {}
    private record Doc(Pages.Section section, Map<String, Integer> tf, int length) {}
    private record Scored(Pages.Section section, double score) {}

    private static final Pattern WORD = Pattern.compile("[A-Za-z0-9_.\\-/]+|[가-힣]+");
    private static final Map<String, String> KIND_WORDS = new LinkedHashMap<>();
    static {
        for (var w : List.of("api", "엔드포인트")) KIND_WORDS.put(w, "엔드포인트");
        for (var w : List.of("토픽", "kafka")) KIND_WORDS.put(w, "토픽");
        for (var w : List.of("서비스", "설정", "환경변수")) KIND_WORDS.put(w, "서비스");
    }
    private static final double K1 = 1.2, B = 0.75;
    static final int LIMIT = 5;
    /** The answer path takes more: with five, RRF pushed out sections BM25 ranked second and fourth that the answer needed. */
    static final int ASK_LIMIT = 8;
    /** Reciprocal rank fusion constant. */
    static final int RRF = 60;
    /** Characters per embedded piece of a section, under the embedding input limit even for Korean text. */
    static final int PIECE = 3000;

    private final Pages pages;
    private final Embeddings embeddings;
    private final Map<String, String> serviceNames = new HashMap<>();
    private List<Doc> docs;
    private final Map<String, Integer> df = new HashMap<>();
    private double averageLength;
    /** Each doc's piece vectors, in docs order. */
    private List<List<float[]>> vectors;

    Search(Pages pages, Analysis analysis, Embeddings embeddings) {
        this.pages = pages;
        this.embeddings = embeddings;
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

    /** Top sections by BM25, one per page. Filters are dropped one at a time, kind first, while nothing matches. */
    List<Result> search(String query) { return search(query, null); }

    /** For /api/ask: BM25 and embedding ranks fused by RRF, top ASK_LIMIT, otherwise as search. Throws when the embedding call fails. */
    List<Result> hybrid(String query) { return search(query, embeddings.query(query)); }

    private List<Result> search(String query, float[] question) {
        var route = route(query);
        var tried = new LinkedHashSet<>(List.of(route, new Route(route.service(), null), new Route(null, null)));
        for (var filter : tried) {
            var results = rank(query, filter, question);
            if (!results.isEmpty()) return results;
        }
        return List.of();
    }

    /** Full text of a result's section. */
    String text(Result result) {
        return docs().stream().map(Doc::section).filter(s -> s.page().equals(result.page()) && s.heading().equals(result.section()))
                .map(Pages.Section::text).findFirst().orElse("");
    }

    private List<Result> rank(String query, Route filter, float[] question) {
        var terms = tokens(query);
        var bm25 = new ArrayList<Scored>();
        var similar = new ArrayList<Scored>();
        var all = docs();
        for (int i = 0; i < all.size(); i++) {
            var doc = all.get(i);
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
            if (score > 0) bm25.add(new Scored(section, Math.round(score * 1000) / 1000.0));
            if (question != null) {
                // A long section is embedded in pieces; its closest piece stands for it.
                double best = vectors().get(i).stream().mapToDouble(v -> Embeddings.cosine(question, v)).max().orElse(0);
                similar.add(new Scored(section, best));
            }
        }
        bm25.sort(Comparator.comparingDouble(Scored::score).reversed());
        var scored = bm25;
        if (question != null) {
            similar.sort(Comparator.comparingDouble(Scored::score).reversed());
            var fused = new LinkedHashMap<Pages.Section, Double>();
            for (var ranking : List.of(bm25, similar))
                for (int rank = 0; rank < ranking.size(); rank++) fused.merge(ranking.get(rank).section(), 1.0 / (RRF + rank + 1), Double::sum);
            scored = new ArrayList<>();
            for (var e : fused.entrySet()) scored.add(new Scored(e.getKey(), Math.round(e.getValue() * 1_000_000) / 1_000_000.0));
            scored.sort(Comparator.comparingDouble(Scored::score).reversed());
        }
        var seen = new HashSet<String>();
        return scored.stream().filter(s -> seen.add(s.section().page())).limit(question == null ? LIMIT : ASK_LIMIT)
                .map(s -> new Result(s.section().page(), s.section().title(), s.section().heading(), s.score(),
                        preview(s.section().text(), terms), s.section().services())).toList();
    }

    private static String preview(String text, List<String> terms) {
        String lower = text.toLowerCase();
        int at = terms.stream().sorted(Comparator.comparingInt(String::length).reversed()).mapToInt(lower::indexOf)
                .filter(i -> i >= 0).findFirst().orElse(0);
        int start = Math.max(0, at - 60), end = Math.min(text.length(), start + 200);
        return (start > 0 ? "…" : "") + text.substring(start, end).replace('\n', ' ') + (end < text.length() ? "…" : "");
    }

    /** What is embedded for a section: its title and heading, then its text cut at line ends into pieces of at most PIECE characters. */
    static List<String> pieces(Pages.Section section) {
        String head = section.title() + " — " + section.heading() + "\n";
        var pieces = new ArrayList<String>();
        var piece = new StringBuilder();
        for (var line : section.text().split("\n")) {
            for (int at = 0; at < Math.max(1, line.length()); at += PIECE) {
                String part = line.substring(at, Math.min(line.length(), at + PIECE));
                if (piece.length() > 0 && piece.length() + 1 + part.length() > PIECE) { pieces.add(head + piece); piece.setLength(0); }
                piece.append(piece.length() > 0 ? "\n" : "").append(part);
            }
        }
        if (piece.length() > 0 || pieces.isEmpty()) pieces.add(head + piece);
        return pieces;
    }

    private synchronized List<List<float[]>> vectors() {
        if (vectors == null) {
            var all = docs().stream().map(d -> pieces(d.section())).toList();
            var flat = embeddings.of(all.stream().flatMap(List::stream).toList());
            var grouped = new ArrayList<List<float[]>>();
            int at = 0;
            for (var pieces : all) { grouped.add(flat.subList(at, at + pieces.size())); at += pieces.size(); }
            vectors = grouped;
        }
        return vectors;
    }

    private synchronized List<Doc> docs() {
        if (docs == null) {
            var built = new ArrayList<Doc>();
            for (var page : pages.sections().values()) {
                for (var section : page) {
                    if (section.page().equals(Pages.REVIVAL)) continue;
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
