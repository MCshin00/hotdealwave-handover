# 갈린 칸 최종 판정 (심판)

코드는 모두 `target/HotDealWave`의 `cc43016`에서 직접 열어 확인했다. 아래 경로의 접두는 줄였다(`P` = `product/src/main/java/com/sparta/hotdeal/product`, `C` = `coupon/src/main/java/com/sparta/hotdeal/coupon`).

### Q14 ask
- 질문 항목별 대조:
  - 시간 필드의 시간대 → "시간대는 확인되지 않음", start/end는 Timestamp → 응답은 `java.sql.Timestamp`(`P/application/dtos/res/promotion/ResGetPromotionDto.java:18-19`)이고, 저장소에 시간대 지정이 없음(`P/application/service/promotion/PromotionService.java:178-179`의 `Timestamp.valueOf`, `LocalDateTime.now()` `:44`) → 맞음
  - 시작·종료 기준 → "확인되지 않음" → 매 정각 스케줄러가 초 단위로 자른 현재 시각과 `start`/`end`가 정확히 같은 프로모션만 시작·종료 처리(`P/application/scheduler/PromotionScheduler.java:27-32,45-50`, `P/infrastructure/repository/promotion/PromotionRepositoryJpa.java:21,24`) → 코드로 확인되는데 답하지 못함, 틀림
  - 가격 필드 각각의 의미 → discountPrice는 "할인 가격"이라고만 하고 "금액 의미를 구분할 정보가 없다"고 함, discountRate는 언급 없음 → discountPrice는 원가보다 작아야 하는 할인 후 판매가(`PromotionService.java:162-165`, 스케줄러가 상품 할인가로 그대로 넣음 `PromotionScheduler.java:41`), discountRate는 `round((원가-할인가)/원가×100)` 정수 %(`PromotionService.java:154-160`) → 답하지 못함, 틀림
- 판정: 정확도 틀림, 확인 범위 밝힘 (정확도는 Opus, 확인 범위는 Codex)
- 이유: 세 항목 중 맞게 답한 것은 시간대 하나이고, 핵심인 시작·종료 기준과 가격 필드 의미는 코드로 정해지는데 "확인할 수 없다"고 해 받은 사람이 얻은 정보가 거의 없다. 기준의 "핵심 항목에 답하지 못했으면 틀림"에 해당하며, 대부분은 답하고 한 요소만 빠진 Q06 선례(부분)와 다르다. 확인 불가로 잘못 말한 것은 기준상 정확도에서만 깎으므로 확인 범위는 밝힘이다.

### Q15 ask
- 질문 항목별 대조:
  - 수정 후 ES 반영까지의 처리 → 수정 서비스가 `indexProduct`를 직접 호출해 반영, 색인 예외는 로그만 남기고 삼켜 수정 성공 응답이 나갈 수 있음 → `updateProduct`가 `product.update` 뒤 `productIndexService.indexProduct(product)` 호출(`P/application/service/product/ProductService.java:112-122`), `indexProduct`는 문서 변환·`save` 후 예외를 잡아 로그만 남김(`P/application/service/product/ProductIndexService.java:19-27`) → 맞음
  - 상세 조회의 읽기 위치 → JPA 상품 데이터 → `productRepository.findById`(`ProductService.java:166`) → 맞음
  - 검색 조회의 읽기 위치 → ES 상품 문서 → `productSearchRepository.findBySearchAndIds` 결과를 문서 필드로만 변환(`ProductService.java:179,221-243`) → 맞음
  - (참고) 클래스 단위 `@Transactional`(`ProductService.java:44`)이라 색인이 커밋 전에 실행된다는 점은 답에 없음. 그러나 답이 "커밋 뒤 실행"이라고 말하지도 않았고, 같은 점을 말하지 않은 MCP 답을 두 채점자 모두 맞음으로 봤다.
- 판정: 정확도 맞음, 확인 범위 밝힘 (Codex 쪽)
- 이유: 질문이 묻는 세 항목에 모두 코드와 맞게 답했고, "문서 변환·저장"은 "색인 서비스 호출로 변경 내용 반영"에 실질이 담겨 있다. 커밋 시점은 같은 질문의 MCP 답에도 요구하지 않았으므로 질의응답에만 요구하면 기준이 어긋난다. 확인된 내용을 "AI 해석"으로 낮춰 표시한 것은 확인 안 된 것을 확인된 것처럼 말한 경우가 아니므로 확인 범위에서 깎지 않는다.

### Q16 ask
- 질문 항목별 대조:
  - ES 반영 실패 시 재시도 방식 → 재시도 방식은 확인되지 않음, 생성·수정·상태 변경의 색인 실패는 로그만 남기고 삼켜 성공 응답이 나갈 수 있음 → `indexProduct`는 예외를 잡아 로그만 남기고 재시도 없음(`ProductIndexService.java:19-27`), 호출 지점(`ProductService.java:83,122,136`) → 맞음
  - 누락된 변경을 맞추는 방법 → 절차가 확인되지 않음 → 저장소에 대조·재색인·outbox 같은 복구 구현 없음(색인 코드는 `ProductIndexService.java` 하나) → 맞음
  - 순서가 뒤바뀐 변경을 맞추는 방법 → 절차가 확인되지 않음 → 문서에 버전·순번이 없고 같은 ID로 그대로 저장(`ProductIndexService.java:40-55`) → 맞음
- 판정: 정확도 맞음, 확인 범위 밝힘 (Codex 쪽)
- 이유: 질문이 묻는 재시도·누락 보정·순서 보정은 저장소에 없으며 답은 없다고 밝혔으므로 기준의 "근거가 없다고 밝히면 맞음"에 해당한다. Opus가 든 재고·할인 경로의 색인 미호출과 삭제 경로 차이는 누락이 생기는 원인으로, 질문이 묻는 "어떻게 재시도하고 맞추나"의 항목이 아니다. "조각" 범위로 말한 것은 MCP 답의 "MCP 문서에서 확인할 수 없다"와 같은 표현이고 전달된 정보는 코드와 일치한다.

