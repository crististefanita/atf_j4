package atf.android.e2e.display.flow.responsive;

import static atf.android.e2e.display.flow.responsive.ResponsiveEvidence.check;
import static atf.android.e2e.display.flow.responsive.ResponsiveEvidence.checkCaptureSize;
import static atf.android.e2e.display.flow.responsive.ResponsiveEvidence.publishCheckpoint;
import static atf.android.e2e.display.flow.responsive.ResponsiveEvidence.record;
import static atf.android.e2e.display.flow.responsive.ResponsiveGeometry.contains;
import static atf.android.e2e.display.flow.responsive.ResponsiveGeometry.darkTextPixels;
import static atf.android.e2e.display.flow.responsive.ResponsiveGeometry.noOverlap;
import static atf.android.e2e.display.flow.responsive.ResponsiveGeometry.overlaps;
import static atf.android.e2e.display.flow.responsive.ResponsiveGeometry.sampleFrameFill;
import static atf.android.e2e.display.flow.responsive.ResponsiveSettings.APP;
import static atf.android.e2e.display.flow.responsive.ResponsiveSettings.WAIT;
import static atf.android.e2e.display.flow.responsive.ResponsiveTableState.awaitPlayableTurn;
import static atf.android.e2e.display.flow.responsive.ResponsiveTableState.dealNumber;
import static atf.android.e2e.display.flow.responsive.ResponsiveTableState.handSize;
import static atf.android.e2e.display.flow.responsive.ResponsiveTableState.revealCompletePlayedCard;
import static atf.android.e2e.display.flow.responsive.ResponsiveTableState.revealSeatCross;
import static atf.android.e2e.display.flow.responsive.ResponsiveTableState.romanianFourPlayerHandSize;
import static atf.android.e2e.display.flow.responsive.ResponsiveTableState.seatBounds;
import static atf.android.e2e.display.flow.responsive.ResponsiveTableState.seatDescriptions;
import static atf.android.e2e.display.support.ScreenGeometry.centerX;
import static atf.android.e2e.display.support.ScreenGeometry.height;
import static atf.android.e2e.display.support.ScreenGeometry.width;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import atf.android.AdbClient;
import atf.android.e2e.display.profile.ResponsiveDisplayProfile;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfAssertion;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;

/** Parcurge jocul scurt și verifică acțiunile la masa de joc. */
public final class ResponsiveGameFlow {
    private ResponsiveGameFlow() {}

