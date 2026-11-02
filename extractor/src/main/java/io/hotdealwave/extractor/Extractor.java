package io.hotdealwave.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hotdealwave.extractor.check.CrossChecks;
import io.hotdealwave.extractor.config.ConfigExtractor;
import io.hotdealwave.extractor.kafka.KafkaExtractor;
import io.hotdealwave.extractor.kafka.KafkaChecks;
import io.hotdealwave.extractor.feign.FeignExtractor;
import io.hotdealwave.extractor.gateway.GatewayExtractor;
import io.hotdealwave.extractor.link.RestLinker;
import io.hotdealwave.extractor.model.Analysis;
import io.hotdealwave.extractor.rest.RestExtractor;
import io.hotdealwave.extractor.scan.ServiceScanner;
import io.hotdealwave.extractor.source.SourceContext;
import java.nio.file.Path;
import java.util.ArrayList;

public final class Extractor {
    public JsonNode analyze(Path root) throws Exception {
        root = root.toAbsolutePath().normalize();
        var analysis = new Analysis();
        var sources = new ArrayList<SourceContext>();
        for (var service : new ServiceScanner().scan(root)) {
            analysis.services.add(service.model());
            var source = new SourceContext(root, service);
            sources.add(source);
            new RestExtractor().extract(source, analysis);
            new FeignExtractor().extract(source, analysis);
            if (service.model().directory().equals("gateway")) new GatewayExtractor().extract(root, source, analysis);
        }
        new RestLinker().link(analysis);
        new CrossChecks().run(sources, analysis);
        var config = new ConfigExtractor();
        config.extract(root, sources, analysis);
        for (var source : sources) new KafkaExtractor(source, config).extract(analysis);
        KafkaExtractor.link(analysis);
        new KafkaChecks().run(sources, analysis);
        return new ObjectMapper().valueToTree(analysis);
    }
}
