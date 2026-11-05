# 추출 결과와 업무 규칙·함정 형식

필수 필드·타입·허용 값은 JSON Schema 2020-12의 [서비스 스키마](service.schema.json)와 [관계 스키마](relations.schema.json)를 따른다. 공통 사실 형식은 서비스 스키마의 `$defs.Fact`다.

업무 규칙·함정은 별도 [항목 스키마](knowledge.schema.json)를 따른다. 아래 추출 결과의 상태 판정 규칙과 달리, 항목의 확인 상태는 개발자가 정한다.

UTF-8 JSON으로 `analysis/<대상>/services/<spring.application.name>.json`과 `analysis/<대상>/relations.json`에 쓴다. 배열 순서는 서비스·소스 경로 순서다. ID는 애플리케이션 이름으로 시작하며(엔드포인트·Feign·Kafka), 앞부분으로 서비스 파일을 찾아 참조한다. 엔드포인트 ID는 `<애플리케이션 이름>.<컨트롤러 클래스>.<메서드>`, Feign 클라이언트 ID는 `<애플리케이션 이름>.<클라이언트 인터페이스>`, Feign 호출 ID는 `<클라이언트 ID>.<메서드>`다. 게이트웨이 경로 ID처럼 대상 코드에 적힌 값은 그대로 쓴다. 시각·로컬 절대경로는 쓰지 않는다.

서비스는 `build.gradle`과 `src/main/java`가 있는 폴더이며, `service` 값은 YAML의 `spring.application.name`이다. `service` 필드가 없는 항목은 근거 경로의 첫 디렉터리와 `service.directory`를 대조해 소속을 정한다. 정할 수 없으면 멈추고 보고한다.

근거 경로는 분석 대상 루트 기준이며, 그 줄에 실제 이름·값·타입이 있어야 한다. 선언의 확인 상태와 하위 타입·연결의 확인 상태는 별개다. 추출 결과에는 모든 깊이에서 `status`를 빼고 `checks`·`warnings`도 쓰지 않는다. 검증기가 스키마 검사와 근거·관계 검사를 거쳐 상태·경고를 정하고 `out/analysis.json` 하나로 합친다.

선언에 속성이 없다는 사실을 확인한 경우(Feign `url`·`path`, 없는 `RewritePath`)는 빈 문자열을 쓴다. 소비자는 빈 문자열과 확인할 수 없는 `null`을 구별해야 한다.

`httpMethod`의 `*`는 메서드 제한이 없는 `RequestMapping`이다. 내부 Feign 호출의 `target.value`는 `endpoints[].id`이며 근거는 호출·서비스 이름·컨트롤러 양쪽을 포함한다. 외부 URL 호출은 `external: true`, `target.status: 저장소에 근거 없음`이다. 외부 호출을 내부 연결 실패 경고로 세지 않는다.

`service.composeService`는 그 애플리케이션을 실행하는 compose 서비스 키다. 근거는 키 선언 줄과 대응을 정한 줄(같은 블록의 이름·빌드·이미지 등)이며, compose에 없거나 대응을 정할 수 없으면 `value: null`이다.

`declarations`는 매핑 선언의 원래 위치이며 Feign 클래스 선언은 `feignClients`에 둔다. `kafkaDeclarations`도 래퍼 호출 횟수와 무관한 원래 선언 위치를 기록한다.

### 파라미터와 DTO

`binding`은 실제 바인딩 어노테이션 또는 `Pageable`이다. `ModelAttribute`는 쿼리·폼 바인딩으로 분류하며 JSON 본문을 뜻하지 않는다. `AuthenticationPrincipal`은 제외한다. `Pageable`은 쿼리 인자로 보존하되 프레임워크의 기본 필드·기본값을 만들어 내지 않는다.

`schema`와 `response`의 `type`은 소스 타입 표기이며 제네릭 필드의 `T`는 실제 타입으로 바꾼다. `qualifiedName`은 해석한 클래스 이름, `arguments`는 제네릭 인자 또는 배열 원소, `enumValues`는 소스 enum 상수 이름이다. 같은 소스 클래스가 현재 탐색 경로에 다시 등장하면 `recursive: true`로 두고 필드 전개를 멈춘다. 타입의 근거는 소스 클래스면 클래스 이름 위치, 외부 타입이면 그 타입을 쓴 위치다. Spring 등 해석하지 못한 외부 타입은 `unresolved`·`확인 안 됨`으로 두고, 그 안의 로컬 제네릭 인자는 계속 전개한다.