### Q09 ask
- 질문 항목별 대조:
  - 운영 카카오페이 콘솔 확인 위치 → 조각에 없음 → 저장소에는 "카카오페이 연동" 한 줄뿐(`README.md:125`)이고 콘솔 위치·계정 근거 없음 → 맞음(근거 없음을 밝힘)
  - 등록 도메인·복귀 주소가 운영 설정과 일치하나 → 등록 도메인·복귀 주소·일치 여부 모두 조각에 없음 → 콘솔 등록값은 저장소 밖이라 일치 판정 불가가 맞지만, 운영 설정 쪽은 저장소에 있음: 도메인은 `PAY_HOST_DOMAIN` 주입(`payment/src/main/resources/application.yml:29`, `docker-compose.yml:187`), 복귀 주소는 `D/payment/success`·`D/payment/fail`(cancel)·`D/payment/cancel`(fail)로 취소·실패가 뒤바뀜(`payment/.../dto/kakaopay/req/ReqPostKakaoPayReadyDto.java:41-43`, `PaymentViewController.java:23-30`) → 결론(판정 불가)은 맞으나 확인 가능한 복귀 주소·도메인 주입을 "없다"고 함, 일부 빠짐
- 판정: 정확도 부분, 확인 범위 밝힘 (둘 다 아님: Codex 맞음·밝힘, Opus 틀림·일부)
- 이유: 콘솔 위치와 등록값 대조 불가라는 핵심 결론은 맞아 틀림은 아니지만, 비교의 한쪽인 운영 설정(도메인 주입 변수, 복귀 경로와 취소·실패 뒤바뀜)은 코드로 확인되는데 빠졌으므로 맞음도 아니다. 등록값 대조 불가 판단은 맞고 한 항목을 놓친 Q13 MCP 선례(부분)와 같은 형태이며, Q04 선례처럼 변수명·주입 지점은 답에 기대되는 정보다. 확인 가능한 것을 "없다"고 한 것은 정확도에서만 깎으므로 확인 범위는 밝힘이다.

### Q21 mcp
- 질문 항목별 대조:
  - Grafana 데이터 소스 관리 위치 → 확인 불가 → 저장소에 Grafana 프로비저닝·데이터 소스 설정 없음(Grafana는 README 배지 `README.md:58-59`뿐) → 맞음
  - 대시보드 조회 조건 관리 위치 → 확인 불가 → 대시보드 JSON·패널 쿼리 없음 → 맞음
  - 누락된 서비스의 지표·로그 저장 대상을 보고 있나 → 연결을 입증할 근거가 없어 확인 불가, prometheus·loki·logback 검색 결과 없음 → Grafana 쪽은 판정 불가가 맞지만, 저장 대상 쪽은 저장소에 있음: Prometheus 수집 대상 7개 서비스(`monitoring/prometheus/prometheus.yml:4-38`), 각 서비스 로그의 Loki 전송 주소와 라벨(`order/src/main/resources/logback.xml:4,8` 등), Loki 파일시스템 저장(`monitoring/loki/loki-config.yml:10-13`) → 빠짐
- 판정: 정확도 부분, 확인 범위 밝힘 (Opus 쪽)
- 이유: Grafana 쪽 두 항목과 최종 판정 불가 결론은 맞지만, 질문이 직접 묻는 "지표와 로그가 저장된 대상"은 저장소 설정으로 확인되는데 답에 없다. 기준상 문서에 없어서 빠진 것도 빠진 것이고, 질문의 목적어 자체이므로 Q04 선례가 말한 "과한 요구"(워크플로의 EC2·경로)와 다르다. "검색 결과 없음이 부재의 증명은 아니다"라고 스스로 한정했으므로 확인 범위는 밝힘이다.

### Q21 ask
- 질문 항목별 대조:
  - Grafana 데이터 소스 관리 위치 → 확인할 수 없음 → 저장소에 근거 없음 → 맞음
  - 대시보드 조회 조건 관리 위치 → 확인할 수 없음 → 저장소에 근거 없음 → 맞음
  - 누락된 서비스의 지표·로그 저장 대상을 보고 있나 → 저장 대상에 관한 내용이 조각에 없음 → `monitoring/prometheus/prometheus.yml:4-38`, `order/src/main/resources/logback.xml:4,8`, `monitoring/loki/loki-config.yml:10-13`에 있음 → 빠짐
- 판정: 정확도 부분, 확인 범위 밝힘 (정확도는 Opus, 확인 범위는 Codex)
- 이유: Q21 MCP와 같은 이유로 Grafana 쪽 결론은 맞지만 확인 가능한 저장 대상이 빠져 부분이다. Opus가 확인 범위를 일부로 깎은 근거("확인되는 것과 저장소 밖 값을 구분하지 않음")는 확인 안 된 것을 확인된 것처럼 말한 경우가 아니어서 기준상 확인 범위 감점 사유가 아니다.

### Q18 mcp
- 질문 항목별 대조:
  - 쿠폰 소진 → 수량 검사에서 400/CO005, 마지막 발급으로 OUT_OF_STOCK이 된 뒤에는 상태 검사에서 400/CO004 → `C/application/service/CouponIssueHelper.java:31-37`, `C/domain/entity/CouponInfo.java:75-76`, `C/application/exception/ErrorCode.java:12-13` → 맞음
  - 중복 발급 → 400/CO006 → `CouponIssueHelper.java:39-41`, `ErrorCode.java:14` → 맞음
  - 발급 기간 종료 → 발급 경로에 만료일 검사 없음, CO008은 검증 로직의 오류 → 맞음. 다만 만료 뒤 스케줄러가 소프트 삭제하면 `findByIdAndDeletedAtIsNull`에서 404/CO001(`C/application/service/SchedulerService.java:25-31`, `C/application/service/CouponInfoService.java:110-112`, `ErrorCode.java:9`) → 빠짐
  - 오류 본문 → 필드명·메시지·JSON 확인 불가 → `ErrorResponseDto(timestamp, code, message)`(`C/application/dto/ErrorResponseDto.java:14-19`)와 `ErrorCode`의 메시지를 핸들러가 그대로 반환(`C/application/exception/GlobalExceptionHandler.java:19-24`) → 확인 가능한데 답하지 못함
- 판정: 정확도 부분, 확인 범위 밝힘 (Opus 쪽; 정확도는 두 채점자 같음)
- 이유: 상태 코드 분기는 정확하지만 오류 본문과 삭제 후 404/CO001이 빠져 부분이다. Codex가 확인 범위를 일부로 깎은 근거는 "확인 가능한 오류 본문을 확인 불가로 처리"인데, 기준은 이를 정확도에서만 깎으라고 정했다. 확인 안 된 것을 확인된 것처럼 말한 곳은 없다.

