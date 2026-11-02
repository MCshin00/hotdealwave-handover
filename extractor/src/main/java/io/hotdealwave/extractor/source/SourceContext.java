package io.hotdealwave.extractor.source;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import io.hotdealwave.extractor.scan.ServiceScanner.ServiceSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SourceContext {
    public final ServiceSource service;
    public final List<CompilationUnit> units = new ArrayList<>();
    public final List<Path> files;
    public final Map<String, TypeDeclaration<?>> types = new LinkedHashMap<>();
    private final Path root;
    private final CombinedTypeSolver solver;

    public SourceContext(Path root, ServiceSource service) throws IOException {
        this.root = root;
        this.service = service;
        var configuration = new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        solver = new CombinedTypeSolver(new JavaParserTypeSolver(service.javaRoot(), configuration),
                new ReflectionTypeSolver());
        configuration.setSymbolResolver(new JavaSymbolSolver(solver));
        var parser = new JavaParser(configuration);
        try (var paths = Files.walk(service.javaRoot())) {
            files = paths.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
        for (Path path : files) {
            var result = parser.parse(path);
            // Never publish a partial successful analysis after a parse failure.
            if (!result.isSuccessful()) throw new IOException("Java 파싱 실패: " + relative(path));
            var unit = result.getResult().orElseThrow();
            units.add(unit);
            for (TypeDeclaration<?> type : unit.findAll(TypeDeclaration.class)) {
                type.getFullyQualifiedName().ifPresent(n -> types.put(n, type));
            }
        }
    }
    public String serviceName() { return service.model().name().value(); }
    public String relative(Path path) { return root.relativize(path).toString().replace('\\', '/'); }
    public List<String> evidence(Node node) {
        Path file = node.findCompilationUnit().orElseThrow().getStorage().orElseThrow().getPath();
        return List.of(relative(file) + ":" + node.getBegin().orElseThrow().line);
    }
    public boolean owns(Node node) {
        return node.findCompilationUnit().flatMap(CompilationUnit::getStorage)
                .map(s -> s.getPath().normalize().startsWith(service.javaRoot().toAbsolutePath().normalize()))
                .orElse(false);
    }
    public TypeDeclaration<?> sourceType(ClassOrInterfaceType type) {
        try {
            var resolved = type.resolve();
            if (resolved.isReferenceType()) {
                var found = types.get(resolved.asReferenceType().getQualifiedName());
                if (found != null) return found;
            }
        } catch (RuntimeException ignored) {
            // An unavailable framework generic argument must not hide a local wrapper.
        }
        var candidates = new ArrayList<String>();
        String name = type.getNameWithScope();
        candidates.add(name);
        var unit = type.findCompilationUnit().orElseThrow();
        unit.getImports().stream().filter(i -> !i.isStatic()).forEach(i -> {
            if (i.isAsterisk()) candidates.add(i.getNameAsString() + "." + name);
            else if (i.getName().getIdentifier().equals(name.split("\\.")[0])) {
                candidates.add(i.getNameAsString() + name.substring(name.split("\\.")[0].length()));
            }
        });
        unit.getPackageDeclaration().ifPresent(p -> candidates.add(p.getNameAsString() + "." + name));
        Node parent = type;
        while ((parent = parent.getParentNode().orElse(null)) != null) {
            if (parent instanceof TypeDeclaration<?> declaration) {
                declaration.getFullyQualifiedName().ifPresent(n -> candidates.add(n + "." + name));
            }
        }
        for (String candidate : candidates) {
            if (!types.containsKey(candidate)) continue;
            var solved = solver.tryToSolveType(candidate);
            if (solved.isSolved()) return types.get(solved.getCorrespondingDeclaration().getQualifiedName());
        }
        return null;
    }
}
