package ro.whistlab.app;

import static org.junit.Assert.*;
import org.junit.Test;

public class TableViewportPolicyTest {
    @Test public void shortWindowsReclaimSpaceWithoutReducingCardsOrPlayLiftClearance() {
        var compact = new TableViewportPolicy(497);
        assertEquals(3, compact.regionGapDp());
        assertEquals(0, compact.minimumBoardHeightDp());
        assertFalse(compact.showPlayHint());
        assertEquals(28, compact.handClearanceDp(false));
        assertEquals(12, compact.handClearanceDp(true));
        assertEquals(98, HandLayoutMetrics.cardHeightDp());
        assertEquals(48, BidGridMetrics.CHOICE_TARGET_DP);
        assertTrue(new TableViewportPolicy(700).showPlayHint());
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
