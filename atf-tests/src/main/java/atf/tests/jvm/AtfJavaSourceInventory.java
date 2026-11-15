package atf.tests.jvm;

import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Shared literal Maven source-root inventory for architecture and readability governance. */
public final class AtfJavaSourceInventory {
    private static final Set<String> EXCLUDED = Set.of(".git", ".tmp", "artifacts", "target", "build", ".gradle", "node_modules", "dist");

    private AtfJavaSourceInventory() { }

    public static List<Path> mainSources(Path project) throws Exception {
        return sources(project, false);
    }

    public static List<Path> testSources(Path project) throws Exception {
        return sources(project, true);
    }

    public static List<Path> allSources(Path project) throws Exception {
        Set<Path> result = new LinkedHashSet<>();
        for (SourceRoot source : roots(project)) result.addAll(files(source.directory()));
        return result.stream().sorted().toList();
    }

    public record SourceRoot(Path module, Path directory, boolean test) { }

    /** Retains the declared root and owning POM instead of inferring them from each file path. */
    public static List<SourceRoot> roots(Path project) throws IOException {
        Path root = project.toRealPath();
        return roots(root, poms(root));
    }

    static List<SourceRoot> roots(Path root, List<Path> poms) throws IOException {
        List<SourceRoot> result = new ArrayList<>();
        for (Path pom : poms) {
            if (!pom.toRealPath().equals(pom)) throw new IOException("GOV-SOURCE-ESCAPE: " + root.relativize(pom));
            Path module = pom.getParent();
            Element build = child(readPom(pom), "build");
            addRoot(result, pom, module, build, false, "sourceDirectory", "src/main/java");
            addRoot(result, pom, module, build, true, "testSourceDirectory", "src/test/java");
        }
        return List.copyOf(result);
    }

    private static void addRoot(List<SourceRoot> roots, Path pom, Path module, Element build,
                                boolean test, String name, String fallback) throws IOException {
        Element configured = build == null ? null : child(build, name);
        String directory = configured == null ? fallback : configured.getTextContent().strip();
        Path source = module.resolve(directory).normalize();
        if (directory.isBlank() || Path.of(directory).isAbsolute() || !source.startsWith(module)
                || source.equals(module) || directory.contains("${")) {
            throw new IOException("GOV-SOURCE-ROOT: invalid " + name + " in " + pom + ": " + directory);
        }
        if (configured == null && !Files.exists(source)) return;
        if (!Files.isDirectory(source) || !source.toRealPath().equals(source))
            throw new IOException("GOV-SOURCE-ROOT: missing or unsafe " + name + " in " + pom + ": " + directory);
        roots.add(new SourceRoot(module, source, test));
    }

    private static List<Path> sources(Path project, boolean test) throws Exception {
        Set<Path> result = new LinkedHashSet<>();
        for (SourceRoot root : roots(project)) if (root.test() == test) result.addAll(files(root.directory()));
        return result.stream().distinct().sorted().toList();
    }

    public static List<Path> files(Path source) throws IOException {
        Path physicalRoot = source.toRealPath();
        try (var files = Files.walk(source)) {
            List<Path> paths = files.sorted().toList();
            for (Path path : paths) if (Files.isSymbolicLink(path))
                throw new IOException("GOV-SOURCE-ESCAPE: symbolic link " + path);
            List<Path> result = paths.stream().filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".java")).sorted().toList();
            for (Path file : result) if (!file.toRealPath().equals(physicalRoot.resolve(source.relativize(file))))
                throw new IOException("GOV-SOURCE-ESCAPE: " + file);
            return result;
        }
    }

    private static Element readPom(Path pom) throws IOException {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Element document = factory.newDocumentBuilder().parse(pom.toFile()).getDocumentElement();
            if (!document.getTagName().equals("project")) throw new IOException("expected a Maven project element");
            return document;
        } catch (javax.xml.parsers.ParserConfigurationException | org.xml.sax.SAXException | IOException invalid) {
            throw new IOException("GOV-SOURCE-POM: " + pom + ": " + invalid.getMessage(), invalid);
        }
    }

    static List<String> declaredModules(Path pom) throws IOException {
        var modules = child(readPom(pom), "modules");
        if (modules == null) return List.of();
        var declarations = modules.getChildNodes();
        var result = new ArrayList<String>();
        for (int i = 0; i < declarations.getLength(); i++) {
            if (declarations.item(i) instanceof Element entry && entry.getTagName().equals("module"))
                result.add(entry.getTextContent().strip());
        }
        return List.copyOf(result);
    }

    private static Element child(Element parent, String name) {
        var children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child && child.getTagName().equals(name)) return child;
        }
        return null;
    }

    static List<Path> poms(Path root) throws IOException {
        var result = new ArrayList<Path>();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                return !directory.equals(root) && EXCLUDED.contains(directory.getFileName().toString())
                        ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                if (attributes.isRegularFile() && file.getFileName().toString().equals("pom.xml")) result.add(file);
                return FileVisitResult.CONTINUE;
            }
        });
        return result.stream().sorted().toList();
    }
}