### Q18 ask
- 질문 항목별 대조:
  - 쿠폰 소진 → 400/CO005로 단정 → 수량 검사 분기는 CO005가 맞지만 정상 소진 뒤에는 OUT_OF_STOCK 상태라 400/CO004(`CouponIssueHelper.java:31-37`, `CouponInfo.java:75-76`) → 일부 코드와 다름
  - 중복 발급 → 400/CO006 → `CouponIssueHelper.java:39-41`, `ErrorCode.java:14` → 맞음
  - 발급 기간 종료 → 상태 코드·본문은 확인 불가, 발급 경로가 만료일을 검사하지 않아 삭제 전이면 발급될 수 있음 → 만료일 미검사는 맞음, 삭제 후 404/CO001은 빠짐
  - 오류 본문 → 필드·메시지 형식 확인 불가 → `ErrorResponseDto.java:14-19`, `GlobalExceptionHandler.java:19-24`로 확인 가능 → 답하지 못함
- 판정: 정확도 부분, 확인 범위 밝힘 (Opus 쪽; 정확도는 두 채점자 같음)
- 이유: 중복 발급과 만료일 미검사는 맞지만 소진의 CO004 경로, 오류 본문, 404/CO001이 빠지거나 단순화되어 부분이다. 소진을 CO005로 단정한 것은 코드 사실의 부정확이라 정확도 문제이지, 저장소 밖 값이나 확인 안 된 연결을 단정한 것이 아니다. 확인 가능한 본문을 확인 불가로 한 것도 기준상 정확도에서만 깎으므로 확인 범위는 밝힘이다.

## 요약

| id | 조건 | 정확도 | 확인 범위 |
|---|---|---|---|
| Q14 | ask | 틀림 | 밝힘 |
| Q15 | ask | 맞음 | 밝힘 |
| Q16 | ask | 맞음 | 밝힘 |
| Q09 | ask | 부분 | 밝힘 |
| Q21 | mcp | 부분 | 밝힘 |
| Q21 | ask | 부분 | 밝힘 |
| Q18 | mcp | 부분 | 밝힘 |
| Q18 | ask | 부분 | 밝힘 |

## 최종 의견

Codex 반박(`codex-rebut.md`)을 칸마다 답 원문과 `cc43016` 코드로 다시 확인했다. 반박 4칸을 모두 받아들인다.

- **Q15 ask → 부분으로 바꾼다.** 처음 판정에서 "MCP 답도 같은 점을 말하지 않았다"고 비교했는데, 이 비교가 틀렸다. MCP 답 원문은 "이 호출 순서를 'MySQL 커밋 완료 후 실행'으로 단정할 근거는 … 확인할 수 없습니다"라고 질문의 전제를 짚었다. 질의응답 답은 "상품 수정 후에는 … `indexProduct`를 호출"이라고만 해 전제를 그대로 둔다. 실제로는 클래스 단위 `@Transactional`(`ProductService.java:44`) 안에서 `product.update` 다음에 바로 ES `save`를 실행한다(`:112-122`, `ProductIndexService.java:22`). 따라서 ES 저장은 MySQL 커밋보다 먼저다. 질문이 묻는 것은 "저장된 뒤 반영되기까지의 처리" 자체이고, 커밋 순서는 그 처리 시점에 직접 걸린다. 처리 시점에 걸리는 조건이 빠지면 부분으로 본 Q06 선례에 맞춘다.
- **Q09 ask → 맞음으로 바꾼다.** 질문은 두 가지를 묻는다. 콘솔을 어디서 확인하는지와, 콘솔 등록값이 운영 설정과 일치하는지다. 두 가지 모두 저장소에 근거가 없다. 답은 이를 밝혔으므로 "근거가 없다고 밝히면 맞음" 기준에 해당한다. 답 원문의 "등록된 도메인·복귀 주소"는 콘솔 등록값을 가리킨다고 읽는 것이 자연스럽고, 코드의 URL 생성 규칙이 없다는 주장으로 볼 근거는 약하다. 취소·실패 경로 뒤바뀜과 `PAY_HOST_DOMAIN` 주입은 유용한 추가 정보다. 그러나 질문이 따로 묻지 않았으므로 요구하지 않는다. 질문이 인증 방식을 따로 물었던 Q13과 다르다.
- **Q21 mcp·ask → 맞음으로 바꾼다.** 셋째 항목은 저장 대상이 어디인지를 묻지 않는다. Grafana가 그 대상을 보고 있는지를 묻는다. 이것은 Grafana 설정이 없는 저장소로는 판정할 수 없다. `prometheus.yml`은 수집 대상만 정한다. Loki 주소는 `${HOST}`에 의존하므로(`order/src/main/resources/logback.xml:4`), 저장소만으로는 실제 저장 인스턴스도 특정되지 않는다. 처음 판정은 대조의 한쪽 설정 설명을 필수로 넣었다. Q16에서 질문 밖 원인을 요구하지 않은 것과 일관되지 않았고, Q04 선례의 "과한 요구"에 가깝다. MCP 답은 검색 결과가 없다는 것이 부재의 증명은 아니라고 스스로 한정했다. 질의응답 답은 "조각에 없다"고만 했다. 둘 다 저장소 밖 연결을 단정하지 않았다.
- **나머지는 처음 판정을 유지한다.** Q14 ask(틀림), Q16 ask(맞음), Q18 mcp·ask(부분)는 Codex도 동의했다. 확인 범위 8칸이 모두 밝힘인 것도 양쪽이 같다.

