package atf.android.e2e.gameplay.persistence;

import static atf.android.e2e.support.flow.UiFlowCheckpoint.checkpoint;
import static atf.android.e2e.support.flow.WhistLabScenario.PACKAGE;
import static atf.android.e2e.support.flow.WhistLabScenario.artifacts;
import static atf.android.e2e.support.flow.WhistLabScenario.installedApp;
import static atf.android.e2e.support.flow.WhistLabScenario.reportSetting;
import static org.junit.jupiter.api.Assertions.assertTrue;
import atf.android.AdbClient;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.support.AndroidUiFlowReportPublisher;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Verifică scorul și recuperarea aceleiași partide după oprirea procesului. */
public final class SavedGameRecoveryScenario {
    private SavedGameRecoveryScenario() {}

    public static void run(String ownerName) throws Exception {
        var installation = installedApp();
        AdbClient adb = installation.adb();

        Path artifacts = artifacts("practice-trick");
        AndroidUiDriver ui = new AndroidUiDriver(adb, artifacts);
        List<Path> evidence = new ArrayList<>();
        AndroidUiFlowTrace trace =
                new AndroidUiFlowTrace(
                        artifacts,
                        "whist-lab-resume-exact-bid-trick",
                        "Whist Lab — deterministic bid, process death, resume, and exact-bid score",
                        ownerName,
                        "atf.android.e2e",
                        PACKAGE,
                        List.of("android", "api-28", "parallel-app", "e2e", "resume", "scoring"));
        ui.recordCapturesAs(trace::capture);
        trace.beginPhase("SETUP");
        ui.recordActionsAs(trace::recordAction);
        AtfReportStatus outcome = AtfReportStatus.FAIL;
        String checks = "The deterministic game flow did not complete.";
        try {
            List<Path> playEvidence = SavedGamePreparationPhase.run(ui, trace, evidence);
            SavedGameResumePhase.run(adb, ui, trace, evidence, playEvidence);
            checks =
                    "API 28 fixture: human bids 1, the game process is force-stopped, and Resume game restores "
                            + "the human play turn and Bid 1; the human then plays A♥, wins the trick, and receives +11 "
                            + "for Total: 11. AI seats completed their turns and the final score sheet was visible.";
            outcome = AtfReportStatus.PASS;
        } catch (Throwable failure) {
            outcome = AtfReportStatus.FAIL;
            trace.fail(failure);
            if (failure instanceof Exception exception) throw exception;
            if (failure instanceof Error error) throw error;
            throw new RuntimeException(failure);
        } finally {
            trace.beginPhase("TEARDOWN");
            try {
                long homeStarted = System.nanoTime();
                ui.tapText("Main menu");
                String home = ui.awaitText("New game", Duration.ofSeconds(10));
                List<String> expectedHomeItems = List.of("New game", "Resume game", "How to play");
                List<String> visibleHomeItems = new ArrayList<>();
                for (String label : expectedHomeItems) {
                    if (ui.hasText(home, label)) visibleHomeItems.add(label);
                }
                boolean homeRestored = visibleHomeItems.size() == expectedHomeItems.size();
                assertTrue(
                        homeRestored,
                        "Final score navigation did not restore the complete home menu: "
                                + ui.texts(home));
                List<Path> teardown = ui.capture("whist-lab-teardown");
                evidence.addAll(teardown);
                long stopStarted = System.nanoTime();
                try {
                    adb.run("shell", "am", "force-stop", PACKAGE).requireSuccess();
                    ui.recordAction(
                            "Force-stop Android app",
                            PACKAGE,
                            "The app process stops after final UI evidence is captured",
                            "ADB accepted force-stop",
                            Duration.ofNanos(System.nanoTime() - stopStarted).toMillis(),
                            AtfReportStatus.PASS);
                } catch (Exception failure) {
                    ui.recordAction(
                            "Force-stop Android app",
                            PACKAGE,
                            "The app process stops after final UI evidence is captured",
                            failure.toString(),
                            Duration.ofNanos(System.nanoTime() - stopStarted).toMillis(),
                            AtfReportStatus.FAIL);
                    throw failure;
                }
                checkpoint(
                        trace,
                        "TEARDOWN",
                        AtfGherkinKeyword.THEN,
                        "Return from exact-bid score to the home menu and retain its final screen",
                        "Main menu",
                        teardown,
                        "tap Main menu; capture home; stop the app process",
                        "New game, Resume game, and How to play are visible",
                        "visible home items="
                                + String.join("; ", visibleHomeItems)
                                + "; retained="
                                + teardown.stream()
                                        .map(file -> file.getFileName().toString())
                                        .toList(),
                        Duration.ofNanos(System.nanoTime() - homeStarted).toMillis(),
                        List.of(
                                new AtfAssertion(
                                        "Home navigation is restored after scoring",
                                        String.join("; ", expectedHomeItems),
                                        String.join("; ", visibleHomeItems),
                                        homeRestored),
                                new AtfAssertion(
                                        "Final home UI evidence is retained",
                                        "PNG and XML",
                                        "2 files",
                                        teardown.size() == 2)),
                        AtfReportStatus.PASS);
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
                    artifacts.resolve("whist-lab-gameplay.html"),
                    installation.packagePath(),
                    reportSetting("atf.android.serial", "ATF_ANDROID_SERIAL", "emulator-5554"),
                    System.getProperty("atf.android.api", "device reported at runtime"),
                    checks,
                    reportEvidence,
                    trace);
        }
    }
}
