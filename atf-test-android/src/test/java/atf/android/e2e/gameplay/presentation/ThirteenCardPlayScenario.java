package atf.android.e2e.gameplay.presentation;

import atf.android.e2e.gameplay.presentation.fan.ThirteenCardFanFlow;
import static atf.android.e2e.support.flow.WhistLabScenario.PACKAGE;
import static atf.android.e2e.support.flow.WhistLabScenario.artifacts;
import static atf.android.e2e.support.flow.WhistLabScenario.installedApp;
import static atf.android.e2e.support.flow.WhistLabScenario.reportSetting;
import atf.android.AdbClient;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.support.AndroidUiFlowReportPublisher;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Scenariul selectării și deplasării unei cărți din mâna lungă. */
public final class ThirteenCardPlayScenario {
    private ThirteenCardPlayScenario() {}

    public static void run(String ownerName) throws Exception {
        var installation = installedApp();
        AdbClient adb = installation.adb();

        Path artifacts = artifacts("thirteen-card-fan");
        AndroidUiDriver ui = new AndroidUiDriver(adb, artifacts);
        AndroidUiFlowTrace trace =
                new AndroidUiFlowTrace(
                        artifacts,
                        "whist-lab-thirteen-card-fan",
                        "Whist · Descending — thirteen-card fan and one-card play",
                        ownerName,
                        "atf.android.e2e",
                        PACKAGE,
                        List.of("android", "e2e", "ui", "card-fan", "single-row", "click-flow"));
        ui.recordCapturesAs(trace::capture);
        trace.beginPhase("SETUP");
        ui.recordActionsAs(trace::recordAction);
        List<Path> evidence = new ArrayList<>();
        List<AtfAssertion> assertions = new ArrayList<>();
        AtfReportStatus outcome = AtfReportStatus.FAIL;
        String checks = "The thirteen-card click flow did not complete.";
        try {
            ThirteenCardFanFlow.run(adb, ui, trace, evidence, assertions);
            checks =
                    "A deterministic 13-card Descending hand rendered as one accessible row. Tapping one legal card "
                            + "ran the lift animation, moved that exact card into the central trick, reduced the hand to 12, "
                            + "kept the human seat at the same bounds, retained the trump tile, and left the remaining cards "
                            + "fully opaque during the AI turn.";
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
                    artifacts.resolve("whist-lab-thirteen-card-fan.html"),
                    installation.packagePath(),
                    reportSetting("atf.android.serial", "ATF_ANDROID_SERIAL", "emulator-5554"),
                    System.getProperty("atf.android.api", "device reported at runtime"),
                    checks,
                    reportEvidence,
                    trace);
        }
    }
}
