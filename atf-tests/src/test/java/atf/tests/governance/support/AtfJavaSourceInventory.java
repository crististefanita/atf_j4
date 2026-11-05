package atf.tests.governance.support;

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
    public static List<SourceRoot> roots(Path project) throws Exception {
        Path root = project.toAbsolutePath().normalize();
        List<SourceRoot> result = new ArrayList<>();
        for (Path pom : pomFiles(root)) {
            Path module = pom.getParent();
            Element build = configuredBuild(pom);
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
            throw new IOException("Invalid " + name + " in " + pom + ": " + directory);
        }
        if (!Files.exists(source)) return;
        if (!Files.isDirectory(source) || !source.toRealPath().startsWith(module.toRealPath()))
            throw new IOException("Unsafe " + name + " in " + pom + ": " + directory);
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
            List<Path> result = files.filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".java")).sorted().toList();
            for (Path file : result) if (!file.toRealPath().startsWith(physicalRoot))
                throw new IOException("GOV-SOURCE-ESCAPE: " + file);
            return result;
        }
    }

    private static Element configuredBuild(Path pom) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Element project = factory.newDocumentBuilder().parse(pom.toFile()).getDocumentElement();
        return child(project, "build");
    }

    private static Element child(Element parent, String name) {
        var children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child && child.getTagName().equals(name)) return child;
        }
        return null;
    }

    private static List<Path> pomFiles(Path root) throws IOException {
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
