package atf.android.e2e.gameplay.persistence;

import static atf.android.e2e.evidence.frames.TrickFrameAnalysis.sameGameViewportPixels;
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
            long actionStarted = System.nanoTime();
            String bidding =
                    ui.restartToActivity(
                            PACKAGE,
                            ".MainActivity",
                            "--ez",
                            "debug_fixture",
                            "true",
                            "--ez",
                            "persist_fixture",
                            "true");
            bidding = ui.awaitText("Place bid", Duration.ofSeconds(10));
            assertTrue(
                    ui.hasText(bidding, "Place bid"),
                    "Fixture did not open at its human bidding turn");
            assertTrue(
                    ui.hasDescriptionContaining(bidding, "Trump suit only, no turned card: spades"),
                    "Fixture trump should be Spades without a turned card");
            List<Path> biddingEvidence = ui.capture("whist-lab-bidding");
            evidence.addAll(biddingEvidence);
            checkpoint(
                    trace,
                    "SETUP",
                    AtfGherkinKeyword.GIVEN,
                    "Launch deterministic 4-player fixture at bidding",
                    "debug_fixture=true; persist_fixture=true",
                    biddingEvidence,
                    "force-stop and launch MainActivity with deterministic fixture",
                    "Place bid; accessible trump suit: spades, with no turned card",
                    "visible labels=" + ui.texts(bidding),
                    Duration.ofNanos(System.nanoTime() - actionStarted).toMillis(),
                    List.of(
                            new AtfAssertion(
                                    "Fixture opens at human bidding turn",
                                    "Place bid",
                                    Boolean.toString(ui.hasText(bidding, "Place bid")),
                                    ui.hasText(bidding, "Place bid")),
                            new AtfAssertion(
                                    "Fixture trump is Spades",
                                    "Trump suit only, no turned card: spades",
                                    Boolean.toString(
                                            ui.hasDescriptionContaining(
                                                    bidding,
                                                    "Trump suit only, no turned card: spades")),
                                    ui.hasDescriptionContaining(
                                            bidding, "Trump suit only, no turned card: spades"))),
                    AtfReportStatus.PASS);
            trace.beginPhase("TEST BODY");

            actionStarted = System.nanoTime();
            ui.selectBid(1);
            ui.tapText("Place bid");
            String play = ui.awaitText("A♥", Duration.ofSeconds(10));
            assertTrue(ui.hasText(play, "PLAY NOW"), "Fixture did not show the active play seat");
            boolean bidAndResultStayAtHumanSeat = ui.hasDescriptionContaining(play, "You. Bid 1.");
            assertTrue(bidAndResultStayAtHumanSeat, "Human bid was not retained into play");
            List<Path> playEvidence = ui.capture("whist-lab-play");
            evidence.addAll(playEvidence);
            checkpoint(
                    trace,
                    "TEST BODY",
                    AtfGherkinKeyword.WHEN,
                    "Select Bid 1 and reach the human A♥ turn",
                    "central bid button=1; Place bid",
                    playEvidence,
                    "select bid 1 at table center and tap Place bid",
                    "PLAY NOW with bid 1",
                    "visible labels=" + ui.texts(play),
                    Duration.ofNanos(System.nanoTime() - actionStarted).toMillis(),
                    List.of(
                            new AtfAssertion(
                                    "Active play seat is marked",
                                    "PLAY NOW",
                                    Boolean.toString(ui.hasText(play, "PLAY NOW")),
                                    ui.hasText(play, "PLAY NOW")),
                            new AtfAssertion(
                                    "Bid and won count remain attached to human seat",
                                    "You. Bid 1.",
                                    Boolean.toString(bidAndResultStayAtHumanSeat),
                                    bidAndResultStayAtHumanSeat)),
                    AtfReportStatus.PASS);

            actionStarted = System.nanoTime();
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
