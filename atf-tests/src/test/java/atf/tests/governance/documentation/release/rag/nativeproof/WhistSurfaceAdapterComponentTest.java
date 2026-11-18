package atf.tests.governance.documentation.release.rag.nativeproof;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts;
import atf.tests.governance.support.GovernanceProject;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

import static atf.testkit.assertions.AuditedAssertions.assertTrue;

@AtfComponentTest
class WhistSurfaceAdapterComponentTest {
    @Test void webAndNativePortablePortsKeepMeasuredGeometryFacesAndGesturesAligned() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        String output = NodeReleaseContracts.runNode(List.of("node", "--test",
                root.resolve("atf-ui-browser/tests/whist-surface-contract.test.mjs").toString()), root, Duration.ofSeconds(160));
        assertTrue(output.contains("Whist adapter observations:"), output);
    }
}
