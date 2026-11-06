package atf.libraries;

import atf.testkit.AtfUnitTest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
@ResourceLock("atf.governance.root")
class PlatformLibraryWorkspaceTest {
    @TempDir Path temporary;

    @Test void anExplicitProjectWinsOverTheLaunchersCurrentDirectory() throws Exception {
        Path root = Files.createDirectory(temporary.resolve("declared"));
        Files.writeString(root.resolve("pom.xml"), "<project/>");
        Files.createDirectory(root.resolve("atf-tests"));
        withRoot(root.resolve("child/..").toString(), () -> assertEquals(root, PlatformLibrarySurface.workspace()));
    }
    @Test void aMissingExplicitRootCannotSilentlyFallBackToTheCurrentProject() {
        withRoot(temporary.resolve("missing").toString(), () -> assertTrue(
                assertThrows(IllegalStateException.class, PlatformLibrarySurface::workspace).getMessage().contains("explicit root=")));
    }
    @Test void aPomWithoutTheConsumerModuleIsNotAnExplicitWorkspace() throws Exception {
        Files.writeString(temporary.resolve("pom.xml"), "<project/>");
        withRoot(temporary.toString(), () -> assertThrows(IllegalStateException.class, PlatformLibrarySurface::workspace));
    }
    private static void withRoot(String value, Runnable verify) {
        String previous = System.getProperty("atf.governance.root");
        try { System.setProperty("atf.governance.root", value);verify.run(); }
        finally { if (previous == null) System.clearProperty("atf.governance.root");else System.setProperty("atf.governance.root", previous); }
    }
}
