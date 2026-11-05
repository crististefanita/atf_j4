package ro.whistlab.app.seat;

import static org.junit.Assert.*;
import org.junit.Test;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.core.rules.HowToPlay;

/** Inject glyph extents: this verifies the layout contract, not a screenshot or font renderer. */
public final class SeatHeadingMetricsComponentTest {
    @Test public void completeLocalizedActionsFitInlineOrReceiveTheWholeNextLine() {
        for (var language : HowToPlay.Language.values()) for (boolean bidding : new boolean[]{true, false}) {
            String action = new WhistUiText(language).seatTurnBadge(bidding);
            assertFalse(action.isBlank());
            for (int seatWidth : new int[]{60, 70, 78, 88, 102, 116}) {
                for (float density : new float[]{1, 1.5f, 2.625f, 2.75f, 3.5f}) {
                    for (float fontScale : new float[]{1, 1.3f, 1.5f, 2}) {
                        int width = Math.round((seatWidth - 12) * density);
                        // Conservative mock extents deliberately include unusually wide glyphs.
                        int actionWidth = (int) Math.ceil(action.length() * 6 * fontScale * density + 8 * density);
                        int nameWidth = (int) Math.ceil(8 * 8 * fontScale * density);
                        var plan = SeatHeadingMetrics.arrange(width, nameWidth, actionWidth,
                                Math.round(4 * density), Math.round(28 * density));
                        if (plan.inline()) {
                            assertTrue(plan.badgeWidthPx() >= actionWidth);
                            assertEquals(width, plan.nameWidthPx() + plan.gapPx() + plan.badgeWidthPx());
                            assertTrue(plan.nameWidthPx() >= Math.round(28 * density));
                        } else {
                            assertEquals(width, plan.badgeWidthPx());
                            assertEquals(width, plan.nameWidthPx());
                            assertEquals(0, plan.gapPx());
                        }
                    }
                }
            }
        }
    }

    @Test public void aLongActionIsNeverTruncatedIntoTheOldFixedBadgeColumn() {
        var narrow = SeatHeadingMetrics.arrange(58, 44, 66, 4, 28);
        assertFalse(narrow.inline());
        assertEquals(58, narrow.badgeWidthPx());
        var wide = SeatHeadingMetrics.arrange(104, 44, 56, 4, 28);
        assertTrue(wide.inline());
        assertEquals(56, wide.badgeWidthPx());
    }

    @Test public void boundariesUseTheSameSpaceWhileTheActivePlayerChanges() {
        assertTrue(SeatHeadingMetrics.arrange(88, 80, 56, 4, 28).inline());
        assertFalse(SeatHeadingMetrics.arrange(87, 80, 56, 4, 28).inline());
        assertThrows(IllegalArgumentException.class, () -> SeatHeadingMetrics.arrange(0, 40, 50, 4, 28));
    }
}
