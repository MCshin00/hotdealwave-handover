package io.hotdealwave.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Main {
    public static void main(String[] args) throws Exception {
        if (args.length > 2) throw new IllegalArgumentException("사용법: [분석 대상 상대경로] [출력 상대경로]");
        Path input = Path.of(args.length > 0 ? args[0] : "target/HotDealWave");
        Path output = Path.of(args.length > 1 ? args[1] : "out/analysis.json");
        var analysis = new Extractor().analyze(input);
        Files.createDirectories(output.toAbsolutePath().getParent());
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output.toFile(), analysis);
        analysis.path("checks").properties().forEach(e -> System.out.println(e.getKey() + ": 경고 "
                + e.getValue().path("warningCount").asInt() + "개"));
        System.out.printf("엔드포인트 %d개, Feign 호출 %d개, 게이트웨이 경로 규칙 %d개%n",
                analysis.path("endpoints").size(), analysis.path("feignCalls").size(), analysis.path("gatewayRoutes").size());
    }
}
