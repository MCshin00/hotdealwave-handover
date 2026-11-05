package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class ExtractionInput {
    private static final List<String> SERVICE_ARRAYS = List.of("endpoints", "httpClients", "httpCalls",
            "gatewayRoutes", "allowedPaths", "declarations", "messageDeclarations", "messagePublications",
            "messageSubscriptions", "environmentVariables", "externalResources", "errorCodes", "exceptionHandlers",
            "gatewayRejections", "pageSerialization", "unsupported");
    private final ObjectMapper json = new ObjectMapper();

    ObjectNode read(Path directory) throws Exception {
        var factory = registry();
        var serviceSchema = factory.getSchema(schema("service"));
        var relationsSchema = factory.getSchema(schema("relations"));
        var merged = json.createObjectNode().put("schemaVersion", 1);
        merged.putArray("services");
        SERVICE_ARRAYS.forEach(merged::putArray);
        merged.putArray("messageLinks");
        merged.putArray("calls");
        merged.putArray("throws");
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
        var target = json.readTree(directory.resolve("target.json").toFile());
        validate(factory.getSchema(schema("target")), target, "target.json", merged);
        merged.set("target", target);

        services.sort(Comparator.comparing(node -> node.path("service").path("directory").asText()));
        for (var service : services) {
            merged.withArray("services").add(service.path("service"));
            for (var key : SERVICE_ARRAYS) service.path(key).forEach(merged.withArray(key)::add);
        }
        relations.path("messageLinks").forEach(merged.withArray("messageLinks")::add);
        relations.path("calls").forEach(merged.withArray("calls")::add);
        relations.path("throws").forEach(merged.withArray("throws")::add);
        var repository = directory.resolve("repository.json");
        if (Files.exists(repository)) {
            var document = json.readTree(repository.toFile());
            validate(factory.getSchema(schema("repository")), document, "repository.json", merged);
            merged.set("repository", document);
        }
        return merged;
    }

    /** Schemas ship inside the jar, so the verifier runs from any folder. */
    static SchemaLocation schema(String name) {
        return SchemaLocation.of("classpath:format/" + name + ".schema.json");
    }

    static SchemaRegistry registry() {
        return SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
    }

    private void validate(Schema schema, JsonNode document, String file, ObjectNode merged) {
        schema.validate(document).stream().sorted(Comparator.comparing(message -> message.toString()))
                .forEach(message -> Checks.warn(merged, "schema", "schema-invalid", json.createArrayNode(), file + ": " + message));
    }
}
