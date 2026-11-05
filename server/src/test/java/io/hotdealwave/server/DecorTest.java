package io.hotdealwave.server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DecorTest {
    @Test void evidenceLinksToTheGivenRepositoryAndResponsesOfAnyTarget() {
        var target = new Analysis.Target("SpartaHub", "https://github.com/13th-SpartaHub/13th-SpartaHub-Backend",
                "caba61c022c068fefb636d2c0a17cfb03fc4f2ca");
        String html = Decor.html("<p class=\"line\">허브 생성 [Hub/src/main/java/A.java:12, responses/hub.json:3]</p>", target);
        assertTrue(html.contains("<a href=\"https://github.com/13th-SpartaHub/13th-SpartaHub-Backend/blob/caba61c022c068fefb636d2c0a17cfb03fc4f2ca/"
                + "Hub/src/main/java/A.java#L12\">"), html);
        assertTrue(html.contains("<a href=\"/responses/hub.json#L3\">"), html);
        assertEquals("caba61c", target.shortCommit());
    }
}
