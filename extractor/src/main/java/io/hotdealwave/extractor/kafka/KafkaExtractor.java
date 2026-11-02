package io.hotdealwave.extractor.kafka;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import io.hotdealwave.extractor.config.ConfigExtractor;
import io.hotdealwave.extractor.model.Analysis;
import io.hotdealwave.extractor.model.Analysis.*;
import io.hotdealwave.extractor.source.SourceContext;
import io.hotdealwave.extractor.source.SourceValues;
import java.util.*;
import java.util.regex.Pattern;
import static io.hotdealwave.extractor.model.Analysis.evidence;

/** Backward argument tracing is confined to one service's source context. */
public final class KafkaExtractor {
    private static final Pattern TOPIC = Pattern.compile("\\$\\{(spring\\.kafka\\.topics\\.[\\w-]+)}");
    private final SourceContext source;
    private final ConfigExtractor config;
    private final Map<String, List<MethodCallExpr>> callers = new LinkedHashMap<>();

    public KafkaExtractor(SourceContext source, ConfigExtractor config) {
        this.source = source;
        this.config = config;
        Set<String> methodNames = new HashSet<>();
        source.units.forEach(u -> u.findAll(MethodDeclaration.class).forEach(m -> methodNames.add(m.getNameAsString())));
        for (var unit : source.units) for (var call : unit.findAll(MethodCallExpr.class)) {
            if (!methodNames.contains(call.getNameAsString())) continue;
            MethodDeclaration method = target(call);
            if (method != null) callers.computeIfAbsent(location(method), ignored -> new ArrayList<>()).add(call);
        }
    }

    public void extract(Analysis analysis) {
        for (var unit : source.units) {
            for (var annotation : unit.findAll(AnnotationExpr.class)) {
                if (!annotation.getName().getIdentifier().equals("KafkaListener")) continue;
                declare(analysis, "KafkaListener", annotation);
                Expression topics = SourceValues.argument(annotation, "topics").orElseThrow();
                add(analysis, false, "KafkaListener", annotation,
                        property(topics, evidence(source.evidence(annotation), source.evidence(topics))));
            }
            for (var call : unit.findAll(MethodCallExpr.class)) {
                if (!call.getNameAsString().equals("send") || call.getScope().isEmpty()) continue;
                Expression scope = call.getScope().get();
                boolean kafka = scope.isNameExpr() && scope.asNameExpr().getNameAsString().equals("kafkaTemplate")
                        || scope.isFieldAccessExpr() && scope.asFieldAccessExpr().getScope().isThisExpr()
                        && scope.asFieldAccessExpr().getNameAsString().equals("kafkaTemplate");
                if (!kafka) continue;
                declare(analysis, "kafkaTemplate.send", call);
                for (var key : trace(call.getArgument(0), source.evidence(call)))
                    add(analysis, true, "kafkaTemplate.send", call, key);
            }
            for (var creation : unit.findAll(ObjectCreationExpr.class)) {
                if (!creation.getType().getNameAsString().equals("DeadLetterPublishingRecoverer")) continue;
                declare(analysis, "DeadLetterPublishingRecoverer", creation);
                var partition = creation.findAll(ObjectCreationExpr.class).stream()
                        .filter(c -> c.getType().getNameAsString().equals("TopicPartition")).findFirst().orElseThrow();
                for (var key : trace(partition.getArgument(0),
                        evidence(source.evidence(creation), source.evidence(partition))))
                    add(analysis, true, "DeadLetterPublishingRecoverer", creation, key);
            }
        }
    }

    private List<Fact<String>> trace(Expression expression, List<String> chain) {
        var ev = evidence(chain, source.evidence(expression));
        if (expression.isEnclosedExpr()) return trace(expression.asEnclosedExpr().getInner(), ev);
        String name;
        boolean explicitField = expression.isFieldAccessExpr() && expression.asFieldAccessExpr().getScope().isThisExpr();
        if (expression.isStringLiteralExpr() || expression.isFieldAccessExpr() && !explicitField)
            return List.of(property(expression, ev));
        if (expression.isNameExpr()) name = expression.asNameExpr().getNameAsString();
        else if (explicitField) name = expression.asFieldAccessExpr().getNameAsString();
        else return List.of(unknown(ev));

        MethodDeclaration method = ancestor(expression, MethodDeclaration.class);
        if (method != null && !explicitField) {
            for (int index = 0; index < method.getParameters().size(); index++) {
                if (!method.getParameter(index).getNameAsString().equals(name)) continue;
                var calls = callers.getOrDefault(location(method), List.of());
                if (calls.isEmpty()) return List.of(unknown(ev));
                var result = new ArrayList<Fact<String>>();
                for (var call : calls) result.addAll(trace(call.getArgument(index),
                        evidence(ev, source.evidence(call))));
                return result;
            }
        }
        TypeDeclaration<?> owner = ancestor(expression, TypeDeclaration.class);
        if (owner != null) for (var field : owner.getFields()) for (var variable : field.getVariables()) {
            if (!variable.getNameAsString().equals(name)) continue;
            var annotation = field.getAnnotationByName("Value");
            if (annotation.isPresent() && annotation.get().isSingleMemberAnnotationExpr()) {
                var value = annotation.get().asSingleMemberAnnotationExpr().getMemberValue();
                return List.of(property(value, evidence(ev, source.evidence(annotation.get()))));
            }
        }
        return List.of(property(expression, ev));
    }

