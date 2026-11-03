# 응답 확인 보고서: HotDealWave 실제 응답과 추출 비교

이슈 #13(4단계)의 결과다. 고정 커밋 `cc43016`을 테스트 환경에 띄워 배포된 서비스를 대신하고, 응답 호출 Skill로 엔드포인트 72개를 처리했다. 61개를 불러 응답을 저장했고 11개는 부르지 못한 이유를 남겼다. 정상 응답 56개의 필드는 추출과 차이가 없어 서비스 파일을 고치지 않았다. 쿠폰을 적용해 주문한 뒤에도 쿠폰이 사용 처리되지 않는 것을 응답으로 확인했다.

## 1. 4단계 전에 바뀐 구조

[3단계 보고서](ai-extract-hotdealwave.md) 뒤에 추출 결과와 검증기의 구조가 바뀌었다. 4단계는 바뀐 구조에서 진행했다.

| 이슈 → PR | 바뀐 것 | 이유 |
|---|---|---|
| #14 → #15 | 검증기의 외부 자원 종류별 설정 키 표를 없앴다. 추출한 `kind`가 근거 줄에 있으면 된다. Kafka 토픽의 compose 근거는 줄 모양 대신 `${환경변수}`가 그 서비스의 `services.<composeService>.environment` 아래에 있는지 본다. 이를 위해 추출 결과에 `service.composeService`를 더했다 | 두 규칙이 HotDealWave의 작성 방식을 그대로 옮긴 것이었다. 경고 수는 변경 전과 같다 |
| #16 → #18 | 파서 추출기(`extractor/`)를 지우고, 추출 결과 형식을 파서 README에서 [docs/format/README.md](../format/README.md)로 옮겼다. 검증기 기본 입력은 AI 추출 결과다 | 파서 결과에는 `composeService`가 없어 쓰는 곳이 없어졌다 |
| #17 → #19 | 형식을 JSON Schema(서비스·관계)로 정했다. `extracted.json` 하나를 서비스별 파일 8개와 `relations.json`(Kafka 연결)으로 나누고, ID 앞부분을 애플리케이션 이름으로 바꿨다. 검증기는 스키마 검사(`schema`) 뒤 파일을 합쳐 기존 검사를 한다. 추출 Skill은 서비스마다 파일 하나를 쓰고, 서비스 파일이 모두 있을 때 관계 단계를 따로 실행한다 | 설명 글로 된 형식은 해석이 갈렸고(3단계 1차의 `service` 표기), 파일 하나로는 서비스가 많은 저장소를 한 세션에서 추출하기 어렵다 |

#19는 다시 추출하지 않고 기존 결과를 나눠 옮겼다. 경고 수와 상태 개수는 바뀌지 않았다.

지금 흐름은 다음과 같다.

```
target/HotDealWave @cc43016
  --> 추출 Skill --> analysis/hotdealwave/services/*.json + relations.json
  --> (서버가 있을 때) 응답 호출 Skill --> responses/*.json --> 비교 스크립트 --> 서비스 파일 수정
  --> verifier --> out/analysis.json (상태·경고)
```

## 2. 흐름

| 단계 | 하는 쪽 | 하는 것 | 나오는 것 |
|---|---|---|---|
| 1 | 개발자 | 고정 커밋을 테스트 환경에 띄우고 테스트 계정을 준비한다(6절) | 서버 주소, 테스트 계정 |
| 2 | AI 세션 | [응답 호출 Skill](../../.claude/skills/probe-msa-java-spring/SKILL.md)로 로그인하고, 추출의 외부 경로로 조회 API 전부와 테스트 데이터로 부를 수 있는 쓰기 API를 부른다 | `analysis/hotdealwave/responses/<엔드포인트 ID>.json` 72개 |
| 3 | 스크립트 | `python scripts/compare_responses.py analysis/hotdealwave analysis/hotdealwave/responses` | 엔드포인트별 응답에만 있음·추출에만 있음·이름 다름·비교 보류 |
| 4 | AI 세션 | 차이대로 서비스 파일을 고치고 근거를 응답 파일로 둔다 | 이번에는 차이 0개라 고친 곳 없음 |
| 5 | 검증기 | `./gradlew :verifier:run` | `out/analysis.json`, 검사별 경고 수 |

