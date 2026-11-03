package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class KnowledgeTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path TARGET = Path.of("target/HotDealWave");
    private static final String CODE = "order/src/main/java/com/sparta/hotdeal/order/application/service/order/OrderService.java";
    private static final String RESPONSE = "coupon-service.CouponController.getUserCouponDetail.json";
    @TempDir Path temporary;

    private ObjectNode prepare() throws Exception {
        Path input = Files.createDirectories(temporary.resolve("analysis/hotdealwave/services"));
        Files.copy(Path.of("analysis/hotdealwave/services/order-service.json"), input.resolve("order-service.json"));
        Files.writeString(input.getParent().resolve("relations.json"), "{\"schemaVersion\":1,\"kafkaLinks\":[]}");
        Path responses = Files.createDirectories(input.getParent().resolve("responses"));
        Files.copy(Path.of("analysis/hotdealwave/responses", RESPONSE), responses.resolve(RESPONSE));
        Files.createDirectories(temporary.resolve("knowledge/hotdealwave"));
        var item = JSON.createObjectNode().put("service", "order-service")
                .put("kind", "업무 규칙")
                .put("title", "주문의 쿠폰 적용 처리")
                .put("description", "쿠폰 검증 응답을 받아 주문에 적용한다. 설명의 정오 판정은 개발자가 한다.")
                .put("source", "AI 해석").put("discoveryPath", "실제 응답").put("confirmationStatus", "미확인");
        item.putArray("evidence").add(CODE + ":141").add("analysis/hotdealwave/responses/" + RESPONSE + ":18");
        item.putArray("affectedIds").add("order-service.OrderController.createOrder");
        return item;
    }

    private JsonNode verify(ObjectNode item) throws Exception { return verify(item, TARGET); }

    private JsonNode verify(ObjectNode item, Path target) throws Exception {
        write("order-service", item);
        return run(target);
    }

    private JsonNode run(Path target) throws Exception {
        Path output = temporary.resolve("analysis.json");
        Main.main(new String[]{temporary.resolve("analysis/hotdealwave").toString(), target.toString(),
                output.toString()});
        return JSON.readTree(output.toFile());
    }

    private void write(String service, ObjectNode... items) throws Exception {
        var document = JSON.createObjectNode().put("schemaVersion", 1);
        document.putArray("items").addAll(List.of(items));
        JSON.writeValue(temporary.resolve("knowledge/hotdealwave/" + service + ".json").toFile(), document);
    }

    @Test void evidenceBeyondCodeOrResponseLengthWarnsWithoutChangingConfirmation() throws Exception {
        var item = prepare();
        var output = verify(item);
        assertEquals(0, output.at("/checks/schema/warningCount").asInt());
        assertEquals(0, output.at("/checks/knowledge-evidence/warningCount").asInt());
        assertEquals(item, output.at("/knowledge/0"));

        int pastEnd = Files.readAllLines(TARGET.resolve(CODE)).size() + 1;
        item.put("confirmationStatus", "확인함");
        item.withArray("evidence").set(0, JSON.getNodeFactory().textNode(CODE + ":" + pastEnd));
        output = verify(item);
        assertEquals(1, output.at("/checks/knowledge-evidence/warningCount").asInt());
        assertEquals(item, output.at("/knowledge/0"));
        assertTrue(output.path("warnings").toString().contains(CODE + ":" + pastEnd));

        item.withArray("evidence").set(0, JSON.getNodeFactory().textNode(CODE + ":141"));
        int responseEnd = Files.readAllLines(temporary.resolve("analysis/hotdealwave/responses").resolve(RESPONSE)).size() + 1;
        String invalidResponse = "analysis/hotdealwave/responses/" + RESPONSE + ":" + responseEnd;
        item.withArray("evidence").set(1, JSON.getNodeFactory().textNode(invalidResponse));
        output = verify(item);
        assertEquals(1, output.at("/checks/knowledge-evidence/warningCount").asInt());
        assertEquals(item, output.at("/knowledge/0"));
        assertTrue(output.path("warnings").toString().contains(invalidResponse));
    }

    @Test void environmentFileEvidenceWarnsWithoutReadingIt() throws Exception {
        Path target = Files.createDirectories(temporary.resolve("target"));
        Files.createDirectories(target.resolve(CODE).getParent());
        Files.copy(TARGET.resolve(CODE), target.resolve(CODE));
        Files.writeString(target.resolve(".env"), "SECRET=test\n");
        var item = prepare();
        assertEquals(0, verify(item, target).at("/checks/knowledge-evidence/warningCount").asInt());
        item.withArray("evidence").add(".env:1").add(".ENV:1").add("ENV~1:1");
        var output = verify(item, target);
        assertEquals(3, output.at("/checks/knowledge-evidence/warningCount").asInt());
        assertTrue(output.path("warnings").toString().contains(".env:1"));
        assertTrue(output.path("warnings").toString().contains(".ENV:1"));
        assertTrue(output.path("warnings").toString().contains("ENV~1:1"));
    }

    @Test void sameAffectedIdsInAnotherServiceWarn() throws Exception {
        var item = prepare();
        item.putArray("affectedIds").add("order-service.OrderController.createOrder").add("order-service.OrderController.cancelOrder");
        var sameService = item.deepCopy().put("title", "주문 쪽 다른 동작");
        var other = item.deepCopy().put("service", "product-service").put("title", "상품 쪽 같은 동작");
        other.putArray("affectedIds").add("order-service.OrderController.cancelOrder").add("order-service.OrderController.createOrder");
        write("product-service", other);
        var output = verify(item);
        assertEquals(1, output.at("/checks/knowledge-duplicates/warningCount").asInt());
        assertTrue(output.path("warnings").toString().contains("상품 쪽 같은 동작"));

        write("product-service");
        write("order-service", item, sameService);
        assertEquals(0, run(TARGET).at("/checks/knowledge-duplicates/warningCount").asInt());
    }

    @Test void incorrectItemIsExcludedFromKnowledgeOutput() throws Exception {
        var item = prepare().put("confirmationStatus", "틀림");
        assertEquals(0, verify(item).path("knowledge").size());
    }

    @Test void missingAffectedIdAndRequiredFieldProduceWarnings() throws Exception {
        var item = prepare();
        assertEquals(0, verify(item).at("/checks/knowledge-affected-ids/warningCount").asInt());
        item.putArray("affectedIds").add("order-service.OrderController.missing");
        item.remove("title");
        var output = verify(item);
        assertEquals(1, output.at("/checks/knowledge-affected-ids/warningCount").asInt());
        assertTrue(output.path("warnings").toString().contains("order-service.OrderController.missing"));
        assertEquals(1, output.at("/checks/schema/warningCount").asInt());
        assertTrue(output.path("warnings").toString().contains("title"));
        assertTrue(output.path("warnings").toString().contains("knowledge/hotdealwave/order-service.json"));
        assertEquals(item, output.at("/knowledge/0"));
    }
}
