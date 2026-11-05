package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import static io.hotdealwave.verifier.Checks.*;

final class EvidenceChecks {
    private static final Pattern ENV = Pattern.compile("\\$\\{[A-Z][A-Z0-9_]*(?::[^}]*)?}");
    private static final Pattern QUOTED = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern SETTINGS = Pattern.compile("/src/main/resources/application\\.(?:yml|yaml|properties):");
    private static final Pattern JSON_PROPERTY = Pattern.compile("@JsonProperty\\(\\s*(?:value\\s*=\\s*)?\"([^\"]*)\"");
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
        if (role.equals("runDefinition") && evidence.isEmpty() && object.path("value").isNull()) {
            verdict(object, ABSENT, "저장소에서 실행 정의를 찾지 못함");
            return;
        }
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
            case "declarations", "kafkaDeclarations", "kafkaPublications", "kafkaSubscriptions" ->
                    declaration(lines.get(0), object.path("kind").asText(), kafkaTemplates(evidence.get(0).asText()));
            case "environmentVariables", "fields", "parameters" -> contains(lines, object.path("name").asText(null));
            case "annotations" -> contains(lines, "@" + object.path("name").asText());
            case "externalResources" -> resource(object);
            case "errorCodes" -> contains(lines, object.path("name").asText());
            case "exceptionHandlers" -> lines.get(0).contains("@ExceptionHandler");
            case "gatewayRejections" -> contains(lines, value(object.path("condition")));
            case "pageSerialization" -> lines.get(0).contains("@EnableSpringDataWebSupport");
            // Category items name their tool's file; the values on the lines are checked as facts.
            case "pipelines", "logShipping", "toolConfigs" -> inFile(object);
            case "containers" -> inFile(object) && contains(lines, object.path("service").asText());
            case "metrics" -> inFile(object) && contains(lines, object.path("job").asText());
            case "triggers" -> contains(lines, object.path("event").asText());
            case "env" -> contains(lines, object.path("name").asText());
            case "secrets" -> object.path("reference").asText().contains(object.path("name").asText())
                    && contains(lines, object.path("reference").asText());
            case "jobs" -> contains(lines, object.path("id").asText());
            case "steps" -> contains(lines, object.path("name").asText());
            case "monitoringTools" -> lines.stream().anyMatch(l -> l.toLowerCase(Locale.ROOT).contains(object.path("name").asText().toLowerCase(Locale.ROOT)));
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

    private static boolean inFile(JsonNode item) {
        return items(item.path("evidence")).get(0).asText().startsWith(item.path("file").asText() + ":");
    }

    private void fact(ObjectNode fact, String role, JsonNode parent, List<String> lines) throws IOException {
        String value = value(fact);
        if (fact.has("source")) { sourced(fact, role, value, parent, lines); return; }
        if (role.equals("secretValue") || role.equals("hostValue")) {
            // Values that only the deployment environment holds: the line must use them the way the item writes.
            String used = parent.path(role.equals("secretValue") ? "reference" : "hostReference").asText();
            if (!used.isEmpty() && contains(lines, used)) verdict(fact, ABSENT, role.equals("secretValue")
                    ? "실제 값은 저장소 밖에 등록된 비밀값에서 정해짐" : "실제 값은 저장소 밖의 환경변수에서 정해짐");
            else reject(fact, role + ": 근거 줄에서 이름을 쓰는 곳을 확인하지 못함");
            return;
        }
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
        if (role.equals("value") && parent.has("location")) {
            var reference = Pattern.compile("\\$\\{" + Pattern.quote(parent.path("name").asText()) + "(?::([^}]*))?}");
            var used = lines.stream().map(reference::matcher).filter(java.util.regex.Matcher::find).findFirst();
            if (used.isPresent()) {
                String fallback = used.get().group(1);
                verdict(fact, ABSENT, "실제 값은 저장소 밖의 환경변수에서 정해짐" + (fallback == null ? "" : ", 없으면 기본값 " + fallback));
                return;
            }
        }
        if (role.equals("jsonName")) {
            String reason = namingOverride(parent);
            if (reason != null) { verdict(fact, UNKNOWN, reason); return; }
        }
        boolean externalValue = switch (role) {
            case "topicName" -> known(parent.path("environment")) && !Objects.toString(value(parent.path("environment")), "").isEmpty()
                    || lines.stream().anyMatch(l -> ENV.matcher(l).find());
            case "connectionValues", "bucketName" -> lines.stream().anyMatch(l -> ENV.matcher(l).find());
            default -> value != null && ENV.matcher(value).matches() && contains(lines, value);
        };
        if (externalValue) { verdict(fact, ABSENT, "실제 값은 저장소 밖의 환경변수에서 정해짐"); return; }
        if (value == null) { verdict(fact, UNKNOWN, role + ": 값 없음"); return; }
        boolean matches = switch (role) {
            case "jsonName" -> value.equals(jsonName(parent, lines));
            case "httpMethod" -> method(lines, value) || contains(lines, "\"" + value + "\"") || contains(lines, "HttpMethod." + value)
                    || value.equals("*") && lines.stream().anyMatch(l -> l.contains("Matchers(") && !l.contains("HttpMethod."));
            case "path" -> parent.has("clientId") || parent.has("controller") || parent.has("externalPaths")
                    ? mappingPath(lines, value) : value.isEmpty() ? emptyAttribute(lines, "path") : contains(lines, value);
            case "url" -> value.isEmpty() ? emptyAttribute(lines, "url") : contains(lines, value);
            case "rewritePath" -> value.isEmpty() ? lines.stream().anyMatch(l -> l.contains("id:") || l.contains(".route("))
                    : contains(lines, value) || routeFilter(lines, value);
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
            case "분기" -> value == null && lines.stream().anyMatch(l -> Pattern.compile("\\bif\\s*\\(").matcher(l).find()) ? null : "근거 줄에서 분기를 확인하지 못함";
            default -> "알 수 없는 source";
        };
        if (reason != null) reject(fact, role + ": " + reason);
        else if (source.equals("예외 메시지")) verdict(fact, UNKNOWN, "예외 메시지를 그대로 씀, 값은 던지는 곳에서 정해짐");
        else if (source.equals("검증 메시지")) verdict(fact, UNKNOWN, "검증 어노테이션의 메시지를 씀, 값은 필드 어노테이션에서 정해짐");
        else if (source.equals("분기")) verdict(fact, UNKNOWN, "처리기 안에서 조건에 따라 값이 갈림, 근거 줄의 분기를 볼 것");
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
            if (SETTINGS.matcher(evidence).find() && source.settingKey(evidence).equals(key)
                    && Pattern.compile("\\$\\{" + Pattern.quote(name) + "(?::[^}]*)?}").matcher(SourceText.settingValue(line)).matches()) yaml = true;
            if (!source.composeFile().isEmpty() && evidence.startsWith(source.composeFile() + ":") && line.contains("${" + name + "}")) {
                String composeKey = source.settingKey(evidence);
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
            if (line != null && (source.settingKey(ev.asText()).toLowerCase(Locale.ROOT).contains(name)
                    || line.toLowerCase(Locale.ROOT).contains(name))) return true;
        }
        for (var ev : node.path("codeEvidence")) {
            String line = source.line(ev.asText());
            if (line != null && line.toLowerCase(Locale.ROOT).contains(name)) return true;
        }
        return false;
    }