    public static void playShortGame(
            AdbClient adb,
            AndroidUiDriver ui,
            AndroidUiFlowTrace trace,
            List<Path> evidence,
            ResponsiveDisplayProfile profile,
            int hands)
            throws Exception {
        String home = ui.restartToActivity(APP, ".MainActivity", "--es", "debug_language", "EN");
        record(
                ui,
                adb,
                trace,
                evidence,
                profile,
                "Open the normal home menu",
                List.of(
                        check(
                                "New game and language are visible",
                                "New game and one language selector",
                                ui.hasText(home, "New game")
                                        && ui.hasClass(home, "android.widget.Spinner")),
                        check(
                                "Home fits the first viewport",
                                "all visible home content inside its scroll viewport",
                                ui.allContentFitsViewport(home))));

        ui.tapText("New game");
        String setup = ui.awaitText("Start game", WAIT);
        ui.selectNamedSpinnerOption("player-count", "4");
        ui.selectNamedSpinnerOption("game-type", "Romanian (1–8–1)");
        setup = ui.awaitText("Start game", WAIT);
        record(
                ui,
                adb,
                trace,
                evidence,
                profile,
                "Set up a real four-player Romanian game",
                List.of(
                        check(
                                "Selected game type",
                                "Romanian (1–8–1)",
                                ui.spinnerValue(setup, "Game type").equals("Romanian (1–8–1)")),
                        check(
                                "Start remains visible without scrolling",
                                "Start game and full setup inside viewport",
                                ui.hasText(setup, "Start game")
                                        && ui.allContentFitsViewport(setup))));

        ui.tapText("Start game");
        for (int handNumber = 1; handNumber <= hands; handNumber++) {
            String bidding = ui.awaitText("Place bid", WAIT);
            int handSize = handSize(ui, bidding);
            int displayedDealNumber = dealNumber(ui, bidding);
            assertEquals(
                    handNumber, displayedDealNumber, "The Romanian deal sequence did not advance");
            assertEquals(
                    romanianFourPlayerHandSize(handNumber),
                    handSize,
                    "Unexpected 1–8–1 hand size at deal " + handNumber);
            recordBidding(ui, adb, trace, evidence, profile, handNumber, handSize, bidding);

            List<Integer> legalBids = AndroidUiDriver.enabledNumericButtonValues(bidding);
            assertTrue(!legalBids.isEmpty(), "No enabled contract is available");
            int bid = legalBids.getFirst();
            ui.selectBid(bid);
            ui.tapText("Place bid");

            int played = 0;
            while (played < handSize) {
                String turn = awaitPlayableTurn(adb, ui);
                String card = ui.firstLegalCardDescription(turn);
                if (played == 0) {
                    record(
                            ui,
                            adb,
                            trace,
                            evidence,
                            profile,
                            "Play hand " + handNumber + " with the displayed legal card",
                            List.of(
                                    check(
                                            "Human turn is marked",
                                            "PLAY NOW",
                                            ui.hasText(turn, "PLAY NOW")),
                                    check(
                                            "A legal hand card is offered",
                                            "one or more legal cards",
                                            !ui.legalCardDescriptions(turn).isEmpty())));
                }
                int remaining = handSize - played - 1;
                ui.tapDescription(card);
                played++;
                if (remaining > 0) {
                    ui.awaitDescriptionContaining("Your hand, " + remaining + " cards.", WAIT);
                }
            }
            String settled = ui.awaitText("Next hand", WAIT);
            record(
                    ui,
                    adb,
                    trace,
                    evidence,
                    profile,
                    "Finish real deal " + handNumber + " with " + handSize + " tricks",
                    List.of(
                            check(
                                    "Completed deal advances normally",
                                    "Next hand",
                                    ui.hasText(settled, "Next hand")),
                            check(
                                    "Score sheet lists all four players",
                                    "You, Ava, Miles, Grace",
                                    ui.hasText(settled, "You")
                                            && ui.hasText(settled, "Ava")
                                            && ui.hasText(settled, "Miles")
                                            && ui.hasText(settled, "Grace"))));
            if (handNumber < hands) {
                ui.tapText("Next hand");
            }
        }

        ui.restartToActivity(APP, ".MainActivity", "--es", "debug_language", "EN");
        String resumed = ui.awaitText("Resume game", WAIT);
        ui.tapText("Resume game");
        resumed = ui.awaitContains("Score history", WAIT);
        ui.revealText("Score history");
        ui.tapText("Score history");
        String history = ui.awaitText("Total", WAIT);
        record(
                ui,
                adb,
                trace,
                evidence,
                profile,
                "Review the score after restarting the app",
                List.of(
                        check(
                                "Score history exposes bid, hand and total",
                                "Bid; Hand; Total",
                                ui.hasText(history, "Bid")
                                        && ui.hasText(history, "Hand")
                                        && ui.hasText(history, "Total")),
                        check(
                                "Score history remains selectable",
                                "player selector",
                                ui.hasClass(history, "android.widget.Spinner"))));
        ui.tapText("CLOSE");
        ui.awaitText("Next hand", WAIT);
    }