| id | 조건 | 최종 정확도 | 최종 확인 범위 | 반박 | 이유 |
|---|---|---|---|---|---|
| Q14 | ask | 틀림 | 밝힘 | 반박 없음(동의) | 시작·종료 기준과 가격 의미는 코드로 정해지는데 답하지 못했다. 핵심 항목 대부분이 빠졌다. |
| Q15 | ask | 부분 | 밝힘 | 받아들임 | ES 저장이 커밋 전 같은 트랜잭션 안이라는 점이 빠졌고, "저장된 뒤"라는 전제를 그대로 뒀다. MCP 답은 이 전제를 짚었으므로 처음의 비교가 틀렸다. |
| Q16 | ask | 맞음 | 밝힘 | 반박 없음(동의) | 재시도·누락·순서 보정 장치가 저장소에 없고, 답도 없다고 밝혔다. |
| Q09 | ask | 맞음 | 밝힘 | 받아들임 | 콘솔 위치와 등록값 일치 여부는 둘 다 저장소에 근거가 없고, 답이 이를 밝혔다. 복귀 URL 코드는 질문이 따로 묻지 않은 추가 정보다. |
| Q21 | mcp | 맞음 | 밝힘 | 받아들임 | 질문은 Grafana가 저장 대상을 보고 있는지를 묻고, Grafana 설정이 없는 저장소로는 판정할 수 없다. 저장 대상 설명을 필수로 넣은 것은 과했다. |
| Q21 | ask | 맞음 | 밝힘 | 받아들임 | MCP와 같은 질문이므로 같은 이유를 적용한다. 조각 한정 표현은 Q16과 같게 허용한다. |
| Q18 | mcp | 부분 | 밝힘 | 반박 없음(동의) | 오류 본문(timestamp·code·message)과 삭제 후 404/CO001이 빠졌다. |
| Q18 | ask | 부분 | 밝힘 | 반박 없음(동의) | 소진을 CO005로 단순화했고(정상 소진 뒤에는 CO004), 오류 본문과 404/CO001이 빠졌다. |

## 확인 칸

두 채점이 같은 값을 준 칸이다. AI 값이 기준·선례(위 최종 의견 포함)에 맞는지 코드로 다시 확인했다. 경로 접두는 앞과 같이 줄인다.

### Q02 ask (AI 부분·밝힘)
- 질문 항목별 대조:
  - 차감 뒤 주문 생성 실패 시 재고 복구 방법 → 재고 차감 실패 시 주문 되돌리기 요청을 발행한다고 함. 복구 성공 뒤 주문 취소 요청을 발행한다고 함. 복구 방법은 "확인되지 않음" → 주문 저장(`order/.../OrderService.java:108`)이 Kafka 차감 요청(`:122`)보다 먼저다. 실제 보상 경로는 결제 준비 실패 시의 재고 복구 메시지(`payment/.../PaymentReadyEventConsumer.java:55-61`)이고, 복구는 수량을 다시 더한다(`P/application/service/product/ProductInventoryService.java:47-49,102-103`). 답이 한 말(`ProductKafkaConsumer.java:45-50,66-70`)은 맞지만 순서와 복구 방법은 빠졌다 → 일부만 맞음
  - 복구까지 실패할 때의 처리 → "확인할 수 없음", 다만 일반 예외는 로그와 ACK만 한다고 함 → 복구 실패도 로그·ACK뿐(`ProductKafkaConsumer.java:74-80`). 결제 쪽 복구 요청 실패도 로그뿐(`PaymentReadyEventConsumer.java:62-64`) → 일부만 맞음
  - 확인 범위: 발행→구독 연결과 토픽 값은 확인되지 않는다고 밝혔다. 실제로도 토픽은 서로 다른 환경변수다(`payment/src/main/resources/application.yml:16`, `product/src/main/resources/application.yml:16-17`).
- 판정: 부분·밝힘 (AI 값 유지)
- 이유: 맞게 말한 코드 사실이 있지만, 질문의 핵심인 복구 방법(수량 가산)과 결제 쪽 보상 경로가 빠져 부분이다. 확인 안 된 연결은 확인되지 않는다고 밝혔다.

### Q08 ask (AI 맞음·일부)
- 질문 항목별 대조:
  - 복귀 주소 → `/payment/success` → `ReqPostKakaoPayReadyDto.java:41` → 맞음
  - 거치는 처리 → 성공 화면은 템플릿만 반환한다. 승인은 별도 `POST /api/v1/payments/confirm`(tid, pgToken)에서 하며, COMPLETE로 바꾼 뒤 주문 상태 이벤트를 발행한다 → `PaymentViewController.java:18-20`, `payment-success.html:9`, `PaymentController.java:44-48`, `ReqPostPaymentConfirmDto.java:14-16`, `PaymentService.java:58-65` → 맞음
  - 운영 화면 이동 → 근거가 없어 확인할 수 없음 → 성공 템플릿에 이동 처리가 없고, 승인 API는 DTO를 반환한다 → 맞음
  - 확인 범위: 두 채점자가 일부로 깎은 이유는 세 가지다. 도메인이 `PAY_HOST_DOMAIN`이라는 점을 말하지 않은 것, "조각"으로 한정한 것, 확인된 내용을 "AI 해석"으로 분류한 것. 셋 다 확인 안 된 것을 확인된 것처럼 말한 경우가 아니다. 답은 경로만 말했고, 전체 주소나 도메인 값을 단정하지 않았다. 이벤트도 "발행"까지만 말했다.
- 판정: 맞음·밝힘 (확인 범위를 일부에서 밝힘으로 변경)
- 이유: 정확도는 코드와 맞는다. 확인 범위 감점 사유는 부풀림이 아니라 표현과 누락이므로, 기준상 확인 범위에서 깎을 수 없다.

### Q08 mcp (AI 맞음·밝힘)
- 질문 항목별 대조: 복귀 경로, 성공 템플릿, 별도 승인 API의 내부 처리(`PaymentService.java:58-67`), 주문 서비스 소비를 말했다. 주문 서비스 소비는 발행·구독이 같은 환경변수 이름 `KAFKA_UPDATE_ORDER_STATUS_TOPIC`을 쓴다(`payment/src/main/resources/application.yml:18`, `order/src/main/resources/application.yml:17`, `OrderStatusUpdateEventConsumer.java:25`). 운영 화면 이동은 확인 불가라고 했다. 모두 코드와 맞다.
- 판정: 맞음·밝힘 (유지)
- 이유: 모든 항목이 코드와 맞다. 도메인·포트와 화면 이동은 저장소에서 확인할 수 없다고 구분했다.

