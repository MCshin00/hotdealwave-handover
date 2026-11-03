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

    private static int count(String text, String part) {
        return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
    }

    @Test void endpointPageShowsResponseCheckAndItemsWithTrapsFirst() throws Exception {
        assertTrue(page("/endpoints/product-service.ReviewController.getReviewList").contains("응답 기록 있음 500"));

        String createOrder = page("/endpoints/order-service.OrderController.createOrder");
        assertEquals(10, count(createOrder, "class=\"item\""));
        assertTrue(createOrder.indexOf("[함정 · 미확인]") < createOrder.indexOf("[업무 규칙 · 미확인]"));
        assertTrue(createOrder.contains("order/src/main/java/com/sparta/hotdeal/order/presentation/controller/OrderController.java:40"));
    }

    @Test void serviceAndTopicPagesShowAttachedItems() throws Exception {
        String product = page("/services/product-service");
        assertTrue(product.contains("리뷰 GET은 인증 제외 분기 때문에 JWT가 있어도 사용자 헤더를 생성하지 않는다"));
        assertTrue(product.contains("href=\"/topics/KAFKA_REDUCE_QUANTITY_TOPIC\""));
        String literal = page("/topics/payment-dlq");
        assertTrue(literal.contains("문자열로 지정한 DLQ 토픽 두 개를 받는 쪽이 없다"));
        assertTrue(literal.contains("[함정 · 확인함]"));
    }

    @Test void unresolvedValuesShowStatusAndReason() throws Exception {
        assertTrue(page("/topics/KAFKA_DLQ_TOPIC").contains("저장소에 근거 없음"));
        String unlinked = page("/topics/KAFKA_REQUEST_PRODUCT_TOPIC");
        assertTrue(unlinked.contains("확인 안 됨"));
        assertTrue(unlinked.contains("구독자 확인 안 됨: order-service, spring.kafka.topics.request-product, KAFKA_REQUEST_PRODUCT_TOPIC"));
    }
}