- 넘겨받는 사람이 가진 것만 쓴다. 서버 주소와 테스트 계정이다. `.env`, 대상의 설정 값, DB·브로커 직접 접근은 판정 근거로 쓰지 않는다.
- 6절의 환경 준비에서 DB를 만졌지만, DB에서 본 값은 응답 파일이나 근거로 쓰지 않았다.
- 토큰·비밀번호·인증 코드는 응답 파일에 남기지 않았다. 저장한 파일에서 검색해 모두 가려진 것을 확인했다.

## 3. 구성

| 부분 | 하는 일 |
|---|---|
| 응답 파일 | `request`(메서드·경로·본문), `statusCode`, `body`, `reason`. 부르지 못하면 `statusCode`·`body`가 `null`이고 `reason`에 이유를 적는다 |
| 비교 스크립트 | 응답 키와 추출한 응답 필드의 `jsonName`을 비교한다. 이름은 `is` 접두사·밑줄·대소문자를 정리해 맞추므로 `isUsed`와 `used`는 "이름 다름"이 된다. 추출 타입이 `Page<T>`·`PagedModel<T>`이고 응답에 `content` 배열이 있으면 `content` 항목만 `T`와 비교하고 페이지 객체의 나머지 키는 비교하지 않는다. 오류 응답, 빈 배열, `null`, 추출에 내부 구조가 없는 타입은 비교 보류다. 파일은 고치지 않는다 |
| 검증기 | 근거가 `analysis/<대상>/responses/` 아래 파일이면 근거 줄 검사를 건너뛰고, 근거가 모두 응답 파일인 사실은 `확인됨`으로 둔다 |

## 4. 결과

| 항목 | 값 |
|---|---:|
| 부른 엔드포인트 / 못 부른 엔드포인트 | 61 / 11 |
| 응답 상태 | 200 48, 201 8, 400 1, 404 2, 500 2 |
| 응답에만 있음 / 추출에만 있음 / 이름 다름 | 0 / 0 / 0 |
| 비교 보류 | 10 (오류 응답 5, `Timestamp` 4, 빈 배열 1) |
| 고친 항목 | 0 |
| 검사별 경고 | schema 0, 근거 줄 0, 선언 목록 1, Feign 대상 0, 게이트웨이 경로 0, Kafka 선언 0, Kafka 토픽 11 |
| 상태 개수 | 확인됨 3906 / 확인 안 됨 38 / 저장소에 근거 없음 320 (변경 없음) |

못 부른 11개의 이유는 다음과 같다.

| 이유 | 수 | 엔드포인트 |
|---|---:|---|
| 외부 결제 호출은 테스트 범위 밖 | 3 | 결제 준비·승인·환불 |
| HTML 화면 | 3 | 결제 성공·실패·취소 화면 |
| 리뷰 생성이 실패해 리뷰 ID 없음 | 3 | 리뷰 단건 조회·수정·삭제 |
| 결제 목록이 비어 결제 ID 없음 | 1 | 결제 단건 조회 |
| 상품 삭제가 S3 이미지 삭제에 의존 | 1 | 상품 삭제 |

비교 보류 10개는 다음과 같다.

| 종류 | 엔드포인트 | 원인 |
|---|---|---|
| 오류 응답 | 주문 환불 400 | 결제가 막혀 주문이 결제 대기 상태라 환불 조건을 충족하지 않는다 |
| 오류 응답 | 결제 취소 404 | 이 주문의 결제 데이터가 없다 |
| 오류 응답 | 상품 생성 500 | S3 업로드 실패(6절) |
| 오류 응답 | 리뷰 생성 404 | 상품 서비스의 내부 주문 조회가 404로 실패했다. 원인은 확인하지 못했다 |
| 오류 응답 | 리뷰 목록 500 | 게이트웨이와 상품 서비스의 인증 제외 경로가 다르다(5절) |
| `Timestamp` | 프로모션 단건·목록의 `start`·`end` | 응답은 문자열 하나이고 추출에는 내부 구조가 없다 |
| 빈 배열 | 결제 목록 | 외부 결제가 막혀 결제 데이터를 만들 수 없다 |

## 5. 응답으로 드러난 것

대상 코드는 고치지 않는다. 아래는 업무 규칙·함정 단계(5단계)에서 다룰 후보이며 판정 전이다.

### 쿠폰을 적용한 주문 뒤에도 쿠폰이 사용 처리되지 않는다

