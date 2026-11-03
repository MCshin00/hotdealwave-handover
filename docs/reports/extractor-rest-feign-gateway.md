# 추출기 보고서: REST·Feign·게이트웨이

이슈 #3(2단계 앞부분)의 결과다. 분석 대상 `cc43016`의 소스를 컴파일 없이 파서로 읽어, 엔드포인트 72개, Feign 호출 17개, 게이트웨이 경로 규칙 6개를 `out/analysis.json`으로 뽑았다. 교차 검증 세 가지에서 나온 경고는 1개다. 숫자는 `./gradlew :extractor:run`으로 다시 만들 수 있다.

파서 추출기(`extractor/`)는 #18에서 지웠다. 링크는 지우기 전 커밋을 가리킨다.

## 1. 추출기가 하는 일

| 단계 | 읽는 것 | 하는 일 | 나오는 것 |
|---|---|---|---|
| 1 | 서비스 폴더, `application.yml` | `build.gradle`과 `src/main/java`가 있는 폴더를 서비스로 보고, `spring.application.name`을 이름으로 쓴다 | 서비스 8개 |
| 2 | 서비스별 `.java` | JavaParser로 파싱한다. SymbolSolver는 그 서비스 소스만 본다 | 줄 번호가 붙은 구문 트리 |
| 3 | 컨트롤러, DTO, Feign, 게이트웨이 | 매핑·파라미터, DTO 필드와 어노테이션, Feign `name`/`url`, 게이트웨이 routes와 `AllowedPath`를 뽑는다 | 엔드포인트 72, Feign 호출 17, 경로 규칙 6 |
| 4 | 3의 결과 | Feign 호출을 대상 엔드포인트에, 엔드포인트를 게이트웨이 경로와 토큰 규칙에 잇는다 | 내부 연결 14, 외부 경로 144 |
| 5 | 4의 결과 + **정규식으로 따로 센 선언 목록** | 교차 검증 3가지를 돌린다 | 경고 1 |
| 6 | 전부 | Jackson으로 쓴다. 항목마다 상태와 근거 `파일:줄`을 붙인다 | `out/analysis.json` |

- 분석 대상을 빌드하지 않는다. Spring 라이브러리를 불러오지 않고 어노테이션은 이름과 인자 값으로 알아본다.
- 값을 소스만으로 정할 수 없으면 추측하지 않고 `확인 안 됨`과 그 줄을 낸다. 같은 서비스 안의 상수는 따라가서 값을 쓴다.
- 선언 목록 검사는 파서가 아니라 정규식으로 센다. 파서와 다른 방법으로 세야 파서가 빠뜨린 것이 드러난다.

## 2. 코드 구조

| 패키지 | 하는 일 | 주요 파일 |
|---|---|---|
| `scan` | 서비스 폴더와 이름 찾기 | `ServiceScanner` |
| `source` | 서비스별 파싱, SymbolSolver, 상수 값, YAML | `SourceContext`, `SourceValues`, `YamlSource` |
| `rest` | 컨트롤러 매핑, 파라미터, DTO 펼치기 | `RestExtractor`, `DtoExtractor`, `Mappings` |
| `feign` | `@FeignClient`와 메서드 | `FeignExtractor` |
| `gateway` | 경로 규칙, `AllowedPath` | `GatewayExtractor` |
| `link` | Feign↔엔드포인트, 경로 패턴 맞추기 | `RestLinker`, `PathMatcher` |
| `check` | 교차 검증 3가지 | `CrossChecks` |
| `model` | `analysis.json`에 쓰는 레코드 | `Analysis` |

