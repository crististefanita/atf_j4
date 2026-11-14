package atf.android.e2e.support.flow;

import atf.testkit.AtfComponentTest;
import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Verifică faptul că ieșirile E2E nu se pierd în checkout ori într-o locație implicită. */
@AtfComponentTest
final class WhistLabScenarioArtifactsComponentTest {
    private static final String PROPERTY = "atf.android.artifacts";
    private final Map<String, String> originalRoots = new HashMap<>();

    WhistLabScenarioArtifactsComponentTest() {
        for (String property : new String[] {PROPERTY, "atf.match.evidenceDir", "whist.records.runRoot"}) {
            originalRoots.put(property, System.getProperty(property));
        }
    }

    @AfterEach
    void restoreArtifactRoot() {
        originalRoots.forEach((property, value) -> {
            if (value == null) System.clearProperty(property);
            else System.setProperty(property, value);
        });
    }

    @Test
    void requiresAnExplicitArtifactRootInsteadOfFallingBackToWindowsTemp() {
        System.clearProperty(PROPERTY);
        var failure = assertThrows(IllegalStateException.class, () -> WhistLabScenario.artifacts("flow"));
        assertTrue(failure.getMessage().contains("atf.android.artifacts"));
    }

    @Test
    void refusesToCreateArtifactsInsideTheAtfCheckout() {
        System.setProperty(PROPERTY, WhistLabScenario.checkoutRoot().toString());
        assertThrows(IllegalStateException.class, () -> WhistLabScenario.artifacts("flow"));
    }

    @Test
    void rejectsInCheckoutRootsForGameRecordsAndInstallRuns() {
        String checkout = WhistLabScenario.checkoutRoot().toString();
        for (String property : new String[] {PROPERTY, "atf.match.evidenceDir", "whist.records.runRoot"}) {
            System.setProperty(property, checkout);
            assertThrows(IllegalStateException.class, () -> WhistLabScenario.configuredExternalPath(property), property);
        }
        Path external = WhistLabScenario.checkoutRoot().getParent().resolve("whist-back-e2e-output-fixture").resolve("run");
        System.setProperty("atf.match.evidenceDir", external.toString());
        assertEquals(external.toAbsolutePath().normalize(), WhistLabScenario.configuredExternalPath("atf.match.evidenceDir"));
    }

    @Test
    void resolvesFlowArtifactsUnderTheExplicitExternalRoot() {
        Path external = WhistLabScenario.checkoutRoot().getParent().resolve("whist-back-e2e-output-fixture");
        System.setProperty(PROPERTY, external.toString());
        assertEquals(external.toAbsolutePath().normalize().resolve("flow"), WhistLabScenario.artifacts("flow"));
    }

    @Test
    void rejectsFlowNamesThatCouldEscapeTheExternalRoot() {
        Path external = WhistLabScenario.checkoutRoot().getParent().resolve("whist-back-e2e-output-fixture");
        System.setProperty(PROPERTY, external.toString());
        assertThrows(IllegalArgumentException.class, () -> WhistLabScenario.artifacts(".."));
        assertThrows(IllegalArgumentException.class, () -> WhistLabScenario.artifacts("../outside"));
    }
}
