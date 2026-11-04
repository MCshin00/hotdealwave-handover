package io.hotdealwave.server;

import java.util.List;

/** One model call that returns an answer, the cited chunk numbers and whether it abstained. Tests use a fake. */
interface AskModel {
    record Reply(String answer, List<String> citations, boolean abstain, long inputTokens, long outputTokens) {}

    Reply reply(String instructions, String input);
}
