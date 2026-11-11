package ro.cardtable.ui.hand;

import org.junit.Test;
import ro.cardtable.ui.cards.CardFaceMetrics;
import static atf.gradle.audit.assertions.GradleAssertions.*;

public final class HandGeometryComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void inheritedPortraitDimensionsAndOneCardCenterStayExact() {
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 63, "declared constant 2", 88, "declared constant 3", 116, "declared constant 4", 1, "declared constant 5", 360, "declared constant 6", 1f, "declared constant 7", 18, "declared constant 8", 148, "declared constant 9", 0);
        assertEquals("CardFaceMetrics.atHeight(88).widthDp() must equal 63", 63,CardFaceMetrics.atHeight(88).widthDp());
        assertEquals("HandLayoutMetrics.cardHeightDp() must equal 88", 88,HandLayoutMetrics.cardHeightDp());assertEquals("HandLayoutMetrics.reservedHandHeightDp() must equal 116", 116,HandLayoutMetrics.reservedHandHeightDp());
        var single=HandLayoutMetrics.fan(1,360,1f,1f,18);
        assertEquals("single.width() must equal 360", 360,single.width());assertEquals("single.first() must equal 148", 148,single.first());assertEquals("single.cardWidth() must equal 63", 63,single.cardWidth());
        assertEquals("single.left(0) must equal 148", 148,single.left(0));
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
