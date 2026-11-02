package io.hotdealwave.extractor.scan;

import io.hotdealwave.extractor.model.Analysis.Service;
import io.hotdealwave.extractor.source.YamlSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ServiceScanner {
    public record ServiceSource(Path directory, Path javaRoot, Service model) {}

    public List<ServiceSource> scan(Path root) throws IOException {
        var services = new ArrayList<ServiceSource>();
        try (var directories = Files.walk(root)) {
            for (Path directory : directories.filter(Files::isDirectory).sorted().toList()) {
                Path javaRoot = directory.resolve("src/main/java");
                if (!Files.isRegularFile(directory.resolve("build.gradle")) || !Files.isDirectory(javaRoot)) continue;
                var yaml = new YamlSource(root, directory.resolve("src/main/resources/application.yml"));
                var name = yaml.text(YamlSource.get(yaml.root, "spring", "application", "name"));
                String relative = root.relativize(directory).toString().replace('\\', '/');
                services.add(new ServiceSource(directory, javaRoot, new Service(relative, name,
                        name.status(), name.evidence())));
            }
        }
        return services;
    }
}
