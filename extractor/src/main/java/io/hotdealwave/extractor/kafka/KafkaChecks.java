package io.hotdealwave.extractor.kafka;

import io.hotdealwave.extractor.model.Analysis;
import io.hotdealwave.extractor.model.Analysis.*;
import io.hotdealwave.extractor.source.SourceContext;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;

public final class KafkaChecks {
    // Independent text inventory, deliberately retaining matches in comments like the REST inventory.
    private static final Map<String, Pattern> PATTERNS = new LinkedHashMap<>();
    static {
        PATTERNS.put("KafkaListener", Pattern.compile("@(?:[\\w]+\\.)*KafkaListener\\b"));
        PATTERNS.put("kafkaTemplate.send", Pattern.compile("\\bkafkaTemplate\\s*\\.\\s*send\\s*\\("));
        PATTERNS.put("DeadLetterPublishingRecoverer", Pattern.compile("\\bnew\\s+(?:[\\w]+\\.)*DeadLetterPublishingRecoverer\\s*\\("));
    }

    public void run(List<SourceContext> sources, Analysis analysis) throws IOException {
        var raw = new LinkedHashMap<String, Declaration>();
        for (var source : sources) for (var file : source.files) {
            var lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) for (var pattern : PATTERNS.entrySet()) {
                var matcher = pattern.getValue().matcher(lines.get(i));
                while (matcher.find()) {
                    var declaration = new Declaration(pattern.getKey(), Analysis.CONFIRMED,
                            List.of(source.relative(file) + ":" + (i + 1)));
                    raw.put(key(declaration), declaration);
                }
            }
        }
        var extracted = new LinkedHashMap<String, Declaration>();
        for (var declaration : analysis.kafkaDeclarations) extracted.put(key(declaration), declaration);
        for (var entry : raw.entrySet()) if (!extracted.containsKey(entry.getKey()))
            warn(analysis, "kafka-declarations", entry.getValue().evidence(), "단순 검색에서 찾은 선언이 추출 결과에 없음: " + entry.getValue().kind());
        for (var entry : extracted.entrySet()) if (!raw.containsKey(entry.getKey()))
            warn(analysis, "kafka-declarations", entry.getValue().evidence(), "추출한 선언을 단순 검색에서 찾지 못함: " + entry.getValue().kind());
        for (var publication : analysis.kafkaPublications)
            if (analysis.kafkaLinks.stream().noneMatch(l -> l.publicationId().equals(publication.id())))
                warn(analysis, "kafka-topics", publication.evidence(), "구독자 확인 안 됨: " + label(publication));
        for (var subscription : analysis.kafkaSubscriptions)
            if (analysis.kafkaLinks.stream().noneMatch(l -> l.subscriptionId().equals(subscription.id())))
                warn(analysis, "kafka-topics", subscription.evidence(), "발행자 확인 안 됨: " + label(subscription));
        for (String name : List.of("kafka-declarations", "kafka-topics")) {
            var counts = new LinkedHashMap<String, Integer>();
            counts.put("warningCount", (int) analysis.warnings.stream().filter(w -> w.check().equals(name)).count());
            if (name.equals("kafka-declarations")) {
                var kinds = List.of("KafkaListener", "kafkaTemplate.send", "DeadLetterPublishingRecoverer");
                var labels = List.of("Listener", "Send", "Dlq");
                for (int i = 0; i < kinds.size(); i++) {
                    String kind = kinds.get(i), label = labels.get(i);
                    counts.put(Character.toLowerCase(label.charAt(0)) + label.substring(1) + "Count", count(raw.values(), kind));
                    counts.put("extracted" + label + "Count", count(extracted.values(), kind));
                }
            }
            analysis.checks.put(name, counts);
        }
    }
    private static String label(KafkaEndpoint endpoint) {
        if (endpoint.topicName().known())
            return endpoint.service() + ", 토픽 " + endpoint.topicName().value() + ", 토픽 키·환경변수 해당 없음";
        return endpoint.service() + ", " + (endpoint.topicKey().known() ? endpoint.topicKey().value() : "토픽 키 확인 안 됨")
                + ", " + (endpoint.environment().known() ? endpoint.environment().value() : "환경변수 확인 안 됨");
    }
    private static int count(Collection<Declaration> declarations, String kind) {
        return (int) declarations.stream().filter(d -> d.kind().equals(kind)).count();
    }
    private static String key(Declaration declaration) { return declaration.kind() + "@" + declaration.evidence().get(0); }
    private static void warn(Analysis analysis, String check, List<String> evidence, String reason) {
        analysis.warnings.add(new Warning(check, Analysis.UNKNOWN, evidence, reason));
    }
}
