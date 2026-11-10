package ro.whistlab.app.table;

import org.junit.Test;
import ro.whistlab.app.bidding.BidGridMetrics;
import static atf.gradle.audit.assertions.GradleAssertions.*;

/** A hand keeps the same measured table anchors while the center changes from bid to trick. */
public final class GameTablePhaseGeometryComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void seatsStayAtIdenticalCoordinatesFromBiddingThroughPlay() {
        int[] widths = {280, 313, 353, 560, 760};
        int[] handSizes = {1, 2, 4, 8, 13};
        float[] fontScales = {1f, 1.3f, 1.5f, 2f};
        for (int players = 3; players <= 6; players++) for (int width : widths)
            for (int handSize : handSizes) for (float fontScale : fontScales) {
                var viewport = new TableViewportPolicy(569, fontScale);
                var reservation = TableLayoutMetrics.centerReservation(players, handSize, width,
                        fontScale, viewport);
                var bids = BidGridMetrics.actionLayout(handSize + 1, width, fontScale);
                var trick = TrickCardMetrics.forTable(players, width, fontScale, viewport);
                int seatWidth = ro.cardtable.ui.table.TableSizing.seatWidthDp(width, viewport.isCompact());
                int seatHeight = 82;
                int gap = viewport.regionGapDp();
                int height = TableGeometry.minimumHeight(players, seatHeight,
                        reservation.reservedHeightDp(), gap) + 120;
                var bidding = TableGeometry.arrangeDisplay(width, height, players, seatWidth, seatHeight,
                        bids.centerWidthDp(), reservation.reservedHeightDp(), gap, true,
                        TableGeometry.CenterAlignment.TOWARD_HUMAN);
                var playing = TableGeometry.arrangeDisplay(width, height, players, seatWidth, seatHeight,
                        trick.safeCenterWidthDp(), reservation.reservedHeightDp(), gap, true,
                        TableGeometry.CenterAlignment.BALANCED);

                atf.gradle.audit.GradleInputs.fixture("One viewport and one reserved band across phases",
                        "players", players, "width", width, "hand size", handSize,
                        "font scale", fontScale, "bidding center", bids, "trick center", trick,
                        "reservation", reservation, "bidding plan", bidding, "playing plan", playing);
                assertEquals("The full ordered seat rectangles remain identical during the hand",
                        playing.seats(), bidding.seats());
                assertTrue("The reservation covers bidding and playing controls",
                        reservation.reservedHeightDp() >= bids.centerHeightDp()
                                && reservation.reservedHeightDp() >= trick.centerHeightDp());
                assertTrue("The bid area stays clear of every seat",
                        bidding.seats().stream().noneMatch(bidding.center()::overlaps));
                assertTrue("The trick area stays clear of every seat",
                        playing.seats().stream().noneMatch(playing.center()::overlaps));
            }
    }

    @Test public void aHandReservesTheLargerOfItsMaximumBidGridAndTrickBand() {
        for (int players = 3; players <= 6; players++) for (int handSize : new int[]{1, 8, 13}) {
            int width = 353;
            var viewport = new TableViewportPolicy(569);
            var reservation = TableLayoutMetrics.centerReservation(players, handSize, width, 1f, viewport);
            int expectedBid = BidGridMetrics.actionLayout(handSize + 1, width, 1f).centerHeightDp();
            int expectedPlay = TrickCardMetrics.forTable(players, width, 1f, viewport).centerHeightDp();
            atf.gradle.audit.GradleInputs.fixture("Phase-independent center reservation",
                    "players", players, "hand size", handSize, "reservation", reservation,
                    "expected bid height", expectedBid, "expected play height", expectedPlay);
            assertEquals("The reserved band is the larger measured phase", Math.max(expectedBid, expectedPlay),
                    reservation.reservedHeightDp());
            assertEquals("The bid band's source measurement remains explicit", expectedBid,
                    reservation.biddingHeightDp());
            assertEquals("The play band's source measurement remains explicit", expectedPlay,
                    reservation.playingHeightDp());
        }
    }
}
