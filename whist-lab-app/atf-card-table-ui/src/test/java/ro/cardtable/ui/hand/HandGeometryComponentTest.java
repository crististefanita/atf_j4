package ro.cardtable.ui.hand;

import java.util.List;
import org.junit.Test;
import ro.cardtable.ui.cards.CardFaceMetrics;
import static atf.gradle.audit.assertions.GradleAssertions.*;

public final class HandGeometryComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void inheritedPortraitDimensionsAndOneCardCenterStayExact() {
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 63, "declared constant 2", 88, "reserved hand height", 94, "declared constant 4", 1, "declared constant 5", 360, "declared constant 6", 1f, "declared constant 7", 18, "centered origin", 156, "declared constant 9", 0);
        assertEquals("CardFaceMetrics.atHeight(88).widthDp() must equal 63", 63,CardFaceMetrics.atHeight(88).widthDp());
        assertEquals("Face height is 75% of 88dp", 66,HandLayoutMetrics.cardHeightDp());assertEquals("Hand reserves face and lift", 94,HandLayoutMetrics.reservedHandHeightDp());
        var single=HandLayoutMetrics.fan(1,360,1f,1f,18);
        assertEquals("single.width() must equal 360", 360,single.width());assertEquals("Single card is centered", 156,single.first());assertEquals("Single card preserves portrait proportion", 47,single.cardWidth());
        assertEquals("Single card starts at its centered origin", 156,single.left(0));
    }

    @Test public void thirteenAndEightCardsKeepMeasuredIndicesAndSymmetricUnusedSpace() {
        for(int count:new int[]{8,13})for(int viewport:new int[]{280,360,760})for(float scale:new float[]{1f,1.3f,2f}) {
            int measured=(int)Math.ceil(18*scale);var fan=HandLayoutMetrics.fan(count,viewport,1f,scale,measured);
            
            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "count", count, "fan", fan, "measured", measured, "scale", scale, "viewport", viewport);
            assertTrue("fan.advance() >= measured must hold", fan.advance()>=measured);
            int unused=fan.width()-(fan.left(count-1)+fan.cardWidth())-fan.first();
            assertTrue("Integer centering differs by at most one pixel",unused>=0 && unused<=1);
            assertTrue("fan.left(count - 1) + fan.cardWidth() <= fan.width() must hold", fan.left(count-1)+fan.cardWidth()<=fan.width());
        }
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->HandLayoutMetrics.fan(0,360,1f,1f,18));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->HandLayoutMetrics.fan(8,360,Float.NaN,1f,18));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class,()->CardFaceMetrics.atHeight(0));
    }

    @Test public void allRanksGrowWithinTheMeasuredBandOfASeventeenCardHand() {
        var doubled = CardFaceMetrics.fitRank(CardFaceMetrics.RANK_SCALE, 15, 16, 55, 48);
        var enlarged = CardFaceMetrics.fitRank(CardFaceMetrics.RANK_SCALE, 39, 32, 41, 42);
        var band = HandLayoutMetrics.indexBandWidthPx(39 * enlarged, 1);
        var fan = HandLayoutMetrics.fan(17, 336, 1f, 2f, band);
        atf.gradle.audit.GradleInputs.fixture("Numeric ranks and measured text",
                "doubled", doubled, "enlarged", enlarged, "band", band, "fan", fan);
        assertEquals("The complete rank 10 is enlarged", 2, CardFaceMetrics.rankLength("10\u2666"));
        assertEquals("Single-digit ranks are enlarged", 1, CardFaceMetrics.rankLength("8\u2663"));
        for (String rank : List.of("J", "Q", "K", "A")) {
            assertEquals("Face rank is enlarged with its suit present: " + rank, 1,
                    CardFaceMetrics.rankLength(rank + "\u2665"));
        }
        assertEquals("Table face width is rounded to 75%", 38, CardFaceMetrics.compactDimension(50));
        assertEquals("Table face height is rounded to 75%", 53, CardFaceMetrics.compactDimension(70));
        assertEquals("Non-card text is outside the card rank rule", 0, CardFaceMetrics.rankLength("100"));
        assertTrue("Standard text reaches an exact factor of two", doubled == 2f);
        assertTrue("Larger text is fitted to the real width and line height", enlarged > 1f && enlarged * 39 <= 41);
        assertTrue("Each measured band remains exposed", fan.advance() >= band);
        assertEquals("The right edge of the last card is reachable by scrolling",
                fan.width(), fan.left(16) + fan.cardWidth());
    }

    @Test public void liftUsesAndroidAccelerationAndLeavesNeighboursInFront() {
        atf.gradle.audit.GradleInputs.fixture("Samples projected to the browser",
                "elapsed fractions", java.util.List.of(0f, .2f, .5f, .8f, 1f), "animation layers",
                java.util.List.of(HandLayoutMetrics.SELECTED_Z_DP, HandLayoutMetrics.NEIGHBOR_Z_DP));
        assertTrue("Lift starts and ends at the original animation endpoints",
                HandLayoutMetrics.liftProgress(0f) == 0f && HandLayoutMetrics.liftProgress(1f) == 1f);
        assertTrue("Midpoint is halfway through translation and scale", HandLayoutMetrics.liftProgress(.5f) == .5f);
        assertTrue("Lift starts slowly and decelerates symmetrically",
                Math.abs(HandLayoutMetrics.liftProgress(.2f) - .0954915f) < .000001f
                && Math.abs(HandLayoutMetrics.liftProgress(.8f) - .9045085f) < .000001f);
        assertTrue("The selected card remains below neighbouring cards",
                HandLayoutMetrics.SELECTED_Z_DP < HandLayoutMetrics.NEIGHBOR_Z_DP);
    }

    @Test public void completeFaceAndLiftRemainInsideEachSlotAtFractionalDensities() {
        float[] densities = {1f, 1.5f, 2f, 2.25f, 2.625f, 2.75f, 3.5f};
        int[] clearances = {16, 28};
        for (float density : densities) for (int clearanceDp : clearances) {
            int facePx = Math.round(HandLayoutMetrics.CARD_HEIGHT_DP * density);
            int clearancePx = Math.round(clearanceDp * density);
            int slotPx = HandLayoutMetrics.slotHeightPx(density, clearanceDp);

            atf.gradle.audit.GradleInputs.fixture("Card slot contains separately rounded face and lift",
                    "density", density, "clearanceDp", clearanceDp, "facePx", facePx,
                    "clearancePx", clearancePx, "slotPx", slotPx);
            assertEquals("The complete face and lift fit without a one-pixel rounding loss",
                    facePx + clearancePx, slotPx);
        }
        float selectedScaleOverflowDp = (HandLayoutMetrics.CARD_LIFT_SCALE - 1f)
                * HandLayoutMetrics.CARD_HEIGHT_DP / 2f;
        float selectedTopDp = HandLayoutMetrics.LIFT_CLEARANCE_DP - HandLayoutMetrics.CARD_LIFT_DP
                - selectedScaleOverflowDp;
        float selectedBottomDp = HandLayoutMetrics.LIFT_CLEARANCE_DP
                + HandLayoutMetrics.CARD_HEIGHT_DP - HandLayoutMetrics.CARD_LIFT_DP
                + selectedScaleOverflowDp;
        assertTrue("The lifted card face stays below the top of its slot", selectedTopDp >= 0);
        assertTrue("The lifted card face and scale stay inside its slot",
                selectedBottomDp <= HandLayoutMetrics.reservedHandHeightDp());
        assertThrows("A nonpositive density is rejected", IllegalArgumentException.class,
                () -> HandLayoutMetrics.slotHeightPx(0f, 28));
        assertThrows("A negative lift region is rejected", IllegalArgumentException.class,
                () -> HandLayoutMetrics.slotHeightPx(1f, -1));
    }
}
