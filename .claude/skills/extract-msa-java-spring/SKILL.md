---
name: extract-msa-java-spring
description: Spring Boot·Spring Cloud MSA의 대상·결과 디렉터리 경로와 서비스 이름 목록(생략하면 전체), 다시 쓸 절 목록(생략하면 전체)을 받아, 코드를 읽어 서비스 JSON을 쓰고 별도 단계에서 관계 JSON과 저장소 수준 JSON을 쓴다.
---

# 코드에서 구조 추출

## 입력과 경계

- 대상 저장소 경로, 결과 디렉터리 경로, 추출할 서비스의 애플리케이션 이름 목록(생략하면 전체), 다시 쓸 절 목록(생략하면 전체)을 받는다. 대상은 읽기만 하고 빌드하지 않는다.
- 읽는 것은 대상 코드·설정, 이 Skill, [추출 결과 형식](../../../docs/format/README.md)과 그 스키마뿐이다. 형식 속 예시를 추출 사실로 쓰지 않는다. 기존 추출 결과는 읽지 않는다(관계 단계의 서비스 파일과, 절 목록을 받았을 때 그 서비스 파일만 예외).
- `.env`, `.env.*`, `.local/`은 열지 않는다. 환경변수는 이름만 쓴다.
- 코드를 직접 읽고 JSON을 직접 쓴다. 추출 스크립트를 만들거나 별도 모델 API를 부르지 않는다.

## 가정하는 구성

| 부분 | 가정 |
|---|---|
| 서비스 | 빌드 파일(`build.gradle`, `build.gradle.kts`, `pom.xml`)과 `spring.application.name`이 있는 모듈 하나가 서비스 하나(하위 폴더 포함, `build/`·`target/` 제외), 설정은 `application*.yml`·`application*.yaml`·`application*.properties` |
| 게이트웨이 | Spring Cloud Gateway의 YAML 또는 Java DSL 경로 규칙과 `RewritePath`·`StripPrefix`, 인증 필터의 토큰 허용 목록(앞부분 일치 비교) 또는 Spring Security의 `permitAll` 규칙 |
| HTTP 호출 | OpenFeign, RestTemplate, RestClient, WebClient, HTTP 인터페이스(`@HttpExchange`) |
| 비동기 | Spring Kafka, Spring AMQP(RabbitMQ), Spring Cloud Stream. 토픽은 코드·설정 파일에 적힌 이름, 또는 설정 키 → 설정 파일의 `${환경변수}` → compose `environment`의 `- 이름=${이름}`·`이름: ${이름}`. compose는 루트의 표준 이름 파일, 루트에 없으면 저장소에 하나뿐인 파일 |
| DTO | Lombok·getter·public 필드, Java record, `@JsonProperty` |
| 오류 응답 | 오류 코드 enum, `@RestControllerAdvice`의 `@ExceptionHandler`, 게이트웨이 인증 필터의 응답 종료 |
| 배포·모니터링 | 배포 파이프라인 정의, 컨테이너 실행·빌드 정의, 메트릭 수집·로그 전송·모니터링 도구의 설정 파일 |

가정과 다른 방식(예: gRPC·GraphQL·WebSocket, 회로 차단기 fallback)은 항목으로 만들지 않고 그 서비스 파일의 `unsupported`에 남긴다. 결과를 보는 사람이 그 서비스의 흐름이 완전한지 알 수 있어야 하기 때문이다.

## 추출 순서

