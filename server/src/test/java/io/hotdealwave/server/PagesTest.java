package io.hotdealwave.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
class PagesTest {
    @Autowired MockMvc mvc;

    private String page(String path) throws Exception {
        var response = mvc.perform(get(path)).andReturn().getResponse();
        assertEquals(200, response.getStatus(), path);
        response.setCharacterEncoding("UTF-8");
        return response.getContentAsString();
    }

    @Test void endpointPageShowsResponseCheckAndItemsWithTrapsFirst() throws Exception {
        assertTrue(page("/endpoints/product-service.ReviewController.getReviewList").contains("응답 기록 있음 500"));

        String createOrder = page("/endpoints/order-service.OrderController.createOrder");
        assertTrue(createOrder.contains("업무 규칙·함정 10개"));
        assertTrue(createOrder.indexOf("[함정 · AI 해석]") < createOrder.indexOf("[업무 규칙 · AI 해석]"));
        assertFalse(createOrder.contains("미확인"));
        assertTrue(createOrder.contains("order/src/main/java/com/sparta/hotdeal/order/presentation/controller/OrderController.java:40"));
        assertTrue(createOrder.contains("gateway/src/main/resources/application.yml:45"));
    }

    @Test void pagesShowVerificationScope() throws Exception {
        assertTrue(page("/endpoints/order-service.OrderController.createOrder").contains("검증 범위: 코드 + 실제 응답</p>"));
        assertTrue(page("/endpoints/product-service.ReviewController.getReviewList").contains("검증 범위: 코드 + 실제 응답(오류 500)"));
        assertTrue(page("/endpoints/payment-service.PaymentController.readyPayment").contains("검증 범위: 코드만"));
        assertTrue(page("/services/payment-service").contains("검증 범위: 엔드포인트 9개 중 코드 + 실제 응답 1개, 코드 + 실제 응답(오류) 1개, 코드만 7개"));
        assertTrue(page("/services/gateway-service").contains("검증 범위: 코드만(엔드포인트 없음)"));
        assertTrue(page("/topics/KAFKA_DLQ_TOPIC").contains("검증 범위: 코드만"));
    }

    @Test void fieldShowsUnconfirmedTypeStructure() throws Exception {
        assertTrue(page("/endpoints/order-service.OrderController.getOrders").contains("타입 구조: 확인 안 됨 — 외부 타입의 구조를 근거 줄만으로 확인할 수 없음"));
    }

    @Test void descriptionsComeFirstApartFromItems() throws Exception {
        String createOrder = page("/endpoints/order-service.OrderController.createOrder");
        assertTrue(createOrder.indexOf("<h2>설명</h2>") > 0 && createOrder.indexOf("<h2>설명</h2>") < createOrder.indexOf("<h2>요청</h2>"));
        assertTrue(createOrder.contains("[설명 · AI 해석]"));
        assertTrue(page("/topics/KAFKA_REQUEST_PRODUCT_TOPIC").contains("<h2>설명</h2>"));
    }

    @Test void fieldWithoutAnnotationsSaysSo() throws Exception {
        String createOrder = page("/endpoints/order-service.OrderController.createOrder");
        assertTrue(createOrder.contains("addressId  UUID  @NotNull  확인됨"));
        assertTrue(createOrder.contains("couponId  UUID  어노테이션 없음  확인됨"));
    }

    @Test void serviceAndTopicPagesShowAttachedItems() throws Exception {
        String product = page("/services/product-service");
        assertTrue(product.contains("리뷰 GET은 인증 제외 분기 때문에 JWT가 있어도 사용자 헤더를 생성하지 않는다"));
        assertTrue(product.contains("href=\"/topics/KAFKA_REDUCE_QUANTITY_TOPIC\""));
        String literal = page("/topics/payment-dlq");
        assertTrue(literal.contains("문자열로 지정한 DLQ 토픽 두 개를 받는 쪽이 없다"));
        assertTrue(literal.contains("[함정 · 판정 · 확인함]"));
        assertTrue(page("/services/order-service").contains("order/src/main/java/com/sparta/hotdeal/order/event/producer/OrderEventProducer.java:37"));
    }

    @Test void unresolvedValuesShowStatusAndReason() throws Exception {
        assertTrue(page("/topics/KAFKA_DLQ_TOPIC").contains("저장소에 근거 없음"));
        String unlinked = page("/topics/KAFKA_REQUEST_PRODUCT_TOPIC");
        assertTrue(unlinked.contains("확인 안 됨"));
        assertTrue(unlinked.contains("구독자 확인 안 됨: 같은 환경변수(KAFKA_REQUEST_PRODUCT_TOPIC)를 쓰는 구독을 찾지 못함. 토픽 실제 값은 저장소 밖 환경에서 정해짐 (order-service, spring.kafka.topics.request-product)"));
    }
}
