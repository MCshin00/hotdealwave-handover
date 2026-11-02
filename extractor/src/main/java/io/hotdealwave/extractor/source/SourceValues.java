package io.hotdealwave.extractor.source;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import io.hotdealwave.extractor.model.Analysis;
import io.hotdealwave.extractor.model.Analysis.AnnotationInfo;
import io.hotdealwave.extractor.model.Analysis.Fact;
import java.util.*;

public final class SourceValues {
    private final SourceContext source;
    public SourceValues(SourceContext source) { this.source = source; }
    public static String name(AnnotationExpr a) { return a.getName().getIdentifier(); }
    public static Optional<Expression> argument(AnnotationExpr a, String key) {
        if (a.isSingleMemberAnnotationExpr() && key.equals("value"))
            return Optional.of(a.asSingleMemberAnnotationExpr().getMemberValue());
        if (a.isNormalAnnotationExpr()) return a.asNormalAnnotationExpr().getPairs().stream()
                .filter(p -> p.getNameAsString().equals(key)).map(MemberValuePair::getValue).findFirst();
        return Optional.empty();
    }
    public Fact<String> attribute(AnnotationExpr a, String key, String fallback) {
        return argument(a, key).map(this::value).orElseGet(() -> Fact.known(fallback, source.evidence(a)));
    }
    public Fact<String> value(Expression expression) { return value(expression, new HashSet<>()); }
    private Fact<String> value(Expression expression, Set<String> visited) {
        var evidence = source.evidence(expression);
        if (expression.isStringLiteralExpr()) {
            String text = expression.asStringLiteralExpr().asString();
            return text.contains("${") || text.contains("#{")
                    ? Fact.unknown(evidence, "환경 또는 식에서 정해지는 값") : Fact.known(text, evidence);
        }
        if (expression.isBooleanLiteralExpr() || expression.isIntegerLiteralExpr()
                || expression.isLongLiteralExpr() || expression.isDoubleLiteralExpr())
            return Fact.known(expression.toString(), evidence);
        if (expression.isNameExpr() || expression.isFieldAccessExpr()) {
            try {
                var resolved = expression.isNameExpr() ? expression.asNameExpr().resolve()
                        : expression.asFieldAccessExpr().resolve();
                Node ast = resolved.toAst().orElse(null);
                VariableDeclarator variable = ast instanceof VariableDeclarator v ? v
                        : ast instanceof FieldDeclaration f ? f.getVariables().stream()
                        .filter(v -> v.getNameAsString().equals(resolved.getName())).findFirst().orElse(null) : null;
                if (variable != null && source.owns(variable)
                        && variable.getParentNode().orElse(null) instanceof FieldDeclaration field
                        && (field.isFinal() || field.getParentNode()
                        .filter(n -> n instanceof com.github.javaparser.ast.body.ClassOrInterfaceDeclaration c && c.isInterface()).isPresent())
                        && variable.getInitializer().isPresent()) {
                    String key = source.evidence(variable).get(0);
                    if (visited.add(key)) {
                        var result = value(variable.getInitializer().get(), visited);
                        return new Fact<>(result.value(), result.status(), Analysis.evidence(evidence,
                                source.evidence(variable), result.evidence()), result.reason());
                    }
                }
            } catch (RuntimeException ignored) {
                // Cross-module constants and missing symbols remain unresolved.
            }
        }
        return Fact.unknown(evidence, "같은 서비스의 리터럴·상수로 확정할 수 없음: " + expression);
    }
    public List<Fact<String>> values(Expression e) {
        return e.isArrayInitializerExpr() ? e.asArrayInitializerExpr().getValues().stream().map(this::value).toList()
                : List.of(value(e));
    }
    public AnnotationInfo annotation(AnnotationExpr a) {
        Map<String, Fact<String>> arguments = new LinkedHashMap<>();
        if (a.isSingleMemberAnnotationExpr()) arguments.put("value", value(a.asSingleMemberAnnotationExpr().getMemberValue()));
        if (a.isNormalAnnotationExpr()) a.asNormalAnnotationExpr().getPairs()
                .forEach(p -> arguments.put(p.getNameAsString(), value(p.getValue())));
        return new AnnotationInfo(name(a), arguments, arguments.values().stream().allMatch(Fact::known)
                ? Analysis.CONFIRMED : Analysis.UNKNOWN, source.evidence(a));
    }
}
