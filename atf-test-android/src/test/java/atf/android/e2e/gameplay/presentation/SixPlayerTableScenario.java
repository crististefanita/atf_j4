package atf.android.e2e.gameplay.presentation;

import static atf.android.e2e.support.flow.WhistLabScenario.PACKAGE;
import static atf.android.e2e.support.flow.WhistLabScenario.artifacts;
import static atf.android.e2e.support.flow.WhistLabScenario.installedApp;
import static atf.android.e2e.support.flow.WhistLabScenario.reportSetting;
import atf.android.AdbClient;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.support.AndroidUiFlowReportPublisher;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfReportStatus;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Scenariul de prezentare și navigare al mesei cu șase jucători. */
public final class SixPlayerTableScenario {
    private SixPlayerTableScenario() {}

    public static void run(String ownerName) throws Exception {
        var installation = installedApp();
        AdbClient adb = installation.adb();

        Path artifacts = artifacts("table-presentation");
        AndroidUiDriver ui = new AndroidUiDriver(adb, artifacts);
        AndroidUiFlowTrace trace =
                new AndroidUiFlowTrace(
                        artifacts,
                        "whist-lab-table-presentation",
                        "Whist Lab — eight-card table, compact bidding, and hamburger navigation",
                        ownerName,
                        "atf.android.e2e",
                        PACKAGE,
                        List.of("android", "e2e", "ui", "table-layout", "navigation", "sanity"));
        ui.recordCapturesAs(trace::capture);
        trace.beginPhase("SETUP");
        ui.recordActionsAs(trace::recordAction);
        List<Path> evidence = new ArrayList<>();
        AtfReportStatus outcome = AtfReportStatus.FAIL;
        String checks = "Presentation flow did not complete.";
        try {
            String setup = SixPlayerSetupPhase.run(ui, trace, evidence);
            trace.beginPhase("TEST BODY");

            var biddingState = SixPlayerBiddingCheck.run(adb, ui, trace, evidence, setup);
            SixPlayerNavigationCheck.run(ui, trace, evidence, biddingState);
            checks =
                    "Six-player Oh Hell opened an eight-card human hand. All eight cards rendered on one row; every bid permitted by the current final-bid rule remained visible and each hit target measured at least 48 dp. The hamburger exposed Score sheet and Main menu. A selected but unconfirmed bid remained unsubmitted in the score view. The flow returned home without submitting a bid or playing a card.";
            outcome = AtfReportStatus.PASS;
            trace.finish(outcome);
        } catch (Throwable failure) {
            outcome = AtfReportStatus.FAIL;
            trace.fail(failure);
            if (failure instanceof Exception exception) throw exception;
            if (failure instanceof Error error) throw error;
            throw new RuntimeException(failure);
        } finally {
            trace.beginPhase("TEARDOWN");
            try {
                adb.run("shell", "am", "force-stop", PACKAGE).requireSuccess();
            } catch (Exception teardownFailure) {
                boolean alreadyFailed = outcome == AtfReportStatus.FAIL;
                trace.fail(teardownFailure);
                if (!alreadyFailed) {
                    outcome = AtfReportStatus.FAIL;
                    throw teardownFailure;
                }
            }
            trace.finish(outcome);
            List<Path> reportEvidence = new ArrayList<>(evidence);
            reportEvidence.add(trace.manifest());
            AndroidUiFlowReportPublisher.publish(
                    artifacts.resolve("whist-lab-table-presentation.html"),
                    installation.packagePath(),
                    reportSetting("atf.android.serial", "ATF_ANDROID_SERIAL", "emulator-5554"),
                    System.getProperty("atf.android.api", "device reported at runtime"),
                    checks,
                    reportEvidence,
                    trace);
        }
    }
}
