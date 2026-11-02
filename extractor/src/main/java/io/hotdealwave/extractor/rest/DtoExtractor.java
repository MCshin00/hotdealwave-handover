package io.hotdealwave.extractor.rest;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.type.Type;
import io.hotdealwave.extractor.model.Analysis;
import io.hotdealwave.extractor.model.Analysis.*;
import io.hotdealwave.extractor.source.SourceContext;
import io.hotdealwave.extractor.source.SourceValues;
import java.util.*;

public final class DtoExtractor {
    private static final Set<String> FIELD_ANNOTATIONS = Set.of("Null", "NotNull", "NotEmpty", "NotBlank",
            "Size", "Min", "Max", "DecimalMin", "DecimalMax", "Digits", "Positive", "PositiveOrZero",
            "Negative", "NegativeOrZero", "Past", "PastOrPresent", "Future", "FutureOrPresent",
            "Pattern", "Email", "AssertTrue", "AssertFalse", "Valid", "JsonProperty", "JsonIgnore");
    private final SourceContext source;
    private final SourceValues values;
    private record Binding(Type type, Map<String, Binding> environment) {}

    public DtoExtractor(SourceContext source) {
        this.source = source;
        values = new SourceValues(source);
    }
    public TypeView extract(Type type) { return extract(type, Map.of(), new HashSet<>()); }

    private TypeView extract(Type type, Map<String, Binding> environment, Set<String> visiting) {
        if (environment.containsKey(type.asString())) {
            var binding = environment.get(type.asString());
            return extract(binding.type(), binding.environment(), visiting);
        }
        var result = new TypeView();
        result.type = display(type, environment);
        result.evidence = source.evidence(type);
        result.status = Analysis.CONFIRMED;
        if (type.isPrimitiveType() || type.isVoidType()) {
            result.kind = "scalar";
            return result;
        }
        if (type.isArrayType()) {
            result.kind = "array";
            result.arguments.add(extract(type.asArrayType().getComponentType(), environment, visiting));
            return result;
        }
        if (!type.isClassOrInterfaceType()) {
            result.kind = "unresolved";
            result.status = Analysis.UNKNOWN;
            result.reason = "구체 타입을 정할 수 없음";
            return result;
        }
        var reference = type.asClassOrInterfaceType();
        var declaration = source.sourceType(reference);
        var arguments = reference.getTypeArguments().map(ArrayList::new).orElseGet(ArrayList::new);
        if (declaration != null) {
            result.qualifiedName = declaration.getFullyQualifiedName().orElseThrow();
            result.evidence = source.evidence(declaration.getName());
            result.kind = declaration.isEnumDeclaration() ? "enum" : "object";
            // Stop at the first repeated source class, including recursively expanding generics.
            if (visiting.contains(result.qualifiedName)) {
                result.recursive = true;
                return result;
            }
            var next = new HashSet<>(visiting);
            next.add(result.qualifiedName);
            for (var argument : arguments) result.arguments.add(extract(argument, environment, next));
            var local = new HashMap<String, Binding>();
            if (declaration instanceof ClassOrInterfaceDeclaration clazz) {
                for (int i = 0; i < Math.min(clazz.getTypeParameters().size(), arguments.size()); i++)
                    local.put(clazz.getTypeParameters().get(i).getNameAsString(), new Binding(arguments.get(i), environment));
            }
            if (declaration instanceof EnumDeclaration enumeration) {
                enumeration.getEntries().forEach(e -> result.enumValues.add(e.getNameAsString()));
                return result;
            }
            for (var field : declaration.getFields()) {
                if (field.isStatic()) continue;
                var annotations = field.getAnnotations().stream()
                        .filter(a -> FIELD_ANNOTATIONS.contains(SourceValues.name(a))).map(values::annotation).toList();
                for (var variable : field.getVariables()) {
                    var fieldEvidence = source.evidence(variable);
                    Fact<String> jsonName = Fact.known(variable.getNameAsString(), fieldEvidence);
                    Fact<Boolean> ignored = Fact.known(false, fieldEvidence);
                    for (var annotation : field.getAnnotations()) {
                        if (SourceValues.name(annotation).equals("JsonProperty")) {
                            var name = values.attribute(annotation, "value", "");
                            jsonName = name.known() && name.value().isEmpty() ? jsonName : name;
                        }
                        if (SourceValues.name(annotation).equals("JsonIgnore")) {
                            var value = values.attribute(annotation, "value", "true");
                            ignored = value.known() ? Fact.known(Boolean.valueOf(value.value()), value.evidence())
                                    : Fact.unknown(value.evidence(), value.reason());
                        }
                    }
                    result.fields.add(new Field(variable.getNameAsString(), extract(variable.getType(), local, next),
                            annotations, jsonName, ignored, Analysis.CONFIRMED, fieldEvidence));
                }
            }
            return result;
        }
        for (var argument : arguments) result.arguments.add(extract(argument, environment, visiting));
        try {
            var resolved = type.resolve();
            if (resolved.isReferenceType()) {
                result.qualifiedName = resolved.asReferenceType().getQualifiedName();
                if (result.qualifiedName.startsWith("java.")) {
                    result.kind = arguments.isEmpty() ? "scalar" : "container";
                    return result;
                }
            }
        } catch (RuntimeException ignored) { }
        result.kind = "unresolved";
        result.status = Analysis.UNKNOWN;
        result.reason = "서비스 소스와 JDK에서 타입 구조를 해석할 수 없음; 제네릭 인자는 별도 표시";
        return result;
    }

    private String display(Type type, Map<String, Binding> environment) {
        var binding = environment.get(type.asString());
        if (binding != null) return display(binding.type(), binding.environment());
        if (type.isClassOrInterfaceType()) {
            var c = type.asClassOrInterfaceType();
            return c.getNameWithScope() + c.getTypeArguments().filter(a -> !a.isEmpty())
                    .map(a -> "<" + String.join(", ", a.stream().map(t -> display(t, environment)).toList()) + ">")
                    .orElse("");
        }
        return type.asString();
    }
}
