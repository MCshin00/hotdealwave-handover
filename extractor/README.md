# REST·Feign·게이트웨이·Kafka·설정 추출기

저장소 루트에서 실행한다. JDK 17 이상이 필요하다.

```bash
./gradlew test
./gradlew :extractor:run
# 선택: 입력·출력 경로 변경 (작업 디렉터리는 항상 저장소 루트)
./gradlew :extractor:run --args="target/HotDealWave out/analysis.json"
```

기본 입력은 `target/HotDealWave`, 출력은 `out/analysis.json`이다. 분석 대상을 빌드하거나 수정하지 않는다. 서비스의 `application.yml`, `application-<프로필>.yml`, `src/main/java/**/*.java`와 루트 `docker-compose.yml`, `.github/workflows/actions.yml`을 읽는다. `.env` 계열과 최종 질문은 입력에 포함하지 않는다. 유료 모델은 호출하지 않는다.

## analysis.json 계약 (schemaVersion: 1)

UTF-8 JSON이다. 배열 순서는 서비스·소스 경로 순서이며, ID는 한 결과 파일 안에서만 참조한다. 시각·로컬 절대경로는 쓰지 않는다. 같은 소스는 같은 결과를 낸다.

각 사실은 `status`, `evidence`를 가진다. `evidence`는 `target/HotDealWave` 기준 `/` 구분 상대경로의 `파일:줄` 배열이다. 줄은 1부터 센다. 선언의 확인 상태와 그 하위 타입·연결의 확인 상태는 별개다.

값에 별도의 확인 상태가 필요한 곳은 다음 공통 형식(`Fact<T>`)을 쓴다.

```json
{
  "value": "/api/v1/orders",
  "status": "확인됨",
  "evidence": ["order/src/main/java/com/sparta/hotdeal/order/presentation/controller/OrderController.java:35"],
  "reason": null
}
```

`status`는 `확인됨`, `확인 안 됨`, `저장소에 근거 없음` 중 하나다. 뒤의 두 상태는 `value: null`과 `reason`을 가진다. 선언에 속성이 없다는 사실을 확인한 경우(Feign `url`·`path`, 없는 `RewritePath`)는 빈 문자열을 쓴다. 소비자는 빈 문자열과 확인할 수 없는 `null`을 구별해야 한다.

| 최상위 키 | 내용 |
|---|---|
| `schemaVersion` | 정수 `1` |
| `services` | `directory`, `name: Fact<string>`, 상태·근거. `build.gradle`과 `src/main/java`가 있는 폴더이며 이름은 YAML의 `spring.application.name` |
| `endpoints` | `id`, `service`, `controller`, `handler`, `httpMethod: Fact<string>`, `path: Fact<string>`, `parameters`, `response`, `externalPaths`, `tokenRequired: Fact<boolean>`, 상태·근거 |
| `feignClients` | `id`, `service`, `type`, `name/url/path: Fact<string>`, 상태·근거 |
| `feignCalls` | `id`, `clientId`, `service`, `handler`, `httpMethod/path: Fact<string>`, `external`, `target: Fact<string>`, 상태·근거 |
| `gatewayRoutes` | `id`, `uri/targetService/rewritePath: Fact<string>`, `paths: Fact<string>[]`, 상태·근거 |
| `allowedPaths` | enum 상수의 `name`, `path/httpMethod: Fact<string>`, 상태·근거 |
| `declarations` | 추출한 매핑 선언의 `kind`, 상태·근거. Feign 클래스 선언은 `feignClients`에 있음 |
| `kafkaDeclarations` | `KafkaListener`, `kafkaTemplate.send`, `DeadLetterPublishingRecoverer` 선언의 `kind`, 상태·근거. 래퍼의 호출 횟수와 무관한 원래 선언 위치 |
| `kafkaPublications`, `kafkaSubscriptions` | `id`, `service`, `kind`, `profile`, `topicKey/topicName/environment: Fact<string>`, 상태·근거 |
| `kafkaLinks` | `publicationId`, `subscriptionId`, `environment`(환경변수 이름), 상태·양쪽 근거 |
| `environmentVariables` | `service`, `profile`, `name`, `location`(설정 키 또는 참조 종류), `value: Fact<string>`, 상태·근거 |
| `externalResources` | `service`, `kind`, `configurationEvidence`, `codeEvidence`, `details: {항목: Fact<string>}`, 상태·전체 근거 |
| `deploymentSteps` | `kind`, `details: {항목: Fact<string>}`, 상태·근거 |
| `checks` | `declarations`, `feign-targets`, `gateway-routes`, `kafka-declarations`, `kafka-topics`별 `warningCount`. 기존 선언 검사는 `mappingCount`, `feignClientCount`, `extractedMappingCount`, `extractedFeignClientCount`도 포함. Kafka 선언 검사는 `listenerCount/sendCount/dlqCount`와 `extractedListenerCount/extractedSendCount/extractedDlqCount` 포함 |
| `warnings` | `check`, `status`, `evidence`, `reason`. 판정 전 상태는 `확인 안 됨` |

