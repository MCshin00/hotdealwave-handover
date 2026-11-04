package io.hotdealwave.server;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class AskTest {
    static class FakeModel implements AskModel {
        Reply next;
        RuntimeException failure;
        String lastInput;
        int calls;

        @Override public Reply reply(String instructions, String input) {
            calls++;
            lastInput = input;
            if (failure != null) throw failure;
            return next;
        }
    }

    @TestConfiguration
    static class Fake {
        @Bean @Primary FakeModel fakeModel() { return new FakeModel(); }
    }

    @Autowired Ask ask;
    @Autowired FakeModel model;
    @Autowired MockMvc mvc;

    @BeforeEach void reset() { model.next = null; model.failure = null; model.lastInput = null; model.calls = 0; }

    private static AskModel.Reply reply(String answer, boolean abstain, String... citations) {
        return new AskModel.Reply(answer, List.of(citations), abstain, 100, 20);
    }

    @Test void citedFileLinesInCitedChunksMakeAnAnswer() {
        model.next = reply("주문 생성은 OrderController.java:40에서 받는다.", false, "C1");
        var answer = ask.ask("createOrder");
        assertEquals("답", answer.status());
        assertEquals("/endpoints/order-service.OrderController.createOrder", answer.citations().get(0).page());
        assertTrue(model.lastInput.startsWith("조각:\n[C1] order-service.OrderController.createOrder"));
    }

    @Test void citationCheckTurnsBadCitationsIntoAbstain() {
        model.next = reply("주문 생성은 OrderController.java:40에서 받는다.", false, "C9");
        assertEquals("인용 검사: 없는 조각 C9", ask.ask("createOrder").reason());

        model.next = reply("주문 생성은 OrderController.java:41에서 받는다.", false, "C1");
        var answer = ask.ask("createOrder");
        assertEquals("보류", answer.status());
        assertEquals("인용 검사: 인용한 조각에 없는 OrderController.java:41", answer.reason());

        model.next = reply("주문 생성 API다.", false);
        assertEquals("인용 검사: 인용 없음", ask.ask("createOrder").reason());
    }

    @Test void modelAbstainNoResultsAndFailureAreKeptApart() {
        model.next = reply("조각에 없다.", true);
        assertEquals("모델 보류", ask.ask("createOrder").reason());

        var none = ask.ask("zzqxv");
        assertEquals("보류", none.status());
        assertEquals("검색 결과 없음", none.reason());
        assertEquals(1, model.calls);

        model.failure = new IllegalStateException("OPENAI_API_KEY가 없다");
        var failed = ask.ask("createOrder");
        assertEquals("오류", failed.status());
        assertTrue(failed.reason().contains("OPENAI_API_KEY"));
    }

    @Test void fieldQuestionPassesTheRequestSection() {
        model.next = reply("조각에 없다.", true);
        ask.ask("createOrder 요청 필드");
        assertTrue(model.lastInput.contains("basketList  List<UUID>  @NotNull"));
    }

    @Test void apiAskReturnsStatusAnswerCitationsAndUsage() throws Exception {
        model.next = reply("주문 생성은 OrderController.java:40에서 받는다.", false, "C1");
        mvc.perform(post("/api/ask").contentType("application/json").content("{\"question\":\"createOrder\"}"))
                .andExpect(jsonPath("$.status").value("답"))
                .andExpect(jsonPath("$.citations[0].page").value("/endpoints/order-service.OrderController.createOrder"))
                .andExpect(jsonPath("$.inputTokens").value(100));
    }

    @Test void searchPageShowsResultsAndAnswerWithCitedLinks() throws Exception {
        String results = mvc.perform(get("/search").param("q", "createOrder")).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(results.contains("href=\"/endpoints/order-service.OrderController.createOrder\""));

        model.next = reply("조각에 없다.", true);
        String abstained = mvc.perform(post("/search").param("question", "createOrder")).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(abstained.contains("보류·오류 이유: 모델 보류"));

        model.next = reply("주문 생성은 OrderController.java:40에서 받는다.", false, "C1");
        String answered = mvc.perform(post("/search").param("question", "createOrder")).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(answered.contains("주문 생성은 OrderController.java:40에서 받는다."));
        assertTrue(answered.contains("<a href=\"/endpoints/order-service.OrderController.createOrder\">"));
    }

    @Test void missingKeyFailsOnlyTheCall() {
        var error = assertThrows(IllegalStateException.class, () -> new OpenAiModel(null).reply("규칙", "질문"));
        assertTrue(error.getMessage().contains("OPENAI_API_KEY"));
    }
}
