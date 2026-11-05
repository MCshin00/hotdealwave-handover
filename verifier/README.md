# 검증기

`verify` 명령(`java -jar build/handover.jar verify`)은 작업 폴더의 `services/*.json`과 `relations.json`, `target.json`을 [서비스 스키마](../docs/format/service.schema.json)·[관계 스키마](../docs/format/relations.schema.json)·[대상 스키마](../docs/format/target.schema.json)로 먼저 검사한다. 어긋남은 `schema` 경고로 내고, 파일들을 합쳐 기존 검사를 실행한다. 대상 코드는 작업 폴더의 `source/`이고, 출력은 작업 폴더의 `out/analysis.json` 하나다. 스키마는 jar 안에 들어 있어 어느 폴더에서 불러도 같다.

선언 검사는 Java 파일을 줄 단위 정규식으로 읽는다. 매핑·HTTP 클라이언트·메시지 선언의 종류와 `파일:줄`을 [추출 결과](../docs/format/README.md)와 대조하며, 줄 주석과 블록 주석 안의 선언은 세지 않는다. 문자열 안의 `//`·`/*`는 주석으로 보지 않는다. 근거 줄 검사는 주석을 지우지 않은 원문을 읽는다. 근거로 읽는 파일은 `.java`, `.yml`, `.yaml`, `.properties`, `build.gradle`, `build.gradle.kts`, `Dockerfile`, `.xml`, `.md`다. 선언 목록은 빌드 파일과 `src/main/java`가 있는 모든 모듈(하위 폴더 포함, 공통 모듈 포함, `build/`·`target/`·숨김 폴더 제외)에서 만든다. 작업 폴더에 `repository.json`(배포·모니터링)이 있으면 스키마로 검사해 출력의 `repository`에 넣고 같은 근거 줄 검사를 한다. 배포·모니터링 항목은 첫 근거가 항목의 설정 파일에 있어야 한다. 비밀값과 로그 전송 주소의 실행 환경 값은 추출이 적은 사용 모양(`reference`·`hostReference`)이 근거 줄에 있으면 `저장소에 근거 없음`, 실행 정의를 찾지 못한 모니터링 도구는 근거 없이 `저장소에 근거 없음`이다. HTTP 대상 검사는 이름·HTTP 메서드·경로로 연결되지 않은 내부 호출을, 게이트웨이 검사는 어느 컨트롤러에도 닿지 않는 각 Path 패턴과 외부 경로가 없는 엔드포인트를 경고로 낸다. 경로 변수 이름은 연결에 영향을 주지 않는다.

HTTP 선언 검사(`http-declarations`)와 메시지 선언 검사(`message-declarations`)는 주석을 지운 줄에서 종류와 `파일:줄`을 대조한다. 종류별 선언 줄 규칙은 `CommunicationRules.RULES`에 모여 있고, 선언 목록·첫 근거·호출 끝 검사가 같은 규칙을 쓴다. HTTP는 OpenFeign·RestTemplate·RestClient·WebClient·HTTP 인터페이스, 메시지는 Kafka·RabbitMQ·Spring Cloud Stream을 다룬다. 새 방식은 이곳의 규칙과 추출 Skill의 읽는 법을 추가한다. Kafka의 선언 규칙은 다음과 같다. `@KafkaListener`, 같은 파일에 선언한 `KafkaTemplate` 필드·파라미터(와 `kafkaTemplate`)의 `send(...)`, `new DeadLetterPublishingRecoverer(...)`를 세며 메일 등 다른 `send`는 세지 않는다. 같은 위치의 선언은 한 번만 센다. `message-destinations`는 확인된 연결이 없는 발행·구독 항목마다 경고를 낸다. 같은 서비스·토픽이라도 서로 다른 발행 경로이면 경고도 별개다. `handler-calls`는 `relations.json`의 `calls`를 검사한다. 출발·도착 ID가 추출 결과에 있고, 첫 근거가 출발 항목의 첫 근거이며, 마지막 근거가 Feign·HTTP 인터페이스 메서드를 부르는 줄(`.메서드(`), 직접 HTTP 호출 항목의 첫 근거 줄이거나, 도착 발행 항목의 첫 근거(`send` 선언 줄)이면서 사슬에 다른 직접 메시지 발행과 겹치지 않는 그 발행의 근거 줄이 있으면 그 연결은 `확인됨`이다. 아니면 `확인 안 됨`과 경고다. 사슬 중간 줄이 실제로 이어지는지는 보지 않는다. 확인된 연결의 도착이 아닌 HTTP 호출과 직접 메시지 발행은 `호출하는 곳 확인 안 됨` 경고를 낸다. `error-throws`는 `relations.json`의 `throws`를 검사한다. 출발 엔드포인트가 있고, 첫 근거가 그 엔드포인트의 첫 근거이며, 마지막 근거 줄에서 문장 끝(`;`)까지 `new <예외>(`와 넘기는 오류 코드·메시지가 있고, 오류 코드가 같은 서비스의 `errorCodes`에 있고, `handlerId`가 `exceptionHandlers`에 있으면 `확인됨`이다. 아니면 `확인 안 됨`과 경고다. 예외 처리기의 상태·오류 코드·메시지는 근거 줄에서 `source`대로 값이나 읽는 곳(`getErrorCode()`, `getMessage()`, `getDefaultMessage()`)을 확인하고, 예외 메시지와 검증 메시지는 값이 다른 곳에서 정해지고, 분기는 처리기 안의 조건(`if (`)에 따라 값이 갈리므로 `확인 안 됨`과 이유를 남긴다. 실행 끝에 스키마·근거 줄 검사와 교차 검사 각각의 경고 수를 출력한다.

