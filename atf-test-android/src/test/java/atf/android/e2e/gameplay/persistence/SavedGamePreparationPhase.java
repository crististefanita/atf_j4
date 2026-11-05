package atf.android.e2e.gameplay.persistence;

import static atf.android.e2e.support.flow.UiFlowCheckpoint.checkpoint;
import static atf.android.e2e.support.flow.WhistLabScenario.PACKAGE;
import static org.junit.jupiter.api.Assertions.assertTrue;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/** Pregătește licitația și păstrează captura turei care va fi restaurată. */
final class SavedGamePreparationPhase {
    private SavedGamePreparationPhase() {}

    static List<Path> run(AndroidUiDriver ui, AndroidUiFlowTrace trace, List<Path> evidence) throws Exception {
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

        return playEvidence;
    }
}