DTO의 검증 어노테이션을 보존한다. 어노테이션 인자의 숫자·불리언도 소스 리터럴 문자열로 기록한다. 같은 서비스의 상수만 따라가며, 다른 모듈의 상수·계산식은 근거와 함께 `확인 안 됨`으로 남긴다.

`name`은 Java 필드 이름이다. `jsonName`은 다음 순서로 정하며, 이름을 확정할 수 없으면 `value: null`, `확인 안 됨`과 이유·근거를 남긴다.

1. 클래스나 필드의 Lombok `@Getter`·`@Data`로 getter 이름을 구하고 Jackson 속성 이름을 정한다(`확인됨`). 원시 `boolean isUsed`는 `isUsed()` → `used`, `Boolean isUsed`는 `getIsUsed()` → `isUsed`다. 원시 boolean의 `is` 접두사는 그 다음 글자가 대문자일 때만 이 규칙을 적용한다. 근거에는 필드와 Lombok 어노테이션 줄을 모두 넣는다.
2. 같은 클래스에 직접 선언한 public 인스턴스 getter(인자 없는 `getX()` 또는 boolean·Boolean을 반환하는 `isX()`)가 있으면 그 이름에서 정한다(`확인됨`). 근거에는 getter 줄을 넣는다.
3. public 필드면 필드 이름을 쓴다(`확인됨`).
4. 나머지는 `확인 안 됨`이다.

전역 ObjectMapper 설정 등 다른 Jackson 설정은 따라가지 않는다.

### 외부 경로와 토큰

`externalPaths[]`는 기본 API 경로를 내며, 실제 `Path` 조건을 확인한다. 확정하지 못한 외부 경로는 만들지 않고 [검증기](../../verifier/README.md)가 교차 검증 경고를 낸다.

엔드포인트의 `tokenRequired`는 기본 API 경로의 판정이다. 외부 경로 자체가 없으면 `확인 안 됨`이다. 토큰 허용은 `AllowedPath.matches`의 `startsWith`와 메서드 비교를 따른다. 서비스 안의 역할별 권한 검사는 포함하지 않는다.

### Kafka 토픽과 연결

구독은 `@KafkaListener(topics = "${spring.kafka.topics.x}")`, 발행은 `kafkaTemplate.send`의 첫 인자와 `DeadLetterPublishingRecoverer` 안의 `TopicPartition` 토픽을 출발점으로 삼는다. 하나의 `send`에 서로 다른 호출 경로로 토픽이 들어오면 경로별 발행 항목을 만든다.

`topicKey`는 전체 설정 키이고, `environment`는 해당 서비스의 기본 `application.yml` → compose `environment`까지 확인한 환경변수 이름이다. 프로필별 토픽 재정의는 연결에 합치지 않는다. 코드·인자 전달·필드·YAML·compose 사슬을 각 항목의 근거에 보존한다. compose에서 `ENV=${ENV}`로 같은 이름을 전달한 경우에만 환경변수를 확정한다. 두 항목의 환경변수 이름이 같을 때만 `kafkaLinks`에 연결을 내며 양쪽 compose 줄도 포함한다. 다른 이름의 후보 연결은 만들지 않는다.

문자열 리터럴 토픽(같은 서비스의 문자열 상수 포함)은 `topicName`에 이름·`확인됨`·근거 줄을 남기고, 해당 없는 `topicKey`·`environment`는 빈 문자열·`확인됨`으로 표시한다. 환경변수 사슬이 확인된 토픽의 실제 `topicName`은 `null`·`저장소에 근거 없음`이며, 문자열 토픽과 환경변수 토픽은 연결하지 않고 미연결 경고에 문자열 토픽 이름을 포함한다.

따라가지 못한 값은 `확인 안 됨`과 해당 줄을 남긴다. 발행·구독의 최상위 상태는 문자열 토픽 이름 또는 환경변수 사슬의 확인 상태이고, 선언 자체의 존재는 `kafkaDeclarations`에서 별도로 확인할 수 있다.

### 핸들러의 나가는 호출

`relations.json`의 `calls`는 엔드포인트 또는 Kafka 구독(`fromId`)이 같은 서비스의 Feign 호출 또는 `kafkaTemplate.send` 발행(`toId`)에 닿는 연결이다. 서비스·포트·어댑터·래퍼를 거치는 호출과 조건 분기·`catch` 안의 호출을 포함한다. 같은 출발·도착이라도 거치는 경로가 다르면 항목을 따로 둔다. `DeadLetterPublishingRecoverer` 발행은 코드에서 직접 부르지 않으므로 연결을 만들지 않는다. 다른 서비스로 넘어간 뒤의 호출과 역방향 연결은 쓰지 않는다. 소비자가 `calls`, Feign `target`, `kafkaLinks`를 이어 따라간다.

