package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Main {
    public static void main(String[] args) throws Exception {
        if (args.length > 3) throw new IllegalArgumentException("사용법: [추출 JSON] [분석 대상] [출력 JSON]");
        Path input = Path.of(args.length > 0 ? args[0] : "out/parser.json");
        Path target = Path.of(args.length > 1 ? args[1] : "target/HotDealWave");
        Path output = Path.of(args.length > 2 ? args[2] : "out/analysis.json");
        var mapper = new ObjectMapper();
        var analysis = new Verifier().verify(mapper.readTree(input.toFile()), target);
        Files.createDirectories(output.toAbsolutePath().getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), analysis);
        analysis.path("checks").properties().forEach(e -> System.out.println(e.getKey() + ": 경고 "
                + e.getValue().path("warningCount").asInt() + "개"));
    }
}
