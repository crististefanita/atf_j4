package atf.android.e2e.gameplay.presentation;

import static atf.android.e2e.display.support.GameplayDisplay.OVERRIDE_SIZE;
import static atf.android.e2e.display.support.GameplayDisplay.PHYSICAL_SIZE;
import static atf.android.e2e.display.support.GameplayDisplay.matchesRestoredDisplay;
import static atf.android.e2e.display.support.GameplayDisplay.overrideDensityValue;
import static atf.android.e2e.display.support.GameplayDisplay.overrideValue;
import static atf.android.e2e.display.support.GameplayDisplay.restoreDisplay;
import static atf.android.e2e.support.flow.UiFlowCheckpoint.checkpoint;
import static atf.android.e2e.support.flow.WhistLabScenario.PACKAGE;
import static atf.android.e2e.support.flow.WhistLabScenario.artifacts;
import static atf.android.e2e.support.flow.WhistLabScenario.installedApp;
import static atf.android.e2e.support.flow.WhistLabScenario.reportSetting;
import atf.android.AdbClient;
import atf.android.e2e.display.support.GameplayDisplay.DisplayProfile;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.support.AndroidUiFlowReportPublisher;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/** Scenariul licitării și geometriei unei mâini lungi pe profiluri portret. */
public final class ThirteenCardDisplayScenario {
    private ThirteenCardDisplayScenario() {}

