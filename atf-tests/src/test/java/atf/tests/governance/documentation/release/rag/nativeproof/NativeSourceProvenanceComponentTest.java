package atf.tests.governance.documentation.release.rag.nativeproof;

import atf.testkit.AtfComponentTest;
import atf.tests.reporting.GameReportAudit;
import atf.android.evidence.component.ComponentInputs;
import atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import static atf.testkit.assertions.AuditedAssertions.assertTrue;

@AtfComponentTest
class NativeSourceProvenanceComponentTest {
    @org.junit.jupiter.api.extension.RegisterExtension
    static final GameReportAudit AUDIT = new GameReportAudit();
    @Test void legacyEvidenceAndSourceSelectionKeepTheirIndependentGuards(ComponentInputs inputs) throws Exception {
        String output = NodeReleaseContracts.runContracts(Duration.ofSeconds(100), value -> inputs.add("Original child process output", value), "prepare-android-e2e.test.mjs",
                "prepare-component-reuse.test.mjs", "release-test-scope.test.mjs");
        assertTrue(!output.isBlank(), output);
    }
    @Test void immutableRuntimeEvidenceSurvivesValidatorMaintenanceButRejectsChangedInputs(ComponentInputs inputs) throws Exception {
        String output = NodeReleaseContracts.runContracts(Duration.ofSeconds(90), value -> inputs.add("Original native-source observations", value), "android-e2e-provenance.test.mjs");
        assertTrue(output.contains("Native source admission:"), output);
    }
}
