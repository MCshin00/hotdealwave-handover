package io.hotdealwave.extractor.config;

import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.hotdealwave.extractor.model.Analysis;
import io.hotdealwave.extractor.model.Analysis.*;
import io.hotdealwave.extractor.source.SourceContext;
import io.hotdealwave.extractor.source.YamlSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.nodes.*;
import static io.hotdealwave.extractor.model.Analysis.evidence;

/** Reads only named configuration inputs; scalar values never enter the output. */
public final class ConfigExtractor {
    private static final Pattern ENV = Pattern.compile("\\$\\{([A-Z][A-Z0-9_]*)(?::[^}]*)?}");
    private static final Pattern PROPERTY = Pattern.compile("\\$\\{([\\w.-]+)(?::[^}]*)?}");
    private static final Pattern ACTION_REF = Pattern.compile("\\$\\{\\{\\s*(secrets|env)\\.([A-Z][A-Z0-9_]*)\\s*}}");
    private record Setting(String profile, String key, String text, List<String> evidence) {}
    private record Binding(String name, String text, List<String> evidence) {}
    private final Map<String, List<Setting>> settings = new LinkedHashMap<>();
    private final Map<String, List<Binding>> bindings = new LinkedHashMap<>();

    public void extract(Path root, List<SourceContext> sources, Analysis analysis) throws IOException {
        readCompose(root, analysis);
        for (var source : sources) {
            var entries = new ArrayList<Setting>();
            Path resources = source.service.directory().resolve("src/main/resources");
            try (var files = Files.list(resources)) {
                for (Path file : files.filter(p -> p.getFileName().toString().matches("application(?:-[\\w-]+)?\\.yml"))
                        .sorted().toList()) {
                    String name = file.getFileName().toString();
                    String profile = name.equals("application.yml") ? "base" : name.substring(12, name.length() - 4);
                    var yaml = new YamlSource(root, file);
                    flatten(yaml, yaml.root, "", profile, entries);
                }
            }
            settings.put(source.serviceName(), entries);
            for (var entry : entries) addEnvironment(analysis, source.serviceName(), entry.profile,
                    entry.key, entry.text, entry.evidence);
            var config = new LinkedHashMap<String, List<String>>();
            var code = new LinkedHashMap<String, List<String>>();
            for (var entry : entries) add(config, resource(entry.key), entry.evidence);
            for (var unit : source.units) {
                for (var type : unit.findAll(ClassOrInterfaceType.class)) {
                    String kind = switch (type.getNameAsString()) {
                        case "JpaRepository", "EntityManager", "DataSource" -> "DB";
                        case "RedisTemplate", "StringRedisTemplate", "RedisConnectionFactory", "RedissonClient" -> "Redis";
                        case "KafkaTemplate", "ConsumerFactory", "ProducerFactory", "DeadLetterPublishingRecoverer" -> "Kafka";
                        case "ElasticsearchClient", "ElasticsearchRepository" -> "Elasticsearch";
                        case "S3Client", "AmazonS3" -> "S3";
                        case "KakaoPayClient", "KakaoPayClientPort" -> "KakaoPay";
                        default -> null;
                    };
                    add(code, kind, source.evidence(type));
                }
                for (var annotation : unit.findAll(AnnotationExpr.class)) {
                    String kind = switch (annotation.getName().getIdentifier()) {
                        case "Entity", "Table" -> "DB";
                        case "RedisHash" -> "Redis";
                        case "KafkaListener", "EnableKafka" -> "Kafka";
                        default -> null;
                    };
                    if (annotation.getName().getIdentifier().equals("FeignClient")
                            && annotation.toString().contains("https://open-api.kakaopay.com/")) kind = "KakaoPay";
                    add(code, kind, source.evidence(annotation));
                    if (!annotation.getName().getIdentifier().equals("Value")) continue;
                    var matcher = PROPERTY.matcher(annotation.toString());
                    while (matcher.find()) {
                        String key = matcher.group(1);
                        add(code, resource(key), source.evidence(annotation));
                        for (var entry : entries) if (entry.key.equals(key))
                            addEnvironment(analysis, source.serviceName(), entry.profile, key, entry.text,
                                    evidence(source.evidence(annotation), entry.evidence));
                        if (key.matches("[A-Z][A-Z0-9_]*"))
                            addEnvironment(analysis, source.serviceName(), "base", "@Value", "${" + key + "}", source.evidence(annotation));
                    }
                }
            }
            var kinds = new LinkedHashSet<>(config.keySet());
            kinds.addAll(code.keySet());
            for (String kind : kinds) {
                var configurationEvidence = config.getOrDefault(kind, List.of());
                var codeEvidence = code.getOrDefault(kind, List.of());
                var all = evidence(configurationEvidence, codeEvidence);
                var details = new LinkedHashMap<String, Fact<String>>();
                details.put("connectionValues", absent(all));
                if (kind.equals("S3")) details.put("bucketName", absent(all));
                analysis.externalResources.add(new ExternalResource(source.serviceName(), kind,
                        configurationEvidence, codeEvidence, details, Analysis.CONFIRMED, all));
            }
        }
        readDeployment(root, analysis);
    }

