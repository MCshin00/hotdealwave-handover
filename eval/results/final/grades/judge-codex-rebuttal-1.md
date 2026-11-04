**4칸을 반박합니다: Q15 ask, Q09 ask, Q21 mcp, Q21 ask.** 확인 범위는 8칸 모두 **밝힘**에 동의합니다. 원문과 `cc43016` 코드를 대조했으며, 파일 변경이나 금지 파일 열람은 하지 않았습니다.

1. **Q14 ask — 동의: 틀림·밝힘.**  
   시간대 미확정 외에 시작·종료 조건과 가격 의미를 답하지 못했습니다. 정각 실행·시각 일치 조건과 할인율 계산·판매가 적용은 코드로 확인됩니다. 핵심 설명 대부분이 빠져 Q06의 부분적 누락과 구별됩니다. 근거: `P/application/scheduler/PromotionScheduler.java:27-41,45-59`, `P/application/service/promotion/PromotionService.java:154-165`.

2. **Q15 ask — 반박: 부분·밝힘.**  
   질문은 **MySQL 저장과 ES 반영 사이의 처리 순서**를 묻습니다. 실제로는 트랜잭션 안에서 엔티티 변경 → ES 저장을 수행하므로, DB 커밋 뒤 색인한다는 질문의 전제를 바로잡아야 합니다. 근거: `P/application/service/product/ProductService.java:44,112-124`.  
   또한 “MCP도 같은 점을 말하지 않았다”는 비교는 부정확합니다. [MCP 원문:30](../mcp/Q15.json#L30)은 **“MySQL 커밋 완료 후 실행”으로 단정할 수 없다**고 명시합니다. 처리 시점에 직접 걸리는 누락을 부분으로 본 Q06 선례와도 맞춰야 합니다.

3. **Q16 ask — 동의: 맞음·밝힘.**  
   재시도·누락 보정·순서 보정 구현이 확인되지 않는다는 답은 맞습니다. 색인은 저장 예외를 로그로 처리하며, 재고·할인 경로의 색인 누락 원인까지 설명하라는 질문은 아닙니다. 근거: `P/application/service/product/ProductIndexService.java:19-27,40-55`.

4. **Q09 ask — 반박: 맞음·밝힘.**  
   질문은 **콘솔 확인 위치와 등록값의 운영 일치 여부**를 묻습니다. `PAY_HOST_DOMAIN`과 URL 조합 코드는 실제 운영 주입값이나 콘솔 등록값을 입증하지 않습니다. 근거: [application.yml:29](../../../../target/HotDealWave/payment/src/main/resources/application.yml#L29), [ReqPostKakaoPayReadyDto.java:41](../../../../target/HotDealWave/payment/src/main/java/com/sparta/hotdeal/payment/infrastructure/dto/kakaopay/req/ReqPostKakaoPayReadyDto.java#L41).  
   [답변 원문:6](../ask/Q09.json#L6)의 “등록된 도메인·복귀 주소”를 **코드상 URL 생성 규칙도 없다는 주장**으로 바꿔 읽으면 안 됩니다. 취소·실패 경로 뒤바뀜은 유용한 추가 정보지만 질문의 독립된 요구 항목은 아닙니다.

5. **Q21 mcp — 반박: 맞음·밝힘.**  
   질문은 저장 대상의 목록이 아니라 **Grafana가 그 대상을 실제로 조회하는지**입니다. Prometheus의 `targets`는 수집할 서비스 주소이고, Loki 전송 URL도 `${HOST}`에 의존합니다. 이것만으로 Grafana 연결을 판정할 수 없습니다. 근거: [prometheus.yml:4](../../../../target/HotDealWave/monitoring/prometheus/prometheus.yml#L4), [logback.xml:4](../../../../target/HotDealWave/order/src/main/resources/logback.xml#L4). 저장 대상 설명을 별도 필수 항목으로 추가한 감점은 과합니다.

6. **Q21 ask — 반박: 맞음·밝힘.**  
   같은 질문이므로 MCP와 동일하게 판단해야 합니다. [원문:6](../ask/Q21.json#L6)은 **제공된 조각에 내용이 없다**고 했으며, 저장소 전체에 설정이 없다고 단정하지 않았습니다. Loki의 로컬 저장 경로가 있어도 실제 Grafana 조회 대상과의 일치는 확인되지 않습니다. 근거: [loki-config.yml:10](../../../../target/HotDealWave/monitoring/loki/loki-config.yml#L10). 조각 한정 표현을 허용한 Q16과도 일관되게 판단해야 합니다.

7. **Q18 mcp — 동의: 부분·밝힘.**  
   소진·중복 분기는 맞지만, 질문이 명시적으로 요구한 오류 본문이 빠졌습니다. 본문은 `timestamp/code/message`로 확인되고 만료 후 소프트 삭제 시 CO001 경로도 있습니다. 근거: `C/application/dto/ErrorResponseDto.java:14-19`, `C/application/exception/GlobalExceptionHandler.java:19-24`, `C/application/service/CouponInfoService.java:110-112`.

8. **Q18 ask — 동의: 부분·밝힘.**  
   정상 소진 뒤에는 상태 검사가 먼저 적용되어 CO004인데 CO005로 단순화했고, 오류 본문도 빠졌습니다. 근거: `C/application/service/CouponIssueHelper.java:31-41`, `C/domain/entity/CouponInfo.java:75-76`, `C/application/dto/ErrorResponseDto.java:14-19`. 이는 코드 정확도 문제이므로 확인 범위까지 깎지는 않습니다.

일관성 측면에서, **Q16에서는 질문이 요구하지 않은 누락 원인을 제외하면서 Q09·Q21에서는 대조의 한쪽 설정 설명을 필수로 추가**했습니다. Q13은 질문이 인증 방식을 별도로 명시했으므로 그 미답을 감점한 경우입니다. Q04에서 변수명·주입 경로를 설명한 답을 맞음으로 인정했다고 해서 다른 운영 대조 질문에도 그 설명이 자동으로 필수가 되지는 않습니다.

**반박한 칸: Q15 ask → 부분·밝힘 / Q09 ask, Q21 mcp, Q21 ask → 맞음·밝힘.**