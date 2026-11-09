package ro.whistlab.app.bidding;

import static atf.gradle.audit.assertions.GradleAssertions.*;

import org.junit.Test;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.core.rules.HowToPlay;

public final class AutoGameBidActionComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void bothCenteredActionsReserveSeparateRowsAtEveryWidth() {
        int[] widths = {280, 313, 353, 560, 760};
        int[] options = {1, 3, 9, 14, 18};
        float[] fontScales = {1f, 1.3f, 1.5f, 2f};
        for (int width : widths) for (float scale : fontScales) for (int count : options) {
            var layout = BidGridMetrics.actionLayout(count, width, scale);
            atf.gradle.audit.GradleInputs.fixture("Centered bid controls",
                    "available width", width, "font scale", scale, "bid options", count, "layout", layout);
            assertEquals("Two 48 dp controls and their 2 dp gap follow the choice rows", 98 + 50 * BidGridMetrics.rowCount(count, width),
                    layout.centerHeightDp());
            assertTrue("The submit target fits inside the board center", layout.centerWidthDp() >= 119);
            assertTrue("The board center stays inside the table", layout.centerWidthDp() <= width);
        }
    }

    @Test public void theOutlineShrinksByFifteenPercentWithoutChangingTextOrTouchTarget() {
        assertEquals("The label stays at 13 sp", 13, BidGridMetrics.SUBMIT_TEXT_SP);
        assertEquals("The touch target stays 119 dp wide", 119, BidGridMetrics.SUBMIT_WIDTH_DP);
        assertEquals("The touch target stays 48 dp high", 48, BidGridMetrics.SUBMIT_TARGET_DP);
        for (float density : new float[]{1f, 1.5f, 2.25f, 2.75f, 3.5f}) {
            int width = Math.round(119 * density);
            int height = Math.round(48 * density);
            int previousHeight = height - 2 * Math.round(2 * density);
            var inset = BidGridMetrics.submitInsets(density, Math.round(58 * density), Math.round(17 * density));
            atf.gradle.audit.GradleInputs.fixture("Bid outline at standard font",
                    "density", density, "width", width, "previous height", previousHeight, "inset", inset);
            assertTrue("Outline width is 85% of the former width within one pixel",
                    Math.abs(width - 2 * inset.horizontalPx() - width * .85) <= 1);
            assertTrue("Outline height is 85% of the former height within one pixel",
                    Math.abs(height - 2 * inset.verticalPx() - previousHeight * .85) <= 1);
        }
    }

    @Test public void enlargedTextCanReclaimOutlineSpaceWithoutLosingPadding() {
        var regular = BidGridMetrics.submitInsets(2, 116, 34);
        var enlarged = BidGridMetrics.submitInsets(2, 212, 76);
        atf.gradle.audit.GradleInputs.fixture("Measured bid label at enlarged font",
                "label width", 212, "label height", 76, "regular", regular, "enlarged", enlarged);
        assertTrue("The outline expands in both directions for the measured text",
                enlarged.horizontalPx() < regular.horizontalPx() && enlarged.verticalPx() < regular.verticalPx());
        assertTrue("The full label and side padding fit within the outline", 238 - 2 * enlarged.horizontalPx() >= 236);
        assertTrue("The full line and vertical padding fit within the outline", 96 - 2 * enlarged.verticalPx() >= 88);
    }

    @Test public void theControlUsesShortVisibleLabelsAndFullLocalizedActions() {
        for (var language : HowToPlay.Language.values()) {
            var copy = new WhistUiText(language);
            atf.gradle.audit.GradleInputs.fixture("Localized AutoGame bid control",
                    "language", language, "toggle", copy.autoGameToggle(),
                    "start description", copy.autoGameHand(), "stop description", copy.stopAutoGameHand());
            assertEquals("the compact label must follow the selected language",
                    language == HowToPlay.Language.EN ? "Auto-play" : "Automat", copy.autoGameToggle());
            assertTrue("the start description must remain explicit", copy.autoGameHand().length() > copy.autoGameToggle().length());
            assertTrue("the stop description must remain explicit", copy.stopAutoGameHand().length() > copy.autoGameToggle().length());
        }
    }

    @Test public void invalidControlMeasurementsAreRejected() {
        atf.gradle.audit.GradleInputs.fixture("Invalid AutoGame control measurements",
                "zero table width", 0, "negative bid options", -1, "non-finite font scale", Float.NaN);
        assertThrows("The operation rejects its input with IllegalArgumentException.class",
                IllegalArgumentException.class, () -> BidGridMetrics.actionLayout(1, 0, 1f));
        assertThrows("The operation rejects its input with IllegalArgumentException.class",
                IllegalArgumentException.class, () -> BidGridMetrics.actionLayout(-1, 320, 1f));
        assertThrows("The operation rejects its input with IllegalArgumentException.class",
                IllegalArgumentException.class, () -> BidGridMetrics.actionLayout(1, 320, Float.NaN));
    }
}
