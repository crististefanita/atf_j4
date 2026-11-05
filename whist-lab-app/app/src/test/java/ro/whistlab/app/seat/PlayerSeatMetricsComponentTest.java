package ro.whistlab.app.seat;

import static org.junit.Assert.*;
import org.junit.Test;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.core.rules.HowToPlay;

/** Inject wrapped glyph extents; actual Android font rendering keeps its native sanity check. */
public final class PlayerSeatMetricsComponentTest {
    @Test public void fullLocalizedActionsStayAboveNamesAcrossCommonResolutionsAndFontScales() {
        int[][] profiles = {{720, 1280, 360}, {1080, 1920, 440}, {1080, 2448, 440},
                {1200, 1920, 320}, {1440, 2560, 560}};
        for (int[] profile : profiles) for (int players = 3; players <= 6; players++) {
            float density = profile[2] / 160f;
            boolean compact = profile[1] / density < 760;
            int widthDp = Math.round(profile[0] / density) - 40;
            int seatWidthPx = Math.round(Math.min(116, Math.min(Math.round(widthDp * (compact ? 0.25f : 0.29f)),
                    (widthDp - 16) / 3)) * density);
            for (float fontScale : new float[]{1, 1.3f, 1.5f, 2}) {
                for (var language : HowToPlay.Language.values()) for (boolean bidding : new boolean[]{true, false}) {
                    String action = new WhistUiText(language).seatTurnBadge(bidding);
                    assertFalse(action.isBlank());
                    int glyphExtent = (int) Math.ceil(action.length() * 7 * fontScale * density);
                    int lines = (glyphExtent + seatWidthPx - 1) / seatWidthPx;
                    int measuredBadgeHeight = lines * (int) Math.ceil(13 * fontScale * density);
                    int naturalFrameHeight = (int) Math.ceil(42 * fontScale * density);
                    int nameHeight = (int) Math.ceil(16 * fontScale * density);
                    int headingHeight = compact ? Math.round(20 * density) : nameHeight + Math.round(16 * density);
                    int referenceHeight = PlayerSeatMetrics.referenceFrameHeightPx(
                            naturalFrameHeight, nameHeight, headingHeight);
                    int gap = Math.round(3 * density);
                    var plan = PlayerSeatMetrics.arrange(referenceHeight, measuredBadgeHeight, gap);
                    assertEquals(measuredBadgeHeight, plan.badgeHeightPx());
                    assertEquals("The complete action ends before the framed name begins",
                            plan.badgeHeightPx() + gap, plan.frameTopPx());
                    assertEquals((int) Math.ceil(referenceHeight * 1.25), plan.frameHeightPx());
                    assertEquals(plan.frameTopPx() + plan.frameHeightPx(), plan.heightPx());
                    // The active flag affects visibility only, not these reserved dimensions.
                    assertEquals(plan, PlayerSeatMetrics.arrange(referenceHeight, measuredBadgeHeight, gap));
                }
            }
        }
    }

    @Test public void multiLineActionExpandsItsOwnRegionAndCannotIntersectTheFrame() {
        var one = PlayerSeatMetrics.arrange(43, 13, 3);
        var three = PlayerSeatMetrics.arrange(43, 39, 3);
        assertEquals(one.frameHeightPx(), three.frameHeightPx());
        assertEquals(26, three.frameTopPx() - one.frameTopPx());
        assertEquals(26, three.heightPx() - one.heightPx());
        assertTrue(three.badgeHeightPx() < three.frameTopPx());
    }

    @Test public void repeatedMeasurementUsesFreshContentRatherThanThePreviousEnlargedFrame() {
        assertEquals(60, PlayerSeatMetrics.frameHeightPx(48));
        assertEquals(62, PlayerSeatMetrics.frameHeightPx(49));
        for (int pass = 0; pass < 10; pass++) {
            assertEquals(75, PlayerSeatMetrics.arrange(48, 12, 3).heightPx());
        }
    }

    @Test public void movingTheBadgeOutsideDoesNotShrinkTheReferenceFrameBeforeEnlargingIt() {
        int compact = PlayerSeatMetrics.referenceFrameHeightPx(43, 15, 20);
        assertEquals(48, compact);
        assertEquals(60, PlayerSeatMetrics.arrange(compact, 12, 3).frameHeightPx());
        int roomy = PlayerSeatMetrics.referenceFrameHeightPx(53, 17, 17 + 16);
        assertEquals(69, roomy);
        assertEquals(87, PlayerSeatMetrics.arrange(roomy, 12, 3).frameHeightPx());
        assertEquals("A larger name must never be squeezed into the minimum heading region",
                80, PlayerSeatMetrics.referenceFrameHeightPx(80, 28, 20));
    }

    @Test public void equalizedSeatSlotsKeepFramesAlignedForDifferentMeasuredContents() {
        int reserved = PlayerSeatMetrics.arrange(59, 26, 3).heightPx();
        var first = PlayerSeatMetrics.arrange(48, 26, 3, reserved);
        var second = PlayerSeatMetrics.arrange(59, 26, 3, reserved);
        assertEquals(first.heightPx(), second.heightPx());
        assertEquals(first.frameHeightPx(), second.frameHeightPx());
        assertEquals(first.frameTopPx(), second.frameTopPx());
    }

    @Test public void invalidExtentsCannotSilentlyHideAnAction() {
        assertThrows(IllegalArgumentException.class, () -> PlayerSeatMetrics.arrange(0, 12, 3));
        assertThrows(IllegalArgumentException.class, () -> PlayerSeatMetrics.arrange(40, 0, 3));
        assertThrows(IllegalArgumentException.class, () -> PlayerSeatMetrics.arrange(40, 12, 0));
    }
}
