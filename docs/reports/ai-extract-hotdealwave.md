# AI 추출 보고서: HotDealWave와 파서 기준선 대조

이슈 #10(3단계)의 결과다. AI 세션이 추출 Skill만 보고 분석 대상 `cc43016`을 읽어 `analysis/hotdealwave/extracted.json`을 썼다. 검증기를 통과한 결과는 파서 기준선과 경고 12개가 같고, 13개 종류의 항목 키와 DTO 필드 727개가 모두 일치한다. 한 번 추출하고 대조한 뒤 Skill을 세 문장 고쳐 다시 추출했다.

## 1. 흐름

| 단계 | 하는 것 | 나오는 것 |
|---|---|---|
| 1 | 새 AI 세션이 [추출 Skill](../../.claude/skills/extract-msa-java-spring/SKILL.md)과 [analysis.json 계약](../../extractor/README.md)만 보고 대상 코드를 직접 읽는다 | `analysis/hotdealwave/extracted.json` (상태·경고 없음) |
| 2 | `./gradlew :verifier:run --args="analysis/hotdealwave/extracted.json target/HotDealWave out/analysis.json"` | 상태·`checks`·`warnings`가 붙은 `out/analysis.json` |
| 3 | 파서 결과(`out/parser.json`)도 같은 검증기에 넣고 두 결과를 키별로 대조한다 | 일치율, 경고·상태 비교 |

- 추출 세션은 도구 저장소의 `extractor/`(계약 파일만 예외), `out/`, `docs/`, `eval/`을 읽지 않는다. 명령 기록에서 이 경로를 읽은 명령이 없음을 확인했다. `.env`와 `.local`은 검색 제외 조건으로만 나온다.
- 추출 세션은 스크립트를 만들지 않고 파일 목록·검색·줄 번호 읽기로 JSON을 직접 썼다.

## 2. Skill 구성

Skill은 대상 경로와 결과 파일 경로만 받는다. 다섯 절로 되어 있다.

| 절 | 내용 |
|---|---|
| 입력과 경계 | 읽는 것은 대상 코드, Skill, 계약뿐이다. 파서 코드·이전 결과·`.env`는 읽지 않고, 환경변수는 이름만 쓴다. 스크립트를 만들지 않는다. |
| 가정하는 구성 | Gradle 서비스와 `application*.yml`, Spring Cloud Gateway, OpenFeign, Spring Kafka, 루트 `docker-compose.yml`. 가정과 다른 부분은 만들어 내지 않고 보고한다. |
| 추출 순서 | 서비스 → 컨트롤러·DTO → Feign → 게이트웨이 → Kafka 선언 → 토픽 사슬 → 환경변수·외부 자원 → 선언 목록과 누락 대조, 8단계 |
| 근거 선택 | 항목마다 어느 줄을 근거로 붙이는지. 검증기가 그 줄에서 항목 이름을 찾기 때문이다. |
| 결과 작성과 점검 | 계약 형식으로 쓰고 상태·`checks`·`warnings`는 쓰지 않는다(검증기가 정한다). 정할 수 없는 값은 `null`. 저장 전 점검과 완료 보고. |

Skill은 대상 코드를 읽지 않은 세션이 계약과 검증기만 보고 썼다. 그래서 HotDealWave의 서비스·클래스·토픽·환경변수 이름은 들어 있지 않다.

"가정하는 구성" 절은 2차 추출 뒤에 더했다. 추출 순서에 이미 있던 가정을 모아 적은 것이고 HotDealWave는 모두 들어맞아, 다시 추출하지 않았다.

## 3. HotDealWave에 맞춘 부분

이름은 없지만 다음은 HotDealWave에서 왔다. 다른 저장소에서는 이 부분이 맞지 않을 수 있다.

