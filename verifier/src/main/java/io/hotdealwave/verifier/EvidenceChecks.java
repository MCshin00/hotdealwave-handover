package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import static io.hotdealwave.verifier.Checks.*;

final class EvidenceChecks {
    private static final Pattern ENV = Pattern.compile("\\$\\{[A-Z][A-Z0-9_]*}");
    private static final Pattern QUOTED = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern MAPPING = Pattern.compile("@(?:\\w*Mapping|FeignClient)\\b");
    private final SourceText source;
    private final ObjectNode analysis;

    EvidenceChecks(SourceText source, ObjectNode analysis) { this.source = source; this.analysis = analysis; }

    void run() throws IOException {
        for (var entry : analysis.properties()) {
            if (Set.of("checks", "warnings").contains(entry.getKey())) continue;
            visit(entry.getValue(), entry.getKey(), analysis);
        }
        count(analysis, "evidence-lines");
    }

    private void visit(JsonNode node, String role, JsonNode parent) throws IOException {
        if (node.isArray()) { for (var child : node) visit(child, role, parent); return; }
        if (!(node instanceof ObjectNode object)) return;
        for (var entry : new ArrayList<>(object.properties())) visit(entry.getValue(), entry.getKey(), object);
        if (!object.has("evidence")) return;
        var evidence = items(object.path("evidence"));
        if (!evidence.isEmpty() && evidence.stream().allMatch(e -> SourceText.isResponseEvidence(e.asText()))) {
            verdict(object, CONFIRMED, null);
            return;
        }
        if (role.equals("composeService") && object.path("value").isNull()) {
            verdict(object, UNKNOWN, "composeService: 값 없음");
            return;
        }
        var lines = source.evidenceLines(object);
        if (lines.isEmpty()) { reject(object, role + ": 근거 파일·줄을 확인하지 못함"); return; }
        // Relationship facts are decided after all local facts have been checked.
        if (Set.of("target", "tokenRequired", "externalPaths", "kafkaLinks", "calls", "throws").contains(role)) return;
        if (object.has("value") && !object.path("value").isObject()) { fact(object, role, parent, lines); return; }
        if (object.has("type") && "unresolved".equals(object.path("kind").asText())) {
            if (!contains(lines, simpleType(object.path("type").asText())))
                reject(object, role + ": 근거 줄에서 타입 이름을 확인하지 못함");
            else verdict(object, UNKNOWN, "외부 타입의 구조를 근거 줄만으로 확인할 수 없음");
            return;
        }
        boolean matches = switch (role) {
            case "services" -> value(object.path("name")) == null || contains(lines, value(object.path("name")));
            case "endpoints", "feignCalls" -> value(object.path("httpMethod")) == null
                    ? Pattern.compile("@\\w*Mapping\\b").matcher(lines.get(0)).find()
                    : method(List.of(lines.get(0)), value(object.path("httpMethod")));
            case "feignClients" -> lines.get(0).contains("@FeignClient");
            case "gatewayRoutes" -> contains(lines, object.path("id").asText());
            case "allowedPaths" -> contains(lines, object.path("name").asText());
            case "declarations", "kafkaDeclarations" -> declaration(lines.get(0), object.path("kind").asText());
            case "kafkaPublications", "kafkaSubscriptions" -> declaration(lines.get(0), object.path("kind").asText());
            case "environmentVariables", "fields", "parameters" -> contains(lines, object.path("name").asText(null));
            case "annotations" -> contains(lines, "@" + object.path("name").asText());
            case "externalResources" -> resource(object);
            case "errorCodes" -> contains(lines, object.path("name").asText());
            case "exceptionHandlers" -> lines.get(0).contains("@ExceptionHandler");
            case "gatewayRejections" -> contains(lines, value(object.path("condition")));
            case "pageSerialization" -> lines.get(0).contains("@EnableSpringDataWebSupport");
            default -> object.has("type") && contains(lines, simpleType(object.path("type").asText()));
        };
        if (!matches) { reject(object, role + ": 근거 줄에서 이름·선언을 확인하지 못함"); return; }
        boolean resolved = switch (role) {
            case "services", "feignClients" -> known(object.path("name"));
            case "endpoints", "feignCalls", "allowedPaths" -> known(object.path("httpMethod")) && known(object.path("path"));
            case "gatewayRoutes" -> known(object.path("uri")) && known(object.path("targetService"))
                    && items(object.path("paths")).stream().allMatch(Checks::known);
            case "kafkaPublications", "kafkaSubscriptions" -> known(object.path("topicName")) || known(object.path("environment"));
            case "errorCodes" -> known(object.path("code")) && known(object.path("httpStatus")) && known(object.path("message"));
            case "exceptionHandlers" -> known(object.path("exception")) && known(object.path("httpStatus"));
            case "gatewayRejections" -> known(object.path("httpStatus"));
            default -> true;
        };
        verdict(object, resolved ? CONFIRMED : UNKNOWN, resolved ? null : "하위 값을 확정할 수 없음");
    }

