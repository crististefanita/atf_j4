package atf.tests.governance.documentation.release.rag.nativeproof;

import atf.testkit.AtfComponentTest;
import atf.android.component.AndroidComponentAudit;
import atf.android.evidence.component.ComponentInputs;
import atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts;
import atf.tests.governance.support.GovernanceProject;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

import static atf.testkit.assertions.AuditedAssertions.assertTrue;

@AtfComponentTest
class WhistSurfaceAdapterComponentTest {
    @org.junit.jupiter.api.extension.RegisterExtension
    static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
    @Test void webAndNativePortablePortsKeepMeasuredGeometryFacesAndGesturesAligned(ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        String output = NodeReleaseContracts.runNode(List.of("node", "--test",
                root.resolve("atf-ui-browser/tests/whist-surface-contract.test.mjs").toString()), root, Duration.ofSeconds(160),
                value -> inputs.add("Original Web/native adapter observations", value));
        assertTrue(output.contains("Whist adapter observations:"), output);
    }
}