`evidence`는 출발 항목의 첫 근거(매핑·리스너 줄)로 시작하고, 거치는 호출 줄을 순서대로 넣고, 도착 지점 줄로 끝난다. 도착 지점 줄은 Feign이면 그 Feign 메서드를 부르는 줄(`.메서드(`), 발행이면 그 발행 항목의 첫 근거(`send` 선언 줄)다. 여러 발행이 같은 래퍼의 `send` 줄을 쓰면, 사슬에는 그 발행의 근거에만 있는 호출 줄(래퍼를 부르는 줄)도 들어가야 한다.

### 오류 응답

`errorCodes`는 서비스의 오류 코드 enum 상수 하나마다 한 항목이다. `enumType`은 enum 이름, `name`은 상수 이름이고, `code`·`httpStatus`·`message`는 상수 선언 줄의 인자를 쓴 그대로 둔다(`HttpStatus.NOT_FOUND`면 `NOT_FOUND`). enum에 그 인자가 없으면 빈 문자열과 상수 선언 줄이다. 첫 근거는 상수 선언 줄이다.

`exceptionHandlers`는 `@RestControllerAdvice`의 `@ExceptionHandler` 메서드 하나마다 한 항목이고, ID는 `<애플리케이션 이름>.<클래스>.<메서드>`다. `exception`은 처리하는 예외 클래스 이름이며 첫 근거는 `@ExceptionHandler` 줄이다. `httpStatus`·`errorCode`·`message`는 응답 상태, 본문의 오류 코드 상수, 본문 메시지가 어디서 정해지는지를 `source`로 적는다. 오류 응답 DTO 생성자가 값을 채우면 그 생성자 줄까지 따라간다.

| `source` | 뜻 | `value` | 근거 |
|---|---|---|---|
| `고정` | 처리기나 DTO 생성자에 적힌 값. 본문 DTO에 그 칸이 없으면 빈 문자열 | 쓴 그대로의 값 | 값이 적힌 줄, 빈 문자열이면 본문 DTO 선언 줄 |
| `던진 오류 코드` | 예외에 담긴 오류 코드 상수의 값 | `null` | `getErrorCode()` 등을 읽는 줄 |
| `예외 메시지` | 예외 메시지를 그대로 씀 | `null` | `getMessage()`를 읽는 줄 |
| `검증 메시지` | 검증 어노테이션의 메시지를 씀 | `null` | `getDefaultMessage()`를 읽는 줄 |
| `분기` | 처리기 안에서 조건에 따라 값이 갈림 | `null` | 조건 줄과 갈래마다 값을 정하는 줄 |

검증기는 `예외 메시지`·`검증 메시지`·`분기`를 `확인 안 됨`으로 두고 이유를 붙인다. 값은 던지는 곳이나 어노테이션, 처리기 안의 조건에서 정해진다. `body`는 응답 본문 DTO이며 필드는 파라미터·응답 DTO와 같은 형식이다.

`relations.json`의 `throws`는 엔드포인트(`fromId`)의 핸들러에서 같은 서비스의 예외 생성 줄(`new <예외>(`)에 닿는 연결이다. `calls`처럼 서비스·헬퍼·엔티티 메서드를 따라가고, `evidence`는 엔드포인트의 첫 근거로 시작해 거치는 호출 줄을 넣고 예외 생성 줄(`new <예외>(`가 있는 줄)로 끝난다. 생성자 인자가 다음 줄에 있으면 검증기는 문장 끝(`;`)까지 이어 읽는다. `exception`은 생성하는 예외 클래스, `errorCode`는 예외에 넘기는 같은 서비스의 `errorCodes` 상수 이름, `message`는 생성자에 넘기는 문자열 리터럴이다. 해당 없는 쪽은 빈 문자열이다. `handlerId`는 그 예외를 받는 `exceptionHandlers` ID이며 상속을 따라 정한다. 받는 처리기가 없으면 `null`이다. 처리기까지 전파되는 예외만 쓴다. 거치는 길의 `catch`가 잡으면 쓰지 않는다(잡은 곳에서 새 예외를 만들면 그 생성 줄이 따로 연결된다). `CompletableFuture`의 `join()`처럼 예외가 감싸여 나가면 감싼 예외를 받는 처리기를 `handlerId`로 한다. Feign 호출이 실패해 클라이언트 설정의 `ErrorDecoder`가 만든 예외가 나가면, 사슬은 Feign 메서드를 부르는 줄, 디코더를 등록하는 줄, 디코더의 생성 줄로 이어진다. Kafka 구독과 필터에서 던지는 예외는 쓰지 않는다.

