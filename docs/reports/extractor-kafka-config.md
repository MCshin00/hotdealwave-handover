# 추출기 보고서: Kafka·설정·외부 자원

이슈 #5(2단계 뒷부분)의 결과다. 분석 대상 `cc43016`에서 Kafka 발행 13개, 구독 9개, 연결 9개, 환경변수 항목 292개(이름 54개), 외부 자원 15개, 배포 단계 5개를 `out/analysis.json`으로 뽑았다. 새로 더한 교차 검증 두 가지에서 나온 경고는 11개다. 숫자는 `./gradlew :extractor:run`으로 다시 만들 수 있다.

파서 추출기(`extractor/`)는 #18에서 지웠다. 링크는 지우기 전 커밋을 가리킨다.

## 1. 추출기가 하는 일

| 단계 | 읽는 것 | 하는 일 | 나오는 것 |
|---|---|---|---|
| 1 | `application*.yml`, `docker-compose.yml`, `actions.yml` | 서비스·프로필별로 읽는다 | 프로필 base·local·prod, compose, workflow |
| 2 | `@KafkaListener` | `topics`의 `${spring.kafka.topics.x}`를 읽는다 | 구독 9 |
| 3 | `kafkaTemplate.send`, `new DeadLetterPublishingRecoverer` | 토픽 인자를 그 서비스 안에서 거꾸로 따라가 `@Value` 필드까지 찾는다 | 발행 13 |
| 4 | 2·3의 결과 | 토픽 키 → `application.yml`의 `${ENV}` → compose의 그 서비스 줄을 잇고, 환경변수 이름이 같은 발행과 구독을 연결한다 | 연결 9 |
| 5 | 설정 키, 코드의 클래스·어노테이션, `actions.yml` | 환경변수 이름, 외부 자원, 배포 단계를 뽑는다 | 환경변수 292, 자원 15, 배포 5 |
| 6 | 2~4의 결과 + **정규식으로 따로 센 선언 목록** | 교차 검증 2가지를 더 돌린다 | 경고 11 |

- 토픽의 실제 이름은 환경변수 값이라 저장소에 없다. 그래서 토픽 이름이 아니라 환경변수 이름으로 잇는다. 이름이 비슷해도 잇지 않는다.
- 코드에 문자열로 쓴 토픽(`"payment-dlq"`)은 이름이 `확인됨`이다. 환경변수로 구독하는 쪽과는 값을 알 수 없어서 잇지 않는다.
- 값은 내지 않는다. 환경변수 292개 모두 값은 `저장소에 근거 없음`이다.

## 2. 코드 구조

[앞 보고서](extractor-rest-feign-gateway.md)의 패키지에 두 개를 더했다.

| 패키지 | 하는 일 | 주요 파일 |
|---|---|---|
| `kafka` | 발행·구독 추출, 토픽 추적, 연결, Kafka 교차 검증 | `KafkaExtractor`, `KafkaChecks` |
| `config` | 프로필별 설정, 환경변수, 외부 자원, 배포 단계 | `ConfigExtractor` |