`httpMethod`의 `*`는 메서드 제한이 없는 `RequestMapping`이다. 내부 Feign 호출의 `target.value`는 `endpoints[].id`이며 근거는 호출·서비스 이름·컨트롤러 양쪽을 포함한다. 외부 URL 호출은 `external: true`, `target.status: 저장소에 근거 없음`이다. 외부 호출을 내부 연결 실패 경고로 세지 않는다.

### 파라미터와 DTO

`parameters[]`는 `name`, `kind`, `binding`, `schema`, `annotations`, 상태·근거다. `kind`는 `경로`, `쿼리`, `본문`(명시된 헤더가 있으면 `헤더`)이며 `binding`은 실제 바인딩 어노테이션 또는 `Pageable`이다. `ModelAttribute`는 쿼리·폼 바인딩으로 분류하며 JSON 본문을 뜻하지 않는다. `AuthenticationPrincipal`은 제외한다. `Pageable`은 쿼리 인자로 보존하되 프레임워크의 기본 필드·기본값을 만들어 내지 않는다.

`schema`와 `response`의 공통 타입 구조:

- `type`: 소스 타입 표기. 제네릭 필드의 `T`는 실제 타입으로 바뀐다.
- `qualifiedName`: 해석된 클래스 이름 또는 `null`.
- `kind`: `object`, `enum`, `scalar`, `container`, `array`, `unresolved`.
- `fields`: `{name, schema, annotations, jsonName: Fact<string>, jsonIgnored: Fact<boolean>, status, evidence}[]`.
- `arguments`: 제네릭 인자 또는 배열 원소의 타입 구조 배열.
- `enumValues`: 소스 enum 상수 이름 배열.
- `recursive`: 같은 소스 클래스가 현재 탐색 경로에 다시 등장하면 `true`. 이 노드의 필드 전개를 멈춘다.
- `status`, `evidence`, `reason`: 소스 클래스면 클래스 이름 위치, 외부 타입이면 그 타입을 쓴 위치. Spring 등 해석하지 못한 외부 타입은 `unresolved`·`확인 안 됨`으로 두고, 그 안의 로컬 제네릭 인자는 계속 전개한다.

`annotations[]`는 `{name, arguments: {인자명: Fact<string>}, status, evidence}`다. 숫자·불리언도 소스 리터럴 문자열로 기록한다. DTO의 검증 어노테이션과 `JsonProperty`, `JsonIgnore`를 보존한다. `JsonIgnore` 필드도 삭제하지 않고 `jsonIgnored.value: true`로 표시한다. 같은 서비스의 상수만 따라가며, 다른 모듈의 상수·계산식은 근거와 함께 `확인 안 됨`으로 남긴다.

`name`은 Java 필드 이름이다. `jsonName`은 다음 순서로 정하며, 이름을 확정할 수 없으면 `value: null`, `확인 안 됨`과 이유·근거를 남긴다.