    public static void recordBidding(
            AndroidUiDriver ui,
            AdbClient adb,
            AndroidUiFlowTrace trace,
            List<Path> evidence,
            ResponsiveDisplayProfile profile,
            int handNumber,
            int handSize,
            String bidding)
            throws Exception {
        var hand = ui.boundsForDescriptionContaining(bidding, "Your hand,");
        var faces =
                ui.handCardFaceBounds(bidding).stream()
                        .sorted(Comparator.comparingInt(AndroidUiDriver.Bounds::left))
                        .toList();
        var seat = ui.boundsForDescriptionContaining(bidding, "You. Bid ");
        var board = ui.boundsForDescription(bidding, "Game table, 4 players.");
        List<Integer> bids = ui.numericButtonValues(bidding);
        List<Integer> enabledBids = AndroidUiDriver.enabledNumericButtonValues(bidding);
        List<AndroidUiDriver.Bounds> targets = new ArrayList<>();
        for (int bid : bids) targets.add(ui.buttonBounds(bidding, Integer.toString(bid)));
        targets.add(ui.buttonBounds(bidding, "Place bid"));
        List<AndroidUiDriver.Bounds> seats = seatBounds(ui, bidding);
        boolean distinctTargets =
                noOverlap(targets)
                        && targets.stream()
                                .allMatch(
                                        target ->
                                                seats.stream()
                                                        .noneMatch(
                                                                other -> overlaps(target, other)));
        int controlTop =
                targets.stream()
                        .mapToInt(AndroidUiDriver.Bounds::top)
                        .min()
                        .orElse(Integer.MAX_VALUE);
        int controlBottom =
                targets.stream()
                        .mapToInt(AndroidUiDriver.Bounds::bottom)
                        .max()
                        .orElse(Integer.MIN_VALUE);
        int upperSeatBottom =
                seats.stream()
                        .filter(other -> other.bottom() <= seat.top())
                        .mapToInt(AndroidUiDriver.Bounds::bottom)
                        .max()
                        .orElse(Integer.MAX_VALUE);
        int upperClearance = controlTop - upperSeatBottom;
        int humanClearance = seat.top() - controlBottom;
        boolean bidZoneFavorsHuman =
                upperSeatBottom < controlTop
                        && controlBottom < seat.top()
                        && humanClearance < upperClearance;
        boolean insideBoard =
                targets.stream().allMatch(target -> contains(board, target))
                        && faces.stream().allMatch(face -> contains(board, face));
        int centerError =
                faces.isEmpty()
                        ? Integer.MAX_VALUE
                        : Math.abs(
                                faces.getFirst().left()
                                        + faces.getLast().right()
                                        - hand.left()
                                        - hand.right());

        List<AtfAssertion> checks =
                new ArrayList<>(
                        List.of(
                                check(
                                        "Current deal size",
                                        handSize + " cards",
                                        faces.size() + " cards",
                                        faces.size() == handSize),
                                check(
                                        "Hand remains centered in the lower table",
                                        "within one pixel",
                                        centerError / 2.0,
                                        centerError <= 2),
                                check(
                                        "Cards share one baseline below the player",
                                        "one row, below You",
                                        faces.size() + " faces",
                                        faces.stream()
                                                .allMatch(
                                                        face ->
                                                                face.top() > seat.bottom()
                                                                        && face.bottom()
                                                                                == faces.getLast()
                                                                                        .bottom())),
                                check(
                                        "Selectable bids stay within the deal range",
                                        "enabled value 0.." + handSize,
                                        enabledBids.toString(),
                                        !enabledBids.isEmpty()
                                                && bids.stream()
                                                        .allMatch(
                                                                value ->
                                                                        value >= 0
                                                                                && value
                                                                                        <= handSize)),
                                check(
                                        "Bids and hand stay inside the game table",
                                        "inside",
                                        insideBoard,
                                        insideBoard),
                                check(
                                        "Bid controls do not cover the four player seats",
                                        "no overlap",
                                        distinctTargets,
                                        distinctTargets),
                                check(
                                        "Bid controls sit closer to the human seat than the opponents",
                                        "upper gap > human gap",
                                        "upper="
                                                + upperClearance
                                                + " px; human="
                                                + humanClearance
                                                + " px",
                                        bidZoneFavorsHuman)));
        List<Path> capture = ui.capture(profile.id() + "-deal-" + handNumber + "-bidding");
        evidence.addAll(capture);
        checkCaptureSize(adb, capture, profile, checks);
        publishCheckpoint(
                ui,
                trace,
                profile,
                "Inspect bidding and centered hand for deal " + handNumber,
                capture,
                checks);
    }

