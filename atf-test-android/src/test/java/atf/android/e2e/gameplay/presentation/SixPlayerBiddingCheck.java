package atf.android.e2e.gameplay.presentation;

import static atf.android.e2e.display.support.GameplayDisplay.effectiveDensityScale;
import static atf.android.e2e.display.support.GameplayGeometry.overlaps;
import static atf.android.e2e.display.support.ScreenGeometry.centerX;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

/** Verifică mâna, licitațiile și geometria mesei înaintea navigării. */
final class SixPlayerBiddingCheck {
    private SixPlayerBiddingCheck() {}

    private static final Pattern SIX_PLAYER_EIGHT_CARD_BID_SUMMARY =
            Pattern.compile("Bids submitted (\\d+)/6\\RBid total (-?\\d+)/8");

    record BiddingState(String initialBidSummary, Set<Integer> expectedBids) {
        BiddingState {
            expectedBids = Set.copyOf(expectedBids);
        }
    }

    static BiddingState run(AdbClient adb, AndroidUiDriver ui, AndroidUiFlowTrace trace, List<Path> evidence,
            String setup) throws Exception {
        long started = System.nanoTime();
        ui.tapTextIn(setup, "Start game");
        String bidding = ui.awaitText("Place bid", Duration.ofSeconds(15));
        String initialBidSummary =
                ui.texts(bidding).stream()
                        .filter(label -> label.startsWith("Bids submitted "))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Bid summary was not visible before opening the score sheet"));
        List<AndroidUiDriver.CardLabel> handRow = ui.bottomRowCardLabels(bidding);
        List<String> handLabels =
                handRow.stream().map(AndroidUiDriver.CardLabel::text).toList();
        boolean oneRow =
                handRow.size() == 8
                        && handRow.stream()
                                        .map(AndroidUiDriver.CardLabel::top)
                                        .distinct()
                                        .count()
                                == 1;
        Matcher bidSummaryMatch = SIX_PLAYER_EIGHT_CARD_BID_SUMMARY.matcher(initialBidSummary);
        assertTrue(
                bidSummaryMatch.matches(), "Unexpected bidding summary: " + initialBidSummary);
        int submittedBids = Integer.parseInt(bidSummaryMatch.group(1));
        int currentBidTotal = Integer.parseInt(bidSummaryMatch.group(2));
        LinkedHashSet<Integer> expectedBids =
                new LinkedHashSet<>(
                        java.util.stream.IntStream.rangeClosed(0, 8).boxed().toList());
        if (submittedBids == 5) {
            int forbiddenFinalBid = 8 - currentBidTotal;
            if (forbiddenFinalBid >= 0 && forbiddenFinalBid <= 8)
                expectedBids.remove(forbiddenFinalBid);
        }
        List<Integer> visibleBids = ui.numericButtonValues(bidding);
        boolean allBidsVisible =
                visibleBids.size() == expectedBids.size()
                        && new LinkedHashSet<>(visibleBids).equals(expectedBids);
        List<AndroidUiDriver.ElementSize> bidSizes = ui.numericButtonSizes(bidding);
        double densityScale = effectiveDensityScale(adb.run("shell", "wm", "density").output());
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
        boolean touchTargetsRemain48Dp =
                bidSizes.size() == expectedBids.size()
                        && minimumBidWidth >= Math.round(48 * densityScale * 0.95)
                        && minimumBidHeight >= Math.round(48 * densityScale * 0.95);
        AndroidUiDriver.ElementSize submitBidSize = ui.buttonSize(bidding, "Place bid");
        boolean submitBidIsDiscreetAndAccessible =
                Math.abs(submitBidSize.widthPixels() - Math.round(119 * densityScale)) <= 1
                        && submitBidSize.heightPixels() >= Math.round(48 * densityScale * 0.95);
        AndroidUiDriver.Bounds submitBounds = ui.boundsForText(bidding, "Place bid");
        AndroidUiDriver.Bounds handBounds =
                ui.boundsForDescription(bidding, "Your hand, 8 cards.");
        boolean actionAnchoredAboveHand = submitBounds.bottom() < handBounds.top();
        boolean turnedTrumpCardVisible = ui.turnedTrumpIdentityMatchesVisibleCard(bidding);
        boolean gameIdentityAndTrumpVisible =
                ui.hasText(bidding, "Whist · Oh Hell") && turnedTrumpCardVisible;
        var boardBounds = ui.boundsForDescription(bidding, "Playing surface");
        List<AndroidUiDriver.Bounds> seatBounds =
                ui.descriptions(bidding).stream()
                        .filter(value -> value.contains(". Bid ") && value.contains(". Score "))
                        .map(
                                description -> {
                                    try {
                                        return ui.boundsForDescription(bidding, description);
                                    } catch (Exception failure) {
                                        throw new IllegalStateException(failure);
                                    }
                                })
                        .toList();
        List<AndroidUiDriver.Bounds> biddingControlBounds = new ArrayList<>();
        for (int bid : visibleBids)
            biddingControlBounds.add(ui.buttonBounds(bidding, Integer.toString(bid)));
        biddingControlBounds.add(submitBounds);
        long biddingSeatOverlaps =
                biddingControlBounds.stream()
                        .flatMap(
                                control ->
                                        seatBounds.stream()
                                                .filter(seat -> overlaps(control, seat)))
                        .count();
        boolean biddingControlsAvoidSeats = biddingSeatOverlaps == 0;
        List<String> seatPositions =
                seatBounds.stream()
                        .map(
                                bounds ->
                                        String.format(
                                                java.util.Locale.ROOT,
                                                "x=%.2f,y=%.2f",
                                                (centerX(bounds) - boardBounds.left())
                                                        / (double) width(boardBounds),
                                                (bounds.top() - boardBounds.top())
                                                        / (double) height(boardBounds)))
                        .toList();
        double tableCenterX = boardBounds.left() + width(boardBounds) / 2.0;
        AndroidUiDriver.Bounds humanSeat = ui.boundsForDescriptionContaining(bidding, "You. ");
        boolean humanSeatIsSouth =
                Math.abs(centerX(humanSeat) - tableCenterX) <= width(boardBounds) * 0.16
                        && (humanSeat.top() - boardBounds.top()) >= height(boardBounds) * 0.78;
        long northSeats =
                seatBounds.stream()
                        .filter(
                                bounds ->
                                        (bounds.top() - boardBounds.top())
                                                        <= height(boardBounds) * 0.12
                                                && Math.abs(centerX(bounds) - tableCenterX)
                                                        <= width(boardBounds) * 0.16)
                        .count();
        long upperFlankSeats =
                seatBounds.stream()
                        .filter(
                                bounds -> {
                                    double y =
                                            (bounds.top() - boardBounds.top())
                                                    / (double) height(boardBounds);
                                    double x =
                                            (centerX(bounds) - boardBounds.left())
                                                    / (double) width(boardBounds);
                                    return y >= 0.13 && y <= 0.40 && (x <= 0.32 || x >= 0.68);
                                })
                        .count();
        long lowerFlankSeats =
                seatBounds.stream()
                        .filter(
                                bounds -> {
                                    double y =
                                            (bounds.top() - boardBounds.top())
                                                    / (double) height(boardBounds);
                                    double x =
                                            (centerX(bounds) - boardBounds.left())
                                                    / (double) width(boardBounds);
                                    return y >= 0.55 && y <= 0.82 && (x <= 0.32 || x >= 0.68);
                                })
                        .count();
        boolean mockSeatRingRendered =
                seatBounds.size() == 6
                        && humanSeatIsSouth
                        && northSeats == 1
                        && upperFlankSeats == 2
                        && lowerFlankSeats == 2;
        assertTrue(
                oneRow,
                "Expected all eight hand cards on one row; visible bottom-row cards="
                        + handLabels);
        assertTrue(
                allBidsVisible,
                "Visible choices differ from the legal bid set "
                        + expectedBids
                        + "; actual="
                        + visibleBids);
        assertTrue(
                touchTargetsRemain48Dp,
                "Bid choices fell below 48 x 48 dp touch bounds: " + bidSizes);
        assertTrue(
                submitBidIsDiscreetAndAccessible,
                "Place bid should stay compact (119 dp wide) with a 48 dp touch height: "
                        + submitBidSize);
        assertTrue(
                actionAnchoredAboveHand,
                "Place bid should sit inside the table above the fixed hand row: submit="
                        + submitBounds
                        + "; hand="
                        + handBounds);
        assertTrue(
                gameIdentityAndTrumpVisible,
                "Variant title and the persistent trump tile must remain visible: "
                        + ui.texts(bidding)
                        + "; descriptions="
                        + ui.descriptions(bidding));
        assertTrue(
                mockSeatRingRendered,
                "Six player seats must match the mock's north, upper flanks, lower flanks, and south positions: "
                        + seatPositions
                        + "; human south="
                        + humanSeatIsSouth);
        assertTrue(
                biddingControlsAvoidSeats,
                "Bidding controls overlap a player seat: controls="
                        + biddingControlBounds
                        + "; seats="
                        + seatBounds);
        List<Path> biddingEvidence = ui.capture("whist-lab-presentation-eight-card-table");
        evidence.addAll(biddingEvidence);
        Path biddingScreenshot =
                biddingEvidence.stream()
                        .filter(path -> path.toString().endsWith(".png"))
                        .findFirst()
                        .orElseThrow();
        String biddingDisplaySize = adb.run("shell", "wm", "size").output();
        boolean feltContinuesBelowTable =
                ui.sameBackgroundAcrossTableMargin(
                        biddingScreenshot, 0.55, 0.87, biddingDisplaySize);
        assertTrue(
                feltContinuesBelowTable,
                "The game surface changes color between the table and space below the action");
        String turnedTrumpDescription =
                ui.descriptions(bidding).stream()
                        .filter(value -> value.startsWith("Trump set by turned card: "))
                        .findFirst()
                        .orElse("<missing>");
        String visibleVariant =
                ui.texts(bidding).stream()
                        .filter(value -> value.startsWith("Whist · "))
                        .findFirst()
                        .orElse("<missing>");
        checkpoint(
                trace,
                "TEST BODY",
                AtfGherkinKeyword.THEN,
                "Render all eight cards and the complete compact bidding grid",
                "Six-player Oh Hell bidding table",
                biddingEvidence,
                "inspect all hand labels, legal bid values, and button bounds",
                "eight cards share one row; legal bids and 48 dp targets remain visible; identity and trump remain in the header; Place bid sits in the table above the hand",
                "hand="
                        + handLabels
                        + "; expected bids="
                        + expectedBids
                        + "; actual bids="
                        + visibleBids
                        + "; bid button bounds px="
                        + bidSizes
                        + "; Place bid bounds px="
                        + submitBidSize
                        + "; table action bounds="
                        + submitBounds
                        + "; hand bounds="
                        + handBounds
                        + "; identity and trump visible="
                        + gameIdentityAndTrumpVisible
                        + "; continuous felt background="
                        + feltContinuesBelowTable
                        + "; visible labels="
                        + ui.texts(bidding),
                Duration.ofNanos(System.nanoTime() - started).toMillis(),
                List.of(
                        new AtfAssertion(
                                "All eight hand cards occupy one row",
                                "8 cards; 1 row",
                                handLabels.size()
                                        + " cards; rows="
                                        + handRow.stream()
                                                .map(AndroidUiDriver.CardLabel::top)
                                                .distinct()
                                                .count(),
                                oneRow),
                        new AtfAssertion(
                                "Six player seats follow the approved oval",
                                "N; NW/NE; SW/SE; S",
                                "seats="
                                        + seatBounds.size()
                                        + "; N="
                                        + northSeats
                                        + "; upper="
                                        + upperFlankSeats
                                        + "; lower="
                                        + lowerFlankSeats
                                        + "; south="
                                        + humanSeatIsSouth,
                                mockSeatRingRendered),
                        new AtfAssertion(
                                "Bidding controls stay clear of player seats",
                                "0 intersections (1 px bounds tolerance)",
                                "intersections=" + biddingSeatOverlaps,
                                biddingControlsAvoidSeats),
                        new AtfAssertion(
                                "All currently legal bids are visible",
                                expectedBids.toString(),
                                visibleBids.toString(),
                                allBidsVisible),
                        new AtfAssertion(
                                "Bid hit targets remain at least 48 dp",
                                "48 dp minimum",
                                "minimum="
                                        + minimumBidWidth
                                        + "x"
                                        + minimumBidHeight
                                        + " px; density="
                                        + String.format(
                                                java.util.Locale.ROOT, "%.2f", densityScale),
                                touchTargetsRemain48Dp),
                        new AtfAssertion(
                                "Place bid is compact without shrinking its hit target",
                                "119 dp wide; >=48 dp high",
                                submitBidSize.toString() + " px at density " + densityScale,
                                submitBidIsDiscreetAndAccessible),
                        new AtfAssertion(
                                "Place bid remains inside the table above the hand",
                                "submit bottom < hand top",
                                submitBounds.bottom() + " < " + handBounds.top(),
                                actionAnchoredAboveHand),
                        new AtfAssertion(
                                "Variant identity and trump remain visible in the header",
                                "Whist · Oh Hell; turned card rank and suit",
                                visibleVariant + "; " + turnedTrumpDescription,
                                gameIdentityAndTrumpVisible),
                        new AtfAssertion(
                                "Trump header distinguishes the actual turned card",
                                "Visible rank and suit match full accessible card identity",
                                "matching face="
                                        + turnedTrumpCardVisible
                                        + "; "
                                        + turnedTrumpDescription,
                                turnedTrumpCardVisible),
                        new AtfAssertion(
                                "Felt remains continuous from the table frame to the lower viewport",
                                "same table-margin color at 55% and 87% height",
                                Boolean.toString(feltContinuesBelowTable),
                                feltContinuesBelowTable)),
                AtfReportStatus.PASS);

        return new BiddingState(initialBidSummary, expectedBids);
    }
}