1. 필드의 `@JsonProperty` 값. 리터럴·같은 서비스의 상수로 해석되면 `확인됨`이다.
2. 클래스나 필드에 `@JsonNaming`이 있으면 `확인 안 됨`이다. 이름 전략은 구현하지 않는다.
3. 클래스나 필드의 Lombok `@Getter`·`@Data`로 getter 이름을 구하고 Jackson 속성 이름을 정한다(`확인됨`). 원시 `boolean isUsed`는 `isUsed()` → `used`, `Boolean isUsed`는 `getIsUsed()` → `isUsed`다. 원시 boolean의 `is` 접두사는 그 다음 글자가 대문자일 때만 이 규칙을 적용한다. 근거에는 필드와 Lombok 어노테이션 줄을 모두 넣는다.
4. 같은 클래스에 직접 선언한 public 인스턴스 getter(인자 없는 `getX()` 또는 boolean·Boolean을 반환하는 `isX()`)가 있으면 그 이름에서 정한다(`확인됨`). 근거에는 getter 줄을 넣는다.
5. public 필드면 필드 이름을 쓴다(`확인됨`).
6. 나머지는 `확인 안 됨`이다.

전역 ObjectMapper 설정 등 다른 Jackson 설정은 따라가지 않는다. 고정 소스에는 record 선언이 없으며, 현재 record 컴포넌트를 별도 필드로 전개하지 않는 동작은 유지한다.

### 외부 경로와 토큰

`externalPaths[]`는 `{routeId, path, tokenRequired: Fact<boolean>, status, evidence}`다. 기본 API 경로와 서비스 접두사를 제거하는 별칭을 모두 내며, 실제 `Path` 조건 및 정방향 `RewritePath` 적용 결과를 확인한다. 역변환은 고정 소스의 `/<서비스>/(?<segment>.*), /$\{segment}` 형태만 지원한다. 확정하지 못한 외부 경로는 만들지 않고 교차 검증 경고를 낸다.

엔드포인트의 `tokenRequired`는 기본 API 경로의 판정이며, 기본 경로가 없으면 첫 외부 경로의 판정이다. 외부 경로 자체가 없으면 `확인 안 됨`이다. 토큰 허용은 `AllowedPath.matches`의 `startsWith`와 메서드 비교를 따른다. 재작성 전·후의 허용 결과가 다른 별칭은 필터 실행 순서를 확정하지 않으므로 `확인 안 됨`과 양쪽 결과를 남긴다. 서비스 안의 역할별 권한 검사는 포함하지 않는다.

### 교차 검증

선언 검사는 JavaParser와 별도로 Java 파일을 줄 단위 정규식으로 읽는다. 매핑·Feign 선언의 종류와 `파일:줄`을 추출 결과와 대조하며, 주석 안의 선언도 단순 검색 결과로 유지한다. Feign 검사는 이름·HTTP 메서드·경로로 연결되지 않은 내부 호출을, 게이트웨이 검사는 어느 컨트롤러에도 닿지 않는 각 Path 패턴과 외부 경로가 없는 엔드포인트를 경고로 낸다. 경로 변수 이름은 연결에 영향을 주지 않는다.

Kafka 선언 검사(`kafka-declarations`)도 JavaParser와 독립적인 정규식 검색으로 종류와 `파일:줄`을 대조한다. `@KafkaListener`, `kafkaTemplate.send(...)`, `new DeadLetterPublishingRecoverer(...)`를 세며 메일 등 다른 `send`는 세지 않는다. 같은 위치의 선언은 한 번만 센다. `kafka-topics`는 확인된 연결이 없는 발행·구독 항목마다 경고를 낸다. 같은 서비스·토픽이라도 서로 다른 발행 경로이면 경고도 별개다. 실행 끝에 다섯 검사 각각의 경고 수를 출력한다.

경고는 실행 실패가 아니며 개발자가 판정한다. 파싱 자체에 실패하면 성공한 결과로 내보내지 않고 실행을 실패시킨다.

### Kafka 토픽과 연결