1. 서비스: 매번 저장소 전체에서 서비스를 찾고 `spring.application.name`을 확인한다. 디렉터리 이름으로 서비스 이름을 추정하지 않는다. `spring.application.name`이 없는 공통 모듈은 서비스로 만들지 않고, 그 안의 DTO·상수·예외 처리기는 그것을 쓰는 서비스에서 따라간다. 선택한 서비스마다 2~10단계를 한다.
2. API: 클래스·메서드 매핑을 합쳐 HTTP 메서드와 전체 경로를 구하고, 파라미터와 요청·응답 DTO를 따라가 형식대로 기록한다. 외부 타입의 구조나 프레임워크 기본값은 만들지 않는다.
3. HTTP 호출: 아래 종류별 읽는 법으로 `httpClients`·`httpCalls`를 쓴다. 대상 서비스 파일에 기대지 않고 대상 컨트롤러를 직접 읽어 서비스 이름·HTTP 메서드·전체 경로로 엔드포인트 ID를 정한다. 메서드 이름이 비슷하다고 연결하지 않는다.
4. 게이트웨이: YAML 또는 Java DSL 경로 규칙으로 외부 경로를 만들고, 토큰 허용 목록과 실제 비교 코드로 토큰 필요 여부를 정한다. 서비스 안의 권한 검사와 섞지 않는다.
5. 메시지 선언: 아래 종류별 읽는 법으로 `messageDeclarations`·`messagePublications`·`messageSubscriptions`를 쓴다. 목적지는 래퍼와 메서드 인자를 거슬러 호출 지점까지 따라간다.
6. 메시지 목적지: 코드나 설정 파일에 목적지 이름이 적혀 있으면 그 이름을, 아니면 코드의 설정 키 → 기본 설정 파일 → 그 서비스의 compose `environment`를 형식대로 잇는다. RabbitMQ는 교환기·라우팅 키·큐와 Binding, Stream은 바인딩의 destination을 확인한다.
7. 환경변수·외부 자원: 형식대로 기록한다. 환경변수 `location`은 설정 파일과 그것을 참조하는 `@Value`면 전체 설정 키, compose면 `environment`다. compose는 서비스의 `environment` 항목만 본다. 근거 없는 자원은 만들지 않는다.
8. 오류 응답: 오류 코드 enum 상수, 예외 처리기, 게이트웨이 필터의 거부 응답, `@EnableSpringDataWebSupport`의 페이지 직렬화 설정을 형식대로 기록한다. 처리기의 상태·오류 코드·메시지는 오류 응답 DTO 생성자까지 따라가 어디서 정해지는지 적고, 처리기 안의 조건에 따라 갈리면 `분기`로 적는다.
9. 다루지 않는 방식: 가정 표에 없는 호출·메시징 방식을 찾아 `unsupported`에 근거 줄(그 방식 이름이 있는 줄), 방식 이름, 한 줄 설명으로 남긴다.
10. 누락 확인: 매핑·HTTP 클라이언트·메시지 선언과 `@ExceptionHandler`·오류 코드 enum을 직접 검색해 추출 항목과 대조한다. 주석 속 선언은 항목으로 만들지 않는다. 검증 경고를 피하려고 사실이나 근거를 바꾸지 않는다.
11. 관계: 1단계에서 찾은 모든 서비스 파일이 있을 때 따로 실행하고, 서비스를 하나라도 다시 추출했으면 다시 한다. 서비스 파일의 같은 종류 메시지 발행·구독을 목적지와 연결 근거로 대조해 `messageLinks`를 쓰고, 엔드포인트·메시지 구독의 핸들러에서 서비스·포트·어댑터·래퍼를 따라가 같은 서비스의 HTTP 호출·직접 메시지 발행에 닿는 `calls`를 쓴다. 프레임워크가 실행하는 DLQ 복구기·함수형 발행은 `calls`의 도착점으로 만들지 않는다. 엔드포인트 핸들러에서 같은 방식으로 따라가 같은 서비스의 예외 생성 줄에 닿는 `throws`를 쓰고, 그 예외를 받는 처리기를 상속을 따라 정한다. 거치는 길의 `catch`가 잡는 예외는 쓰지 않고, `join()` 등으로 감싸여 나가는 예외는 감싼 예외의 처리기로 잇는다. 셋을 `relations.json` 하나로 낸다. HTTP 대상 ID가 대상 서비스 파일에 없으면 완료 보고에 적는다.
12. 저장소 수준: 서비스 단계와 따로 실행한다. 배포 파이프라인, 컨테이너 실행 정의와 빌드 정의, 메트릭 수집 대상, 로그 전송, 모니터링 도구 자체 설정, 저장소에 설정이나 언급이 있는 모니터링 도구와 그 실행 정의를 범주별로 형식대로 `repository.json` 하나에 쓴다. 항목마다 그 설정 파일의 도구 이름을 쓴다. 실행 정의를 저장소 전체에서 찾지 못한 도구는 빈 근거로 둔다. 파이프라인에 적힌 변수 값은 쓴 그대로 두고, 비밀값과 실행 환경에서 들어오는 변수 값은 쓰지 않고 그 값을 쓰는 모양만 적는다.

