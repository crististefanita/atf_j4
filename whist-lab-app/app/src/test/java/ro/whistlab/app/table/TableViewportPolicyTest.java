package ro.whistlab.app.table;

import ro.whistlab.app.bidding.BidGridMetrics;
import ro.whistlab.app.hand.HandLayoutMetrics;

import static org.junit.Assert.*;
import org.junit.Test;

public class TableViewportPolicyTest {
    @Test public void shortWindowsReclaimDecorationAndSlightlyCompactOnlyThePlayedCards() {
        var compact = new TableViewportPolicy(497);
        assertEquals(3, compact.regionGapDp());
        assertEquals(0, compact.minimumBoardHeightDp());
        assertFalse(compact.showPlayHint());
        assertEquals(28, compact.handClearanceDp(false));
        assertEquals(12, compact.handClearanceDp(true));
        assertEquals(1, compact.seatVerticalPaddingDp());
        assertEquals(2, compact.outerVerticalPaddingDp());
        assertEquals(1, compact.turnBadgeVerticalPaddingDp());
        assertEquals(14, compact.turnBadgeSlotHeightDp());
        assertEquals(94, compact.playedCardFaceHeightDp());
        assertEquals(2, compact.playedCardItemVerticalPaddingDp());
        assertEquals(98, HandLayoutMetrics.cardHeightDp());
        assertEquals(48, BidGridMetrics.CHOICE_TARGET_DP);
        var roomy = new TableViewportPolicy(700);
        assertTrue(roomy.showPlayHint());
        assertEquals(4, roomy.seatVerticalPaddingDp());
        assertEquals(4, roomy.outerVerticalPaddingDp());
        assertEquals(104, roomy.playedCardFaceHeightDp());
        assertEquals(TrickCardMetrics.ITEM_VERTICAL_PADDING_DP, roomy.playedCardItemVerticalPaddingDp());
    }

    @Test public void bidZoneUsesTheLowerPartOfTheAvailableSafeBand() {
        for (int width : new int[]{280, 320, 353, 560}) {
            int seatWidth = TableLayoutMetrics.seatWidthDp(width);
            int columns = BidGridMetrics.columnCount(9, width);
            int centerWidth = Math.max(132, Math.min(9, columns) * 52);
            int centerHeight = BidGridMetrics.rowCount(9, width) * BidGridMetrics.CHOICE_ROW_DP
                    + BidGridMetrics.SUBMIT_TARGET_DP;
            int gap = 3;
            int seatHeight = 62;
            int minimum = TableGeometry.minimumHeight(4, seatHeight, centerHeight, gap);
            for (int extra : new int[]{0, 40, 120}) {
                var plan = TableGeometry.arrange(width, minimum + extra, 4, seatWidth,
                        seatHeight, centerWidth, centerHeight, gap, false,
                        TableGeometry.CenterAlignment.TOWARD_HUMAN);
                var human = plan.seats().getFirst();
                int humanGap = human.top() - plan.center().bottom();
                int upperGap = plan.seats().stream().skip(1)
                        .filter(seat -> seat.bottom() <= plan.center().top())
                        .mapToInt(seat -> plan.center().top() - seat.bottom())
                        .min().orElseThrow();
                assertTrue("Bids must sit closer to the human seat at width=" + width
                                + ", extra=" + extra + ": upper=" + upperGap + ", human=" + humanGap,
                        humanGap < upperGap);
                assertTrue("The measured bid area must remain clear of every seat",
                        plan.seats().stream().noneMatch(plan.center()::overlaps));
            }
        }
    }

    @Test public void pixelRoundingDoesNotChangeTheFourSeatPlayingRing() {
        for (int width : new int[]{280, 313, 353, 560, 760}) for (float density : new float[]{1, 1.5f, 2, 2.25f, 2.75f, 3.5f}) {
            int w = Math.round(width * density), seat = Math.round(TableLayoutMetrics.seatWidthDp(width) * density);
            var plan = TableGeometry.arrange(w, Math.round(400 * density), 4, seat, Math.round(70 * density),
                    Math.round(TableLayoutMetrics.centerPlayWidthDp(width) * density), Math.round(144 * density), Math.round(8 * density));
            assertEquals("west/east must stay centered at " + width + "@" + density,
                    Math.round(400 * density) / 2.0, (plan.seats().get(1).top() + plan.seats().get(1).bottom()) / 2.0, 0.5);
        }
    }
}
