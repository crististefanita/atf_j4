package atf.tests.governance.source;

import atf.core.files.AtfJavaSourceReferences;
import atf.tests.governance.support.AtfJavaSourceInventory;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

/** Resolves composed layer markers; nested fixtures never label their enclosing class. */
public final class TestLayerRules {
    private static final Set<String> LAYER_ANNOTATIONS = Set.of(
            "@AtfUnitTest", "@AtfComponentTest", "@AtfIntegrationTest", "@AtfE2eTest");
    private static final Pattern TEST_DECLARATION = Pattern.compile(
            "(?m)^\\s*@(Test|ParameterizedTest|TestFactory|TestTemplate|RepeatedTest|Suite|SelectPackages)\\b");
    private static final Pattern ANNOTATION = Pattern.compile("(?m)^\\s*@([\\w.]+)\\b");
    private static final Pattern ANNOTATION_TYPE = Pattern.compile(
            "(?m)^\\s*(?:public\\s+)?@interface\\s+(\\w+)\\b");
    private static final Pattern PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Pattern IMPORT = Pattern.compile("(?m)^\\s*import\\s+([\\w.]+)\\s*;");
    private static final Pattern TYPE_DECLARATION = Pattern.compile(
            "(?m)^\\s*(?:(?:public|protected|private|static|final|abstract|sealed|non-sealed)\\s+)*"
                    + "(?:class|interface|enum|record|@interface)\\s+\\w+");

    private TestLayerRules() {}
    public static boolean isConcreteExecutable(String source) {
        String code=AtfJavaSourceReferences.codeOnly(source);
        var type=TYPE_DECLARATION.matcher(code);
        if (!type.find() || type.group().contains("abstract ") || type.group().contains("interface")) return false;
        return TEST_DECLARATION.matcher(code).find();
    }
    public static List<String> declaredLayers(String source, Map<String, String> annotations) {
        return declaredAnnotations(source, annotations, LAYER_ANNOTATIONS);
    }

    /** Shared annotation graph for layer, reporting-marker and suite governance. */
    public static List<String> declaredAnnotations(String source, Map<String, String> annotations, Set<String> terminals) {
        return declaredAnnotations(source, annotations, terminals, new HashSet<>());
    }

    private static List<String> declaredAnnotations(String source, Map<String, String> annotations,
                                                   Set<String> terminals, Set<String> resolving) {
        source = atf.core.files.AtfJavaSourceReferences.codeOnly(source);
        var declaration = TYPE_DECLARATION.matcher(source);
        if (declaration.find()) source = source.substring(0, declaration.start());
        var imports = new HashMap<String, String>();
        IMPORT.matcher(source).results().forEach(match -> {
            String name = match.group(1);
            imports.put(name.substring(name.lastIndexOf('.') + 1), name);
        });
        var packageMatcher = PACKAGE.matcher(source);
        String packagePrefix = packageMatcher.find() ? packageMatcher.group(1) + "." : "";
        var result = new ArrayList<String>();
        ANNOTATION.matcher(source).results().forEach(match -> {
            String name = match.group(1);
            String layer = "@" + name.substring(name.lastIndexOf('.') + 1);
            if (terminals.contains(layer)) {
                result.add(layer);
                return;
            }
            String type = imports.getOrDefault(name, name.contains(".") ? name : packagePrefix + name);
            String composed = annotations.get(type);
            if (composed != null && resolving.add(type)) {
                result.addAll(declaredAnnotations(composed, annotations, terminals, resolving));
                resolving.remove(type);
            }
        });
        return List.copyOf(result);
    }

    public static Map<String, String> annotationSources(Path root) throws Exception {
        var result = new HashMap<String, String>();
        Set<Path> sources = new LinkedHashSet<>(AtfJavaSourceInventory.mainSources(root));
        sources.addAll(AtfJavaSourceInventory.testSources(root));
        for (Path file : sources) {
            String source = Files.readString(file);
            var owner = PACKAGE.matcher(source);
            if (!owner.find()) continue;
            var type = ANNOTATION_TYPE.matcher(source);
            while (type.find()) {
                String prefix = source.substring(0, type.start()).replace("\r\n", "\n");
                int block = prefix.lastIndexOf("\n\n");
                String header = "package " + owner.group(1) + ";\n"
                        + IMPORT.matcher(source).results()
                                .map(match -> "import " + match.group(1) + ";\n")
                                .collect(java.util.stream.Collectors.joining());
                result.put(owner.group(1) + "." + type.group(1),
                        header + prefix.substring(block < 0 ? 0 : block));
            }
        }
        return Map.copyOf(result);
    }


}