| 부분 | 어디서 왔나 | 다른 저장소에서 |
|---|---|---|
| 가정하는 구성 | HotDealWave의 기술 구성 | Maven, `.properties`, Kotlin DSL, 다른 메시지 브로커는 다루지 않는다 |
| 게이트웨이 토큰 판정 | 인증 필터가 허용 목록과 경로·메서드를 비교하는 방식(`gateway/.../filter/AllowedPath.java`) | 허용 목록이 없는 인증 방식이면 토큰 필요 여부를 정하지 못한다 |
| Kafka 발행 추적: private 래퍼, 여러 메서드를 거친 토픽 인자, `send` 없는 DLQ | [따라가는 범위](../decisions.md#따라가는-범위)의 패턴 | Spring Kafka의 흔한 모양이라 그대로 쓴다 |
| 토픽 키 → YAML → compose 사슬, 환경변수 이름이 같을 때만 발행·구독 연결 | 토픽 이름이 저장소에 값으로 없는 구조([Kafka 연결 규칙](../decisions.md#kafka-연결-규칙)) | 토픽을 문자열로 쓰면 계약대로 이름이 `확인됨`이고 사슬은 없다 |
| 1차 대조로 고친 세 문장(5절) | HotDealWave 대조 | 계약의 표기 규칙이라 공용이다 |
| 계약(`extractor/README.md`) | 파서 출력 형식이라 예시가 HotDealWave다 | Skill이 예시를 추출 사실로 쓰지 않게 막는다 |
| 검증기가 읽는 근거 | `.java`, `.yml`, `build.gradle`, compose의 `- 이름=${이름}` 줄 | 다른 형식의 근거는 근거 줄 검사에서 `확인 안 됨`이 된다 |

## 4. 결과

항목을 맞추는 키는 종류별로 정했다(엔드포인트는 HTTP 메서드+전체 경로, Feign 호출은 서비스+메서드+경로, Kafka는 서비스+종류+첫 근거, 환경변수는 서비스+프로필+이름+위치). 서비스 이름 등은 정규화하지 않고 그대로 비교했다.

| 항목 | 개수 | 1차 키 일치 | 2차 키 일치 |
|---|---|---|---|
| 서비스 | 8 | 100% | 100% |
| 엔드포인트 | 72 | 100% | 100% |
| Feign 클라이언트 / 호출 | 9 / 17 | 0% / 0% | 100% / 100% |
| 게이트웨이 경로 / 허용 경로 | 6 / 7 | 100% / 100% | 100% / 100% |
| 매핑 선언 / Kafka 선언 | 102 / 19 | 100% / 100% | 100% / 100% |
| Kafka 발행 / 구독 / 연결 | 13 / 9 / 9 | 0% | 100% |
| 환경변수 | 281 | 0% | 100% |
| 외부 자원 | 15 | 0% | 100% |
| DTO 필드(이름·JSON 이름·검증 어노테이션) | 727 | 99.3% | 100% |
| 검증 경고 | | 158 | 12 |

2차 경고 12개는 파서와 대상·사유가 모두 같다(선언 목록 1, Kafka 발행↔구독 11). 이 중 5개는 AI가 토픽 필드·래퍼 인자 줄 같은 근거를 더 붙였고, 붙인 줄은 모두 고정 소스에서 확인했다.

## 5. 1차에서 고친 것

1차 경고 158개 중 146개는 `service`에 디렉터리 이름(`company`)을 쓴 탓이다. 파서와 게이트웨이 경로는 애플리케이션 이름(`company-service`)을 쓰므로 Feign·Kafka·게이트웨이 대조가 모두 어긋났다. 코드와 다르게 뽑은 항목은 없었다.

Skill에 고친 문장은 셋이다.

| 1차 차이 | Skill에 더한 문장 |
|---|---|
| `service`에 디렉터리 이름 (경고 146) | 모든 항목의 `service`에는 애플리케이션 이름(`spring.application.name`)을 쓰고, 디렉터리 이름은 `services[].directory`와 근거 경로에만 쓴다. |
| 환경변수 `location`이 `environment.<이름>`, `@Value(<키>)` 같은 제각각 표기. compose `image:`의 환경변수 16개도 수집 | `location`은 YAML과 그것을 참조하는 `@Value`면 전체 설정 키, compose면 `environment`다. compose는 서비스의 `environment` 항목만 본다. |
| 정규식 인자를 소스 표기 그대로 기록(`\\d`) (DTO 5개) | 어노테이션의 문자열 인자는 Java 이스케이프를 푼 값으로 기록한다. |

반영하지 않은 제안은 셋이다. compose 서비스 키와 애플리케이션 이름이 다를 때의 처리는 고정 커밋에 그런 경우가 없다. compose `image:`의 환경변수는 이미지 레지스트리, 곧 배포 쪽 정보라 배포 경로를 뺀 결정에 맞춰 뺐다. 타입 표기 규칙(기본형 `qualifiedName`은 `null` 등)은 1차의 경고·키 차이와 무관해 뺐는데, 2차 내용 비교에서 6절의 기본형 표기 차이로 남았다.

## 6. 남은 차이

항목 키는 모두 맞지만, 내용 속성까지 비교하면 엔드포인트 27개에서 JSON 노드 118개가 다르다. 코드와 다르게 뽑은 사례는 없다.

| 원인 | 노드 | 예 |
|---|---|---|
| 기본형 `qualifiedName` (AI는 `int`·`double`·`boolean`, 파서는 `null`) | 74 | `coupon/.../ResGetUserCouponsDto.java:18` |
| 외부 타입의 `qualifiedName` (AI는 import로 확인한 `Page`·`Pageable`·`MultipartFile` 이름, 파서는 `null`) | 30 | `company/.../CompanyController.java:18` |
| `java.sql.Timestamp` (파서는 scalar, AI는 unresolved) | 8 | `product/.../ResGetPromotionDto.java:18` |
| `List<MultipartFile>` (파서는 List까지 unresolved, AI는 container + 내부만 unresolved) | 4 | `product/.../ReqPostProductDto.java:42` |
| `@PageableDefault`가 붙은 `Pageable`의 binding | 2 | `order/.../BasketController.java:60` |

이 차이로 하위 사실의 `확인 안 됨`이 파서 34개, AI 38개다. 최상위 567개 항목은 양쪽 모두 `확인됨`이고, `저장소에 근거 없음` 320개(환경변수 값, 외부 자원 접속 값, 토픽 실제 이름)는 같다. 검증기는 해석하지 못한 타입을 경고 없이 `확인 안 됨`으로 두므로 경고 수에는 나타나지 않는다.

## 7. 한계

- 추출은 1차와 2차 한 번씩만 했다. 같은 Skill로 다시 돌렸을 때 결과가 얼마나 흔들리는지는 보지 않았다.
- Skill은 같은 대상을 파서와 대조해 고쳤다. HotDealWave에서 맞는다고 다른 저장소에서도 맞는다는 근거는 아니며, 9단계(두 번째 대상)에서 확인한다.
- 경고 12개의 판정(진짜 문제인지 추출 오류인지)은 아직 하지 않았다. 4단계에서 필요한 것만 판정한다.
