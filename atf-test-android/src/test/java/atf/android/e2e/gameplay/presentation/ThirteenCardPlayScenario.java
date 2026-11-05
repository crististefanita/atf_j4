package atf.android.e2e.gameplay.presentation;

import static atf.android.e2e.display.support.GameplayGeometry.isIvoryCardSurface;
import static atf.android.e2e.display.support.ScreenGeometry.centerX;
import static atf.android.e2e.display.support.ScreenGeometry.centerY;
import static atf.android.e2e.display.support.ScreenGeometry.height;
import static atf.android.e2e.display.support.ScreenGeometry.width;
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

/** Scenariul selectării și deplasării unei cărți din mâna lungă. */
public final class ThirteenCardPlayScenario {
    private ThirteenCardPlayScenario() {}

    public static void run(String ownerName) throws Exception {
        var installation = installedApp();
        AdbClient adb = installation.adb();

        Path artifacts = artifacts("thirteen-card-fan");
        AndroidUiDriver ui = new AndroidUiDriver(adb, artifacts);
        AndroidUiFlowTrace trace =
                new AndroidUiFlowTrace(
                        artifacts,
                        "whist-lab-thirteen-card-fan",
                        "Whist · Descending — thirteen-card fan and one-card play",
                        ownerName,
                        "atf.android.e2e",
                        PACKAGE,
                        List.of("android", "e2e", "ui", "card-fan", "single-row", "click-flow"));
        ui.recordCapturesAs(trace::capture);
        trace.beginPhase("SETUP");
        ui.recordActionsAs(trace::recordAction);
        List<Path> evidence = new ArrayList<>();
        List<AtfAssertion> assertions = new ArrayList<>();
        AtfReportStatus outcome = AtfReportStatus.FAIL;
        String checks = "The thirteen-card click flow did not complete.";
        try {
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
                    ui.hasText(before, "NO TURNED CARD")
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
                            "A full-deck hand identifies that no turned card exists",
                            "NO TURNED CARD; suit-based trump",
                            "NO TURNED CARD="
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

            started = System.nanoTime();
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
            checks =
                    "A deterministic 13-card Descending hand rendered as one accessible row. Tapping one legal card "
                            + "ran the lift animation, moved that exact card into the central trick, reduced the hand to 12, "
                            + "kept the human seat at the same bounds, retained the trump tile, and left the remaining cards "
                            + "fully opaque during the AI turn.";
            outcome = AtfReportStatus.PASS;
            trace.finish(outcome);
        } catch (Throwable failure) {
            outcome = AtfReportStatus.FAIL;
            trace.fail(failure);
            if (failure instanceof Exception exception) throw exception;
            if (failure instanceof Error error) throw error;
            throw new RuntimeException(failure);
        } finally {
            trace.beginPhase("TEARDOWN");
            try {
                adb.run("shell", "am", "force-stop", PACKAGE).requireSuccess();
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
                    artifacts.resolve("whist-lab-thirteen-card-fan.html"),
                    installation.packagePath(),
                    reportSetting("atf.android.serial", "ATF_ANDROID_SERIAL", "emulator-5554"),
                    System.getProperty("atf.android.api", "device reported at runtime"),
                    checks,
                    reportEvidence,
                    trace);
        }
    }
}
