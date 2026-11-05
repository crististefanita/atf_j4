package atf.android.e2e.gameplay.presentation;

import static atf.android.e2e.support.flow.UiFlowCheckpoint.checkpoint;
import static org.junit.jupiter.api.Assertions.assertTrue;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Verifică scorul fără confirmarea licitației și revenirea la meniul principal. */
final class SixPlayerNavigationCheck {
    private SixPlayerNavigationCheck() {}

    static void run(AndroidUiDriver ui, AndroidUiFlowTrace trace, List<Path> evidence,
            SixPlayerBiddingCheck.BiddingState state) throws Exception {
        String initialBidSummary = state.initialBidSummary();
        Set<Integer> expectedBids = state.expectedBids();
        long started = System.nanoTime();
        int pendingBid = expectedBids.contains(3) ? 3 : 2;
        ui.selectBid(pendingBid);
        ui.tapDescription("Game menu");
        String menu = ui.awaitText("Score sheet", Duration.ofSeconds(8));
        assertTrue(ui.hasText(menu, "Main menu"), "Game menu omitted Main menu");
        List<Path> menuEvidence = ui.capture("whist-lab-presentation-hamburger-menu");
        evidence.addAll(menuEvidence);
        checkpoint(
                trace,
                "TEST BODY",
                AtfGherkinKeyword.WHEN,
                "Open the top-left hamburger after selecting a bid",
                "Game menu",
                menuEvidence,
                "select bid " + pendingBid + " and activate Game menu",
                "Score sheet and Main menu are available",
                "visible labels=" + ui.texts(menu),
                Duration.ofNanos(System.nanoTime() - started).toMillis(),
                List.of(
                        new AtfAssertion(
                                "Score sheet is in the game menu",
                                "Score sheet",
                                Boolean.toString(ui.hasText(menu, "Score sheet")),
                                ui.hasText(menu, "Score sheet")),
                        new AtfAssertion(
                                "Main menu is in the game menu",
                                "Main menu",
                                Boolean.toString(ui.hasText(menu, "Main menu")),
                                ui.hasText(menu, "Main menu"))),
                AtfReportStatus.PASS);

        started = System.nanoTime();
        ui.tapText("Score sheet");
        String score = ui.awaitText("CLOSE", Duration.ofSeconds(8));
        boolean selectionWasNotSubmitted = ui.hasTextContaining(score, "Bid not placed");
        assertTrue(
                selectionWasNotSubmitted,
                "Opening Score sheet submitted the unconfirmed bid; visible="
                        + ui.texts(score));
        List<String> expectedScoreColumns = List.of("Player", "Bid", "Won", "Hand", "Total");
        List<String> visibleScoreColumns = new ArrayList<>();
        for (String label : expectedScoreColumns) {
            if (ui.hasText(score, label)) visibleScoreColumns.add(label);
        }
        boolean scoreColumnsVisible = visibleScoreColumns.size() == expectedScoreColumns.size();
        assertTrue(
                scoreColumnsVisible,
                "Current score table omitted a column: " + ui.texts(score));
        List<Path> scoreEvidence = ui.capture("whist-lab-presentation-live-score");
        evidence.addAll(scoreEvidence);
        checkpoint(
                trace,
                "TEST BODY",
                AtfGherkinKeyword.THEN,
                "Open the read-only score sheet without submitting the selected bid",
                "Current scores · Hand 1",
                scoreEvidence,
                "open Score sheet and inspect the human score row",
                "unsubmitted bid remains unsubmitted; Player/Bid/Won/Hand/Total columns are aligned",
                "visible labels=" + ui.texts(score),
                Duration.ofNanos(System.nanoTime() - started).toMillis(),
                List.of(
                        new AtfAssertion(
                                "Score view does not commit the pending bid",
                                "Bid not placed",
                                Boolean.toString(selectionWasNotSubmitted),
                                selectionWasNotSubmitted),
                        new AtfAssertion(
                                "Score sheet presents aligned player and score columns",
                                "Player · Bid · Won · Hand · Total",
                                String.join(" · ", visibleScoreColumns),
                                scoreColumnsVisible)),
                AtfReportStatus.PASS);

        started = System.nanoTime();
        ui.tapTextIn(score, "CLOSE");
        String biddingAfterScore = ui.awaitText("Place bid", Duration.ofSeconds(8));
        boolean selectedBidPersisted =
                ui.isSelected(biddingAfterScore, Integer.toString(pendingBid));
        String summaryAfterScore =
                ui.texts(biddingAfterScore).stream()
                        .filter(label -> label.startsWith("Bids submitted "))
                        .findFirst()
                        .orElse("<missing bid summary>");
        boolean bidSummaryPersisted = initialBidSummary.equals(summaryAfterScore);
        assertTrue(
                selectedBidPersisted, "Closing Score sheet lost the unconfirmed bid selection");
        assertTrue(
                bidSummaryPersisted,
                "Score navigation changed submitted bids or bid total: before='"
                        + initialBidSummary
                        + "', after='"
                        + summaryAfterScore
                        + "'");
        List<Path> restoredTableEvidence =
                ui.capture("whist-lab-presentation-restored-bidding");
        evidence.addAll(restoredTableEvidence);
        checkpoint(
                trace,
                "TEST BODY",
                AtfGherkinKeyword.AND,
                "Close Score sheet and verify the same bid remains selected",
                "Bidding table; value " + pendingBid,
                restoredTableEvidence,
                "close Score sheet and inspect the selected bid",
                "bid " + pendingBid + " remains selected and no bid has been submitted",
                "selected="
                        + selectedBidPersisted
                        + "; summary="
                        + summaryAfterScore
                        + "; visible labels="
                        + ui.texts(biddingAfterScore),
                Duration.ofNanos(System.nanoTime() - started).toMillis(),
                List.of(
                        new AtfAssertion(
                                "Score view preserves the pending selection",
                                "Bid " + pendingBid + " selected",
                                Boolean.toString(selectedBidPersisted),
                                selectedBidPersisted),
                        new AtfAssertion(
                                "Score navigation leaves the bid summary unchanged",
                                initialBidSummary,
                                summaryAfterScore,
                                bidSummaryPersisted)),
                AtfReportStatus.PASS);

        trace.beginPhase("TEARDOWN");
        started = System.nanoTime();
        ui.tapDescription("Game menu");
        ui.tapText("Main menu");
        String returnedHome = ui.awaitText("New game", Duration.ofSeconds(10));
        List<Path> returnedHomeEvidence = ui.capture("whist-lab-presentation-return-home");
        evidence.addAll(returnedHomeEvidence);
        List<String> expectedHomeItems = List.of("New game", "Resume game", "How to play");
        List<String> visibleHomeItems = new ArrayList<>();
        for (String label : expectedHomeItems) {
            if (ui.hasText(returnedHome, label)) visibleHomeItems.add(label);
        }
        boolean homeReady = visibleHomeItems.size() == expectedHomeItems.size();
        assertTrue(homeReady, "Main menu did not restore the home navigation options");
        checkpoint(
                trace,
                "TEARDOWN",
                AtfGherkinKeyword.THEN,
                "Return to home from the game menu without advancing play",
                "Main menu",
                returnedHomeEvidence,
                "close Score sheet; open Game menu; choose Main menu",
                "home screen offers New game, Resume game, and How to play",
                "visible labels=" + ui.texts(returnedHome),
                Duration.ofNanos(System.nanoTime() - started).toMillis(),
                List.of(
                        new AtfAssertion(
                                "Home navigation is restored",
                                String.join("; ", expectedHomeItems),
                                String.join("; ", visibleHomeItems),
                                homeReady)),
                AtfReportStatus.PASS);
    }
}
