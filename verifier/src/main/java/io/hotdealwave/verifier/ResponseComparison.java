package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

/** Keys of a stored response against the JSON names of the extracted response fields. Values and types are not compared. */
final class ResponseComparison {
    private ResponseComparison() {}

    /** Differences of one stored response body, or the reason it is not compared; empty when the names match. */
    static List<String> compare(JsonNode check, JsonNode response, JsonNode body) {
        if (check.path("statusCode").isNull()) return List.of();
        int status = check.path("statusCode").asInt();
        if (status < 200 || status >= 300) return List.of("비교 보류: HTTP " + status + " (정상 응답과 비교하지 않음)");
        return compare(response, List.of(body), "$");
    }

    /** Objects at the same array position are compared by the union of their observed keys. */
    static List<String> compare(JsonNode schema, List<JsonNode> all, String path) {
        var samples = all.stream().filter(s -> s != null && !s.isNull() && !s.isMissingNode()).toList();
        if (samples.isEmpty()) return List.of("비교 보류: " + path + " (본문 또는 값이 null)");
        String kind = schema.path("kind").asText();
        var fields = Checks.items(schema.path("fields"));
        var arguments = Checks.items(schema.path("arguments"));
        String type = schema.path("type").asText();
        if ((type.startsWith("Page<") || type.startsWith("PagedModel<")) && arguments.size() == 1
                && samples.stream().allMatch(s -> s.isObject() && s.path("content").isArray())) {
            var elements = samples.stream().flatMap(s -> Checks.items(s.path("content")).stream()).toList();
            if (elements.isEmpty()) return List.of("비교 보류: " + path + ".content[] (빈 배열)");
            return compare(arguments.get(0), elements, path + ".content[]");
        }
        if ((kind.equals("container") || kind.equals("array")) && fields.isEmpty()) {
            if (arguments.size() != 1 || !samples.stream().allMatch(JsonNode::isArray))
                return List.of("비교 보류: " + path + " (추출 인자와 응답 배열을 대응할 수 없음)");
            var elements = samples.stream().flatMap(s -> Checks.items(s).stream()).toList();
            if (elements.isEmpty()) return List.of("비교 보류: " + path + "[] (빈 배열)");
            return compare(arguments.get(0), elements, path + "[]");
        }
        if (kind.equals("scalar") || kind.equals("enum")) return List.of();
        if (fields.isEmpty()) return List.of("비교 보류: " + path + " (추출에 내부 구조 없음: " + type + ")");
        if (!samples.stream().allMatch(JsonNode::isObject)) return List.of("비교 보류: " + path + " (추출 객체와 응답 구조가 다름)");

        var expected = new LinkedHashMap<String, JsonNode>();
        for (var field : fields) if (field.path("jsonName").path("value").isTextual())
            expected.put(field.path("jsonName").path("value").asText(), field.path("schema"));
        var actual = new TreeSet<String>();
        samples.forEach(s -> s.fieldNames().forEachRemaining(actual::add));
        var extra = new TreeSet<>(actual); extra.removeAll(expected.keySet());
        var missing = new TreeSet<>(expected.keySet()); missing.removeAll(actual);
        var differences = new ArrayList<String>();
        if (fields.stream().anyMatch(f -> !f.path("jsonName").path("value").isTextual()))
            differences.add("비교 보류: " + path + " (JSON 이름이 미확정인 추출 필드 있음)");

        // A name difference only when the normalized names pair up uniquely inside the same parent.
        var renamed = new LinkedHashMap<String, String>();
        for (String name : extra) {
            var matches = missing.stream().filter(old -> normalized(name).equals(normalized(old))).toList();
            if (matches.size() == 1 && extra.stream().filter(other -> normalized(other).equals(normalized(name))).count() == 1)
                renamed.put(name, matches.get(0));
        }
        renamed.forEach((name, old) -> differences.add("이름 다름: " + path + "." + name + " ↔ " + path + "." + old));
        extra.stream().filter(n -> !renamed.containsKey(n)).forEach(n -> differences.add("응답에만 있음: " + path + "." + n));
        missing.stream().filter(n -> !renamed.containsValue(n)).forEach(n -> differences.add("추출에만 있음: " + path + "." + n));

        var compared = new TreeSet<String>(actual); compared.retainAll(expected.keySet()); compared.addAll(renamed.keySet());
        for (String name : compared) {
            var child = expected.get(renamed.getOrDefault(name, name));
            // A null scalar is observed by its key; nothing inside an object can be observed from null.
            if (Set.of("scalar", "enum").contains(child.path("kind").asText())) continue;
            differences.addAll(compare(child, samples.stream().filter(s -> s.has(name)).map(s -> s.path(name)).toList(), path + "." + name));
        }
        return differences;
    }

    /** Only the is + capital prefix, underscores and case. */
    private static String normalized(String name) {
        return name.replaceFirst("^is(?=[A-Z])", "").replace("_", "").toLowerCase(Locale.ROOT);
    }
}