### Q12 base (AI 맞음·밝힘)
- 질문 항목별 대조:
  - 재시도 위치 → 애플리케이션 재발행 없음(`PaymentEventProducer.java:51-53`). 소비 실패는 DLQ로 보내고(`OrderStatusUpdateEventConsumer.java:30-56`) 컨테이너는 재시도 0회(`order/.../ConsumerApplicationKafkaConfig.java:67`). DLQ 소비자에는 `@Component`가 없음(`OrderDlqConsumer.java:10-12`) → 맞음
  - 중복 전달 처리 → 걸러내지 않고 `this.status = status`로 덮어씀(`OrderConsumerService.java:26-30`, `Order.java:70-71`) → 맞음
  - 확인 범위: 결제 → 주문 연결은 같은 환경변수 이름이라 코드로 확인된다. Kafka 내부 재시도, 운영 재처리, 실제 재전달은 확인 불가로 구분했다.
- 판정: 맞음·밝힘 (유지)
- 이유: 질문의 두 항목에 코드와 맞게 답했고, 저장소 밖 동작은 단정하지 않았다.

### Q14 mcp (AI 맞음·밝힘)
- 질문 항목별 대조: 시간대 확정 불가(`ResGetPromotionDto.java:18-19`), 정각 스케줄러의 시각 일치(`PromotionScheduler.java:27-32,45-50`, `PromotionRepositoryJpa.java:21,24`), 할인 후 판매가와 반올림 정수 %(`PromotionService.java:154-165`)를 말했다. 앞의 Q14 ask 판정 때 같은 코드를 확인했다.
- 판정: 맞음·밝힘 (유지)
- 이유: 세 항목 모두 코드와 맞다. 시간대·통화는 확인 불가로 구분했다.

### Q02 base (AI 맞음·일부)
- 질문 항목별 대조:
  - 복구 방법 → 주문 저장이 먼저이고 `createOrder`에는 복구가 없다고 함. 결제 준비 실패 시 복구 메시지를 발행하고, 수량을 가산한다고 함 → `OrderService.java:108,122`, `PaymentReadyEventConsumer.java:55-61`, `ProductInventoryService.java:47-49,102-103` → 맞음
  - 복구 실패 처리 → 양쪽 모두 로그·ACK뿐이라고 함 → `PaymentReadyEventConsumer.java:62-64`, `ProductKafkaConsumer.java:74-80` → 맞음
  - 확인 범위: 답은 "상품 서비스가 복구 메시지를 받아 `restoreQuantity()`를 호출"한다고 연결을 사실로 썼다. 그러나 결제 쪽 발행 토픽은 `KAFKA_ROLLBACK_REDUCE_QUANTITY_TOPIC`이다(`payment/src/main/resources/application.yml:16`, `PaymentEventProducer.java:26-27,47`). 상품 쪽 구독 토픽은 `KAFKA_RESTORE_QUANTITY_TOPIC`이다(`product/src/main/resources/application.yml:16`, `ProductKafkaConsumer.java:57`). 서로 다른 환경변수이고 값은 저장소 밖이다.
- 판정: 맞음·일부 (유지)
- 이유: 코드 사실은 모두 맞다. 다만 확인 안 된 서비스 간 연결을 확인된 것처럼 말했으므로, 기준이 정한 확인 범위 감점에 그대로 해당한다.

### Q01 ask (AI 부분·일부)
- 질문 항목별 대조:
  - 서비스 순서 → 장바구니, 주소, 상품·금액 계산, 쿠폰, 저장, Kafka 순서라고 함 → 실제로는 장바구니 조회(`OrderService.java:85`) 다음에 상품·주소를 병렬로 조회한다(`:88-99`). 그다음이 쿠폰 검증·사용(`:102`), 금액 계산(`:105`), 저장(`:108-119`), 차감 메시지(`:122`) 순서다. 병렬 조회가 빠졌고, 금액 계산과 쿠폰의 순서가 코드와 다르다 → 일부 틀림
  - 재고 차감 → Kafka 요청 발행까지만 말함. 상품 서비스의 차감 처리(`ProductKafkaConsumer.java:29-44`)는 없다 → 빠짐
  - 확인 범위: "재고 차감은 주문 데이터 저장·요청 발행 이후의 비동기 경로로 설명되어 있으며"라고 했다. 이는 주문 발행 → 상품 차감 연결을 전제한 말이다. 그러나 주문 쪽 발행 토픽은 `KAFKA_REQUEST_PRODUCT_TOPIC`이고(`order/src/main/resources/application.yml:15`, `OrderEventProducer.java:24-25`), 상품 쪽 구독 토픽은 `KAFKA_REDUCE_QUANTITY_TOPIC`이다(`product/src/main/resources/application.yml:14`, `ProductKafkaConsumer.java:29`). 서로 다른 환경변수다. 답은 전달 성공 보장 불가와 중복 상품의 경우에만 미확인을 밝혔다.
- 판정: 부분·일부 (유지)
- 이유: 순서 일부가 코드와 다르고 차감 처리가 빠져 부분이다. 확인 안 된 토픽 연결을 전제로 설명하면서 일부 경우만 미확인으로 밝혔으므로, 진짜 부풀림이 섞인 일부다.

### Q13 ask (AI 틀림·일부)
- 질문 항목별 대조:
  - ECR 업로드 단계의 AWS 인증 방식 → 정보 없음 → `aws-actions/configure-aws-credentials@v4`에 `secrets.AWS_ACCESS_KEY_ID`·`AWS_SECRET_ACCESS_KEY`를 쓰는 액세스 키 방식이고(`.github/workflows/actions.yml:19-24`), 이어서 `amazon-ecr-login`을 쓴다(`:26-28`). 코드로 확인되는데 답하지 못함 → 틀림
  - GitHub 등록 설정과 워크플로 참조값의 일치 → 정보 없음 → 등록값은 저장소 밖이라 일치 판정 불가는 맞다. 하지만 질문이 명시한 "워크플로에서 참조하는 값"(시크릿 이름 `actions.yml:22-23`, 리전 `:9`)은 저장소에 있다 → 일부만 맞음
  - 확인 범위: 답은 아무것도 단정하지 않았다. 두 채점자가 일부로 깎은 이유는 "확인 가능한 부분을 구분하지 않음"뿐이다.
- 판정: 틀림·밝힘 (확인 범위를 일부에서 밝힘으로 변경)
- 이유: 핵심인 인증 방식을 답하지 못해 틀림이다. 같은 질문에서 등록값 대조 불가 판단 등을 맞힌 Q13 MCP 선례(부분)보다 얻은 정보가 적다. 확인 가능한 것을 확인 불가로 한 것은 정확도에서만 깎으므로 확인 범위는 밝힘이다.

