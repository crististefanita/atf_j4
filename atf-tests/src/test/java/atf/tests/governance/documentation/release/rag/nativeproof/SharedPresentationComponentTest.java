package atf.tests.governance.documentation.release.rag.nativeproof;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts;
import atf.tests.governance.support.GovernanceProject;
import atf.tests.reporting.GameReportAudit;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static atf.testkit.assertions.AuditedAssertions.assertTrue;

@AtfComponentTest
class SharedPresentationComponentTest {
    @RegisterExtension static final GameReportAudit AUDIT = new GameReportAudit();

    @Test void scoreTextAdapterKeepsLocalizedActionsAtLargerWebSizes(ComponentInputs inputs) throws Exception {
        String output = run("real Web score", inputs);
        assertTrue(output.contains("Score text adapter observations:"), "The actual Chromium renderer must publish its measurements");
    }

    @Test void trickPlaybackRejectsPrematureClearingAndKeepsCollectionOrigins(ComponentInputs inputs) throws Exception {
        String output = run("delivered shared shell", inputs);
        assertTrue(output.contains("Shared trick observations:"), "Actual shell, clock, frame and collection comparisons must be captured");
    }

    private static String run(String selection, ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        return NodeReleaseContracts.runNode(List.of("node", "--test", "--test-name-pattern=" + selection,
                root.resolve("atf-ui-browser/tests/shared-presentation.test.mjs").toString()), root, Duration.ofSeconds(90),
                output -> inputs.add("Original shared presentation comparisons", output));
    }
}