## 종류별 읽는 법

선언 줄 규칙은 검증기의 `CommunicationRules.RULES`에 같은 `kind`로 모여 있다. 첫 근거는 아래 표의 선언·호출 줄이다. 호출 경로·주소·목적지를 채우는 다른 줄도 각 사실의 근거에 넣는다. 선언 자체의 존재와 대상 연결의 확인은 별개다.

| kind | 선언 줄과 읽는 법 |
|---|---|
| `feign` | `@FeignClient`를 클라이언트로, 인터페이스의 매핑 어노테이션을 호출로 읽는다. `name`·`url`·`path`와 클래스·메서드 매핑을 합친다. |
| `restTemplate` | `RestTemplate` 필드·파라미터의 `getForObject`·`getForEntity`·`postForObject`·`postForEntity`·`put`·`patchForObject`·`delete`·`headForHeaders`·`optionsForAllow`·`exchange`·`execute` 호출 줄이다. 첫 URL 인자와 메서드 이름 또는 `HttpMethod` 인자를 읽는다. 클라이언트 선언 없이 `clientId`를 비운다. |
| `restClient` | `RestClient` 빈의 `baseUrl` 줄을 클라이언트로 읽는다. 타입을 선언한 필드·파라미터의 `get`·`post`·`put`·`patch`·`delete`·`head`·`options`·`method` 줄을 호출로, 이어지는 `uri`를 경로로 읽는다. 전체 URL이면 `clientId`를 비운다. |
| `webClient` | `WebClient` 빈의 `baseUrl` 줄과 타입을 선언한 필드·파라미터의 fluent 호출 시작 줄이다. 메서드와 `uri`, 기본 주소를 함께 읽는다. URI 빌더·람다의 값을 정할 수 없으면 `null`로 둔다. |
| `httpExchange` | 인터페이스의 `@HttpExchange`와 메서드의 `@HttpExchange`·`@GetExchange`·`@PostExchange`·`@PutExchange`·`@PatchExchange`·`@DeleteExchange`·`@HeadExchange`·`@OptionsExchange`를 읽는다. 프록시를 만드는 곳의 기본 주소와 인터페이스·메서드 경로도 근거에 넣는다. |
| `kafka` | `@KafkaListener`, 같은 파일에 선언한 `KafkaTemplate` 필드·파라미터의 `send`, `new DeadLetterPublishingRecoverer`다. 구독의 topics, send 첫 인자, 복구기의 TopicPartition 토픽을 읽는다. 문자열 상수·래퍼 인자·설정 키 사슬을 근거에 보존한다. |
| `rabbit` | `@RabbitListener`, `RabbitTemplate`·`AmqpTemplate` 필드·파라미터의 `send`·`convertAndSend`다. 발행의 교환기·라우팅 키는 `교환기/라우팅 키`, 구독은 큐 이름으로 쓴다. 기본 교환기면 라우팅 키와 큐를 대조한다. 명명된 교환기는 `new Binding(queue, QUEUE, exchange, key, ...)` 또는 `BindingBuilder.bind(queue).to(exchange).with(key)`와 큐·교환기 생성 줄을 관계 근거로 잇는다. 기본값을 바꾸는 템플릿 설정이나 바인딩을 정할 수 없으면 연결하지 않는다. |
| `stream` | `StreamBridge` 필드·파라미터의 `send`, `@Bean`의 `Supplier`·`Consumer`·`Function` 선언 줄이다. send 첫 인자는 바인딩 이름이다. 함수 이름의 `-in-0`·`-out-0` 바인딩과 `spring.cloud.stream.bindings.<이름>.destination` 설정을 대조한다. 별칭을 썼으면 그 설정도 따라가며, destination을 찾지 못하면 이름을 만들어 내지 않는다. |

