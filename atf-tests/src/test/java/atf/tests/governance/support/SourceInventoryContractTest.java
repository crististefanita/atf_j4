package atf.tests.governance.support;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class SourceInventoryContractTest {
    @Test void mavenDiscoveryHonoursCustomRootsAndKeepsOutputNamedSourcePackages(@TempDir Path root) throws Exception {
        write(root, "module/pom.xml", "<project><build><sourceDirectory>src/main</sourceDirectory></build></project>");
        Path main = write(root, "module/src/main/legal/target/Visible.java", "package legal.target; class Visible {}");
        Path test = write(root, "module/src/test/java/legal/Test.java", "package legal; class Test {}");
        write(root, "module/target/generated/Hidden.java", "not compiled source");
        write(root, "artifacts/stale/pom.xml", "not XML");
        assertEquals(Set.of(main, test), Set.copyOf(AtfJavaSourceInventory.allSources(root)));
    }

    @Test void emptyEscapingAndUnresolvedConfiguredRootsCannotHideSources(@TempDir Path root) throws Exception {
        Path pom = write(root, "module/pom.xml", "<project><build><sourceDirectory>../outside</sourceDirectory></build></project>");
        assertThrows(IOException.class, () -> AtfJavaSourceInventory.allSources(root));
        Files.writeString(pom, "<project><build><sourceDirectory> </sourceDirectory></build></project>");
        assertThrows(IOException.class, () -> AtfJavaSourceInventory.allSources(root));
        Files.writeString(pom, "<project><build><sourceDirectory>${external}</sourceDirectory></build></project>");
        assertThrows(IOException.class, () -> AtfJavaSourceInventory.allSources(root));
        assertThrows(IOException.class, () -> GovernanceProject.safeDirectory(root, "../outside"));
    }

    private static Path write(Path root, String relative, String text) throws IOException {
        Path file = root.resolve(relative); Files.createDirectories(file.getParent()); Files.writeString(file, text); return file;
    }
}
