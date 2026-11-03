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

### 환경변수·외부 자원

환경변수는 사용 위치별로 기록한다. `profile`은 기본 YAML의 `base`, 파일 이름의 `local`·`prod` 등, compose의 `compose`다. 프로필을 병합하거나 실행할 프로필을 추정하지 않는다. Java `@Value`가 설정 키를 참조하면 해당 프로필의 YAML 환경변수와 코드 근거를 함께 남긴다. 같은 이름이 여러 항목에 나올 수 있으므로 환경변수 이름 수는 `name`의 중복을 제거해서 센다. 값은 모두 `null`·`저장소에 근거 없음`이다.

외부 자원은 서비스·종류별로 묶는다. 설정 키의 `spring.datasource`·`spring.jpa`는 DB, `spring.data.redis`는 Redis, `spring.kafka`는 Kafka, `spring.elasticsearch`는 Elasticsearch, `s3`는 S3, `pay`는 KakaoPay다. 코드에서는 JPA·Redis·Kafka·Elasticsearch·S3의 클래스·어노테이션 사용과 KakaoPay 클라이언트·Feign URL을 근거로 삼는다. 설정이나 코드 근거가 하나라도 있어야 항목을 내며, 없는 쪽의 근거 배열은 비워 둔다. `details.connectionValues`와 S3의 `details.bucketName`은 `저장소에 근거 없음`이다. 외부 자원의 설정·코드 근거 수는 각각의 배열에서 중복 위치를 제거한 수다.

## 업무 규칙·함정

UTF-8 JSON으로 `knowledge/<대상>/<spring.application.name>.json`에 서비스마다 한 파일을 쓴다. 파일은 `schemaVersion: 1`과 `items` 배열을 담는다. 항목이 없으면 `items: []`로 쓴다.

| 필드 | 내용 |
|---|---|
| `service` | 서비스의 `spring.application.name` |
| `title` | 제목 |
| `description` | 발생 조건과 동작·영향 설명 |
| `evidence` | 하나 이상의 근거 `파일:줄` |
| `affectedIds` | 하나 이상의 영향받는 엔드포인트·Feign 호출·Kafka 발행·구독 ID |
| `source` | `AI 해석` 또는 `판정` |
| `discoveryPath` | `코드` 또는 `실제 응답` |
| `confirmationStatus` | `확인함`, `틀림`, `미확인` 중 개발자가 정한 값 |

코드 근거는 고정 커밋의 대상 루트 기준 경로, 응답 근거는 `analysis/<대상>/responses/<파일>.json:줄`이다. 응답 파일을 근거에 포함하면 발견 경로는 `실제 응답`이다. AI 해석의 출처는 `AI 해석`, 확인 상태는 `미확인`으로 시작한다.

검증기는 항목 파일을 스키마로 검사하고, 근거 파일에 해당 줄이 존재하는지와 영향 ID가 `endpoints`, `feignCalls`, `kafkaPublications`, `kafkaSubscriptions` 중 하나에 있는지만 검사한다. 설명과 줄 내용을 대조하거나 확인 상태를 바꾸지 않는다. 어긋남은 각각 `schema`, `knowledge-evidence`, `knowledge-affected-ids` 경고다.

`analysis/<대상>`을 입력하면 같은 기준 위치의 `knowledge/<대상>`을 읽는다(절대 입력 경로도 같은 구조). 파일 이름순으로 `items`를 합쳐 `out/analysis.json`의 `knowledge` 배열에 원문 그대로 넣고 경고는 기존 `warnings`·`checks`에 더한다. `틀림`도 이 검증 출력에는 보존하며, 이후 문서 페이지·검색 색인에서 제외한다. 항목 디렉터리가 없으면 `knowledge` 배열과 두 항목 검사 집계를 추가하지 않아 기존 출력과 같다. 서버는 이 출력 하나만 읽는다.
