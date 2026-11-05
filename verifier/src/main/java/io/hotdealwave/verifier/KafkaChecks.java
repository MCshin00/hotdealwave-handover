package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.regex.Pattern;
import static io.hotdealwave.verifier.Checks.*;

final class KafkaChecks {
    private static final Map<String, Pattern> PATTERNS = new LinkedHashMap<>();
    static {
        PATTERNS.put("KafkaListener", Pattern.compile("@(?:[\\w]+\\.)*KafkaListener\\b"));
        PATTERNS.put("DeadLetterPublishingRecoverer", Pattern.compile("\\bnew\\s+(?:[\\w]+\\.)*DeadLetterPublishingRecoverer\\s*\\("));
    }

    private static final Pattern TEMPLATE = Pattern.compile("\\bKafkaTemplate\\s*(?:<[^;=()]*>)?\\s+(\\w+)\\s*[;=,)]");

    /** Names of KafkaTemplate fields and parameters declared in a file, with kafkaTemplate. */
    static Set<String> templates(Collection<String> lines) {
        var names = new TreeSet<>(Set.of("kafkaTemplate"));
        for (String line : lines) {
            var matcher = TEMPLATE.matcher(line);
            while (matcher.find()) names.add(matcher.group(1));
        }
        return names;
    }

    void run(Map<String, String> lines, ObjectNode analysis, String composeFile) {
        var byFile = new LinkedHashMap<String, List<String>>();
        for (var line : lines.entrySet()) byFile.computeIfAbsent(line.getKey().substring(0, line.getKey().lastIndexOf(':')), f -> new ArrayList<>()).add(line.getValue());
        var raw = new LinkedHashMap<String, String>();
        for (var line : lines.entrySet()) {
            for (var pattern : PATTERNS.entrySet()) {
                var matcher = pattern.getValue().matcher(line.getValue());
                while (matcher.find()) raw.put(pattern.getKey() + "@" + line.getKey(), pattern.getKey());
            }
            var file = byFile.get(line.getKey().substring(0, line.getKey().lastIndexOf(':')));
            if (EvidenceChecks.declaration(line.getValue(), "kafkaTemplate.send", templates(file)))
                raw.put("kafkaTemplate.send@" + line.getKey(), "kafkaTemplate.send");
        }
        var extracted = new LinkedHashMap<String, String>();
        for (var declaration : analysis.path("kafkaDeclarations"))
            extracted.put(declaration.path("kind").asText() + "@" + declaration.path("evidence").path(0).asText(), declaration.path("kind").asText());
        CrossChecks.inventory(analysis, "kafka-declarations", raw, extracted);
        var counts = count(analysis, "kafka-declarations");
        var kinds = List.of("KafkaListener", "kafkaTemplate.send", "DeadLetterPublishingRecoverer");
        var labels = List.of("Listener", "Send", "Dlq");
        for (int i = 0; i < kinds.size(); i++) {
            String label = labels.get(i), kind = kinds.get(i);
            counts.put(Character.toLowerCase(label.charAt(0)) + label.substring(1) + "Count", Collections.frequency(new ArrayList<>(raw.values()), kind));
            counts.put("extracted" + label + "Count", Collections.frequency(new ArrayList<>(extracted.values()), kind));
        }

        for (var link : analysis.path("kafkaLinks")) {
            var pub = find(analysis.path("kafkaPublications"), "id", link.path("publicationId").asText());
            var sub = find(analysis.path("kafkaSubscriptions"), "id", link.path("subscriptionId").asText());
            String environment = link.path("environment").asText();
            var evidence = items(link.path("evidence"));
            boolean same = known(pub) && known(sub) && !UNKNOWN.equals(link.path("status").asText());
            if (environment.isEmpty()) {
                // A topic name written on both sides is the same topic; the link carries both name lines.
                String topic = value(pub.path("topicName"));
                same &= known(pub.path("topicName")) && known(sub.path("topicName")) && topic != null && !topic.isEmpty()
                        && topic.equals(value(sub.path("topicName")))
                        && evidence.containsAll(items(pub.path("topicName").path("evidence")))
                        && evidence.containsAll(items(sub.path("topicName").path("evidence")));
            } else {
                same &= known(pub.path("environment")) && known(sub.path("environment"))
                        && environment.equals(value(pub.path("environment"))) && environment.equals(value(sub.path("environment")));
                for (var endpoint : List.of(pub, sub)) {
                    var compose = items(endpoint.path("environment").path("evidence")).stream()
                            .filter(e -> !composeFile.isEmpty() && e.asText().startsWith(composeFile + ":")).toList();
                    same &= !compose.isEmpty() && evidence.containsAll(compose);
                }
            }
            verdict(link, same ? CONFIRMED : UNKNOWN, same ? null : environment.isEmpty()
                    ? "발행·구독의 토픽 이름과 양쪽 근거가 일치하지 않음" : "발행·구독의 환경변수 이름과 양쪽 compose 근거가 일치하지 않음");
            if (!same) warn(analysis, "kafka-topics", "extraction-mismatch", link.path("evidence"), link.path("reason").asText());
        }
        for (var publication : analysis.path("kafkaPublications")) {
            if (items(analysis.path("kafkaLinks")).stream().noneMatch(l -> known(l) && l.path("publicationId").asText().equals(publication.path("id").asText()))) {
                warn(analysis, "kafka-topics", code(publication), publication.path("evidence"), "구독자 확인 안 됨: " + reason(publication, "구독"));
            }
        }
        for (var subscription : analysis.path("kafkaSubscriptions")) {
            if (items(analysis.path("kafkaLinks")).stream().noneMatch(l -> known(l) && l.path("subscriptionId").asText().equals(subscription.path("id").asText()))) {
                warn(analysis, "kafka-topics", code(subscription), subscription.path("evidence"), "발행자 확인 안 됨: " + reason(subscription, "발행"));
            }
        }
        count(analysis, "kafka-topics");
    }

    /** An environment variable chain leaves the value to the deployment; a topic name or a broken chain is a gap inside the repository. */
    private static String code(JsonNode endpoint) {
        String environment = value(endpoint.path("environment"));
        return known(endpoint.path("environment")) && environment != null && !environment.isEmpty() ? "outside-value" : "unlinked";
    }

    /** Why no link is confirmed: nothing on the other side uses the same topic name or environment variable. */
    private static String reason(JsonNode endpoint, String otherSide) {
        String service = endpoint.path("service").asText();
        if (known(endpoint.path("topicName")))
            return "같은 토픽 이름(" + value(endpoint.path("topicName")) + ")을 쓰는 " + otherSide + "을 찾지 못함 (" + service + ")";
        return "같은 환경변수(" + (known(endpoint.path("environment")) ? value(endpoint.path("environment")) : "확인 안 됨") + ")를 쓰는 " + otherSide
                + "을 찾지 못함. 토픽 실제 값은 저장소 밖 환경에서 정해짐 ("
                + service + ", " + (known(endpoint.path("topicKey")) ? value(endpoint.path("topicKey")) : "토픽 키 확인 안 됨") + ")";
    }
}
