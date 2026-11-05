package io.hotdealwave.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import static io.hotdealwave.verifier.Checks.*;

/** The declaration rules for each transport. Inventory, evidence and call ends use the same rules. */
final class CommunicationRules {
    record Rule(String receiverType, String client, String call, String publication, String subscription,
                String automatic, boolean interfaceCall) {}
    static final Map<String, Rule> RULES = new LinkedHashMap<>();
    private static final Map<String, Pattern> CANDIDATES = new HashMap<>();
    static {
        RULES.put("feign", new Rule("", "@(?:[\\w]+\\.)*FeignClient\\b", "@(?:[\\w]+\\.)*\\w*Mapping\\b", "", "", "", true));
        RULES.put("restTemplate", new Rule("RestTemplate", "", "{receiver}\\s*\\.\\s*(?:getForObject|getForEntity|postForObject|postForEntity|put|patchForObject|delete|headForHeaders|optionsForAllow|exchange|execute)\\s*\\(", "", "", "", false));
        RULES.put("restClient", new Rule("RestClient", "\\.baseUrl\\s*\\(", "{receiver}\\s*\\.\\s*(?:get|post|put|patch|delete|head|options|method)\\s*\\(", "", "", "", false));
        RULES.put("webClient", new Rule("WebClient", "\\.baseUrl\\s*\\(", "{receiver}\\s*\\.\\s*(?:get|post|put|patch|delete|head|options|method)\\s*\\(", "", "", "", false));
        RULES.put("httpExchange", new Rule("", "@(?:[\\w]+\\.)*HttpExchange\\b", "@(?:[\\w]+\\.)*(?:Http|Get|Post|Put|Patch|Delete|Head|Options)Exchange\\b", "", "", "", true));
        RULES.put("kafka", new Rule("KafkaTemplate", "", "", "{receiver}\\s*\\.\\s*send\\s*\\(|\\bnew\\s+(?:[\\w]+\\.)*DeadLetterPublishingRecoverer\\s*\\(", "@(?:[\\w]+\\.)*KafkaListener\\b", "\\bnew\\s+(?:[\\w]+\\.)*DeadLetterPublishingRecoverer\\s*\\(", false));
        RULES.put("rabbit", new Rule("(?:RabbitTemplate|AmqpTemplate)", "", "", "{receiver}\\s*\\.\\s*(?:convertAndSend|send)\\s*\\(", "@(?:[\\w]+\\.)*RabbitListener\\b", "", false));
        RULES.put("stream", new Rule("StreamBridge", "", "", "{receiver}\\s*\\.\\s*send\\s*\\(|\\b(?:Supplier|Function)\\s*<.*>\\s+\\w+\\s*\\(", "\\b(?:Consumer|Function)\\s*<.*>\\s+\\w+\\s*\\(", "\\b(?:Supplier|Function)\\s*<.*>\\s+\\w+\\s*\\(", false));
        for (var rule : RULES.values())
            for (String regex : List.of(rule.client(), rule.call(), rule.publication(), rule.subscription(), rule.automatic(),
                    "(?:" + rule.publication() + "|" + rule.subscription() + ")"))
                CANDIDATES.put(regex, Pattern.compile(regex.replace("{receiver}", "\\b\\w+")));

    }
    private CommunicationRules() {}
    static boolean matches(String kind, String role, String line, List<String> file) {
        Rule rule = RULES.get(kind);
        if (rule == null || line == null) return false;
        String regex = switch (role) {
            case "httpClients" -> rule.client();
            case "httpCalls" -> rule.call();
            case "messagePublications" -> rule.publication();
            case "messageSubscriptions" -> rule.subscription();
            case "automatic" -> rule.automatic();
            case "messageDeclarations" -> rule.publication().isEmpty() ? "" : "(?:" + rule.publication() + "|" + rule.subscription() + ")";
            default -> "";
        };
        if (regex.isEmpty() || !CANDIDATES.get(regex).matcher(line).find()) return false;
        if (role.equals("httpCalls") && kind.equals("feign") && file.subList(0, Math.max(0, file.indexOf(line))).stream().noneMatch(l -> l.contains("@FeignClient"))) return false;
        if (kind.equals("httpExchange")) {
            // HttpExchange may decorate either an interface or a method; only the former is a client.
            int at = file.indexOf(line);
            String next = at >= 0 && at + 1 < file.size() ? file.get(at + 1) : "";
            boolean type = Pattern.compile("\\binterface\\b").matcher(line + " " + next).find();
            if (role.equals("httpClients") && !type || role.equals("httpCalls") && type) return false;
        }
        if (role.equals("httpClients") && !rule.receiverType().isEmpty() && !clientType(kind, line, file)) return false;
        if (kind.equals("stream") && (line.contains("Consumer<") || line.contains("Supplier<") || line.contains("Function<"))) {
            int at = file.indexOf(line);
            if (!line.contains("@Bean") && (at < 1 || !file.get(at - 1).contains("@Bean"))) return false;
        }
        if (regex.contains("{receiver}")) {
            var names = receivers(rule.receiverType(), file);
            // Preserve the original Kafka inventory's conventional name as well as declared fields/parameters.
            if (kind.equals("kafka")) names.add("kafkaTemplate");
            String receiver = names.isEmpty() ? "(?!)" : "\\b(?:" + String.join("|", names.stream().map(Pattern::quote).toList()) + ")";
            regex = regex.replace("{receiver}", receiver);
        }
        return Pattern.compile(regex).matcher(line).find();
    }
    /** Associate baseUrl with its builder or the nearest typed bean/field, not a type elsewhere in the file. */
    private static boolean clientType(String kind, String line, List<String> file) {
        for (int i = file.indexOf(line); i >= 0; i--) {
            var types = new HashSet<String>();
            for (var entry : RULES.entrySet()) {
                var rule = entry.getValue();
                if (rule.client().isEmpty() || rule.receiverType().isEmpty()) continue;
                var declaration = Pattern.compile("\\b" + rule.receiverType() + "(?:\\s*\\.builder\\s*\\(|\\s+\\w+\\s*[=(])");
                if (declaration.matcher(file.get(i)).find()) types.add(entry.getKey());
            }
            if (!types.isEmpty()) return types.size() == 1 && types.contains(kind);
        }
        return false;
    }

