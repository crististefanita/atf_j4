package ro.whistlab.app.seat;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import org.junit.Test;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.core.rules.HowToPlay;

/** Inject wrapped glyph extents; actual Android font rendering keeps its native sanity check. */
public final class PlayerSeatMetricsComponentTest extends atf.gradle.audit.GradleAuditedTest {
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

                    atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "action", action, "bidding", bidding, "compact", compact, "density", density, "fontScale", fontScale, "language", language, "players", players, "profile", profile, "profiles", profiles, "seatWidthPx", seatWidthPx, "widthDp", widthDp);
                    assertFalse("action.isBlank() must remain false", action.isBlank());
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
                    assertEquals("plan.badgeHeightPx() must equal measuredBadgeHeight", measuredBadgeHeight, plan.badgeHeightPx());
                    assertEquals("The complete action ends before the framed name begins",
                            plan.badgeHeightPx() + gap, plan.frameTopPx());
                    assertEquals("plan.frameHeightPx() must equal (int)Math.ceil(referenceHeight * 1.25)", (int) Math.ceil(referenceHeight * 1.25), plan.frameHeightPx());
                    assertEquals("plan.heightPx() must equal plan.frameTopPx() + plan.frameHeightPx()", plan.frameTopPx() + plan.frameHeightPx(), plan.heightPx());
                    // The active flag affects visibility only, not these reserved dimensions.
                    assertEquals("PlayerSeatMetrics.arrange(referenceHeight, measuredBadgeHeight, gap) must equal plan", plan, PlayerSeatMetrics.arrange(referenceHeight, measuredBadgeHeight, gap));
                }
            }
        }
    }

    @Test public void multiLineActionExpandsItsOwnRegionAndCannotIntersectTheFrame() {
        var one = PlayerSeatMetrics.arrange(43, 13, 3);
        var three = PlayerSeatMetrics.arrange(43, 39, 3);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "one", one, "three", three);
        assertEquals("three.frameHeightPx() must equal one.frameHeightPx()", one.frameHeightPx(), three.frameHeightPx());
        assertEquals("three.frameTopPx() - one.frameTopPx() must equal 26", 26, three.frameTopPx() - one.frameTopPx());
        assertEquals("three.heightPx() - one.heightPx() must equal 26", 26, three.heightPx() - one.heightPx());
        assertTrue("three.badgeHeightPx() < three.frameTopPx() must hold", three.badgeHeightPx() < three.frameTopPx());
    }

    @Test public void repeatedMeasurementUsesFreshContentRatherThanThePreviousEnlargedFrame() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 60, "declared constant 2", 48, "declared constant 3", 62, "declared constant 4", 49, "declared constant 5", 0, "declared constant 6", 10, "declared constant 7", 75, "declared constant 8", 12, "declared constant 9", 3);
        assertEquals("PlayerSeatMetrics.frameHeightPx(48) must equal 60", 60, PlayerSeatMetrics.frameHeightPx(48));
        assertEquals("PlayerSeatMetrics.frameHeightPx(49) must equal 62", 62, PlayerSeatMetrics.frameHeightPx(49));
        for (int pass = 0; pass < 10; pass++) {
            assertEquals("PlayerSeatMetrics.arrange(48, 12, 3).heightPx() must equal 75", 75, PlayerSeatMetrics.arrange(48, 12, 3).heightPx());
        }
    }

    @Test public void movingTheBadgeOutsideDoesNotShrinkTheReferenceFrameBeforeEnlargingIt() {
        int compact = PlayerSeatMetrics.referenceFrameHeightPx(43, 15, 20);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "compact", compact);
        assertEquals("compact must equal 48", 48, compact);
        assertEquals("PlayerSeatMetrics.arrange(compact, 12, 3).frameHeightPx() must equal 60", 60, PlayerSeatMetrics.arrange(compact, 12, 3).frameHeightPx());
        int roomy = PlayerSeatMetrics.referenceFrameHeightPx(53, 17, 17 + 16);
        assertEquals("roomy must equal 69", 69, roomy);
        assertEquals("PlayerSeatMetrics.arrange(roomy, 12, 3).frameHeightPx() must equal 87", 87, PlayerSeatMetrics.arrange(roomy, 12, 3).frameHeightPx());
        assertEquals("A larger name must never be squeezed into the minimum heading region",
                80, PlayerSeatMetrics.referenceFrameHeightPx(80, 28, 20));
    }

    @Test public void equalizedSeatSlotsKeepFramesAlignedForDifferentMeasuredContents() {
        int reserved = PlayerSeatMetrics.arrange(59, 26, 3).heightPx();
        var first = PlayerSeatMetrics.arrange(48, 26, 3, reserved);
        var second = PlayerSeatMetrics.arrange(59, 26, 3, reserved);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "first", first, "reserved", reserved, "second", second);
        assertEquals("second.heightPx() must equal first.heightPx()", first.heightPx(), second.heightPx());
        assertEquals("second.frameHeightPx() must equal first.frameHeightPx()", first.frameHeightPx(), second.frameHeightPx());
        assertEquals("second.frameTopPx() must equal first.frameTopPx()", first.frameTopPx(), second.frameTopPx());
    }

    @Test public void invalidExtentsCannotSilentlyHideAnAction() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 0, "declared constant 2", 12, "declared constant 3", 3, "declared constant 4", 40);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> PlayerSeatMetrics.arrange(0, 12, 3));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> PlayerSeatMetrics.arrange(40, 0, 3));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> PlayerSeatMetrics.arrange(40, 12, 0));
    }
}
