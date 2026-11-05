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

class ErrorResponseCheckTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    @TempDir Path temporary;

    private JsonNode run(Path input) throws Exception {
        Path output = temporary.resolve("analysis.json");
        Main.main(new String[]{input.toString(), "target/HotDealWave", output.toString()});
        return JSON.readTree(output.toFile());
    }

    private static JsonNode only(JsonNode nodes, java.util.function.Predicate<JsonNode> match) {
        var found = Checks.items(nodes).stream().filter(match).toList();
        assertEquals(1, found.size());
        return found.get(0);
    }

    @Test void errorCodesHandlersRejectionsAndPageSettingsAreChecked() throws Exception {
        var analysis = run(Path.of("analysis/hotdealwave"));

        var code = only(analysis.path("errorCodes"), c -> c.path("service").asText().equals("coupon-service")
                && c.path("name").asText().equals("NOT_FOUND_COUPON_INFO"));
        assertEquals(Checks.CONFIRMED, code.path("status").asText());
        assertEquals("CO001", Checks.value(code.path("code")));
        assertEquals("NOT_FOUND", Checks.value(code.path("httpStatus")));
        assertEquals("해당 쿠폰 정보 ID가 존재하지 않습니다.", Checks.value(code.path("message")));
        assertEquals("coupon/src/main/java/com/sparta/hotdeal/coupon/application/exception/ErrorCode.java:9", code.path("evidence").path(0).asText());

        var runtime = only(analysis.path("exceptionHandlers"), h -> h.path("id").asText().equals("product-service.GlobalExceptionHandler.handleRuntimeException"));
        assertEquals(Checks.CONFIRMED, runtime.path("status").asText());
        assertEquals("INTERNAL_SERVER_ERROR", Checks.value(runtime.path("httpStatus")));
        assertEquals(Checks.UNKNOWN, runtime.path("message").path("status").asText());
        assertTrue(runtime.path("message").path("reason").asText().contains("던지는 곳"));
        var custom = only(analysis.path("exceptionHandlers"), h -> h.path("id").asText().equals("coupon-service.GlobalExceptionHandler.handleCustomException"));
        assertEquals(Checks.CONFIRMED, custom.path("httpStatus").path("status").asText());
        assertEquals("던진 오류 코드", custom.path("httpStatus").path("source").asText());
        // The user body has no error code field at all.
        var user = only(analysis.path("exceptionHandlers"), h -> h.path("id").asText().equals("user-service.GlobalExceptionHandler.illegalArgumentException"));
        assertEquals(Checks.CONFIRMED, user.path("errorCode").path("status").asText());
        assertEquals("", Checks.value(user.path("errorCode")));

        // The status depends on whether the cause is an ApplicationException.
        var completion = only(analysis.path("exceptionHandlers"), h -> h.path("id").asText().equals("order-service.GlobalExceptionHandler.handleCompletionException"));
        assertEquals(Checks.UNKNOWN, completion.path("httpStatus").path("status").asText());
        assertTrue(completion.path("httpStatus").path("reason").asText().contains("조건에 따라"));

        var rejection = only(analysis.path("gatewayRejections"), r -> true);
        assertEquals(Checks.CONFIRMED, rejection.path("status").asText());
        assertEquals("UNAUTHORIZED", Checks.value(rejection.path("httpStatus")));
        assertEquals("", Checks.value(rejection.path("body")));

        assertEquals(List.of("coupon-service", "order-service", "payment-service", "product-service"),
                Checks.items(analysis.path("pageSerialization")).stream()
                        .filter(p -> Checks.known(p) && "VIA_DTO".equals(Checks.value(p.path("mode"))))
                        .map(p -> p.path("service").asText()).sorted().toList());
    }

    @Test void throwsReachExceptionLinesAndHandlers() throws Exception {
        var analysis = run(Path.of("analysis/hotdealwave"));
        var expired = only(analysis.path("throws"), t -> t.path("fromId").asText().equals("coupon-service.CouponController.validateCoupon")
                && t.path("errorCode").asText().equals("COUPON_EXPIRED"));
        assertEquals(Checks.CONFIRMED, expired.path("status").asText());
        assertEquals("coupon-service.GlobalExceptionHandler.handleCustomException", expired.path("handlerId").asText());
        assertEquals("coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponService.java:64",
                expired.path("evidence").path(expired.path("evidence").size() - 1).asText());

        var owner = only(analysis.path("throws"), t -> t.path("fromId").asText().equals("user-service.AddressController.updateAddress")
                && t.path("errorCode").asText().equals("NOT_ALLOWED_CONTENT"));
        assertEquals(Checks.CONFIRMED, owner.path("status").asText());
        assertEquals("IllegalArgumentException", owner.path("exception").asText());
        assertEquals("user-service.GlobalExceptionHandler.illegalArgumentException", owner.path("handlerId").asText());
        // Exceptions caught on the way (logged, or rethrown as another exception) never reach a handler.
        assertTrue(Checks.items(analysis.path("throws")).stream().map(x -> x.path("evidence").path(x.path("evidence").size() - 1).asText())
                .noneMatch(line -> line.endsWith("OrderEventProducer.java:56") || line.endsWith("PaymentEventProducer.java:40")));
        // The error code is on the line after "new ApplicationException(".
        var company = only(analysis.path("throws"), t -> t.path("fromId").asText().equals("company-service.CompanyController.updateCompany")
                && t.path("evidence").path(t.path("evidence").size() - 1).asText().endsWith("CompanyService.java:46"));
        assertEquals(Checks.CONFIRMED, company.path("status").asText());
        assertTrue(Checks.items(analysis.path("warnings")).stream().noneMatch(w -> w.path("check").asText().equals("error-throws")));
    }

    @Test void throwsEndingElsewhereOrWithUnknownHandlerAreUnknown() throws Exception {
        Path input = Files.createDirectories(temporary.resolve("hotdealwave"));
        Path services = Files.createDirectories(input.resolve("services"));
        try (var files = Files.list(Path.of("analysis/hotdealwave/services"))) {
            for (var file : files.toList()) Files.copy(file, services.resolve(file.getFileName()));
        }
        var relations = (ObjectNode) JSON.readTree(Path.of("analysis/hotdealwave/relations.json").toFile());
        var expired = (ObjectNode) only(relations.path("throws"), t -> t.path("fromId").asText().equals("coupon-service.CouponController.validateCoupon")
                && t.path("errorCode").asText().equals("COUPON_EXPIRED"));
        var evidence = expired.withArray("evidence");
        // The line before the throw names the expiry check but creates no exception.
        evidence.set(evidence.size() - 1, JSON.getNodeFactory().textNode(
                "coupon/src/main/java/com/sparta/hotdeal/coupon/application/service/CouponService.java:63"));
        var owner = (ObjectNode) only(relations.path("throws"), t -> t.path("fromId").asText().equals("user-service.AddressController.updateAddress")
                && t.path("errorCode").asText().equals("NOT_ALLOWED_CONTENT"));
        owner.put("handlerId", "user-service.GlobalExceptionHandler.missing");
        JSON.writeValue(input.resolve("relations.json").toFile(), relations);

        var analysis = run(input);
        var reasons = Checks.items(analysis.path("warnings")).stream()
                .filter(w -> w.path("check").asText().equals("error-throws")).map(w -> w.path("reason").asText()).toList();
        assertTrue(reasons.stream().anyMatch(r -> r.startsWith("마지막 근거가 예외 생성 줄이 아님") && r.contains("COUPON_EXPIRED")));
        assertTrue(reasons.stream().anyMatch(r -> r.startsWith("처리기 ID가 추출 결과에 없음") && r.contains("NOT_ALLOWED_CONTENT")));
    }
}
