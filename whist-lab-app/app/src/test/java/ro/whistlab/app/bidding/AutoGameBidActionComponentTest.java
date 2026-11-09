package ro.whistlab.app.bidding;

import static atf.gradle.audit.assertions.GradleAssertions.*;

import org.junit.Test;
import ro.cardtable.ui.table.TableGeometry;
import ro.cardtable.ui.table.TableSizing;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.core.rules.HowToPlay;

public final class AutoGameBidActionComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void theAutoGameControlMovesBelowTheBidButtonWhenTheSideSeatsNeedTheWidth() {
        int[] widths = {280, 313, 353, 560, 760};
        int[] options = {1, 3, 9, 14, 18};
        float[] fontScales = {1f, 1.3f, 1.5f, 2f};
        for (int width : widths) for (int players = 3; players <= 6; players++)
            for (int gap : new int[]{1, 8}) for (float scale : fontScales) for (int count : options) {
                int seat = TableSizing.seatWidthDp(width, gap == 1);
                var layout = BidGridMetrics.actionLayout(count, width, seat, gap, scale);
                int gridWidth = Math.max(132,
                        Math.min(count, BidGridMetrics.columnCount(count, width)) * 52);
                int toggleWidth = Math.min(width,
                        (int) Math.ceil(BidGridMetrics.AUTO_GAME_TOGGLE_BASE_WIDTH_DP * Math.max(1f, scale)));
                int inlineWidth = Math.max(gridWidth, BidGridMetrics.SUBMIT_WIDTH_DP
                        + BidGridMetrics.AUTO_GAME_ACTION_GAP_DP + toggleWidth);
                boolean fitsBesideSeats = inlineWidth + 2 * seat + 2 * gap <= width;
                int expectedHeight = BidGridMetrics.rowCount(count, width) * BidGridMetrics.CHOICE_ROW_DP
                        + BidGridMetrics.SUBMIT_TARGET_DP
                        + (fitsBesideSeats ? 0 : BidGridMetrics.AUTO_GAME_STACK_GAP_DP
                                + BidGridMetrics.AUTO_GAME_TOGGLE_TARGET_DP);

                atf.gradle.audit.GradleInputs.fixture("Measured bid controls before assertions",
                        "available width", width, "players", players, "region gap", gap,
                        "font scale", scale, "bid options", count, "seat width", seat,
                        "action layout", layout, "inline width", inlineWidth);
                assertEquals("layout.autoGameInline() must equal the independent seat-clearance decision",
                        fitsBesideSeats, layout.autoGameInline());
                assertTrue("layout.centerWidthDp() must stay inside the table", layout.centerWidthDp() <= width);
                assertTrue("layout.controlsWidthDp() must fit the center", layout.controlsWidthDp() <= layout.centerWidthDp());
                assertEquals("layout.centerHeightDp() must include the active control rows",
                        expectedHeight, layout.centerHeightDp());

                int height = TableGeometry.minimumHeight(players, 82, layout.centerHeightDp(), gap) + 24;
                var plan = TableGeometry.arrange(width, height, players, seat, 82,
                        layout.centerWidthDp(), layout.centerHeightDp(), gap, false,
                        TableGeometry.CenterAlignment.TOWARD_HUMAN);
                for (var occupied : plan.seats())
                    assertFalse("the AutoGame-enabled center must not cover a seat", occupied.overlaps(plan.center()));
            }
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
                IllegalArgumentException.class, () -> BidGridMetrics.actionLayout(1, 0, 80, 1, 1f));
        assertThrows("The operation rejects its input with IllegalArgumentException.class",
                IllegalArgumentException.class, () -> BidGridMetrics.actionLayout(-1, 320, 80, 1, 1f));
        assertThrows("The operation rejects its input with IllegalArgumentException.class",
                IllegalArgumentException.class, () -> BidGridMetrics.actionLayout(1, 320, 80, 1, Float.NaN));
    }
}
