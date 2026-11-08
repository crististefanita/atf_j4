package atf.tests.governance.source.workspace;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;

/** Root layout only: source packages named target remain valid inside their owning modules. */
final class WorkspaceHygieneRules {
    record Finding(Path path, String reason) { }
    static Set<String> roots(JsonNode policy) {
        var names = new HashSet<>(Set.of(".git", ".mvn", ".locks", ".idea", "target", "docs", "config", "lib", "scripts", "release"));
        policy.path("mavenModules").forEach(module -> names.add(first(module.asText())));
        policy.path("additionalSourceRoots").forEach(source -> names.add(first(source.path("module").asText())));
        policy.path("projectRootOwners").forEach(owner -> names.add(rootOwner(owner.asText())));
        return Set.copyOf(names);
    }
    private static String rootOwner(String owner) {
        if (owner.isBlank() || owner.contains("/") || owner.contains("\\") || owner.equals(".") || owner.equals(".."))
            throw new IllegalArgumentException("Expected a direct project-root owner: " + owner);
        return owner;
    }
    private static String first(String module) {
        if (module.isBlank() || module.startsWith("/") || module.contains("\\") || module.contains(":"))
            throw new IllegalArgumentException("Expected a relative module owner: " + module);
        for (String segment : module.split("/", -1)) if (Set.of("", ".", "..").contains(segment))
            throw new IllegalArgumentException("Expected a canonical module owner: " + module);
        return module.split("/")[0];
    }
    static List<Finding> inspect(Path root, Set<String> registered) throws IOException {
        var findings = new ArrayList<Finding>();
        try (var entries = Files.list(root)) {
            for (Path entry : entries.sorted().toList()) {
                String name = entry.getFileName().toString(), lower = name.toLowerCase(Locale.ROOT);
                boolean localTemporaryPath = lower.matches("(?:[a-z])?users.*appdata.*temp.*");
                if (localTemporaryPath) findings.add(new Finding(entry, "flattened local temporary path"));
                else if (Files.isDirectory(entry) && !registered.contains(name))
                    findings.add(new Finding(entry, "unregistered root directory; generated work belongs in owned TMP"));
                else if (Files.isRegularFile(entry) && (lower.matches(".*\\.(?:class|jar|apk|hprof|tmp)")
                        || lower.matches("hs_err_pid.*\\.log") || lower.matches("javac\\..*\\.args")))
                    findings.add(new Finding(entry, "generated file outside its owner"));
            }
        }
        return List.copyOf(findings);
    }
    private WorkspaceHygieneRules() { }
}