`gatewayRejections`는 Gateway 필터가 요청을 서비스로 넘기지 않고 응답을 끝내는 곳이다. `filter`는 필터 클래스 이름이고, `condition`은 거부 조건 식을 쓴 그대로, `httpStatus`는 정하는 상태, `body`는 응답 본문이다. 본문 없이 끝내면 `body`는 빈 문자열이고 근거는 `setComplete()` 줄이다. 첫 근거는 조건 줄이다.

`pageSerialization`은 `@EnableSpringDataWebSupport`의 `pageSerializationMode` 값이다. 어노테이션이 없는 서비스에는 항목을 만들지 않는다.

### 환경변수·외부 자원

환경변수는 사용 위치별로 기록한다. `profile`은 기본 YAML의 `base`, 파일 이름의 `local`·`prod` 등, compose의 `compose`다. 프로필을 병합하거나 실행할 프로필을 추정하지 않는다. Java `@Value`가 설정 키를 참조하면 해당 프로필의 YAML 환경변수와 코드 근거를 함께 남긴다. 같은 이름이 여러 항목에 나올 수 있으므로 환경변수 이름 수는 `name`의 중복을 제거해서 센다. 값은 모두 `null`·`저장소에 근거 없음`이다.

외부 자원은 서비스·종류별로 묶는다. 설정 키의 `spring.datasource`·`spring.jpa`는 DB, `spring.data.redis`는 Redis, `spring.kafka`는 Kafka, `spring.elasticsearch`는 Elasticsearch, `s3`는 S3, `pay`는 KakaoPay다. 코드에서는 JPA·Redis·Kafka·Elasticsearch·S3의 클래스·어노테이션 사용과 KakaoPay 클라이언트·Feign URL을 근거로 삼는다. 설정이나 코드 근거가 하나라도 있어야 항목을 내며, 없는 쪽의 근거 배열은 비워 둔다. `details.connectionValues`와 S3의 `details.bucketName`은 `저장소에 근거 없음`이다. 외부 자원의 설정·코드 근거 수는 각각의 배열에서 중복 위치를 제거한 수다.

## 저장소 수준 사실(배포·모니터링)

서비스 하나에 속하지 않는 배포·모니터링 설정은 [저장소 스키마](repository.schema.json)를 따라 `analysis/<대상>/repository.json` 하나에 쓴다. 파일이 없으면 검증기는 이 부분 없이 진행한다. 검증기는 합친 결과의 `repository`에 그대로 넣고 상태를 정한다.

- `workflows`: `.github/workflows/`의 파일마다 한 항목이다. `triggers`는 이벤트와 브랜치, `env`는 워크플로 수준 변수, `secrets`는 `${{ secrets.X }}`로 쓰는 이름이다. `jobs[].steps[]`는 단계 이름, 쓰는 액션(`uses`, 없으면 키를 두지 않음), 단계 변수, 실행 명령이다. `env`의 값은 워크플로에 쓴 그대로(`${{ ... }}` 식 포함) 둔다. `commands`는 `run`(SSH 액션의 `script` 포함)의 명령 줄을 하나씩 앞뒤 공백만 빼고 쓴 그대로 두며 주석과 빈 줄은 뺀다.
- `secrets[].secretValue`는 `null`이며 검증기가 `저장소에 근거 없음`으로 둔다. 근거는 그 이름을 쓰는 줄이다.
- `images`: compose 서비스마다 이미지 이름(compose `image` 줄), 그 서비스의 `Dockerfile`의 `FROM` 이미지, 빌드 명령(`RUN`), `EXPOSE` 포트다. 첫 근거는 compose 서비스 키 줄이다.
- `scrapeTargets`: Prometheus `scrape_configs`의 작업마다 대상 주소(`address`), 수집 경로, 수집 주기(작업에 없으면 `global`)다. 첫 근거는 `job_name` 줄이다.
- `logShipping`: 서비스의 logback 설정에서 로그를 외부로 보내는 appender다. `url`·`labels`는 쓴 그대로 두고, 주소에 쓰인 변수 이름을 `hostVariable`, 그 값을 `hostValue`(`null`, `저장소에 근거 없음`)로 둔다.
- `lokiConfig`: Loki 설정 파일의 HTTP 포트와 저장소 종류다.
- `monitoringTools`: 저장소에 설정이나 언급이 있는 모니터링 도구마다 한 항목이다. 첫 근거는 설정 파일이나 언급 줄이다. `runDefinition`은 그 도구를 띄우는 정의(compose 서비스 등)이며, 저장소에서 찾지 못하면 `value: null`, 근거 없음(빈 배열)으로 두고 검증기가 `저장소에 근거 없음`으로 둔다.

