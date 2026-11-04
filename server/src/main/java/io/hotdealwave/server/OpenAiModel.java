package io.hotdealwave.server;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonSchemaLocalValidation;
import com.openai.models.Reasoning;
import com.openai.models.ReasoningEffort;
import com.openai.models.responses.ResponseCreateParams;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** gpt-6-luna through the Responses API with a fixed output format. The key comes from OPENAI_API_KEY. */
@Component
class OpenAiModel implements AskModel {
    static final String MODEL = "gpt-6-luna";
    static final long MAX_OUTPUT_TOKENS = 1500;

    record Output(
            @JsonPropertyDescription("질문에 대한 답. 보류할 때는 확인할 수 없는 이유") String answer,
            @JsonPropertyDescription("근거로 쓴 조각 번호. 예: [\"C1\", \"C3\"]") List<String> citations,
            @JsonPropertyDescription("질문이 묻는 것에 대한 답이 조각에 없으면 true") boolean abstain) {}

    private final OpenAIClient client;

    OpenAiModel(@Value("${OPENAI_API_KEY:#{null}}") String key) {
        this.client = key == null || key.isBlank() ? null : OpenAIOkHttpClient.builder().apiKey(key).build();
    }

    @Override
    public Reply reply(String instructions, String input) {
        if (client == null) throw new IllegalStateException("OPENAI_API_KEY 환경변수가 없어 모델을 부를 수 없다");
        var params = ResponseCreateParams.builder().model(MODEL).instructions(instructions).input(input)
                .reasoning(Reasoning.builder().effort(ReasoningEffort.NONE).build()).maxOutputTokens(MAX_OUTPUT_TOKENS)
                .text(Output.class, JsonSchemaLocalValidation.YES).build();
        var response = client.responses().create(params);
        // Take the first message that carries the structured output; an empty extra message is skipped.
        var output = response.output().stream().flatMap(item -> item.message().stream())
                .flatMap(message -> message.content().stream()).flatMap(content -> content.outputText().stream())
                .findFirst().orElseThrow(() -> new IllegalStateException("모델 응답에 정해진 형식의 답이 없다"));
        var usage = response.usage();
        return new Reply(output.answer(), output.citations(), output.abstain(),
                usage.map(u -> u.inputTokens()).orElse(0L), usage.map(u -> u.outputTokens()).orElse(0L));
    }
}