    private void fact(ObjectNode fact, String role, JsonNode parent, List<String> lines) throws IOException {
        String value = value(fact);
        if (fact.has("source")) { sourced(fact, role, value, parent, lines); return; }
        if (value != null && value.isEmpty() && parent.has("enumType")) {
            // An enum without that argument: the constant line itself shows what it declares.
            if (contains(lines, parent.path("name").asText())) verdict(fact, CONFIRMED, null);
            else reject(fact, role + ": 근거 줄에서 상수를 확인하지 못함");
            return;
        }
        if (value != null && value.isEmpty() && role.equals("body") && parent.has("filter")) {
            if (contains(lines, "setComplete()")) verdict(fact, CONFIRMED, null);
            else reject(fact, "body: 근거 줄에서 본문 없이 끝내는 줄을 확인하지 못함");
            return;
        }
        boolean externalValue = switch (role) {
            case "value" -> parent.has("location") && contains(lines, "${" + parent.path("name").asText() + "}");
            case "topicName" -> known(parent.path("environment")) && !Objects.toString(value(parent.path("environment")), "").isEmpty()
                    || lines.stream().anyMatch(l -> ENV.matcher(l).find());
            case "connectionValues", "bucketName" -> lines.stream().anyMatch(l -> ENV.matcher(l).find());
            default -> value != null && ENV.matcher(value).matches() && contains(lines, value);
        };
        if (externalValue) { verdict(fact, ABSENT, "실제 값은 저장소 밖의 환경변수에서 정해짐"); return; }
        if (value == null) { verdict(fact, UNKNOWN, role + ": 값 없음"); return; }
        boolean matches = switch (role) {
            case "jsonName" -> value.equals(jsonName(parent, lines));
            case "httpMethod" -> method(lines, value) || contains(lines, "\"" + value + "\"");
            case "path" -> parent.has("clientId") || parent.has("controller") || parent.has("externalPaths")
                    ? mappingPath(lines, value) : value.isEmpty() ? emptyAttribute(lines, "path") : contains(lines, value);
            case "url" -> value.isEmpty() ? emptyAttribute(lines, "url") : contains(lines, value);
            case "rewritePath" -> value.isEmpty() ? lines.stream().anyMatch(l -> l.contains("id:")) : contains(lines, value);
            case "topicKey" -> value.isEmpty() ? contains(lines, value(parent.path("topicName"))) : contains(lines, value);
            case "environment" -> value.isEmpty() ? contains(lines, value(parent.path("topicName"))) : topicEnvironment(fact, parent, value);
            default -> contains(lines, value);
        };
        if (matches) verdict(fact, CONFIRMED, null);
        else reject(fact, role + ": 근거 줄에서 값·이름을 확인하지 못함");
    }

