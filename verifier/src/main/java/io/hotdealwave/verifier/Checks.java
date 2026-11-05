package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

final class Checks {
    static final String CONFIRMED = "확인됨", UNKNOWN = "확인 안 됨", ABSENT = "저장소에 근거 없음";
    /** Why a warning was raised and what the reader does about it: code → title, action. */
    static final Map<String, List<String>> CODES = new LinkedHashMap<>();
    /** What "확인됨" checked, per array of the output. */
    static final Map<String, String> CONFIRMED_MEANS = new LinkedHashMap<>();
    static {
        CODES.put("outside-value", List.of("저장소 밖 값", "값이 배포 환경의 환경변수에서 정해져 저장소만으로는 잇지 못한다. 배포 환경에서 양쪽 값을 확인한다."));
        CODES.put("unlinked", List.of("저장소 안에서 잇지 못함", "저장소 안에 상대(구독·발행, 부르는 곳, 외부 경로, 받는 처리기)가 없다. 근거 줄을 보고 끊긴 연결인지, 저장소 밖에서 쓰는지 확인한다."));
        CODES.put("target-mismatch", List.of("연결 대상이 어긋남", "추출 오류이거나 코드의 호출과 대상이 실제로 어긋난 것이다. 양쪽 근거 줄을 열어 본다."));
        CODES.put("extraction-mismatch", List.of("근거 줄과 맞지 않음", "추출이 적은 값이나 연결이 근거 줄과 맞지 않는다. 근거 줄을 열어 코드의 값을 직접 본다."));
        CODES.put("inventory-mismatch", List.of("선언 목록과 다름", "단순 검색으로 찾은 선언과 추출 결과가 다르다. 그 위치의 선언을 직접 본다."));
        CODES.put("file-missing", List.of("근거 파일 없음", "고정 커밋에 그 파일이 없다. 그 값은 믿지 않는다."));
        CODES.put("file-type", List.of("읽지 않는 파일 종류", "도구가 이 파일 종류를 읽지 않는다. 다른 구성이면 README의 \"다른 구성에 맞추려면 고칠 곳\"을 본다."));
        CODES.put("line-missing", List.of("근거 줄 없음", "그 파일에 그 줄이 없다. 그 값은 믿지 않는다."));
        CODES.put("schema-invalid", List.of("형식에 맞지 않음", "추출 결과 파일이 형식에 맞지 않아 그 부분을 읽지 못했다. 추출을 맡은 AI 세션이 고친다."));
        CODES.put("unknown-id", List.of("업무 규칙의 대상 없음", "업무 규칙·함정 항목이 가리키는 ID가 추출 결과에 없다. 그 항목은 믿지 않는다."));
        CODES.put("duplicate-item", List.of("업무 규칙 중복", "다른 서비스의 항목이 같은 대상을 가리킨다. 두 항목을 함께 본다."));

        CONFIRMED_MEANS.put("services", "서비스 이름이 근거 줄(설정 파일의 spring.application.name)에 있다.");
        CONFIRMED_MEANS.put("endpoints", "HTTP 메서드와 전체 경로가 클래스·메서드 매핑 어노테이션 줄에서 나온다.");
        CONFIRMED_MEANS.put("fields", "필드 이름·타입이 근거 줄에 있고, JSON 이름은 형식 문서의 순서(@JsonProperty, record, Lombok·getter, public 필드)로 정했다. 응답 근거면 저장된 응답의 그 줄에 JSON 키가 있다.");
        CONFIRMED_MEANS.put("httpCalls", "종류에 맞는 호출 선언·HTTP 메서드·경로를 확인했다. 내부 target은 같은 서비스 이름·HTTP 메서드·경로의 엔드포인트 하나와 대조했다.");
        CONFIRMED_MEANS.put("externalPaths", "게이트웨이 경로 규칙의 Path와 경로 필터를 거쳐 그 컨트롤러 경로에 닿는다.");
        CONFIRMED_MEANS.put("tokenRequired", "허용 목록과 그 비교 방식(앞부분 일치와 HTTP 메서드)으로 판정했다.");
        CONFIRMED_MEANS.put("messagePublications", "목적지 이름이 근거 줄에 있거나, 설정 키에서 설정 파일과 compose까지 환경변수 이름이 이어진다.");
        CONFIRMED_MEANS.put("messageSubscriptions", CONFIRMED_MEANS.get("messagePublications"));
        CONFIRMED_MEANS.put("httpClients", "종류에 맞는 클라이언트 선언 줄과 기본 주소 또는 서비스 이름을 확인했다.");
        CONFIRMED_MEANS.put("messageDeclarations", "종류에 맞는 발행·구독 선언이 첫 근거 줄에 있다.");
        CONFIRMED_MEANS.put("messageLinks", "양쪽이 같은 종류이며 같은 환경변수 이름(양쪽 compose 줄) 또는 같은 목적지를 쓴다. RabbitMQ는 기본 교환기의 큐 이름 또는 Binding 선언을 대조했다.");
        CONFIRMED_MEANS.put("calls", "출발 항목의 첫 줄과 도착 줄(HTTP 메서드 호출, 발행 줄)만 확인했다. 중간 줄이 실제로 이어지는지는 보지 않는다.");
        CONFIRMED_MEANS.put("throws", "출발 엔드포인트와 마지막 줄의 예외 생성·오류 코드·받는 처리기만 확인했다. 중간 줄은 보지 않는다.");
        CONFIRMED_MEANS.put("errors", "오류 코드·처리기·거부 응답의 값이 근거 줄에 쓴 그대로 있다.");
        CONFIRMED_MEANS.put("environmentVariables", "이름이 근거 줄에 있다. 값은 저장소 밖에서 정해진다.");
        CONFIRMED_MEANS.put("repository", "항목의 설정 파일에 첫 근거가 있고 근거 줄에 이름·값이 있다.");
        CONFIRMED_MEANS.put("unsupported", "근거 줄에 그 방식 이름이 있다. 도구는 그 방식을 따라가지 않는다.");
    }