    private static Set<String> receivers(String type, List<String> file) {
        var names = new TreeSet<String>();
        if (type.isEmpty()) return names;
        var pattern = Pattern.compile("\\b" + type + "\\s*(?:<[^;=()]*>)?\\s+(\\w+)\\s*[;=,)]");
        for (String line : file) pattern.matcher(line).results().forEach(m -> names.add(m.group(1)));
        return names;
    }
    static List<String> file(SourceText source, String evidence) throws IOException {
        int colon = evidence.lastIndexOf(':');
        return colon < 0 ? List.of() : source.lines(evidence.substring(0, colon));
    }
    static boolean matches(SourceText source, JsonNode item, String role) throws IOException {
        String first = item.path("evidence").path(0).asText();
        return matches(item.path("kind").asText(), role, source.line(first), file(source, first));
    }
    static boolean interfaceCall(JsonNode call) {
        Rule rule = RULES.get(call.path("kind").asText());
        return rule != null && rule.interfaceCall();
    }
    static boolean callable(SourceText source, JsonNode publication) throws IOException {
        return matches(source, publication, "messagePublications") && !matches(source, publication, "automatic");
    }

    static void inventory(Map<String,String> lines, com.fasterxml.jackson.databind.node.ObjectNode analysis) {
        var files = new LinkedHashMap<String,List<String>>();
        lines.forEach((ev,line) -> files.computeIfAbsent(ev.substring(0,ev.lastIndexOf(':')), k -> new ArrayList<>()).add(line));
        for (String group : List.of("http", "message")) {
            var raw = new LinkedHashMap<String,String>();
            var extracted = new LinkedHashMap<String,String>();
            var roles = group.equals("http") ? List.of("httpClients", "httpCalls") : List.of("messageDeclarations");
            for (var entry : lines.entrySet()) {
                var file = files.get(entry.getKey().substring(0,entry.getKey().lastIndexOf(':')));
                for (String kind : RULES.keySet()) for (String role : roles)
                    if (matches(kind, role, entry.getValue(), file)) raw.put(role + ":" + kind + "@" + entry.getKey(), kind);
            }
            for (String role : roles) for (var item : analysis.path(role))
                extracted.put(role + ":" + item.path("kind").asText() + "@" + item.path("evidence").path(0).asText(), item.path("kind").asText());
            CrossChecks.inventory(analysis, group + "-declarations", raw, extracted);
            var counts = count(analysis, group + "-declarations");
            counts.put("declarationCount", raw.size()).put("extractedDeclarationCount", extracted.size());
            if (group.equals("http")) counts.put("clientCount", (int) raw.keySet().stream().filter(k -> k.startsWith("httpClients:")).count());
            if (group.equals("message")) {
                int listeners = 0, sends = 0, automatic = 0;
                for (var entry : lines.entrySet()) {
                    var file = files.get(entry.getKey().substring(0,entry.getKey().lastIndexOf(':')));
                    for (String kind : RULES.keySet()) {
                        if (matches(kind, "messageSubscriptions", entry.getValue(), file)) listeners++;
                        if (matches(kind, "automatic", entry.getValue(), file)) automatic++;
                        else if (matches(kind, "messagePublications", entry.getValue(), file)) sends++;
                    }
                }
                counts.put("listenerCount", listeners).put("sendCount", sends).put("automaticCount", automatic);
            }
        }
    }

