package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import static io.hotdealwave.verifier.Checks.*;

final class MessageChecks {
    void run(ObjectNode analysis, SourceText source) throws java.io.IOException {
        String composeFile = source.composeFile();
        for (var link : analysis.path("messageLinks")) {
            var pub = find(analysis.path("messagePublications"), "id", link.path("publicationId").asText());
            var sub = find(analysis.path("messageSubscriptions"), "id", link.path("subscriptionId").asText());
            String environment = link.path("environment").asText();
            var evidence = items(link.path("evidence"));
            boolean same = known(pub) && known(sub) && pub.path("kind").equals(sub.path("kind"))
                    && !UNKNOWN.equals(link.path("status").asText());
            if (environment.isEmpty()) {
                // A topic name written on both sides is the same topic; the link carries both name lines.
                String topic = value(pub.path("topicName"));
                same &= known(pub.path("topicName")) && known(sub.path("topicName")) && topic != null && !topic.isEmpty()
                        && CommunicationRules.sameDestination(pub.path("kind").asText(), topic, value(sub.path("topicName")), source.evidenceLines(link))
                        && evidence.containsAll(items(pub.path("topicName").path("evidence")))
                        && evidence.containsAll(items(sub.path("topicName").path("evidence")));
            } else {
                same &= !pub.path("kind").asText().equals("rabbit") && known(pub.path("environment")) && known(sub.path("environment"))
                        && environment.equals(value(pub.path("environment"))) && environment.equals(value(sub.path("environment")));
                for (var endpoint : List.of(pub, sub)) {
                    var compose = items(endpoint.path("environment").path("evidence")).stream()
                            .filter(e -> !composeFile.isEmpty() && e.asText().startsWith(composeFile + ":")).toList();
                    same &= !compose.isEmpty() && evidence.containsAll(compose);
                }
            }
            verdict(link, same ? CONFIRMED : UNKNOWN, same ? null : environment.isEmpty()
                    ? "발행·구독의 토픽 이름과 양쪽 근거가 일치하지 않음" : "발행·구독의 환경변수 이름과 양쪽 compose 근거가 일치하지 않음");
            if (!same) warn(analysis, "message-destinations", "extraction-mismatch", link.path("evidence"), link.path("reason").asText());
        }
        for (var publication : analysis.path("messagePublications")) {
            if (items(analysis.path("messageLinks")).stream().noneMatch(l -> known(l) && l.path("publicationId").asText().equals(publication.path("id").asText()))) {
                warn(analysis, "message-destinations", code(publication), publication.path("evidence"), "구독자 확인 안 됨: " + reason(publication, "구독"));
            }
        }
        for (var subscription : analysis.path("messageSubscriptions")) {
            if (items(analysis.path("messageLinks")).stream().noneMatch(l -> known(l) && l.path("subscriptionId").asText().equals(subscription.path("id").asText()))) {
                warn(analysis, "message-destinations", code(subscription), subscription.path("evidence"), "발행자 확인 안 됨: " + reason(subscription, "발행"));
            }
        }
        count(analysis, "message-destinations");
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
