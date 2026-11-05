# 개발자 확인 목록

정확도: 맞음·부분·틀림. 확인 범위: 밝힘·일부·안 밝힘. 갈림은 두 채점자의 값이 다르다는 표시다.

| 묶음 | 질문 | 조건 | 정확도 Codex / Opus | 확인 범위 Codex / Opus |
|---|---|---|---|---|
| 1 | [Q04](#q04-ask) | 질의응답 | 맞음 / 부분 | 밝힘 / 밝힘 |
| 1 | [Q09](#q09-ask) | 질의응답 | 맞음 / 틀림 | 밝힘 / 일부 |
| 1 | [Q13](#q13-mcp) | MCP | 부분 / 틀림 | 밝힘 / 밝힘 |
| 1 | [Q17](#q17-mcp) | MCP | 맞음 / 부분 | 밝힘 / 밝힘 |
| 1 | [Q21](#q21-mcp) | MCP | 맞음 / 부분 | 밝힘 / 밝힘 |
| 2 | [Q05](#q05-mcp) | MCP | 부분 / 부분 | 밝힘 / 일부 |
| 2 | [Q05](#q05-ask) | 질의응답 | 부분 / 부분 | 밝힘 / 일부 |
| 2 | [Q06](#q06-ask) | 질의응답 | 맞음 / 맞음 | 밝힘 / 일부 |
| 2 | [Q07](#q07-ask) | 질의응답 | 부분 / 부분 | 밝힘 / 일부 |
| 2 | [Q12](#q12-ask) | 질의응답 | 부분 / 부분 | 밝힘 / 일부 |
| 2 | [Q16](#q16-ask) | 질의응답 | 맞음 / 맞음 | 밝힘 / 일부 |
| 2 | [Q17](#q17-ask) | 질의응답 | 부분 / 부분 | 밝힘 / 일부 |
| 2 | [Q18](#q18-mcp) | MCP | 부분 / 부분 | 밝힘 / 일부 |
| 2 | [Q19](#q19-mcp) | MCP | 부분 / 부분 | 밝힘 / 일부 |
| 2 | [Q19](#q19-ask) | 질의응답 | 틀림 / 틀림 | 밝힘 / 안 밝힘 |
| 2 | [Q20](#q20-mcp) | MCP | 부분 / 부분 | 밝힘 / 일부 |
| 2 | [Q20](#q20-ask) | 질의응답 | 부분 / 부분 | 밝힘 / 일부 |
| 3 | [Q08](#q08-mcp) | MCP | 맞음 / 맞음 | 밝힘 / 밝힘 |
| 3 | [Q11](#q11-ask) | 질의응답 | 틀림 / 틀림 | 안 밝힘 / 안 밝힘 |
| 3 | [Q14](#q14-base) | 기준선 | 맞음 / 맞음 | 밝힘 / 밝힘 |
| 3 | [Q08](#q08-base) | 기준선 | 맞음 / 맞음 | 밝힘 / 밝힘 |
| 3 | [Q02](#q02-ask) | 질의응답 | 부분 / 부분 | 밝힘 / 밝힘 |

## 1. 정확도가 갈린 답 (5)

<a id="q04-ask"></a>

### Q04 질의응답

> 운영 RDS와 Redis의 실제 접속 대상과 인증 정보는 어디서 확인하고, 배포된 서비스가 그 대상으로 접속하는지는 어떻게 확인하나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 맞음 | 부분 |
| 확인 범위 | 밝힘 | 밝힘 |
| 이유 | DB·Redis 변수명과 활성 프로파일 설명이 코드와 일치합니다. 실제 값은 배포 환경에서 확인하고, 현재 접속 여부는 런타임 설정·연결 상태와 대조해야 한다며 저장소의 확인 한계를 밝혔습니다. | coupon·order prod yml의 DB·Redis 환경변수 매핑과 order의 ACTIVE_PROFILE 인용은 코드와 맞고, 실제 값이 저장소 밖에 있다는 점도 밝혔다. 다만 docker-compose 환경변수 전달이나 EC2 배포 워크플로처럼 저장소에서 확인되는 주입 경로를 짚지 않았다. '배포 환경의 시크릿 설정', '런타임 연결 상태 확인'처럼 일반론에 그쳐 질문의 확인 방법 부분이 빈약하다. |
| 대조한 근거 | `coupon/src/main/resources/application-prod.yml:3-5`, `coupon/src/main/resources/application-prod.yml:16-19`, `order/src/main/resources/application-prod.yml:3-5`, `order/src/main/resources/application-prod.yml:22-25` 외 1개 | `coupon/src/main/resources/application-prod.yml:3-5`, `coupon/src/main/resources/application-prod.yml:16-19`, `order/src/main/resources/application-prod.yml:3-5`, `order/src/main/resources/application-prod.yml:22-25` 외 2개 |

<details><summary>답 원문</summary>

운영 RDS·Redis의 실제 접속 대상과 인증 정보는 저장소에 값이 없고, 저장소 밖의 환경변수에서 정해집니다. 따라서 배포 환경의 환경변수·시크릿 설정에서 확인해야 합니다. 예를 들어 coupon-service의 운영 DB는 `PROD_DB_ENDPOINT`, `PROD_DB_USERNAME`, `PROD_DB_PASSWORD`, Redis는 `REDIS_PROD_HOST`, `REDIS_PROD_PORT`, `REDIS_PROD_USERNAME`, `REDIS_PROD_PASSWORD`로 연결됩니다. order-service도 같은 운영 DB·Redis 변수와 함께 `ACTIVE_PROFILE`을 사용합니다. (C2: [coupon/src/main/resources/application-prod.yml:3], [coupon/src/main/resources/application-prod.yml:4], [coupon/src/main/resources/application-prod.yml:5], [coupon/src/main/resources/application-prod.yml:16], [coupon/src/main/resources/application-prod.yml:17], [coupon/src/main/resources/application-prod.yml:18], [coupon/src/main/resources/application-prod.yml:19]; C3: [order/src/main/resources/application-prod.yml:3], [order/src/main/resources/application-prod.yml:4], [order/src/main/resources/application-prod.yml:5], [order/src/main/resources/application-prod.yml:22], [order/src/main/resources/application-prod.yml:23], [order/src/main/resources/application-prod.yml:24], [order/src/main/resources/application-prod.yml:25], [order/src/main/resources/application.yml:5])

실제 배포된 서비스가 그 대상으로 접속 중인지는 이 조각만으로 확인할 수 없습니다. 배포 환경에서 활성 프로파일과 실제 주입된 변수·시크릿을 확인한 뒤, 서비스 런타임의 DB·Redis 연결 상태나 접속 로그를 대조해야 합니다. 제공된 조각은 환경변수 값이 저장소 밖에 있다고만 밝히며, 배포된 인스턴스의 현재 설정이나 연결 성공 여부는 제시하지 않습니다.

</details>

<a id="q09-ask"></a>

### Q09 질의응답

> 운영 결제에 사용하는 카카오페이 콘솔은 어디서 확인하고, 등록된 도메인과 복귀 관련 주소가 실제 운영 설정과 일치하나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 맞음 | 틀림 |
| 확인 범위 | 밝힘 | 일부 |
| 이유 | 콘솔 위치와 콘솔에 등록된 도메인·복귀 주소, 운영 설정과의 일치 여부를 확인할 근거가 없다는 핵심 답변은 맞다. 코드상 복귀 경로 설명은 생략했지만, 실제 등록값이나 운영 일치 여부를 확인했다고 주장하지 않았다. | B는 복귀 주소를 확인할 수 없다고 했지만, 저장소의 ReqPostKakaoPayReadyDto에 approval·cancel·fail 경로가 있고 취소·실패 경로가 뒤바뀐 사실도 확인됩니다. 그래서 질문의 핵심에 답하지 못했습니다. 운영값을 확인된 것처럼 말하지는 않았지만, 코드로 확인되는 부분과 확인되지 않는 부분을 구분하지 않았습니다. |
| 대조한 근거 | `README.md:125`, `docker-compose.yml:185-187`, `payment/src/main/resources/application.yml:26-29`, `payment/src/main/java/com/sparta/hotdeal/payment/infrastructure/dto/kakaopay/req/ReqPostKakaoPayReadyDto.java:41-43` | `payment/src/main/java/com/sparta/hotdeal/payment/infrastructure/dto/kakaopay/req/ReqPostKakaoPayReadyDto.java:41-43`, `payment/src/main/resources/application.yml:29`, `docker-compose.yml:187` |

<details><summary>답 원문</summary>

조각에는 카카오페이 콘솔의 확인 위치나 등록 도메인·복귀 주소, 실제 운영 설정과의 일치 여부가 없습니다. 결제 준비·승인·환불 API는 호출 범위 밖이라 응답도 확인되지 않았습니다.

</details>

<a id="q13-mcp"></a>

### Q13 MCP

> 실패한 ECR 업로드 단계는 어떤 방식으로 AWS 인증을 받고, GitHub에 등록된 인증 설정이 워크플로에서 참조하는 값과 맞나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 부분 | 틀림 |
| 확인 범위 | 밝힘 | 밝힘 |
| 이유 | S3 설정 설명과 GitHub 실제 등록값을 확인할 수 없다는 판단은 맞습니다. 다만 저장소에 ECR 인증 워크플로가 있어 확인 가능한 액세스 키 인증 방식과 Secret 이름을 놓쳤습니다. | A는 워크플로를 찾지 못했다며 ECR 인증 방식을 확인할 수 없다고 했습니다. 하지만 저장소의 .github/workflows/actions.yml에는 secrets 액세스 키를 쓰는 configure-aws-credentials 단계가 있어 핵심 질문에 답하지 못했습니다. GitHub 등록값이 저장소 밖에 있다는 구분은 했고, 확인되지 않은 것을 확인된 것처럼 말하지는 않았습니다. |
| 대조한 근거 | `docker-compose.yml:96-98`, `product/src/main/resources/application.yml:34-36`, `.github/workflows/actions.yml:19-28` | `.github/workflows/actions.yml:19`, `.github/workflows/actions.yml:22`, `.github/workflows/actions.yml:23`, `docker-compose.yml:97` 외 2개 |

<details><summary>답 원문</summary>

**현재 `hotdealwave` MCP가 제공한 근거만으로는 실패한 ECR 업로드의 AWS 인증 방식이나 GitHub 등록 설정과의 일치 여부를 확인할 수 없습니다.**

- **확인된 설정:** product 서비스는 `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`를 환경변수로 전달받으며, 애플리케이션에서는 이를 `s3.access-key`, `s3.secret-key`로 참조합니다. 이는 S3 설정의 근거입니다. (`docker-compose.yml:97`, `docker-compose.yml:98`, `product/src/main/resources/application.yml:35`, `product/src/main/resources/application.yml:36`)
- **ECR 인증 방식은 미확인:** MCP에서 `ECR`, `.github/workflows`, `GitHub Actions`를 검색했지만 관련 문서를 찾지 못했습니다. 따라서 실패한 단계가 액세스 키를 사용하는지, OIDC로 IAM 역할을 받는지, 어떤 Secret 이름을 참조하는지는 판정할 수 없습니다. 해당 워크플로의 `파일:줄` 근거도 확보되지 않았습니다.
- **GitHub 설정과의 일치 여부도 미확인:** 문서는 위 AWS 환경변수의 실제 값이 저장소 밖에서 정해진다고 명시합니다. GitHub에 등록된 Secrets·Variables·Environment 설정은 제시되지 않아 대조할 수 없습니다. (`docker-compose.yml:97`, `docker-compose.yml:98`)

따라서 **S3용 환경변수 이름만 보고 ECR 인증 설정이 맞다거나, 불일치가 실패 원인이라고 결론 내릴 수 없습니다.** 이를 판단하려면 실패한 실행의 워크플로와 로그, GitHub 인증 설정의 이름·적용 범위가 필요합니다.

</details>

<a id="q17-mcp"></a>

### Q17 MCP

> 외부 메일 발송 계정의 인증 상태와 발송 제한, 수신 거부나 발송 실패 기록은 어디서 확인할 수 있나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 맞음 | 부분 |
| 확인 범위 | 밝힘 | 밝힘 |
| 이유 | 환경변수 주입, 인증코드 유효기간, 가입자 이메일 인증 상태와 비동기 실패 처리를 정확하게 설명했습니다. 운영 로그·제공자 화면 확인은 권고로 제시하고 실제 상태와 기록 위치는 미확인이라고 구분했습니다. | 환경변수 주입, 1800000ms가 인증코드 TTL이라는 점, verified가 수신 동의 정보가 아니라는 점, @Async void에서 예외를 IllegalArgumentException으로 바꿔 던지는 처리, 컨트롤러가 성공 응답을 돌려준다는 점은 인용 줄과 모두 맞습니다. 다만 logback.xml에 Loki(app=user-service)로 로그를 보내는 설정이 있는데도 로그 보관 위치를 저장소로 확인할 수 없다고 해, 실패 기록 위치라는 핵심 하나가 코드와 어긋납니다. |
| 대조한 근거 | `docker-compose.yml:34`, `docker-compose.yml:35`, `user/src/main/resources/application.yml:22`, `user/src/main/java/com/sparta/hotdeal/user/application/service/AuthService.java:95` 외 4개 | `docker-compose.yml:34-35`, `user/src/main/java/com/sparta/hotdeal/user/infrastructure/config/MailConfig.java:19`, `user/src/main/java/com/sparta/hotdeal/user/infrastructure/config/MailConfig.java:22`, `user/src/main/resources/application.yml:22` 외 6개 |

<details><summary>답 원문</summary>

**외부 메일 계정의 실제 인증 상태·발송 한도·수신 거부 이력은 제공된 MCP 문서와 저장소 근거만으로 확인할 수 없습니다.** 확인 가능한 설정과 추가 확인 위치는 다음과 같습니다.

- **계정 인증 상태:** 저장소에서는 `EMAIL_SERVICE_USERNAME`, `EMAIL_SERVICE_PASSWORD`를 외부 환경변수로 주입하는 구조만 확인됩니다. 현재 자격 증명의 유효성이나 계정 차단 여부는 배포 환경과 메일 제공자의 계정·보안 화면에서 별도로 확인해야 합니다. 근거: `docker-compose.yml:34`, `docker-compose.yml:35`, `user/src/main/java/com/sparta/hotdeal/user/infrastructure/config/MailConfig.java:19`, `user/src/main/java/com/sparta/hotdeal/user/infrastructure/config/MailConfig.java:22`.

- **발송 제한:** 제공자 측 일일 한도·속도 제한·남은 할당량은 확인할 수 없습니다. 메일 제공자의 한도·사용량 화면에서 확인할 항목입니다. 코드에 있는 `1800000ms`는 **인증 코드 유효기간**이며 발송 한도가 아닙니다. 근거: `user/src/main/resources/application.yml:22`, `user/src/main/java/com/sparta/hotdeal/user/infrastructure/redis/RedisUtilImpl.java:17`.

- **수신 거부 기록:** 제공된 문서에서 수신 거부 기록의 저장·조회 위치는 확인되지 않았습니다. 확인된 이메일 상태는 가입용 인증 코드 일치 시 설정하는 `verified`입니다. 이를 수신 동의나 거부 기록으로 해석할 수 없습니다. 메일 제공자의 수신 거부·차단 목록 제공 여부를 별도로 확인해야 합니다. 근거: `user/src/main/java/com/sparta/hotdeal/user/application/service/AuthService.java:95`, `user/src/main/java/com/sparta/hotdeal/user/application/service/AuthService.java:99`, `user/src/main/java/com/sparta/hotdeal/user/application/service/AuthService.java:100`.

- **발송 실패 기록:** 확인된 구현은 `@Async void` 발송 중 발생한 예외를 `IllegalArgumentException`으로 바꾸는 처리입니다. **요청의 HTTP 200만으로 발송 성공을 판단할 수 없습니다.** 운영 `user-service`의 비동기 예외 로그와 메일 제공자의 전송·반송 이력을 확인할 것을 권합니다. 다만 실제 로그 보관 위치나 실패 이력 조회 화면은 저장소 근거로 확인할 수 없습니다. 근거: `user/src/main/java/com/sparta/hotdeal/user/infrastructure/service/MailServiceImpl.java:17`, `user/src/main/java/com/sparta/hotdeal/user/infrastructure/service/MailServiceImpl.java:18`, `user/src/main/java/com/sparta/hotdeal/user/infrastructure/service/MailServiceImpl.java:21`, `user/src/main/java/com/sparta/hotdeal/user/infrastructure/service/MailServiceImpl.java:25`, `user/src/main/java/com/sparta/hotdeal/user/presentation/controller/AuthController.java:53`.

</details>

<a id="q21-mcp"></a>

### Q21 MCP

> Grafana에 실제 연결된 데이터 소스와 대시보드 조회 조건은 어디서 관리하고, 누락된 서비스의 지표와 로그가 저장된 대상을 보고 있나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 맞음 | 부분 |
| 확인 범위 | 밝힘 | 밝힘 |
| 이유 | 실제 Grafana 관리 위치와 저장 대상까지의 연결을 판정할 근거가 없다는 핵심 결론은 맞으며, 인용한 DB·Redis 설정도 코드와 일치한다. 검색에서 반환되지 않았다는 사실을 저장소 내 설정 부재로 단정하지 않고 확인 범위를 명시했다. | Grafana 연결과 대시보드 조회 조건을 확인할 수 없다는 판정, application-prod.yml의 DB·Redis 설정을 Grafana 데이터 소스로 보면 안 된다는 설명(인용 줄 정확)은 맞습니다. 그러나 prometheus·loki·logback 관련 근거가 없다고 해서, 저장소에 있는 수집 대상과 Loki 전송 설정이 답에서 빠졌습니다. 검색 결과가 없다는 것이 부재의 증명이 아니라고 단서를 달았고, 운영 환경에서 확인할 것을 구분했습니다. |
| 대조한 근거 | `user/src/main/resources/application-prod.yml:3`, `user/src/main/resources/application-prod.yml:15`, `monitoring/prometheus/prometheus.yml:23`, `order/src/main/resources/logback.xml:4` 외 2개 | `user/src/main/resources/application-prod.yml:3`, `user/src/main/resources/application-prod.yml:15`, `monitoring/prometheus/prometheus.yml:5-38`, `order/src/main/resources/logback.xml:4` |

<details><summary>답 원문</summary>

**제공된 MCP 문서만으로는 Grafana의 실제 데이터 소스·대시보드 조회 조건의 관리 위치를 확인할 수 없으며, 누락된 서비스의 지표·로그 저장 대상을 조회하고 있는지도 판정할 수 없습니다.**

| 질문 | 확인 결과 |
|---|---|
| 실제 연결된 데이터 소스는 어디서 관리하나요? | Grafana 연결 URL·데이터 소스 UID·프로비저닝 설정의 근거가 반환되지 않아 **확인 불가**입니다. |
| 대시보드 조회 조건은 어디서 관리하나요? | 대시보드 정의·패널 쿼리·변수·라벨 필터의 근거가 반환되지 않아 **확인 불가**입니다. |
| 누락된 서비스의 지표·로그가 저장된 대상을 보고 있나요? | 서비스별 수집 대상 → 저장소 → Grafana 데이터 소스 → 패널 쿼리 연결을 입증하는 근거가 없어 **확인 불가**입니다. |

도구에서 확인되는 애플리케이션 DB·Redis 설정을 Grafana 데이터 소스로 해석해서는 안 됩니다. 예를 들어 `user-service` 문서는 `spring.datasource.url`과 `spring.data.redis.host`를 각각 외부 환경변수로 설정하며, 실제 값은 저장소에 근거가 없다고 명시합니다. 이 설정은 Grafana 연결을 입증하지 않습니다. (`user/src/main/resources/application-prod.yml:3`, `user/src/main/resources/application-prod.yml:15`)

`Grafana`, `prometheus`, `loki`, `dashboard`, `logback`, `actuator`, `지표`, `수집` 검색에서는 관련 문서가 반환되지 않았습니다. 이는 **설정이 저장소에 없다는 증명은 아닙니다**. 위 확인 불가 항목에는 도구가 제시한 `파일:줄` 근거가 없어 임의로 붙이지 않았습니다.

판정하려면 운영 Grafana의 데이터 소스 설정과 대시보드 JSON, 해당 서비스의 지표·로그 수집 및 저장 대상 설정을 대조해야 합니다.

</details>

## 2. 확인 범위만 갈린 답 (12)

<a id="q05-mcp"></a>

### Q05 MCP

> 상품 목록 응답에는 어떤 상품 정보와 페이지 정보가 들어가고, 검색 결과가 없을 때는 어떤 형식으로 내려오나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 부분 | 부분 |
| 확인 범위 | 밝힘 | 일부 |
| 이유 | 상품 필드·타입, 응답 래퍼와 전체 건수 계산 문제는 맞다. 다만 VIA_DTO 설정을 놓쳐 페이지 형식 설명을 빠뜨렸고, 빈 결과에도 적용되는 메시지와 정상 반환 흐름까지 확인 불가로 처리했다. | C가 설명한 ResponseDto 래핑, 상품 필드와 타입, total 인자에 searchResults.size()를 넘기는 문제는 코드와 맞습니다. 하지만 ProductApplication.java:13의 VIA_DTO 설정을 놓쳐 페이지 정보 구조를 확정할 수 없다고 했고, 빈 결과 응답도 추론으로만 남겨 핵심 두 가지가 미완입니다. 확인된 것과 추론을 구분하기는 했으나, 저장소로 정해지는 직렬화 형식을 외부 미확인 영역으로 잘못 분류했습니다. |
| 대조한 근거 | `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/ResponseDto.java:9`, `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/product/ResGetProductDto.java:17`, `product/src/main/java/com/sparta/hotdeal/product/ProductApplication.java:13`, `product/src/main/java/com/sparta/hotdeal/product/presentation/controller/ProductController.java:151` 외 4개 | `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/ResponseDto.java:10`, `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/product/ResGetProductDto.java:29`, `product/src/main/java/com/sparta/hotdeal/product/ProductApplication.java:13`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/repository/product/ProductSearchRepositoryCustomImpl.java:71` 외 1개 |

<details><summary>답 원문</summary>

상품 목록 응답은 **`message`와 `data`로 감싼 구조**이며, `data`의 타입은 `Page<ResGetProductDto>`입니다.  
근거: `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/ResponseDto.java:9`, `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/ResponseDto.java:10`

각 상품에는 다음 정보가 들어갑니다.

| 필드 | 정보·타입 | 근거 `파일:줄` |
|---|---|---|
| `productId` | 상품 ID · UUID | `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/product/ResGetProductDto.java:17` |
| `name` | 상품명 · String | `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/product/ResGetProductDto.java:18` |
| `price` | 가격 · Integer | `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/product/ResGetProductDto.java:19` |
| `quantity` | 수량 · Integer | `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/product/ResGetProductDto.java:20` |
| `category` | 카테고리 · ProductCategoryEnum | `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/product/ResGetProductDto.java:21` |
| `companyId` | 업체 ID · UUID | `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/product/ResGetProductDto.java:22` |
| `description` | 상품 설명 · String | `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/product/ResGetProductDto.java:23` |
| `detailImgs` | 상세 이미지 목록 · List&lt;String&gt; | `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/product/ResGetProductDto.java:24` |
| `thumbImg` | 썸네일 이미지 · String | `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/product/ResGetProductDto.java:25` |
| `status` | 상품 상태 · ProductStatusEnum | `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/product/ResGetProductDto.java:26` |
| `rating` | 평점 · double | `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/product/ResGetProductDto.java:27` |
| `reviewCnt` | 리뷰 수 · Integer | `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/product/ResGetProductDto.java:28` |
| `discountPrice` | 할인 가격 · Integer | `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/product/ResGetProductDto.java:29` |

**페이지 정보는 확인 범위에 제한이 있습니다.** `Page`를 반환하는 것은 확인되지만, MCP 문서는 외부 타입인 `Page`의 내부 구조를 “확인 안 됨”으로 표시합니다. 따라서 `content`, `number`, `size`, `first`, `last` 등의 정확한 JSON 필드 구성을 저장소 근거만으로 확정할 수 없습니다.  
근거: `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/ResponseDto.java:10`

또한 페이지 생성 시 전체 검색 건수가 아닌 **현재 검색 결과의 `searchResults.size()`를 `PageImpl`의 total 인자로 전달**합니다. 문서는 이 때문에 `totalElements`·`totalPages`가 전체 일치 건수를 제대로 나타내지 못할 수 있다고 지적합니다.  
근거: `product/src/main/java/com/sparta/hotdeal/product/infrastructure/repository/product/ProductSearchRepositoryCustomImpl.java:53`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/repository/product/ProductSearchRepositoryCustomImpl.java:54`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/repository/product/ProductSearchRepositoryCustomImpl.java:71`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:188`

**검색 결과가 없을 때의 정확한 응답 JSON은 확인할 수 없습니다.** 위 페이지 생성 로직으로는 빈 결과 목록과 total 인자 `0`으로 페이지를 만드는 것으로 해석할 수 있지만, 이는 코드 설명에 따른 추론입니다. 도구가 제공한 문서에는 무검색 결과 응답 본문이 없으므로, `content: []`, `empty: true`, 메시지 문구 및 HTTP 상태를 해당 경우의 확정된 응답으로 제시할 수 없습니다.  
추론 근거: `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:188`

</details>

<a id="q05-ask"></a>

### Q05 질의응답

> 상품 목록 응답에는 어떤 상품 정보와 페이지 정보가 들어가고, 검색 결과가 없을 때는 어떤 형식으로 내려오나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 부분 | 부분 |
| 확인 범위 | 밝힘 | 일부 |
| 이유 | 반환 타입과 상품 필드는 맞지만, 저장소에서 확인되는 페이지 직렬화 설정과 빈 목록을 같은 성공 응답으로 반환하는 흐름을 누락했다. 확인하지 못한 부분은 명시했으나 확인 불가 범위를 지나치게 넓혔다. | B가 나열한 상품 필드는 코드와 맞습니다. 그러나 페이지 정보 구조와 빈 결과 형식은 저장소의 VIA_DTO 설정과 PageImpl 생성 코드로 확인할 수 있는데도 답할 수 없다고 해 질문의 두 핵심을 놓쳤습니다. 확인되지 않은 것을 확인된 것처럼 말하지는 않았지만, 저장소로 확인되는 것까지 확인 불가로 잘못 분류했고 파일:줄 근거도 제시하지 않았습니다. |
| 대조한 근거 | `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/product/ResGetProductDto.java:17`, `product/src/main/java/com/sparta/hotdeal/product/presentation/controller/ProductController.java:138`, `product/src/main/java/com/sparta/hotdeal/product/presentation/controller/ProductController.java:151`, `product/src/main/java/com/sparta/hotdeal/product/ProductApplication.java:13` 외 2개 | `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/product/ResGetProductDto.java:17`, `product/src/main/java/com/sparta/hotdeal/product/ProductApplication.java:13`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:188` |

<details><summary>답 원문</summary>

상품 목록은 `ResponseDto<Page<ResGetProductDto>>` 형식입니다. 각 상품 항목에는 `productId`, `name`, `price`, `quantity`, `category`, `companyId`, `description`, `detailImgs`, `thumbImg`, `status`, `rating`, `reviewCnt`, `discountPrice`가 들어갑니다. 다만 `Page`의 구체적인 페이지 메타데이터 구조는 확인 안 됨입니다(외부 타입 구조를 근거 줄만으로 확인할 수 없음). 검색 결과가 없을 때의 응답 형식이나 실제 빈 결과 응답은 조각에 확인된 내용이 없어 답할 수 없습니다.

</details>

<a id="q06-ask"></a>

### Q06 질의응답

> 쿠폰 발급 요청에 성공을 응답하는 시점은 언제고, 그 뒤 사용자에게 발급된 쿠폰이 저장되기까지 어떤 처리를 거치나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 맞음 | 맞음 |
| 확인 범위 | 밝힘 | 일부 |
| 이유 | 성공 응답 전 트랜잭션에서 검증·수량 갱신·저장이 이루어진다는 핵심과 408/CO020, 응답 data=null 설명이 모두 코드와 일치합니다. 설명을 코드에서 확인되는 흐름에 한정했으며, 실제 실행이나 저장소 밖의 연결을 단정하지 않았습니다. | 10초 락 대기, 실패 시 408/CO020, 검증 항목, OUT_OF_STOCK 전환, 같은 트랜잭션에서 저장, 락 해제 후 201·data null 응답이라는 주장이 모두 코드와 맞습니다. 다만 근거 파일:줄을 하나도 제시하지 않았습니다. 또 코드로 확인되는 처리 순서와 트랜잭션 설명까지 통째로 'AI 해석'이라고 표시해, 확인된 것과 확인되지 않는 것을 제대로 가르지 못했습니다. |
| 대조한 근거 | `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponService.java:36`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponService.java:39`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/exception/ErrorCode.java:28`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/exception/GlobalExceptionHandler.java:23` 외 5개 | `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponService.java:39-44`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/exception/ErrorCode.java:28`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:26-53`, `coupon/src/main/java/com/sparta/hotdeal/coupon/domain/entity/CouponInfo.java:75-76` 외 1개 |

<details><summary>답 원문</summary>

성공 응답은 발급 처리가 끝난 뒤 반환됩니다. 발급 경로는 쿠폰 정보별 Redis 락을 최대 10초 기다려 획득하고, 락을 얻지 못하면 408/CO020으로 실패합니다. 락 획득 후 별도 `CouponIssueHelper`의 트랜잭션 메서드에서 쿠폰 정보 존재 여부, ISSUED 상태, 잔여 수량, 동일 사용자의 중복 발급 여부를 검사합니다. 통과하면 발급 수를 증가시키고 수량에 도달하면 상태를 OUT_OF_STOCK으로 바꾸며, 같은 트랜잭션에서 미사용 사용자 쿠폰을 저장합니다. 이후 락을 소유한 경우 해제하고, 컨트롤러는 201 응답을 반환합니다. 성공 응답의 `data`는 `null`이므로 발급된 사용자 쿠폰 ID를 응답에서 직접 얻을 수는 없습니다. (처리 순서·트랜잭션 설명은 AI 해석입니다.)

</details>

<a id="q07-ask"></a>

### Q07 질의응답

> 보유 쿠폰 목록은 어떤 데이터를 기준으로 조회하고, 발급 성공 후 저장이나 조회 반영에 실패하면 어떻게 처리하나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 부분 | 부분 |
| 확인 범위 | 밝힘 | 일부 |
| 이유 | 조회 기준과 동일 트랜잭션에서의 발급 처리는 맞지만, 저장 실패 시 ‘함께 처리’된다는 설명만으로는 롤백과 오류 응답을 설명하지 못하며 저장 완료가 성공 응답보다 앞선다는 핵심도 빠졌습니다. 후속 복구 처리의 확인 한계는 밝혔습니다. | B의 조회 조건과 표시 데이터 설명은 맞습니다. 그러나 저장 실패 시의 처리를 '함께 처리되는 구조'라고만 해 롤백이나 500 응답을 밝히지 못했습니다. Redis 락의 근거로 든 CouponIssueHelper.java:19는 @Service 줄이고, 실제 락은 CouponService.java:36-39에 있습니다. 또 저장소에 재시도·보상 처리가 없다는 것은 코드로 확인되는데도 '조각에서 확인할 수 없다'고 미뤘습니다. |
| 대조한 근거 | `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponService.java:124`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/mapper/CouponMapper.java:16`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:26`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:44` 외 3개 | `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponService.java:36-44`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponService.java:123-127`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:19-27`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:44-53` 외 2개 |

<details><summary>답 원문</summary>

보유 쿠폰 목록은 요청한 `userId`, 삭제되지 않은 쿠폰(`deletedAt`이 null), `isUsed` 값으로 조회합니다. `isUsed`를 생략하면 미사용 쿠폰만 조회하며, 만료일이나 쿠폰 정보 상태는 목록 제외 조건이 아닙니다. 이름·할인액·최소 주문액·만료일은 연결된 현재 쿠폰 정보에서 가져옵니다. 이 조회 기준 설명은 AI 해석입니다. (coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponService.java:123, coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponService.java:124, coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponService.java:126)

발급은 쿠폰 정보별 Redis 락을 얻은 뒤 트랜잭션 메서드에서 발급 수 증가와 사용자 쿠폰 저장을 함께 처리합니다. 따라서 해당 트랜잭션 안에서 저장이 실패하면 발급 수 변경과 사용자 쿠폰 저장도 함께 처리되는 구조입니다(AI 해석). 다만 발급 성공 응답은 `data=null`이므로 응답만으로 사용자 쿠폰 ID를 얻을 수 없습니다. 이후 목록 조회에 실패했을 때 발급을 되돌리거나 재시도하는 별도 처리 여부는 조각에서 확인할 수 없습니다. (coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:19, coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:44, coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:46, coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:50, coupon/src/main/java/com/sparta/hotdeal/coupon/presentation/controller/CouponController.java:30, coupon/src/main/java/com/sparta/hotdeal/coupon/presentation/controller/CouponController.java:34, coupon/src/main/java/com/sparta/hotdeal/coupon/application/dto/ResponseDto.java:10)

</details>

<a id="q12-ask"></a>

### Q12 질의응답

> 결제 결과 전달이나 Order의 상태 변경에 실패하면 어디에서 재시도하고, 같은 결과가 여러 번 전달되면 어떻게 처리하나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 부분 | 부분 |
| 확인 범위 | 밝힘 | 일부 |
| 이유 | 컨테이너 재시도 0회, 리스너 내부 예외 처리, 비동기 발행 실패 처리의 부재는 코드와 맞습니다. 다만 중복 메시지도 상태를 다시 대입한다는 동작은 확인되므로, 중복 시 구체적 처리를 확인할 수 없다는 결론은 질문의 핵심을 충분히 설명하지 못합니다. | FixedBackOff(0L,0)로 재시도 없이 DLQ로 보낸다는 점, send() 결과를 확인하지 않는다는 점, 이전 상태를 검사하지 않는다는 점은 코드와 맞습니다. 그러나 코드상 updateStatus가 상태를 그대로 덮어쓰므로 중복 처리 결과는 판단할 수 있는데도 '확인할 수 없다'고 해 질문의 핵심 하나를 비워 두었습니다. DLQ를 다시 처리하는 경로가 없다는 점(OrderDlqConsumer가 빈으로 등록되지 않음), MANUAL ACK인데 acknowledge 호출이 없다는 점, Payment 쪽 동기 예외 처리도 빠졌습니다. 또한 '내부에서 잡은 오류는 컨테이너 오류 처리기로 가지 않는다'는 단정은 의심스럽습니다. 리스너와 서비스가 둘 다 @Transactional이라, 상태 변경 예외가 나면 롤백 전용으로 표시된 트랜잭션이 커밋 시점에 예외를 낼 수 있기 때문입니다. 근거도 파일:줄 없이 조각 번호만 달았습니다. |
| 대조한 근거 | `order/src/main/java/com/sparta/hotdeal/order/infrastructure/config/ConsumerApplicationKafkaConfig.java:67`, `order/src/main/java/com/sparta/hotdeal/order/event/consumer/OrderStatusUpdateEventConsumer.java:30`, `payment/src/main/java/com/sparta/hotdeal/payment/event/producer/PaymentEventProducer.java:52`, `order/src/main/java/com/sparta/hotdeal/order/event/service/OrderConsumerService.java:26` 외 1개 | `order/src/main/java/com/sparta/hotdeal/order/infrastructure/config/ConsumerApplicationKafkaConfig.java:67`, `order/src/main/java/com/sparta/hotdeal/order/event/consumer/OrderStatusUpdateEventConsumer.java:24`, `order/src/main/java/com/sparta/hotdeal/order/event/consumer/OrderStatusUpdateEventConsumer.java:32`, `order/src/main/java/com/sparta/hotdeal/order/event/service/OrderConsumerService.java:14` 외 5개 |

<details><summary>답 원문</summary>

조각에 따르면 결제 결과 상태 갱신은 payment-service가 `KAFKA_UPDATE_ORDER_STATUS_TOPIC`으로 발행하고 order-service가 구독합니다. Order 소비자는 메시지 상태를 `OrderStatus.valueOf(status)`로 적용하며 이전 상태를 검사하지 않습니다. 이 흐름의 상태 갱신에는 멱등성 검사나 중복 메시지 처리 규칙이 확인되지 않아, 같은 결과가 여러 번 전달될 때의 구체적 처리는 확인할 수 없습니다. [C3]

재시도는 두 경로를 구분해야 합니다. Order 소비자 밖으로 전파된 오류는 `FixedBackOff(0L, 0)` 설정이므로 재시도 없이 DLQ 복구기로 넘깁니다. 다만 리스너가 파싱·처리 오류를 내부에서 잡아 DLQ 발행하고 반환하는 경로는 이 컨테이너 오류 처리기로 자동 전달되지 않습니다. 또 상태 갱신 Kafka 발행은 비동기 전송 결과를 확인하지 않으므로, 나중의 전송 실패가 호출자에게 전달되어 재시도되는 경로도 확인되지 않습니다. [C3]

주문 취소·환불 리스너 오류도 원문과 오류 설명을 DLQ로 보내며, DLQ 발행 호출이 반환하면 원래 예외를 다시 던지지 않습니다. 따라서 이 경로의 자동 재시도는 확인되지 않습니다. [C1, C6, C8]

</details>

<a id="q16-ask"></a>

### Q16 질의응답

> Elasticsearch 반영에 실패하면 어떻게 재시도하고, 누락되거나 순서가 뒤바뀐 변경 사항은 어떻게 맞추나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 맞음 | 맞음 |
| 확인 범위 | 밝힘 | 일부 |
| 이유 | 생성·수정·상태 변경에서 색인 예외를 삼켜 성공 응답이 가능하고, 재고 변경은 색인을 갱신하지 않는다는 설명이 코드와 일치합니다. 재시도·누락·순서 보정 절차를 확인할 수 없다고 답해 핵심을 충족하며, 결론을 제공된 자료의 범위로 한정했습니다. | 재시도·정합성 보정 절차가 없다는 점, 색인 예외를 삼켜 성공 응답이 갈 수 있다는 점, 재고 증감이 색인을 갱신하지 않는다는 점은 코드와 맞습니다. 다만 근거 파일:줄을 하나도 대지 않았습니다. 또 '부재'를 저장소 전체에서 확인된 사실이 아니라 '조각' 자료의 한계로만 표현해서, 코드로 확인된 부재와 저장소 밖 운영 요소를 따로 구분하지 않았습니다. |
| 대조한 근거 | `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductIndexService.java:24`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:83`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:122`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:136` 외 2개 | `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductIndexService.java:24-26`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:83`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductInventoryService.java:99-107` |

<details><summary>답 원문</summary>

조각에서 Elasticsearch 반영 실패 시 재시도하거나 누락·순서 역전 변경을 조정하는 절차는 확인되지 않습니다. 확인된 내용은 상품 생성·수정·상태 변경의 색인 호출에서 예외를 로그만 남기고 삼켜 호출자에게 성공 응답이 갈 수 있다는 점입니다. 재고 증감도 색인을 갱신하지 않아 목록의 수량이 이전 값으로 남을 수 있습니다. 따라서 이 자료만으로 재시도·정합성 보정 방법이 있다고 답할 수 없습니다. (두 동작 설명은 AI 해석입니다.)

</details>

<a id="q17-ask"></a>

### Q17 질의응답

> 외부 메일 발송 계정의 인증 상태와 발송 제한, 수신 거부나 발송 실패 기록은 어디서 확인할 수 있나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 부분 | 부분 |
| 확인 범위 | 밝힘 | 일부 |
| 이유 | 실제 계정 상태·발송 제한·수신 거부 이력을 확인할 수 없다는 판단과 비동기 발송 설명은 맞습니다. 다만 저장소에 있는 Loki 로그 전송 설정과 실패 로그 처리라는 확인 단서를 빠뜨렸습니다. | 제공자 측 인증 상태·발송 한도·수신 거부 기록을 확인할 수 없다는 핵심, 그리고 비동기 발송이라 SMTP 실패가 응답에 반영되지 않는다는 지적은 맞습니다. 그러나 저장소로 확인되는 Gmail SMTP 설정, 환경변수 주입, Loki 로그 전송을 전혀 짚지 않았고 근거 파일:줄도 없습니다. 확인되는 것과 확인되지 않는 것을 나누지 않고 전부 확인 불가로 처리했습니다. |
| 대조한 근거 | `user/src/main/resources/logback.xml:4`, `user/src/main/resources/logback.xml:8`, `user/src/main/java/com/sparta/hotdeal/user/infrastructure/service/MailServiceImpl.java:17`, `user/src/main/java/com/sparta/hotdeal/user/infrastructure/service/MailServiceImpl.java:23` | `user/src/main/resources/application.yml:7-15`, `user/src/main/java/com/sparta/hotdeal/user/infrastructure/service/MailServiceImpl.java:17-26`, `user/src/main/resources/logback.xml:4`, `user/src/main/resources/logback.xml:8` |

<details><summary>답 원문</summary>

조각에는 외부 메일 발송 계정의 인증 상태, 발송 제한, 수신 거부 또는 발송 실패 기록을 확인할 위치가 나와 있지 않아 확인할 수 없습니다. C1은 메일 발송이 비동기라고 설명하지만, SMTP 전송 실패가 요청 성공 응답에 반영되지 않을 수 있다고만 적고 발송 기록이나 계정 상태 확인 경로는 제시하지 않습니다.

</details>

<a id="q18-mcp"></a>

### Q18 MCP

> 쿠폰 소진이나 중복 발급, 발급 기간 종료로 발급이 실패하면 각각 어떤 상태 코드와 오류 본문이 내려오나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 부분 | 부분 |
| 확인 범위 | 밝힘 | 일부 |
| 이유 | 소진 시 검사 순서와 중복 발급, 발급 경로의 만료일 검사 부재는 정확하며 확인 범위도 명시했습니다. 다만 오류 본문의 필드와 메시지는 DTO·예외 처리기·ErrorCode로 확인되므로 확인 불가라는 주장은 잘못됐습니다. | 상태 코드 매핑(CO004/CO005/CO006, 400)과 소진 뒤 OUT_OF_STOCK이 되어 CO004가 나는 흐름, 발급 경로에 만료일 검사가 없다는 점은 코드와 맞습니다. 그러나 오류 본문의 필드와 메시지가 확인되지 않는다고 했는데, ErrorResponseDto와 ErrorCode를 보면 timestamp/code/message와 각 메시지가 정해져 있습니다. 코드로 확인되는 것을 확인할 수 없다고 잘못 나눴고, 스케줄러가 쿠폰 정보를 삭제한 뒤 404/CO001이 나는 경우도 빠졌습니다. |
| 대조한 근거 | `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:29`, `coupon/src/main/java/com/sparta/hotdeal/coupon/domain/entity/CouponInfo.java:75`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/dto/ErrorResponseDto.java:14`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/dto/ErrorResponseDto.java:19` 외 2개 | `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:29-41`, `coupon/src/main/java/com/sparta/hotdeal/coupon/domain/entity/CouponInfo.java:75-76`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/exception/ErrorCode.java:9-16`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/exception/GlobalExceptionHandler.java:19-24` 외 2개 |

<details><summary>답 원문</summary>

**코드상 소진·중복·발급 불가 상태는 HTTP 400이지만, “기간 종료”를 날짜로 직접 검사하는 발급 로직은 없습니다. 정확한 오류 JSON 본문은 MCP 문서에서 확인되지 않아 단정할 수 없습니다.**

| 실패 조건 | HTTP 상태와 오류 코드 | 근거 |
|---|---|---|
| 수량 검사에서 `issuedCount >= quantity` | **400 / CO005** | `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:35`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/exception/ErrorCode.java:13` |
| 같은 사용자가 같은 쿠폰 정보를 이미 발급받음 | **400 / CO006**. 앞선 상태·수량 검사를 통과해야 이 분기에 도달합니다. | `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:31`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:35`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:39`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/exception/ErrorCode.java:14` |
| 발급 상태가 `ISSUED`가 아님 | **400 / CO004** | `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:31`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/exception/ErrorCode.java:12` |

위 HTTP 상태 매핑의 공통 근거는 `coupon/src/main/java/com/sparta/hotdeal/coupon/application/exception/GlobalExceptionHandler.java:19`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/exception/GlobalExceptionHandler.java:23`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/exception/GlobalExceptionHandler.java:24`입니다.

**소진 후 요청이 항상 CO005인 것은 아닙니다.** 마지막 발급으로 수량에 도달하면 상태가 `OUT_OF_STOCK`으로 바뀌므로, 이후 요청은 수량 검사보다 앞선 상태 검사에서 **400 / CO004**로 거절됩니다. 근거: `coupon/src/main/java/com/sparta/hotdeal/coupon/domain/entity/CouponInfo.java:75`, `coupon/src/main/java/com/sparta/hotdeal/coupon/domain/entity/CouponInfo.java:76`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:31`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:35`.

**기간 종료는 별도 구분이 필요합니다.** 발급 경로는 `expirationDate`를 검사하지 않아, 만료됐어도 삭제 전이고 상태·수량·중복 조건을 통과하면 발급될 수 있습니다. **400 / CO008은 발급 API가 아니라 쿠폰 검증 로직의 만료 오류**입니다. 근거: `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:29`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:31`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:35`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:39`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponService.java:63`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponService.java:64`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/exception/ErrorCode.java:16`.

오류 본문의 **필드명·정확한 메시지·전체 JSON은 확인 불가**입니다. 제공된 발급 응답 기록은 성공 **201**이며, 위 실패들의 실제 응답 본문을 입증하지 않습니다. 따라서 CO004·CO005·CO006을 임의로 `{"code": "...", "message": "..."}` 형태로 제시할 수는 없습니다. 근거: `responses/coupon-service.CouponController.issueFirstComeFirstServeCoupon.json:6`, `responses/coupon-service.CouponController.issueFirstComeFirstServeCoupon.json:9`, `responses/coupon-service.CouponController.issueFirstComeFirstServeCoupon.json:12`.

</details>

<a id="q19-mcp"></a>

### Q19 MCP

> Gateway로 들어온 요청은 어떤 라우팅·인증 처리를 거쳐 대상 서비스로 전달되고, 그 과정에서 경로나 헤더가 어떻게 바뀌나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 부분 | 부분 |
| 확인 범위 | 밝힘 | 일부 |
| 이유 | 라우팅 사례와 인증 생략·헤더 덮어쓰기, 대상 서비스의 권한 처리는 맞습니다. 확인 한계와 환경 의존 값은 밝혔지만, 저장소에서 확인되는 RewritePath 규칙과 인증 실패 시 401, 전체 인증 예외 목록을 확인 불가로 남겨 핵심 설명이 빠졌습니다. | 라우팅 대상, 인증 예외 분기, X-User-* 헤더 덮어쓰기, reviews GET 불일치, company의 @Secured 검사는 코드와 맞다. 하지만 RewritePath 식(yml:47에 있음), 401 실패 처리(AuthenticationFilter.java:30-32에 있음), 전체 인증 예외 목록(AllowedPath에 있음)을 확인할 수 없다고 했다. 그래서 질문의 핵심인 경로 변경을 설명하지 못했고, 코드로 확인되는 내용을 확인되지 않는 것으로 잘못 나눴다. 환경변수로 정해지는 값은 저장소 밖 값으로 맞게 구분했다. |
| 대조한 근거 | `gateway/src/main/resources/application.yml:19`, `gateway/src/main/resources/application.yml:47`, `gateway/src/main/resources/application.yml:54`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AllowedPath.java:6` 외 8개 | `gateway/src/main/resources/application.yml:47`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:24-42`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AllowedPath.java:6-12`, `gateway/src/main/java/com/sparta/hotdeal/gateway/security/JwtUtil.java:31-34` 외 6개 |

<details><summary>답 원문</summary>

문서에서 확인되는 흐름은 **경로 규칙으로 대상 서비스를 정하고, 인증 제외 여부에 따라 JWT 검증과 사용자 헤더 설정을 거쳐 전달하는 구조**입니다. 다만 MCP가 보여 준 본문에는 라우팅 설정 원문과 JWT 실패 처리 전체가 없어, 경로 재작성식이나 실패 응답까지 모두 확정할 수는 없습니다.

1. **경로에 따라 대상 서비스가 정해집니다.**

   아래는 문서에서 외부 경로와 대상 서비스의 연결이 확인된 예입니다. 와일드카드 규칙 전체를 옮긴 표는 아닙니다.

   | Gateway 외부 경로 | 대상 서비스 | 근거 |
   |---|---|---|
   | `/api/v1/auth/login` | `user-service` | `gateway/src/main/resources/application.yml:14`, `gateway/src/main/resources/application.yml:15`, `gateway/src/main/resources/application.yml:17` |
   | `/api/v1/companies` | `company-service` | `gateway/src/main/resources/application.yml:21`, `gateway/src/main/resources/application.yml:22`, `gateway/src/main/resources/application.yml:24` |
   | `/api/v1/products`, `/api/v1/promotions`, `/api/v1/reviews` | `product-service` | `gateway/src/main/resources/application.yml:28`, `gateway/src/main/resources/application.yml:29`, `gateway/src/main/resources/application.yml:31` |
   | `/api/v1/coupon-infos` | `coupon-service` | `gateway/src/main/resources/application.yml:35`, `gateway/src/main/resources/application.yml:36`, `gateway/src/main/resources/application.yml:38` |
   | `/api/v1/orders` | `order-service` | `gateway/src/main/resources/application.yml:42`, `gateway/src/main/resources/application.yml:43`, `gateway/src/main/resources/application.yml:45` |
   | `/api/v1/payments` | `payment-service` | `gateway/src/main/resources/application.yml:49`, `gateway/src/main/resources/application.yml:50`, `gateway/src/main/resources/application.yml:52` |

2. **인증 제외 요청은 JWT 검사와 사용자 헤더 생성을 건너뜁니다.**

   `AuthenticationFilter`는 `AllowedPath`에 해당하면 JWT 추출·검증 전에 요청을 그대로 다음 단계로 전달합니다. 문서상 로그인, 프로모션 목록 GET, 리뷰 GET은 토큰이 필요 없는 경로입니다. 특히 리뷰는 `/api/v1/reviews`로 시작하는 GET을 허용합니다.  
   근거: `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AllowedPath.java:6`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AllowedPath.java:12`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AllowedPath.java:28`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AllowedPath.java:29`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:24`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:28`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:30`。

   따라서 인증 제외 요청은 **`Authorization`에 JWT를 넣어도 사용자 헤더를 생성하지 않습니다.** 요청에 이미 사용자 헤더가 있다면 이 분기에서는 JWT로 검증하거나 덮어쓰지도 않습니다.  
   근거: `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:28`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:30`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:40`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:41`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:42`。

3. **인증 대상 요청은 JWT를 검증하고 사용자 헤더를 덮어씁니다.**

   Gateway의 `Authorization` 파서는 `Bearer ` 접두사를 제거한 토큰을 검증에 사용합니다. 이는 **파싱할 토큰 문자열의 처리**이며, 전달되는 `Authorization` 헤더 자체를 제거하거나 변경한다는 근거는 제공된 문서에서 확인되지 않았습니다.  
   근거: `gateway/src/main/java/com/sparta/hotdeal/gateway/security/JwtUtil.java:31`, `gateway/src/main/java/com/sparta/hotdeal/gateway/security/JwtUtil.java:33`, `gateway/src/main/java/com/sparta/hotdeal/gateway/security/JwtUtil.java:34`。

   검증을 통과하면 다음 세 헤더를 설정·덮어써 대상 서비스에 전달합니다: `X-User-UserId`, `X-User-Email`, `X-User-Role`. 업체 API는 이러한 인증 대상의 확인된 사례입니다.  
   근거: `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:40`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:41`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:42`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AllowedPath.java:6`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AllowedPath.java:28`。

4. **대상 서비스에서도 인증·권한 처리가 이어집니다.**

   업체 서비스는 전달된 사용자 헤더로 인증 객체를 만들고 `@Secured`로 역할을 검사합니다. 따라서 Gateway의 토큰 검증 성공만으로 모든 서비스 작업이 허용되는 것은 아닙니다.  
   근거: `company/src/main/java/com/sparta/hotdeal/company/infrastructure/filter/AuthenticationFilter.java:34`, `company/src/main/java/com/sparta/hotdeal/company/infrastructure/filter/AuthenticationFilter.java:60`, `company/src/main/java/com/sparta/hotdeal/company/infrastructure/filter/AuthenticationFilter.java:65`, `company/src/main/java/com/sparta/hotdeal/company/infrastructure/config/SecurityConfig.java:17`, `company/src/main/java/com/sparta/hotdeal/company/presentation/controller/CompanyController.java:42`。

   실제로 **리뷰 GET은 양쪽 인증 규칙이 어긋납니다.** Gateway는 인증을 생략하지만 product 서비스 필터는 리뷰 GET을 제외하지 않아, 세 사용자 헤더 중 하나라도 없으면 예외를 던집니다. JWT만 추가해도 Gateway의 제외 분기는 바뀌지 않습니다.  
   근거: `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AllowedPath.java:12`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:30`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/filter/AuthenticationFilter.java:31`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/filter/AuthenticationFilter.java:40`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/filter/AuthenticationFilter.java:50`。

**경로 변경과 확인 한계:** 문서상 외부 `/api/v1/orders`는 서비스 컨트롤러에서도 `/api/v1/orders`로 대응합니다. 따라서 이 사례에서 `/api/v1`이 제거된다고 설명할 근거는 없습니다. 하지만 정확한 `RewritePath` 정규식·치환식은 본문에 노출되지 않아, 모든 경로의 변환을 확정할 수 없습니다. 근거: `gateway/src/main/resources/application.yml:45`, `gateway/src/main/resources/application.yml:47`, `order/src/main/java/com/sparta/hotdeal/order/presentation/controller/OrderController.java:35`, `order/src/main/java/com/sparta/hotdeal/order/presentation/controller/OrderController.java:40`。

또한 제공된 문서만으로는 JWT 누락·만료·위조 각각의 정확한 HTTP 응답, 전체 인증 제외 목록, 실제 전달 대상 인스턴스 주소를 확인할 수 없습니다. JWT 비밀키와 활성 프로필의 실제 값은 저장소 밖 환경변수로 정해집니다. 근거: `gateway/src/main/resources/application.yml:6`, `gateway/src/main/resources/application.yml:80`, `docker-compose.yml:18`, `docker-compose.yml:19`。

</details>

<a id="q19-ask"></a>

### Q19 질의응답

> Gateway로 들어온 요청은 어떤 라우팅·인증 처리를 거쳐 대상 서비스로 전달되고, 그 과정에서 경로나 헤더가 어떻게 바뀌나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 틀림 | 틀림 |
| 확인 범위 | 밝힘 | 안 밝힘 |
| 이유 | 확인할 수 없다는 한계는 밝혔지만, 저장소에는 라우팅·경로 재작성·인증·헤더 변경 코드가 모두 있어 질문의 핵심에 답하지 못했습니다. | 저장소의 라우트 설정, RewritePath, GlobalFilter의 JWT 검증, X-User-* 헤더 추가 코드로 질문에 답할 수 있는데도 정보가 없다고 했다. 코드로 확인되는 것과 확인되지 않는 것을 나누지 않고 전부 확인할 수 없다고 했다. |
| 대조한 근거 | `gateway/src/main/resources/application.yml:13`, `gateway/src/main/resources/application.yml:19`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:24`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:30` 외 1개 | `gateway/src/main/resources/application.yml:13-54`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:15-46`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AllowedPath.java:5-30` |

<details><summary>답 원문</summary>

제공된 조각에는 Gateway의 라우팅·인증 흐름이나 전달 과정에서 경로·헤더가 어떻게 바뀌는지에 대한 정보가 없습니다. 확인할 수 없습니다.

</details>

<a id="q20-mcp"></a>

### Q20 MCP

> Gateway 자체에서 요청을 거부한 경우와 대상 서비스에서 실패한 경우를 어떻게 구분하고, 각 오류 응답은 어떤 처리를 거쳐 클라이언트에 반환되나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 부분 | 부분 |
| 확인 범위 | 밝힘 | 일부 |
| 이유 | 서비스별 오류 처리와 리뷰 500의 원인을 확정하지 않은 설명은 타당합니다. 그러나 코드에 명시된 Gateway의 본문 없는 401 종료를 확인 불가로 처리했고, 라우트와 체인에서 확인되는 프록시 위임 흐름도 빠뜨렸습니다. | 서비스 쪽 오류 처리 설명은 코드와 맞습니다. 결제 PA001→404 처리, user의 IllegalArgumentException→400 errorMessage 변환, 리뷰 GET에서 Gateway와 product 필터가 어긋나는 점이 그렇습니다. 하지만 Gateway 자체 거부의 상태와 본문을 확인할 수 없다고 한 것은 틀렸습니다. AuthenticationFilter:30-32에 본문 없는 401 응답과 setComplete()가 그대로 있어, 코드로 확인되는 것을 확인되지 않는다고 잘못 나눴습니다. |
| 대조한 근거 | `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:30`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:32`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:46`, `gateway/src/main/resources/application.yml:13` 외 3개 | `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:30`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:31`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:32`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AllowedPath.java:12` 외 8개 |

<details><summary>답 원문</summary>

**오류가 발생한 위치와 응답을 만든 처리기를 기준으로 구분해야 합니다.** 다만 MCP 문서로는 서비스 내부의 오류 처리 사례까지 확인되며, **Gateway 자체 거부 응답의 생성 과정과 서비스 응답의 최종 전달·변환 과정은 확인되지 않습니다.**

1. **Gateway에서 판단하는 것은 우선 인증 통과 여부입니다.**  
   인증 제외 경로는 JWT 추출·검증과 사용자 헤더 설정 전에 그대로 전달합니다. 인증을 통과하는 경로에서는 `X-User-UserId`, `X-User-Email`, `X-User-Role`을 설정합니다. 따라서 Gateway의 인증 단계와 서비스 내부의 인증·업무 처리 단계는 구분해야 합니다.  
   근거: `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:24`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:28`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:30`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:40`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:41`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:42`

   **그러나 토큰 누락·검증 실패 시 Gateway가 어떤 예외 처리기를 거쳐 어떤 HTTP 상태·JSON 본문을 쓰는지는 제공된 문서에서 확인할 수 없습니다.** 이를 “항상 401을 직접 반환한다”거나 특정 공통 오류 형식을 쓴다고 단정할 근거는 없습니다.

2. **대상 서비스의 업무 오류는 해당 서비스의 예외 처리기가 응답으로 바꾸는 사례가 확인됩니다.**  
   예를 들어 결제 상세 조회는 요청자 소유의 미삭제 결제가 없으면 `404 / PA001`로 처리됩니다. 확인되는 흐름은 **서비스 조회 실패 → 서비스의 오류 코드·`GlobalExceptionHandler` → HTTP 오류 응답**입니다. 이 엔드포인트는 코드 확인만 되었고 실제 오류 응답을 호출해 검증한 기록은 없습니다.  
   근거: `payment/src/main/java/com/sparta/hotdeal/payment/application/service/PaymentService.java:72`, `payment/src/main/java/com/sparta/hotdeal/payment/application/service/PaymentService.java:73`, `payment/src/main/java/com/sparta/hotdeal/payment/common/exception/ErrorCode.java:20`, `payment/src/main/java/com/sparta/hotdeal/payment/common/exception/GlobalExceptionHandler.java:23`, `payment/src/main/java/com/sparta/hotdeal/payment/common/exception/GlobalExceptionHandler.java:26`, `payment/src/main/java/com/sparta/hotdeal/payment/common/exception/GlobalExceptionHandler.java:27`

   사용자 서비스에서는 이메일 인증 조건을 충족하지 못한 가입 요청의 `IllegalArgumentException`을 **HTTP 400의 `errorMessage` 응답**으로 바꿉니다. 오류 응답은 서비스별 처리 근거를 확인해야 합니다.  
   근거: `user/src/main/java/com/sparta/hotdeal/user/application/service/AuthService.java:161`, `user/src/main/java/com/sparta/hotdeal/user/application/service/AuthService.java:162`, `user/src/main/java/com/sparta/hotdeal/user/application/service/AuthService.java:163`, `user/src/main/java/com/sparta/hotdeal/user/application/exception/GlobalExceptionHandler.java:14`, `user/src/main/java/com/sparta/hotdeal/user/application/exception/GlobalExceptionHandler.java:19`, `user/src/main/java/com/sparta/hotdeal/user/application/exception/GlobalExceptionHandler.java:25`

3. **인증 관련 실패라도 대상 서비스에서 발생할 수 있습니다.**  
   리뷰 GET은 Gateway의 인증 제외 경로이지만, product 서비스 필터는 리뷰 GET을 제외하지 않습니다. 사용자 헤더가 누락되면 서비스 필터에서 `javax.security.sasl.AuthenticationException`을 던져 컨트롤러 전에 막힙니다. 이는 Gateway 자체 거부와 다른 경로입니다.  
   근거: `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AllowedPath.java:12`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:28`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/filter/AuthenticationFilter.java:40`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/filter/AuthenticationFilter.java:50`

   실제 리뷰 목록 응답에는 **500 / Internal Server Error**가 기록되어 있습니다. 다만 요청 헤더가 기록되지 않아, 그 500의 원인을 위 헤더 누락 분기로 확정할 수는 없습니다.  
   근거: `responses/product-service.ReviewController.getReviewList.json:3`, `responses/product-service.ReviewController.getReviewList.json:4`, `responses/product-service.ReviewController.getReviewList.json:7`, `responses/product-service.ReviewController.getReviewList.json:11`

**클라이언트에 반환되는 마지막 단계는 근거가 부족합니다.** 서비스 오류 응답을 Gateway가 그대로 전달하는지, 본문·헤더를 재작성하는지, Gateway 자체 예외와 공통 형식으로 통합하는지는 이번 MCP 문서에서 확인할 수 없습니다. 따라서 위 서비스별 응답 생성 과정과 실제 저장된 응답을 넘어, 전체 반환 경로를 확정해서 설명할 수는 없습니다.

</details>

<a id="q20-ask"></a>

### Q20 질의응답

> Gateway 자체에서 요청을 거부한 경우와 대상 서비스에서 실패한 경우를 어떻게 구분하고, 각 오류 응답은 어떤 처리를 거쳐 클라이언트에 반환되나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 부분 | 부분 |
| 확인 범위 | 밝힘 | 일부 |
| 이유 | Gateway의 토큰 누락·검증 실패 분기가 본문 없는 401로 종료된다는 설명은 맞습니다. 확인하지 못한 범위는 밝혔지만, 저장소에 존재하는 서비스 예외 처리와 Gateway를 통한 반환 흐름을 설명하지 않아 질문의 핵심 일부가 빠졌습니다. | Gateway 자체 거부(토큰 없음 또는 검증 실패 시 본문 없는 401)는 코드와 맞습니다. 하지만 대상 서비스 실패의 처리 과정은 근거가 없다고 했는데, 저장소에는 서비스별 @RestControllerAdvice와 Security 처리기가 있습니다. 질문의 절반을 근거 없음으로 잘못 처리했습니다. |
| 대조한 근거 | `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:30`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:32`, `order/src/main/java/com/sparta/hotdeal/order/common/exception/GlobalExceptionHandler.java:24`, `order/src/main/java/com/sparta/hotdeal/order/infrastructure/exception/handler/CustomAuthenticationHandler.java:14` 외 2개 | `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:30`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:32`, `order/src/main/java/com/sparta/hotdeal/order/common/exception/GlobalExceptionHandler.java:24`, `user/src/main/java/com/sparta/hotdeal/user/application/exception/GlobalExceptionHandler.java:14` 외 1개 |

<details><summary>답 원문</summary>

Gateway 자체의 거부는 토큰이 없거나 유효하지 않을 때 발생하며, `AuthenticationFilter`가 `UNAUTHORIZED` 상태로 거부하고 응답 본문은 없는 것으로 확인됩니다. 대상 서비스의 실패와 그 응답이 어떤 처리를 거쳐 반환되는지는 제공된 조각에 근거가 없어 확인할 수 없습니다. [gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:30, gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:31, gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:32]

</details>

## 3. 두 채점이 같은 답 가운데 무작위 표본(시드 45) (5)

<a id="q08-mcp"></a>

### Q08 MCP

> 카카오페이 결제를 마친 뒤 어떤 주소로 돌아와서 어떤 처리를 거쳐 운영 화면으로 이동하나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 맞음 | 맞음 |
| 확인 범위 | 밝힘 | 밝힘 |
| 이유 | 성공 경로의 안내 화면과 별도 승인 API, 결제·주문 상태 처리에 대한 설명이 코드와 일치한다. 운영 도메인과 화면 이동 연결의 미확인 여부, 비동기 주문 갱신의 한계도 구분했다. | A의 주장은 코드와 일치합니다. 복귀 경로는 /payment/success이고 이 경로는 안내 템플릿만 반환합니다. 승인은 별도 confirm API에서 COMPLETE 변경과 Kafka 발행을 거치며, 전송 결과를 기다리지 않는 비동기 send라는 점도 코드와 같습니다. 도메인과 포트, 운영 화면으로의 이동은 저장소에서 확인되지 않는다고 구분해 밝혔습니다. |
| 대조한 근거 | `payment/src/main/java/com/sparta/hotdeal/payment/infrastructure/dto/kakaopay/req/ReqPostKakaoPayReadyDto.java:41`, `payment/src/main/java/com/sparta/hotdeal/payment/presentation/controller/PaymentViewController.java:18-20`, `payment/src/main/resources/templates/payment-success.html:1-12`, `payment/src/main/java/com/sparta/hotdeal/payment/application/service/PaymentService.java:58-67` 외 3개 | `payment/src/main/java/com/sparta/hotdeal/payment/infrastructure/dto/kakaopay/req/ReqPostKakaoPayReadyDto.java:41`, `payment/src/main/java/com/sparta/hotdeal/payment/presentation/controller/PaymentViewController.java:18-21`, `payment/src/main/resources/templates/payment-success.html:9`, `payment/src/main/java/com/sparta/hotdeal/payment/presentation/controller/PaymentController.java:44-48` 외 5개 |

<details><summary>답 원문</summary>

**확인되는 복귀 경로는 `/payment/success`입니다. 다만 여기서 결제를 승인하고 운영 화면으로 이동하는 자동 흐름은 확인되지 않습니다.** 성공 화면은 완료 안내만 표시합니다. 근거: `payment/src/main/java/com/sparta/hotdeal/payment/infrastructure/dto/kakaopay/req/ReqPostKakaoPayReadyDto.java:41`, `payment/src/main/java/com/sparta/hotdeal/payment/presentation/controller/PaymentViewController.java:20`, `payment/src/main/resources/templates/payment-success.html:9`.

1. **카카오페이에서 돌아오는 곳**  
   결제 준비 요청의 `approvalUrl`은 `/payment/success`로 설정됩니다. `GET /payment/success` 처리기는 성공 템플릿을 반환합니다. 도구가 제시한 근거로는 경로까지 확인되며, 운영 환경의 도메인·포트를 포함한 전체 주소는 확인할 수 없습니다. 근거: `payment/src/main/java/com/sparta/hotdeal/payment/infrastructure/dto/kakaopay/req/ReqPostKakaoPayReadyDto.java:41`, `payment/src/main/java/com/sparta/hotdeal/payment/presentation/controller/PaymentViewController.java:18`, `payment/src/main/java/com/sparta/hotdeal/payment/presentation/controller/PaymentViewController.java:20`.

2. **실제 결제 승인은 별도 요청**  
   성공 화면에는 `pg_token` 수신이나 승인 API 호출 코드가 없습니다. 별도로 `tid`와 `pgToken`을 담아 `POST /api/v1/payments/confirm`을 호출해야 합니다. 근거: `payment/src/main/resources/templates/payment-success.html:9`, `payment/src/main/java/com/sparta/hotdeal/payment/presentation/controller/PaymentController.java:44`, `payment/src/main/java/com/sparta/hotdeal/payment/application/dtos/payment/req/ReqPostPaymentConfirmDto.java:14`, `payment/src/main/java/com/sparta/hotdeal/payment/application/dtos/payment/req/ReqPostPaymentConfirmDto.java:16`.

3. **승인 API 내부 처리**  
   `tid`로 결제를 조회하고 카카오페이 승인을 요청합니다. 정상 응답을 받으면 결제를 `COMPLETE`로 변경하고 주문 상태 변경 이벤트를 발행한 뒤 승인 결과를 반환합니다. 근거: `payment/src/main/java/com/sparta/hotdeal/payment/application/service/PaymentService.java:59`, `payment/src/main/java/com/sparta/hotdeal/payment/application/service/PaymentService.java:61`, `payment/src/main/java/com/sparta/hotdeal/payment/application/service/PaymentService.java:64`, `payment/src/main/java/com/sparta/hotdeal/payment/application/service/PaymentService.java:65`, `payment/src/main/java/com/sparta/hotdeal/payment/application/service/PaymentService.java:67`.

4. **주문 상태 반영**  
   `KAFKA_UPDATE_ORDER_STATUS_TOPIC` 설정을 통해 주문 서비스가 이벤트를 받아 주문을 `COMPLETE`로 변경합니다. 다만 발행 코드는 비동기 전송 결과를 기다리지 않으므로 승인 API 성공만으로 주문 갱신 완료까지 보장하지는 않습니다. 근거: `payment/src/main/resources/application.yml:18`, `order/src/main/resources/application.yml:17`, `order/src/main/java/com/sparta/hotdeal/order/event/consumer/OrderStatusUpdateEventConsumer.java:25`, `order/src/main/java/com/sparta/hotdeal/order/event/service/OrderConsumerService.java:30`, `payment/src/main/java/com/sparta/hotdeal/payment/event/producer/PaymentEventProducer.java:52`.

**운영 화면의 목적지 주소와 이동 처리는 저장소에서 확인할 수 없습니다.** 확인된 성공 페이지는 안내 템플릿이고, 승인 API는 결과 DTO를 반환하므로 이 둘을 연결하는 클라이언트 처리와 이후 화면 이동을 추정해서는 안 됩니다. 근거: `payment/src/main/java/com/sparta/hotdeal/payment/presentation/controller/PaymentViewController.java:20`, `payment/src/main/resources/templates/payment-success.html:9`, `payment/src/main/java/com/sparta/hotdeal/payment/application/service/PaymentService.java:67`.

MCP 문서의 확인 상태는 **코드 기준 확인**이며, 성공 HTML과 카카오페이 승인 API의 실제 호출 검증은 수행되지 않았습니다.

</details>

<a id="q11-ask"></a>

### Q11 질의응답

> 결제 완료 안내는 어떤 처리 결과를 기준으로 표시되고, Payment의 결제 결과가 Order에 전달돼 주문 상태가 바뀌기까지 어떤 경로를 거치나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 틀림 | 틀림 |
| 확인 범위 | 안 밝힘 | 안 밝힘 |
| 이유 | 답이 없어 화면 표시 기준과 결제 결과 전달 경로에 답하지 않았고, 확인 범위도 밝히지 않았다. | 답이 없어 질문에 답하지 못했다. |
| 대조한 근거 |  |  |

<details><summary>답 원문</summary>

(답 없음)

</details>

<a id="q14-base"></a>

### Q14 기준선

> 프로모션 응답의 시간 필드는 어떤 시간대와 시작·종료 기준을 쓰고, 가격 필드는 각각 어떤 금액을 뜻하나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 맞음 | 맞음 |
| 확인 범위 | 밝힘 | 밝힘 |
| 이유 | LocalDateTime 저장과 Timestamp 변환, 시작·종료 처리 및 가격 계산 설명이 코드와 일치하며, 할인 판매 단가라는 설명도 주문의 수량 곱셈으로 확인된다. JVM 기본 시간대 의존성과 실제 배포·직렬화 설정의 불확실성을 구분했다. | 시간대 정보 없이 LocalDateTime으로 저장하고 Timestamp.valueOf로 변환하는 점, LocalDateTime.now()가 JVM 기본 시간대에 기대는 점, Dockerfile에 시간대 설정이 없는 점, 정각 스케줄러의 정확 일치 비교, 중복 조건, discountPrice가 단가로 쓰여 주문 계산에서 수량을 곱한다는 점, discountRate 계산식이 모두 코드와 맞습니다. 배포 시간대, JSON 오프셋, 통화는 확인할 수 없다고 구분했습니다. |
| 대조한 근거 | `product/src/main/java/com/sparta/hotdeal/product/domain/entity/promotion/Promotion.java:40-49`, `product/src/main/java/com/sparta/hotdeal/product/application/service/promotion/PromotionService.java:44-65`, `product/src/main/java/com/sparta/hotdeal/product/application/service/promotion/PromotionService.java:154-181`, `product/src/main/java/com/sparta/hotdeal/product/application/scheduler/PromotionScheduler.java:27-59` 외 3개 | `product/src/main/java/com/sparta/hotdeal/product/domain/entity/promotion/Promotion.java:40`, `product/src/main/java/com/sparta/hotdeal/product/domain/entity/promotion/Promotion.java:43`, `product/src/main/java/com/sparta/hotdeal/product/application/service/promotion/PromotionService.java:44`, `product/src/main/java/com/sparta/hotdeal/product/application/service/promotion/PromotionService.java:178` 외 9개 |

<details><summary>답 원문</summary>

**시간대는 KST나 UTC로 확정할 수 없고, `discountPrice`는 할인 후 판매 단가입니다.** 근거와 세부 기준은 다음과 같습니다.

- **시간 필드의 의미와 시간대:** `start`·`end`는 등록한 프로모션 시작·종료 시각입니다. 엔티티에는 시간대 정보가 없는 `LocalDateTime`으로 저장하고, 응답에서는 `Timestamp.valueOf(...)`로 변환합니다. 현재 시각도 시간대를 지정하지 않은 `LocalDateTime.now()`를 사용하므로 JVM 기본 시간대에 의존합니다. 실제 배포 환경의 시간대와 JSON 응답의 오프셋은 저장소만으로 확인할 수 없습니다.  
  근거: `product/src/main/java/com/sparta/hotdeal/product/domain/entity/promotion/Promotion.java:40`, `:43`, `:66`; `product/src/main/java/com/sparta/hotdeal/product/application/service/promotion/PromotionService.java:44`, `:178`; `product/Dockerfile:19`.

- **실제 시작·종료 기준:** 매 정각 실행되는 스케줄러가 현재 시각의 소수 초를 버린 뒤, `start` 또는 `end`가 **정확히 일치하는** 프로모션을 조회합니다. 시작 시 `IN_PROGRESS`로 바꾸고 상품에 할인 판매가를 설정하며, 종료 시 `COMPLETED`로 바꾸고 상품의 할인 판매가를 `null`로 만듭니다. 따라서 일반적인 `start ≤ 현재 < end` 판정이라고 단정할 수 없으며, 정각이 아닌 시각이나 놓친 실행을 따라잡는 처리는 이 코드에 없습니다.  
  근거: `product/src/main/java/com/sparta/hotdeal/product/application/scheduler/PromotionScheduler.java:27`, `:32`, `:38`, `:45`, `:50`, `:56`; `product/src/main/java/com/sparta/hotdeal/product/infrastructure/repository/promotion/PromotionRepositoryJpa.java:21`, `:24`.

- **기간 입력·중복 기준:** 생성·수정 시 시작이나 종료가 현재보다 과거이면 거절합니다. `start < end` 검증은 해당 메서드에 없습니다. 중복 검사는 `기존 start < 요청 end AND 기존 end > 요청 start`이므로, 기존 종료와 새 시작이 같은 경우는 중복으로 보지 않습니다. 이는 중복 판정 기준이며, 판매 가능 기간의 경계 보장과는 구분해야 합니다.  
  근거: `product/src/main/java/com/sparta/hotdeal/product/application/service/promotion/PromotionService.java:39`, `:46`, `:51`, `:74`, `:86`, `:95`; `product/src/main/java/com/sparta/hotdeal/product/infrastructure/repository/promotion/PromotionRepositoryJpa.java:16`.

- **`discountPrice`:** 프로모션을 적용한 **상품 1개당 판매금액**입니다. 차감되는 할인액을 뜻하지 않습니다. 상품의 기본 `price`보다 작아야 하며, 주문 계산에서도 이 금액을 단가로 선택해 수량을 곱합니다.  
  근거: `product/src/main/java/com/sparta/hotdeal/product/application/service/promotion/PromotionService.java:59`, `:163`, `:181`; `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderCalculationService.java:45`, `:52`.

- **`discountRate`:** 금액이 아니라 할인율(%)입니다. `round((price - discountPrice) / price × 100)`으로 계산한 정수이며, 조회 시 저장된 값을 반환합니다.  
  근거: `product/src/main/java/com/sparta/hotdeal/product/application/service/promotion/PromotionService.java:65`, `:154`, `:180`.

- **`price`와 응답 범위:** `price`는 상품 응답의 기본 판매 단가이자 할인율 계산 기준입니다. 프로모션 조회 응답 자체에는 `price`나 별도의 할인액 필드가 없고, 가격 관련 필드는 `discountPrice`·`discountRate`입니다. 이 필드 정의에는 통화·금액 단위가 명시되어 있지 않아 원화라고 확정할 수 없습니다.  
  근거: `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/product/ResGetProductDto.java:19`; `product/src/main/java/com/sparta/hotdeal/product/application/service/promotion/PromotionService.java:65`; `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/promotion/ResGetPromotionDto.java:15`.

</details>

<a id="q08-base"></a>

### Q08 기준선

> 카카오페이 결제를 마친 뒤 어떤 주소로 돌아와서 어떤 처리를 거쳐 운영 화면으로 이동하나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 맞음 | 맞음 |
| 확인 범위 | 밝힘 | 밝힘 |
| 이유 | 환경변수를 포함한 복귀 주소 구성, 게이트웨이 경로, 안내 화면과 별도 승인 처리를 정확히 설명했다. 실제 운영 도메인 값과 운영 화면으로의 이동 로직이 확인되지 않는다는 점도 명확하다. | B는 복귀 주소를 domain+/payment/success로 설명하고, 도메인이 PAY_HOST_DOMAIN 환경변수에서 온다고 짚었습니다. 게이트웨이의 /payment/** 라우팅, 문구만 있는 성공 템플릿, 승인 API의 카카오 approve 호출과 COMPLETE 처리도 모두 코드와 맞습니다. 게이트웨이 인용 줄(49)은 실제 Path 줄(52)이 아니라 라우트 블록의 시작 줄입니다. 운영 도메인 값과 이후 화면 이동은 확인할 수 없다고 구분했습니다. |
| 대조한 근거 | `payment/src/main/resources/application.yml:25-29`, `payment/src/main/java/com/sparta/hotdeal/payment/infrastructure/adapter/KakaoPayClientAdapter.java:35-47`, `payment/src/main/java/com/sparta/hotdeal/payment/infrastructure/dto/kakaopay/req/ReqPostKakaoPayReadyDto.java:41`, `gateway/src/main/resources/application.yml:49-54` 외 4개 | `payment/src/main/java/com/sparta/hotdeal/payment/infrastructure/dto/kakaopay/req/ReqPostKakaoPayReadyDto.java:41`, `payment/src/main/resources/application.yml:29`, `docker-compose.yml:187`, `gateway/src/main/resources/application.yml:49-52` 외 5개 |

<details><summary>답 원문</summary>

저장소에서 확인되는 흐름은 **성공 주소로 복귀해 ‘결제 완료 페이지’를 표시하는 것까지**입니다. 이 페이지에서 결제를 자동 승인하거나 운영 화면으로 이동하는 처리는 없습니다. (`payment/src/main/java/com/sparta/hotdeal/payment/presentation/controller/PaymentViewController.java:18`, `payment/src/main/resources/templates/payment-success.html:1`)

1. **돌아오는 주소는 `${PAY_HOST_DOMAIN}/payment/success`입니다.** 카카오페이 결제 준비 요청의 `approvalUrl`을 `domain + "/payment/success"`로 설정하며, 도메인은 환경변수 `PAY_HOST_DOMAIN`에서 가져옵니다. 실제 운영 도메인 값은 저장소에서 확인할 수 없습니다. (`payment/src/main/java/com/sparta/hotdeal/payment/infrastructure/dto/kakaopay/req/ReqPostKakaoPayReadyDto.java:41`, `payment/src/main/resources/application.yml:29`, `docker-compose.yml:187`)

2. **복귀 요청은 성공 화면을 렌더링합니다.** 게이트웨이는 `/payment/**`를 결제 서비스로 전달하고, `GET /payment/success`는 `payment-success` 템플릿을 반환합니다. 템플릿에는 “결제 완료 페이지” 문구만 있고, 토큰 추출·승인 API 호출·화면 이동 스크립트가 없습니다. (`gateway/src/main/resources/application.yml:49`, `payment/src/main/java/com/sparta/hotdeal/payment/presentation/controller/PaymentViewController.java:8`, `payment/src/main/java/com/sparta/hotdeal/payment/presentation/controller/PaymentViewController.java:18`, `payment/src/main/resources/templates/payment-success.html:1`)

3. **실제 결제 승인은 별도의 API입니다.** `POST /api/v1/payments/confirm`에 `tid`, `pgToken`을 보내면 인증 사용자의 ID로 승인 서비스를 호출합니다. 서비스는 `tid`로 결제를 조회하고 카카오페이 `/online/v1/payment/approve`를 호출한 뒤, 결제 상태를 `COMPLETE`로 바꾸고 주문 상태도 `COMPLETE`로 변경하도록 메시지를 보냅니다. (`payment/src/main/java/com/sparta/hotdeal/payment/presentation/controller/PaymentController.java:33`, `payment/src/main/java/com/sparta/hotdeal/payment/presentation/controller/PaymentController.java:44`, `payment/src/main/java/com/sparta/hotdeal/payment/application/dtos/payment/req/ReqPostPaymentConfirmDto.java:13`, `payment/src/main/java/com/sparta/hotdeal/payment/application/service/PaymentService.java:58`, `payment/src/main/java/com/sparta/hotdeal/payment/infrastructure/client/KakaoPayClient.java:14`, `payment/src/main/java/com/sparta/hotdeal/payment/infrastructure/client/KakaoPayClient.java:22`)

4. **승인 이후 운영 화면으로 이동하는 주소와 연결 로직은 확인할 수 없습니다.** 승인 API는 결과를 `ResponseDto`로 반환하고, 성공 페이지에도 이동 코드가 없습니다. 따라서 “복귀 → 자동 승인 → 운영 화면 이동”이 구현되어 있다고 설명할 근거는 없습니다. (`payment/src/main/java/com/sparta/hotdeal/payment/presentation/controller/PaymentController.java:48`, `payment/src/main/resources/templates/payment-success.html:1`)

</details>

<a id="q02-ask"></a>

### Q02 질의응답

> 재고를 차감한 뒤 주문 생성에 실패하면 재고를 어떻게 복구하고, 복구까지 실패하면 어떻게 처리하나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 부분 | 부분 |
| 확인 범위 | 밝힘 | 밝힘 |
| 이유 | 복구 소비자의 예외 처리와 비동기 발행의 한계는 맞지만, 주문 저장이 먼저라는 핵심과 결제 준비 실패 시 복구 요청 경로를 빠뜨렸다. 또한 두 후속 토픽으로 설명했으나 실제로 차감 실패와 복구 성공 모두 같은 request-order 설정을 사용하며, 연결·완료 여부의 불확실성은 밝혔다. | 복구 실패 시 로그·ACK만 하고, try 밖 예외에만 재시도 0회와 product-dlq가 적용되며, ProductKafkaProducer 전송을 확인하지 않는다는 점은 코드와 맞습니다. 그러나 재고 복구를 누가 어떻게 트리거하는지(결제 준비 실패 경로)와 복구 방식(수량 가산)이라는 핵심이 빠졌습니다. 차감 실패 시와 복구 성공 시 메시지는 실제로 같은 request-order 토픽으로 가는데 '두 후속 토픽'이라고 했고, 근거 줄도 제시하지 않았습니다. 구독자와 토픽 값을 확인할 수 없다는 점은 구분했습니다. |
| 대조한 근거 | `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:108`, `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:122`, `payment/src/main/java/com/sparta/hotdeal/payment/event/consumer/PaymentReadyEventConsumer.java:55`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java:48` 외 4개 | `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java:47`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java:70`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java:74`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductInventoryService.java:144` 외 3개 |

<details><summary>답 원문</summary>

코드 설명상 재고 차감 실패 시 `rollbackOrder` 요청을 발행하고, 재고 복구 메시지 처리가 성공하면 `requestOrder`를 발행해 주문 취소를 요청합니다. 다만 이 두 후속 토픽의 구독자는 확인 안 됐고, 토픽 실제 값도 저장소에 근거가 없습니다. 따라서 주문 생성 실패 후 재고 복구·주문 롤백이 실제로 완료된다고 확인할 수는 없습니다. [AI 해석] 복구 처리 자체가 실패하면 리스너는 예외를 다시 던지지 않고 로그와 acknowledge만 수행하며, 공통 오류 처리기로 전달되지 않는 분기가 있습니다. JSON 역직렬화 등 try 밖으로 전파되는 실패에는 재시도 0회와 `product-dlq` recoverer가 설정되어 있지만, 내부 트랜잭션 종료 시 추가 예외가 나는 경우까지 항상 DLQ에 간다고 단정할 수는 없습니다. 또 `ProductKafkaProducer`는 전송 완료를 비동기로 확인하지 않아 후속 메시지 전달 성공도 보장되지 않습니다.

</details>