`analysis.json`의 구조는 [extractor/README.md](https://github.com/MCshin00/hotdealwave-handover/blob/f2753bb10ce4c3abeae1932ab8281e4b48ca51f0/extractor/README.md)에 있다.

## 3. 추출 결과

| 항목 | 개수 | 상태 |
|---|---|---|
| 서비스 | 8 (게이트웨이·Eureka 포함) | 모두 확인됨 |
| 엔드포인트 | 72 | 모두 확인됨 |
| Feign 클라이언트 / 호출 | 9 / 17 | 내부 14 확인됨, 외부 3 저장소에 근거 없음 |
| 게이트웨이 경로 규칙 | 6 | 확인됨 |
| 엔드포인트별 외부 경로 | 144 | 129 확인됨, 15 확인 안 됨(토큰 판정) |
| 매핑 선언(정규식 / 추출) | 103 / 102 | 차이 1 = 경고 1 |

| 서비스 | 엔드포인트 |
|---|---|
| product | 18 |
| user | 17 |
| coupon | 12 |
| order | 11 |
| payment | 9 |
| company | 5 |

| HTTP 메서드 | 개수 |
|---|---|
| GET | 25 |
| POST | 24 |
| PATCH | 9 |
| DELETE | 7 |
| PUT | 7 |

## 4. 서비스 사이 호출 (Feign)

| 부르는 쪽 ↓ / 받는 쪽 → | order | product | coupon | user | company | KakaoPay(외부) |
|---|---|---|---|---|---|---|
| **order** |  | 3 | 3 | 2 |  |  |
| **payment** | 2 |  |  |  |  | 3 |
| **product** | 1 |  |  | 1 | 1 |  |
| **coupon** |  |  |  |  | 1 |  |

칸의 숫자는 Feign 메서드 수다. user와 company는 다른 서비스를 부르지 않는다. 가장 많이 부르는 쪽은 order(8)다. 받는 쪽은 order·product·coupon·user가 3개씩, company가 2개다. 내부 호출 14개는 모두 대상 컨트롤러의 엔드포인트에 HTTP 메서드와 경로로 연결됐다.

| 부르는 서비스 | Feign 메서드 | 호출 | 받는 엔드포인트 |
|---|---|---|---|
| order | `validateCoupon` | `POST /api/v1/coupons/{couponId}/validate` | coupon `validateCoupon` |
| order | `useCoupon` | `POST /api/v1/coupons/{couponId}/use` | coupon `useCoupon` |
| order | `recoverCoupon` | `POST /api/v1/coupons/{couponId}/recover` | coupon `recoverCoupon` |
| order | `getProduct` | `GET /api/v1/products/{productId}` | product `getProduct` |
| order | `getProductList` | `GET /api/v1/products` | product `getAllProducts` |
| order | `reduceQuantity` | `PUT /api/v1/products/restore-quantity` | product `restoreQuantity` |
| order | `getAddress` | `GET /api/v1/address/{addressId}` | user `getAddress` |
| order | `getUser` | `GET /api/v1/users/{userId}` | user `getUser` |
| payment | `getOrderById` | `GET /api/v1/orders/{orderId}` | order `getOrderById` |
| payment | `updateOrderStatus` | `PUT /api/v1/orders/{orderId}/status` | order `updateOrderStatus` |
| payment | `ready` / `approve` / `cancel` | `POST /ready` 등 (KakaoPay URL) | 저장소에 근거 없음 |
| product | `getOrderById` | `GET /api/v1/orders/{orderId}` | order `getOrderById` |
| product | `getUserById` | `GET /api/v1/users/{userId}` | user `getUser` |
| product | `getCompanyById` | `GET /api/v1/companies/{companyId}` | company `getCompanyById` |
| coupon | `getCompanyById` | `GET /api/v1/companies/{companyId}` | company `getCompanyById` |

## 5. 게이트웨이와 토큰

| 경로 규칙 | 외부 경로 패턴 | 별칭(RewritePath) |
|---|---|---|
| user-service | `/api/v1/auth/**`, `/api/v1/users/**`, `/api/v1/address/**`, `/api/v1/address-default/**` | `/user-service/**` |
| company-service | `/api/v1/companies/**` | `/company-service/**` |
| product-service | `/api/v1/products/**`, `/api/v1/promotions/**`, `/api/v1/reviews/**` | `/product-service/**` |
| coupon-service | `/api/v1/coupons/**`, `/api/v1/coupon-infos/**` | `/coupon-service/**` |
| order-service | `/api/v1/orders/**`, `/api/v1/baskets/**` | `/order-service/**` |
| payment-service | `/api/v1/payments/**`, `/payment/**` | `/payment-service/**` |

토큰 없이 통과하는 경로는 `gateway/.../filter/AllowedPath.java`의 enum 상수로 정해져 있다. 판정은 이 enum의 `startsWith`와 메서드 비교를 그대로 따른다.

| 상수 | 경로 | 메서드 |
|---|---|---|
| `AUTH` | `/api/v1/auth` | 모두 |
| `PAYMENT` | `/payment/` | 모두 |
| `PRODUCTS` | `/api/v1/products` | GET |
| `PROMOTIONS` | `/api/v1/promotions` | GET |
| `REVIEWS` | `/api/v1/reviews` | GET |
| `SWAGGER`, `API_DOCS` | `/swagger-ui/`, `/v3/api-docs` | 모두 |

| 서비스 | 토큰 필요 | 토큰 불필요 |
|---|---|---|
| product | 12 | 6 |
| user | 11 | 6 |
| coupon | 12 | 0 |
| order | 11 | 0 |
| payment | 6 | 3 |
| company | 5 | 0 |
| **합계** | **57** | **15** |

토큰이 필요 없는 15개는 product 6(상품·프로모션·리뷰 GET), user 6(`/api/v1/auth` 아래 회원가입·로그인·이메일 인증·토큰 갱신), payment 3(`/payment/success`, `/fail`, `/cancel`)이다.

## 6. 요청·응답과 DTO

| 파라미터 종류 | 개수 |
|---|---|
| 경로 | 37 |
| 쿼리 | 33 |
| 본문 | 29 |

`@AuthenticationPrincipal`처럼 클라이언트가 보내지 않는 인자는 빼고, `Pageable`은 쿼리로 둔다. 응답은 69개가 객체, 3개가 단일 값이다. `ResponseDto<T>`는 `T`까지 펼친다.

| DTO 필드의 검증 어노테이션 | 개수 |
|---|---|
| `@NotNull` | 37 |
| `@NotBlank` | 9 |
| `@Pattern` | 6 |
| `@Positive`, `@Max` | 각 4 |
| `@Min`, `@Size` | 각 2 |
| `@Digits` | 1 |

예: `POST /api/v1/orders` (`order/.../OrderController.java:40`)

```text
외부 경로  /api/v1/orders            토큰 필요 (확인됨)
본문       ReqPostOrderDto
           ├─ basketList  List<UUID>  @NotNull
           ├─ addressId   UUID        @NotNull
           └─ couponId    UUID
응답       ResponseDto<ResPostOrderDto>
```

## 7. 교차 검증

| 검사 | 비교 | 경고 |
|---|---|---|
| 선언 목록 | 정규식으로 센 `@*Mapping`·`@FeignClient` ↔ 추출 결과 | 1 |
| Feign 대상 | Feign 메서드 ↔ 대상 서비스 컨트롤러 | 0 |
| 게이트웨이 경로 | 경로 규칙 ↔ 컨트롤러 경로 (양방향) | 0 |

유일한 경고는 `payment/.../PaymentViewController.java:12`다. `/* 삭제 예정 ... */` 주석 안에 있는 `@GetMapping`을 정규식이 셌고 파서는 세지 않았다. 판정은 3단계에서 한다.

경고가 적게 나온 이유는 이 저장소의 매핑이 모두 리터럴 문자열이고, Feign 경로가 대상 컨트롤러 경로와 그대로 같기 때문이다. Kafka 쪽(토픽이 환경변수로 이어짐)은 2단계 뒷부분에서 다룬다.

## 8. 눈에 띈 것 (판정 전)

- **이름과 경로가 반대인 Feign 메서드.** order의 `ProductClient.reduceQuantity`(`ProductClient.java:26`)는 이름은 재고 차감인데, 부르는 곳은 product의 `PUT /api/v1/products/restore-quantity`(`restoreQuantity`, `ProductController.java:112`)다. 추출기는 이름이 아니라 경로로 연결하므로 연결 자체는 확인됨이다. 실제 의도는 4단계 업무 규칙·함정에서 볼 항목이다(미확인).
- **별칭 경로의 토큰 판정.** `/order-service/...` 같은 별칭 15개는 게이트웨이가 경로를 바꾸기 전에 토큰을 검사하는지, 바꾼 뒤에 검사하는지가 저장소 코드만으로 정해지지 않아 `확인 안 됨`이다. 양쪽 결과와 근거를 함께 남겼다. 프론트가 실제로 부르는 `/api/v1/...` 경로는 모두 판정됐다.

## 9. 검증

| 확인 | 결과 |
|---|---|
| `./gradlew :extractor:test` | 6개 통과 (대표 사례 5 + 상수·순환·해석 불가 1). 대표 사례는 구현 전에 실패하는 것을 먼저 확인했다 |
| `./gradlew :extractor:run` | 교차 검증별 경고 수 출력, `out/analysis.json` 생성 |
| 대표 사례 대조 | `OrderController.java:40`, `ProductClient.java:26`, `KakaoPayClient.java:14`, 게이트웨이 order 규칙, `AllowedPath.java:10`의 출력이 코드와 일치 |
| 분석 대상 | `target/HotDealWave` 변경 없음 |
