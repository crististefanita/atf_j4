package atf.android.e2e.gameplay.tricks;

import static atf.android.e2e.evidence.frames.PngUptimeMetadata.reencodePngWithUptime;
import static atf.android.e2e.evidence.frames.TrickFrameAnalysis.cardFacesAreRendered;
import static atf.android.e2e.evidence.frames.TrickFrameAnalysis.cardFacesHaveHeartMarks;
import static atf.android.e2e.evidence.frames.TrickFrameAnalysis.countCardFaceFrames;
import static atf.android.e2e.evidence.frames.TrickFrameAnalysis.findRenderedPause;
import static atf.android.e2e.evidence.frames.TrickFrameCapture.finishFrameSampler;
import static atf.android.e2e.evidence.frames.TrickFrameCapture.startFrameSampler;
import static atf.android.e2e.gameplay.support.TrickUiState.awaitNextTrickReady;
import static atf.android.e2e.gameplay.support.TrickUiState.playedCardCount;
import static atf.android.e2e.support.flow.UiFlowCheckpoint.checkpoint;
import static atf.android.e2e.support.flow.WhistLabScenario.PACKAGE;
import static atf.android.e2e.support.flow.WhistLabScenario.artifacts;
import static atf.android.e2e.support.flow.WhistLabScenario.installedApp;
import static atf.android.e2e.support.flow.WhistLabScenario.reportSetting;

import static org.junit.jupiter.api.Assertions.assertTrue;

import atf.android.AdbClient;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.evidence.frames.TrickFrameAnalysis.TrickFrame;
import atf.android.e2e.evidence.frames.TrickFrameAnalysis.TrickFramePair;
import atf.android.e2e.evidence.frames.TrickFrameCapture.FrameSampler;
import atf.android.e2e.support.AndroidUiFlowReportPublisher;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Verifică păstrarea levatei încheiate înaintea reluării interacțiunii. */
public final class CompletedTrickScenario {
    private CompletedTrickScenario() {}