    /** Error handler values: a fixed value is on its line; the others are read from the exception or the validation. */
    private void sourced(ObjectNode fact, String role, String value, JsonNode handler, List<String> lines) {
        String source = fact.path("source").asText();
        // An empty fixed value: the body DTO has no such field, shown by its declaration.
        String expected = "".equals(value) ? simpleType(handler.path("body").path("type").asText()) : value;
        String reason = switch (source) {
            case "고정" -> contains(lines, expected) ? null : "근거 줄에서 값을 확인하지 못함";
            case "던진 오류 코드" -> value == null && contains(lines, "getErrorCode()") ? null : "근거 줄에서 오류 코드를 읽는 곳을 확인하지 못함";
            case "예외 메시지" -> value == null && contains(lines, "getMessage()") ? null : "근거 줄에서 예외 메시지를 읽는 곳을 확인하지 못함";
            case "검증 메시지" -> value == null && contains(lines, "getDefaultMessage()") ? null : "근거 줄에서 검증 메시지를 읽는 곳을 확인하지 못함";
            default -> "알 수 없는 source";
        };
        if (reason != null) reject(fact, role + ": " + reason);
        else if (source.equals("예외 메시지")) verdict(fact, UNKNOWN, "예외 메시지를 그대로 씀, 값은 던지는 곳에서 정해짐");
        else if (source.equals("검증 메시지")) verdict(fact, UNKNOWN, "검증 어노테이션의 메시지를 씀, 값은 필드 어노테이션에서 정해짐");
        else verdict(fact, CONFIRMED, null);
    }

    private boolean topicEnvironment(JsonNode fact, JsonNode endpoint, String name) throws IOException {
        String key = value(endpoint.path("topicKey"));
        if (key == null || key.isEmpty() || !contains(source.evidenceLines(endpoint.path("topicKey")), key)) return false;
        String composeService = items(analysis.path("services")).stream()
                .filter(s -> known(s.path("name")) && endpoint.path("service").asText().equals(value(s.path("name"))))
                .map(s -> s.path("composeService")).filter(Checks::known).map(Checks::value).findFirst().orElse(null);
        if (composeService == null || composeService.isEmpty()) return false;
        String composePath = "services." + composeService + ".environment";
        boolean yaml = false, compose = false;
        for (var ev : fact.path("evidence")) {
            String evidence = ev.asText(), line = source.line(evidence);
            if (evidence.contains("/src/main/resources/application.yml:") && source.yamlKey(evidence).equals(key)
                    && line.substring(line.indexOf(':') + 1).trim().equals("${" + name + "}")) yaml = true;
            if (evidence.startsWith("docker-compose.yml:") && line.contains("${" + name + "}")) {
                String composeKey = source.yamlKey(evidence);
                if (composeKey.equals(composePath) || composeKey.startsWith(composePath + ".")) compose = true;
            }
        }
        return yaml && compose;
    }

    private boolean resource(JsonNode node) throws IOException {
        // The extraction supplies the resource name; the verifier has no resource catalogue.
        String name = node.path("kind").asText().toLowerCase(Locale.ROOT);
        if (name.isBlank()) return false;
        for (var ev : node.path("configurationEvidence")) {
            String line = source.line(ev.asText());
            if (line != null && (source.yamlKey(ev.asText()).toLowerCase(Locale.ROOT).contains(name)
                    || line.toLowerCase(Locale.ROOT).contains(name))) return true;
        }
        for (var ev : node.path("codeEvidence")) {
            String line = source.line(ev.asText());
            if (line != null && line.toLowerCase(Locale.ROOT).contains(name)) return true;
        }
        return false;
    }

