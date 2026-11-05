package atf.android.e2e.gameplay.presentation;

import static atf.android.e2e.display.support.GameplayDisplay.overrideDisplay;
import static atf.android.e2e.display.support.GameplayGeometry.singleCardCenterDelta;
import static atf.android.e2e.display.support.GameplayGeometry.singleCardCentered;
import static atf.android.e2e.support.flow.UiFlowCheckpoint.checkpoint;
import static atf.android.e2e.support.flow.WhistLabScenario.PACKAGE;
import static org.junit.jupiter.api.Assertions.assertTrue;
import atf.android.AdbClient;
import atf.android.e2e.display.support.GameplayDisplay.DisplayProfile;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/** Verifică centrarea mâinii de o carte la licitare și la intrarea în joc. */
final class SingleCardCenteringCheck {
    private SingleCardCenteringCheck() {}

    static void run(AdbClient adb, AndroidUiDriver ui, AndroidUiFlowTrace trace, List<Path> evidence,
            DisplayProfile oneCardProfile) throws Exception {
        long oneCardStarted = System.nanoTime();
        overrideDisplay(adb, ui, oneCardProfile);
        String home =
                ui.restartToActivity(PACKAGE, ".MainActivity", "--es", "debug_language", "EN");
        assertTrue(
                ui.hasText(home, "New game"),
                "Home did not open for the one-card layout check");
        ui.tapText("New game");
        ui.awaitText("Start game", Duration.ofSeconds(12));
        ui.selectNamedSpinnerOption("player-count", "4");
        ui.selectNamedSpinnerOption("game-type", "Romanian (1–8–1)");
        String oneCardSetup = ui.awaitText("Start game", Duration.ofSeconds(10));
        assertTrue(
                ui.spinnerValue(oneCardSetup, "Players").equals("4")
                        && ui.spinnerValue(oneCardSetup, "Game type")
                                .equals("Romanian (1–8–1)"),
                "The centered one-card hand check must use the normal four-player Romanian Whist setup");
        ui.revealText("Start game");
        ui.tapText("Start game");
        String oneCardBidding = ui.awaitText("Place bid", Duration.ofSeconds(20));
        oneCardBidding =
                ui.awaitDescriptionContaining("Your hand, 1 cards.", Duration.ofSeconds(10));
        AndroidUiDriver.Bounds oneCardBidViewport =
                ui.boundsForDescription(
                        oneCardBidding,
                        "Your hand, 1 cards.",
                        "android.widget.HorizontalScrollView");
        List<AndroidUiDriver.Bounds> oneCardBidBounds = ui.handCardFaceBounds(oneCardBidding);
        boolean oneCardBidCentered = singleCardCentered(oneCardBidViewport, oneCardBidBounds);
        List<Path> oneCardBidEvidence = ui.capture("whist-lab-single-card-bidding-center");
        evidence.addAll(oneCardBidEvidence);
        checkpoint(
                trace,
                "TEST BODY",
                AtfGherkinKeyword.AND,
                "Center the single-card hand at the bottom of the Romanian Whist bidding screen",
                "Four-player Romanian Whist · one card",
                oneCardBidEvidence,
                "open normal Romanian Whist setup and inspect the one-card hand bounds",
                "the only card is centered within its full-width bottom hand viewport",
                "hand=" + oneCardBidViewport + "; card bounds=" + oneCardBidBounds,
                Duration.ofNanos(System.nanoTime() - oneCardStarted).toMillis(),
                List.of(
                        new AtfAssertion(
                                "One-card bidding hand is centered at the bottom",
                                "1 card; horizontal center delta <= 2% of hand width",
                                "cards="
                                        + oneCardBidBounds.size()
                                        + "; center delta="
                                        + singleCardCenterDelta(
                                                oneCardBidViewport, oneCardBidBounds)
                                        + " px",
                                oneCardBidCentered)),
                oneCardBidCentered ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
        assertTrue(
                oneCardBidCentered,
                "The single card is not centered at the bottom of the bid screen: hand="
                        + oneCardBidViewport
                        + "; card="
                        + oneCardBidBounds);

        oneCardStarted = System.nanoTime();
        ui.selectBid(ui.numericButtonValues(oneCardBidding).getFirst());
        ui.tapText("Place bid");
        ui.awaitText("PLAY NOW", Duration.ofSeconds(15));
        String oneCardPlay =
                ui.awaitDescriptionContaining("Your hand, 1 cards.", Duration.ofSeconds(10));
        AndroidUiDriver.Bounds oneCardPlayViewport =
                ui.boundsForDescription(
                        oneCardPlay,
                        "Your hand, 1 cards.",
                        "android.widget.HorizontalScrollView");
        List<AndroidUiDriver.Bounds> oneCardPlayBounds = ui.handCardFaceBounds(oneCardPlay);
        boolean oneCardPlayCentered =
                singleCardCentered(oneCardPlayViewport, oneCardPlayBounds);
        List<Path> oneCardPlayEvidence = ui.capture("whist-lab-single-card-playing-center");
        evidence.addAll(oneCardPlayEvidence);
        checkpoint(
                trace,
                "TEST BODY",
                AtfGherkinKeyword.THEN,
                "Keep the single card centered at the bottom after bidding enters play",
                "Romanian Whist · PLAY NOW · one card",
                oneCardPlayEvidence,
                "submit one bid and inspect the human turn before playing the card",
                "PLAY NOW is visible and the single card remains centered in the bottom hand row",
                "hand=" + oneCardPlayViewport + "; card bounds=" + oneCardPlayBounds,
                Duration.ofNanos(System.nanoTime() - oneCardStarted).toMillis(),
                List.of(
                        new AtfAssertion(
                                "One-card playing hand remains centered at the bottom",
                                "PLAY NOW; 1 card; horizontal center delta <= 2% of hand width",
                                "PLAY NOW="
                                        + ui.hasText(oneCardPlay, "PLAY NOW")
                                        + "; cards="
                                        + oneCardPlayBounds.size()
                                        + "; center delta="
                                        + singleCardCenterDelta(
                                                oneCardPlayViewport, oneCardPlayBounds)
                                        + " px",
                                oneCardPlayCentered && ui.hasText(oneCardPlay, "PLAY NOW"))),
                oneCardPlayCentered && ui.hasText(oneCardPlay, "PLAY NOW")
                        ? AtfReportStatus.PASS
                        : AtfReportStatus.FAIL);
        assertTrue(
                oneCardPlayCentered,
                "The single card is not centered at the bottom of the play screen: hand="
                        + oneCardPlayViewport
                        + "; card="
                        + oneCardPlayBounds);
    }
}
