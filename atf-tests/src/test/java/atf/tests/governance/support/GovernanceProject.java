package atf.tests.governance.support;

import atf.tests.documentation.DocumentationInventory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Shared explicit perimeter. Discovery never treats a missing project as a successful empty scan. */
public final class GovernanceProject {
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
        return DocumentationInventory.documents(root);
    }

    public static boolean isLibraryGuide(Path root, Path file) {
        return DocumentationInventory.isLibraryGuide(root, file);
    }

    public static List<SourceFile> sources(Path root) throws Exception {
        root = root.toAbsolutePath().normalize();
        var result = new LinkedHashMap<Path, SourceFile>();
        for (var source : AtfJavaSourceInventory.roots(root)) {
            String module = relative(root, source.module());
            addSources(result, source.directory(), module, source.test());
        }
        JsonNode additional = policy(root).path("additionalSourceRoots");
        if (!additional.isMissingNode() && !additional.isArray()) throw new IOException("GOV-SOURCE-CONFIG: additionalSourceRoots must be an array");
        for (JsonNode entry : additional) {
            if (!entry.path("module").isTextual() || entry.path("module").asText().isBlank()
                    || !entry.path("path").isTextual() || !entry.path("test").isBoolean())
                throw new IOException("GOV-SOURCE-CONFIG: explicit module, path and boolean test required: " + entry);
            Path module = safeDirectory(root, entry.path("module").asText());
            Path source = safeDirectory(root, entry.required("path").asText());
            if (!source.startsWith(module) || source.equals(module))
                throw new IOException("GOV-SOURCE-OWNER: root must be inside its module: " + entry);
            addSources(result, source, relative(root, module), entry.path("test").booleanValue());
        }
        return result.values().stream().sorted(java.util.Comparator.comparing(row -> row.file().toString())).toList();
    }

    private static void addSources(Map<Path, SourceFile> result, Path source, String module, boolean test) throws IOException {
        for (Path file : AtfJavaSourceInventory.files(source)) {
            var entry = new SourceFile(file, source, module, test);
            SourceFile previous = result.putIfAbsent(file, entry);
            if (previous != null) throw new IOException("GOV-SOURCE-OVERLAP: " + file + " claimed by " + previous + " and " + entry);
        }
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
