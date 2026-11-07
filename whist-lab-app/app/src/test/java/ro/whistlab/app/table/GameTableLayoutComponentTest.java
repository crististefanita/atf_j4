package ro.whistlab.app.table;

import ro.whistlab.app.bidding.BidGridMetrics;
import ro.whistlab.app.hand.HandLayoutMetrics;
import ro.whistlab.app.localization.WhistUiText;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.ArrayList;
import org.junit.Test;

/** Tests occupied regions together, including the nine-option regression. */
public final class GameTableLayoutComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void cardHeightsApplyTheTenPercentReductionOnceWithOnePortraitProportion() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 98, "declared constant 2", 0.9f, "declared constant 3", 78, "declared constant 4", 63, "declared constant 5", 1f, "declared constant 6", 1.3f, "declared constant 7", 1.5f, "declared constant 8", 2f, "declared constant 9", 71, "declared constant 10", 599, "declared constant 11", 600, "declared constant 12", 116, "declared constant 13", 280, "declared constant 14", 313, "declared constant 15", 353, "declared constant 16", 560, "declared constant 17", 1, "declared constant 18", 13, "declared constant 19", "Only overlap changes with the hand size", "declared constant 20", 88, "declared constant 21", 0);
        assertEquals("HandLayoutMetrics.cardHeightDp() must equal Math.round(98 * 0.9F)", Math.round(98 * 0.9f), HandLayoutMetrics.cardHeightDp());
        assertEquals("TrickCardMetrics.CARD_HEIGHT_DP must equal Math.round(78 * 0.9F)", Math.round(78 * 0.9f), TrickCardMetrics.CARD_HEIGHT_DP);
        assertEquals("HandLayoutMetrics.cardWidthDp(1.0F) must equal 63", 63, HandLayoutMetrics.cardWidthDp(1f));
        for (float scale : new float[]{1f, 1.3f, 1.5f, 2f}) {
            assertEquals("new TableViewportPolicy(599, scale).playedCardFaceHeightDp() must equal Math.round(71 * 0.9F)", Math.round(71 * 0.9f), new TableViewportPolicy(599, scale).playedCardFaceHeightDp());
            assertEquals("new TableViewportPolicy(600, scale).playedCardFaceHeightDp() must equal Math.round(78 * 0.9F)", Math.round(78 * 0.9f), new TableViewportPolicy(600, scale).playedCardFaceHeightDp());
        }
        assertEquals("HandLayoutMetrics.reservedHandHeightDp() must equal 116", 116, HandLayoutMetrics.reservedHandHeightDp());
        for (int width : new int[]{280, 313, 353, 560}) for (int cards=1;cards<=13;cards++) {
            for (float scale : new float[]{1f,1.3f,1.5f,2f}) {
                assertEquals("Only overlap changes with the hand size",63,HandLayoutMetrics.cardWidthDp(cards,width,scale));
            }
        }
        assertEquals("HandLayoutMetrics.cardHeightDp() must equal 88", 88, HandLayoutMetrics.cardHeightDp());
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->ro.whistlab.app.cards.CardFaceMetrics.atHeight(0));
    }

    @Test public void allHandCountsRemainCenteredWithoutGapsAtActualWindowDensities() {
        for (int width : new int[]{280, 313, 353, 560, 760}) for (float density : new float[]{1, 2, 2.625f, 2.75f}) {
            int pixels = Math.round(width * density);
            for (float scale : new float[]{1, 1.3f, 1.5f}) for (int count = 1; count <= 17; count++) {
                int index = Math.round(19 * scale * density);
                var fan = HandLayoutMetrics.fan(count, pixels, density, scale, index);
                int end = fan.left(count - 1) + fan.cardWidth();

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "count", count, "density", density, "end", end, "fan", fan, "index", index, "pixels", pixels, "scale", scale, "width", width);
                assertEquals(fan.first(), fan.width() - end, 1);
                assertTrue("fan.advance() <= fan.cardWidth() must hold", fan.advance() <= fan.cardWidth());
                if (count > 1) assertTrue("fan.advance() >= index must hold", fan.advance() >= index);
                if (count <= 3) {
                    assertEquals("fan.width() must equal pixels", pixels, fan.width());
                    if (count > 1) assertTrue("fan.advance() < fan.cardWidth() must hold", fan.advance() < fan.cardWidth());
                }
            }
        }
    }

    @Test public void thirteenStandardFacesFitWithoutAccumulatingFractionalPixelAdvanceRounding() {
        for (int width : new int[]{280,313,353,560}) for (float density : new float[]{1f,1.5f,2.25f,2.625f,2.75f,3.5f}) {
            int viewport=Math.round(width*density),glyph=Math.round(17*density);
            var fan=HandLayoutMetrics.fan(13,viewport,density,1f,glyph);

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "density", density, "fan", fan, "glyph", glyph, "viewport", viewport, "width", width);
            assertEquals("All thirteen faces fit the first viewport",viewport,fan.width());
            assertEquals("fan.cardWidth() must equal Math.round(63 * density)", Math.round(63*density),fan.cardWidth());
            assertTrue("fan.first() >= 0 must hold", fan.first()>=0);
            assertTrue("fan.left(12) + fan.cardWidth() <= viewport must hold", fan.left(12)+fan.cardWidth()<=viewport);
            for(int card=1;card<13;card++)assertTrue("fan.left(card) - fan.left(card - 1) >= glyph must hold", fan.left(card)-fan.left(card-1)>=glyph);
        }
    }

    @Test public void invalidScalesAndMeasuredExtentsCannotInventAValidHand() {
        for(float invalid:new float[]{0,-1,Float.NaN,Float.POSITIVE_INFINITY}) {

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "invalid", invalid);
            assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->HandLayoutMetrics.fan(13,630,invalid,1f,38));
            assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->HandLayoutMetrics.cardWidthDp(invalid));
            assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->HandLayoutMetrics.minVisibleAdvanceDp(invalid));
        }
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->HandLayoutMetrics.fan(13,0,2.25f,1f,38));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->HandLayoutMetrics.fan(13,630,2.25f,1f,0));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->new ro.whistlab.app.cards.CardFaceMetrics(-1));
    }

    @Test public void newGameListsSixModesWithStableSelectionInBothLanguages() {
        var modes = WhistUiText.MENU_MODES;

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "modes", modes);
        assertEquals("modes.stream().map(Enum::name).toList() must equal java.util.List.of(\"OH_HELL\", \"DESCENDING\", \"ROMANIAN_WHIST\", \"DOWN_AND_UP\", \"LONG_ROMANIAN\", \"REVERSE_OH_HELL\")", java.util.List.of("OH_HELL", "DESCENDING", "ROMANIAN_WHIST", "DOWN_AND_UP", "LONG_ROMANIAN", "REVERSE_OH_HELL"),
                modes.stream().map(Enum::name).toList());
        for (var language : ro.whistlab.core.rules.HowToPlay.Language.values()) {
            var copy = new WhistUiText(language);
            assertEquals("modes.stream().map(copy::modeName).distinct().count() must equal 6", 6, modes.stream().map(copy::modeName).distinct().count());
        }
    }

    @Test public void everyMeasuredTableRegionFitsWithoutCoveringAnotherRegion() {
        var checks = new atf.gradle.audit.matrix.ExhaustiveMatrixChecks();
        int[] widths = {280, 313, 353, 560, 760}, gaps = {3, 8}, seatHeights = {62, 82, 110}, extras = {0, 80, 240};
        atf.gradle.audit.GradleInputs.fixture("Complete measured table parameter grid", "widths", widths,
                "player range", "3..6 inclusive", "bid option range", "1..18 inclusive", "gaps", gaps,
                "seat heights", seatHeights, "extra heights", extras, "horizontal tolerance", .5);
        for (int width : widths) for (int players = 3; players <= 6; players++) for (int options = 1; options <= 18; options++) {
            for (int gap : gaps) for (int seatHeight : seatHeights) {
                int seatWidth = TableLayoutMetrics.seatWidthDp(width);
                int centerWidth = Math.max(132, Math.min(options, BidGridMetrics.columnCount(options, width)) * 52);
                int centerHeight = BidGridMetrics.rowCount(options, width) * 50 + 48;
                int minimum = TableGeometry.minimumHeight(players, seatHeight, centerHeight, gap);
                for (int extra : extras) {
                    var plan = TableGeometry.arrange(width, minimum + extra, players, seatWidth, seatHeight, centerWidth, centerHeight, gap);
                    String context = "width=" + width + ", players=" + players + ", options=" + options + ", gap=" + gap
                            + ", seatHeight=" + seatHeight + ", extra=" + extra + ", plan=" + plan;
                    var boxes = new ArrayList<>(plan.seats()); boxes.add(plan.center());
                    for (int i = 0; i < boxes.size(); i++) {
                        var box = boxes.get(i); checks.at(context + " / region " + i);
                        checks.equal("Regions start within the viewport", true, box.left() >= 0 && box.top() >= 0);
                        checks.equal("Regions end within the viewport", true, box.right() <= width && box.bottom() <= minimum + extra);
                        for (int j = i + 1; j < boxes.size(); j++) {
                            checks.at(context + " / regions " + i + " and " + j);
                            checks.equal("Distinct regions never overlap", false, box.overlaps(boxes.get(j)));
                        }
                    }
                    checks.at(context);
                    checks.within("Human seat is horizontally centered", width / 2.0,
                            (plan.seats().get(0).left() + plan.seats().get(0).right()) / 2.0, .5);
                    checks.equal("Human seat is anchored at the viewport bottom", minimum + extra, plan.seats().get(0).bottom());
                }
            }
        }
        long perGrid = java.util.stream.IntStream.rangeClosed(3, 6).map(players ->
                2 * (players + 1) + (players + 1) * players / 2 + 2).sum();
        long expectedPairs = (long) widths.length * 18 * gaps.length * seatHeights.length * extras.length * perGrid;
        assertEquals("Every declared region, pair and anchor in the Cartesian grid was checked", expectedPairs, checks.checkedPairs());
        checks.publish();
    }
    @Test public void nineBidsAtFourSeatsCannotOverlapTheSideNames() {
        var plan = TableGeometry.arrange(353, 368, 4, 102, 70, 260, 148, 8);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "plan", plan);
        assertTrue("plan.seats().get(1).bottom() < plan.center().top() must hold", plan.seats().get(1).bottom() < plan.center().top());
        assertTrue("plan.seats().get(3).bottom() < plan.center().top() must hold", plan.seats().get(3).bottom() < plan.center().top());
        assertFalse("plan.seats().get(1).overlaps(plan.center()) must remain false", plan.seats().get(1).overlaps(plan.center()));
        assertFalse("plan.seats().get(3).overlaps(plan.center()) must remain false", plan.seats().get(3).overlaps(plan.center()));
    }

    @Test public void sixSeatsUseTheAvailableArcBesideAWideBiddingGrid() {
        var plan = TableGeometry.arrange(971, 1160, 6, 281, 196, 720, 408, 22);
        var leftShoulder = plan.seats().get(2);
        var north = plan.seats().get(3);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "leftShoulder", leftShoulder, "north", north, "plan", plan);
        assertTrue("leftShoulder.top() - north.top() >= 1160 * 0.13 must hold", leftShoulder.top() - north.top() >= 1160 * 0.13);
        assertEquals("plan.center().top() - leftShoulder.bottom() must equal 22", 22, plan.center().top() - leftShoulder.bottom());
    }

    @Test public void undersizedRegionsFailInsteadOfInventingAValidLayout() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 353, "declared constant 2", 100, "declared constant 3", 4, "declared constant 4", 102, "declared constant 5", 70, "declared constant 6", 260, "declared constant 7", 148, "declared constant 8", 8, "declared constant 9", 200, "declared constant 10", 400, "declared constant 11", 160, "declared constant 12", 7);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> TableGeometry.arrange(353, 100, 4, 102, 70, 260, 148, 8));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> TableGeometry.arrange(200, 400, 4, 102, 70, 160, 148, 8));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> TableGeometry.arrange(353, 400, 7, 102, 70, 160, 148, 8));
    }

    @Test public void handIndicesReserveTheMeasuredGlyphBandAtEverySupportedWidth() {
        for (int width : new int[]{280, 313, 353, 560}) {
            for (int count = 2; count <= 17; count++) {
                for (int measuredIndex : new int[]{19, 25, 31}) {
                    int advance = HandLayoutMetrics.horizontalAdvanceDp(count, width, 1f, measuredIndex);

                    atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "advance", advance, "count", count, "measuredIndex", measuredIndex, "width", width);
                    assertTrue("advance >= measuredIndex must hold", advance >= measuredIndex);
                }
            }
            assertFalse("HandLayoutMetrics.requiresHorizontalScroll(13, width, 1.0F) must remain false", HandLayoutMetrics.requiresHorizontalScroll(13, width, 1f));
        }
        assertTrue("HandLayoutMetrics.requiresHorizontalScroll(17, 280, 1.0F) must hold", HandLayoutMetrics.requiresHorizontalScroll(17, 280, 1f));
        assertTrue("HandLayoutMetrics.horizontalAdvanceDp(13, 280, 1.5F, 31) >= 31 must hold", HandLayoutMetrics.horizontalAdvanceDp(13, 280, 1.5f, 31) >= 31);
    }

    @Test public void oneCardHasNoArtificialNeighbourOffsetAndEmptyHandsAreSeparate() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 1, "declared constant 2", 353, "declared constant 3", 1f, "declared constant 4", 0, "declared constant 5", 18, "declared constant 6", 13, "declared constant 7", 280);
        assertEquals("HandLayoutMetrics.contentWidthDp(1, 353, 1.0F) must equal HandLayoutMetrics.cardWidthDp(1, 353, 1.0F)", HandLayoutMetrics.cardWidthDp(1, 353, 1f), HandLayoutMetrics.contentWidthDp(1, 353, 1f));
        assertFalse("HandLayoutMetrics.usesSingleRow(0) must remain false", HandLayoutMetrics.usesSingleRow(0));
        assertFalse("HandLayoutMetrics.usesSingleRow(18) must remain false", HandLayoutMetrics.usesSingleRow(18));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> HandLayoutMetrics.horizontalAdvanceDp(13, 280, 1f, 0));
    }

    @Test public void liftedCardKeepsItsClearanceAndRemainsBehindItsNeighbours() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 88, "declared constant 2", 1, "declared constant 3", 2, "declared constant 4", 1f, "declared constant 5", 1.5f, "declared constant 6", 2.25f, "declared constant 7", 2.625f, "declared constant 8", 2.75f, "declared constant 9", 3.5f);
        assertEquals("HandLayoutMetrics.cardHeightDp() must equal 88", 88, HandLayoutMetrics.cardHeightDp());
        assertTrue("HandLayoutMetrics.LIFT_CLEARANCE_DP >= HandLayoutMetrics.CARD_LIFT_DP + (int)Math.ceil(HandLayoutMetrics.CARD_HEIGHT_DP * (HandLayoutMetrics.CARD_LIFT_SCALE - 1) / 2) must hold", HandLayoutMetrics.LIFT_CLEARANCE_DP >= HandLayoutMetrics.CARD_LIFT_DP
                + (int) Math.ceil(HandLayoutMetrics.CARD_HEIGHT_DP * (HandLayoutMetrics.CARD_LIFT_SCALE - 1) / 2));
        assertTrue("HandLayoutMetrics.SELECTED_Z_DP < HandLayoutMetrics.NEIGHBOR_Z_DP must hold", HandLayoutMetrics.SELECTED_Z_DP < HandLayoutMetrics.NEIGHBOR_Z_DP);
        for (float density : new float[]{1f, 1.5f, 2.25f, 2.625f, 2.75f, 3.5f}) {
            int scaledFace = Math.round(88 * density);
            int liftedExtra = (int) Math.ceil(scaledFace * (HandLayoutMetrics.CARD_LIFT_SCALE - 1) / 2);
            assertTrue("Math.round(HandLayoutMetrics.LIFT_CLEARANCE_DP * density) >= Math.round(HandLayoutMetrics.CARD_LIFT_DP * density) + liftedExtra must hold", Math.round(HandLayoutMetrics.LIFT_CLEARANCE_DP * density)
                    >= Math.round(HandLayoutMetrics.CARD_LIFT_DP * density) + liftedExtra);
        }
    }

    @Test public void fractionalDensityMeasuresTheIndexOnceInPixelsWithoutClippingEitherEnd() {
        int measured = HandLayoutMetrics.indexBandWidthPx(35.6f, 2);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "measured", measured);
        assertEquals("measured must equal 40", 40, measured);
        var fan = HandLayoutMetrics.fan(13, 630, 2.25f, 1f, measured);
        assertEquals("fan.cardWidth() must equal 142", 142, fan.cardWidth());
        assertTrue("fan.advance() >= measured must hold", fan.advance() >= measured);
        assertTrue("fan.first() >= 0 must hold", fan.first() >= 0);
        assertTrue("fan.left(12) + fan.cardWidth() <= 630 must hold", fan.left(12) + fan.cardWidth() <= 630);
    }

    @Test public void invalidMeasuredGlyphsDoNotCreateAReadableBand() {
        for (float width : new float[]{0, -1, Float.NaN, Float.POSITIVE_INFINITY})
            {
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "width", width);
assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> HandLayoutMetrics.indexBandWidthPx(width, 2)); }
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> HandLayoutMetrics.indexBandWidthPx(30, -1));
    }

    @Test public void denseBidsUseAvailableColumnsWithoutShrinkingTouchTargets() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 5, "declared constant 2", 14, "declared constant 3", 280, "declared constant 4", 3, "declared constant 5", 2, "declared constant 6", 9, "declared constant 7", 353, "declared constant 8", 48, "declared constant 9", 313, "declared constant 10", 560, "declared constant 11", 1, "declared constant 12", 18, "declared constant 13", 52);
        assertEquals("BidGridMetrics.columnCount(14, 280) must equal 5", 5, BidGridMetrics.columnCount(14, 280));
        assertEquals("BidGridMetrics.rowCount(14, 280) must equal 3", 3, BidGridMetrics.rowCount(14, 280));
        assertEquals("BidGridMetrics.rowCount(9, 353) must equal 2", 2, BidGridMetrics.rowCount(9, 353));
        assertEquals("BidGridMetrics.CHOICE_TARGET_DP must equal 48", 48, BidGridMetrics.CHOICE_TARGET_DP);
        assertTrue("BidGridMetrics.CHOICE_ROW_DP >= BidGridMetrics.CHOICE_TARGET_DP + 2 must hold", BidGridMetrics.CHOICE_ROW_DP >= BidGridMetrics.CHOICE_TARGET_DP + 2);
        for (int width : new int[]{280, 313, 353, 560}) {
            for (int count = 1; count <= 18; count++) assertTrue("BidGridMetrics.columnCount(count, width) * 52 <= width must hold", BidGridMetrics.columnCount(count, width) * 52 <= width);
        }
    }
}
