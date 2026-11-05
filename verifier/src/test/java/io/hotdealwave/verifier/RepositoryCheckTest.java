package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class RepositoryCheckTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    @TempDir Path temporary;

    private JsonNode run(Path input) throws Exception {
        Path output = temporary.resolve("analysis.json");
        Main.main(new String[]{input.toString(), "target/HotDealWave", output.toString()});
        return JSON.readTree(output.toFile());
    }

    private static JsonNode only(JsonNode nodes, Predicate<JsonNode> match) {
        var found = Checks.items(nodes).stream().filter(match).toList();
        assertEquals(1, found.size());
        return found.get(0);
    }

    @Test void deployWorkflowAndMonitoringAreChecked() throws Exception {
        var repository = run(Path.of("analysis/hotdealwave")).path("repository");

        var workflow = only(repository.path("workflows"), w -> w.path("file").asText().equals(".github/workflows/actions.yml"));
        assertEquals(Checks.CONFIRMED, workflow.path("status").asText());
        var push = only(workflow.path("triggers"), t -> t.path("event").asText().equals("push"));
        assertEquals("main", Checks.value(push.path("branches").path(0)));
        assertEquals(List.of("AWS_ACCESS_KEY_ID", "AWS_SECRET_ACCESS_KEY", "EC2_HOST", "EC2_PRIVATE_KEY", "EC2_USER"),
                Checks.items(workflow.path("secrets")).stream().map(s -> s.path("name").asText()).sorted().toList());
        assertTrue(Checks.items(workflow.path("secrets")).stream().allMatch(s -> Checks.ABSENT.equals(s.path("secretValue").path("status").asText())));
        // Values written in the workflow stay as written; only Secrets and runtime values are absent.
        var region = only(workflow.path("env"), v -> v.path("name").asText().equals("AWS_REGION"));
        assertEquals("ap-northeast-2", Checks.value(region.path("value")));
        var deploy = only(workflow.path("jobs"), j -> j.path("id").asText().equals("deploy"));
        assertTrue(Checks.items(deploy.path("steps")).stream().flatMap(s -> Checks.items(s.path("commands")).stream())
                .anyMatch(c -> Checks.known(c) && "docker-compose up -d".equals(Checks.value(c))));
        var registry = only(only(deploy.path("steps"), s -> true).path("env"), v -> v.path("name").asText().equals("ECR_REGISTRY"));
        assertEquals("${{ steps.login-ecr.outputs.registry }}", Checks.value(registry.path("value")));

        var gateway = only(repository.path("scrapeTargets"), t -> t.path("job").asText().equals("gateway-service"));
        assertEquals(Checks.CONFIRMED, gateway.path("status").asText());
        assertEquals("gateway-service:19091", Checks.value(gateway.path("address")));
        assertEquals("/actuator/prometheus", Checks.value(gateway.path("metricsPath")));
        assertEquals("15s", Checks.value(gateway.path("interval")));
        assertEquals(7, repository.path("scrapeTargets").size());

        var image = only(repository.path("images"), i -> i.path("composeService").asText().equals("gateway-service"));
        assertEquals("19091", Checks.value(image.path("exposedPort")));

        var logs = only(repository.path("logShipping"), l -> l.path("service").asText().equals("gateway-service"));
        assertEquals("http://${HOST}:3100/loki/api/v1/push", Checks.value(logs.path("url")));
        assertEquals(Checks.ABSENT, logs.path("hostValue").path("status").asText());

        var grafana = only(repository.path("monitoringTools"), t -> t.path("name").asText().equals("Grafana"));
        assertEquals(Checks.ABSENT, grafana.path("runDefinition").path("status").asText());
    }

    @Test void commandNotOnItsLineIsUnknown() throws Exception {
        Path input = Files.createDirectories(temporary.resolve("hotdealwave"));
        Path services = Files.createDirectories(input.resolve("services"));
        try (var files = Files.list(Path.of("analysis/hotdealwave/services"))) {
            for (var file : files.toList()) Files.copy(file, services.resolve(file.getFileName()));
        }
        Files.copy(Path.of("analysis/hotdealwave/relations.json"), input.resolve("relations.json"));
        var repository = (ObjectNode) JSON.readTree(Path.of("analysis/hotdealwave/repository.json").toFile());
        var command = (ObjectNode) Checks.items(repository.path("workflows").path(0).path("jobs")).stream()
                .flatMap(j -> Checks.items(j.path("steps")).stream()).flatMap(s -> Checks.items(s.path("commands")).stream())
                .filter(c -> "docker-compose up -d".equals(c.path("value").asText())).findFirst().orElseThrow();
        command.put("value", "docker-compose up -d --build");
        JSON.writeValue(input.resolve("repository.json").toFile(), repository);

        var analysis = run(input);
        assertTrue(Checks.items(analysis.path("warnings")).stream().anyMatch(w -> w.path("check").asText().equals("evidence-lines")
                && w.path("reason").asText().startsWith("commands:")));
    }
}