    private String jsonName(JsonNode field, List<String> lines) throws IOException {
        String name = field.path("name").asText();
        if (name.isEmpty() || !contains(source.evidenceLines(field), name)) return null;
        String declaration = lines.stream().filter(l -> Pattern.compile("\\b" + Pattern.quote(name) + "\\s*[;=]").matcher(l).find()).findFirst().orElse(null);
        if (declaration == null) return null;
        String suffix = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        boolean primitive = Pattern.compile("\\bboolean\\s+" + Pattern.quote(name) + "\\b").matcher(declaration).find();
        String booleanGetter = name.startsWith("is") && name.length() > 2 && Character.isUpperCase(name.charAt(2)) ? name : "is" + suffix;
        if (lines.stream().anyMatch(l -> Pattern.compile("@(?:Getter|Data)\\b").matcher(l).find()))
            return property(primitive ? booleanGetter : "get" + suffix);
        for (String line : lines) {
            var getter = Pattern.compile("\\bpublic\\s+([\\w.<>]+)\\s+((?:get|is)\\w+)\\s*\\(\\s*\\)").matcher(line);
            if (!getter.find() || getter.group(1).equals("void")) continue;
            String method = getter.group(2);
            if (method.equals("get" + suffix) || Set.of("boolean", "Boolean", "java.lang.Boolean").contains(getter.group(1))
                    && (method.equals("is" + suffix) || method.equals(booleanGetter))) return property(method);
        }
        return Pattern.compile("\\bpublic\\b").matcher(declaration).find() ? name : null;
    }

    private static String property(String getter) {
        String suffix = getter.substring(getter.startsWith("is") ? 2 : 3);
        var result = new StringBuilder(suffix);
        for (int i = 0; i < suffix.length(); i++) {
            char lower = Character.toLowerCase(suffix.charAt(i));
            if (lower == suffix.charAt(i)) break;
            result.setCharAt(i, lower);
        }
        return result.toString();
    }

    static boolean declaration(String line, String kind) {
        return switch (kind) {
            case "kafkaTemplate.send" -> Pattern.compile("\\bkafkaTemplate\\s*\\.\\s*send\\s*\\(").matcher(line).find();
            case "DeadLetterPublishingRecoverer" -> line.contains("new DeadLetterPublishingRecoverer(");
            default -> line.contains("@" + kind);
        };
    }
    private static boolean method(List<String> lines, String method) {
        if (method == null) return false;
        String annotation = method.equals("*") ? "RequestMapping" : method.charAt(0) + method.substring(1).toLowerCase(Locale.ROOT) + "Mapping";
        return lines.stream().anyMatch(l -> l.contains("@" + annotation) || l.contains("RequestMethod." + method));
    }
    private static boolean emptyAttribute(List<String> lines, String attribute) {
        return lines.stream().anyMatch(l -> l.contains("@FeignClient")
                && !Pattern.compile("\\b" + attribute + "\\s*=").matcher(l).find());
    }
    private static boolean mappingPath(List<String> lines, String path) {
        var pieces = new ArrayList<String>();
        for (String line : lines) {
            if (!MAPPING.matcher(line).find()) return false;
            var matcher = QUOTED.matcher(line);
            while (matcher.find()) if (line.contains("Mapping") || matcher.group(1).startsWith("/")) pieces.add(matcher.group(1));
        }
        if (pieces.isEmpty()) return path.equals("/");
        String joined = ("/" + String.join("/", pieces)).replaceAll("/{2,}", "/");
        if (joined.length() > 1 && joined.endsWith("/")) joined = joined.substring(0, joined.length() - 1);
        return joined.equals(path);
    }
    private static String simpleType(String type) {
        String name = type.split("[<\\[]", 2)[0];
        return name.substring(name.lastIndexOf('.') + 1);
    }
    private static boolean contains(List<String> lines, String text) {
        if (text == null || text.isEmpty()) return false;
        String escaped = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return lines.stream().anyMatch(l -> l.contains(text) || l.contains(escaped));
    }
    private void reject(JsonNode node, String reason) {
        verdict(node, UNKNOWN, reason);
        warn(analysis, "evidence-lines", node.path("evidence"), reason);
    }
}