## 업무 규칙·함정

UTF-8 JSON으로 `knowledge/<대상>/<spring.application.name>.json`에 서비스마다 한 파일을 쓴다. 파일은 `schemaVersion: 1`과 `items` 배열을 담는다. 항목이 없으면 `items: []`로 쓴다.

| 필드 | 내용 |
|---|---|
| `service` | 서비스의 `spring.application.name` |
| `kind` | 종류: `업무 규칙`, `함정`, `설명` |
| `title` | 제목 |
| `description` | 발생 조건과 실제 동작. 함정은 누가 무엇을 하다 어떤 결과를 받는지도 쓴다 |
| `evidence` | 하나 이상의 근거 `파일:줄` |
| `affectedIds` | 하나 이상의 영향받는 엔드포인트·Feign 호출·Kafka 발행·구독 ID |
| `source` | `AI 해석` 또는 `판정` |
| `discoveryPath` | `코드` 또는 `실제 응답` |
| `confirmationStatus` | `확인함`, `틀림`, `미확인` 중 개발자가 정한 값 |

`설명`은 엔드포인트·Kafka 발행·구독 하나가 하는 일을 한국어로 쓴 항목이다. 영향 ID는 그 하나이고, 페이지 맨 위 "설명" 절에 나온다.

코드 근거는 고정 커밋의 대상 루트 기준 경로, 응답 근거는 `analysis/<대상>/responses/<파일>.json:줄`이다. 응답 파일을 근거에 포함하면 발견 경로는 `실제 응답`이다. AI 해석의 출처는 `AI 해석`, 확인 상태는 `미확인`으로 시작한다.

검증기는 항목 파일을 스키마로 검사하고, 근거 파일에 해당 줄이 존재하는지와 영향 ID가 `endpoints`, `feignCalls`, `kafkaPublications`, `kafkaSubscriptions` 중 하나에 있는지, 서비스가 다른 AI 해석 항목끼리 영향 ID 목록이 같은지만 검사한다. 설명과 줄 내용을 대조하거나 확인 상태를 바꾸지 않는다. 어긋남은 각각 `schema`, `knowledge-evidence`, `knowledge-affected-ids`, `knowledge-duplicates` 경고다.

`analysis/<대상>`을 입력하면 같은 기준 위치의 `knowledge/<대상>`을 읽는다(절대 입력 경로도 같은 구조). 모든 항목을 검사하고 경고는 기존 `warnings`·`checks`에 더한다. `confirmationStatus`가 `틀림`인 항목은 `out/analysis.json`의 `knowledge` 배열에 넣지 않는다. 나머지 항목은 파일 이름순으로 원문 그대로 합친다. 항목 디렉터리가 없으면 `knowledge` 배열과 세 항목 검사 집계를 추가하지 않아 기존 출력과 같다. 서버는 이 출력 하나만 읽는다.

## 응답 확인 상태

검증기는 `out/analysis.json`의 엔드포인트마다 `responseCheck`를 붙인다. 입력 디렉터리의 `responses/<엔드포인트 ID>.json`을 읽는다.

| 필드 | 내용 |
|---|---|
| `status` | `응답 기록 있음`(`statusCode`가 null이 아님), `호출 못 함`(`statusCode`가 null), `코드만`(응답 파일 없음) |
| `statusCode` | 응답 파일의 HTTP 상태 코드. 없으면 null |
| `reason` | 응답 파일의 `reason`. 없으면 null |
| `file` | `analysis/<대상>/responses/<엔드포인트 ID>.json`. 응답 파일이 없으면 null |
| `lines` | 응답 파일 원문의 줄 배열. N번째 원소가 원문 N줄이라 응답 근거 `파일:N`을 그대로 찾는다. 응답 파일이 없으면 빈 배열 |

응답 기록은 그 요청 조건에서 관찰한 것이며 엔드포인트의 `status`(확인 상태)를 바꾸지 않는다. 오류 응답도 `응답 기록 있음`이고 `statusCode`로 구분한다.
