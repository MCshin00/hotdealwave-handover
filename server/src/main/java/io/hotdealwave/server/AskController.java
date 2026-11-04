package io.hotdealwave.server;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AskController {
    record Question(String question) {}

    private final Ask ask;

    AskController(Ask ask) { this.ask = ask; }

    @PostMapping("/api/ask")
    Ask.Answer ask(@RequestBody Question question) { return ask.ask(question.question()); }
}
