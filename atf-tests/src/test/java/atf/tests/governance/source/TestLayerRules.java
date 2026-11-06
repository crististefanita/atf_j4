package atf.tests.governance.source;

import atf.tests.governance.support.AtfJavaSourceInventory;
import com.sun.source.tree.Tree;
import java.nio.file.*;
import java.util.*;

/** Resolves composed layer markers; nested fixtures never label their enclosing class. */
public final class TestLayerRules {
    private static final Set<String> LAYER_ANNOTATIONS = Set.of(
            "@AtfUnitTest", "@AtfComponentTest", "@AtfIntegrationTest", "@AtfE2eTest");
    private static final Set<String> TEST_METHODS = Set.of(
            "Test", "ParameterizedTest", "TestFactory", "TestTemplate", "RepeatedTest");
    private static final Set<String> SUITES = Set.of("Suite", "SelectPackages");

    record Inspection(String type, boolean executable, List<String> layers) { }

    private TestLayerRules() {}
    public record ExecutableSource(String module, String name, List<String> layers, boolean suite, String source) { }
    public static List<ExecutableSource> executableSources(Path root) throws Exception {
        var annotations = annotationSources(root);
        var result = new ArrayList<ExecutableSource>();
        try (var parser = JavaTestDeclarations.open()) {
            for (var sourceRoot : AtfJavaSourceInventory.roots(root)) if (sourceRoot.test()) {
                for (Path file : AtfJavaSourceInventory.files(sourceRoot.directory())) {
                    String source = Files.readString(file);
                    var unit = parser.parse(source);
                    for (var type : unit.types()) if (executable(type)) result.add(new ExecutableSource(
                            root.relativize(sourceRoot.module()).toString().replace('\\', '/'),
                            unit.packageName() + "." + type.name(),
                            resolve(type, unit, annotations, LAYER_ANNOTATIONS, new HashSet<>(), parser),
                            !resolve(type, unit, annotations, Set.of("@Suite"), new HashSet<>(), parser).isEmpty(), source));
                }
            }
        }
        return List.copyOf(result);
    }
    public static Map<String, List<String>> suiteSelectors(String source, Map<String, String> knownTypes) {
        var unit = JavaTestDeclarations.parse(source);
        var selectors = new HashMap<>(unit.types().getFirst().selectors());
        if (selectors.containsKey("SelectClasses")) selectors.put("SelectClasses", selectors.get("SelectClasses").stream()
                .map(name -> unit.qualified(name, knownTypes)).toList());
        return Map.copyOf(selectors);
    }
    public static boolean isConcreteExecutable(String source) {
        return JavaTestDeclarations.parse(source).types().stream().anyMatch(TestLayerRules::executable);
    }
    public static List<String> declaredLayers(String source, Map<String, String> annotations) {
        return declaredAnnotations(source, annotations, LAYER_ANNOTATIONS);
    }

    static List<Inspection> inspect(String source, Map<String, String> annotations) {
        try (var parser = JavaTestDeclarations.open()) { return inspect(source, annotations, parser); }
    }

    static List<Inspection> inspect(String source, Map<String, String> annotations, JavaTestDeclarations.Parser parser) {
        var unit = parser.parse(source);
        return unit.types().stream().map(type -> new Inspection(type.name(), executable(type),
                resolve(type, unit, annotations, LAYER_ANNOTATIONS, new HashSet<>(), parser))).toList();
    }

    /** Shared annotation graph for layer, reporting-marker and suite governance. */
    public static List<String> declaredAnnotations(String source, Map<String, String> annotations, Set<String> terminals) {
        try (var parser = JavaTestDeclarations.open()) {
            var unit = parser.parse(source);
            var selected = unit.types().stream().filter(TestLayerRules::executable).findFirst()
                    .orElse(unit.types().isEmpty() ? null : unit.types().getFirst());
            return selected == null ? List.of() : resolve(selected, unit, annotations, terminals, new HashSet<>(), parser);
        }
    }

    private static List<String> resolve(JavaTestDeclarations.Type declaration, JavaTestDeclarations.Unit unit,
                                        Map<String, String> annotations, Set<String> terminals, Set<String> resolving,
                                        JavaTestDeclarations.Parser parser) {
        var result = new ArrayList<String>();
        for (String name : declaration.annotations()) {
            String layer = "@" + simple(name);
            if (terminals.contains(layer)) {
                result.add(layer);
                continue;
            }
            String type = unit.qualified(name, annotations);
            String composed = annotations.get(type);
            if (composed != null && resolving.add(type)) {
                try {
                    var meta = parser.parse(composed);
                    for (var candidate : meta.types())
                        result.addAll(resolve(candidate, meta, annotations, terminals, resolving, parser));
                } finally { resolving.remove(type); }
            }
        }
        return List.copyOf(result);
    }

    private static boolean executable(JavaTestDeclarations.Type type) {
        if (!type.concrete()) return false;
        if (type.annotations().stream().map(TestLayerRules::simple).anyMatch(SUITES::contains)) return true;
        if (type.methods().stream().flatMap(List::stream).map(TestLayerRules::simple).anyMatch(TEST_METHODS::contains)) return true;
        return type.nested().stream().anyMatch(nested -> nested.annotations().stream()
                .map(TestLayerRules::simple).anyMatch("Nested"::equals) && executable(nested));
    }

    private static String simple(String name) { return name.substring(name.lastIndexOf('.') + 1); }

    public static Map<String, String> annotationSources(Path root) throws Exception {
        var result = new HashMap<String, String>();
        try (var parser = JavaTestDeclarations.open()) {
            for (Path file : AtfJavaSourceInventory.allSources(root)) {
                var unit = parser.parse(Files.readString(file));
                for (var type : unit.types()) collectAnnotations(unit, type, unit.packageName(), result);
            }
        }
        return Map.copyOf(result);
    }

    private static void collectAnnotations(JavaTestDeclarations.Unit unit, JavaTestDeclarations.Type declaration,
                                            String owner, Map<String, String> result) {
        String name = owner.isEmpty() ? declaration.name() : owner + "." + declaration.name();
        if (declaration.kind() == Tree.Kind.ANNOTATION_TYPE) {
            String enclosingImport = owner.equals(unit.packageName()) ? "" : "import " + owner + ".*;\n";
            String previous = result.putIfAbsent(name, unit.header() + enclosingImport + declaration.annotationSource());
            if (previous != null) throw new IllegalArgumentException("GOV-ANNOTATION-DUPLICATE: " + name);
        }
        for (var nested : declaration.nested()) collectAnnotations(unit, nested, name, result);
    }
}