구독은 `@KafkaListener(topics = "${spring.kafka.topics.x}")`, 발행은 `kafkaTemplate.send`의 첫 인자와 `DeadLetterPublishingRecoverer` 안의 `TopicPartition` 토픽을 출발점으로 삼는다. 한 서비스의 메서드 인자를 호출 지점으로 거슬러 올라가 `@Value` 필드를 찾는다. private 래퍼와 여러 메서드를 거치는 인자도 따라간다. 하나의 `send`에 서로 다른 호출 경로로 토픽이 들어오면 경로별 발행 항목을 만든다.

`topicKey`는 전체 설정 키이고, `environment`는 해당 서비스의 기본 `application.yml` → compose `environment`까지 확인한 환경변수 이름이다. Kafka의 `profile`은 `base`이며 프로필별 토픽 재정의는 연결에 합치지 않는다. 코드·인자 전달·필드·YAML·compose 사슬을 각 항목의 근거에 보존한다. compose에서 `ENV=${ENV}`로 같은 이름을 전달한 경우에만 환경변수를 확정한다. 두 항목의 환경변수 이름이 같을 때만 `kafkaLinks`에 연결을 내며 양쪽 compose 줄도 포함한다. 다른 이름의 후보 연결은 만들지 않는다.

문자열 리터럴 토픽(같은 서비스의 문자열 상수 포함)은 `topicName`에 이름·`확인됨`·근거 줄을 남기고, 해당 없는 `topicKey`·`environment`는 빈 문자열·`확인됨`으로 표시한다. 환경변수 사슬이 확인된 토픽의 실제 `topicName`은 `null`·`저장소에 근거 없음`이며, 문자열 토픽과 환경변수 토픽은 연결하지 않고 미연결 경고에 문자열 토픽 이름을 포함한다.

분기·재할당, 컬렉션 접근, 다른 모듈의 값은 `확인 안 됨`과 해당 줄을 남긴다. 발행·구독의 최상위 상태는 문자열 토픽 이름 또는 환경변수 사슬의 확인 상태이고, 선언 자체의 존재는 `kafkaDeclarations`에서 별도로 확인할 수 있다.

### 환경변수·외부 자원·배포

환경변수는 사용 위치별로 기록한다. `profile`은 기본 YAML의 `base`, 파일 이름의 `local`·`prod` 등, compose의 `compose`, Actions 참조의 `workflow`다. 프로필을 병합하거나 실행할 프로필을 추정하지 않는다. Java `@Value`가 설정 키를 참조하면 해당 프로필의 YAML 환경변수와 코드 근거를 함께 남긴다. Actions의 `secrets`·`env` 참조는 `service: deployment`로 구별한다. 같은 이름이 여러 항목에 나올 수 있으므로 환경변수 이름 수는 `name`의 중복을 제거해서 센다. 값은 모두 `null`·`저장소에 근거 없음`이다.

외부 자원은 서비스·종류별로 묶는다. 설정 키의 `spring.datasource`·`spring.jpa`는 DB, `spring.data.redis`는 Redis, `spring.kafka`는 Kafka, `spring.elasticsearch`는 Elasticsearch, `s3`는 S3, `pay`는 KakaoPay다. 코드에서는 JPA·Redis·Kafka·Elasticsearch·S3의 클래스·어노테이션 사용과 KakaoPay 클라이언트·Feign URL을 근거로 삼는다. 설정이나 코드 근거가 하나라도 있어야 항목을 내며, 없는 쪽의 근거 배열은 비워 둔다. `details.connectionValues`와 S3의 `details.bucketName`은 `저장소에 근거 없음`이다. 외부 자원의 설정·코드 근거 수는 각각의 배열에서 중복 위치를 제거한 수다.

배포 단계는 Actions에 적힌 `ecr-login`, `image-build`, `image-push`, `ec2-connect`, `compose-deploy`다. 같은 종류의 여러 명령은 근거를 합친다. `ec2-connect`에는 선행 job(`needs`)과 SSH action 근거를 함께 남기며 `details.ec2Address/username/key`는 `저장소에 근거 없음`이다. 이는 배포 스크립트의 존재를 확인하는 것이며 실제 배포 성공이나 서버 구성을 뜻하지 않는다. 원문 설정값·명령문·비밀값은 새 결과 항목에 복사하지 않는다.
