package atf.tests.jvm;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

/** Assigns one physical Java source to one declared module and source set. */
public final class JvmSourceInventory {
    public static final String POLICY_PATH = "config/atf-governance-policy.json";

    private JvmSourceInventory() { }

    public static JsonNode readPolicy(Path root) throws IOException {
        Path path = root.resolve(POLICY_PATH);
        if (!Files.isRegularFile(path) || !path.toRealPath().equals(path))
            throw new IOException("GOV-SOURCE-CONFIG: missing or unsafe " + POLICY_PATH);
        var policy = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION).readTree(path.toFile());
        if (policy == null || !policy.isObject())
            throw new IOException("GOV-SOURCE-CONFIG: expected a policy object");
        return policy;
    }

    public static Inventory scan(Path project, JsonNode policy) throws IOException {
        Path root = project.toRealPath();
        var poms = AtfJavaSourceInventory.poms(root);
        var modules = new TreeSet<String>();
        for (Path pom : poms) if (!pom.getParent().equals(root)) modules.add(relative(root, pom.getParent()));
        if (policy.has("mavenModules")) validateMavenModules(root, policy, poms, modules);

        var roots = new ArrayList<AtfJavaSourceInventory.SourceRoot>(AtfJavaSourceInventory.roots(root, poms));
        JsonNode additional = policy.path("additionalSourceRoots");
        if (!additional.isMissingNode() && !additional.isArray())
            throw new IOException("GOV-SOURCE-CONFIG: additionalSourceRoots must be an array");
        for (JsonNode entry : additional) {
            if (!entry.path("module").isTextual() || !entry.path("path").isTextual() || !entry.path("test").isBoolean())
                throw new IOException("GOV-SOURCE-CONFIG: explicit module, path and boolean test required: " + entry);
            Path module = safeDirectory(root, entry.path("module").textValue());
            Path source = safeDirectory(root, entry.path("path").textValue());
            if (!source.startsWith(module) || source.equals(module))
                throw new IOException("GOV-SOURCE-OWNER: " + relative(root, source) + " is outside " + relative(root, module));
            roots.add(new AtfJavaSourceInventory.SourceRoot(module, source, entry.path("test").booleanValue()));
        }

        roots.sort(java.util.Comparator.comparing(AtfJavaSourceInventory.SourceRoot::directory));
        var files = new ArrayList<SourceFile>();
        var sets = new ArrayList<SourceSet>();
        var ownedRoots = new HashSet<Path>();
        for (var source : roots) {
            Path directory = source.directory();
            for (Path parent = directory; !parent.equals(root); parent = parent.getParent()) {
                if (ownedRoots.contains(parent))
                    throw new IOException("GOV-SOURCE-OVERLAP: " + relative(root, parent) + " and " + relative(root, directory));
            }
            ownedRoots.add(directory);
            String module = moduleName(root, source.module());
            modules.add(module);
            var members = AtfJavaSourceInventory.files(directory);
            sets.add(new SourceSet(module, source.test() ? "test" : "main", relative(root, directory), members.size()));
            for (Path file : members) files.add(new SourceFile(file, directory, module, source.test()));
        }
        files.sort(java.util.Comparator.comparing(SourceFile::file));
        return new Inventory(List.copyOf(modules), List.copyOf(sets), List.copyOf(files));
    }

    private static void validateMavenModules(Path root, JsonNode policy, List<Path> poms, TreeSet<String> discovered)
            throws IOException {
        JsonNode declared = policy.path("mavenModules");
        if (!declared.isArray()) throw new IOException("GOV-SOURCE-CONFIG: mavenModules must be an array");
        var expected = new TreeSet<String>();
        for (JsonNode entry : declared) {
            if (!entry.isTextual() || entry.textValue().isBlank() || !expected.add(entry.textValue()))
                throw new IOException("GOV-SOURCE-CONFIG: invalid or repeated Maven module: " + entry);
        }
        var reactor = new TreeSet<String>();
        var pomsInCheckout = new HashSet<>(poms);
        collectMavenModules(root, root, pomsInCheckout, reactor);
        if (!expected.equals(discovered) || !expected.equals(reactor))
            throw new IOException("GOV-SOURCE-MODULES: policy=" + expected + ", reactor=" + reactor + ", POMs=" + discovered);
    }

    private static void collectMavenModules(Path root, Path parent, HashSet<Path> poms, TreeSet<String> reactor)
            throws IOException {
        Path pom = parent.resolve("pom.xml");
        if (!poms.contains(pom)) throw new IOException("GOV-SOURCE-MODULES: missing " + relative(root, pom));
        for (String entry : AtfJavaSourceInventory.declaredModules(pom)) {
            if (entry.isBlank() || Path.of(entry).isAbsolute() || entry.contains("${"))
                throw new IOException("GOV-SOURCE-MODULES: invalid module " + entry);
            Path module = safeDirectory(root, relative(root, parent.resolve(entry).normalize()));
            if (!module.startsWith(parent) || module.equals(parent) || !reactor.add(relative(root, module)))
                throw new IOException("GOV-SOURCE-MODULES: invalid or repeated module " + entry);
            collectMavenModules(root, module, poms, reactor);
        }
    }

    public static Path safeDirectory(Path root, String name) throws IOException {
        Path path = root.resolve(name).normalize();
        if (name.isBlank() || Path.of(name).isAbsolute() || !path.startsWith(root) || path.equals(root)
                || !relative(root, path).equals(name) || !Files.isDirectory(path) || !path.toRealPath().equals(path))
            throw new IOException("GOV-SOURCE-ROOT: missing, unsafe or noncanonical directory " + name);
        return path;
    }

    public static String relative(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }

    private static String moduleName(Path root, Path module) {
        return root.equals(module) ? "." : relative(root, module);
    }

    public record Inventory(List<String> modules, List<SourceSet> sourceSets, List<SourceFile> files) { }
    public record SourceSet(String module, String set, String root, int files) { }
    public record SourceFile(Path file, Path sourceRoot, String module, boolean test) {
        public String scope() { return module + (test ? ":test" : ":main"); }
    }
}