    public static void inspectPlayedCard(
            AdbClient adb,
            AndroidUiDriver ui,
            AndroidUiFlowTrace trace,
            List<Path> evidence,
            ResponsiveDisplayProfile profile)
            throws Exception {
        ui.restartToActivity(
                APP,
                ".MainActivity",
                "--ez",
                "debug_played_card_fixture",
                "true",
                "--ez",
                "debug_hold_ai_turns",
                "true",
                "--es",
                "debug_language",
                "EN");
        String xml = ui.awaitDescriptionContaining("You played ", WAIT);
        List<String> playedCards =
                ui.descriptions(xml).stream()
                        .filter(description -> description.startsWith("You played "))
                        .toList();
        if (playedCards.size() != 1) {
            throw new IllegalStateException(
                    "Expected one deterministic card in the current trick: " + playedCards);
        }
        xml = revealCompletePlayedCard(adb, ui, playedCards.getFirst(), profile, xml);
        AndroidUiDriver.Bounds cardSlot = ui.boundsForDescription(xml, playedCards.getFirst());
        AndroidUiDriver.Bounds cardFace = ui.boundsForText(xml, "A♥");
        AndroidUiDriver.Bounds board = ui.boundsForDescription(xml, "Playing surface");
        AndroidUiDriver.Bounds currentSeat =
                ui.boundsForDescriptionContaining(xml, "Current turn.");
        AndroidUiDriver.Bounds trump = ui.boundsForDescriptionContaining(xml, "Trump");
        List<String> initialSeatDescriptions = seatDescriptions(ui, xml);
        double density = profile.density() / 160.0;
        int playedCardFaceHeightDp = profile.playedCardFaceHeightDp();
        int playedCardFaceHeight = (int) Math.round(playedCardFaceHeightDp * density);
        boolean faceHeightReserved = height(cardFace) >= playedCardFaceHeight;
        boolean cardInsideTable = contains(board, cardSlot);
        boolean centered = Math.abs(centerX(cardSlot) - centerX(board)) <= width(board) * 0.12;
        List<Path> styleCapture = ui.capture(profile.id() + "-responsive-played-card-style");
        evidence.addAll(styleCapture);
        Path screenshot =
                styleCapture.stream()
                        .filter(path -> path.toString().endsWith(".png"))
                        .findFirst()
                        .orElseThrow();
        BufferedImage image = ImageIO.read(screenshot.toFile());
        int seatFill = sampleFrameFill(image, currentSeat, profile);
        int trumpFill = sampleFrameFill(image, trump, profile);
        boolean trumpMatchesCurrentSeat = seatFill == trumpFill;
        int trumpHeadingInk = darkTextPixels(image, ui.boundsForText(xml, "TRUMP"), profile);
        int trumpSuitInk = darkTextPixels(image, ui.boundsForText(xml, "♠"), profile);
        int trumpStatusInk =
                darkTextPixels(image, ui.boundsForText(xml, "NO TURNED CARD"), profile);
        List<AtfAssertion> styleChecks =
                new ArrayList<>(
                        List.of(
                                check(
                                        "Played-card slot reserves its responsive face height",
                                        playedCardFaceHeightDp
                                                + " dp / "
                                                + playedCardFaceHeight
                                                + " px minimum at "
                                                + profile.density()
                                                + " dpi",
                                        height(cardFace) + " px",
                                        faceHeightReserved),
                                check(
                                        "Played card stays centered on the table",
                                        "within 12% of table width",
                                        Math.abs(centerX(cardSlot) - centerX(board)) + " px",
                                        centered),
                                check(
                                        "Played card remains inside the table surface",
                                        "contained",
                                        cardSlot,
                                        cardInsideTable),
                                check(
                                        "Trump frame matches the current player's frame fill",
                                        "same rendered color",
                                        String.format(
                                                "seat #%06X, trump #%06X", seatFill, trumpFill),
                                        trumpMatchesCurrentSeat),
                                check(
                                        "Active trump heading has readable contrast",
                                        "at least 3 dark text pixels",
                                        trumpHeadingInk,
                                        trumpHeadingInk >= 3),
                                check(
                                        "Active trump suit has readable contrast",
                                        "at least 3 dark suit pixels",
                                        trumpSuitInk,
                                        trumpSuitInk >= 3),
                                check(
                                        "Active trump status has readable contrast",
                                        "at least 3 dark text pixels",
                                        trumpStatusInk,
                                        trumpStatusInk >= 3)));
        checkCaptureSize(adb, styleCapture, profile, styleChecks);
        publishCheckpoint(
                ui,
                trace,
                profile,
                "Inspect responsive card height and matching active trump frame",
                styleCapture,
                styleChecks);

        xml = revealSeatCross(adb, ui, playedCards.getFirst(), profile, xml);
        cardSlot = ui.boundsForDescription(xml, playedCards.getFirst());
        cardFace = ui.boundsForText(xml, "A♥");
        board = ui.boundsForDescription(xml, "Playing surface");
        List<String> visibleSeatDescriptions = seatDescriptions(ui, xml);
        List<AndroidUiDriver.Bounds> seats = seatBounds(ui, xml);
        boolean fullFaceStillVisible = height(cardFace) >= playedCardFaceHeight;
        int fullCardRowHeight = playedCardFaceHeightDp + profile.playedCardLabelAndPaddingDp();
        boolean completeCardRowVisible =
                height(cardSlot) >= (int) Math.round(fullCardRowHeight * density);
        boolean humanSeatVisible =
                visibleSeatDescriptions.stream()
                        .anyMatch(description -> description.startsWith("You. Bid "));
        java.util.Set<String> observedPlayers =
                new java.util.LinkedHashSet<>(initialSeatDescriptions);
        observedPlayers.addAll(visibleSeatDescriptions);
        boolean allSeatsObserved = observedPlayers.size() == 4 && humanSeatVisible;
        AndroidUiDriver.Bounds auditedCardSlot = cardSlot;
        int overlapCount =
                (int) seats.stream().filter(seat -> overlaps(auditedCardSlot, seat)).count();
        boolean clearOfSeats = humanSeatVisible && !seats.isEmpty() && overlapCount == 0;
        AndroidUiDriver.Bounds humanSeat =
                ui.boundsForDescription(
                        xml,
                        visibleSeatDescriptions.stream()
                                .filter(description -> description.startsWith("You. Bid "))
                                .findFirst()
                                .orElseThrow());
        int horizontalOffset = Math.abs(centerX(cardSlot) - centerX(humanSeat));
        boolean alignedWithHuman = horizontalOffset <= width(humanSeat) * 0.15;
        List<Path> seatCapture = ui.capture(profile.id() + "-seat-oriented-card-clearance");
        evidence.addAll(seatCapture);
        List<AtfAssertion> seatChecks =
                new ArrayList<>(
                        List.of(
                                check(
                                        "Complete responsive-height card remains visible beside the player cross",
                                        playedCardFaceHeight + " px minimum",
                                        height(cardFace) + " px",
                                        fullFaceStillVisible),
                                check(
                                        "Card face and player label are fully inside the table viewport",
                                        (int) Math.round(fullCardRowHeight * density)
                                                + " px full row",
                                        height(cardSlot) + " px",
                                        completeCardRowVisible),
                                check(
                                        "All four player frames are accounted for across the scrollable table",
                                        "4 player frames",
                                        observedPlayers.size() + "/4 unique seats including You",
                                        allSeatsObserved),
                                check(
                                        "The bottom card remains aligned with the human seat",
                                        "same horizontal center",
                                        "center offset="
                                                + horizontalOffset
                                                + " px; human seat visible="
                                                + humanSeatVisible,
                                        humanSeatVisible && alignedWithHuman),
                                check(
                                        "Played card and player labels do not cover a seat frame",
                                        "no rectangle intersections",
                                        "visible seats="
                                                + seats.size()
                                                + "; overlapping frames="
                                                + overlapCount,
                                        clearOfSeats),
                                check(
                                        "Played card remains within the visible table surface",
                                        "contained",
                                        cardSlot,
                                        contains(board, auditedCardSlot))));
        checkCaptureSize(adb, seatCapture, profile, seatChecks);
        publishCheckpoint(
                ui,
                trace,
                profile,
                "Audit card clearance around all four player frames",
                seatCapture,
                seatChecks);
    }
}
