package atf.android.e2e.gameplay.presentation;

import static atf.android.e2e.display.support.GameplayDisplay.effectiveDensityScale;
import static atf.android.e2e.display.support.GameplayDisplay.effectiveDisplaySize;
import static atf.android.e2e.display.support.GameplayDisplay.overrideDisplay;
import static atf.android.e2e.display.support.GameplayGeometry.overlaps;
import static atf.android.e2e.support.flow.UiFlowCheckpoint.checkpoint;
import static atf.android.e2e.support.flow.WhistLabScenario.PACKAGE;
import static org.junit.jupiter.api.Assertions.assertTrue;
import atf.android.AdbClient;
import atf.android.e2e.display.support.GameplayDisplay.DisplayProfile;
import atf.android.e2e.display.support.ScreenGeometry;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Verifică o singură configurație de licitare cu treisprezece cărți. */
final class ThirteenCardBiddingCheck {
    private ThirteenCardBiddingCheck() {}

    static void run(AdbClient adb, AndroidUiDriver ui, AndroidUiFlowTrace trace, List<Path> evidence,
            DisplayProfile profile, int index, int physicalCaptureWidth, int physicalCaptureHeight) throws Exception {
        long started = System.nanoTime();
        overrideDisplay(adb, ui, profile);
        String home =
                ui.restartToActivity(
                        PACKAGE, ".MainActivity", "--es", "debug_language", "EN");
        assertTrue(ui.hasText(home, "New game"), "Home did not open at " + profile.name());
        ui.tapText("New game");
        ui.awaitText("Players", Duration.ofSeconds(12));
        ui.selectNamedSpinnerOption("player-count", "4");
        ui.selectNamedSpinnerOption("game-type", "Descending");
        String setup = ui.awaitText("Players", Duration.ofSeconds(10));
        String selectedPlayers = ui.spinnerValue(setup, "Players");
        String selectedMode = ui.spinnerValue(setup, "Game type");
        assertTrue(
                selectedPlayers.equals("4") && selectedMode.equals("Descending"),
                "The 13-card deal requires 4 players and Descending at "
                        + profile.name()
                        + "; observed "
                        + selectedPlayers
                        + "/"
                        + selectedMode);
        ui.revealText("Start game");
        setup = ui.awaitText("Start game", Duration.ofSeconds(10));
        ui.tapText("Start game");
        ui.awaitText("Place bid", Duration.ofSeconds(20));
        String bidding =
                ui.awaitDescriptionContaining(
                        "Your hand, 13 cards.", Duration.ofSeconds(10));

        AndroidUiDriver.Bounds handBounds =
                ui.boundsForDescription(
                        bidding,
                        "Your hand, 13 cards.",
                        "android.widget.HorizontalScrollView");
        double densityScale =
                effectiveDensityScale(adb.run("shell", "wm", "density").output());
        List<AndroidUiDriver.CardLabel> cardLabels = ui.bottomRowCardLabels(bidding);
        List<AndroidUiDriver.Bounds> cardBounds = ui.handCardFaceBounds(bidding);
        long handRows =
                cardLabels.stream().map(AndroidUiDriver.CardLabel::top).distinct().count();
        boolean fullHandVisible =
                cardLabels.size() == 13
                        && cardBounds.size() == 13
                        && cardBounds.stream()
                                .allMatch(
                                        card ->
                                                card.left() >= handBounds.left()
                                                        && card.right()
                                                                <= handBounds.right()
                                                        && card.top() >= handBounds.top()
                                                        && card.bottom()
                                                                <= handBounds.bottom());
        int minimumCardFaceHeight =
                cardBounds.stream().mapToInt(ScreenGeometry::height).min().orElse(0);
        boolean fullCardFacesVisible =
                cardBounds.size() == 13
                        && minimumCardFaceHeight >= Math.round(98 * densityScale * 0.95);
        boolean handIsOneRow =
                cardLabels.size() == 13
                        && handRows == 1
                        && fullHandVisible
                        && fullCardFacesVisible;

        List<Integer> expectedBids =
                java.util.stream.IntStream.rangeClosed(0, 13).boxed().toList();
        List<Integer> visibleBids = ui.numericButtonValues(bidding);
        boolean allBidsVisible =
                visibleBids.size() == expectedBids.size()
                        && new LinkedHashSet<>(visibleBids)
                                .equals(new LinkedHashSet<>(expectedBids));
        List<AndroidUiDriver.Bounds> bidBounds = new ArrayList<>();
        List<AndroidUiDriver.ElementSize> bidSizes = ui.numericButtonSizes(bidding);
        for (int bid : visibleBids)
            bidBounds.add(ui.buttonBounds(bidding, Integer.toString(bid)));
        AndroidUiDriver.ElementSize submitSize = ui.buttonSize(bidding, "Place bid");
        AndroidUiDriver.Bounds submitBounds = ui.boundsForText(bidding, "Place bid");
        AndroidUiDriver.Bounds boardBounds =
                ui.boundsForDescription(bidding, "Playing surface");
        int minimumBidWidth =
                bidSizes.stream()
                        .mapToInt(AndroidUiDriver.ElementSize::widthPixels)
                        .min()
                        .orElse(0);
        int minimumBidHeight =
                bidSizes.stream()
                        .mapToInt(AndroidUiDriver.ElementSize::heightPixels)
                        .min()
                        .orElse(0);
        boolean bidTargetsRemainAccessible =
                bidSizes.size() == 14
                        && minimumBidWidth >= Math.round(48 * densityScale * 0.95)
                        && minimumBidHeight >= Math.round(48 * densityScale * 0.95);
        boolean submitRemainsCompact =
                Math.abs(submitSize.widthPixels() - Math.round(119 * densityScale)) <= 1
                        && submitSize.heightPixels()
                                >= Math.round(48 * densityScale * 0.95);
        AndroidUiDriver.Bounds humanSeat =
                ui.boundsForDescriptionContaining(bidding, "You. ");
        List<AndroidUiDriver.Bounds> seats =
                ui.descriptions(bidding).stream()
                        .filter(
                                value ->
                                        value.contains(". Bid ")
                                                && value.contains(". Score "))
                        .map(
                                description -> {
                                    try {
                                        return ui.boundsForDescription(
                                                bidding, description);
                                    } catch (Exception failure) {
                                        throw new IllegalStateException(failure);
                                    }
                                })
                        .toList();
        boolean seatsFit =
                seats.size() == 4
                        && seats.stream()
                                .allMatch(
                                        seat ->
                                                seat.left() >= boardBounds.left()
                                                        && seat.right()
                                                                <= boardBounds.right()
                                                        && seat.top() >= boardBounds.top()
                                                        && seat.bottom()
                                                                <= boardBounds.bottom());
        boolean allControlsInsideTable =
                bidBounds.stream()
                                .allMatch(
                                        control ->
                                                control.left() >= boardBounds.left()
                                                        && control.right()
                                                                <= boardBounds.right()
                                                        && control.top()
                                                                >= boardBounds.top()
                                                        && control.bottom()
                                                                <= boardBounds.bottom())
                        && submitBounds.left() >= boardBounds.left()
                        && submitBounds.right() <= boardBounds.right()
                        && submitBounds.top() >= boardBounds.top()
                        && submitBounds.bottom() <= boardBounds.bottom();
        boolean controlsAvoidSeats =
                java.util.stream.Stream.concat(
                                bidBounds.stream(),
                                java.util.stream.Stream.of(submitBounds))
                        .noneMatch(
                                control ->
                                        seats.stream()
                                                .anyMatch(seat -> overlaps(control, seat)));
        boolean submitAboveHand = submitBounds.bottom() < handBounds.top();
        boolean headerVisible =
                ui.hasText(bidding, "Whist · Descending")
                        && ui.hasText(bidding, "TRUMP")
                        && ui.hasText(bidding, "♠")
                        && !ui.hasText(bidding, "NO TURNED CARD")
                        && ui.hasDescriptionContaining(bidding, "Trump suit only, no turned card: spades");
        List<Path> checkpointEvidence =
                ui.capture("whist-lab-thirteen-card-bidding-" + profile.fileKey());
        evidence.addAll(checkpointEvidence);
        Path screenshot =
                checkpointEvidence.stream()
                        .filter(
                                path ->
                                        path.toString()
                                                .toLowerCase(java.util.Locale.ROOT)
                                                .endsWith(".png"))
                        .findFirst()
                        .orElseThrow();
        java.awt.image.BufferedImage captured =
                javax.imageio.ImageIO.read(screenshot.toFile());
        boolean fullResolutionCapture =
                captured != null
                        && physicalCaptureWidth > 0
                        && captured.getWidth() == physicalCaptureWidth
                        && captured.getHeight() == physicalCaptureHeight;
        String displayOutput = adb.run("shell", "wm", "size").output().trim();
        boolean logicalDisplayApplied =
                (profile.width() + "x" + profile.height())
                        .equals(effectiveDisplaySize(displayOutput));
        boolean densityApplied = Math.abs(densityScale * 160 - profile.density()) < 0.01;
        String diagnostic =
                "profile="
                        + profile.name()
                        + "; display="
                        + displayOutput
                        + "; logicalProfileApplied="
                        + logicalDisplayApplied
                        + "; hand labels="
                        + cardLabels
                        + "; hand bounds="
                        + handBounds
                        + "; minimum card-face height="
                        + minimumCardFaceHeight
                        + " px"
                        + "; bid values="
                        + visibleBids
                        + "; bid bounds="
                        + bidBounds
                        + "; submit="
                        + submitBounds
                        + "; seats="
                        + seats
                        + "; table="
                        + boardBounds
                        + "; physicalScreenshot="
                        + (captured == null
                                ? "unreadable"
                                : captured.getWidth() + "x" + captured.getHeight());
        boolean profilePass =
                handIsOneRow
                        && allBidsVisible
                        && bidTargetsRemainAccessible
                        && submitRemainsCompact
                        && submitAboveHand
                        && seatsFit
                        && allControlsInsideTable
                        && controlsAvoidSeats
                        && headerVisible
                        && logicalDisplayApplied
                        && densityApplied
                        && fullResolutionCapture;
        checkpoint(
                trace,
                "TEST BODY",
                index == 0 ? AtfGherkinKeyword.GIVEN : AtfGherkinKeyword.AND,
                "Show and audit the full thirteen-card bidding table at " + profile.name(),
                "Four-player Descending · hand 13 · legal bids 0–13",
                checkpointEvidence,
                "Inspect rendered hand, bid grid, fixed player seats, header, and physical display capture",
                "13 card faces in one visible row; bids 0–13 stay visible; all targets remain usable and within the table",
                diagnostic,
                Duration.ofNanos(System.nanoTime() - started).toMillis(),
                List.of(
                        new AtfAssertion(
                                "All thirteen card faces remain in one visible row",
                                "13 cards; 1 row; complete 98 dp faces inside viewport",
                                cardLabels.size()
                                        + " cards; rows="
                                        + handRows
                                        + "; inside="
                                        + fullHandVisible
                                        + "; min face="
                                        + minimumCardFaceHeight
                                        + " px",
                                handIsOneRow),
                        new AtfAssertion(
                                "The complete bid domain remains visible",
                                "0 through 13",
                                visibleBids.toString(),
                                allBidsVisible),
                        new AtfAssertion(
                                "All bid choices preserve 48 dp hit targets",
                                "minimum 48 dp",
                                minimumBidWidth
                                        + "x"
                                        + minimumBidHeight
                                        + " px; density="
                                        + String.format(
                                                java.util.Locale.ROOT,
                                                "%.2f",
                                                densityScale),
                                bidTargetsRemainAccessible),
                        new AtfAssertion(
                                "Seat and bid geometry stays clear inside the table",
                                "four seats; controls inside bounds; zero overlaps",
                                "seats="
                                        + seats.size()
                                        + "; controlsInside="
                                        + allControlsInsideTable
                                        + "; overlaps="
                                        + (controlsAvoidSeats ? 0 : 1),
                                seatsFit && allControlsInsideTable && controlsAvoidSeats),
                        new AtfAssertion(
                                "Place bid stays compact and above the hand",
                                "119 dp wide; >=48 dp high; above hand",
                                submitSize.widthPixels()
                                        + "x"
                                        + submitSize.heightPixels()
                                        + " px; above="
                                        + submitAboveHand,
                                submitRemainsCompact && submitAboveHand),
                        new AtfAssertion(
                                "Variant and trump stay visible",
                                "Whist · Descending; TRUMP; ♠; full accessible trump identity; no status footer",
                                "variant/trump=" + headerVisible,
                                headerVisible),
                        new AtfAssertion(
                                "Requested logical display and density are effective",
                                profile.width()
                                        + "x"
                                        + profile.height()
                                        + "; "
                                        + profile.density()
                                        + " dpi",
                                effectiveDisplaySize(displayOutput)
                                        + "; "
                                        + Math.round(densityScale * 160)
                                        + " dpi",
                                logicalDisplayApplied && densityApplied),
                        new AtfAssertion(
                                "Report image preserves the complete display resolution",
                                "physical "
                                        + physicalCaptureWidth
                                        + "x"
                                        + physicalCaptureHeight
                                        + "; logical "
                                        + profile.width()
                                        + "x"
                                        + profile.height(),
                                captured == null
                                        ? "unreadable"
                                        : captured.getWidth() + "x" + captured.getHeight(),
                                fullResolutionCapture)),
                profilePass ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
        assertTrue(
                handIsOneRow,
                "13-card bidding hand is clipped, hidden, or wrapped at "
                        + profile.name()
                        + "; labels="
                        + cardLabels
                        + "; card bounds="
                        + cardBounds
                        + "; viewport="
                        + handBounds);
        assertTrue(
                allBidsVisible,
                "The complete 0–13 bid domain is not visible at "
                        + profile.name()
                        + "; actual="
                        + visibleBids);
        assertTrue(
                bidTargetsRemainAccessible,
                "A bid target is below 48 dp at "
                        + profile.name()
                        + "; min="
                        + minimumBidWidth
                        + "x"
                        + minimumBidHeight
                        + " px; density="
                        + densityScale);
        assertTrue(
                submitRemainsCompact && submitAboveHand,
                "Place bid must be compact, accessible, and above the 13-card hand at "
                        + profile.name()
                        + "; bounds="
                        + submitBounds
                        + "; size="
                        + submitSize
                        + "; hand="
                        + handBounds);
        assertTrue(
                seatsFit && allControlsInsideTable && controlsAvoidSeats,
                "13-card bid controls or seats escape/overlap the table at "
                        + profile.name()
                        + "; seats="
                        + seats
                        + "; bids="
                        + bidBounds
                        + "; submit="
                        + submitBounds
                        + "; table="
                        + boardBounds);
        assertTrue(
                headerVisible,
                "Game variant or trump is missing at "
                        + profile.name()
                        + "; labels="
                        + ui.texts(bidding));
        assertTrue(
                logicalDisplayApplied && densityApplied,
                "Requested display profile was not applied: "
                        + profile.name()
                        + "; "
                        + displayOutput
                        + "; density="
                        + densityScale * 160);
        assertTrue(
                fullResolutionCapture,
                "Checkpoint PNG is not full-display for "
                        + profile.name()
                        + "; actual="
                        + (captured == null
                                ? "unreadable"
                                : captured.getWidth() + "x" + captured.getHeight()));
    }
}
