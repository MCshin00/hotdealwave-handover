**판정 변경은 Q13 ask·Q17 ask 두 칸을 반박합니다.** Q02 base·Q07 ask는 판정에 동의하지만 설명 일부를 정정해야 합니다. 파일은 수정하지 않았고, 금지된 파일도 열지 않았습니다. 아래 코드 근거는 모두 `cc43016` 기준입니다.

확인 칸 8개입니다.

1. **Q02 ask — 동의: 부분·밝힘.** 복구 수량을 다시 더하는 핵심 처리를 답하지 못했습니다. 연결 미확인은 밝혔습니다. 근거: [ProductInventoryService.java:103](../../../../target/HotDealWave/product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductInventoryService.java#L103).

2. **Q08 ask — 동의: 맞음·밝힘.** 성공 화면과 별도 승인 API를 구분했고 운영 화면 연결도 단정하지 않았습니다. 도메인 변수명 누락이나 ‘AI 해석’ 표시는 확인 범위 감점 사유가 아닙니다. 근거: [PaymentViewController.java:18](../../../../target/HotDealWave/payment/src/main/java/com/sparta/hotdeal/payment/presentation/controller/PaymentViewController.java#L18), [PaymentService.java:58](../../../../target/HotDealWave/payment/src/main/java/com/sparta/hotdeal/payment/application/service/PaymentService.java#L58).

3. **Q08 mcp — 동의: 맞음·밝힘.** 복귀·승인·이벤트 처리 설명이 맞고, 실제 주문 갱신 완료와 운영 화면 이동은 보장하지 않았습니다. 양쪽 토픽 참조도 같습니다. 근거: [payment/application.yml:18](../../../../target/HotDealWave/payment/src/main/resources/application.yml#L18), [order/application.yml:17](../../../../target/HotDealWave/order/src/main/resources/application.yml#L17).

4. **Q12 base — 동의: 맞음·밝힘.** 재시도 0회·DLQ 처리와 중복 상태 재대입을 설명하고 실제 재전달은 구분했습니다. 근거: [ConsumerApplicationKafkaConfig.java:67](../../../../target/HotDealWave/order/src/main/java/com/sparta/hotdeal/order/infrastructure/config/ConsumerApplicationKafkaConfig.java#L67), [Order.java:70](../../../../target/HotDealWave/order/src/main/java/com/sparta/hotdeal/order/domain/entity/order/Order.java#L70).

5. **Q14 mcp — 동의: 맞음·밝힘.** 정각 실행·시각 일치 조건과 할인가·반올림 할인율을 맞게 설명했습니다. 시간대 미확정도 적절합니다. 근거: [PromotionScheduler.java:27](../../../../target/HotDealWave/product/src/main/java/com/sparta/hotdeal/product/application/scheduler/PromotionScheduler.java#L27), [PromotionService.java:154](../../../../target/HotDealWave/product/src/main/java/com/sparta/hotdeal/product/application/service/promotion/PromotionService.java#L154).

6. **Q02 base — 판정 동의: 맞음·일부. 설명은 정정.** 서로 다른 토픽 변수 사이의 연결을 확정적으로 설명한 감점은 타당합니다. 다만 **“양쪽 모두 로그·ACK”는 틀립니다.** 결제 쪽 복구 실패는 로그만 남기고, 상품 쪽은 로그와 ACK입니다. 답 원문은 이를 구분했습니다. 근거: [PaymentReadyEventConsumer.java:62](../../../../target/HotDealWave/payment/src/main/java/com/sparta/hotdeal/payment/event/consumer/PaymentReadyEventConsumer.java#L62), [ProductKafkaConsumer.java:74](../../../../target/HotDealWave/product/src/main/java/com/sparta/hotdeal/product/infrastructure/kafka/ProductKafkaConsumer.java#L74).

7. **Q01 ask — 동의: 부분·일부.** 상품·주소 병렬 조회와 쿠폰→금액 계산 순서를 잘못 설명했습니다. 전달 성공 보장 불가를 밝힌 것만으로 서로 다른 토픽의 연결 불확실성까지 밝힌 것은 아닙니다. 근거: [OrderService.java:88](../../../../target/HotDealWave/order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java#L88), [order/application.yml:15](../../../../target/HotDealWave/order/src/main/resources/application.yml#L15), [product/application.yml:14](../../../../target/HotDealWave/product/src/main/resources/application.yml#L14).

8. **Q13 ask — 반박: 틀림·밝힘 → 부분·밝힘.** 인증 방식 미답은 분명하지만, GitHub 등록 설정을 대조할 정보가 없다는 항목은 맞았습니다. **이는 확정 선례인 Q13 MCP와 같은 정오 구성입니다.** MCP의 추가 S3 설명은 ECR 인증 방식에 대한 답이 아니므로 등급 차이를 정당화하지 못합니다. 시크릿 이름·리전은 대조할 한쪽 자료이지 실제 등록값의 일치를 판단하는 자료가 아닙니다. 근거: [.github/workflows/actions.yml:19–28](../../../../target/HotDealWave/.github/workflows/actions.yml#L19).

규칙 적용 칸 8개입니다.

9. **Q03 ask — 동의: 부분·밝힘.** 필요한 설정 범주는 설명했지만, Compose가 컨테이너 환경변수로 전달하는 구조와 배포 실행 지점 설명은 빠졌습니다. 실제 값의 공급 원본을 모른다는 판단과 구분해야 합니다. 근거: [docker-compose.yml:31](../../../../target/HotDealWave/docker-compose.yml#L31), [.github/workflows/actions.yml:87–95](../../../../target/HotDealWave/.github/workflows/actions.yml#L87). EC2의 구체적인 디렉터리명까지 필수로 요구할 필요는 없습니다.

10. **Q04 ask — 동의: 틀림·밝힘.** 실제 운영값은 몰라도 확인을 시작할 변수명과 주입 지점은 제시할 수 있는데, 이를 전부 답하지 못했습니다. 근거: [user/application-prod.yml:3](../../../../target/HotDealWave/user/src/main/resources/application-prod.yml#L3), [docker-compose.yml:37–44](../../../../target/HotDealWave/docker-compose.yml#L37).

11. **Q05 ask — 동의: 부분·밝힘.** 상품 필드는 맞지만 질문이 직접 요구한 페이지 정보와 빈 결과 형식을 빠뜨렸습니다. 근거: [ProductApplication.java:13](../../../../target/HotDealWave/product/src/main/java/com/sparta/hotdeal/product/ProductApplication.java#L13), [ProductService.java:179–188](../../../../target/HotDealWave/product/src/main/java/com/sparta/hotdeal/product/application/service/product/ProductService.java#L179).

12. **Q07 ask — 판정 동의: 부분·밝힘. 설명은 정정.** 답 원문에 이미 **“컨트롤러가 data=null인 201 응답을 반환”**한다고 나옵니다. 따라서 ‘201 누락’은 빼야 합니다. 빠진 것은 성공 응답 전 트랜잭션 저장·실패 시 롤백과 오류 처리입니다. 이는 Q06·Q15의 처리 시점 누락 선례와 일관됩니다. 근거: [CouponIssueHelper.java:26–53](../../../../target/HotDealWave/coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponIssueHelper.java#L26), [GlobalExceptionHandler.java:34](../../../../target/HotDealWave/coupon/src/main/java/com/sparta/hotdeal/coupon/application/exception/GlobalExceptionHandler.java#L34).

13. **Q12 ask — 동의: 부분·밝힘.** 상태 덮어쓰기는 이미 설명했으므로 완전한 미답으로 세면 안 됩니다. 다만 재시도 0회 설정을 놓치고 중복 처리 결론도 확인 불가로 남겨 부분은 타당합니다. 근거: [ConsumerApplicationKafkaConfig.java:67](../../../../target/HotDealWave/order/src/main/java/com/sparta/hotdeal/order/infrastructure/config/ConsumerApplicationKafkaConfig.java#L67), [Order.java:70](../../../../target/HotDealWave/order/src/main/java/com/sparta/hotdeal/order/domain/entity/order/Order.java#L70).

14. **Q17 ask — 반박: 부분·밝힘 → 맞음·밝힘.** Gmail SMTP 주소·계정 변수는 접속 설정이며, 질문한 인증 상태·발송 제한·수신 거부 기록의 확인 위치를 알려주지는 않습니다. 또한 메일 메서드의 실패 로그는 `DEBUG`인데 루트는 `INFO`이므로, 그 로그가 Loki에 남는다고 볼 수 없습니다. 이것이 모든 실패 로그의 부재를 뜻하지는 않지만, **Loki 전송 설정만으로 질문한 기록의 확인 위치를 확보한 것도 아닙니다.** 근거: [user/application.yml:7–11](../../../../target/HotDealWave/user/src/main/resources/application.yml#L7), [MailServiceImpl.java:23](../../../../target/HotDealWave/user/src/main/java/com/sparta/hotdeal/user/infrastructure/service/MailServiceImpl.java#L23), [logback.xml:14](../../../../target/HotDealWave/user/src/main/resources/logback.xml#L14). Q09도 콘솔의 **확인 위치**를 물었습니다. 따라서 “위치를 물었으므로 Q09와 다르다”는 구분은 성립하지 않습니다.

15. **Q19 ask — 동의: 틀림·밝힘.** 라우트·경로 재작성·인증·헤더 변경이라는 핵심 항목을 모두 답하지 못했습니다. 근거: [gateway/application.yml:13](../../../../target/HotDealWave/gateway/src/main/resources/application.yml#L13), [AuthenticationFilter.java:24–46](../../../../target/HotDealWave/gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java#L24).

16. **Q20 ask — 동의: 틀림·밝힘.** Gateway의 본문 없는 401 종료와 서비스 예외 처리기의 오류 응답 생성을 모두 놓쳤습니다. 근거: [AuthenticationFilter.java:30](../../../../target/HotDealWave/gateway/src/main/java/com/sparta/hotdeal/gateway/filter/AuthenticationFilter.java#L30), [company/GlobalExceptionHandler.java:23](../../../../target/HotDealWave/company/src/main/java/com/sparta/hotdeal/company/application/exception/GlobalExceptionHandler.java#L23).

**반박한 칸 목록**

- **판정 변경:** Q13 ask → **부분·밝힘**, Q17 ask → **맞음·밝힘**.
- **판정 유지·설명 정정:** Q02 base의 ‘양쪽 모두 ACK’, Q07 ask의 ‘201 누락’.

