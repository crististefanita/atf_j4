package atf.tests.governance.support;

import atf.tests.documentation.DocumentationInventory;
import com.fasterxml.jackson.databind.JsonNode;
import atf.tests.jvm.JvmSourceInventory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
        return JvmSourceInventory.readPolicy(root.toRealPath());
    }

    public static List<Path> markdown(Path root) throws IOException {
        return DocumentationInventory.documents(root);
    }

    public static boolean isLibraryGuide(Path root, Path file) {
        return DocumentationInventory.isLibraryGuide(root, file);
    }

    public static List<JvmSourceInventory.SourceFile> sources(Path root) throws IOException {
        return JvmSourceInventory.scan(root, policy(root)).files();
    }

    public static String relative(Path root, Path file) {
        return JvmSourceInventory.relative(root, file);
    }
}
