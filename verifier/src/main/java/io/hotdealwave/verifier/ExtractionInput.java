package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class ExtractionInput {
    private static final List<String> SERVICE_ARRAYS = List.of("endpoints", "feignClients", "feignCalls",
            "gatewayRoutes", "allowedPaths", "declarations", "kafkaDeclarations", "kafkaPublications",
            "kafkaSubscriptions", "environmentVariables", "externalResources");
    private final ObjectMapper json = new ObjectMapper();

    ObjectNode read(Path directory) throws Exception {
        var factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        var serviceSchema = factory.getSchema(Path.of("docs/format/service.schema.json").toUri());
        var relationsSchema = factory.getSchema(Path.of("docs/format/relations.schema.json").toUri());
        var merged = json.createObjectNode().put("schemaVersion", 1);
        merged.putArray("services");
        SERVICE_ARRAYS.forEach(merged::putArray);
        merged.putArray("kafkaLinks");
        merged.putArray("calls");
        merged.putArray("warnings");

        var services = new ArrayList<JsonNode>();
        try (var files = Files.list(directory.resolve("services"))) {
            for (var file : files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList()) {
                var document = json.readTree(file.toFile());
                validate(serviceSchema, document, "services/" + file.getFileName(), merged);
                services.add(document);
            }
        }
        var relations = json.readTree(directory.resolve("relations.json").toFile());
        validate(relationsSchema, relations, "relations.json", merged);

        services.sort(Comparator.comparing(node -> node.path("service").path("directory").asText()));
        for (var service : services) {
            merged.withArray("services").add(service.path("service"));
            for (var key : SERVICE_ARRAYS) service.path(key).forEach(merged.withArray(key)::add);
        }
        relations.path("kafkaLinks").forEach(merged.withArray("kafkaLinks")::add);
        relations.path("calls").forEach(merged.withArray("calls")::add);
        return merged;
    }

    private void validate(JsonSchema schema, JsonNode document, String file, ObjectNode merged) {
        schema.validate(document).stream().sorted(Comparator.comparing(message -> message.toString()))
                .forEach(message -> Checks.warn(merged, "schema", json.createArrayNode(), file + ": " + message));
    }
}
