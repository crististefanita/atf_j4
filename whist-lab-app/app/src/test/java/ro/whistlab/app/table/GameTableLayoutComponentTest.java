package ro.whistlab.app.table;

import ro.whistlab.app.bidding.BidGridMetrics;
import ro.whistlab.app.hand.HandLayoutMetrics;
import ro.whistlab.app.localization.WhistUiText;

import static org.junit.Assert.*;
import java.util.ArrayList;
import org.junit.Test;

/** Tests occupied regions together, including the nine-option regression. */
public final class GameTableLayoutComponentTest {
    @Test public void allHandCountsRemainCenteredWithoutGapsAtActualWindowDensities() {
        for (int width : new int[]{280, 313, 353, 560, 760}) for (float density : new float[]{1, 2, 2.625f, 2.75f}) {
            int pixels = Math.round(width * density);
            for (float scale : new float[]{1, 1.3f, 1.5f}) for (int count = 1; count <= 17; count++) {
                int index = Math.round(19 * scale * density);
                var fan = HandLayoutMetrics.fan(count, pixels, density, scale, index);
                int end = fan.left(count - 1) + fan.cardWidth();
                assertEquals(fan.first(), fan.width() - end, 1);
                assertTrue(fan.advance() <= fan.cardWidth());
                if (count > 1) assertTrue(fan.advance() >= index);
                if (count <= 3) {
                    assertEquals(pixels, fan.width());
                    if (count > 1) assertTrue(fan.advance() < fan.cardWidth());
                }
            }
        }
    }

    @Test public void newGameListsSixModesWithStableSelectionInBothLanguages() {
        var modes = WhistUiText.MENU_MODES;
        assertEquals(java.util.List.of("OH_HELL", "DESCENDING", "ROMANIAN_WHIST", "DOWN_AND_UP", "LONG_ROMANIAN", "REVERSE_OH_HELL"),
                modes.stream().map(Enum::name).toList());
        for (var language : ro.whistlab.core.rules.HowToPlay.Language.values()) {
            var copy = new WhistUiText(language);
            assertEquals(6, modes.stream().map(copy::modeName).distinct().count());
        }
    }

    @Test public void everyMeasuredTableRegionFitsWithoutCoveringAnotherRegion() {
        for (int width : new int[]{280, 313, 353, 560, 760}) {
            for (int players = 3; players <= 6; players++) {
                for (int options = 1; options <= 18; options++) {
                    for (int gap : new int[]{3, 8}) for (int seatHeight : new int[]{62, 82, 110}) {
                        int seatWidth = TableLayoutMetrics.seatWidthDp(width);
                        int centerWidth = Math.max(132, Math.min(options, BidGridMetrics.columnCount(options, width)) * 52);
                        int centerHeight = BidGridMetrics.rowCount(options, width) * 50 + 48;
                        int minimum = TableGeometry.minimumHeight(players, seatHeight, centerHeight, gap);
                        for (int extra : new int[]{0, 80, 240}) {
                            var plan = TableGeometry.arrange(width, minimum + extra, players, seatWidth,
                                    seatHeight, centerWidth, centerHeight, gap);
                            var boxes = new ArrayList<>(plan.seats());
                            boxes.add(plan.center());
                            for (int i = 0; i < boxes.size(); i++) {
                                var box = boxes.get(i);
                                assertTrue(box.left() >= 0 && box.top() >= 0);
                                assertTrue(box.right() <= width && box.bottom() <= minimum + extra);
                                for (int j = i + 1; j < boxes.size(); j++) {
                                    assertFalse("Overlap: width=" + width + ", players=" + players + ", options=" + options,
                                            box.overlaps(boxes.get(j)));
                                }
                            }
                            assertEquals(width / 2.0, (plan.seats().get(0).left() + plan.seats().get(0).right()) / 2.0, 0.5);
                            assertEquals(minimum + extra, plan.seats().get(0).bottom());
                        }
                    }
                }
            }
        }
    }

