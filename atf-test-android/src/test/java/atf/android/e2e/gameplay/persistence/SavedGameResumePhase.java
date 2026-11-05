package atf.android.e2e.gameplay.persistence;

import static atf.android.e2e.evidence.frames.TrickFrameAnalysis.sameGameViewportPixels;
import static atf.android.e2e.support.flow.UiFlowCheckpoint.checkpoint;
import static atf.android.e2e.support.flow.WhistLabScenario.PACKAGE;
import static org.junit.jupiter.api.Assertions.assertTrue;
import atf.android.AdbClient;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Verifică oprirea procesului, reluarea aceleiași ture și scorul rezultat. */
final class SavedGameResumePhase {
    private SavedGameResumePhase() {}

    static void run(AdbClient adb, AndroidUiDriver ui, AndroidUiFlowTrace trace, List<Path> evidence,
            List<Path> playEvidence) throws Exception {
        long actionStarted = System.nanoTime();
        long processStopStarted = System.nanoTime();
        try {
            adb.run("shell", "am", "force-stop", PACKAGE).requireSuccess();
            ui.recordAction(
                    "Force-stop Android app",
                    PACKAGE,
                    "The active game process stops",
                    "ADB accepted force-stop",
                    Duration.ofNanos(System.nanoTime() - processStopStarted).toMillis(),
                    AtfReportStatus.PASS);
        } catch (Exception failure) {
            ui.recordAction(
                    "Force-stop Android app",
                    PACKAGE,
                    "The active game process stops",
                    failure.toString(),
                    Duration.ofNanos(System.nanoTime() - processStopStarted).toMillis(),
                    AtfReportStatus.FAIL);
            throw failure;
        }
        long relaunchStarted = System.nanoTime();
        try {
            adb.run("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivity")
                    .requireSuccess();
            ui.recordAction(
                    "Relaunch Android activity",
                    PACKAGE + "/.MainActivity",
                    "The app returns to its home screen",
                    "ADB accepted activity start",
                    Duration.ofNanos(System.nanoTime() - relaunchStarted).toMillis(),
                    AtfReportStatus.PASS);
        } catch (Exception failure) {
            ui.recordAction(
                    "Relaunch Android activity",
                    PACKAGE + "/.MainActivity",
                    "The app returns to its home screen",
                    failure.toString(),
                    Duration.ofNanos(System.nanoTime() - relaunchStarted).toMillis(),
                    AtfReportStatus.FAIL);
            throw failure;
        }
        String home = ui.awaitText("Resume game", Duration.ofSeconds(10));
        assertTrue(
                ui.hasText(home, "New game"),
                "App did not return to its home screen after process restart");
        ui.tapText("Resume game");
        String resumed = ui.awaitText("A♥", Duration.ofSeconds(10));
        assertTrue(
                ui.hasText(resumed, "PLAY NOW"),
                "Saved game did not restore the active play seat");
        boolean resumedBidAndResult = ui.hasDescriptionContaining(resumed, "You. Bid 1.");
        assertTrue(resumedBidAndResult, "Saved human bid was lost after process restart");
        List<Path> resumeEvidence = ui.capture("whist-lab-resume");
        Path beforeRestartScreenshot =
                playEvidence.stream()
                        .filter(
                                file ->
                                        file.toString()
                                                .toLowerCase(java.util.Locale.ROOT)
                                                .endsWith(".png"))
                        .findFirst()
                        .orElseThrow();
        Path resumedScreenshot =
                resumeEvidence.stream()
                        .filter(
                                file ->
                                        file.toString()
                                                .toLowerCase(java.util.Locale.ROOT)
                                                .endsWith(".png"))
                        .findFirst()
                        .orElseThrow();
        boolean resumedViewportMatches =
                sameGameViewportPixels(beforeRestartScreenshot, resumedScreenshot);
        assertTrue(
                resumedViewportMatches,
                "Process resume changed the rendered play viewport despite restoring the same turn");

        actionStarted = System.nanoTime();
        ui.tapText("A♥");
        String score = ui.awaitText("Score sheet", Duration.ofSeconds(12));
        String exactScore = "You | Bid: 1 | Tricks won: 1 | Hand: +11 | Total: 11";
        assertTrue(
                ui.hasDescription(score, exactScore),
                "Exact-bid score row is incorrect; visible texts=" + ui.texts(score));
        List<Path> scoreEvidence = ui.capture("whist-lab-score");
        evidence.addAll(scoreEvidence);
        evidence.addAll(resumeEvidence);
        List<Path> resumedScoringEvidence = new ArrayList<>(scoreEvidence);
        resumeEvidence.stream()
                .filter(
                        file ->
                                file.toString()
                                        .toLowerCase(java.util.Locale.ROOT)
                                        .endsWith(".xml"))
                .forEach(resumedScoringEvidence::add);
        checkpoint(
                trace,
                "TEST BODY",
                AtfGherkinKeyword.THEN,
                "Resume the saved turn and play A♥ to verify exact-bid scoring",
                "Resume game; A♥; Score sheet",
                resumedScoringEvidence,
                "force-stop; relaunch; resume; play A♥; let AI finish the trick",
                exactScore,
                "visible labels=" + ui.texts(score),
                Duration.ofNanos(System.nanoTime() - actionStarted).toMillis(),
                List.of(
                        new AtfAssertion(
                                "Resume restores the active play seat",
                                "PLAY NOW",
                                Boolean.toString(ui.hasText(resumed, "PLAY NOW")),
                                ui.hasText(resumed, "PLAY NOW")),
                        new AtfAssertion(
                                "Resume preserves the human bid and won count",
                                "You. Bid 1.",
                                Boolean.toString(resumedBidAndResult),
                                resumedBidAndResult),
                        new AtfAssertion(
                                "Resume preserves the rendered play viewport",
                                "same game pixels before and after process restart",
                                Boolean.toString(resumedViewportMatches),
                                resumedViewportMatches),
                        new AtfAssertion(
                                "Exact bid earns 10 bonus plus one trick point",
                                exactScore,
                                Boolean.toString(ui.hasDescription(score, exactScore)),
                                ui.hasDescription(score, exactScore))),
                AtfReportStatus.PASS);
    }
}
