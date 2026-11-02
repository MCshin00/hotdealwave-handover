package io.hotdealwave.extractor.rest;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.type.Type;
import io.hotdealwave.extractor.model.Analysis;
import io.hotdealwave.extractor.model.Analysis.*;
import io.hotdealwave.extractor.source.SourceContext;
import io.hotdealwave.extractor.source.SourceValues;
import java.util.*;

public final class DtoExtractor {
    private static final Set<String> FIELD_ANNOTATIONS = Set.of("NotNull", "NotBlank", "Size", "Min", "Max",
            "Digits", "Positive", "Pattern", "Valid");
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
                    Fact<String> jsonName = jsonName(declaration, field, variable);
                    result.fields.add(new Field(variable.getNameAsString(), extract(variable.getType(), local, next),
                            annotations, jsonName, Analysis.CONFIRMED, fieldEvidence));
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

    private Fact<String> jsonName(TypeDeclaration<?> declaration, FieldDeclaration field,
                                  VariableDeclarator variable) {
        var evidence = source.evidence(variable);
        var annotations = new ArrayList<>(field.getAnnotations());
        annotations.addAll(declaration.getAnnotations());

        String name = variable.getNameAsString();
        String suffix = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        boolean primitiveBoolean = variable.getType().isPrimitiveType()
                && variable.getType().asString().equals("boolean");
        String booleanGetter = name.startsWith("is") && name.length() > 2 && Character.isUpperCase(name.charAt(2))
                ? name : "is" + suffix;
        var lombok = annotations.stream()
                .filter(a -> Set.of("Getter", "Data").contains(SourceValues.name(a))).findFirst();
        if (lombok.isPresent()) {
            String getter = primitiveBoolean ? booleanGetter : "get" + suffix;
            return Fact.known(getterProperty(getter), Analysis.evidence(evidence, source.evidence(lombok.get())));
        }

        for (var method : declaration.getMethods()) {
            if (!method.isPublic() || method.isStatic() || !method.getParameters().isEmpty()
                    || method.getType().isVoidType()) continue;
            String getter = method.getNameAsString();
            boolean booleanResult = Set.of("boolean", "Boolean", "java.lang.Boolean").contains(method.getType().asString());
            if (getter.equals("get" + suffix) || (booleanResult
                    && (getter.equals("is" + suffix) || getter.equals(booleanGetter)))) {
                return Fact.known(getterProperty(getter), Analysis.evidence(evidence, source.evidence(method)));
            }
        }
        if (field.isPublic()) return Fact.known(name, evidence);
        return Fact.unknown(evidence, "JSON 이름을 정할 Lombok getter, 직접 선언한 public getter 또는 public 필드가 없음");
    }

    private String getterProperty(String getter) {
        String suffix = getter.substring(getter.startsWith("is") ? 2 : 3);
        // Jackson's default getter naming lowercases the leading uppercase run (getURL -> url).
        var property = new StringBuilder(suffix);
        for (int i = 0; i < suffix.length(); i++) {
            char lower = Character.toLowerCase(suffix.charAt(i));
            if (lower == suffix.charAt(i)) break;
            property.setCharAt(i, lower);
        }
        return property.toString();
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