    /** A literal argument only. Concatenations and runtime expressions stay unresolved. */
    static String literal(String expression) {
        var match = Pattern.compile("^\\s*\"((?:\\\\.|[^\"\\\\])*)\"\\s*$").matcher(expression);
        return match.matches() ? match.group(1).replace("\\\"", "\"").replace("\\\\", "\\") : null;
    }
    static List<String> arguments(String line, String method) {
        var match = Pattern.compile(method + "\\s*\\(").matcher(line);
        if (!match.find()) return List.of();
        var args = new ArrayList<String>();
        int start = match.end(), depth = 0;
        boolean quoted = false;
        for (int i = start; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted && c == '\\') { i++; continue; }
            if (c == '"') quoted = !quoted;
            if (quoted) continue;
            if (c == '(' || c == '[' || c == '{') depth++;
            if (c == ')' && depth == 0) { args.add(line.substring(start,i).trim()); return args; }
            if (c == ')' || c == ']' || c == '}') depth--;
            if (c == ',' && depth == 0) { args.add(line.substring(start,i).trim()); start = i + 1; }
        }
        return List.of();
    }
    static String argument(List<String> lines, String method, int position) {
        for (String line : lines) {
            var args = arguments(line, method);
            if (args.size() > position) return argumentValue(args.get(position), lines);
        }
        return null;
    }
    private static String argumentValue(String expression, List<String> lines) {
        String value = literal(expression);
        if (value != null || !expression.matches("[a-zA-Z_$][\\w$]*")) return value;
        String name = Pattern.quote(expression);
        var values = new HashSet<String>();
        var assigned = Pattern.compile("\\bString\\s+" + name + "\\s*=\\s*(\"(?:\\\\.|[^\"\\\\])*\")\\s*;");
        var injected = Pattern.compile("@Value\\(\\s*(\"(?:\\\\.|[^\"\\\\])*\")\\s*\\)\\s*(?:(?:private|public|protected|final)\\s+)*String\\s+" + name + "\\b");
        String evidence = String.join("\n", lines);
        for (var pattern : List.of(assigned, injected)) pattern.matcher(evidence).results().forEach(m -> values.add(literal(m.group(1))));
        return values.size() == 1 ? values.iterator().next() : null;
    }

    static String attribute(List<String> lines, String annotation, String name) {
        for (String line : lines) {
            var args = arguments(line, "@(?:[\\w]+\\.)*" + annotation);
            for (String arg : args) {
                var pair = arg.split("\\s*=\\s*",2);
                if (pair.length == 2 && (pair[0].equals(name) || name.equals("url") && pair[0].equals("value"))) return literal(pair[1]);
                if (pair.length == 1 && (name.equals("url") || name.equals("value"))) return literal(pair[0]);
            }
        }
        return null;
    }
    static String httpMethod(List<String> lines) {
        var annotation = Pattern.compile("@(?:[\\w]+\\.)*(Get|Post|Put|Patch|Delete|Head|Options)(?:Mapping|Exchange)\\b");
        var explicit = Pattern.compile("(?:RequestMethod|HttpMethod)\\.(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\\b");
        var fluent = Pattern.compile("\\.(get|post|put|patch|delete|head|options)\\s*\\(");
        var template = Pattern.compile("\\.(getForObject|getForEntity|postForObject|postForEntity|patchForObject|headForHeaders|optionsForAllow)\\s*\\(");
        for (String line : lines) {
            for (var pattern : List.of(annotation,explicit,fluent)) {
                var m = pattern.matcher(line);
                if (m.find()) return m.group(1).toUpperCase(Locale.ROOT);
            }
            var m = template.matcher(line);
            if (m.find()) return m.group(1).split("For",2)[0].toUpperCase(Locale.ROOT);
        }
        String method = attribute(lines, "HttpExchange", "method");
        return method;
    }
    static String httpPath(JsonNode call, List<String> lines) {
        if (interfaceCall(call)) {
            var pieces = new ArrayList<String>();
            for (String line : lines) {
                var annotation = Pattern.compile("@(?:[\\w]+\\.)*(\\w*(?:Mapping|Exchange))\\b").matcher(line);
                if (!annotation.find()) continue;
                String path = attribute(List.of(line), annotation.group(1), "url");
                if (path == null) path = attribute(List.of(line), annotation.group(1), "path");
                if (path != null) pieces.add(path);
            }
            return join(pieces);
        }
        String path = argument(lines, "\\.uri", 0);
        if (path != null) return path;
        return argument(lines, "\\.(?:getForObject|getForEntity|postForObject|postForEntity|put|patchForObject|delete|headForHeaders|optionsForAllow|exchange|execute)", 0);
    }
    static String join(List<String> pieces) {
        if (pieces.isEmpty()) return "/";
        String result = String.join("/", pieces).replaceAll("(?<!:)/{2,}", "/");
        if (!result.startsWith("/") && !result.contains("://") && !result.startsWith("${")) result = "/" + result;
        return result.length() > 1 && result.endsWith("/") ? result.substring(0,result.length()-1) : result;
    }
    static String baseUrl(List<String> lines) {
        String url = argument(lines, "\\.baseUrl", 0);
        if (url == null) url = attribute(lines, "HttpExchange", "url");
        if (url == null) url = attribute(lines, "FeignClient", "url");
        return url;
    }

    /** Literal Rabbit publication names retain exchange and routing key without guessing a queue. */
    static String rabbitDestination(List<String> lines, List<String> file) {
        for (String line : lines) {
            var args = arguments(line, "\\.(?:convertAndSend|send)");
            if (args.size() < 2) continue;
            if (args.size() == 2 && file.stream().anyMatch(l -> Pattern.compile("\\.setExchange\\s*\\(").matcher(l).find())) return null;
            String exchange = args.size() == 2 ? "" : literal(args.get(0));
            String routing = literal(args.get(args.size() == 2 ? 0 : 1));
            return exchange == null || routing == null ? null : exchange + "/" + routing;
        }
        return null;
    }
    static boolean rabbitBinding(String destination, String queue, List<String> lines) {
        if (destination == null || queue == null) return false;
        int slash = destination.indexOf('/');
        if (slash < 0) return false;
        String exchange = destination.substring(0,slash), key = destination.substring(slash+1);
        if (exchange.isEmpty()) return key.equals(queue);
        for (String line : lines) {
            var args = arguments(line,"new\\s+Binding");
            if (args.size() >= 4 && queue.equals(literal(args.get(0))) && args.get(1).equals("Binding.DestinationType.QUEUE")
                    && exchange.equals(literal(args.get(2))) && key.equals(literal(args.get(3)))) return true;
            // BindingBuilder with explicit Queue/DirectExchange declarations in the supplied evidence.
            var binding = Pattern.compile("BindingBuilder\\.bind\\(\\s*(\\w+)\\s*\\)\\.to\\(\\s*(\\w+)\\s*\\)\\.with\\(\\s*\"([^\"]*)\"\\s*\\)").matcher(line);
            if (binding.find() && key.equals(binding.group(3))
                    && namedBean(lines, binding.group(1), "Queue", queue) && namedBean(lines, binding.group(2), "DirectExchange", exchange)) return true;
        }
        return false;
    }
    static boolean sameDestination(String kind, String publication, String subscription, List<String> evidence) {
        return kind.equals("rabbit") ? rabbitBinding(publication, subscription, evidence) : publication.equals(subscription);
    }

    private static boolean namedBean(List<String> lines, String variable, String type, String name) {
        return lines.stream().anyMatch(l -> Pattern.compile("\\b" + type + "\\s+" + Pattern.quote(variable) + "\\s*=\\s*new\\s+" + type + "\\(")
                .matcher(l).find() && name.equals(argument(List.of(l), "new\\s+" + type, 0)));
    }
}