| id | 조건 | AI 값 | 판정 | 유지/변경 |
|---|---|---|---|---|
| Q02 | ask | 부분·밝힘 | 부분·밝힘 | 유지 |
| Q08 | ask | 맞음·일부 | 맞음·밝힘 | 변경(확인 범위) |
| Q08 | mcp | 맞음·밝힘 | 맞음·밝힘 | 유지 |
| Q12 | base | 맞음·밝힘 | 맞음·밝힘 | 유지 |
| Q14 | mcp | 맞음·밝힘 | 맞음·밝힘 | 유지 |
| Q02 | base | 맞음·일부 | 맞음·일부 | 유지 |
| Q01 | ask | 부분·일부 | 부분·일부 | 유지 |
| Q13 | ask | 틀림·일부 | 틀림·밝힘 | 변경(확인 범위) |

## 규칙 적용 칸

모두 질의응답(ask) 칸이다. 정확도는 두 채점이 같았고, 확인 범위는 메인 세션이 "확인할 수 있는 것을 확인 불가로 한 것은 확인 범위가 아니라 정확도에서 깎는다"는 규칙으로 밝힘을 정했다. 칸마다 답 원문에서 확인 안 된 연결이나 저장소 밖 값을 확인된 것처럼 말한 곳을 찾았고, 정확도도 코드로 다시 확인했다.

### Q03 ask (현재 부분·밝힘)
- 부풀림 여부: 실제 값은 "저장소에 근거 없음", 주입 위치는 "확인할 수 없음"이라고 했다. 단정한 곳이 없다.
- 정확도: 필요한 값의 범주(DB·Redis·Kafka·ES·토픽·메일·JWT·결제·관리자·AWS)와 DB 변수명은 Compose 환경변수 목록과 맞다(`docker-compose.yml:32-47` 등, `user/src/main/resources/application-prod.yml:3-5`). 그러나 주입 위치는 저장소에서 확인된다. Compose `environment`가 호스트 변수를 컨테이너에 넘기고(`docker-compose.yml:31-47`), 배포 워크플로가 EC2에서 `docker-compose`를 실행한다(`.github/workflows/actions.yml:87-95`). 답은 이를 확인 불가로 했다. Q04 MCP 선례는 변수명과 Compose 주입 지점을 맞음의 근거로 봤다.
- 판정: 부분·밝힘 (유지)
- 이유: 값 범주는 맞고 주입 위치가 빠져 부분이다. 확인 불가로 한 것은 정확도에서만 깎는다.

### Q04 ask (현재 틀림·밝힘)
- 부풀림 여부: "조각에는 … 없습니다. 따라서 … 답할 수 없습니다"뿐이다. 단정한 곳이 없다. Codex가 일부, Opus가 안 밝힘을 준 이유는 둘 다 "근거가 없다는 결론이 잘못" 또는 "구분하지 않음"이다. 이는 확인 가능한 것을 확인 불가로 한 경우라 확인 범위 감점 사유가 아니다.
- 정확도: 같은 질문의 MCP 선례(맞음)는 접속 변수명(`user/src/main/resources/application-prod.yml:3-5,15-18`)과 Compose 주입 지점(`docker-compose.yml:37-44`)을 맞음의 근거로 봤다. 이 답은 그 어느 것도 답하지 못했다.
- 판정: 틀림·밝힘 (유지)
- 이유: 핵심인 확인 위치에 답하지 못해 틀림이다. 확인 범위는 두 채점 값과 달리 밝힘이 기준에 맞는다.

### Q05 ask (현재 부분·밝힘)
- 부풀림 여부: 래퍼 `ResponseDto<Page<ResGetProductDto>>`(`ProductController.java:138`, `ResponseDto.java:9-10`)와 상품 필드 13개는 코드 사실이다. 페이지 메타데이터와 빈 결과는 "확인 안 됨"이라고 했다. 단정한 곳이 없다.
- 정확도: 페이지 구조는 `VIA_DTO` 직렬화 설정(`ProductApplication.java:13`)으로 확인된다. 빈 결과는 ES 검색 결과가 빈 목록이면 빈 `PageImpl`을 반환하는 것으로 확인된다(`ProductService.java:179-188`, `ProductSearchRepositoryCustomImpl.java:71-73`). 둘 다 빠졌다.
- 판정: 부분·밝힘 (유지)
- 이유: 상품 정보는 맞고, 페이지 정보와 빈 결과 형식이 빠져 부분이다. 확인 불가로 한 것은 정확도에서만 깎는다.

### Q07 ask (현재 부분·밝힘)
- 부풀림 여부: 조회 조건은 `userId`, `deletedAt IS NULL`, `isUsed`(기본 false)이고, 표시값은 `CouponInfo`에서 읽는다고 했다. 모두 코드 사실이다(`CouponController.java:41`, `CouponService.java:123-126`, `CouponMapper.java:16-19`). 실패 처리는 "확인할 수 없음"이라고 했다. 단정한 곳이 없다.
- 정확도: 발급은 한 `@Transactional` 안에서 수량 증가와 저장을 함께 한다(`CouponIssueHelper.java:26,44-53`). 이 처리가 끝나야 201을 반환하고(`CouponController.java:29-35`), 런타임 예외는 500으로 처리한다(`GlobalExceptionHandler.java:34-38`). 질문 후반부인 실패 처리가 코드로 확인되는데 빠졌다.
- 판정: 부분·밝힘 (유지)
- 이유: 조회 기준은 맞고 실패 처리가 빠져 부분이다. 확인 불가로 한 것은 정확도에서만 깎는다.

### Q12 ask (현재 부분·밝힘)
- 부풀림 여부: 리스너가 예외를 DLQ로 보내고(`OrderStatusUpdateEventConsumer.java:30-56`), DLQ 전송 결과를 확인하지 않으며(`OrderDlqProducer.java:31`), 상태를 덮어쓴다(`OrderConsumerService.java:26-30`)고 했다. 모두 코드 사실이다. 재시도 위치와 중복 처리는 "확인되지 않음"이라고 했다. 단정한 곳이 없다.
- 정확도: 컨테이너의 재시도 0회 설정(`order/.../ConsumerApplicationKafkaConfig.java:67`)이 빠졌다. 중복 전달 시 걸러내지 않고 다시 대입한다는 결론(`Order.java:70-71`)도 확인 불가로 남겼다. 덮어쓰기 자체는 말했으므로 중복 처리 항목은 결론만 빠진 상태다.
- 판정: 부분·밝힘 (유지)
- 이유: 맞게 말한 사실은 있지만 두 항목의 결론이 빠져 부분이다. 확인 불가로 한 것은 정확도에서만 깎는다.