    /** Why a class-wide or global naming rule leaves the field's JSON name open, or null. */
    private String namingOverride(JsonNode field) throws IOException {
        for (var evidence : field.path("evidence")) {
            String file = file(evidence.asText());
            if (file != null && source.lines(file).stream().anyMatch(l -> l.contains("@JsonNaming")))
                return "클래스의 @JsonNaming이 JSON 이름을 바꿈";
        }
        if (source.namingStrategy()) return "설정의 spring.jackson.property-naming-strategy가 JSON 이름을 바꿈";
        return null;
    }

    private String jsonName(JsonNode field, List<String> lines) throws IOException {
        String name = field.path("name").asText();
        if (name.isEmpty() || !contains(source.evidenceLines(field), name)) return null;
        var declared = Pattern.compile("\\b" + Pattern.quote(name) + "\\s*[;=]");
        var component = Pattern.compile("\\b" + Pattern.quote(name) + "\\s*[,)]");
        for (var evidence : field.path("evidence")) {
            String file = file(evidence.asText());
            if (file == null) continue;
            int number = Integer.parseInt(evidence.asText().substring(file.length() + 1));
            var all = source.lines(file);
            String line = all.get(number - 1);
            var inRecord = component.matcher(line);
            boolean record = inRecord.find() && all.subList(0, number).stream().anyMatch(l -> l.contains("record "));
            if (!record && !declared.matcher(line).find()) continue;
            // The annotation is on the field's own line, or on the annotation lines just above it; a record component carries it before its type.
            var annotations = new ArrayList<String>();
            if (record) {
                String before = line.substring(0, inRecord.start());
                annotations.add(before.substring((before.contains(",") ? before.lastIndexOf(',') : before.indexOf('(')) + 1));
            } else {
                annotations.add(line);
                for (int i = number - 2; i >= 0 && all.get(i).trim().startsWith("@"); i--) annotations.add(all.get(i));
            }
            for (String annotation : annotations) {
                var property = JSON_PROPERTY.matcher(annotation);
                if (property.find()) return property.group(1);
            }
            if (record) return name;
            break;
        }
        String declaration = lines.stream().filter(l -> declared.matcher(l).find()).findFirst().orElse(null);
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

    /** The source file of a file:line evidence whose line exists, or null. */
    private String file(String evidence) throws IOException {
        int colon = evidence.lastIndexOf(':');
        if (colon < 1 || SourceText.isResponseEvidence(evidence)) return null;
        String file = evidence.substring(0, colon);
        try {
            int number = Integer.parseInt(evidence.substring(colon + 1));
            return number > 0 && number <= source.lines(file).size() ? file : null;
        } catch (NumberFormatException e) { return null; }
    }

    /** Java DSL route filters: rewritePath with its two arguments joined by ", ", and StripPrefix=N. */
    private static boolean routeFilter(List<String> lines, String value) {
        var strip = Pattern.compile("StripPrefix=(\\d+)").matcher(value);
        if (strip.matches()) return lines.stream().anyMatch(l -> Pattern.compile("\\.stripPrefix\\(\\s*" + strip.group(1) + "\\s*\\)").matcher(l).find());
        var parts = value.split(",\\s*", 2);
        return parts.length == 2 && lines.stream().anyMatch(l -> l.contains("rewritePath(\"" + parts[0] + "\"") && l.contains("\"" + parts[1] + "\""));
    }

    /** Names of KafkaTemplate fields and parameters in the evidence file, and kafkaTemplate. */
    private Set<String> kafkaTemplates(String evidence) throws IOException {
        String file = file(evidence);
        return file == null ? Set.of("kafkaTemplate") : KafkaChecks.templates(source.lines(file));
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

    static boolean declaration(String line, String kind, Set<String> templates) {
        return switch (kind) {
            case "kafkaTemplate.send" -> templates.stream().anyMatch(t -> Pattern.compile("\\b" + Pattern.quote(t) + "\\s*\\.\\s*send\\s*\\(").matcher(line).find());
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