    public static void run(String ownerName) throws Exception {
        var installation = installedApp();
        AdbClient adb = installation.adb();

        Path artifacts = artifacts("thirteen-card-bidding-resolutions");
        AndroidUiDriver ui = new AndroidUiDriver(adb, artifacts);
        AndroidUiFlowTrace trace =
                new AndroidUiFlowTrace(
                        artifacts,
                        "whist-lab-thirteen-card-bidding-resolutions",
                        "Whist · Descending — thirteen-card bid screen across portrait display sizes",
                        ownerName,
                        "atf.android.e2e",
                        PACKAGE,
                        List.of(
                                "android",
                                "e2e",
                                "ui",
                                "13-cards",
                                "bidding",
                                "resolution",
                                "responsive"));
        ui.recordCapturesAs(trace::capture);
        trace.beginPhase("TEST BODY");
        ui.recordActionsAs(trace::recordAction);
        List<Path> evidence = new ArrayList<>();
        AtfReportStatus outcome = AtfReportStatus.FAIL;
        String checks = "The thirteen-card bidding display review did not complete.";
        String originalSizeOutput = adb.run("shell", "wm", "size").output();
        String originalDensityOutput = adb.run("shell", "wm", "density").output();
        Matcher physicalSize = PHYSICAL_SIZE.matcher(originalSizeOutput);
        int physicalCaptureWidth = 0;
        int physicalCaptureHeight = 0;
        if (physicalSize.find()) {
            physicalCaptureWidth = Integer.parseInt(physicalSize.group(1));
            physicalCaptureHeight = Integer.parseInt(physicalSize.group(2));
        }
        String originalSizeOverride = overrideValue(OVERRIDE_SIZE, originalSizeOutput);
        String originalDensityOverride = overrideDensityValue(originalDensityOutput);
        try {
            List<DisplayProfile> profiles =
                    List.of(
                            new DisplayProfile("1080x1920 · 440 dpi", 1080, 1920, 440),
                            new DisplayProfile("720x1280 · 320 dpi", 720, 1280, 320),
                            new DisplayProfile(
                                    "Portrait 1080x2448 · simulated 440 dpi",
                                    1080,
                                    2448,
                                    440),
                            new DisplayProfile(
                                    "600 dp tablet · 1200x1920 · 320 dpi", 1200, 1920, 320));
            int index = 0;
            for (DisplayProfile profile : profiles) {
                ThirteenCardBiddingCheck.run(adb, ui, trace, evidence, profile, index,
                        physicalCaptureWidth, physicalCaptureHeight);
                index++;
            }
            SingleCardCenteringCheck.run(adb, ui, trace, evidence, profiles.getFirst());
            outcome = AtfReportStatus.PASS;
            checks =
                    "Four-player Descending presents a genuine thirteen-card first-hand bid screen. The same layout "
                            + "was exercised at "
                            + String.join(
                                    ", ", profiles.stream().map(DisplayProfile::name).toList())
                            + ". Each checkpoint "
                            + "audited every card face, every legal bid, table/seat/button geometry, 48 dp targets, and a "
                            + "full-resolution inline screenshot; no thirteen-card bid was submitted. A normal one-card "
                            + "Romanian Whist was then checked at bidding and play, with its card centered at the bottom.";
        } catch (Throwable failure) {
            outcome = AtfReportStatus.FAIL;
            try {
                List<Path> failureEvidence =
                        ui.capture("whist-lab-thirteen-card-bidding-failure-state");
                evidence.addAll(failureEvidence);
                checkpoint(
                        trace,
                        "TEST BODY",
                        AtfGherkinKeyword.THEN,
                        "Capture the current Android screen after a display check fails",
                        "Current emulator state",
                        failureEvidence,
                        "capture current screenshot and accessibility hierarchy before restoring display",
                        "The failed UI state is preserved in this checkpoint",
                        failure.getClass().getSimpleName()
                                + ": "
                                + String.valueOf(failure.getMessage()),
                        0,
                        List.of(
                                new AtfAssertion(
                                        "Failure screen is retained for audit",
                                        "failure PNG and accessibility XML",
                                        "files=" + failureEvidence.size(),
                                        false)),
                        AtfReportStatus.FAIL);
            } catch (Throwable captureFailure) {
                failure.addSuppressed(captureFailure);
            }
            trace.fail(failure);
            if (failure instanceof Exception exception) throw exception;
            if (failure instanceof Error error) throw error;
            throw new RuntimeException(failure);
        } finally {
            trace.beginPhase("TEARDOWN");
            try {
                restoreDisplay(adb, ui, originalSizeOverride, originalDensityOverride);
                String home =
                        ui.restartToActivity(
                                PACKAGE, ".MainActivity", "--es", "debug_language", "EN");
                List<Path> homeEvidence =
                        ui.capture("whist-lab-thirteen-card-bidding-restored-display-home");
                evidence.addAll(homeEvidence);
                boolean homeRestored =
                        ui.hasText(home, "New game")
                                && matchesRestoredDisplay(
                                        adb, originalSizeOutput, originalDensityOutput);
                checkpoint(
                        trace,
                        "TEARDOWN",
                        AtfGherkinKeyword.THEN,
                        "Restore the original emulator display and leave the game at home",
                        "Original Android display settings",
                        homeEvidence,
                        "reset temporary wm size/density overrides",
                        "original physical display settings restored; Whist Lab home is available",
                        "home visible="
                                + ui.hasText(home, "New game")
                                + "; size="
                                + adb.run("shell", "wm", "size").output().trim()
                                + "; density="
                                + adb.run("shell", "wm", "density").output().trim(),
                        0,
                        List.of(
                                new AtfAssertion(
                                        "Emulator display settings are restored",
                                        "original size and density",
                                        "restored=" + homeRestored,
                                        homeRestored),
                                new AtfAssertion(
                                        "Whist Lab is left at its main menu",
                                        "New game visible",
                                        Boolean.toString(ui.hasText(home, "New game")),
                                        ui.hasText(home, "New game"))),
                        homeRestored ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
                if (!homeRestored) {
                    outcome = AtfReportStatus.FAIL;
                    throw new IllegalStateException(
                            "Emulator display or app home was not restored after resolution checks");
                }
            } catch (Throwable teardownFailure) {
                boolean alreadyFailed = outcome == AtfReportStatus.FAIL;
                trace.fail(teardownFailure);
                if (!alreadyFailed) {
                    outcome = AtfReportStatus.FAIL;
                    if (teardownFailure instanceof Exception exception) throw exception;
                    if (teardownFailure instanceof Error error) throw error;
                    throw new RuntimeException(teardownFailure);
                }
            }
            trace.finish(outcome);
            List<Path> reportEvidence = new ArrayList<>(evidence);
            reportEvidence.add(trace.manifest());
            AndroidUiFlowReportPublisher.publish(
                    artifacts.resolve("whist-lab-thirteen-card-bidding-resolutions.html"),
                    installation.packagePath(),
                    reportSetting("atf.android.serial", "ATF_ANDROID_SERIAL", "emulator-5554"),
                    System.getProperty("atf.android.api", "device reported at runtime"),
                    checks,
                    reportEvidence,
                    trace);
        }
    }
}