### Q17 ask (현재 부분·밝힘)
- 부풀림 여부: 외부 계정 상태·발송 제한·수신 거부·실패 기록의 확인 위치가 없다고 했다. 비동기 발송 실패가 요청 응답에 반영되지 않을 수 있다고 했다. 뒤의 말은 `@Async`(`user/.../MailServiceImpl.java:17-26`)로 확인된다. 단정한 곳이 없다.
- 정확도: 계정 인증 상태, 발송 제한, 수신 거부 기록은 저장소 밖이므로 근거 없음이 맞다. 그러나 질문은 "어디서 확인"을 묻는다. 발송 계정이 Gmail SMTP이고 계정 정보가 환경변수라는 점(`user/src/main/resources/application.yml:7-11`)은 확인 위치를 가리키는 저장소 근거다. 실패 기록도 애플리케이션 로그가 Loki로 간다는 점(`user/src/main/resources/logback.xml:4,8`)이 근거가 된다. 다만 발송 예외 로그는 debug 수준이다(`MailServiceImpl.java:23`). 이것들이 빠졌다. 위치를 묻는 질문이므로 Q04 선례와 같은 성격이고, 일치 여부를 묻는 Q09·Q21과 다르다.
- 판정: 부분·밝힘 (유지)
- 이유: 저장소 밖 항목은 맞게 밝혔지만, 위치를 가리키는 저장소 근거가 빠져 부분이다. 확인 범위는 밝힘이다.

### Q19 ask (현재 틀림·밝힘)
- 부풀림 여부: "확인할 수 없습니다. 조각에는 … 없습니다"뿐이다. 단정한 곳이 없다. Opus가 안 밝힘을 준 이유는 "구분하지 않음"이라 감점 사유가 아니다.
- 정확도: 라우트와 `RewritePath`(`gateway/src/main/resources/application.yml:13-19`)가 있다. JWT 검증 실패 시 401(`gateway/.../AuthenticationFilter.java:24-33`)과 `X-User-*` 헤더 덮어쓰기(`:38-44`)도 코드로 확인된다. 답은 이를 하나도 답하지 못했다.
- 판정: 틀림·밝힘 (유지)
- 이유: 핵심 항목 모두 답하지 못해 틀림이다. 확인 범위는 밝힘이 기준에 맞는다.

### Q20 ask (현재 틀림·밝힘)
- 부풀림 여부: "조각은 Gateway의 환경변수 설정만 다루며 … 확인할 수 없습니다"뿐이다. 단정한 곳이 없다.
- 정확도: Gateway 자체 거부는 본문 없는 401로 바로 끝낸다(`AuthenticationFilter.java:30-33`). 서비스 실패는 각 서비스의 예외 처리기가 응답을 만든다(예: `C/application/exception/GlobalExceptionHandler.java:19-38`). 둘 다 코드로 확인되는데 답하지 못했다.
- 판정: 틀림·밝힘 (유지)
- 이유: 구분 기준과 반환 흐름 모두 답하지 못해 틀림이다. 확인 범위는 밝힘이 기준에 맞는다.

| id | 현재 판정 | 내 판정 | 유지/변경 | 이유 |
|---|---|---|---|---|
| Q03 | 부분·밝힘 | 부분·밝힘 | 유지 | 값 범주는 맞고, Compose 주입 지점을 확인 불가로 해 빠졌다. 부풀림은 없다. |
| Q04 | 틀림·밝힘 | 틀림·밝힘 | 유지 | MCP 선례가 맞음으로 본 변수명·주입 지점을 하나도 답하지 못했다. 단정한 곳이 없어 밝힘이다. |
| Q05 | 부분·밝힘 | 부분·밝힘 | 유지 | VIA_DTO 페이지 구조와 빈 PageImpl 반환이 빠졌다. 말한 내용은 모두 코드 사실이다. |
| Q07 | 부분·밝힘 | 부분·밝힘 | 유지 | 조회 기준은 맞고, 트랜잭션·201·500 처리가 빠졌다. 부풀림은 없다. |
| Q12 | 부분·밝힘 | 부분·밝힘 | 유지 | 재시도 0회 설정과 중복 재대입 결론이 빠졌다. 말한 내용은 코드 사실이다. |
| Q17 | 부분·밝힘 | 부분·밝힘 | 유지 | 위치를 묻는 질문인데 Gmail SMTP·환경변수·Loki 로그 근거가 빠졌다. 부풀림은 없다. |
| Q19 | 틀림·밝힘 | 틀림·밝힘 | 유지 | 라우트·RewritePath·JWT 필터·헤더 변경을 모두 답하지 못했다. 단정한 곳이 없다. |
| Q20 | 틀림·밝힘 | 틀림·밝힘 | 유지 | 401 즉시 종료와 서비스 예외 처리기를 답하지 못했다. 단정한 곳이 없다. |

## 두 번째 최종 의견

Codex의 두 번째 반박(`codex-rebut2.md`)을 답 원문과 `cc43016` 코드로 다시 확인했다. 판정 변경 2칸과 설명 정정 2칸을 모두 받아들인다.

