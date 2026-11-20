package atf.tests.governance.uiadapter;

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
class SharedMenuHistoryComponentTest {
    @RegisterExtension static final GameReportAudit AUDIT = new GameReportAudit();
    @Test void historyTypographyAndDividersFollowTheNativePolicy(ComponentInputs inputs) throws Exception {
        assertTrue(run("menu-history.test.mjs", "history appearance matches", inputs).contains("History appearance observations:"),
                "Actual Chromium history measurements and native policy must be captured");
    }
    @Test void menuAdaptersKeepNativeLabelsOrderAndDispatch(ComponentInputs inputs) throws Exception {
        assertTrue(run("menu-history.test.mjs", "menu adapters preserve", inputs).contains("Menu adapter observations:"),
                "Both localized menu plans retain identity, ordering and isolated callbacks");
    }
    @Test void invalidStylesAreAtomicAndClearingRestoresTheBaseline(ComponentInputs inputs) throws Exception {
        assertTrue(run("menu-history.test.mjs", "invalid history appearance", inputs).contains("History rejection observations:"),
                "Rejected styles cannot leave partial state or modify unrelated controls");
    }
    @Test void newLibraryCoordinatesUseTwoPartsAndLegacyCoordinatesAreReuseOnly(ComponentInputs inputs) throws Exception {
        assertTrue(run("package-version.test.mjs", "new library versions", inputs).contains("Library version observations:"),
                "The real shared packaging guard must accept 0.x and reject newly produced legacy coordinates");
    }
    private static String run(String file, String selection, ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        return NodeReleaseContracts.runNode(List.of("node", "--test", "--test-name-pattern=" + selection,
                root.resolve("atf-ui-browser/tests/" + file).toString()), root, Duration.ofMinutes(3),
                output -> inputs.add("Original shared adapter comparisons", output));
    }
}
