package atf.tests.publishing;

import atf.testkit.AtfIntegrationTest;
import atf.tests.reporting.acceptance.CombinedAcceptanceReport;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

/** Stable opt-in JUnit publisher and compatibility entry points. */
@AtfIntegrationTest
public class DeepSuiteExtentReportTest {
    private static final List<String> MODULES =
            List.of("atf-validation", "atf-tests", "atf-test-android");

    @Test
    void publishesCombinedComponentAndLiveGameplayReport() throws Exception {
        CombinedAcceptanceReport.publish(MODULES);
    }

    public static void publishUiFlow(
            atf.reporting.contract.AtfReportPorts.Session session, Path manifest, Path evidenceDir)
            throws Exception {
        atf.tests.reporting.replay.UiFlowReplay.publishUiFlow(session, manifest, evidenceDir);
    }

    public static void publishUiFlow(
            atf.reporting.contract.AtfReportPorts.Session session,
            Path manifest,
            Path evidenceDir,
            boolean completeActionBodies)
            throws Exception {
        atf.tests.reporting.replay.UiFlowReplay.publishUiFlow(
                session, manifest, evidenceDir, completeActionBodies);
    }
}