    public static void run(String ownerName) throws Exception {
        var installation = installedApp();
        AdbClient adb = installation.adb();

        Path artifacts = artifacts("completed-trick-display-pause");
        AndroidUiDriver ui = new AndroidUiDriver(adb, artifacts);
        AndroidUiFlowTrace trace =
                new AndroidUiFlowTrace(
                        artifacts,
                        "whist-lab-completed-trick-display-pause",
                        "Whist Lab — four played cards remain visible before the next trick",
                        ownerName,
                        "atf.android.e2e",
                        PACKAGE,
                        List.of(
                                "android",
                                "api-28",
                                "parallel-app",
                                "e2e",
                                "trick-display",
                                "timing"));
        ui.recordCapturesAs(trace::capture);
        trace.beginPhase("SETUP");
        ui.recordActionsAs(trace::recordAction);
        List<Path> evidence = new ArrayList<>();
        AtfReportStatus outcome = AtfReportStatus.FAIL;
        String checks = "The completed trick did not remain visible before the next trick.";
        FrameSampler sampler = null;
        try {
            long started = System.nanoTime();
            String bidding =
                    ui.restartToActivity(
                            PACKAGE,
                            ".MainActivity",
                            "--ez",
                            "debug_trick_pause_fixture",
                            "true",
                            "--es",
                            "debug_language",
                            "EN");
            bidding = ui.awaitText("Place bid", Duration.ofSeconds(12));
            assertTrue(
                    ui.hasDescription(bidding, "Your hand, 2 cards."),
                    "Two-trick fixture did not open at the human bidding turn");
            List<Path> biddingEvidence = ui.capture("whist-lab-trick-pause-bidding");
            evidence.addAll(biddingEvidence);
            checkpoint(
                    trace,
                    "SETUP",
                    AtfGherkinKeyword.GIVEN,
                    "Open a deterministic two-trick deal at the human bidding turn",
                    "debug_trick_pause_fixture=true",
                    biddingEvidence,
                    "launch the two-trick fixture",
                    "Place bid with two cards in hand",
                    "visible labels=" + ui.texts(bidding),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    List.of(
                            new AtfAssertion(
                                    "Fixture opens with two cards",
                                    "Your hand, 2 cards.",
                                    Boolean.toString(
                                            ui.hasDescription(bidding, "Your hand, 2 cards.")),
                                    ui.hasDescription(bidding, "Your hand, 2 cards.")),
                            new AtfAssertion(
                                    "Bidding control is available",
                                    "Place bid",
                                    Boolean.toString(ui.hasText(bidding, "Place bid")),
                                    ui.hasText(bidding, "Place bid"))),
                    AtfReportStatus.PASS);
            trace.beginPhase("TEST BODY");

            started = System.nanoTime();
            ui.selectBid(0);
            ui.tapText("Place bid");
            String play = ui.awaitText("PLAY NOW", Duration.ofSeconds(12));
            assertTrue(
                    ui.hasDescription(play, "Your hand, 2 cards."),
                    "Human did not retain both cards for the first trick");
            assertTrue(ui.hasText(play, "A♥"), "The deterministic lead card A♥ is not visible");
            List<Path> playEvidence = ui.capture("whist-lab-trick-pause-first-turn");
            evidence.addAll(playEvidence);
            checkpoint(
                    trace,
                    "TEST BODY",
                    AtfGherkinKeyword.WHEN,
                    "Submit Bid 0 and reach the human lead with A♥",
                    "bid button 0; Place bid",
                    playEvidence,
                    "select Bid 0; submit the bid",
                    "PLAY NOW with A♥ and two cards",
                    "visible labels=" + ui.texts(play),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    List.of(
                            new AtfAssertion(
                                    "Human can lead the deterministic first trick",
                                    "A♥ and PLAY NOW",
                                    Boolean.toString(
                                            ui.hasText(play, "A♥") && ui.hasText(play, "PLAY NOW")),
                                    ui.hasText(play, "A♥") && ui.hasText(play, "PLAY NOW"))),
                    AtfReportStatus.PASS);

            started = System.nanoTime();
            sampler = startFrameSampler(adb, ui, artifacts);
            ui.tapTextIn(play, "A♥");
            List<TrickFrame> sampledFrames = finishFrameSampler(adb, sampler);
            sampler = null;
            long cardFaceFrameCount = countCardFaceFrames(sampledFrames);
            TrickFramePair renderedPause = findRenderedPause(sampledFrames);
            boolean heldForObservationWindow = renderedPause != null;
            long visibleMillis = renderedPause == null ? 0 : renderedPause.elapsedMillis();
            assertTrue(
                    heldForObservationWindow,
                    "No pair of device-side frames showed all four rendered cards "
                            + "at least 900 ms apart within the 1.5-second pause; sampled="
                            + sampledFrames.size()
                            + "; card-face frames="
                            + cardFaceFrameCount);
            Path firstTrickImage =
                    artifacts.resolve("whist-lab-completed-four-card-trick-at-start.png");
            Path laterTrickImage =
                    artifacts.resolve("whist-lab-completed-four-card-trick-after-900ms.png");
            Files.copy(
                    renderedPause.first().image(),
                    firstTrickImage,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            reencodePngWithUptime(
                    renderedPause.later().image(),
                    laterTrickImage,
                    renderedPause.later().uptimeSeconds());
            List<Path> trickEvidence = List.of(firstTrickImage);
            evidence.add(firstTrickImage);
            evidence.add(laterTrickImage);
            ui.recordAction(
                    "Capture device-side full-display frame",
                    firstTrickImage.getFileName().toString(),
                    "Preserve a frame with all four completed-trick cards",
                    "Frame sampled on Android",
                    0,
                    AtfReportStatus.PASS);
            checkpoint(
                    trace,
                    "TEST BODY",
                    AtfGherkinKeyword.THEN,
                    "Show all four cards at the start of the 1.5-second trick pause",
                    "the completed trick at the center of the table",
                    trickEvidence,
                    "play A♥ and sample full-display PNG frames on the Android device",
                    "four separate ivory card faces with red heart markings remain in the center-table slots",
                    "device-side sampled frames="
                            + sampledFrames.size()
                            + "; four-card frames="
                            + cardFaceFrameCount,
                    visibleMillis,
                    List.of(
                            new AtfAssertion(
                                    "All four completed-trick faces are rendered",
                                    "four ivory card faces in the 2×2 center-table layout",
                                    Boolean.toString(cardFacesAreRendered(firstTrickImage)),
                                    cardFacesAreRendered(firstTrickImage)),
                            new AtfAssertion(
                                    "Each completed card shows its heart rank mark",
                                    "visible red heart/rank pixels on all four card faces",
                                    Boolean.toString(cardFacesHaveHeartMarks(firstTrickImage)),
                                    cardFacesHaveHeartMarks(firstTrickImage))),
                    AtfReportStatus.PASS);

            ui.recordAction(
                    "Capture device-side full-display frame",
                    laterTrickImage.getFileName().toString(),
                    "Retain the same four card faces at least 900 ms later with lossless capture-time metadata",
                    "Frame sampled "
                            + visibleMillis
                            + " ms later; decoded pixels preserved exactly",
                    visibleMillis,
                    AtfReportStatus.PASS);
            checkpoint(
                    trace,
                    "TEST BODY",
                    AtfGherkinKeyword.AND,
                    "Confirm the four-card display remains unchanged 900 ms later",
                    "center-table card regions",
                    List.of(laterTrickImage),
                    "compare two independently sampled full-display PNG frames and the live hierarchy",
                    "the same four card faces remain visible before the 1.5-second pause expires",
                    "all four rendered card faces remain equivalent; elapsed="
                            + visibleMillis
                            + " ms",
                    visibleMillis,
                    List.of(
                            new AtfAssertion(
                                    "Four-card display persists through the observation window",
                                    "same visible center-table cards after >=900 ms and before 1.5 s",
                                    Boolean.toString(heldForObservationWindow),
                                    heldForObservationWindow)),
                    AtfReportStatus.PASS);

            started = System.nanoTime();
            String nextTurn = awaitNextTrickReady(ui, Duration.ofSeconds(5));
            boolean nextTrickReady =
                    ui.hasText(nextTurn, "PLAY NOW") && playedCardCount(ui, nextTurn) == 0;
            assertTrue(
                    nextTrickReady,
                    "The next trick did not start cleanly after the display pause; descriptions="
                            + ui.descriptions(nextTurn));
            List<Path> nextEvidence = ui.capture("whist-lab-next-trick-ready");
            evidence.addAll(nextEvidence);
            ui.recordAction(
                    "Capture next-trick Android UI state",
                    PACKAGE,
                    "Retain the cleared table and human lead before the next card is played",
                    "Captured " + nextEvidence.size() + " final-turn evidence files",
                    0,
                    AtfReportStatus.PASS);
            checkpoint(
                    trace,
                    "TEST BODY",
                    AtfGherkinKeyword.THEN,
                    "Start the next trick only after the completed cards clear",
                    "human lead seat with one card remaining",
                    nextEvidence,
                    "wait for the 1.5-second table pause",
                    "PLAY NOW, one card, and no prior trick cards",
                    "visible labels="
                            + ui.texts(nextTurn)
                            + "; played-card count="
                            + playedCardCount(ui, nextTurn),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    List.of(
                            new AtfAssertion(
                                    "Next trick begins with the winner leading",
                                    "PLAY NOW",
                                    Boolean.toString(ui.hasText(nextTurn, "PLAY NOW")),
                                    ui.hasText(nextTurn, "PLAY NOW")),
                            new AtfAssertion(
                                    "Previous four cards have cleared before the next trick",
                                    "0 played cards",
                                    Integer.toString(playedCardCount(ui, nextTurn)),
                                    playedCardCount(ui, nextTurn) == 0)),
                    AtfReportStatus.PASS);
            checks =
                    "The deterministic two-trick fixture showed all four heart-card faces in Android-side sampled frames "
                            + visibleMillis
                            + " ms apart during the configured 1.5-second pause, then cleared the completed trick before the human "
                            + "winner received the lead for the next trick.";
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
                if (sampler != null)
                    adb.run("shell", "rm", "-rf", sampler.remoteDirectory()).requireSuccess();
                adb.run("shell", "am", "force-stop", PACKAGE).requireSuccess();
                List<Path> teardown = ui.capture("whist-lab-trick-pause-teardown");
                evidence.addAll(teardown);
                ui.recordAction(
                        "Capture final Android UI state",
                        PACKAGE,
                        "Retain the final screenshot and hierarchy after the E2E flow",
                        "Captured " + teardown.size() + " final evidence files",
                        0,
                        AtfReportStatus.PASS);
                checkpoint(
                        trace,
                        "TEARDOWN",
                        AtfGherkinKeyword.THEN,
                        "Retain the final table state",
                        "Whist Lab Android app",
                        teardown,
                        "force-stop and capture the final screen",
                        "final UI evidence remains attached to the flow",
                        "captured=" + teardown.size() + " files",
                        0,
                        List.of(
                                new AtfAssertion(
                                        "Final UI evidence is retained",
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
                    artifacts.resolve("whist-lab-trick-display-pause.html"),
                    installation.packagePath(),
                    reportSetting("atf.android.serial", "ATF_ANDROID_SERIAL", "emulator-5554"),
                    System.getProperty("atf.android.api", "device reported at runtime"),
                    checks,
                    reportEvidence,
                    trace);
        }
    }
}