    @Test public void nineBidsAtFourSeatsCannotOverlapTheSideNames() {
        var plan = TableGeometry.arrange(353, 368, 4, 102, 70, 260, 148, 8);
        assertTrue(plan.seats().get(1).bottom() < plan.center().top());
        assertTrue(plan.seats().get(3).bottom() < plan.center().top());
        assertFalse(plan.seats().get(1).overlaps(plan.center()));
        assertFalse(plan.seats().get(3).overlaps(plan.center()));
    }

    @Test public void sixSeatsUseTheAvailableArcBesideAWideBiddingGrid() {
        var plan = TableGeometry.arrange(971, 1160, 6, 281, 196, 720, 408, 22);
        var leftShoulder = plan.seats().get(1);
        var north = plan.seats().get(2);
        assertTrue(leftShoulder.top() - north.top() >= 1160 * 0.13);
        assertEquals(22, plan.center().top() - leftShoulder.bottom());
    }

    @Test public void undersizedRegionsFailInsteadOfInventingAValidLayout() {
        assertThrows(IllegalArgumentException.class, () -> TableGeometry.arrange(353, 100, 4, 102, 70, 260, 148, 8));
        assertThrows(IllegalArgumentException.class, () -> TableGeometry.arrange(200, 400, 4, 102, 70, 160, 148, 8));
        assertThrows(IllegalArgumentException.class, () -> TableGeometry.arrange(353, 400, 7, 102, 70, 160, 148, 8));
    }

    @Test public void handIndicesReserveTheMeasuredGlyphBandAtEverySupportedWidth() {
        for (int width : new int[]{280, 313, 353, 560}) {
            for (int count = 2; count <= 17; count++) {
                for (int measuredIndex : new int[]{19, 25, 31}) {
                    int advance = HandLayoutMetrics.horizontalAdvanceDp(count, width, 1f, measuredIndex);
                    assertTrue(advance >= measuredIndex);
                }
            }
            assertFalse(HandLayoutMetrics.requiresHorizontalScroll(13, width, 1f));
        }
        assertTrue(HandLayoutMetrics.requiresHorizontalScroll(17, 280, 1f));
        assertTrue(HandLayoutMetrics.horizontalAdvanceDp(13, 280, 1.5f, 31) >= 31);
    }

    @Test public void oneCardHasNoArtificialNeighbourOffsetAndEmptyHandsAreSeparate() {
        assertEquals(HandLayoutMetrics.cardWidthDp(1, 353, 1f), HandLayoutMetrics.contentWidthDp(1, 353, 1f));
        assertFalse(HandLayoutMetrics.usesSingleRow(0));
        assertFalse(HandLayoutMetrics.usesSingleRow(18));
        assertThrows(IllegalArgumentException.class, () -> HandLayoutMetrics.horizontalAdvanceDp(13, 280, 1f, 0));
    }

    @Test public void liftedCardKeepsItsClearanceAndRemainsBehindItsNeighbours() {
        assertEquals(98, HandLayoutMetrics.cardHeightDp());
        assertTrue(HandLayoutMetrics.LIFT_CLEARANCE_DP >= HandLayoutMetrics.CARD_LIFT_DP
                + (int) Math.ceil(HandLayoutMetrics.CARD_HEIGHT_DP * (HandLayoutMetrics.CARD_LIFT_SCALE - 1) / 2));
        assertTrue(HandLayoutMetrics.SELECTED_Z_DP < HandLayoutMetrics.NEIGHBOR_Z_DP);
    }

    @Test public void denseBidsUseAvailableColumnsWithoutShrinkingTouchTargets() {
        assertEquals(5, BidGridMetrics.columnCount(14, 280));
        assertEquals(3, BidGridMetrics.rowCount(14, 280));
        assertEquals(2, BidGridMetrics.rowCount(9, 353));
        assertEquals(48, BidGridMetrics.CHOICE_TARGET_DP);
        assertTrue(BidGridMetrics.CHOICE_ROW_DP >= BidGridMetrics.CHOICE_TARGET_DP + 2);
        for (int width : new int[]{280, 313, 353, 560}) {
            for (int count = 1; count <= 18; count++) assertTrue(BidGridMetrics.columnCount(count, width) * 52 <= width);
        }
    }
}