- **Q13 ask → 부분·밝힘으로 바꾼다.** Q13 MCP 답 원문을 다시 열어 봤다. MCP 답도 인증 방식과 시크릿 이름을 "판정할 수 없다"고 했고, 워크플로 근거를 하나도 대지 못했다. 맞힌 것은 "GitHub 등록 설정은 대조할 수 없다"는 판단이다. S3 변수 설명은 ECR 인증에 대한 답이 아니라 혼동을 막는 주의 문장이다. 그러면 질의응답 답과 정오 구성이 같다. 인증 방식(`.github/workflows/actions.yml:19-28`)은 빠졌고, 등록값 대조 불가 판단은 맞았다. 개발자가 MCP에 준 부분을 그대로 따른다. 처음 판정에서 Q14 ask(틀림)와 견준 것은 맞지 않았다. Q14 ask는 핵심 세 항목 중 둘을 답하지 못했고, 이 칸은 두 항목 중 하나를 맞혔다.
- **Q17 ask → 맞음·밝힘으로 바꾼다.** 처음 판정은 "위치를 묻는 질문"과 "일치를 묻는 질문"을 나눴다. 그러나 Q09도 콘솔의 확인 위치를 물었으므로 이 구분은 성립하지 않는다. 일관된 기준은 질문이 묻는 위치가 저장소에 있느냐다. Q04에서는 접속 대상과 인증 정보가 저장소의 변수와 Compose 주입으로 정해진다. Q17에서 묻는 계정 인증 상태·발송 제한·수신 거부 기록은 저장소에 없다. `smtp.gmail.com`과 계정 변수(`user/src/main/resources/application.yml:7-11`)는 접속 설정일 뿐이고, 수신 거부·반송 처리 코드도 없다. 발송 실패 로그는 DEBUG 수준이다(`MailServiceImpl.java:23`). 루트 로그 수준은 INFO이므로(`user/src/main/resources/logback.xml:14`) 이 로그는 Loki로 가지 않는다. 비동기 예외를 받는 별도 처리기도 없다(`user/.../libs/config/AsyncConfig.java`에 `@EnableAsync`만 있음). 따라서 저장소에 확인 위치가 없다고 답한 것은 맞다. 비동기 실패가 응답에 드러나지 않는다는 덧붙인 설명(`MailServiceImpl.java:17-26`)도 맞다.
- **Q02 base 설명을 정정한다(판정 맞음·일부 유지).** "양쪽 모두 로그·ACK"는 틀렸다. 결제 쪽 복구 요청 실패는 로그만 남기고(`PaymentReadyEventConsumer.java:62-64`, 이 리스너에는 ACK가 없다), 상품 쪽 복구 실패는 로그와 ACK를 한다(`ProductKafkaConsumer.java:74-80`). 답 원문도 이렇게 구분했다. 판정은 바뀌지 않는다.
- **Q07 ask 설명을 정정한다(판정 부분·밝힘 유지).** 답 원문에 "컨트롤러가 `data=null`인 201 응답을 반환"한다는 말이 있다. 따라서 "201 누락"은 틀렸다. 빠진 것은 다음 세 가지다. 수량 증가와 저장이 한 트랜잭션이라는 점(`CouponIssueHelper.java:26,44-53`), 이것이 끝나야 응답이 나간다는 점, 실패 시 롤백되고 런타임 예외는 500으로 응답된다는 점(`GlobalExceptionHandler.java:34-38`).
- 나머지 12칸은 Codex도 동의했으므로 앞 절의 판정을 유지한다.

| id | 조건 | 정확도 | 확인 범위 | 반박 처리 | 이유 |
|---|---|---|---|---|---|
| Q02 | ask | 부분 | 밝힘 | 반박 없음(동의) | 수량 가산 복구 방법과 결제 쪽 보상 경로가 빠졌다. 연결 미확인은 밝혔다. |
| Q08 | ask | 맞음 | 밝힘 | 반박 없음(동의) | 복귀 경로, 승인 API 구분, 운영 화면 이동 미확인이 코드와 맞다. 단정한 곳이 없다. |
| Q08 | mcp | 맞음 | 밝힘 | 반박 없음(동의) | 모든 항목이 코드와 맞다. 도메인과 화면 이동은 미확인으로 구분했다. |
| Q12 | base | 맞음 | 밝힘 | 반박 없음(동의) | 재시도 0회·DLQ와 중복 재대입을 맞게 설명했다. 재전달은 구분했다. |
| Q14 | mcp | 맞음 | 밝힘 | 반박 없음(동의) | 정각 시각 일치, 할인 후 판매가, 반올림 할인율이 맞다. 시간대는 미확정이라고 했다. |
| Q02 | base | 맞음 | 일부 | 설명 정정 받아들임(판정 유지) | 서로 다른 토픽 변수 사이의 결제→상품 연결을 사실로 썼다. 실패 처리는 결제 쪽 로그만, 상품 쪽 로그·ACK로 정정했다. |
| Q01 | ask | 부분 | 일부 | 반박 없음(동의) | 병렬 조회와 쿠폰→금액 순서가 틀렸고 차감 처리가 빠졌다. 다른 토픽 변수 사이의 연결을 전제했다. |
| Q13 | ask | 부분 | 밝힘 | 받아들임(틀림→부분) | 인증 방식은 빠졌고 등록값 대조 불가 판단은 맞다. Q13 MCP 선례(부분)와 정오 구성이 같다. |
| Q03 | ask | 부분 | 밝힘 | 반박 없음(동의) | 값 범주는 맞고, Compose 전달 구조와 배포 실행 지점이 빠졌다. |
| Q04 | ask | 틀림 | 밝힘 | 반박 없음(동의) | 변수명과 주입 지점을 전혀 답하지 못했다. 단정한 곳이 없다. |
| Q05 | ask | 부분 | 밝힘 | 반박 없음(동의) | 상품 필드는 맞고, VIA_DTO 페이지 구조와 빈 결과 형식이 빠졌다. |
| Q07 | ask | 부분 | 밝힘 | 설명 정정 받아들임(판정 유지) | 조회 기준과 201은 맞다. 응답 전 트랜잭션 저장, 실패 시 롤백·500 처리가 빠졌다. |
| Q12 | ask | 부분 | 밝힘 | 반박 없음(동의) | 덮어쓰기는 말했지만 재시도 0회 설정과 중복 처리 결론이 빠졌다. |
| Q17 | ask | 맞음 | 밝힘 | 받아들임(부분→맞음) | 묻는 기록의 위치는 저장소에 없다. 실패 로그는 DEBUG라 INFO 루트에서 Loki로 가지 않는다. 근거 없음과 비동기 설명 모두 맞다. |
| Q19 | ask | 틀림 | 밝힘 | 반박 없음(동의) | 라우트, RewritePath, JWT 필터, 헤더 변경을 모두 답하지 못했다. |
| Q20 | ask | 틀림 | 밝힘 | 반박 없음(동의) | Gateway의 401 즉시 종료와 서비스 예외 처리기를 답하지 못했다. |
