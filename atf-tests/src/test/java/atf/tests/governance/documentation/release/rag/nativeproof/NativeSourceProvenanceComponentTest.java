package atf.tests.governance.documentation.release.rag.nativeproof;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import static atf.testkit.assertions.AuditedAssertions.assertTrue;

@AtfComponentTest
class NativeSourceProvenanceComponentTest {
    @Test void legacyEvidenceAndSourceSelectionKeepTheirIndependentGuards() throws Exception {
        String output = NodeReleaseContracts.runContracts(Duration.ofSeconds(100), "prepare-android-e2e.test.mjs",
                "prepare-component-reuse.test.mjs", "release-test-scope.test.mjs");
        assertTrue(!output.isBlank(), output);
    }
    @Test void immutableRuntimeEvidenceSurvivesValidatorMaintenanceButRejectsChangedInputs() throws Exception {
        String output = NodeReleaseContracts.runContracts(Duration.ofSeconds(90), "android-e2e-provenance.test.mjs");
        assertTrue(output.contains("Native source admission:"), output);
    }
}
