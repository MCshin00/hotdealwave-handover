# 검증기

`./gradlew :verifier:run`은 기본 입력 `analysis/hotdealwave`의 `services/*.json`과 `relations.json`, `target.json`을 [서비스 스키마](../docs/format/service.schema.json)·[관계 스키마](../docs/format/relations.schema.json)·[대상 스키마](../docs/format/target.schema.json)로 먼저 검사한다. 어긋남은 `schema` 경고로 내고, 파일들을 합쳐 기존 검사를 실행한다. 출력은 `out/analysis.json` 하나다. 입력·대상·출력은 `./gradlew :verifier:run --args="analysis/hotdealwave target/HotDealWave out/analysis.json"`으로 지정한다.

선언 검사는 Java 파일을 줄 단위 정규식으로 읽는다. 매핑·Feign 선언의 종류와 `파일:줄`을 [추출 결과](../docs/format/README.md)와 대조하며, 줄 주석과 블록 주석 안의 선언은 세지 않는다. 문자열 안의 `//`·`/*`는 주석으로 보지 않는다. 근거 줄 검사는 주석을 지우지 않은 원문을 읽는다. 근거로 읽는 파일은 `.java`, `.yml`, `build.gradle`, `Dockerfile`, `.xml`, `.md`다. 입력 디렉터리에 `repository.json`(배포·모니터링)이 있으면 스키마로 검사해 출력의 `repository`에 넣고 같은 근거 줄 검사를 한다. 배포·모니터링 항목은 첫 근거가 항목의 설정 파일에 있어야 한다. 비밀값과 로그 전송 주소의 실행 환경 값은 추출이 적은 사용 모양(`reference`·`hostReference`)이 근거 줄에 있으면 `저장소에 근거 없음`, 실행 정의를 찾지 못한 모니터링 도구는 근거 없이 `저장소에 근거 없음`이다. Feign 검사는 이름·HTTP 메서드·경로로 연결되지 않은 내부 호출을, 게이트웨이 검사는 어느 컨트롤러에도 닿지 않는 각 Path 패턴과 외부 경로가 없는 엔드포인트를 경고로 낸다. 경로 변수 이름은 연결에 영향을 주지 않는다.

Kafka 선언 검사(`kafka-declarations`)도 주석을 지운 줄에서 정규식 검색으로 종류와 `파일:줄`을 대조한다. `@KafkaListener`, `kafkaTemplate.send(...)`, `new DeadLetterPublishingRecoverer(...)`를 세며 메일 등 다른 `send`는 세지 않는다. 같은 위치의 선언은 한 번만 센다. `kafka-topics`는 확인된 연결이 없는 발행·구독 항목마다 경고를 낸다. 같은 서비스·토픽이라도 서로 다른 발행 경로이면 경고도 별개다. `handler-calls`는 `relations.json`의 `calls`를 검사한다. 출발·도착 ID가 추출 결과에 있고, 첫 근거가 출발 항목의 첫 근거이며, 마지막 근거가 Feign 메서드를 부르는 줄(`.메서드(`)이거나, 도착 발행 항목의 첫 근거(`send` 선언 줄)이면서 사슬에 다른 `kafkaTemplate.send` 발행과 겹치지 않는 그 발행의 근거 줄이 있으면 그 연결은 `확인됨`이다. 아니면 `확인 안 됨`과 경고다. 사슬 중간 줄이 실제로 이어지는지는 보지 않는다. 확인된 연결의 도착이 아닌 Feign 호출과 `kafkaTemplate.send` 발행은 `호출하는 곳 확인 안 됨` 경고를 낸다. `error-throws`는 `relations.json`의 `throws`를 검사한다. 출발 엔드포인트가 있고, 첫 근거가 그 엔드포인트의 첫 근거이며, 마지막 근거 줄에서 문장 끝(`;`)까지 `new <예외>(`와 넘기는 오류 코드·메시지가 있고, 오류 코드가 같은 서비스의 `errorCodes`에 있고, `handlerId`가 `exceptionHandlers`에 있으면 `확인됨`이다. 아니면 `확인 안 됨`과 경고다. 예외 처리기의 상태·오류 코드·메시지는 근거 줄에서 `source`대로 값이나 읽는 곳(`getErrorCode()`, `getMessage()`, `getDefaultMessage()`)을 확인하고, 예외 메시지와 검증 메시지는 값이 다른 곳에서 정해지고, 분기는 처리기 안의 조건(`if (`)에 따라 값이 갈리므로 `확인 안 됨`과 이유를 남긴다. 실행 끝에 스키마·근거 줄 검사와 일곱 교차 검사 각각의 경고 수를 출력한다.

경고는 실행 실패가 아니며 개발자가 판정한다.

## 업무 규칙·함정 검사

입력이 `analysis/<대상>`이면 같은 기준 위치의 `knowledge/<대상>/*.json`을 [항목 스키마](../docs/format/knowledge.schema.json)로 검사한다. 절대 입력 경로도 `analysis/`와 `knowledge/`가 형제 디렉터리인 구조를 따른다. CLI 인자는 그대로다.

- 필수 필드·타입·허용 값이 어긋나면 기존 `schema` 경고에 더한다.
- 근거가 고정 커밋의 대상 파일이나 해당 입력의 `responses/` 파일에 있고 줄 번호가 실제 길이 안이면 근거 존재 검사를 통과한다. 없으면 `knowledge-evidence` 경고를 낸다. 응답 근거 표기는 `analysis/<대상>/responses/<파일>.json:줄`이다. 추출 근거 검사에서 응답을 건너뛰는 동작과 달리, 항목의 응답 근거는 실제 줄 존재를 검사한다.
- 영향 ID는 추출 결과의 `endpoints`, `feignCalls`, `kafkaPublications`, `kafkaSubscriptions`에서 찾는다. 없으면 `knowledge-affected-ids` 경고를 낸다.
- 서비스가 다른 AI 해석 항목끼리 영향 ID 목록(순서 무관)이 같으면 `knowledge-duplicates` 경고를 낸다. 같은 문제가 두 서비스 결과에 따로 쓰인 경우를 찾는다.

설명과 근거 줄 내용을 대조하지 않고 `confirmationStatus`도 바꾸지 않는다. `틀림`인 항목도 스키마·근거·영향 ID를 검사하지만 `out/analysis.json`의 `knowledge` 배열에는 넣지 않는다. 나머지 항목은 경고 유무와 관계없이 파일 이름순으로 원문 그대로 합친다. 서버는 이 출력 하나만 읽는다.

항목 디렉터리가 없으면 항목 배열·검사 집계도 추가하지 않아 기존 출력이 유지된다. 대표 테스트는 임시 항목 파일에서 코드·응답의 범위 밖 줄 번호, 존재하지 않는 영향 ID, 필수 필드 누락을 경고로 내는지와 확인 상태 보존을 확인한다.

## 응답 확인 상태

입력 디렉터리의 `responses/<엔드포인트 ID>.json`을 읽어 엔드포인트마다 `responseCheck`(상태, HTTP 코드, 이유, 파일 경로, 원문 줄 배열)를 붙인다. `statusCode`가 null이 아니면 `응답 기록 있음`, null이면 `호출 못 함`, 파일이 없으면 `코드만`이다. 실제 경로가 응답 디렉터리 밖이거나 제외 대상인 파일은 읽지 않고 `코드만`으로 둔다. 경고를 내지 않고 엔드포인트의 확인 상태도 바꾸지 않는다. 필드는 [형식 문서](../docs/format/README.md#응답-확인-상태)를 따른다.
