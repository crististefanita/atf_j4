package atf.tests.governance.documentation.factual;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static atf.tests.governance.documentation.factual.BuildModuleDeclarations.*;

final class DocumentedModuleMap {
    private static final String MODULES = "| Build | Module declarate |";
    private static final String DEPENDENCIES = "| Build | Consumator | Configurație | Dependențe directe |";
    private static final Pattern ARROW = Pattern.compile("(?m)^\\h*([\\w-]+)\\h*-->\\|[^|]+\\|\\h*([\\w-]+)\\h*$");

    private DocumentedModuleMap() { }

    static List<String> inspect(Path root, String readme) throws Exception {
        Path gradle = root.resolve("whist-lab-app");
        var maven = mavenModules(root);
        var android = gradleModules(gradle.resolve("settings.gradle"));
        var jvm = gradleModules(gradle.resolve("algorithm-build/settings.gradle"));
        var failures = new ArrayList<String>();
        String map = section(readme.replace("\r\n", "\n"), "## Harta modulelor", "\n## ");
        var inventories = Map.of("Maven", maven, "Gradle", android, "Gradle JVM", jvm);
        var declaredBuilds = new LinkedHashSet<String>();
        for (var row : rows(map, MODULES, 2)) {
            if (!declaredBuilds.add(row[0])) failures.add("DOC-BUILD-MAP README.md: duplicate build " + row[0]);
            compare(failures, row[0] + " modules", inventories.getOrDefault(row[0], Set.of()), values(row[1]));
        }
        compare(failures, "build inventories", inventories.keySet(), declaredBuilds);

        var expected = mavenEdges(root, maven);
        var gradleDependencies = gradleEdges(gradle, android);
        expected.addAll(gradleDependencies);
        var declared = new LinkedHashSet<Edge>();
        for (var row : rows(map, DEPENDENCIES, 4)) {
            for (String target : values(row[3])) {
                var edge = new Edge(row[0], row[1], row[2], target);
                if (!declared.add(edge)) failures.add("DOC-BUILD-MAP README.md: duplicate dependency " + edge);
            }
        }
        compare(failures, "dependency scopes (POM, build.gradle, component-audit.gradle)", expected, declared);

        var actualArrows = new LinkedHashSet<String>();
        var product = section(map, "### Motorul și platformele", "\n### ");
        var diagram = ARROW.matcher(section(product, "flowchart LR", "\n" + "```"));
        while (diagram.find()) {
            String arrow = diagram.group(1).replace('_', '-') + " -> " + diagram.group(2).replace('_', '-');
            if (!actualArrows.add(arrow)) failures.add("DOC-BUILD-MAP README.md: duplicate diagram edge " + arrow);
        }
        var productArrows = new LinkedHashSet<String>();
        for (var edge : gradleDependencies) {
            if (Set.of("app", "ios-bridge", "game-core").contains(edge.owner())
                    && Set.of("api", "implementation", "runtimeOnly").contains(edge.scope())) {
                productArrows.add(edge.owner() + " -> " + edge.target());
            }
        }
        productArrows.add("ios -> ios-bridge");
        compare(failures, "product diagram edges", productArrows, actualArrows);
        return List.copyOf(failures);
    }

    private static List<String[]> rows(String map, String header, int width) {
        int first = map.indexOf(header + "\n");
        if (first < 0 || map.indexOf(header + "\n", first + 1) >= 0) {
            throw new IllegalArgumentException("DOC-BUILD-MAP README.md: expected one " + header);
        }
        return map.substring(first).lines().skip(2).takeWhile(line -> line.startsWith("|")).map(line -> {
            String[] cells = line.substring(1, line.length() - 1).split("\\|", -1);
            if (!line.endsWith("|") || cells.length != width) {
                throw new IllegalArgumentException("DOC-BUILD-MAP README.md: malformed row " + line);
            }
            return java.util.Arrays.stream(cells).map(DocumentedModuleMap::label).toArray(String[]::new);
        }).toList();
    }

    private static Set<String> values(String text) {
        var result = new LinkedHashSet<String>();
        for (String value : text.split(",", -1)) {
            if (!result.add(label(value)) || label(value).isEmpty()) {
                throw new IllegalArgumentException("DOC-BUILD-MAP README.md: blank or duplicate value in " + text);
            }
        }
        return result;
    }

    private static String label(String value) {
        return value.replace("\u0060", "").strip();
    }

    private static String section(String content, String heading, String end) {
        int start = content.indexOf(heading + "\n");
        if (start < 0) return "";
        int stop = content.indexOf(end, start + heading.length());
        return content.substring(start, stop < 0 ? content.length() : stop);
    }

    private static <T> void compare(List<String> failures, String claim, Set<T> expected, Set<T> declared) {
        if (expected.equals(declared)) return;
        var missing = new LinkedHashSet<>(expected);
        missing.removeAll(declared);
        var unexpected = new LinkedHashSet<>(declared);
        unexpected.removeAll(expected);
        failures.add("DOC-BUILD-MAP README.md / Harta modulelor: " + claim
                + " | missing=" + missing + " | unexpected=" + unexpected);
    }
}