`analysis.json`에 최상위 키 7개(`kafkaDeclarations`, `kafkaPublications`, `kafkaSubscriptions`, `kafkaLinks`, `environmentVariables`, `externalResources`, `deploymentSteps`)가 늘었다. 구조는 [extractor/README.md](https://github.com/MCshin00/hotdealwave-handover/blob/f2753bb10ce4c3abeae1932ab8281e4b48ca51f0/extractor/README.md)에 있다.

## 3. 추출 결과

| 항목 | 개수 | 상태 |
|---|---|---|
| Kafka 선언(정규식 / 추출) | 19 / 19 | 차이 없음 |
| 발행 | 13 (`send` 10, DLQ 3) | 모두 확인됨 |
| 구독 | 9 | 모두 확인됨 |
| 발행↔구독 연결 | 9 | 확인됨 |
| 환경변수 항목 / 이름 | 292 / 54 | 이름 확인됨, 값은 저장소에 근거 없음 |
| 외부 자원 | 15 | 확인됨, 접속 값은 저장소에 근거 없음 |
| 배포 단계 | 5 | 확인됨, EC2 주소·사용자·키는 저장소에 근거 없음 |

| Kafka 선언 | 개수 |
|---|---|
| `@KafkaListener` | 9 |
| `kafkaTemplate.send` | 7 |
| `DeadLetterPublishingRecoverer` | 3 |

`send`는 7줄인데 발행은 10개다. order `OrderEventProducer.java:62`는 래퍼 하나로 토픽 3개를, payment `PaymentEventProducer.java:52`는 토픽 2개를 보낸다. 발행은 토픽마다 한 항목으로 센다. user의 `MailServiceImpl.java:21`은 메일 발송이라 세지 않는다.

| 서비스 | 발행 | 구독 |
|---|---|---|
| order | 5 | 3 |
| payment | 4 | 4 |
| product | 4 | 2 |

## 4. Kafka 흐름

연결된 9개다. 6개가 DLQ다.

| 발행 | 토픽 키 / 환경변수 | 구독 |
|---|---|---|
| order `OrderEventProducer.java:62` | `cancel-payment` / `KAFKA_CANCEL_PAYMENT_TOPIC` | payment `PaymentCancelEventConsumer.java:25` |
| order `OrderEventProducer.java:62` | `refund-payment` / `KAFKA_REFUND_PAYMENT_TOPIC` | payment `PaymentRefundEventConsumer.java:25` |
| payment `PaymentEventProducer.java:52` | `update-order-status` / `KAFKA_UPDATE_ORDER_STATUS_TOPIC` | order `OrderStatusUpdateEventConsumer.java:25` |
| order `OrderDlqProducer.java:31`, order `ConsumerApplicationKafkaConfig.java:61`, payment `PaymentDlqProducer.java:31` | `dlq` / `KAFKA_DLQ_TOPIC` | order `OrderDlqConsumer.java:16`, payment `PaymentDlqConsumer.java:16` (발행 3 × 구독 2) |

연결되지 않은 것은 11개다.

| 구독자 없는 발행 | 토픽 키 / 환경변수 |
|---|---|
| order `OrderEventProducer.java:62` | `request-product` / `KAFKA_REQUEST_PRODUCT_TOPIC` |
| payment `PaymentEventProducer.java:52` | `rollback-reduce-quantity` / `KAFKA_ROLLBACK_REDUCE_QUANTITY_TOPIC` |
| payment `ConsumerApplicationKafkaConfig.java:58` | 문자열 `payment-dlq` |
| product `ConsumerApplicationKafkaConfig.java:58` | 문자열 `product-dlq` |
| product `ProductKafkaProducer.java:14`, `:22` | `request-order` / `KAFKA_REQUEST_ORDER_TOPIC` |
| product `ProductKafkaProducer.java:18` | `request-payment` / `KAFKA_REQUEST_PAYMENT_TOPIC` |

| 발행자 없는 구독 | 토픽 키 / 환경변수 |
|---|---|
| order `OrderCancelEventConsumer.java:25` | `cancel-order` / `KAFKA_CANCEL_ORDER_TOPIC` |
| payment `PaymentReadyEventConsumer.java:27` | `ready-payment` / `KAFKA_READY_PAYMENT_TOPIC` |
| product `ProductKafkaConsumer.java:29` | `reduce-quantity` / `KAFKA_REDUCE_QUANTITY_TOPIC` |
| product `ProductKafkaConsumer.java:57` | `restore-quantity` / `KAFKA_RESTORE_QUANTITY_TOPIC` |

예: order의 재고 차감 요청 (구독자 없는 발행)

```text
OrderEventProducer.java:37   sendMessage(reduceProductQuantityTopic, ...)
OrderEventProducer.java:62   kafkaTemplate.send(topic, key, message)
OrderEventProducer.java:24   @Value("${spring.kafka.topics.request-product}")
application.yml:15           request-product: ${KAFKA_REQUEST_PRODUCT_TOPIC}
docker-compose.yml:156       KAFKA_REQUEST_PRODUCT_TOPIC=${KAFKA_REQUEST_PRODUCT_TOPIC}
구독                         KAFKA_REQUEST_PRODUCT_TOPIC을 쓰는 @KafkaListener 없음 → 경고
```

## 5. 설정과 환경변수

| 프로필 | 읽은 곳 | 항목 |
|---|---|---|
| base | 서비스별 `application.yml` | 91 |
| compose | `docker-compose.yml`의 서비스별 `environment` | 90 |
| prod | `application-prod.yml` | 55 |
| local | `application-local.yml` | 45 |
| workflow | `actions.yml`의 `secrets`·`env` | 11 |

| 서비스 | base | local | prod | compose |
|---|---|---|---|---|
| product | 30 | 12 | 14 | 21 |
| order | 21 | 12 | 12 | 19 |
| payment | 22 | 8 | 8 | 18 |
| coupon | 7 | 3 | 11 | 12 |
| user | 7 | 7 | 7 | 12 |
| company | 1 | 3 | 3 | 5 |
| gateway | 3 | | | 3 |

프로필 파일은 합치지 않고 각각 표시한다. 예를 들어 coupon의 Redis 호스트는 `application-prod.yml:16`에서 `${REDIS_PROD_HOST}`를 읽고, compose에서는 user·coupon·order가 같은 이름을 넘긴다(`docker-compose.yml:41`, `:120`, `:159`).

## 6. 외부 자원

| 서비스 | DB | Redis | Kafka | Elasticsearch | S3 | KakaoPay |
|---|---|---|---|---|---|---|
| order | ● | ● | ● | | | |
| payment | ● | | ● | | | ● |
| product | ● | | ● | ● | ● | |
| coupon | ● | ● | | | | |
| user | ● | ● | | | | |
| company | ● | | | | | |

자원 종류는 설정 키(`spring.datasource.url`, `spring.data.redis`, `spring.kafka.bootstrap-servers` 등)와 코드의 클래스·어노테이션으로 정했다. 항목마다 설정 줄과 코드 줄을 함께 붙였다. 접속 값과 S3 버킷 이름은 `저장소에 근거 없음`이다. 게이트웨이와 Eureka에서는 나온 자원이 없다.

## 7. 배포 경로

`main`에 푸시하면 `.github/workflows/actions.yml`이 돈다.

| 단계 | 근거 | 저장소에 근거 없음 |
|---|---|---|
| ECR 로그인 | `actions.yml:28`, `:84` | |
| 이미지 빌드 | `:57` | |
| 이미지 푸시 | `:60` | |
| EC2 접속 | `:70` | 주소 `:74`, 사용자 `:75`, 키 `:76` |
| compose로 배포 | `:91`, `:94`, `:95` | |

## 8. 교차 검증

| 검사 | 비교 | 경고 |
|---|---|---|
| 선언 목록 | 정규식으로 센 `@*Mapping`·`@FeignClient` ↔ 추출 결과 | 1 |
| Feign 대상 | Feign 메서드 ↔ 대상 서비스 컨트롤러 | 0 |
| 게이트웨이 경로 | 경로 규칙 ↔ 컨트롤러 경로 (양방향) | 0 |
| Kafka 선언 목록 | 정규식으로 센 `@KafkaListener`·`kafkaTemplate.send`·`DeadLetterPublishingRecoverer` ↔ 추출 결과 | 0 |
| Kafka 발행↔구독 | 환경변수 이름 기준 구독자 없는 발행, 발행자 없는 구독 | 11 |

위 세 검사는 앞 보고서와 같다. 발행↔구독 경고 11개는 4절의 연결되지 않은 표 그대로다. 판정은 3단계에서 한다.

## 9. 눈에 띈 것 (판정 전)

- **DLQ 문자열 토픽.** payment와 product의 `DeadLetterPublishingRecoverer`는 문자열 `"payment-dlq"`, `"product-dlq"`로 보낸다(각 `ConsumerApplicationKafkaConfig.java:61`). DLQ 구독은 order·payment의 `KAFKA_DLQ_TOPIC` 하나뿐이다. 두 문자열이 서로 다르므로 둘 다 그 값일 수는 없어, 적어도 하나는 구독자가 없다. 4단계 업무 규칙·함정에서 볼 항목이다(미확인).
- **환경변수 이름이 다른 발행·구독.** 예시의 order `request-product` 발행은 필드 이름이 `reduceProductQuantityTopic`이고, product에는 `reduce-quantity` 구독이 있다. 이름만 보면 짝 같지만 환경변수 이름이 달라, 같은 토픽인지는 값을 봐야 안다. 추출기는 잇지 않고 경고로 남겼다.

## 10. 검증

| 확인 | 결과 |
|---|---|
| `./gradlew :extractor:test` | 13개 통과. 대표 사례는 구현 전에 실패하는 것을 먼저 확인했다 |
| `./gradlew :extractor:run` | 검사별 경고 1, 0, 0, 0, 11과 발행 13, 구독 9, 연결 9 출력 |
| 대표 사례 대조 | order `OrderEventProducer.java:24`·`:37`·`:62`, `application.yml:15`, `docker-compose.yml:156`, coupon `application-prod.yml:16`, payment·product `ConsumerApplicationKafkaConfig.java:58`·`:61`의 출력이 코드와 일치 |
| 분석 대상 | `target/HotDealWave` 변경 없음 |
