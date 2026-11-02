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
        PATTERNS.put("kafkaTemplate.send", Pattern.compile("\\bkafkaTemplate\\s*\\.\\s*send\\s*\\("));
        PATTERNS.put("DeadLetterPublishingRecoverer", Pattern.compile("\\bnew\\s+(?:[\\w]+\\.)*DeadLetterPublishingRecoverer\\s*\\("));
    }

    void run(Map<String, String> lines, ObjectNode analysis) {
        var raw = new LinkedHashMap<String, String>();
        for (var line : lines.entrySet()) for (var pattern : PATTERNS.entrySet()) {
            var matcher = pattern.getValue().matcher(line.getValue());
            while (matcher.find()) raw.put(pattern.getKey() + "@" + line.getKey(), pattern.getKey());
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
            boolean same = !environment.isEmpty() && known(pub) && known(sub)
                    && known(pub.path("environment")) && known(sub.path("environment"))
                    && environment.equals(value(pub.path("environment"))) && environment.equals(value(sub.path("environment")))
                    && !UNKNOWN.equals(link.path("status").asText());
            for (var endpoint : List.of(pub, sub)) {
                var compose = items(endpoint.path("environment").path("evidence")).stream()
                        .filter(e -> e.asText().startsWith("docker-compose.yml:")).toList();
                same &= !compose.isEmpty() && evidence.containsAll(compose);
            }
            verdict(link, same ? CONFIRMED : UNKNOWN, same ? null : "발행·구독의 환경변수 이름과 양쪽 compose 근거가 일치하지 않음");
            if (!same) warn(analysis, "kafka-topics", link.path("evidence"), link.path("reason").asText());
        }
        for (var publication : analysis.path("kafkaPublications")) {
            if (items(analysis.path("kafkaLinks")).stream().noneMatch(l -> known(l) && l.path("publicationId").asText().equals(publication.path("id").asText()))) {
                warn(analysis, "kafka-topics", publication.path("evidence"), "구독자 확인 안 됨: " + label(publication));
            }
        }
        for (var subscription : analysis.path("kafkaSubscriptions")) {
            if (items(analysis.path("kafkaLinks")).stream().noneMatch(l -> known(l) && l.path("subscriptionId").asText().equals(subscription.path("id").asText()))) {
                warn(analysis, "kafka-topics", subscription.path("evidence"), "발행자 확인 안 됨: " + label(subscription));
            }
        }
        count(analysis, "kafka-topics");
    }

    private static String label(JsonNode endpoint) {
        String service = endpoint.path("service").asText();
        if (known(endpoint.path("topicName")))
            return service + ", 토픽 " + value(endpoint.path("topicName")) + ", 토픽 키·환경변수 해당 없음";
        return service + ", " + (known(endpoint.path("topicKey")) ? value(endpoint.path("topicKey")) : "토픽 키 확인 안 됨")
                + ", " + (known(endpoint.path("environment")) ? value(endpoint.path("environment")) : "환경변수 확인 안 됨");
    }
}
