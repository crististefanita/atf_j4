package atf.tests.governance.documentation.factual;

import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

final class BuildModuleDeclarations {
    record Edge(String build, String owner, String scope, String target) { }

    private static final Pattern INCLUDE = Pattern.compile("\\binclude\\h*\\(([^\\r\\n]*)\\)");
    private static final Pattern PROJECT = Pattern.compile("['\"]:([\\w-]+)['\"]");
    private static final Pattern DEPENDENCY = Pattern.compile(
            "(?m)^\\h*(\\w+)\\h*\\(?\\h*project\\h*\\(\\h*" + PROJECT + "\\h*\\)\\h*\\)?\\h*(?://.*)?$");
    private static final Pattern FIXTURES = Pattern.compile(
            "owner\\.dependencies\\.add\\(\\s*['\"](\\w+)['\"]\\s*,\\s*"
                    + "owner\\.dependencies\\.testFixtures\\(project\\(" + PROJECT + "\\)\\)\\s*\\)");

    private BuildModuleDeclarations() { }

    static Set<String> mavenModules(Path root) throws Exception {
        return new LinkedHashSet<>(children(child(pom(root.resolve("pom.xml")), "modules"), "module")
                .stream().map(node -> node.getTextContent().strip()).toList());
    }

    static Set<Edge> mavenEdges(Path root, Set<String> modules) throws Exception {
        var result = new LinkedHashSet<Edge>();
        for (String module : modules) {
            var project = pom(root.resolve(module).resolve("pom.xml"));
            for (var dependency : children(child(project, "dependencies"), "dependency")) {
                if (!text(dependency, "groupId", "").equals("atf.api")) continue;
                String artifact = text(dependency, "artifactId", "");
                result.add(new Edge("Maven", module, text(dependency, "scope", "compile"), artifact));
            }
        }
        return result;
    }

    static Set<String> gradleModules(Path settings) throws Exception {
        var modules = new LinkedHashSet<String>();
        for (String line : Files.readAllLines(settings)) {
            if (line.stripLeading().startsWith("//")) continue;
            var include = INCLUDE.matcher(line);
            if (!include.find()) continue;
            for (String argument : include.group(1).split(",")) {
                var project = PROJECT.matcher(argument.strip());
                if (!project.matches() || !modules.add(project.group(1))) {
                    throw new IllegalArgumentException("DOC-BUILD-MAP: unsupported or duplicate include in " + settings + ": " + line);
                }
            }
        }
        if (modules.isEmpty()) throw new IllegalArgumentException("DOC-BUILD-MAP: missing includes in " + settings);
        return modules;
    }

    static Set<Edge> gradleEdges(Path gradle, Set<String> modules) throws Exception {
        var result = new LinkedHashSet<Edge>();
        for (String module : modules) {
            Path build = gradle.resolve(module).resolve("build.gradle");
            var dependencies = DEPENDENCY.matcher(Files.readString(build));
            while (dependencies.find()) {
                result.add(new Edge("Gradle", module, dependencies.group(1), dependencies.group(2)));
            }
        }
        var fixtures = FIXTURES.matcher(Files.readString(gradle.resolve("gradle/component-audit.gradle")));
        if (!fixtures.find()) throw new IllegalArgumentException("DOC-BUILD-MAP: missing Gradle audit fixture declaration");
        result.add(new Edge("Gradle", "fixture-uri comune", fixtures.group(1), fixtures.group(2)));
        return result;
    }

    private static Element pom(Path path) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(path.toFile()).getDocumentElement();
    }

    private static List<Element> children(Element parent, String tag) {
        var matches = new ArrayList<Element>();
        if (parent != null) for (var node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && tag.equals(element.getTagName())) matches.add(element);
        }
        return matches;
    }

    private static Element child(Element parent, String tag) {
        var children = children(parent, tag);
        return children.isEmpty() ? null : children.getFirst();
    }

    private static String text(Element parent, String tag, String fallback) {
        var node = child(parent, tag);
        return node == null ? fallback : node.getTextContent().strip();
    }
}
