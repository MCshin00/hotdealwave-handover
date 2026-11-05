# 개발자 확인 목록

정확도: 맞음·부분·틀림. 확인 범위: 밝힘·일부·안 밝힘. 갈림은 두 채점자의 값이 다르다는 표시다.

| 묶음 | 질문 | 조건 | 정확도 Codex / Opus | 확인 범위 Codex / Opus |
|---|---|---|---|---|
| 1 | [T01](#t01-mcp) | MCP | 부분 / 맞음 | 밝힘 / 밝힘 |
| 3 | [T02](#t02-base) | 기준선 | 맞음 / 맞음 | 밝힘 / 밝힘 |
| 3 | [T02](#t02-mcp) | MCP | 맞음 / 맞음 | 밝힘 / 밝힘 |
| 3 | [T01](#t01-ask) | 질의응답 | 부분 / 부분 | 밝힘 / 밝힘 |
| 3 | [T02](#t02-ask) | 질의응답 | 맞음 / 맞음 | 밝힘 / 밝힘 |
| 3 | [T01](#t01-base) | 기준선 | 맞음 / 맞음 | 밝힘 / 밝힘 |

## 1. 정확도가 갈린 답 (1)

<a id="t01-mcp"></a>

### T01 MCP

> 주문 생성 API를 부르면 어떤 서비스를 호출하고 Kafka로 무엇을 발행하는지, 이어지지 않는 곳은 이유까지

| | Codex | Opus |
|---|---|---|
| 정확도 | 부분 | 맞음 |
| 확인 범위 | 밝힘 | 밝힘 |
| 이유 | 호출 대상·메시지 내용은 맞고 실제 토픽 연결과 쿠폰 역직렬화의 확인 한계도 밝혔다. 다만 상품 조회→배송지 조회를 순차적인 동기 흐름으로 설명했는데, 실제로는 두 조회를 supplyAsync로 병렬 실행한 뒤 join한다. | Feign 호출 대상, 조건부 쿠폰 호출, 재고 차감 메시지 발행, 그리고 KAFKA_REQUEST_PRODUCT_TOPIC과 KAFKA_REDUCE_QUANTITY_TOPIC이 서로 다른 환경변수라서 연결을 확정할 수 없다는 판단이 코드와 맞습니다. 확인된 것과 미확인인 것도 구분했습니다. 다만 응답 파일에는 valid:true만 있을 뿐 '쓰기 매핑 없음'을 지적하는 내용은 없어 출처 표현이 부정확합니다. isValid 필드와 setter 부재는 코드와 맞습니다. |
| 대조한 근거 | `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:88`, `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:93`, `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:98`, `order/src/main/java/com/sparta/hotdeal/order/application/dtos/product/req/ReqProductReduceQuantityDto.java:36` 외 4개 | `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:88-122,138-142`, `order/src/main/java/com/sparta/hotdeal/order/infrastructure/client/ProductClient.java:23`, `product/src/main/java/com/sparta/hotdeal/product/presentation/controller/ProductController.java:136`, `order/src/main/java/com/sparta/hotdeal/order/infrastructure/client/UserClient.java:15-16` 외 8개 |

<details><summary>답 원문</summary>

주문 생성은 **상품 조회 → 배송지 조회 → 조건부 쿠폰 검증·사용 → 주문 저장 → Kafka 재고 차감 요청**으로 이어집니다. 다만 **Kafka 요청이 상품 서비스에 전달되는 연결은 저장소에서 확인되지 않습니다.** 근거: `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:89`, `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:94`, `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:102`, `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:108`, `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:122`, `order/src/main/resources/application.yml:15`, `product/src/main/resources/application.yml:14`

1. **진입점 — 확인됨**

   `POST /api/v1/orders`의 `OrderController.createOrder`가 주문 생성 서비스를 실행합니다. 근거: `order/src/main/java/com/sparta/hotdeal/order/presentation/controller/OrderController.java:35`, `order/src/main/java/com/sparta/hotdeal/order/presentation/controller/OrderController.java:40`, `order/src/main/java/com/sparta/hotdeal/order/presentation/controller/OrderController.java:46`

2. **동기 서비스 호출 — Feign 대상 확인됨**

   - **product-service**: `ProductClient.getProductList` → `ProductController.getAllProducts`. 장바구니 상품들을 조회합니다. 근거: `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:89`, `order/src/main/java/com/sparta/hotdeal/order/infrastructure/client/ProductClient.java:23`, `product/src/main/java/com/sparta/hotdeal/product/presentation/controller/ProductController.java:136`
   - **user-service**: `UserClient.getAddress` → `AddressController.getAddress`. 배송지를 조회하며 요청자의 사용자 ID·이메일·역할을 전달합니다. 근거: `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:94`, `order/src/main/java/com/sparta/hotdeal/order/infrastructure/adapter/UserClientAdapter.java:18`, `order/src/main/java/com/sparta/hotdeal/order/infrastructure/client/UserClient.java:16`, `user/src/main/java/com/sparta/hotdeal/user/presentation/controller/AddressController.java:51`
   - **coupon-service, 조건부**: `couponId`가 있으면 `validateCoupon`을 호출하고, 수신한 `isValid`가 `true`일 때만 `useCoupon`을 호출합니다. 쿠폰이 없으면 두 호출을 생략하고 할인액은 0입니다. 근거: `order/src/main/java/com/sparta/hotdeal/order/infrastructure/adapter/CouponClientAdapter.java:34`, `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:138`, `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:141`, `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:142`, `coupon/src/main/java/com/sparta/hotdeal/coupon/presentation/controller/CouponController.java:63`, `coupon/src/main/java/com/sparta/hotdeal/coupon/presentation/controller/CouponController.java:71`

3. **Kafka로 발행하는 것 — 발행 코드 확인됨, 실제 전달은 미확인**

   주문·주문 상품을 저장하고 사용한 장바구니를 삭제 처리한 뒤, `OrderProducerService` → `OrderEventProducer.requestProduct`에서 **주문 ID와 상품별 ID·수량 목록을 담은 재고 차감 요청**을 `kafkaTemplate.send`로 발행합니다. 근거: `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:108`, `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:118`, `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:119`, `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:122`, `order/src/main/java/com/sparta/hotdeal/order/event/service/OrderProducerService.java:24`, `order/src/main/java/com/sparta/hotdeal/order/application/dtos/product/req/ReqProductReduceQuantityDto.java:28`, `order/src/main/java/com/sparta/hotdeal/order/application/dtos/product/req/ReqProductReduceQuantityDto.java:36`, `order/src/main/java/com/sparta/hotdeal/order/event/producer/OrderEventProducer.java:62`

   목적지 설정은 `spring.kafka.topics.request-product`이며 환경변수는 **`KAFKA_REQUEST_PRODUCT_TOPIC`**입니다. 실제 토픽 문자열은 저장소 밖 환경에서 정해져 **확인할 수 없습니다.** 근거: `order/src/main/java/com/sparta/hotdeal/order/event/producer/OrderEventProducer.java:24`, `order/src/main/resources/application.yml:15`, `docker-compose.yml:156`

4. **상품 서비스로 이어지지 않는 이유 — 연결 확인 안 됨**

   MCP 분석에는 `KAFKA_REQUEST_PRODUCT_TOPIC`을 쓰는 구독자가 없습니다. 상품 서비스의 재고 차감 리스너 `product-service.reduce`는 별도 환경변수 **`KAFKA_REDUCE_QUANTITY_TOPIC`**을 구독합니다. 두 환경변수의 실제 값이 같은지 저장소에서 확인할 수 없으므로, **“주문 생성 메시지를 상품 서비스가 받아 재고를 차감한다”라고 확정할 수 없습니다.** 환경변수 이름이 다르다는 사실만으로 운영에서도 연결이 끊겼다고 단정할 수도 없습니다. 근거: `order/src/main/resources/application.yml:15`, `docker-compose.yml:156`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java:29`, `product/src/main/resources/application.yml:14`, `docker-compose.yml:90`

5. **상품 리스너 이후의 별도 코드 — 존재하지만 주문 생성과의 연결은 미확인**

   - 재고 차감에 성공하면 **주문 ID를 담은 결제 요청**을 `KAFKA_REQUEST_PAYMENT_TOPIC`으로 발행합니다. 이 환경변수를 쓰는 구독자도 확인되지 않아 결제 서비스까지의 연결은 확정할 수 없습니다. 근거: `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java:38`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java:42`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductInventoryService.java:134`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaProducer.java:18`, `product/src/main/resources/application.yml:15`, `docker-compose.yml:91`
   - 재고 차감 중 **업무 예외**가 발생하면 **주문 ID를 담은 주문 되돌리기 요청**을 `KAFKA_REQUEST_ORDER_TOPIC`으로 발행합니다. 이 환경변수를 쓰는 구독자 역시 확인되지 않아 실제 주문 취소까지 이어지는지는 확인할 수 없습니다. 근거: `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java:45`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java:48`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductInventoryService.java:125`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaProducer.java:14`, `product/src/main/resources/application.yml:17`, `docker-compose.yml:89`

추가로, **주문 API 성공이 Kafka 전달 성공을 뜻하지는 않습니다.** 주문 발행 코드는 `send`의 비동기 완료 결과를 확인하지 않아 나중에 발생한 전송 실패가 상위 보상 처리로 전달되지 않습니다. 근거: `order/src/main/java/com/sparta/hotdeal/order/event/producer/OrderEventProducer.java:61`, `order/src/main/java/com/sparta/hotdeal/order/event/producer/OrderEventProducer.java:62`, `order/src/main/java/com/sparta/hotdeal/order/event/service/OrderProducerService.java:22`

**쿠폰 검증 → 사용에도 확인 한계가 있습니다.** 분석 문서는 응답 JSON의 `valid`와 주문 수신 DTO의 `isValid` 사이에 쓰기 매핑이 없다고 지적합니다. 따라서 저장된 검증 응답이 `valid=true`여도 실제 Feign 역직렬화 결과와 `useCoupon` 실행 여부는 해당 응답 기록만으로 확정할 수 없습니다. 근거: `analysis/hotdealwave/responses/coupon-service.CouponController.validateCoupon.json:20`, `order/src/main/java/com/sparta/hotdeal/order/infrastructure/dtos/coupon/res/ResPostCouponValidateDto.java:9`, `order/src/main/java/com/sparta/hotdeal/order/infrastructure/dtos/coupon/res/ResPostCouponValidateDto.java:14`, `order/src/main/java/com/sparta/hotdeal/order/infrastructure/dtos/coupon/res/ResPostCouponValidateDto.java:17`, `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:141`

</details>

## 2. 확인 범위만 갈린 답 (0)

## 3. 두 채점이 같은 답 가운데 무작위 표본(시드 45) (5)

<a id="t02-base"></a>

### T02 기준선

> createOrder 요청 필드는?

| | Codex | Opus |
|---|---|---|
| 정확도 | 맞음 | 맞음 |
| 확인 범위 | 밝힘 | 밝힘 |
| 이유 | 요청 DTO의 필드·타입·필수 검증을 정확히 설명했다. userId, email, role을 인증 principal에서 가져온다는 설명도 컨트롤러와 일치한다. | 요청 본문이 ReqPostOrderDto라는 점(44줄), 세 필드와 필수 여부를 정확히 적었습니다. userId·email·role이 본문이 아니라 @AuthenticationPrincipal에서 온다는 설명도 43·46줄과 맞습니다. |
| 대조한 근거 | `order/src/main/java/com/sparta/hotdeal/order/application/dtos/order/req/ReqPostOrderDto.java:16-20`, `order/src/main/java/com/sparta/hotdeal/order/presentation/controller/OrderController.java:43-46` | `order/src/main/java/com/sparta/hotdeal/order/presentation/controller/OrderController.java:43-46`, `order/src/main/java/com/sparta/hotdeal/order/application/dtos/order/req/ReqPostOrderDto.java:16-20` |

<details><summary>답 원문</summary>

`createOrder`의 요청 본문은 `ReqPostOrderDto`입니다. (`order/src/main/java/com/sparta/hotdeal/order/presentation/controller/OrderController.java:44`)

| 필드 | 타입 | 필수 여부 | 근거 (`파일:줄`, 저장소 루트 기준) |
|---|---|---|---|
| `basketList` | `List<UUID>` — 장바구니 ID 목록 | 필수 (`@NotNull`) | `order/src/main/java/com/sparta/hotdeal/order/application/dtos/order/req/ReqPostOrderDto.java:16`–`17` |
| `addressId` | `UUID` — 주소 ID | 필수 (`@NotNull`) | `order/src/main/java/com/sparta/hotdeal/order/application/dtos/order/req/ReqPostOrderDto.java:18`–`19` |
| `couponId` | `UUID` — 쿠폰 ID | DTO에 필수 검증 없음 | `order/src/main/java/com/sparta/hotdeal/order/application/dtos/order/req/ReqPostOrderDto.java:20` |

`userId`, `email`, `role`은 요청 본문이 아니라 인증된 사용자 정보에서 가져옵니다. (`order/src/main/java/com/sparta/hotdeal/order/presentation/controller/OrderController.java:43`, `order/src/main/java/com/sparta/hotdeal/order/presentation/controller/OrderController.java:46`)

</details>

<a id="t02-mcp"></a>

### T02 MCP

> createOrder 요청 필드는?

| | Codex | Opus |
|---|---|---|
| 정확도 | 맞음 | 맞음 |
| 확인 범위 | 밝힘 | 밝힘 |
| 이유 | 경로와 요청 필드가 정확하며, couponId가 null이면 외부 쿠폰 검증·사용 호출을 생략하고 할인액을 0으로 처리한다. basketList의 검증 선언과 추가 조건의 확인 범위도 구분했다. | POST /api/v1/orders 경로와 세 필드·검증 선언이 코드와 맞습니다. couponId가 null이면 검증 호출을 건너뛰고 (false, 0)을 돌려주며, isValid가 false이므로 사용 호출도 생략되어 할인액이 0이 된다는 설명도 맞습니다. 다만 근거로 든 줄은 메서드 선언(34)이고 실제 null 확인은 36줄, 사용 호출 생략은 OrderService:141에 있습니다. basketList에 최소 개수 검증이 없다는 점은 DTO만 봐도 확인되는데 '확인할 수 없다'고 쓴 것은 지나치게 조심스러운 표현이지만, 확인된 선언이 @NotNull뿐이라는 점은 구분해 밝혔습니다. |
| 대조한 근거 | `order/src/main/java/com/sparta/hotdeal/order/presentation/controller/OrderController.java:35-44`, `order/src/main/java/com/sparta/hotdeal/order/application/dtos/order/req/ReqPostOrderDto.java:16-20`, `order/src/main/java/com/sparta/hotdeal/order/infrastructure/adapter/CouponClientAdapter.java:34-47`, `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:138-146` 외 1개 | `order/src/main/java/com/sparta/hotdeal/order/presentation/controller/OrderController.java:35`, `order/src/main/java/com/sparta/hotdeal/order/presentation/controller/OrderController.java:40`, `order/src/main/java/com/sparta/hotdeal/order/application/dtos/order/req/ReqPostOrderDto.java:16-20`, `order/src/main/java/com/sparta/hotdeal/order/infrastructure/adapter/CouponClientAdapter.java:34-38` 외 1개 |

<details><summary>답 원문</summary>

`createOrder`는 `POST /api/v1/orders`입니다. 근거: `order/src/main/java/com/sparta/hotdeal/order/presentation/controller/OrderController.java:35`, `order/src/main/java/com/sparta/hotdeal/order/presentation/controller/OrderController.java:40`

요청 본문 필드는 다음 3개이며, 모두 **확인됨** 상태입니다.

| 필드 | 타입 | 검증 선언 | 근거 |
|---|---|---|---|
| `basketList` | `List<UUID>` | `@NotNull` | `order/src/main/java/com/sparta/hotdeal/order/application/dtos/order/req/ReqPostOrderDto.java:17` |
| `addressId` | `UUID` | `@NotNull` | `order/src/main/java/com/sparta/hotdeal/order/application/dtos/order/req/ReqPostOrderDto.java:19` |
| `couponId` | `UUID` | 검증 어노테이션 없음 | `order/src/main/java/com/sparta/hotdeal/order/application/dtos/order/req/ReqPostOrderDto.java:20` |

`couponId`는 `null`일 수 있으며, 이 경우 쿠폰 검증·사용 호출을 생략하고 할인액을 0으로 처리합니다. 근거: `order/src/main/java/com/sparta/hotdeal/order/infrastructure/adapter/CouponClientAdapter.java:34`

`basketList`의 최소 항목 수나 요소별 검증 조건은 제공된 도구 근거만으로 확인할 수 없습니다. 확인된 선언은 `@NotNull`입니다. 근거: `order/src/main/java/com/sparta/hotdeal/order/application/dtos/order/req/ReqPostOrderDto.java:17`

</details>

<a id="t01-ask"></a>

### T01 질의응답

> 주문 생성 API를 부르면 어떤 서비스를 호출하고 Kafka로 무엇을 발행하는지, 이어지지 않는 곳은 이유까지

| | Codex | Opus |
|---|---|---|
| 정확도 | 부분 | 부분 |
| 확인 범위 | 밝힘 | 밝힘 |
| 이유 | 주요 호출 대상과 실제 Kafka 연결을 확정할 수 없다는 설명은 맞다. 그러나 발행 내용을 requestProduct라는 이름만으로 설명해 재고 차감 목적과 주문 ID·상품별 ID·수량을 빠뜨렸고, 쿠폰이 없으면 검증 서비스 호출을 생략한다는 조건도 누락했다. | 상품·주소·쿠폰 호출과 재고 차감 발행, 구독 연결 미확인과 토픽 값이 저장소 밖에서 정해진다는 점은 맞습니다. 그러나 쿠폰 검증을 무조건 호출하는 것처럼 썼지만 couponId가 null이면 원격 호출을 하지 않습니다. 또 메시지 내용(orderId, productList)과 상품 서비스 쪽 리스너(KAFKA_REDUCE_QUANTITY_TOPIC)를 설명하지 않았고, 파일:줄 근거도 없습니다. |
| 대조한 근거 | `order/src/main/java/com/sparta/hotdeal/order/infrastructure/adapter/CouponClientAdapter.java:36`, `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:141`, `order/src/main/java/com/sparta/hotdeal/order/event/producer/OrderEventProducer.java:34`, `order/src/main/java/com/sparta/hotdeal/order/application/dtos/product/req/ReqProductReduceQuantityDto.java:16` 외 3개 | `order/src/main/java/com/sparta/hotdeal/order/infrastructure/adapter/CouponClientAdapter.java:36-38`, `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:88-122`, `order/src/main/java/com/sparta/hotdeal/order/application/dtos/product/req/ReqProductReduceQuantityDto.java:16-17,36-40`, `order/src/main/resources/application.yml:15` 외 1개 |

<details><summary>답 원문</summary>

주문 생성은 먼저 상품 서비스의 상품 목록 조회(`ProductClient.getProductList`), 사용자 서비스의 주소 조회(`UserClient.getAddress`), 쿠폰 서비스의 쿠폰 검증(`validateCoupon`)을 호출합니다. 쿠폰이 유효한 경우 쿠폰 사용(`useCoupon`)도 호출합니다. 주문 처리가 이어지면 Kafka로 `requestProduct` 이벤트를 발행합니다. 다만 이 이벤트의 구독자는 확인되지 않았고, 같은 토픽 설정을 쓰는 구독을 찾지 못했으며 실제 토픽 값도 저장소 밖 환경에 정해집니다. 따라서 Kafka 발행 뒤 실제 소비·후속 처리가 이어지는지는 확인할 수 없습니다. 발행 호출은 `OrderProducerService`에서 `OrderEventProducer`로 이어집니다.

</details>

<a id="t02-ask"></a>

### T02 질의응답

> createOrder 요청 필드는?

| | Codex | Opus |
|---|---|---|
| 정확도 | 맞음 | 맞음 |
| 확인 범위 | 밝힘 | 밝힘 |
| 이유 | 요청 본문의 세 필드와 타입, 검증 어노테이션이 코드와 일치한다. DTO에서 확인되는 선언만 설명했다. | ReqPostOrderDto의 세 필드와 타입, 검증 어노테이션(basketList·addressId만 @NotNull, couponId는 없음)을 정확히 적었고 줄 번호도 맞습니다. 이 질문에는 저장소 밖에서 정해지는 값이 없으므로 확인된 내용을 그대로 말한 것으로 충분합니다. |
| 대조한 근거 | `order/src/main/java/com/sparta/hotdeal/order/presentation/controller/OrderController.java:44`, `order/src/main/java/com/sparta/hotdeal/order/application/dtos/order/req/ReqPostOrderDto.java:16-20` | `order/src/main/java/com/sparta/hotdeal/order/application/dtos/order/req/ReqPostOrderDto.java:16-20`, `order/src/main/java/com/sparta/hotdeal/order/presentation/controller/OrderController.java:44` |

<details><summary>답 원문</summary>

createOrder 본문은 `ReqPostOrderDto`이며 필드는 다음과 같습니다.
- `basketList`: `List<UUID>`, `@NotNull`
- `addressId`: `UUID`, `@NotNull`
- `couponId`: `UUID`, 검증 어노테이션 없음
근거: [order/src/main/java/com/sparta/hotdeal/order/application/dtos/order/req/ReqPostOrderDto.java:17], [order/src/main/java/com/sparta/hotdeal/order/application/dtos/order/req/ReqPostOrderDto.java:19], [order/src/main/java/com/sparta/hotdeal/order/application/dtos/order/req/ReqPostOrderDto.java:20]

</details>

<a id="t01-base"></a>

### T01 기준선

> 주문 생성 API를 부르면 어떤 서비스를 호출하고 Kafka로 무엇을 발행하는지, 이어지지 않는 곳은 이유까지

| | Codex | Opus |
|---|---|---|
| 정확도 | 맞음 | 맞음 |
| 확인 범위 | 밝힘 | 밝힘 |
| 이유 | 병렬 조회, 조건부 쿠폰 처리, 재고 차감 메시지와 후속·보상 경로 설명이 코드와 일치한다. 토픽 연결의 환경변수 의존성, 비동기 전송 완료 미확인, 보상이 이어지지 않는 지점도 구분했다. | 호출 순서(장바구니→상품·주소 병렬→조건부 쿠폰 검증·사용→저장→재고 차감 발행), 메시지 key·payload, 후속 상품→결제 흐름과 보상 흐름, 쿠폰 미복구·send 미확인·DLQ 컴포넌트 미등록이 모두 코드와 맞습니다. 토픽 연결은 발행 측과 소비 측 환경변수 쌍이 같은 값을 가리켜야 하는 조건부 연결이라고 구분했습니다. |
| 대조한 근거 | `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:88`, `order/src/main/java/com/sparta/hotdeal/order/infrastructure/adapter/CouponClientAdapter.java:36`, `order/src/main/java/com/sparta/hotdeal/order/event/producer/OrderEventProducer.java:34`, `order/src/main/java/com/sparta/hotdeal/order/event/producer/OrderEventProducer.java:62` 외 5개 | `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:83-125`, `order/src/main/java/com/sparta/hotdeal/order/infrastructure/adapter/CouponClientAdapter.java:36-38`, `order/src/main/java/com/sparta/hotdeal/order/infrastructure/adapter/ProductClientAdapter.java:26`, `order/src/main/java/com/sparta/hotdeal/order/event/producer/OrderEventProducer.java:34-37,61-62` 외 10개 |

<details><summary>답 원문</summary>

주문 생성은 **상품·주소 조회 → 쿠폰 검증/사용 → 주문 저장 → Kafka 재고 차감 요청** 순서입니다. 결제 준비는 상품 서비스의 후속 메시지로 이어지지만, **실제 토픽 연결은 환경변수 값에 달려 있어 저장소만으로 연결 성공을 확정할 수 없습니다.**  
근거: `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:83`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java:29`, `order/src/main/resources/application.yml:15`, `product/src/main/resources/application.yml:14`.

1. **주문 생성 API에서 호출하는 서비스**

   `POST /api/v1/orders`가 `OrderService.createOrder()`를 호출합니다. 정상 반환은 HTTP 201이며, 응답에는 주문 ID를 넣습니다.  
   근거: `order/src/main/java/com/sparta/hotdeal/order/presentation/controller/OrderController.java:35`, `order/src/main/java/com/sparta/hotdeal/order/presentation/controller/OrderController.java:40`, `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:124`.

   | 순서 | 호출 및 처리 | 근거 |
   |---|---|---|
   | ① | 내부 `OrderBasketService`가 사용자 ID와 장바구니 ID 목록으로 장바구니를 조회합니다. 결과가 없으면 예외로 중단합니다. | `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderBasketService.java:20` |
   | ② | `product-service`의 `GET /api/v1/products?productIds=…`와 `user-service`의 `GET /api/v1/address/{addressId}`를 병렬 실행하고, 두 결과를 기다립니다. 상품 목록은 캐시 적중 시 HTTP 호출을 생략할 수 있습니다. | `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:88`, `order/src/main/java/com/sparta/hotdeal/order/infrastructure/client/ProductClient.java:18`, `order/src/main/java/com/sparta/hotdeal/order/infrastructure/client/UserClient.java:13`, `order/src/main/java/com/sparta/hotdeal/order/infrastructure/adapter/ProductClientAdapter.java:26` |
   | ③ | 쿠폰 ID가 있으면 `coupon-service`의 `POST /api/v1/coupons/{couponId}/validate`, 유효하면 `/use`를 호출합니다. 쿠폰 ID가 없으면 원격 호출 없이 할인액 0으로 진행합니다. | `order/src/main/java/com/sparta/hotdeal/order/infrastructure/adapter/CouponClientAdapter.java:34`, `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:138`, `order/src/main/java/com/sparta/hotdeal/order/infrastructure/client/CouponClient.java:13` |
   | ④ | 상품 존재·수량·판매 상태를 검증하며 금액을 계산하고, 주문과 주문 상품을 저장한 뒤 장바구니를 삭제 처리합니다. 초기 주문 상태는 `CREATE`입니다. | `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderCalculationService.java:17`, `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderCalculationService.java:30`, `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:105`, `order/src/main/java/com/sparta/hotdeal/order/domain/entity/order/Order.java:64` |
   | ⑤ | `OrderProducerService` → `OrderEventProducer`를 통해 재고 차감 요청을 발행합니다. | `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:122`, `order/src/main/java/com/sparta/hotdeal/order/event/service/OrderProducerService.java:22` |

2. **Kafka로 발행하는 내용과 후속 처리**

   주문 생성이 직접 발행하는 것은 **재고 차감 요청**입니다. 토픽은 `${KAFKA_REQUEST_PRODUCT_TOPIC}`, key는 주문 UUID 문자열, value는 다음 구조의 JSON입니다.  
   근거: `order/src/main/resources/application.yml:15`, `order/src/main/java/com/sparta/hotdeal/order/event/producer/OrderEventProducer.java:34`, `order/src/main/java/com/sparta/hotdeal/order/application/dtos/product/req/ReqProductReduceQuantityDto.java:16`.

   ```json
   {
     "orderId": "<주문 UUID>",
     "productList": [
       { "productId": "<상품 UUID>", "quantity": 2 }
     ]
   }
   ```

   이후 경로는 다음과 같습니다. 아래 토픽 표기는 실제 토픽명이 아니라 **토픽명을 공급하는 환경변수**입니다.  
   근거: `product/src/main/resources/application.yml:13`, `payment/src/main/resources/application.yml:14`.

   | 조건 | 발행 내용과 다음 처리 | 근거 |
   |---|---|---|
   | 상품 서비스가 차감 요청 수신 | `ProductInventoryService.reduceQuantity()`로 재고 차감. 성공하면 `KAFKA_REQUEST_PAYMENT_TOPIC`에 key=`orderId`, JSON=`{"orderId":"…"}`를 발행합니다. | `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java:33`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductInventoryService.java:31`, `product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductInventoryService.java:132` |
   | 결제 서비스가 준비 요청 수신 | 주문 서비스의 `GET /api/v1/orders/{orderId}`로 주문을 다시 조회한 뒤 카카오페이 `/ready`를 호출하고, 결제를 `PENDING`으로 저장합니다. | `payment/src/main/java/com/sparta/hotdeal/payment/event/consumer/PaymentReadyEventConsumer.java:27`, `payment/src/main/java/com/sparta/hotdeal/payment/infrastructure/client/OrderClient.java:14`, `payment/src/main/java/com/sparta/hotdeal/payment/event/service/PaymentConsumerService.java:28`, `payment/src/main/java/com/sparta/hotdeal/payment/infrastructure/client/KakaoPayClient.java:14` |
   | 재고 차감 중 `ApplicationException` | `KAFKA_REQUEST_ORDER_TOPIC`으로 key=`orderId`, JSON=`{"orderId":"…"}`를 발행합니다. 주문 취소 소비자는 주문 상태를 `CANCEL`로 바꿉니다. | `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java:45`, `product/src/main/resources/application.yml:17`, `order/src/main/java/com/sparta/hotdeal/order/event/consumer/OrderCancelEventConsumer.java:25`, `order/src/main/java/com/sparta/hotdeal/order/event/service/OrderConsumerService.java:19` |
   | 결제 준비 중 일반 예외 | 주문을 다시 조회해 `KAFKA_ROLLBACK_REDUCE_QUANTITY_TOPIC`으로 `{orderId, productList:[{productId, quantity}]}`를 발행합니다. 상품 서비스는 복구 요청을 받으면 재고 복구 후 주문 취소 요청을 발행합니다. | `payment/src/main/java/com/sparta/hotdeal/payment/event/consumer/PaymentReadyEventConsumer.java:55`, `payment/src/main/java/com/sparta/hotdeal/payment/event/producer/PaymentEventProducer.java:44`, `payment/src/main/java/com/sparta/hotdeal/payment/event/message/product/ReqProductRecoverQuantityDto.java:15`, `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java:57` |

   결제 준비 과정의 주문 재조회는 다시 **사용자 조회와 상품 목록 조회**를 호출합니다. 이때 주소 조회는 “자신이 저장한 주소만 조회 가능한 권한 설정”이라는 코드 주석과 함께 비활성화되어 있습니다.  
   근거: `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:183`, `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:187`, `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:207`.

3. **이어지지 않거나, 연결 여부를 확정할 수 없는 지점**

   - **토픽 연결은 조건부입니다.** 아래 각 행의 두 환경변수가 같은 실제 토픽을 가리켜야 해당 경로가 이어집니다. 변수명이 다르다는 사실만으로 연결이 끊겼다고 단정할 수는 없습니다. Compose도 값을 외부에서 전달받으므로, 배포 시 실제 값과 메시지 수신 여부는 저장소에서 확인할 수 없습니다.  
     근거: `docker-compose.yml:88`, `docker-compose.yml:152`, `docker-compose.yml:156`, `docker-compose.yml:193`.

     | 경로 | 발행 측 → 소비 측 환경변수 | 근거 |
     |---|---|---|
     | 주문 → 상품 차감 | `KAFKA_REQUEST_PRODUCT_TOPIC` → `KAFKA_REDUCE_QUANTITY_TOPIC` | `order/src/main/resources/application.yml:15`, `product/src/main/resources/application.yml:14` |
     | 상품 → 결제 준비 | `KAFKA_REQUEST_PAYMENT_TOPIC` → `KAFKA_READY_PAYMENT_TOPIC` | `product/src/main/resources/application.yml:15`, `payment/src/main/resources/application.yml:15` |
     | 결제 → 상품 복구 | `KAFKA_ROLLBACK_REDUCE_QUANTITY_TOPIC` → `KAFKA_RESTORE_QUANTITY_TOPIC` | `payment/src/main/resources/application.yml:16`, `product/src/main/resources/application.yml:16` |
     | 상품 → 주문 취소 | `KAFKA_REQUEST_ORDER_TOPIC` → `KAFKA_CANCEL_ORDER_TOPIC` | `product/src/main/resources/application.yml:17`, `order/src/main/resources/application.yml:16` |

   - **주문 생성만으로 결제 승인까지 진행되지는 않습니다.** Kafka 소비 경로는 결제 준비·`PENDING` 저장에서 끝납니다. 별도 `POST /api/v1/payments/confirm`이 호출되어야 결제를 승인하고 `{orderId, status:"COMPLETE"}`를 `KAFKA_UPDATE_ORDER_STATUS_TOPIC`으로 발행합니다. 주문 서비스가 이를 소비해 주문 상태를 갱신합니다.  
     근거: `payment/src/main/java/com/sparta/hotdeal/payment/event/service/PaymentConsumerService.java:28`, `payment/src/main/java/com/sparta/hotdeal/payment/presentation/controller/PaymentController.java:44`, `payment/src/main/java/com/sparta/hotdeal/payment/application/service/PaymentService.java:58`, `payment/src/main/java/com/sparta/hotdeal/payment/event/producer/PaymentEventProducer.java:32`, `payment/src/main/resources/application.yml:18`, `order/src/main/java/com/sparta/hotdeal/order/event/consumer/OrderStatusUpdateEventConsumer.java:25`.

   - **실패 보상이 쿠폰 복구까지 이어지지 않습니다.** 쿠폰은 주문 저장 전에 사용되지만, Kafka 주문 취소 처리는 주문 상태만 `CANCEL`로 바꿉니다. 쿠폰 복구용 어댑터는 있으나 이 취소 처리에서는 호출하지 않습니다.  
     근거: `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:102`, `order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java:142`, `order/src/main/java/com/sparta/hotdeal/order/event/service/OrderConsumerService.java:19`, `order/src/main/java/com/sparta/hotdeal/order/infrastructure/adapter/CouponClientAdapter.java:29`.

   - **일부 예외는 다음 메시지 없이 끝납니다.** 상품 소비자는 일반 예외를 로그로 남기고 ACK합니다. 결제 준비 실패 시에도 보상용 주문 재조회가 다시 실패하면 로그만 남기므로 재고 복구 요청을 보내지 못합니다.  
     근거: `product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java:51`, `payment/src/main/java/com/sparta/hotdeal/payment/event/consumer/PaymentReadyEventConsumer.java:60`.

   - **API 성공이 Kafka 전달 완료를 뜻하지는 않습니다.** 주문 발행 코드는 `kafkaTemplate.send()`의 완료 결과를 기다리거나 콜백으로 처리하지 않습니다. 따라서 비동기 전송 실패까지 `OrderProducerService`의 `catch`가 처리한다고 볼 수 없습니다.  
     근거: `order/src/main/java/com/sparta/hotdeal/order/event/producer/OrderEventProducer.java:61`, `order/src/main/java/com/sparta/hotdeal/order/event/service/OrderProducerService.java:22`.

   - **DLQ 이후 자동 복구 연결도 확인되지 않습니다.** 주문·결제의 DLQ 소비자 클래스에는 컴포넌트 등록 애너테이션이 없고, 별도 빈 등록도 찾지 못했습니다. 메서드 자체도 메시지 분석·로그까지만 수행합니다.  
     근거: `order/src/main/java/com/sparta/hotdeal/order/event/consumer/OrderDlqConsumer.java:10`, `order/src/main/java/com/sparta/hotdeal/order/event/consumer/OrderDlqConsumer.java:17`, `payment/src/main/java/com/sparta/hotdeal/payment/event/consumer/PaymentDlqConsumer.java:10`, `payment/src/main/java/com/sparta/hotdeal/payment/event/consumer/PaymentDlqConsumer.java:17`.

파일은 수정하지 않았습니다.

</details>
