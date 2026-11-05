package atf.android.e2e.gameplay.presentation.fan;

import static atf.android.e2e.display.support.GameplayGeometry.isIvoryCardSurface;
import static atf.android.e2e.display.support.ScreenGeometry.centerX;
import static atf.android.e2e.display.support.ScreenGeometry.centerY;
import static atf.android.e2e.display.support.ScreenGeometry.height;
import static atf.android.e2e.display.support.ScreenGeometry.width;
import static atf.android.e2e.support.flow.UiFlowCheckpoint.checkpoint;
import static org.junit.jupiter.api.Assertions.assertTrue;
import atf.android.AdbClient;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/** Verifică deplasarea cărții și păstrarea mâinii, locului și atuului după apăsare. */
final class FanPlayPhase {
    private FanPlayPhase() {}

    static void verify(AdbClient adb, AndroidUiDriver ui, AndroidUiFlowTrace trace, List<Path> evidence,
            List<AtfAssertion> assertions, FanSetupPhase.InitialState initial) throws Exception {
        String before = initial.hierarchy();
        AndroidUiDriver.Bounds humanSeatBefore = initial.humanSeat();
        long started = System.nanoTime();
        String selectedCard = ui.firstLegalCardDescription(before);
        String cardName = selectedCard.substring(0, selectedCard.lastIndexOf(". "));
        ui.tapDescription(selectedCard);
        String played =
                ui.awaitDescriptionContaining("You played " + cardName, Duration.ofSeconds(8));
        played = ui.awaitDescriptionContaining("Your hand, 12 cards.", Duration.ofSeconds(8));
        List<Path> afterEvidence = ui.capture("whist-lab-thirteen-card-fan-after-play");
        evidence.addAll(afterEvidence);
        AndroidUiDriver.Bounds handBoundsAfter =
                ui.boundsForDescription(played, "Your hand, 12 cards.");
        Path afterScreenshot =
                afterEvidence.stream()
                        .filter(path -> path.getFileName().toString().endsWith(".png"))
                        .findFirst()
                        .orElseThrow();
        String displaySize = adb.run("shell", "wm", "size").output();
        int remainingHandRgb = ui.sampleHandFace(afterScreenshot, handBoundsAfter, displaySize);
        boolean remainingCardsStayOpaque = isIvoryCardSurface(remainingHandRgb);
        AndroidUiDriver.Bounds humanSeatAfter =
                ui.boundsForDescriptionContaining(played, "You. ");
        boolean seatStayedFixed = humanSeatBefore.equals(humanSeatAfter);
        AndroidUiDriver.Bounds playedCardBounds =
                ui.boundsForDescription(played, "You played " + cardName);
        AndroidUiDriver.Bounds playingSurfaceBounds =
                ui.boundsForDescription(played, "Playing surface");
        boolean cardMovedToCenter =
                Math.abs(centerX(playedCardBounds) - centerX(playingSurfaceBounds))
                                <= width(playingSurfaceBounds) * 0.08
                        && Math.abs(centerY(playedCardBounds) - centerY(playingSurfaceBounds))
                                <= height(playingSurfaceBounds) * 0.20;
        int playedCardCenterDeltaX =
                Math.abs(centerX(playedCardBounds) - centerX(playingSurfaceBounds));
        int playedCardCenterDeltaY =
                Math.abs(centerY(playedCardBounds) - centerY(playingSurfaceBounds));
        boolean trumpRemainsVisible =
                ui.descriptions(played).stream().anyMatch(value -> value.startsWith("Trump"));
        assertions.add(
                new AtfAssertion(
                        "Tap plays exactly the selected card into the current trick",
                        "selected card centered within 8% x / 20% y of table",
                        cardName
                                + "; dx="
                                + playedCardCenterDeltaX
                                + "/"
                                + Math.round(width(playingSurfaceBounds) * 0.08)
                                + " px; dy="
                                + playedCardCenterDeltaY
                                + "/"
                                + Math.round(height(playingSurfaceBounds) * 0.20)
                                + " px",
                        cardMovedToCenter));
        assertions.add(
                new AtfAssertion(
                        "One card leaves the hand after the animation",
                        "Your hand, 12 cards.",
                        ui.descriptions(played).stream()
                                .filter(value -> value.startsWith("Your hand"))
                                .findFirst()
                                .orElse("<missing>"),
                        ui.hasDescription(played, "Your hand, 12 cards.")));
        assertions.add(
                new AtfAssertion(
                        "The remaining hand stays opaque during the AI turn",
                        "ivory card face (R≥220, G≥220, B≥210)",
                        String.format(java.util.Locale.ROOT, "#%06X", remainingHandRgb),
                        remainingCardsStayOpaque));
        assertions.add(
                new AtfAssertion(
                        "Human seat bounds remain fixed after play",
                        humanSeatBefore.toString(),
                        humanSeatAfter.toString(),
                        seatStayedFixed));
        assertions.add(
                new AtfAssertion(
                        "Trump stays in the header after play",
                        "accessible trump tile remains",
                        ui.descriptions(played).stream()
                                .filter(value -> value.startsWith("Trump"))
                                .findFirst()
                                .orElse("<missing>"),
                        trumpRemainsVisible));
        checkpoint(
                trace,
                "TEST BODY",
                AtfGherkinKeyword.WHEN,
                "Tap one legal card and verify the central trick and unchanged seat position",
                selectedCard,
                afterEvidence,
                "tap the accessible card slot and let its lift animation finish",
                "the chosen card is centered in the current trick; hand shows 12 cards; south seat stays fixed",
                "selected="
                        + selectedCard
                        + "; trick="
                        + ui.descriptions(played).stream()
                                .filter(value -> value.contains(" played "))
                                .toList()
                        + "; hand="
                        + ui.descriptions(played).stream()
                                .filter(value -> value.startsWith("Your hand"))
                                .toList()
                        + "; remaining card sample="
                        + String.format(java.util.Locale.ROOT, "#%06X", remainingHandRgb)
                        + "; display size="
                        + displaySize.trim()
                        + "; selected card bounds="
                        + playedCardBounds
                        + "; table="
                        + playingSurfaceBounds
                        + "; seat before="
                        + humanSeatBefore
                        + "; after="
                        + humanSeatAfter,
                Duration.ofNanos(System.nanoTime() - started).toMillis(),
                List.copyOf(assertions),
                cardMovedToCenter
                                && seatStayedFixed
                                && ui.hasDescription(played, "Your hand, 12 cards.")
                                && remainingCardsStayOpaque
                                && trumpRemainsVisible
                        ? AtfReportStatus.PASS
                        : AtfReportStatus.FAIL);
        assertTrue(cardMovedToCenter, "Selected card did not appear in the center trick");
        assertTrue(
                seatStayedFixed,
                "Human seat moved during card play: "
                        + humanSeatBefore
                        + " -> "
                        + humanSeatAfter);
        assertTrue(
                ui.hasDescription(played, "Your hand, 12 cards."),
                "Playing one card did not leave a twelve-card hand");
        assertTrue(
                remainingCardsStayOpaque,
                "Remaining cards were dimmed while the AI holds the turn: "
                        + String.format(java.util.Locale.ROOT, "#%06X", remainingHandRgb));
        assertTrue(trumpRemainsVisible, "Trump tile disappeared after play");
    }
}