    private Fact<String> property(Expression expression, List<String> ev) {
        if (expression.isStringLiteralExpr()) {
            var matcher = TOPIC.matcher(expression.asStringLiteralExpr().asString());
            if (matcher.matches()) return Fact.known(expression.asStringLiteralExpr().asString(), ev);
        }
        var value = new SourceValues(source).value(expression);
        ev = evidence(ev, value.evidence());
        if (value.known()) {
            try {
                if (expression.isStringLiteralExpr() || expression.calculateResolvedType().describe().equals("java.lang.String"))
                    return Fact.known(value.value(), ev);
            } catch (RuntimeException ignored) {
                // Only a resolved string constant can supply a literal topic name.
            }
        }
        return unknown(ev);
    }
    private static Fact<String> unknown(List<String> ev) {
        return Fact.unknown(ev, "토픽 값을 확인하지 못함");
    }
    private String location(Node node) { return source.evidence(node).get(0); }
    private void declare(Analysis analysis, String kind, Node node) {
        analysis.kafkaDeclarations.add(new Declaration(kind, Analysis.CONFIRMED, source.evidence(node)));
    }
    private void add(Analysis analysis, boolean publication, String kind, Node node, Fact<String> topic) {
        var list = publication ? analysis.kafkaPublications : analysis.kafkaSubscriptions;
        var key = topic;
        Fact<String> name;
        Fact<String> env;
        var matcher = TOPIC.matcher(topic.known() ? topic.value() : "");
        if (topic.known() && !matcher.matches()) {
            name = topic;
            key = Fact.known("", topic.evidence());
            env = Fact.known("", topic.evidence());
        } else {
            if (topic.known()) key = Fact.known(matcher.group(1), topic.evidence());
            env = config.topicEnvironment(source.serviceName(), key);
            name = env.known()
                    ? new Fact<>(null, Analysis.ABSENT, env.evidence(), "실제 토픽 이름은 저장소 밖의 환경변수 값에서 정해짐")
                    : Fact.unknown(env.evidence(), env.reason());
        }
        var ev = evidence(source.evidence(node), key.evidence(), name.evidence(), env.evidence());
        String id = (publication ? "kafka-publication-" : "kafka-subscription-") + (list.size() + 1);
        list.add(new KafkaEndpoint(id, source.serviceName(), kind, "base", key, name, env, env.status(), ev));
    }

    private MethodDeclaration target(MethodCallExpr call) {
        try {
            var resolved = call.resolve().toAst();
            if (resolved.isPresent() && resolved.get() instanceof MethodDeclaration method && source.owns(method)) return method;
        } catch (RuntimeException ignored) {
            // Missing framework types can prevent resolution; use a unique local receiver declaration only.
        }
        TypeDeclaration<?> owner = ancestor(call, TypeDeclaration.class);
        if (owner == null) return null;
        if (call.getScope().isPresent() && !call.getScope().get().isThisExpr()) {
            var scope = call.getScope().get();
            String name = scope.isNameExpr() ? scope.asNameExpr().getNameAsString()
                    : scope.isFieldAccessExpr() && scope.asFieldAccessExpr().getScope().isThisExpr()
                    ? scope.asFieldAccessExpr().getNameAsString() : null;
            if (name == null) return null;
            TypeDeclaration<?> receiver = null;
            for (var field : owner.getFields()) for (var variable : field.getVariables())
                if (variable.getNameAsString().equals(name) && variable.getType().isClassOrInterfaceType())
                    receiver = source.sourceType(variable.getType().asClassOrInterfaceType());
            if (receiver == null) return null;
            owner = receiver;
        }
        var methods = owner.getMethodsByName(call.getNameAsString()).stream()
                .filter(m -> m.getParameters().size() == call.getArguments().size()).toList();
        return methods.size() == 1 ? methods.get(0) : null;
    }

    private static <T extends Node> T ancestor(Node node, Class<T> type) {
        while ((node = node.getParentNode().orElse(null)) != null)
            if (type.isInstance(node)) return type.cast(node);
        return null;
    }

    public static void link(Analysis analysis) {
        for (var publication : analysis.kafkaPublications) for (var subscription : analysis.kafkaSubscriptions) {
            if (publication.environment().known() && subscription.environment().known()
                    && !publication.environment().value().isEmpty()
                    && publication.environment().value().equals(subscription.environment().value()))
                analysis.kafkaLinks.add(new KafkaLink(publication.id(), subscription.id(), publication.environment().value(),
                        Analysis.CONFIRMED, evidence(publication.evidence(), subscription.evidence())));
        }
    }
}
