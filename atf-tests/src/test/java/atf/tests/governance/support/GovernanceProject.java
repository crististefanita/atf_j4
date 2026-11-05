package atf.tests.governance.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Shared explicit perimeter. Discovery never treats a missing project as a successful empty scan. */
public final class GovernanceProject {
    private static final Set<String> GENERATED = Set.of(".git", ".locks", ".gradle", ".idea",
            "target", "build", "artifacts", "dist", "node_modules", ".presentation-build");
    private GovernanceProject() { }

    public static Path locateProjectRoot() {
        String configured = System.getProperty("atf.governance.root", "");
        Path candidate = Path.of(configured.isBlank() ? System.getProperty("user.dir") : configured)
                .toAbsolutePath().normalize();
        while (candidate != null) {
            if (Files.isRegularFile(candidate.resolve("pom.xml"))
                    && Files.isRegularFile(candidate.resolve("config/atf-governance-policy.json"))) return candidate;
            if (!configured.isBlank()) break;
            candidate = candidate.getParent();
        }
        throw new IllegalStateException("GOV-ROOT: expected project POM and governance policy; explicit root=" + configured);
    }

    public static JsonNode policy(Path root) throws IOException {
        return new ObjectMapper().readTree(root.resolve("config/atf-governance-policy.json").toFile());
    }

    public static List<Path> markdown(Path root) throws IOException {
        var result = new ArrayList<Path>();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                return !directory.equals(root) && GENERATED.contains(directory.getFileName().toString())
                        ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                String relative = relative(root, file);
                if (attributes.isRegularFile() && (file.getFileName().toString().equalsIgnoreCase("README.md")
                        || isLibraryGuide(root, file)
                        || relative.startsWith("docs/") && relative.endsWith(".md"))) result.add(file);
                return FileVisitResult.CONTINUE;
            }
        });
        return result.stream().sorted().toList();
    }

    public static boolean isLibraryGuide(Path root, Path file) {
        return file.getParent().equals(root.resolve("lib")) && Set.of("README.md", "README.Core.md",
                "README.Reporting.md", "README.Console.md", "README.Mock.md").contains(file.getFileName().toString());
    }

    public static List<SourceFile> sources(Path root) throws Exception {
        var result = new ArrayList<SourceFile>();
        for (Path file : AtfJavaSourceInventory.mainSources(root)) result.add(mavenSource(root, file, false));
        for (Path file : AtfJavaSourceInventory.testSources(root)) result.add(mavenSource(root, file, true));
        for (JsonNode entry : policy(root).withArray("additionalSourceRoots")) {
            Path source = safeDirectory(root, entry.required("path").asText());
            try (var files = Files.walk(source)) {
                for (Path file : files.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java")).toList()) {
                    if (!file.toRealPath().startsWith(source.toRealPath())) throw new IOException("GOV-SOURCE-ESCAPE: " + file);
                    result.add(new SourceFile(file, source, entry.required("module").asText(), entry.path("test").asBoolean()));
                }
            }
        }
        return result.stream().distinct().sorted(java.util.Comparator.comparing(row -> row.file().toString())).toList();
    }

    private static SourceFile mavenSource(Path root, Path file, boolean test) throws IOException {
        Path relative = root.relativize(file);
        Path module = root.resolve(relative.getName(0));
        Path source = module.resolve(test ? "src/test/java" : "src/main/java");
        if (!file.startsWith(source)) throw new IOException("GOV-MAVEN-LAYOUT: declare adapter for " + file);
        return new SourceFile(file, source, relative.getName(0).toString(), test);
    }

    public static Path safeDirectory(Path root, String relative) throws IOException {
        Path resolved = root.resolve(relative).normalize();
        if (Path.of(relative).isAbsolute() || !resolved.startsWith(root) || resolved.equals(root)
                || !Files.isDirectory(resolved) || !resolved.toRealPath().startsWith(root.toRealPath()))
            throw new IOException("GOV-SOURCE-ROOT: " + relative);
        return resolved;
    }

    public static String relative(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }

    public record SourceFile(Path file, Path sourceRoot, String module, boolean test) {
        public String scope() { return module + (test ? ":test" : ":main"); }
    }
}