주소·메서드·목적지를 코드에서 조립해 정할 수 없으면 `확인 안 됨`, 저장소 밖 설정값이면 `저장소에 근거 없음`이다. 외부 URL에는 `external: true`를 쓴다. 내부 대상은 이름·HTTP 메서드·경로까지 같아야 연결한다. RabbitMQ는 같은 문자열이 보인다는 이유만으로 교환기와 큐를 잇지 않는다. 선언이 여러 줄이면 선언 시작 줄을 첫 근거로 두고 값을 담은 줄을 추가한다. 줄 규칙으로 확인되지 않는 표현은 검증 경고를 그대로 남긴다.

Spring 밖 방식(예: gRPC)을 더할 때는 이 표에 읽는 법을 더하고 `CommunicationRules`에 그 종류의 선언·호출 근거 규칙을 더한다. 역할 배열·대상 대조·메시지 연결·서버 흐름은 같은 형식을 쓴다. 현재 지원하지 않는 방식은 `unsupported`다.

## 근거 선택

- 모든 사실에 대상 루트 기준 `파일:줄`을 붙인다. 관계에는 양쪽과 중간 전달 줄을 넣는다. 줄을 꾸미거나 다른 선언으로 바꾸지 않는다.
- 엔드포인트·HTTP 호출·메시지 항목·선언 목록의 첫 근거는 매핑 또는 위 종류별 선언·호출 줄이다. 래퍼 호출 줄로 대신하지 않는다.
- `path.evidence`에는 클래스(또는 클라이언트) 매핑 → 메서드 매핑, 직접 호출이면 호출 → URI 줄을 순서대로 넣는다.
- 필드·파라미터는 이름이 있는 선언 줄, 타입은 클래스 이름 또는 사용 타입이 있는 줄, 어노테이션은 그 어노테이션 줄이다.
- 서비스 이름은 설정 값 줄, 경로 규칙은 ID·URI·Path·재작성 값 줄, 허용 목록은 상수 이름·경로·메서드 줄, Security 규칙이면 `pathMatchers` 줄이다. 외부 경로에는 엔드포인트·경로 규칙의 첫 근거와 재작성 근거를, 토큰 판정에는 허용 목록과 비교 코드 줄, Security 규칙이면 허용 규칙 줄과 `anyExchange().authenticated()` 줄을 넣는다.
- Kafka `topicKey`에는 전체 키가 적힌 코드 줄을 넣는다.
- 저장소 수준 항목은 파이프라인 단계·명령·이름·변수, 설정 파일의 키, 빌드 정의의 지시문이 적힌 줄이다. 비밀값의 근거는 그 값을 쓰는 줄이다.
- `calls`에는 출발 항목의 첫 근거, 거치는 호출 줄, 도착 지점 줄을 순서대로 넣는다. `throws`의 도착 지점 줄은 예외 생성 줄이다.
- 오류 코드는 상수 선언 줄, 예외 처리기는 `@ExceptionHandler` 줄 다음에 상태·본문을 정하는 줄과 그 값을 채우는 DTO 생성자 줄, 게이트웨이 거부는 조건 줄 다음에 상태를 정하는 줄과 응답을 끝내는 줄, 페이지 직렬화는 어노테이션 줄이다.
- 빈 문자열 값에는 부재를 확인한 선언이나 경로 규칙 줄을 붙인다.

## 결과 작성과 점검

- 서비스 하나마다 결과 디렉터리의 `services/<spring.application.name>.json` 하나만 쓴다. 저장소 수준은 `repository.json` 하나만 쓴다. 절 목록을 받았으면 기존 파일에서 그 절(최상위 키, 관계는 `relations.json`, 저장소 수준은 `repository.json`의 키)만 바꾸고 나머지는 글자 하나 바꾸지 않는다.
- 어노테이션 문자열 인자는 Java 이스케이프를 푼 값으로 쓴다(`"\\d"` → `\d`).
- 저장 전 JSON 문법, 스키마 구조, ID 참조, 근거 줄, 상태 필드·환경변수 값·절대경로가 없는지 확인한다.
- 완료 후 결과 파일 위치, 정하지 못한 부분, 관계 단계 실행 여부와 HTTP 대상 ID 누락을 짧게 보고한다. 검증 통과를 직접 선언하지 않는다.