    private Checks() {}
    static List<JsonNode> items(JsonNode node) { return StreamSupport.stream(node.spliterator(), false).toList(); }
    static String value(JsonNode node) { return node.path("value").isTextual() ? node.path("value").asText() : null; }
    static boolean known(JsonNode node) { return CONFIRMED.equals(node.path("status").asText()); }
    static JsonNode find(JsonNode nodes, String key, String value) {
        return items(nodes).stream().filter(n -> n.path(key).asText().equals(value)).findFirst()
                .orElse(com.fasterxml.jackson.databind.node.MissingNode.getInstance());
    }
    static void verdict(JsonNode node, String status, String reason) {
        if (!(node instanceof ObjectNode object)) return;
        object.put("status", status);
        boolean fact = object.has("value") && !object.path("value").isObject();
        if (reason != null || fact || object.has("type")) object.put("reason", reason);
        if (!CONFIRMED.equals(status) && fact) object.putNull("value");
    }
    static void warn(ObjectNode analysis, String check, String code, JsonNode evidence, String reason) {
        if (!CODES.containsKey(code)) throw new IllegalArgumentException(code);
        var warning = analysis.withArray("warnings").addObject().put("check", check).put("code", code).put("status", UNKNOWN);
        warning.set("evidence", evidence.deepCopy());
        warning.put("reason", reason);
    }
    static ObjectNode count(ObjectNode analysis, String check) {
        int count = (int) items(analysis.path("warnings")).stream().filter(w -> check.equals(w.path("check").asText())).count();
        return ((ObjectNode) analysis.path("checks")).putObject(check).put("warningCount", count);
    }
    /** The codes the warnings use, with their title and action, and what "확인됨" means per array. */
    static void legend(ObjectNode analysis) {
        var codes = analysis.putObject("warningCodes");
        for (var entry : CODES.entrySet())
            if (items(analysis.path("warnings")).stream().anyMatch(w -> entry.getKey().equals(w.path("code").asText())))
                codes.putObject(entry.getKey()).put("title", entry.getValue().get(0)).put("action", entry.getValue().get(1));
        var confirmed = analysis.putObject("confirmed");
        CONFIRMED_MEANS.forEach(confirmed::put);
    }

    static void clearVerdicts(JsonNode node) {
        if (node instanceof ObjectNode object) object.remove(List.of("status", "reason"));
        node.forEach(Checks::clearVerdicts);
    }
}