    /** A link is allowed only after the exact service-local YAML -> compose identity binding. */
    public Fact<String> topicEnvironment(String service, Fact<String> key) {
        if (!key.known()) return Fact.unknown(key.evidence(), key.reason());
        var found = settings.getOrDefault(service, List.of()).stream()
                .filter(s -> s.profile.equals("base") && s.key.equals(key.value())).toList();
        if (found.size() != 1) return Fact.unknown(key.evidence(), "application.yml 토픽 설정을 하나로 확인하지 못함");
        var setting = found.get(0);
        var ev = evidence(key.evidence(), setting.evidence);
        var matcher = ENV.matcher(setting.text);
        if (!matcher.matches() || !setting.text.equals("${" + matcher.group(1) + "}"))
            return Fact.unknown(ev, "토픽 설정이 단일 환경변수 참조가 아님");
        String name = matcher.group(1);
        var matches = bindings.getOrDefault(service, List.of()).stream().filter(b -> b.name.equals(name)).toList();
        for (var match : matches) ev = evidence(ev, match.evidence);
        if (matches.size() != 1 || !matches.get(0).text.equals("${" + name + "}"))
            return Fact.unknown(ev, "서비스 compose의 동일 이름 환경변수 바인딩을 확인하지 못함");
        return Fact.known(name, ev);
    }

    private static void flatten(YamlSource yaml, Node node, String key, String profile, List<Setting> result) {
        if (node instanceof MappingNode map) for (var tuple : map.getValue()) {
            if (tuple.getKeyNode() instanceof ScalarNode scalar)
                flatten(yaml, tuple.getValueNode(), key.isEmpty() ? scalar.getValue() : key + "." + scalar.getValue(), profile, result);
        }
        else if (node instanceof SequenceNode sequence) {
            for (int i = 0; i < sequence.getValue().size(); i++)
                flatten(yaml, sequence.getValue().get(i), key + "[" + i + "]", profile, result);
        } else if (node instanceof ScalarNode scalar) result.add(new Setting(profile, key, scalar.getValue(), yaml.evidence(node)));
    }

    private void readCompose(Path root, Analysis analysis) throws IOException {
        Path file = root.resolve("docker-compose.yml");
        if (!Files.isRegularFile(file)) return;
        var yaml = new YamlSource(root, file);
        if (!(YamlSource.get(yaml.root, "services") instanceof MappingNode services)) return;
        for (var tuple : services.getValue()) {
            String service = ((ScalarNode) tuple.getKeyNode()).getValue();
            var entries = new ArrayList<Binding>();
            Node environment = YamlSource.get(tuple.getValueNode(), "environment");
            if (environment instanceof SequenceNode sequence) for (Node item : sequence.getValue()) {
                if (!(item instanceof ScalarNode scalar)) continue;
                String[] pair = scalar.getValue().split("=", 2);
                entries.add(new Binding(pair[0], pair.length == 2 ? pair[1] : "", yaml.evidence(item)));
            }
            if (environment instanceof MappingNode mapping) for (var item : mapping.getValue()) {
                if (item.getKeyNode() instanceof ScalarNode key && item.getValueNode() instanceof ScalarNode value)
                    entries.add(new Binding(key.getValue(), value.getValue(), yaml.evidence(key)));
            }
            bindings.put(service, entries);
            for (var entry : entries) {
                addVariable(analysis, service, "compose", entry.name, "environment", entry.evidence);
                var refs = ENV.matcher(entry.text);
                while (refs.find()) if (!refs.group(1).equals(entry.name))
                    addVariable(analysis, service, "compose", refs.group(1), "environment-source", entry.evidence);
            }
        }
    }

