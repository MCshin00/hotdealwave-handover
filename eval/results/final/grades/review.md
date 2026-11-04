# 개발자 확인 목록

정확도: 맞음·부분·틀림. 확인 범위: 밝힘·일부·안 밝힘. 갈림은 두 채점자의 값이 다르다는 표시다.

| 묶음 | 질문 | 조건 | 정확도 Codex / Opus | 확인 범위 Codex / Opus |
|---|---|---|---|---|
| 1 | [Q04](#q04-mcp) | MCP | 부분 / 맞음 | 밝힘 / 밝힘 |
| 1 | [Q06](#q06-ask) | 질의응답 | 맞음 / 부분 | 밝힘 / 일부 |
| 1 | [Q09](#q09-ask) | 질의응답 | 맞음 / 틀림 | 밝힘 / 일부 |
| 1 | [Q13](#q13-mcp) | MCP | 부분 / 틀림 | 밝힘 / 일부 |
| 1 | [Q14](#q14-ask) | 질의응답 | 부분 / 틀림 | 밝힘 / 일부 |
| 1 | [Q15](#q15-ask) | 질의응답 | 맞음 / 부분 | 밝힘 / 일부 |
| 1 | [Q16](#q16-ask) | 질의응답 | 맞음 / 부분 | 밝힘 / 밝힘 |
| 1 | [Q21](#q21-mcp) | MCP | 맞음 / 부분 | 밝힘 / 밝힘 |
| 1 | [Q21](#q21-ask) | 질의응답 | 맞음 / 부분 | 밝힘 / 일부 |
| 2 | [Q03](#q03-ask) | 질의응답 | 부분 / 부분 | 밝힘 / 일부 |
| 2 | [Q04](#q04-ask) | 질의응답 | 틀림 / 틀림 | 일부 / 안 밝힘 |
| 2 | [Q05](#q05-ask) | 질의응답 | 부분 / 부분 | 밝힘 / 일부 |
| 2 | [Q07](#q07-ask) | 질의응답 | 부분 / 부분 | 밝힘 / 일부 |
| 2 | [Q12](#q12-ask) | 질의응답 | 부분 / 부분 | 밝힘 / 일부 |
| 2 | [Q17](#q17-ask) | 질의응답 | 부분 / 부분 | 밝힘 / 일부 |
| 2 | [Q18](#q18-mcp) | MCP | 부분 / 부분 | 일부 / 밝힘 |
| 2 | [Q18](#q18-ask) | 질의응답 | 부분 / 부분 | 일부 / 밝힘 |
| 2 | [Q19](#q19-ask) | 질의응답 | 틀림 / 틀림 | 밝힘 / 안 밝힘 |
| 2 | [Q20](#q20-ask) | 질의응답 | 틀림 / 틀림 | 밝힘 / 안 밝힘 |
| 3 | [Q08](#q08-ask) | 질의응답 | 맞음 / 맞음 | 일부 / 일부 |
| 3 | [Q12](#q12-base) | 기준선 | 맞음 / 맞음 | 밝힘 / 밝힘 |
| 3 | [Q14](#q14-mcp) | MCP | 맞음 / 맞음 | 밝힘 / 밝힘 |
| 3 | [Q08](#q08-mcp) | MCP | 맞음 / 맞음 | 밝힘 / 밝힘 |
| 3 | [Q02](#q02-ask) | 질의응답 | 부분 / 부분 | 밝힘 / 밝힘 |

## 1. 정확도가 갈린 답 (9)

<a id="q04-mcp"></a>

### Q04 MCP

> 운영 RDS와 Redis의 실제 접속 대상과 인증 정보는 어디서 확인하고, 배포된 서비스가 그 대상으로 접속하는지는 어떻게 확인하나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 부분 | 맞음 |
| 확인 범위 | 밝힘 | 밝힘 |
| 이유 | 변수와 Compose 주입 설명 및 제안한 연결 검증 절차는 타당하며, 실제 운영 값·접속 결과와 구분했다. 다만 워크플로에 명시된 배포 대상 EC2와 /home/ubuntu/HotDealWave 경로를 빠뜨려, 어디서 확인할지에 대한 저장소의 구체적인 근거를 충분히 제시하지 못했다. | PROD_DB_*·REDIS_PROD_* 변수와 user/order 서비스의 Compose 주입 줄, ACTIVE_PROFILE 연결이 모두 코드와 맞습니다. 실제 값과 접속 여부는 확인할 수 없으며 검증 절차는 권장안이라고 밝혔습니다. 배포 워크플로(EC2 디렉터리)는 언급하지 않았지만 틀린 주장은 없습니다. |
| 대조한 근거 | `user/src/main/resources/application-prod.yml:3`, `user/src/main/resources/application-prod.yml:15`, `docker-compose.yml:32`, `docker-compose.yml:37` 외 4개 | `user/src/main/resources/application-prod.yml:3-5`, `user/src/main/resources/application-prod.yml:15-18`, `docker-compose.yml:32`, `docker-compose.yml:37-44` 외 3개 |

<details><summary>답 원문</summary>

**저장소에서 확인되는 것은 접속 설정의 변수명과 주입 경로까지입니다. 운영 RDS·Redis의 실제 주소·계정·비밀번호와 현재 접속 여부는 확인할 수 없습니다.** MCP 문서도 아래 변수의 실제 값은 “저장소 밖의 환경변수에서 정해짐”으로 표시합니다. (`user/src/main/resources/application-prod.yml:3`, `user/src/main/resources/application-prod.yml:4`, `user/src/main/resources/application-prod.yml:5`, `user/src/main/resources/application-prod.yml:15`, `user/src/main/resources/application-prod.yml:16`, `user/src/main/resources/application-prod.yml:17`, `user/src/main/resources/application-prod.yml:18`)

| 확인할 내용 | 저장소에서 확인된 설정과 근거 |
|---|---|
| 운영 DB 접속 대상 | `PROD_DB_ENDPOINT` → `spring.datasource.url`. 실제 JDBC URL과 이것이 가리키는 RDS 인스턴스는 확인 불가. (`user/src/main/resources/application-prod.yml:3`) |
| 운영 DB 인증 | `PROD_DB_USERNAME`, `PROD_DB_PASSWORD` → datasource 계정·비밀번호. (`user/src/main/resources/application-prod.yml:4`, `user/src/main/resources/application-prod.yml:5`) |
| 운영 Redis 접속 대상 | `REDIS_PROD_HOST`, `REDIS_PROD_PORT` → Redis 호스트·포트. (`user/src/main/resources/application-prod.yml:15`, `user/src/main/resources/application-prod.yml:16`) |
| 운영 Redis 인증 | `REDIS_PROD_USERNAME`, `REDIS_PROD_PASSWORD` → Redis 사용자명·비밀번호. (`user/src/main/resources/application-prod.yml:17`, `user/src/main/resources/application-prod.yml:18`) |
| 배포 시 주입 지점 | Compose의 user-service 환경변수로 DB와 Redis 설정을 전달하도록 선언되어 있음. 실제 배포에 사용됐는지는 별도 확인 필요. (`docker-compose.yml:37`, `docker-compose.yml:38`, `docker-compose.yml:39`, `docker-compose.yml:41`, `docker-compose.yml:42`, `docker-compose.yml:43`, `docker-compose.yml:44`) |

**실제 값은 운영 배포에서 이 변수들을 공급하는 곳을 추적해야 합니다.** 배포 서버의 환경변수·사용한 env 파일·CI/CD의 비밀 설정·별도 비밀 관리 서비스 등이 확인 후보지만, 어느 곳을 사용하는지는 제공된 저장소 근거로 특정할 수 없습니다. 저장소에서 확인된 연결점은 Compose의 환경변수 선언입니다. (`docker-compose.yml:37`, `docker-compose.yml:38`, `docker-compose.yml:39`, `docker-compose.yml:41`, `docker-compose.yml:42`, `docker-compose.yml:43`, `docker-compose.yml:44`)

배포된 서비스의 접속 여부는 다음 순서로 확인하면 됩니다. **아래는 권장 운영 검증 절차이며, 이번에 실행하거나 확인한 결과는 아닙니다.**

1. **실행 중인 서비스의 활성 프로필을 확인합니다.** `ACTIVE_PROFILE` 주입값과 애플리케이션 시작 로그의 활성 프로필이 `prod`인지 대조합니다. 저장소에서는 `spring.profiles.active`가 이 변수에 연결되고 Compose가 전달하도록 되어 있습니다. (`user/src/main/resources/application.yml:5`, `docker-compose.yml:32`)

2. **실행 중인 컨테이너의 주입값과 애플리케이션의 최종 적용 설정을 대조합니다.** DB URL·사용자명, Redis 호스트·포트·사용자명을 운영에서 의도한 대상과 비교하고, 비밀번호는 출력하지 않고 공급 원본과 적용 여부를 확인합니다. 서비스별로 확인해야 하며, 예를 들어 주문 서비스에도 별도의 DB·Redis 환경변수 전달 선언이 있습니다. (`docker-compose.yml:142`, `docker-compose.yml:143`, `docker-compose.yml:144`, `docker-compose.yml:159`, `docker-compose.yml:160`, `docker-compose.yml:161`, `docker-compose.yml:162`)

3. **애플리케이션이 만든 실제 연결을 대상 서버 측 기록과 대조합니다.** DB는 애플리케이션 연결을 통한 읽기 쿼리와 DB 세션 정보를, Redis는 애플리케이션 클라이언트의 연결 성공과 Redis 측 클라이언트 정보를 확인하는 방식입니다. 별도 CLI 접속 성공만으로 서비스 자체의 접속을 증명하지는 못합니다. 이 검증의 설정 출발점은 datasource와 Redis 속성이며, 실제 운영 연결·세션을 입증하는 자료는 이번 도구 결과에 없습니다. (`user/src/main/resources/application-prod.yml:3`, `user/src/main/resources/application-prod.yml:15`, `user/src/main/resources/application-prod.yml:16`)

</details>

<a id="q06-ask"></a>

### Q06 질의응답

> 쿠폰 발급 요청에 성공을 응답하는 시점은 언제고, 그 뒤 사용자에게 발급된 쿠폰이 저장되기까지 어떤 처리를 거치나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 맞음 | 부분 |
| 확인 범위 | 밝힘 | 일부 |
| 이유 | 조건 검사와 발급 수 증가·쿠폰 저장을 같은 트랜잭션에서 수행한 뒤 201을 반환한다는 핵심 설명이 정확하다. data=null과 201 기록도 확인되며, 응답 기록으로 실제 DB 커밋까지 검증했다고 주장하지 않는다. | 검사, 같은 트랜잭션 안에서의 발급 수 증가와 저장, 처리 뒤 201 응답(data=null)이라는 주장은 코드와 맞습니다. 그러나 발급 흐름의 핵심 단계인 Redisson 락 획득과 해제가 빠졌습니다. '실제 응답 기록: 201'은 파일 근거 없이 적었고, 그 기록이 커밋 시점까지 보장하지는 않는다는 점을 구분하지 않았습니다. |
| 대조한 근거 | `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:26-53`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponService.java:44-52`, `coupon/src/main/java/com/sparta/hotdeal/coupon/presentation/controller/CouponController.java:30-35`, `analysis/hotdealwave/responses/coupon-service.CouponController.issueFirstComeFirstServeCoupon.json:9-12` | `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:26-53`, `coupon/src/main/java/com/sparta/hotdeal/coupon/presentation/controller/CouponController.java:30-35`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponService.java:35-52`, `analysis/hotdealwave/responses/coupon-service.CouponController.issueFirstComeFirstServeCoupon.json:9-12` |

<details><summary>답 원문</summary>

발급 처리에서 쿠폰 정보 상태·잔여 수량·사용자 중복 여부 검사를 통과한 뒤, 발급 수 증가와 미사용 사용자 쿠폰 저장을 같은 트랜잭션에서 처리합니다. 컨트롤러는 이 처리가 끝난 뒤 `201` 응답을 반환하며, 응답 본문은 `data=null`이어서 사용자 쿠폰 ID는 돌려주지 않습니다. 이 흐름은 AI 해석으로 정리된 내용입니다. (근거: `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:31`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:35`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:39`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:44`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:46`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:53`, `coupon/src/main/java/com/sparta/hotdeal/coupon/presentation/controller/CouponController.java:30`, `coupon/src/main/java/com/sparta/hotdeal/coupon/presentation/controller/CouponController.java:34`, `coupon/src/main/java/com/sparta/hotdeal/coupon/presentation/controller/CouponController.java:35`; 실제 응답 기록: 201)

</details>

<a id="q09-ask"></a>

### Q09 질의응답

> 운영 결제에 사용하는 카카오페이 콘솔은 어디서 확인하고, 등록된 도메인과 복귀 관련 주소가 실제 운영 설정과 일치하나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 맞음 | 틀림 |
| 확인 범위 | 밝힘 | 일부 |
| 이유 | 질문의 핵심인 운영 콘솔 위치·등록값·운영 일치 여부에 확인 근거가 없다고 밝혔으며, API 응답 미확인 설명도 응답 파일과 일치합니다. 코드상의 복귀 경로 설명은 없지만, 이를 콘솔 등록값이나 운영 검증 결과로 단정하지 않았습니다. | B는 복귀 주소에 관한 근거가 없다고 했지만, 실제로는 코드에 approval/cancel/fail URL 생성과 PAY_HOST_DOMAIN 주입이 들어 있어 확인할 수 있는 내용을 놓쳤습니다. 콘솔과 운영 값을 확인할 수 없다는 판단은 맞지만, 확인되는 것과 확인되지 않는 것을 구분하지 않았습니다. |
| 대조한 근거 | `payment/src/main/resources/application.yml:26-29`, `docker-compose.yml:185-187`, `analysis/hotdealwave/responses/payment-service.PaymentController.readyPayment.json:7-9`, `analysis/hotdealwave/responses/payment-service.PaymentController.confirmPayment.json:7-9` 외 1개 | `payment/src/main/java/com/sparta/hotdeal/payment/infrastructure/dto/kakaopay/req/ReqPostKakaoPayReadyDto.java:41-43`, `payment/src/main/resources/application.yml:29`, `docker-compose.yml:187` |

<details><summary>답 원문</summary>

조각에는 카카오페이 콘솔의 확인 위치나 등록된 도메인·복귀 주소, 실제 운영 설정과의 일치 여부가 없습니다. 결제 준비·승인·환불 API는 호출 범위 밖이라 응답도 확인되지 않았습니다(C1).

</details>

<a id="q13-mcp"></a>

### Q13 MCP

> 실패한 ECR 업로드 단계는 어떤 방식으로 AWS 인증을 받고, GitHub에 등록된 인증 설정이 워크플로에서 참조하는 값과 맞나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 부분 | 틀림 |
| 확인 범위 | 밝힘 | 일부 |
| 이유 | S3 설정 설명과 GitHub 실제 등록값을 대조할 수 없다는 판단은 맞습니다. 다만 저장소 워크플로에 액세스 키 인증 방식과 Secret 이름이 명시되어 있는데도 이를 확인하지 못해 핵심 답변이 빠졌습니다. | A는 워크플로 근거가 없다고 했지만 저장소에 .github/workflows/actions.yml이 있습니다. 이 파일은 secrets.AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY 액세스 키 방식으로 인증하므로 질문의 핵심에 답하지 못했습니다. GitHub에 등록된 값이 저장소 밖에 있다는 구분은 맞지만, 코드로 확인되는 인증 방식까지 확인 불가로 분류해 구분이 일부만 맞습니다. |
| 대조한 근거 | `docker-compose.yml:97`, `docker-compose.yml:98`, `product/src/main/resources/application.yml:35`, `product/src/main/resources/application.yml:36` 외 3개 | `.github/workflows/actions.yml:19`, `.github/workflows/actions.yml:22`, `.github/workflows/actions.yml:23`, `docker-compose.yml:97` 외 3개 |

<details><summary>답 원문</summary>

**현재 `hotdealwave` MCP가 제공한 근거만으로는 실패한 ECR 업로드의 AWS 인증 방식이나 GitHub 등록 설정과의 일치 여부를 확인할 수 없습니다.**

- **확인된 설정:** product 서비스는 `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`를 환경변수로 전달받으며, 애플리케이션에서는 이를 `s3.access-key`, `s3.secret-key`로 참조합니다. 이는 S3 설정의 근거입니다. (`docker-compose.yml:97`, `docker-compose.yml:98`, `product/src/main/resources/application.yml:35`, `product/src/main/resources/application.yml:36`)
- **ECR 인증 방식은 미확인:** MCP에서 `ECR`, `.github/workflows`, `GitHub Actions`를 검색했지만 관련 문서를 찾지 못했습니다. 따라서 실패한 단계가 액세스 키를 사용하는지, OIDC로 IAM 역할을 받는지, 어떤 Secret 이름을 참조하는지는 판정할 수 없습니다. 해당 워크플로의 `파일:줄` 근거도 확보되지 않았습니다.
- **GitHub 설정과의 일치 여부도 미확인:** 문서는 위 AWS 환경변수의 실제 값이 저장소 밖에서 정해진다고 명시합니다. GitHub에 등록된 Secrets·Variables·Environment 설정은 제시되지 않아 대조할 수 없습니다. (`docker-compose.yml:97`, `docker-compose.yml:98`)

따라서 **S3용 환경변수 이름만 보고 ECR 인증 설정이 맞다거나, 불일치가 실패 원인이라고 결론 내릴 수 없습니다.** 이를 판단하려면 실패한 실행의 워크플로와 로그, GitHub 인증 설정의 이름·적용 범위가 필요합니다.

</details>

<a id="q14-ask"></a>

### Q14 질의응답

> 프로모션 응답의 시간 필드는 어떤 시간대와 시작·종료 기준을 쓰고, 가격 필드는 각각 어떤 금액을 뜻하나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 부분 | 틀림 |
| 확인 범위 | 밝힘 | 일부 |
| 이유 | Timestamp 타입과 시간대 확정의 한계를 짚은 부분은 맞지만, 저장소에서 확인 가능한 시작·종료의 시각 일치 조건과 할인 후 판매가·할인율의 의미까지 확인 불가로 처리해 핵심 답변이 빠졌습니다. 확인하지 못한 사항을 명시했고 이를 확인된 사실로 단정하지는 않았습니다. | 시작·종료 기준(정각 스케줄러의 정확한 시각 일치)과 가격 필드의 의미(할인 후 판매가, 계산된 할인율)는 저장소에서 확인되는데도 '확인할 수 없다'고 답해 질문에 답하지 못했습니다. 시간대를 확정할 수 없다는 점만 맞고, 확인 가능한 것을 확인 불가로 잘못 구분했습니다. |
| 대조한 근거 | `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/promotion/ResGetPromotionDto.java:18`, `product/src/main/java/com/sparta/hotdeal/product/application/scheduler/PromotionScheduler.java:32`, `product/src/main/java/com/sparta/hotdeal/product/application/scheduler/PromotionScheduler.java:50`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/repository/promotion/PromotionRepositoryJpa.java:21` 외 4개 | `product/src/main/java/com/sparta/hotdeal/product/application/scheduler/PromotionScheduler.java:27`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/repository/promotion/PromotionRepositoryJpa.java:21`, `product/src/main/java/com/sparta/hotdeal/product/application/service/promotion/PromotionService.java:154`, `product/src/main/java/com/sparta/hotdeal/product/application/service/promotion/PromotionService.java:162` 외 1개 |

<details><summary>답 원문</summary>

확인할 수 없습니다. 조각에는 프로모션 응답의 start/end가 Timestamp 타입이라는 정보만 있고, 시간대나 시작·종료 기준은 확인되지 않습니다. 응답 DTO의 discountPrice는 할인 가격으로 설명되지만, 가격 필드별 금액 의미를 구분할 충분한 정보는 없습니다. Timestamp의 구조도 조각에서 확인 안 됨으로 표시되어 있습니다.

</details>

<a id="q15-ask"></a>

### Q15 질의응답

> 상품 수정 내용이 MySQL에 저장된 뒤 Elasticsearch에 반영되기까지 어떤 처리를 거치고, 상세 조회와 검색 조회는 각각 어디서 데이터를 읽나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 맞음 | 부분 |
| 확인 범위 | 밝힘 | 일부 |
| 이유 | 직접 색인 호출, 예외를 삼켜 수정 성공 응답이 가능하다는 설명, 상세와 검색의 데이터 원천이 모두 코드와 일치한다. 커밋 이후 실행이나 외부 동기화 체계를 단정하지 않으며, ‘AI 해석’이라는 표현 자체는 감점 근거가 아니다. | indexProduct 호출, 예외를 삼키는 처리, 상세 조회는 JPA·목록 조회는 ES라는 핵심은 코드와 맞습니다. 하지만 색인이 트랜잭션 안에서 커밋 전에 실행된다는 점과 문서 변환·저장 과정이 빠져 질문 전제를 바로잡지 못했습니다. 또 코드로 확인되는 내용까지 모두 'AI 해석'이라고만 표시해, 확인된 것과 확인되지 않는 것을 구분하지 않았습니다. |
| 대조한 근거 | `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:122`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:124`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductIndexService.java:24`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:166` 외 3개 | `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:44`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:122`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:136`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:166` 외 3개 |

<details><summary>답 원문</summary>

상품 수정 후에는 서비스가 Elasticsearch 색인 서비스 `indexProduct`를 호출해 변경 내용을 반영합니다. 다만 색인 과정의 예외는 로그만 남기고 삼키는 것으로 분석되어 있어, 색인 실패에도 수정 성공 응답이 반환될 수 있습니다. 이 설명은 조각의 **AI 해석**입니다. (product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:122, product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:136, product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductIndexService.java:19, product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductIndexService.java:24)

상세 조회는 JPA의 상품 데이터를 읽고, 검색·목록 조회는 Elasticsearch의 상품 문서를 읽습니다. 따라서 색인 실패나 재색인 전에는 두 조회 결과가 다를 수 있습니다. 이 데이터 경로 설명도 **AI 해석**입니다. (product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:166, product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:179, product/src/main/java/com/sparta/hotdeal/product/infrastructure/repository/product/ProductSearchRepositoryCustomImpl.java:33, product/src/main/java/com/sparta/hotdeal/product/infrastructure/repository/product/ProductSearchRepositoryCustomImpl.java:40)

</details>

<a id="q16-ask"></a>

### Q16 질의응답

> Elasticsearch 반영에 실패하면 어떻게 재시도하고, 누락되거나 순서가 뒤바뀐 변경 사항은 어떻게 맞추나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 맞음 | 부분 |
| 확인 범위 | 밝힘 | 밝힘 |
| 이유 | 재시도와 누락·역순 변경 복구 절차가 확인되지 않는다는 핵심 결론 및 색인 실패 후에도 성공 응답을 만들 수 있다는 설명이 코드와 일치한다. 주장을 인용된 생성·수정·상태 변경 경로로 제한하고, 복구 동작을 추정하지 않았다. | indexProduct가 예외를 삼켜 재시도가 없다는 핵심은 맞고, 틀린 주장은 없다. 다만 아래 내용은 다루지 않았다. 누락의 실제 원인인 재고·할인 변경 경로의 색인 미호출, 순서 역전을 막는 장치가 없다는 점, 삭제 경로의 차이. 또 저장소 전체가 아니라 '조각' 범위로만 한정해서 답했다. |
| 대조한 근거 | `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductIndexService.java:19`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductIndexService.java:24`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:83`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:85` 외 2개 | `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductIndexService.java:19-26`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:83,122,136,246-251`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductInventoryService.java:99-107` |

<details><summary>답 원문</summary>

조각에는 Elasticsearch 반영 실패에 대한 재시도 방식이나, 누락·순서가 뒤바뀐 변경 사항을 맞추는 절차가 확인되어 있지 않습니다. 확인되는 것은 상품 생성·수정·상태 변경 중 색인 실패가 발생하면 `indexProduct`가 예외를 로그만 남기고 삼켜 호출자가 성공 응답을 만들 수 있다는 점뿐입니다(AI 해석). 따라서 이 근거만으로 재시도나 정합성 복구가 수행된다고 말할 수 없습니다. 근거: `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductIndexService.java:19`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductIndexService.java:24`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:83`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:85`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:122`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:136`

</details>

<a id="q21-mcp"></a>

### Q21 MCP

> Grafana에 실제 연결된 데이터 소스와 대시보드 조회 조건은 어디서 관리하고, 누락된 서비스의 지표와 로그가 저장된 대상을 보고 있나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 맞음 | 부분 |
| 확인 범위 | 밝힘 | 밝힘 |
| 이유 | 실제 Grafana 연결과 대시보드 조건을 확인할 수 없다는 결론 및 애플리케이션 DB·Redis 설정이 그 연결의 증거가 아니라는 설명이 타당하다. 검색 결과 부재를 저장소 설정 부재로 단정하지 않고, 외부 환경변수와 확인되지 않은 연결을 구분했다. | Grafana 데이터 소스와 대시보드 근거가 저장소에 없으므로 확인 불가라는 판단과, DB·Redis 설정이 Grafana 연결이 아니라는 지적은 맞습니다. 인용한 application-prod.yml:3,15도 정확합니다. 다만 저장소에 있는 Prometheus 수집 설정과 Loki 전송·저장 설정을 찾지 못해 핵심 근거가 빠졌습니다. 검색 결과가 없다는 것이 부재의 증명이 아니라고 스스로 한정해 확인 범위는 구분했습니다. |
| 대조한 근거 | `user/src/main/resources/application-prod.yml:3`, `user/src/main/resources/application-prod.yml:15`, `monitoring/prometheus/prometheus.yml:4`, `user/src/main/resources/logback.xml:4` 외 2개 | `user/src/main/resources/application-prod.yml:3`, `user/src/main/resources/application-prod.yml:15`, `monitoring/prometheus/prometheus.yml:4-38`, `monitoring/loki/loki-config.yml:12` 외 1개 |

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

<a id="q21-ask"></a>

### Q21 질의응답

> Grafana에 실제 연결된 데이터 소스와 대시보드 조회 조건은 어디서 관리하고, 누락된 서비스의 지표와 로그가 저장된 대상을 보고 있나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 맞음 | 부분 |
| 확인 범위 | 밝힘 | 일부 |
| 이유 | 질문의 핵심인 실제 Grafana 관리 위치와 저장 대상 조회 여부는 저장소에서 확인되지 않으므로 확인 불가라는 결론은 맞다. 제공된 조각으로 판단 범위를 한정했으며, 운영 연결을 단정하지 않았다. | Grafana 연결과 대시보드 조건을 확인할 수 없다는 결론 자체는 맞습니다. 하지만 저장소에 있는 지표·로그 저장 대상 설정(prometheus.yml, loki-config.yml, logback의 Loki URL)을 '내용이 없다'고 해 핵심을 놓쳤습니다. 무엇이 확인되고 무엇이 저장소 밖 값인지도 구분하지 않았습니다. |
| 대조한 근거 | `docker-compose.yml:3`, `docker-compose.yml:203`, `.github/workflows/actions.yml:95`, `monitoring/prometheus/prometheus.yml:4` 외 1개 | `monitoring/prometheus/prometheus.yml:4-38`, `monitoring/loki/loki-config.yml:10-13`, `order/src/main/resources/logback.xml:4-8` |

<details><summary>답 원문</summary>

확인할 수 없습니다. 제공된 조각에는 Grafana 데이터 소스·대시보드 조회 조건의 관리 위치나, 누락된 서비스의 지표·로그가 저장된 대상에 관한 내용이 없습니다.

</details>

## 2. 확인 범위만 갈린 답 (10)

<a id="q03-ask"></a>

### Q03 질의응답

> 저장소 설정에서 운영 환경에 별도로 주입해야 하는 값은 무엇이고, 실제 배포에는 어디서 주입하나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 부분 | 부분 |
| 확인 범위 | 밝힘 | 일부 |
| 이유 | 외부 공급이 필요한 설정의 주요 범주는 맞고 실제 값이 미확인임도 밝혔다. 다만 필요한 변수들을 충분히 특정하지 않았고, Compose의 컨테이너 환경변수 전달과 워크플로의 Secrets·EC2 실행 경로를 설명하지 않아 배포 주입 위치에 대한 답이 불완전하다. | C가 인용한 줄(DB, Kafka, ES, Redis, ACTIVE_PROFILE)은 코드와 맞지만, 변수를 예시 수준으로만 뭉뚱그려 나열했습니다. 배포 워크플로(GitHub Secrets, EC2에서 docker-compose 실행)는 전혀 다루지 않았습니다. 주입 위치를 통째로 '확인할 수 없음'으로 처리해, Compose environment까지는 확인되고 EC2의 값 공급원은 확인되지 않는다는 구분을 하지 못했습니다. |
| 대조한 근거 | `user/src/main/resources/application-prod.yml:3`, `order/src/main/resources/application-prod.yml:22`, `product/src/main/resources/application-prod.yml:21`, `docker-compose.yml:32` 외 5개 | `docker-compose.yml:32`, `docker-compose.yml:37`, `user/src/main/resources/application-prod.yml:3`, `payment/src/main/resources/application-prod.yml:15` 외 4개 |

<details><summary>답 원문</summary>

운영 환경에 필요한 값은 서비스별 `prod` 설정 및 Compose 환경변수에 연결된 항목입니다. 예를 들어 DB 접속정보(`PROD_DB_ENDPOINT`, `PROD_DB_USERNAME`, `PROD_DB_PASSWORD`), 각 서비스에 해당하는 Redis·Kafka·Elasticsearch 정보와 토픽, 그리고 메일·JWT·결제·관리자·AWS 설정 등이 있습니다. 조각에 기록된 실제 값은 모두 “저장소에 근거 없음”이며, 실제 배포에서 값이 어디서 주입되는지는 확인할 수 없습니다. user-service 항목은 값이 저장소 밖 환경변수에서 정해진다고 명시되어 있지만, 그 환경변수를 배포 단계에서 제공하는 구체적인 위치나 시스템은 조각에 없습니다. [docker-compose.yml:32] [docker-compose.yml:37] [user/src/main/resources/application-prod.yml:3] [payment/src/main/resources/application-prod.yml:15] [order/src/main/resources/application-prod.yml:22] [product/src/main/resources/application-prod.yml:21]

</details>

<a id="q04-ask"></a>

### Q04 질의응답

> 운영 RDS와 Redis의 실제 접속 대상과 인증 정보는 어디서 확인하고, 배포된 서비스가 그 대상으로 접속하는지는 어떻게 확인하나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 틀림 | 틀림 |
| 확인 범위 | 일부 | 안 밝힘 |
| 이유 | 실제 운영 값과 연결 결과가 확인되지 않는다는 한계는 맞지만, 확인 위치나 절차를 제시할 근거도 없다는 결론은 잘못이다. 저장소에는 환경변수 주입, EC2 배포 경로, health 설정이 있다. | 저장소에는 운영 DB·Redis 접속 변수(application-prod.yml), Compose 주입 경로, 배포 워크플로가 있습니다. 그런데도 A는 근거가 없다며 질문에 답하지 않았습니다. 코드로 확인되는 것과 저장소 밖 값을 구분하지 않았습니다. |
| 대조한 근거 | `user/src/main/resources/application-prod.yml:3`, `docker-compose.yml:37`, `.github/workflows/actions.yml:87`, `.github/workflows/actions.yml:95` 외 1개 | `user/src/main/resources/application-prod.yml:3`, `user/src/main/resources/application-prod.yml:15`, `docker-compose.yml:37`, `docker-compose.yml:41` 외 1개 |

<details><summary>답 원문</summary>

조각에는 운영 RDS·Redis의 실제 접속 주소나 인증 정보가 어디에 설정되어 있는지, 배포 서비스가 그 대상으로 연결되는지 확인하는 방법이 없습니다. 따라서 확인 위치나 절차를 이 근거만으로 답할 수 없습니다.

</details>

<a id="q05-ask"></a>

### Q05 질의응답

> 상품 목록 응답에는 어떤 상품 정보와 페이지 정보가 들어가고, 검색 결과가 없을 때는 어떤 형식으로 내려오나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 부분 | 부분 |
| 확인 범위 | 밝힘 | 일부 |
| 이유 | 응답 래퍼와 상품 필드는 맞지만, VIA_DTO 설정으로 설명할 수 있는 페이지 구조와 코드에서 확인되는 빈 목록·total 0 반환을 누락했다. 확인하지 못한 범위는 명시했으나, 근거가 없다는 판단은 실제 저장소에 비해 과도하다. | 응답 래퍼와 상품 필드 13개는 맞습니다. 그러나 저장소에 있는 VIA_DTO 페이지 직렬화 설정을 놓쳤고, 결과가 없을 때 빈 PageImpl을 반환하는 코드가 있는데도 '근거가 없다'고 했습니다. 코드로 확인되는 것을 확인할 수 없는 것으로 잘못 나눴고, 근거 파일:줄도 제시하지 않았습니다. |
| 대조한 근거 | `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/ResponseDto.java:9`, `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/product/ResGetProductDto.java:17`, `product/src/main/java/com/sparta/hotdeal/product/ProductApplication.java:13`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:183` 외 2개 | `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/ResponseDto.java:9`, `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/product/ResGetProductDto.java:17`, `product/src/main/java/com/sparta/hotdeal/product/ProductApplication.java:13`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java:183` 외 1개 |

<details><summary>답 원문</summary>

상품 목록 응답은 `ResponseDto<Page<ResGetProductDto>>` 형태로, `message`와 `data`를 포함합니다. 각 상품에는 `productId`, `name`, `price`, `quantity`, `category`, `companyId`, `description`, `detailImgs`, `thumbImg`, `status`, `rating`, `reviewCnt`, `discountPrice`가 확인됩니다. `data`가 페이지 타입이라는 점은 확인되지만, 외부 `Page` 타입의 페이지 메타데이터 구조는 확인 안 됨입니다. 검색 결과가 없을 때의 응답 형식이나 내용도 조각에 근거가 없어 확인할 수 없습니다.

</details>

<a id="q07-ask"></a>

### Q07 질의응답

> 보유 쿠폰 목록은 어떤 데이터를 기준으로 조회하고, 발급 성공 후 저장이나 조회 반영에 실패하면 어떻게 처리하나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 부분 | 부분 |
| 확인 범위 | 밝힘 | 일부 |
| 이유 | 조회 조건과 표시 데이터는 맞지만, 코드로 확인되는 성공 응답 전 동기 저장·트랜잭션 롤백 구조·일반 런타임 예외의 500 처리를 설명하지 않아 실패 처리라는 핵심에 불완전하게 답했습니다. 확인하지 못한 동작을 단정하지 않고 한계를 명시했습니다. | 조회 조건과 CouponInfo에서 읽는 표시 필드 설명은 맞습니다. 하지만 질문 후반부인 실패 처리는 '확인할 수 없다'고만 했습니다. 실제로는 CouponIssueHelper.java:26의 @Transactional로 수량 증가와 저장이 한 트랜잭션이고, 이 처리가 끝나야 201을 반환하며, RuntimeException은 500으로 처리된다는 점이 코드로 확인됩니다. 코드로 확인되는 것까지 확인 불가로 분류한 셈입니다. |
| 대조한 근거 | `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponService.java:124`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/mapper/CouponMapper.java:16`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:26`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:53` 외 2개 | `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponService.java:123-126`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/mapper/CouponMapper.java:16-19`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:26`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:44-53` 외 2개 |

<details><summary>답 원문</summary>

보유 쿠폰 목록은 요청 `userId`, 삭제되지 않은 상태(`deletedAt IS NULL`), `isUsed` 값으로 조회합니다. `isUsed`를 생략하면 미사용(`false`) 쿠폰만 조회하며, 만료일이나 쿠폰 정보 상태는 목록 조회의 제외 조건이 아닙니다. 목록에 표시되는 이름·할인액·최소 주문액·만료일은 연결된 현재 `CouponInfo`에서 읽습니다. [coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponService.java:123, coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponService.java:124, coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponService.java:126, coupon/src/main/java/com/sparta/hotdeal/coupon/application/mapper/CouponMapper.java:12, coupon/src/main/java/com/sparta/hotdeal/coupon/application/mapper/CouponMapper.java:16, coupon/src/main/java/com/sparta/hotdeal/coupon/application/mapper/CouponMapper.java:17, coupon/src/main/java/com/sparta/hotdeal/coupon/application/mapper/CouponMapper.java:18, coupon/src/main/java/com/sparta/hotdeal/coupon/application/mapper/CouponMapper.java:19]

발급 성공 후 저장이나 조회 반영에 실패했을 때의 재시도·롤백·오류 처리 방식은 제공된 조각에서 확인할 수 없습니다. 다만 조각은 발급 처리에서 별도 사용자 쿠폰을 저장하고 컨트롤러가 `data=null`인 201 응답을 반환한다고 설명하지만, 그 뒤 저장 또는 조회 반영 실패 시의 동작은 제시하지 않습니다. [coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:46, coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:53, coupon/src/main/java/com/sparta/hotdeal/coupon/presentation/controller/CouponController.java:30, coupon/src/main/java/com/sparta/hotdeal/coupon/presentation/controller/CouponController.java:34, coupon/src/main/java/com/sparta/hotdeal/coupon/presentation/controller/CouponController.java:35]

</details>

<a id="q12-ask"></a>

### Q12 질의응답

> 결제 결과 전달이나 Order의 상태 변경에 실패하면 어디에서 재시도하고, 같은 결과가 여러 번 전달되면 어떻게 처리하나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 부분 | 부분 |
| 확인 범위 | 밝힘 | 일부 |
| 이유 | 직접 DLQ 발행과 비동기 완료 미확인 설명은 맞습니다. 다만 저장소에서 확인되는 컨테이너 재시도 0회 설정과 중복 메시지의 무조건 상태 재대입을 확인 불가로 남겨 핵심 답변이 빠졌습니다. | 리스너가 예외를 잡아 DLQ로 보내고, 그 전송 결과를 확인하지 않으며, 상태를 이전 상태와 상관없이 덮어쓴다는 점은 코드와 맞습니다. 하지만 컨테이너의 FixedBackOff(0L, 0)라는 재시도 0회 정책을 빠뜨렸습니다. 중복 처리가 없다는 점도 코드로 확인되는데 '확인할 수 없다'고 해서, 확인 가능한 사실을 미확인으로 잘못 나눴습니다. |
| 대조한 근거 | `order/src/main/java/com/sparta/hotdeal/order/event/consumer/OrderStatusUpdateEventConsumer.java:48`, `order/src/main/java/com/sparta/hotdeal/order/event/producer/OrderDlqProducer.java:31`, `order/src/main/java/com/sparta/hotdeal/order/infrastructure/config/ConsumerApplicationKafkaConfig.java:67`, `order/src/main/java/com/sparta/hotdeal/order/event/service/OrderConsumerService.java:30` 외 1개 | `order/src/main/java/com/sparta/hotdeal/order/event/consumer/OrderStatusUpdateEventConsumer.java:32`, `order/src/main/java/com/sparta/hotdeal/order/event/producer/OrderDlqProducer.java:31`, `order/src/main/java/com/sparta/hotdeal/order/infrastructure/config/ConsumerApplicationKafkaConfig.java:67`, `order/src/main/java/com/sparta/hotdeal/order/event/service/OrderConsumerService.java:30` 외 2개 |

<details><summary>답 원문</summary>

조각에는 결제 결과 전달이나 주문 상태 변경 실패 시 재시도하는 위치·정책이 확인되지 않습니다. 확인되는 범위에서는 상태 갱신 리스너가 처리 예외를 직접 DLQ 발행 경로로 넘기지만, DLQ 전송 완료도 확인하지 않아 재시도나 전달 성공을 보장한다고 볼 수 없습니다. 또한 주문 상태를 메시지의 enum 값으로 덮어쓰며 이전 상태별 전이 제한이 없다고 AI 해석되어 있습니다. 같은 결과가 여러 번 전달될 때의 중복 처리·멱등성 동작은 조각에서 확인할 수 없습니다.

근거: `payment/src/main/java/com/sparta/hotdeal/payment/event/producer/PaymentEventProducer.java:52`, `order/src/main/java/com/sparta/hotdeal/order/event/consumer/OrderStatusUpdateEventConsumer.java:48`, `order/src/main/java/com/sparta/hotdeal/order/event/producer/OrderDlqProducer.java:31`, `order/src/main/java/com/sparta/hotdeal/order/event/service/OrderConsumerService.java:26`, `order/src/main/java/com/sparta/hotdeal/order/domain/entity/order/OrderStatus.java:9`.

</details>

<a id="q17-ask"></a>

### Q17 질의응답

> 외부 메일 발송 계정의 인증 상태와 발송 제한, 수신 거부나 발송 실패 기록은 어디서 확인할 수 있나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 부분 | 부분 |
| 확인 범위 | 밝힘 | 일부 |
| 이유 | 외부 계정 상태·한도·수신 거부 이력을 확인할 근거가 없다는 설명은 맞습니다. 다만 저장소에는 실패 조사에 활용할 Loki 로그 전송 경로와 서비스 라벨이 있어, 확인 경로를 전혀 제시하지 않은 답은 불완전합니다. | A의 핵심 결론은 틀리지 않았습니다. 외부 계정의 인증 상태, 발송 한도, 수신 거부 기록은 저장소에서 확인되지 않고, 비동기 발송이 실패해도 요청 응답에는 드러나지 않습니다. 다만 저장소에서 확인되는 것을 하나도 짚지 않았습니다. 빠진 것은 Gmail SMTP 설정, 환경변수로 넣는 계정 정보, Loki로 보내는 로그 설정입니다. 근거 위치도 하나도 대지 않았고, '조각'이라는 표현에 기대어 확인된 것과 확인되지 않은 것을 나누지 못했습니다. |
| 대조한 근거 | `user/src/main/resources/application.yml:10`, `user/src/main/resources/logback.xml:4`, `user/src/main/resources/logback.xml:8`, `user/src/main/java/com/sparta/hotdeal/user/infrastructure/service/MailServiceImpl.java:17` | `user/src/main/resources/application.yml:8`, `user/src/main/resources/logback.xml:4`, `user/src/main/java/com/sparta/hotdeal/user/infrastructure/service/MailServiceImpl.java:17`, `user/src/main/java/com/sparta/hotdeal/user/presentation/controller/AuthController.java:53` |

<details><summary>답 원문</summary>

조각에는 외부 메일 발송 계정의 인증 상태, 발송 제한, 수신 거부 또는 발송 실패 기록의 확인 위치가 나와 있지 않습니다. 비동기 SMTP 발송 실패가 요청 성공 응답에 반영되지 않을 수 있다는 AI 해석은 있으나, 실패 기록이나 외부 계정 상태를 확인할 경로는 확인할 수 없습니다.

</details>

<a id="q18-mcp"></a>

### Q18 MCP

> 쿠폰 소진이나 중복 발급, 발급 기간 종료로 발급이 실패하면 각각 어떤 상태 코드와 오류 본문이 내려오나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 부분 | 부분 |
| 확인 범위 | 일부 | 밝힘 |
| 이유 | 소진 상태에 따른 CO004·CO005 구분, 중복 발급 CO006, 발급 경로에 만료일 검사가 없다는 설명은 맞습니다. 그러나 코드로 확인되는 오류 본문의 timestamp·code·message와 정확한 메시지를 확인 불가로 처리했고, 만료 후 소프트 삭제된 경우의 404/CO001도 빠졌습니다. | A가 말한 400/CO004·CO005·CO006 분기, OUT_OF_STOCK 전환 뒤 CO004가 나오는 점, 발급 경로에 만료 검사가 없고 CO008은 validateCoupon에서만 나온다는 점은 코드와 맞습니다. 하지만 오류 본문은 ErrorResponseDto(timestamp, code, message)와 ErrorCode의 메시지로 코드에서 확정되는데도 '확인 불가'라고 했습니다. 만료 후 스케줄러가 소프트 삭제하면 404/CO001이 나오는 경로도 빠졌습니다. 다만 확인되지 않은 것을 확인된 것처럼 말하지는 않았습니다. |
| 대조한 근거 | `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:29-41`, `coupon/src/main/java/com/sparta/hotdeal/coupon/domain/entity/CouponInfo.java:75-76`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/dto/ErrorResponseDto.java:14-19`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/exception/GlobalExceptionHandler.java:19-24` 외 3개 | `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:29-41`, `coupon/src/main/java/com/sparta/hotdeal/coupon/domain/entity/CouponInfo.java:75-76`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/exception/ErrorCode.java:9-16`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/exception/GlobalExceptionHandler.java:19-24` 외 4개 |

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

오류 본문의 **필드명·정확한 메시지·전체 JSON은 확인 불가**입니다. 제공된 발급 응답 기록은 성공 **201**이며, 위 실패들의 실제 응답 본문을 입증하지 않습니다. 따라서 CO004·CO005·CO006을 임의로 `{"code": "...", "message": "..."}` 형태로 제시할 수는 없습니다. 근거: `analysis/hotdealwave/responses/coupon-service.CouponController.issueFirstComeFirstServeCoupon.json:6`, `analysis/hotdealwave/responses/coupon-service.CouponController.issueFirstComeFirstServeCoupon.json:9`, `analysis/hotdealwave/responses/coupon-service.CouponController.issueFirstComeFirstServeCoupon.json:12`.

</details>

<a id="q18-ask"></a>

### Q18 질의응답

> 쿠폰 소진이나 중복 발급, 발급 기간 종료로 발급이 실패하면 각각 어떤 상태 코드와 오류 본문이 내려오나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 부분 | 부분 |
| 확인 범위 | 일부 | 밝힘 |
| 이유 | 수량 검사에서의 400/CO005와 중복 검사에서의 400/CO006, 발급 경로의 만료일 검사 부재는 맞지만, 정상 소진 후에는 앞선 상태 검사로 400/CO004가 반환되는 점을 누락했습니다. 만료 관련 불확실성은 밝혔으나 코드로 확인 가능한 오류 본문을 확인 불가로 처리했고, 삭제 후 404/CO001 분기도 설명하지 않았습니다. | B가 말한 중복 발급 400/CO006과 발급 경로에 만료 검사가 없다는 점은 맞습니다. 하지만 소진을 CO005 하나로 단정해, 마지막 발급으로 상태가 OUT_OF_STOCK이 된 뒤에는 CO004가 나오는 경로를 놓쳤습니다. 코드로 확정되는 오류 본문(timestamp/code/message)과 소프트 삭제 뒤 404/CO001도 제시하지 못했습니다. 확인하지 못한 부분은 확인 불가나 해석이라고 표시했습니다. |
| 대조한 근거 | `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:27-53`, `coupon/src/main/java/com/sparta/hotdeal/coupon/domain/entity/CouponInfo.java:75-76`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/exception/ErrorCode.java:9-14`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/dto/ErrorResponseDto.java:14-19` 외 3개 | `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:31-41`, `coupon/src/main/java/com/sparta/hotdeal/coupon/domain/entity/CouponInfo.java:75-76`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/exception/ErrorCode.java:12-14`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/dto/ErrorResponseDto.java:14-19` 외 2개 |

<details><summary>답 원문</summary>

쿠폰 소진은 HTTP 400/CO005, 중복 발급은 HTTP 400/CO006으로 처리됩니다. 다만 발급 기간 종료에 대한 발급 경로의 상태 코드와 오류 본문은 조각에서 확인할 수 없습니다. AI 해석으로는 발급 경로가 expirationDate를 검사하지 않아, 만료됐더라도 아직 삭제되지 않은 쿠폰 정보가 발급 조건을 통과할 수 있습니다.

오류 본문의 구체적인 필드·메시지 형식은 조각에 제시되어 있지 않습니다. 발급 결과로 확인된 것은 201 응답뿐이며, 위 오류 본문 내용은 확인할 수 없습니다. 근거: `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:35`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:39`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/exception/GlobalExceptionHandler.java:19`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/exception/GlobalExceptionHandler.java:23`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/exception/GlobalExceptionHandler.java:24`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponService.java:63`, `coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java:44`, `analysis/hotdealwave/responses/coupon-service.CouponController.issueFirstComeFirstServeCoupon.json:6`.

</details>

<a id="q19-ask"></a>

### Q19 질의응답

> Gateway로 들어온 요청은 어떤 라우팅·인증 처리를 거쳐 대상 서비스로 전달되고, 그 과정에서 경로나 헤더가 어떻게 바뀌나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 틀림 | 틀림 |
| 확인 범위 | 밝힘 | 안 밝힘 |
| 이유 | 확인할 수 없다는 한계는 밝혔지만, 저장소에는 라우팅·경로 재작성·JWT 검증·헤더 변경 코드가 있어 질문의 핵심에 답하지 못했습니다. | 저장소에는 Gateway 라우트, RewritePath, AuthenticationFilter·AllowedPath·JwtUtil 등 질문에 직접 답할 근거가 있습니다. 그런데도 A는 확인할 수 없다고만 답해 질문에 답하지 못했습니다. 코드로 확인되는 것과 확인되지 않는 것을 나누지도 않았습니다. |
| 대조한 근거 | `gateway/src/main/resources/application.yml:17`, `gateway/src/main/resources/application.yml:19`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:24`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:30` 외 1개 | `gateway/src/main/resources/application.yml:13-54`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:20-47`, `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AllowedPath.java:5-30` |

<details><summary>답 원문</summary>

확인할 수 없습니다. 제공된 조각에는 gateway-service의 디렉터리와 Compose 서비스 정보만 있고, 요청의 라우팅·인증 처리나 경로·헤더 변경에 관한 내용은 없습니다.

</details>

<a id="q20-ask"></a>

### Q20 질의응답

> Gateway 자체에서 요청을 거부한 경우와 대상 서비스에서 실패한 경우를 어떻게 구분하고, 각 오류 응답은 어떤 처리를 거쳐 클라이언트에 반환되나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 틀림 | 틀림 |
| 확인 범위 | 밝힘 | 안 밝힘 |
| 이유 | 확인하지 못한 흐름을 단정하지는 않았습니다. 그러나 저장소에는 Gateway의 직접 거부와 서비스 예외 응답 생성 코드가 있으므로, 환경변수 설정만 있다는 설명으로는 질문의 핵심에 답하지 못했습니다. | 저장소에는 Gateway 인증 필터(401 즉시 종료)와 서비스별 예외 처리기가 있는데도 확인할 수 없다고만 답해 질문에 답하지 못했습니다. 저장소에 근거가 있는 내용을 근거가 없다고 한 셈이고, 확인 가능한 것과 불가능한 것을 구분하지도 않았습니다. |
| 대조한 근거 | `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:24-46`, `gateway/src/main/resources/application.yml:13-19`, `order/src/main/java/com/sparta/hotdeal/order/common/exception/GlobalExceptionHandler.java:24-35` | `gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java:30`, `order/src/main/java/com/sparta/hotdeal/order/common/exception/GlobalExceptionHandler.java:24`, `user/src/main/java/com/sparta/hotdeal/user/application/exception/GlobalExceptionHandler.java:14` |

<details><summary>답 원문</summary>

제공된 조각은 Gateway의 환경변수 설정만 다루며, Gateway 자체 거부와 대상 서비스 실패의 구분 기준이나 오류 응답 처리·반환 흐름은 확인할 수 없습니다. [C1]

</details>

## 3. 두 채점이 같은 답 가운데 무작위 표본(시드 45) (5)

<a id="q08-ask"></a>

### Q08 질의응답

> 카카오페이 결제를 마친 뒤 어떤 주소로 돌아와서 어떤 처리를 거쳐 운영 화면으로 이동하나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 맞음 | 맞음 |
| 확인 범위 | 일부 | 일부 |
| 이유 | 복귀 경로, 안내 화면과 별도 승인 API의 구분, 상태 변경과 이벤트 발행이라는 핵심은 코드와 일치한다. 운영 화면 연결의 부재는 밝혔지만, 복귀 주소에 붙는 PAY_HOST_DOMAIN과 실제 운영 도메인이 외부 설정이라는 점은 구분하지 않았다. | /payment/success 복귀, 템플릿만 반환, 별도 confirm에서 COMPLETE 처리와 이벤트 발행, 운영 화면 이동은 근거가 없다는 주장은 코드와 맞지만 파일:줄 근거가 없습니다. 미확인 범위를 저장소가 아니라 '조각'으로 한정했고, 도메인이 환경변수라는 점도 밝히지 않았습니다. 또 코드로 확인되는 화면과 승인의 구분을 'AI 해석'으로 분류했습니다. |
| 대조한 근거 | `payment/src/main/java/com/sparta/hotdeal/payment/infrastructure/dto/kakaopay/req/ReqPostKakaoPayReadyDto.java:41`, `payment/src/main/resources/application.yml:29`, `payment/src/main/java/com/sparta/hotdeal/payment/presentation/controller/PaymentViewController.java:18-20`, `payment/src/main/resources/templates/payment-success.html:1-12` 외 3개 | `payment/src/main/java/com/sparta/hotdeal/payment/infrastructure/dto/kakaopay/req/ReqPostKakaoPayReadyDto.java:41`, `payment/src/main/java/com/sparta/hotdeal/payment/presentation/controller/PaymentViewController.java:18-21`, `payment/src/main/java/com/sparta/hotdeal/payment/presentation/controller/PaymentController.java:44-48`, `payment/src/main/java/com/sparta/hotdeal/payment/application/service/PaymentService.java:58-67` 외 1개 |

<details><summary>답 원문</summary>

준비 요청에서 카카오페이에 설정하는 복귀 주소는 `/payment/success`입니다. 다만 확인된 성공 화면 처리기는 템플릿만 반환하며 승인이나 상태 변경을 하지 않습니다. 실제 승인은 별도 `POST /api/v1/payments/confirm`에 `tid`와 `pgToken`을 보내 처리하고, 정상 흐름에서는 결제 상태를 COMPLETE로 바꾼 뒤 주문 상태 업데이트 이벤트를 발행합니다. 이 후 운영 화면으로 이동하는 주소나 화면 전환 처리는 조각에 근거가 없어 확인할 수 없습니다. 성공 화면 진입만으로 승인 완료를 판단할 수 없습니다. 위 설명 중 화면과 승인 동작의 구분은 AI 해석입니다.

</details>

<a id="q12-base"></a>

### Q12 기준선

> 결제 결과 전달이나 Order의 상태 변경에 실패하면 어디에서 재시도하고, 같은 결과가 여러 번 전달되면 어떻게 처리하나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 맞음 | 맞음 |
| 확인 범위 | 밝힘 | 밝힘 |
| 이유 | 애플리케이션 재발행 부재, 재시도 0회의 DLQ 경로, 자동 재처리 부재와 중복 상태 덮어쓰기 설명이 코드와 일치합니다. Kafka 내부 재시도·운영 재처리·오프셋에 따른 재전달 및 트랜잭션 확정 여부도 구분했습니다. | 다음 주장이 코드와 맞습니다: orderId를 키로 발행함, 실패 콜백과 producer retries 설정이 없음, CANCEL 보상은 RuntimeException으로 롤백됨, DLQ 전송과 FixedBackOff(0L, 0), OrderDlqConsumer에 @Component가 없음, 메시지에 orderId와 status만 있고 this.status = status로 덮어씀, MANUAL ACK인데 ack 호출이 없음. Kafka 클라이언트 내부 재시도와 운영자의 외부 재처리, 실제 재전달 여부는 저장소만으로 확인할 수 없다고 구분했습니다. |
| 대조한 근거 | `payment/src/main/java/com/sparta/hotdeal/payment/application/service/PaymentService.java:64`, `payment/src/main/java/com/sparta/hotdeal/payment/event/producer/PaymentEventProducer.java:38`, `payment/src/main/java/com/sparta/hotdeal/payment/event/producer/PaymentEventProducer.java:52`, `payment/src/main/java/com/sparta/hotdeal/payment/infrastructure/config/ProducerApplicationKafkaConfig.java:22` 외 8개 | `payment/src/main/java/com/sparta/hotdeal/payment/event/producer/PaymentEventProducer.java:38`, `payment/src/main/java/com/sparta/hotdeal/payment/event/producer/PaymentEventProducer.java:52`, `payment/src/main/java/com/sparta/hotdeal/payment/infrastructure/config/ProducerApplicationKafkaConfig.java:22`, `payment/src/main/java/com/sparta/hotdeal/payment/event/service/PaymentProducerService.java:28` 외 6개 |

<details><summary>답 원문</summary>

**현재 구현은 Order 처리 실패를 재시도 없이 DLQ로 보내며, 중복 결과는 걸러내지 않고 상태를 다시 대입합니다.** 근거: `order/src/main/java/com/sparta/hotdeal/order/infrastructure/config/ConsumerApplicationKafkaConfig.java:67`, `order/src/main/java/com/sparta/hotdeal/order/domain/entity/order/Order.java:70`.

1. **결제 결과 전달 경로**  
   결제 승인 후 Payment를 `COMPLETE`로 바꾸고, Order를 `COMPLETE`로 변경하는 Kafka 메시지를 발행합니다. 메시지 키는 `orderId`입니다.  
   근거: `payment/src/main/java/com/sparta/hotdeal/payment/application/service/PaymentService.java:58`, `payment/src/main/java/com/sparta/hotdeal/payment/event/producer/PaymentEventProducer.java:32`.

2. **발행 실패에 대한 애플리케이션 재시도는 없습니다.**  
   `kafkaTemplate.send()`의 반환 결과를 기다리거나 실패 콜백을 등록하지 않아, 비동기 전송 실패를 해당 호출부에서 처리하지 않습니다. Producer 설정에도 재시도 횟수가 명시되어 있지 않습니다. 따라서 **Kafka 클라이언트 내부 재시도와 애플리케이션의 재전송을 구분해야 하며, 실제 전송 재시도 횟수·성공 여부는 저장소만으로 확인할 수 없습니다.**  
   근거: `payment/src/main/java/com/sparta/hotdeal/payment/event/producer/PaymentEventProducer.java:51`, `payment/src/main/java/com/sparta/hotdeal/payment/infrastructure/config/ProducerApplicationKafkaConfig.java:21`.

   호출 중 동기적으로 예외가 발생하면 Payment를 `CANCEL`로 변경하고 `saveAndFlush()`한 뒤 예외를 던집니다. 다만 트랜잭션 안에서 `RuntimeException`을 던지므로, 이를 **취소 상태가 확정 저장되는 보상 처리**라고 볼 수는 없습니다.  
   근거: `payment/src/main/java/com/sparta/hotdeal/payment/event/service/PaymentProducerService.java:17`, `payment/src/main/java/com/sparta/hotdeal/payment/event/service/PaymentProducerService.java:24`, `payment/src/main/java/com/sparta/hotdeal/payment/common/exception/ApplicationException.java:8`.

3. **Order 상태 변경 실패는 DLQ 전송 대상입니다.**  
   소비자가 JSON 파싱 오류나 상태 변경 오류를 잡으면 원본 메시지와 오류 내용을 DLQ로 보냅니다. 컨테이너까지 전파되는 오류도 `FixedBackOff(0L, 0)`으로 설정되어 있어 추가 재시도 없이 DLQ 복구 처리를 수행합니다.  
   근거: `order/src/main/java/com/sparta/hotdeal/order/event/consumer/OrderStatusUpdateEventConsumer.java:26`, `order/src/main/java/com/sparta/hotdeal/order/event/consumer/OrderStatusUpdateEventConsumer.java:48`, `order/src/main/java/com/sparta/hotdeal/order/event/producer/OrderDlqProducer.java:23`, `order/src/main/java/com/sparta/hotdeal/order/infrastructure/config/ConsumerApplicationKafkaConfig.java:60`.

4. **DLQ에서 자동으로 다시 처리하는 코드는 확인되지 않습니다.**  
   `OrderDlqConsumer`는 메시지를 파싱하고 로그만 남깁니다. 이 클래스에는 `@Component`도 없으며 별도 빈 등록도 찾지 못했습니다. 운영자가 외부에서 재처리하는지는 저장소에서 확인할 수 없습니다. DLQ 발행 역시 `send()` 결과를 확인하지 않습니다.  
   근거: `order/src/main/java/com/sparta/hotdeal/order/event/consumer/OrderDlqConsumer.java:10`, `order/src/main/java/com/sparta/hotdeal/order/event/consumer/OrderDlqConsumer.java:16`, `order/src/main/java/com/sparta/hotdeal/order/event/producer/OrderDlqProducer.java:31`.

5. **중복 결과는 매번 다시 적용됩니다.**  
   메시지는 `orderId`와 `status`만 담고, 소비자는 주문을 조회해 `this.status = status`를 수행합니다. 이벤트 ID를 이용한 중복 제거나 현재 상태·전이 순서 검증은 없습니다. 따라서 연속된 동일 결과는 상태값만 보면 같지만, 취소 등 다른 상태로 바뀐 뒤 오래된 `COMPLETE`가 재전달되면 이를 다시 덮어쓸 수 있습니다.  
   근거: `payment/src/main/java/com/sparta/hotdeal/payment/event/message/order/ReqOrderUpdateStatusMessage.java:10`, `order/src/main/java/com/sparta/hotdeal/order/event/service/OrderConsumerService.java:26`, `order/src/main/java/com/sparta/hotdeal/order/domain/entity/order/Order.java:70`.

6. **재전달과 처리 완료 보장에도 빈틈이 있습니다.**  
   컨테이너는 `MANUAL` ACK 모드인데 상태 변경 리스너에는 ACK 호출이 없습니다. 따라서 코드만으로 “성공한 메시지는 확실히 커밋되어 다시 오지 않는다”는 보장을 확인할 수 없으며, 실제 재전달 여부는 실행 환경과 오프셋 상태를 확인해야 합니다.  
   근거: `order/src/main/java/com/sparta/hotdeal/order/infrastructure/config/ConsumerApplicationKafkaConfig.java:58`, `order/src/main/java/com/sparta/hotdeal/order/event/consumer/OrderStatusUpdateEventConsumer.java:26`.

</details>

<a id="q14-mcp"></a>

### Q14 MCP

> 프로모션 응답의 시간 필드는 어떤 시간대와 시작·종료 기준을 쓰고, 가격 필드는 각각 어떤 금액을 뜻하나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 맞음 | 맞음 |
| 확인 범위 | 밝힘 | 밝힘 |
| 이유 | 정각 스케줄러의 시각 일치 조건, 기간 검증과 중복 기준, 할인 후 판매가와 할인율 계산 설명이 코드와 일치합니다. 배포·직렬화 시간대와 통화는 미확정으로 남기고, 중복 검사와 실제 판매 시간 경계도 구분했습니다. | 시각이 정확히 일치하는지 보는 매 정각 스케줄러, 과거 시각 검증만 있고 start<end 검증은 없다는 점, 맞닿는 기간을 중복으로 보지 않는 중복 조건, discountPrice가 할인 후 판매가라는 점, discountRate가 반올림한 정수 %라는 점이 모두 코드와 맞습니다. 저장소에 시간대 설정이 없으니 시간대·직렬화 오프셋·통화는 확인할 수 없다고 구분해 밝혔습니다. |
| 대조한 근거 | `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/promotion/ResGetPromotionDto.java:18`, `product/src/main/java/com/sparta/hotdeal/product/application/scheduler/PromotionScheduler.java:27`, `product/src/main/java/com/sparta/hotdeal/product/application/scheduler/PromotionScheduler.java:32`, `product/src/main/java/com/sparta/hotdeal/product/application/scheduler/PromotionScheduler.java:59` 외 7개 | `product/src/main/java/com/sparta/hotdeal/product/application/scheduler/PromotionScheduler.java:27`, `product/src/main/java/com/sparta/hotdeal/product/application/scheduler/PromotionScheduler.java:32`, `product/src/main/java/com/sparta/hotdeal/product/application/scheduler/PromotionScheduler.java:41`, `product/src/main/java/com/sparta/hotdeal/product/application/scheduler/PromotionScheduler.java:59` 외 8개 |

<details><summary>답 원문</summary>

**시간대는 확인할 수 없고, 시작·종료는 스케줄러의 시각 일치로 처리됩니다. `discountPrice`는 할인 후 판매가이고, `discountRate`는 할인율입니다.** 구체적인 근거는 다음과 같습니다.

- **시간 필드와 시간대:** `start`·`end`는 프로모션 시작·종료 시각입니다. 요청은 `LocalDateTime`, 조회 응답은 `Timestamp`이며 응답 필드에 시간대 어노테이션은 없습니다. MCP가 제공한 저장소 근거만으로는 **KST인지 UTC인지, JSON 직렬화 시 어떤 시간대를 적용하는지 확인할 수 없습니다.**  
  근거: `product/src/main/java/com/sparta/hotdeal/product/application/dtos/req/promotion/ReqPostPromotionDto.java:20`, `product/src/main/java/com/sparta/hotdeal/product/application/dtos/req/promotion/ReqPostPromotionDto.java:23`, `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/promotion/ResGetPromotionDto.java:18`, `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/promotion/ResGetPromotionDto.java:19`

- **실제 시작·종료 기준:** 매 정각 실행하는 스케줄러가 현재 시각을 초 단위로 자르고, `start` 또는 `end`와 **정확히 같은** 프로모션을 찾습니다. 시작 시 `IN_PROGRESS`로 변경하고 상품에 할인가를 적용하며, 종료 시 `COMPLETED`로 변경하고 상품 할인가를 `null`로 만듭니다. 따라서 이를 일반적인 `start ≤ 현재 < end` 판정으로 설명할 수는 없습니다. 비정각 시각이나 놓친 실행을 `≤` 조건으로 따라잡는 처리도 없습니다.  
  근거: `product/src/main/java/com/sparta/hotdeal/product/application/scheduler/PromotionScheduler.java:27`, `product/src/main/java/com/sparta/hotdeal/product/application/scheduler/PromotionScheduler.java:32`, `product/src/main/java/com/sparta/hotdeal/product/application/scheduler/PromotionScheduler.java:38`, `product/src/main/java/com/sparta/hotdeal/product/application/scheduler/PromotionScheduler.java:41`, `product/src/main/java/com/sparta/hotdeal/product/application/scheduler/PromotionScheduler.java:45`, `product/src/main/java/com/sparta/hotdeal/product/application/scheduler/PromotionScheduler.java:50`, `product/src/main/java/com/sparta/hotdeal/product/application/scheduler/PromotionScheduler.java:56`, `product/src/main/java/com/sparta/hotdeal/product/application/scheduler/PromotionScheduler.java:59`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/repository/promotion/PromotionRepositoryJpa.java:21`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/repository/promotion/PromotionRepositoryJpa.java:24`

- **기간 검증:** 생성·수정에서 시작과 종료가 각각 현재보다 과거인지는 검사하지만, `start < end`는 검사하지 않습니다. 기간 중복은 `기존 시작 < 요청 종료 AND 기존 종료 > 요청 시작`으로 판단하므로 경계가 맞닿는 기간은 이 조건만으로 거절하지 않습니다. 이는 중복 검사 규칙이며, 실제 판매 가능 시간의 경계 포함 여부를 보장하는 규칙은 아닙니다.  
  근거: `product/src/main/java/com/sparta/hotdeal/product/application/service/promotion/PromotionService.java:46`, `product/src/main/java/com/sparta/hotdeal/product/application/service/promotion/PromotionService.java:86`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/repository/promotion/PromotionRepositoryJpa.java:16`

- **`discountPrice`:** 프로모션에서 적용할 **할인 후 판매 금액**입니다. 차감하는 할인액이 아닙니다. 서비스는 이 값이 상품 원가보다 작아야 한다고 검사합니다.  
  근거: `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/promotion/ResGetPromotionDto.java:21`, `product/src/main/java/com/sparta/hotdeal/product/application/service/promotion/PromotionService.java:162`, `product/src/main/java/com/sparta/hotdeal/product/application/scheduler/PromotionScheduler.java:41`

- **`discountRate`:** 금액이 아니라 **퍼센트 할인율**로, `Math.round((원가 − 할인가) / 원가 × 100)`으로 계산한 정수입니다.  
  근거: `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/promotion/ResGetPromotionDto.java:20`, `product/src/main/java/com/sparta/hotdeal/product/application/service/promotion/PromotionService.java:154`

- **상품 응답의 `price`와 구분:** 프로모션 조회 응답 자체에는 `price`가 없습니다. 별도 상품 응답의 `price`가 할인 전 기준 가격이고, 상품의 `discountPrice`는 스케줄러가 적용·해제하는 할인가입니다. 금액 필드는 정수지만, **통화가 KRW인지, 세금 포함 금액인지 등은 제공된 저장소 근거에서 확인할 수 없습니다.**  
  근거: `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/promotion/ResGetPromotionDto.java:20`, `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/promotion/ResGetPromotionDto.java:21`, `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/product/ResGetProductDto.java:19`, `product/src/main/java/com/sparta/hotdeal/product/application/dtos/res/product/ResGetProductDto.java:29`, `product/src/main/java/com/sparta/hotdeal/product/application/service/promotion/PromotionService.java:154`, `product/src/main/java/com/sparta/hotdeal/product/application/scheduler/PromotionScheduler.java:41`, `product/src/main/java/com/sparta/hotdeal/product/application/scheduler/PromotionScheduler.java:59`

</details>

<a id="q08-mcp"></a>

### Q08 MCP

> 카카오페이 결제를 마친 뒤 어떤 주소로 돌아와서 어떤 처리를 거쳐 운영 화면으로 이동하나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 맞음 | 맞음 |
| 확인 범위 | 밝힘 | 밝힘 |
| 이유 | 성공 경로의 안내 화면과 별도 승인 API를 정확히 구분했고, 결제·주문 상태 처리 설명도 코드와 일치한다. 실제 운영 주소와 화면 이동 연결은 미확인이라고 밝혔으며, 비동기 주문 갱신의 한계도 정확하다. | approvalUrl의 /payment/success 경로, 템플릿만 반환하는 성공 화면, 별도 confirm API(tid 조회, 카카오 승인, COMPLETE, Kafka 발행), 주문 서비스 소비와 비동기 send 모두 코드와 맞습니다. 도메인·포트와 운영 화면 이동은 저장소에서 확인할 수 없다고 구분해 밝혔습니다. |
| 대조한 근거 | `payment/src/main/java/com/sparta/hotdeal/payment/infrastructure/dto/kakaopay/req/ReqPostKakaoPayReadyDto.java:41`, `payment/src/main/java/com/sparta/hotdeal/payment/presentation/controller/PaymentViewController.java:18-20`, `payment/src/main/resources/templates/payment-success.html:1-12`, `payment/src/main/java/com/sparta/hotdeal/payment/application/service/PaymentService.java:58-67` 외 3개 | `payment/src/main/java/com/sparta/hotdeal/payment/infrastructure/dto/kakaopay/req/ReqPostKakaoPayReadyDto.java:41`, `payment/src/main/java/com/sparta/hotdeal/payment/presentation/controller/PaymentViewController.java:18-21`, `payment/src/main/resources/templates/payment-success.html:9`, `payment/src/main/java/com/sparta/hotdeal/payment/presentation/controller/PaymentController.java:44-48` 외 7개 |

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

<a id="q02-ask"></a>

### Q02 질의응답

> 재고를 차감한 뒤 주문 생성에 실패하면 재고를 어떻게 복구하고, 복구까지 실패하면 어떻게 처리하나요?

| | Codex | Opus |
|---|---|---|
| 정확도 | 부분 | 부분 |
| 확인 범위 | 밝힘 | 밝힘 |
| 이유 | 차감 실패 시 주문 되돌리기 요청과 복구 성공 후 주문 취소 요청, 일반 예외의 로그·ACK 설명은 맞고 연결의 불확실성도 밝혔다. 그러나 질문의 주문 생성 순서를 짚지 않았고, 코드로 확인되는 결제 준비 실패의 복구 요청·수량 증가·복구 실패 처리를 충분히 설명하지 않았다. | B는 질문의 핵심을 비껴갔습니다. 재고 차감 실패 경로(ProductKafkaConsumer:45-50)를 앞세웠고, 주문 저장이 차감보다 먼저라는 순서와 결제 측의 복구 요청 경로는 빠뜨렸습니다. 재고를 어떻게 복구하는지(수량 가산)도 설명하지 않았고, 복구 실패 시 조치는 코드(74-80행의 로그와 ACK)로 확인되는데도 '확인할 수 없다'고 했습니다. 토픽 값과 구독 연결을 확인할 수 없다는 점은 구분해 밝혔습니다. |
| 대조한 근거 | `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java:45`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java:70`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java:74`, `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:122` 외 2개 | `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java:45-50`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java:64-80`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaProducer.java:13-15`, `payment/src/main/java/com/sparta/hotdeal/payment/event/consumer/PaymentReadyEventConsumer.java:55-65` 외 1개 |

<details><summary>답 원문</summary>

차감 실패를 처리하는 Kafka 경로에서는 업무 예외가 발생하면 주문 ID를 담은 주문 되돌리기 요청을 발행합니다. 다만 발행 → 구독 연결은 확인되지 않았고, 토픽의 실제 값도 저장소에서 확인되지 않아 이 요청이 실제로 처리되는지는 단정할 수 없습니다. 이 경로의 구체적인 재고 보상 동작은 조각에서 확인되지 않습니다. (AI 해석 설명; `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java:45`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java:48`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaProducer.java:14`)

별도의 재고 복구 Kafka 경로는 복구 처리 성공 후 주문 취소 요청을 발행하도록 설명되어 있지만, 이 요청의 구독자도 확인되지 않았습니다. 복구 처리 자체가 실패했을 때 재시도·보상 등 어떤 조치를 하는지는 조각만으로 확인할 수 없습니다. 또한 리스너의 일반 예외 처리는 로그와 acknowledge만 하고 예외를 다시 던지지 않는다고 AI 해석되어 있습니다. (`product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java:61`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java:67`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java:70`)

</details>
