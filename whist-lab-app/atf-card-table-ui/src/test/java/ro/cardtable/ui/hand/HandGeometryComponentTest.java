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
}