    private static String resource(String key) {
        if (key.startsWith("spring.datasource.") || key.startsWith("spring.jpa.")) return "DB";
        if (key.startsWith("spring.data.redis.")) return "Redis";
        if (key.startsWith("spring.kafka.")) return "Kafka";
        if (key.startsWith("spring.elasticsearch.")) return "Elasticsearch";
        if (key.startsWith("s3.")) return "S3";
        if (key.startsWith("pay.")) return "KakaoPay";
        return null;
    }

    private static void add(Map<String, List<String>> map, String key, List<String> ev) {
        if (key != null) map.put(key, evidence(map.getOrDefault(key, List.of()), ev));
    }
    private static Fact<String> absent(List<String> ev) {
        return new Fact<>(null, Analysis.ABSENT, ev, "실제 값은 저장소에 근거 없음");
    }
    private static void addEnvironment(Analysis analysis, String service, String profile, String location, String text, List<String> ev) {
        var matcher = ENV.matcher(text);
        while (matcher.find()) addVariable(analysis, service, profile, matcher.group(1), location, ev);
    }
    private static void addVariable(Analysis analysis, String service, String profile, String name, String location, List<String> ev) {
        analysis.environmentVariables.add(new EnvironmentVariable(service, profile, name, location, absent(ev), Analysis.CONFIRMED, ev));
    }

    private static void readDeployment(Path root, Analysis analysis) throws IOException {
        Path file = root.resolve(".github/workflows/actions.yml");
        if (!Files.isRegularFile(file)) return;
        var yaml = new YamlSource(root, file);
        var lines = Files.readAllLines(file);
        if (!(YamlSource.get(yaml.root, "jobs") instanceof MappingNode jobs)) return;
        var stages = new LinkedHashMap<String, List<String>>();
        var details = new LinkedHashMap<String, Fact<String>>();
        for (var job : jobs.getValue()) for (Node step : YamlSource.items(YamlSource.get(job.getValueNode(), "steps"))) {
            Node uses = YamlSource.get(step, "uses");
            if (uses instanceof ScalarNode action) {
                if (action.getValue().startsWith("aws-actions/amazon-ecr-login@")) add(stages, "ecr-login", yaml.evidence(uses));
                if (action.getValue().startsWith("appleboy/ssh-action@")) {
                    var ev = yaml.evidence(uses);
                    for (String key : List.of("host", "username", "key")) {
                        Node value = YamlSource.get(step, "with", key);
                        if (value != null) {
                            ev = evidence(ev, yaml.evidence(value));
                            details.put(key.equals("host") ? "ec2Address" : key, absent(yaml.evidence(value)));
                        }
                    }
                    Node needs = YamlSource.get(job.getValueNode(), "needs");
                    if (needs != null) ev = evidence(ev, yaml.evidence(needs));
                    add(stages, "ec2-connect", ev);
                }
            }
            for (Node script : Arrays.asList(YamlSource.get(step, "run"), YamlSource.get(step, "with", "script"))) {
                if (!(script instanceof ScalarNode)) continue;
                // EOF without a trailing newline ends inside the final command's line.
                int end = script.getEndMark().getLine() + (script.getEndMark().getColumn() > 0 ? 1 : 0);
                for (int i = script.getStartMark().getLine(); i < end; i++) {
                    String line = lines.get(i).strip();
                    if (line.startsWith("#")) continue;
                    String kind = line.startsWith("docker build ") ? "image-build"
                            : line.startsWith("docker push ") ? "image-push"
                            : line.startsWith("aws ecr get-login-password ") ? "ecr-login"
                            : line.matches("docker-compose (down|pull|up)( .*)?") ? "compose-deploy" : null;
                    add(stages, kind, List.of(".github/workflows/actions.yml:" + (i + 1)));
                }
            }
        }
        for (var stage : stages.entrySet()) analysis.deploymentSteps.add(new DeploymentStep(stage.getKey(),
                stage.getKey().equals("ec2-connect") ? details : Map.of(), Analysis.CONFIRMED, stage.getValue()));
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).strip().startsWith("#")) continue;
            var matcher = ACTION_REF.matcher(lines.get(i));
            while (matcher.find()) addVariable(analysis, "deployment", "workflow", matcher.group(2), matcher.group(1),
                    List.of(".github/workflows/actions.yml:" + (i + 1)));
        }
    }
}