HTTP 대상(`http-targets`)은 클라이언트의 기본 주소와 호출 경로를 합쳐 서비스·HTTP 메서드·경로가 맞는 엔드포인트 하나와 대조한다. 외부 URL과 환경에서 주는 주소는 `저장소에 근거 없음`, 동적 주소는 `확인 안 됨`이다. `target-mismatch`를 포함한 경고 이유 코드는 유지한다.

메시지는 같은 `kind`끼리만 연결한다. RabbitMQ 기본 교환기는 라우팅 키와 큐 이름, 명명된 교환기는 관계 근거의 `Binding` 또는 `BindingBuilder`와 큐·교환기 생성 줄을 대조한다. 바인딩을 찾지 못하면 `확인 안 됨`이다. Stream은 `StreamBridge.send` 또는 `@Bean` 함수 이름에서 바인딩의 `destination` 설정까지 확인한다. 기본값·동적 표현을 추정하지 않는다. 줄 단위 규칙으로 읽지 못한 표현은 확인된 사실로 내지 않는다. DLQ 복구기와 함수형 발행은 프레임워크가 실행하므로 `calls` 도착점에서 제외한다.

경고마다 이유 코드(`code`)가 붙는다. 검사 이름과 원인은 일대일이 아니어서(같은 `message-destinations`라도 값이 저장소 밖인 경우와 저장소 안에 상대가 없는 경우가 있다) 사용자는 코드로 할 일을 정한다. 출력의 `warningCodes`에는 나온 코드마다 제목과 할 일 한 줄이, `confirmed`에는 배열마다 "확인됨"이 무엇을 확인했는지 한 줄이 있다. 근거 줄을 읽지 못한 경우는 `file-missing`(파일 없음), `file-type`(읽지 않는 파일 종류), `line-missing`(줄 없음)으로, 줄에 이름·값이 없으면 `extraction-mismatch`로 갈린다.

`responses/`의 응답 파일은 관찰 기록이다. 엔드포인트의 `responseCheck`에 응답 상태·원문과 함께 `comparison`(저장된 응답의 키와 추출한 응답 필드의 JSON 이름 비교: 응답에만 있음, 추출에만 있음, 이름 다름, 비교 보류)을 붙이고, 확인 상태는 바꾸지 않는다. 오류 응답·빈 배열·`null`은 비교하지 않는다. `Page<T>`·`PagedModel<T>`는 `content` 항목만 `T`와 비교한다. 서비스 파일의 필드가 응답 근거(`responses/<파일>.json:줄`)를 쓰면 그 줄에 필드의 JSON 키가 있어야 "확인됨"이다. `unsupported` 항목은 근거 줄에 방식 이름이 있는지 본다.

경고는 실행 실패가 아니며 개발자가 판정한다.

## 업무 규칙·함정 검사

작업 폴더의 `knowledge/*.json`을 [항목 스키마](../docs/format/knowledge.schema.json)로 검사한다.

- 필수 필드·타입·허용 값이 어긋나면 기존 `schema` 경고에 더한다.
- 근거가 고정 커밋의 대상 파일이나 해당 입력의 `responses/` 파일에 있고 줄 번호가 실제 길이 안이면 근거 존재 검사를 통과한다. 없으면 `knowledge-evidence` 경고를 낸다. 응답 근거 표기는 `responses/<파일>.json:줄`이다. 추출 근거 검사에서 응답을 건너뛰는 동작과 달리, 항목의 응답 근거는 실제 줄 존재를 검사한다.
- 영향 ID는 추출 결과의 `endpoints`, `httpCalls`, `messagePublications`, `messageSubscriptions`에서 찾는다. 없으면 `knowledge-affected-ids` 경고를 낸다.
- 서비스가 다른 AI 해석 항목끼리 영향 ID 목록(순서 무관)이 같으면 `knowledge-duplicates` 경고를 낸다. 같은 문제가 두 서비스 결과에 따로 쓰인 경우를 찾는다.

설명과 근거 줄 내용을 대조하지 않고 `confirmationStatus`도 바꾸지 않는다. `틀림`인 항목도 스키마·근거·영향 ID를 검사하지만 `out/analysis.json`의 `knowledge` 배열에는 넣지 않는다. 나머지 항목은 경고 유무와 관계없이 파일 이름순으로 원문 그대로 합친다. 서버는 이 출력 하나만 읽는다.

항목 디렉터리가 없으면 항목 배열·검사 집계도 추가하지 않아 기존 출력이 유지된다. 대표 테스트는 임시 항목 파일에서 코드·응답의 범위 밖 줄 번호, 존재하지 않는 영향 ID, 필수 필드 누락을 경고로 내는지와 확인 상태 보존을 확인한다.

## 응답 확인 상태

작업 폴더의 `responses/<엔드포인트 ID>.json`을 읽어 엔드포인트마다 `responseCheck`(상태, HTTP 코드, 이유, 파일 경로, 원문 줄 배열)를 붙인다. `statusCode`가 null이 아니면 `응답 기록 있음`, null이면 `호출 못 함`, 파일이 없으면 `코드만`이다. 실제 경로가 응답 디렉터리 밖이거나 제외 대상인 파일은 읽지 않고 `코드만`으로 둔다. 경고를 내지 않고 엔드포인트의 확인 상태도 바꾸지 않는다. 필드는 [형식 문서](../docs/format/README.md#응답-확인-상태)를 따른다.
