package atf.android.e2e.gameplay.presentation.fan;

import static atf.android.e2e.display.support.ScreenGeometry.centerX;
import static atf.android.e2e.display.support.ScreenGeometry.centerY;
import static atf.android.e2e.display.support.ScreenGeometry.height;
import static atf.android.e2e.display.support.ScreenGeometry.width;
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
import java.util.ArrayList;
import java.util.List;

/** Pregătește mâna deterministă și verifică geometria inițială înaintea apăsării. */
final class FanSetupPhase {
    record InitialState(String hierarchy, AndroidUiDriver.Bounds humanSeat) {}

    private FanSetupPhase() {}

    static InitialState open(AndroidUiDriver ui, AndroidUiFlowTrace trace, List<Path> evidence,
            List<AtfAssertion> assertions) throws Exception {
        long started = System.nanoTime();
        String before =
                ui.restartToActivity(
                        PACKAGE,
                        ".MainActivity",
                        "--ez",
                        "debug_hand_fan_fixture",
                        "true",
                        "--ez",
                        "debug_hold_ai_turns",
                        "true",
                        "--es",
                        "debug_language",
                        "EN");
        before = ui.awaitDescriptionContaining("Your hand, 13 cards.", Duration.ofSeconds(12));
        List<String> legalCards = ui.legalCardDescriptions(before);
        List<AndroidUiDriver.Bounds> cardSlots = new ArrayList<>();
        for (String description : legalCards)
            cardSlots.add(ui.boundsForDescription(before, description));
        boolean oneVisibleRow =
                legalCards.size() == 13
                        && cardSlots.stream()
                                        .map(AndroidUiDriver.Bounds::top)
                                        .distinct()
                                        .count()
                                == 1;
        AndroidUiDriver.Bounds handBoundsBefore =
                ui.boundsForDescription(
                        before, "Your hand, 13 cards.", "android.widget.HorizontalScrollView");
        boolean finalCardFitsViewport =
                cardSlots.size() == 13
                        && cardSlots.getLast().right() <= handBoundsBefore.right()
                        && width(cardSlots.getLast()) > width(cardSlots.getFirst());
        String trumpDescription =
                ui.descriptions(before).stream()
                        .filter(value -> value.startsWith("Trump"))
                        .findFirst()
                        .orElse("");
        boolean suitOnlyTrumpIsExplicit =
                ui.hasText(before, "♠")
                        && !ui.hasText(before, "NO TURNED CARD")
                        && trumpDescription.startsWith("Trump suit only, no turned card: ");
        boolean identityAndTrump =
                ui.hasText(before, "Whist · Descending") && suitOnlyTrumpIsExplicit;
        boolean seatSignalsAreVisible =
                ui.hasText(before, "PLAY NOW")
                        && ui.hasDescriptionContaining(before, ". Bid 0")
                        && ui.hasDescriptionContaining(before, ". Tricks won 0");
        AndroidUiDriver.Bounds playingSurfaceBefore =
                ui.boundsForDescription(before, "Playing surface");
        AndroidUiDriver.Bounds gamePanelBefore =
                ui.boundsForDescription(before, "Game table, 4 players.");
        double presentationCenterY =
                (playingSurfaceBefore.top() + handBoundsBefore.bottom()) / 2.0;
        double gamePanelCenterY = centerY(gamePanelBefore);
        double verticalBalanceTolerance = height(playingSurfaceBefore) * 0.08;
        boolean tableAndHandVerticallyBalanced =
                Math.abs(presentationCenterY - gamePanelCenterY) <= verticalBalanceTolerance;
        AndroidUiDriver.Bounds humanSeatBefore =
                ui.boundsForDescriptionContaining(before, "You. ");
        String tableSnapshot = before;
        List<AndroidUiDriver.Bounds> fourPlayerSeats =
                ui.descriptions(before).stream()
                        .filter(value -> value.contains(". Bid ") && value.contains(". Score "))
                        .map(
                                description -> {
                                    try {
                                        return ui.boundsForDescription(
                                                tableSnapshot, description);
                                    } catch (Exception failure) {
                                        throw new IllegalStateException(failure);
                                    }
                                })
                        .toList();
        double fourPlayerCenterX =
                playingSurfaceBefore.left() + width(playingSurfaceBefore) / 2.0;
        boolean humanSeatIsSouth =
                Math.abs(centerX(humanSeatBefore) - fourPlayerCenterX)
                                <= width(playingSurfaceBefore) * 0.16
                        && (humanSeatBefore.top() - playingSurfaceBefore.top())
                                >= height(playingSurfaceBefore) * 0.78;
        long northSeats =
                fourPlayerSeats.stream()
                        .filter(
                                bounds ->
                                        (bounds.top() - playingSurfaceBefore.top())
                                                        <= height(playingSurfaceBefore) * 0.12
                                                && Math.abs(centerX(bounds) - fourPlayerCenterX)
                                                        <= width(playingSurfaceBefore) * 0.16)
                        .count();
        long westSeats =
                fourPlayerSeats.stream()
                        .filter(
                                bounds -> {
                                    double x =
                                            (centerX(bounds) - playingSurfaceBefore.left())
                                                    / (double) width(playingSurfaceBefore);
                                    double y =
                                            (bounds.top() - playingSurfaceBefore.top())
                                                    / (double) height(playingSurfaceBefore);
                                    return x <= 0.32 && y >= 0.30 && y <= 0.65;
                                })
                        .count();
        long eastSeats =
                fourPlayerSeats.stream()
                        .filter(
                                bounds -> {
                                    double x =
                                            (centerX(bounds) - playingSurfaceBefore.left())
                                                    / (double) width(playingSurfaceBefore);
                                    double y =
                                            (bounds.top() - playingSurfaceBefore.top())
                                                    / (double) height(playingSurfaceBefore);
                                    return x >= 0.68 && y >= 0.30 && y <= 0.65;
                                })
                        .count();
        boolean fourPlayerMockRingRendered =
                fourPlayerSeats.size() == 4
                        && humanSeatIsSouth
                        && northSeats == 1
                        && westSeats == 1
                        && eastSeats == 1;
        List<Path> beforeEvidence = ui.capture("whist-lab-thirteen-card-fan-before-play");
        evidence.addAll(beforeEvidence);
        assertions.add(
                new AtfAssertion(
                        "All thirteen cards are exposed on one row",
                        "13 clickable cards; 1 y-coordinate",
                        legalCards.size()
                                + " cards; rows="
                                + cardSlots.stream()
                                        .map(AndroidUiDriver.Bounds::top)
                                        .distinct()
                                        .count(),
                        oneVisibleRow));
        assertions.add(
                new AtfAssertion(
                        "The final card face is not clipped by the hand viewport",
                        "last slot fits viewport and remains wider than overlap",
                        "last width="
                                + width(cardSlots.getLast())
                                + " px; first width="
                                + width(cardSlots.getFirst())
                                + " px; right="
                                + cardSlots.getLast().right()
                                + "/"
                                + handBoundsBefore.right()
                                + " px",
                        finalCardFitsViewport));
        assertions.add(
                new AtfAssertion(
                        "Variant and trump stay visible in the header",
                        "Whist · Descending; trump tile",
                        (ui.hasText(before, "Whist · Descending")
                                        ? "Whist · Descending; "
                                        : "variant missing; ")
                                + trumpDescription,
                        identityAndTrump));
        assertions.add(
                new AtfAssertion(
                        "A full-deck hand shows the trump suit without a redundant status footer",
                        "♠ visible; full accessible suit identity; no status footer",
                        "suit=" + ui.hasText(before, "♠") + "; redundant footer="
                                + ui.hasText(before, "NO TURNED CARD")
                                + "; "
                                + trumpDescription,
                        suitOnlyTrumpIsExplicit));
        assertions.add(
                new AtfAssertion(
                        "Turn badge and seat bid/result stay visible",
                        "PLAY NOW; Bid 0; Won 0",
                        "PLAY NOW="
                                + ui.hasText(before, "PLAY NOW")
                                + "; Bid 0="
                                + ui.hasDescriptionContaining(before, ". Bid 0")
                                + "; Won 0="
                                + ui.hasDescriptionContaining(before, ". Tricks won 0"),
                        seatSignalsAreVisible));
        assertions.add(
                new AtfAssertion(
                        "Four player seats follow the approved oval",
                        "4 seats; N/W/E/S",
                        "seats="
                                + fourPlayerSeats.size()
                                + "; N="
                                + northSeats
                                + "; W="
                                + westSeats
                                + "; E="
                                + eastSeats
                                + "; south="
                                + humanSeatIsSouth,
                        fourPlayerMockRingRendered));
        assertions.add(
                new AtfAssertion(
                        "Table and hand are vertically balanced in the game viewport",
                        "surface-to-hand center within "
                                + Math.round(verticalBalanceTolerance)
                                + " px of panel center",
                        "center delta="
                                + Math.round(Math.abs(presentationCenterY - gamePanelCenterY))
                                + " px; limit="
                                + Math.round(verticalBalanceTolerance)
                                + " px",
                        tableAndHandVerticallyBalanced));
        checkpoint(
                trace,
                "SETUP",
                AtfGherkinKeyword.GIVEN,
                "Open a deterministic thirteen-card hand with the human on lead",
                "debug_hand_fan_fixture; AI turns held",
                beforeEvidence,
                "launch the debug deal and inspect each clickable card bound",
                "13 cards on one row; four seats match N/W/E/S anchors; PLAY NOW and Bid/Won facts visible; header identity and trump persist",
                "legal cards="
                        + legalCards
                        + "; seat="
                        + humanSeatBefore
                        + "; seat positions="
                        + fourPlayerSeats
                        + "; hand viewport="
                        + handBoundsBefore
                        + "; final slot="
                        + cardSlots.getLast()
                        + "; descriptions="
                        + ui.descriptions(before),
                Duration.ofNanos(System.nanoTime() - started).toMillis(),
                List.copyOf(assertions),
                oneVisibleRow
                                && finalCardFitsViewport
                                && identityAndTrump
                                && fourPlayerMockRingRendered
                                && tableAndHandVerticallyBalanced
                        ? AtfReportStatus.PASS
                        : AtfReportStatus.FAIL);
        trace.beginPhase("TEST BODY");
        assertTrue(
                oneVisibleRow,
                "Expected thirteen accessible card slots in one row: " + legalCards);
        assertTrue(
                finalCardFitsViewport,
                "The terminal card is clipped by the hand viewport: "
                        + cardSlots.getLast()
                        + " in "
                        + handBoundsBefore);
        assertTrue(identityAndTrump, "Variant title or trump indicator was missing");
        assertTrue(seatSignalsAreVisible, "Turn badge or seat bid/result facts were missing");
        assertTrue(
                fourPlayerMockRingRendered,
                "Four player seats differ from the approved oval: "
                        + fourPlayerSeats
                        + "; human south="
                        + humanSeatIsSouth);
        assertTrue(
                tableAndHandVerticallyBalanced,
                "The table and hand are unbalanced vertically: surface="
                        + playingSurfaceBefore
                        + "; hand="
                        + handBoundsBefore
                        + "; panel="
                        + gamePanelBefore);

        return new InitialState(before, humanSeatBefore);
    }
}