- 쿠폰을 적용해 주문했다(`responses/order-service.OrderController.createOrder.json:10`의 `couponId`). 주문에는 할인 1,000원이 반영됐다.
- 직후 그 쿠폰의 상세 조회는 `used: false`, `usedDate: null`이다(`responses/coupon-service.CouponController.getUserCouponDetail.json:16`, `:18`). 사용자 쿠폰 목록에서도 `false`다(`responses/coupon-service.CouponController.getUserCoupons.json:19`).
- 코드에서 확인한 원인
  - 쿠폰 서비스의 검증 응답 필드는 `boolean isValid`이고(`coupon/.../ResPostCouponValidateDto.java:13`), 실제 응답 키는 `valid`다(`responses/coupon-service.CouponController.validateCoupon.json:20`).
  - 주문 서비스의 수신 클래스는 필드 `isValid`에 setter가 없어(`order/.../infrastructure/dtos/coupon/res/ResPostCouponValidateDto.java:14`) `valid`를 받지 못한다.
  - 그래서 `order/.../OrderService.java:141`의 조건이 거짓이 되어 쿠폰 사용 API를 부르지 않는다. 할인액(`totalDiscountAmount`)은 이름이 같아 그대로 받는다(`:114`).

### 리뷰 목록이 500을 낸다

게이트웨이는 리뷰 조회를 토큰 없이 통과시키지만(`gateway/.../filter/AllowedPath.java:12`), 상품 서비스의 인증 필터는 같은 경로를 제외하지 않아 사용자 헤더가 없다는 예외를 던진다(`product/.../filter/AuthenticationFilter.java:50`). 같은 인증 제외 규칙이 두 곳에 따로 있고 서로 다르다.

## 6. 환경 준비

테스트 환경을 띄우면서 막힌 곳이다. 넘겨받는 사람도 서비스를 다시 띄울 때 같은 곳에서 막힌다. 준비는 Skill 밖에서 했고, 쓴 값과 스크립트는 이 저장소에 남기지 않았다.

| 막힌 곳 | 근거 | 한 일 |
|---|---|---|
| 가입에 이메일 인증이 필요하다 | `user-service.AuthController.sendVerifyEmail` | 테스트 메일 수신기를 붙였다 |
| 가입 API로 관리자 계정을 만들 수 없다 | `user/.../AuthService.java:155` | 테스트 계정 하나에 DB로 관리자 역할을 줬다 |
| 업체 생성·조회 응답에 업체 ID가 없다 | `company/.../CompanyController.java:44` | 업체 ID를 DB에서 찾았다 |
| 새 검색 인덱스에 기본 정렬 필드 `createdAt` 매핑이 없어 상품 목록이 500이다 | `product/.../ProductController.java:142` | 매핑을 추가했다 |
| S3 클라이언트에 접속 주소 설정 경로가 없어 테스트 S3를 붙일 수 없다 | `product/.../FileUploadService.java:33` | 상품 생성 실패 응답을 저장한 뒤 상품 데이터를 DB와 검색 인덱스에 직접 넣었다 |
| 결제가 외부 결제 서비스를 부른다 | `payment/.../KakaoPayClient.java:14` | 결제 서비스를 외부 접속이 막힌 네트워크에 두고 확인한 뒤 주문했다 |

## 7. 한계

- 응답으로 고친 항목이 없어, 응답 근거 처리(근거 줄 검사를 건너뛰고 `확인됨`)는 아직 실제 결과에 쓰인 적이 없다.
- `out/analysis.json`의 상태는 응답으로 확인한 사실과 코드로만 확인한 사실을 구분하지 않는다. 응답 파일이 있으니 문서 페이지(6단계)에서 따로 보여 줄 수 있다.
- 비교는 필드 이름만 본다. 타입과 값은 비교하지 않는다.
- 외부 결제를 막아 결제 이후 흐름과 Kafka 발행·구독은 응답으로 확인하지 못했다. Kafka 토픽 경고 11개는 그대로다.
- 서비스 단위로 바꾼 추출 Skill(#19)은 아직 실제 추출에 쓰지 않았다. 지금 서비스 파일은 3단계에서 한 번에 추출한 결과를 나눈 것이다. 두 번째 대상(10단계)에서 처음 쓴다.
- 호출은 한 번 했다. 같은 Skill로 다시 불렀을 때 호출 범위가 얼마나 달라지는지는 보지 않았다.
