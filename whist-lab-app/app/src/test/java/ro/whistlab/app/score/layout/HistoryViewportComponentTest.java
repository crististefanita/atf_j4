package ro.whistlab.app.score.layout;

import static org.junit.Assert.*;
import org.junit.Test;

public final class HistoryViewportComponentTest {
    @Test public void wrappedLegendAndHeadersRetainTheirWholeMeasuredHeight() {
        assertEquals(280, HistoryViewport.bodyHeight(500, 920, 700));
        assertEquals(980, HistoryViewport.bodyHeight(500, 920, 1400));
        assertEquals(0, HistoryViewport.bodyHeight(500, 920, 300));
    }

    @Test public void eachNewMeasurementCanGrowAgainAfterACompactWindow() {
        assertEquals(120, HistoryViewport.bodyHeight(380, 560, 300));
        assertEquals(720, HistoryViewport.bodyHeight(380, 560, 900));
        assertEquals(80, HistoryViewport.bodyHeight(380, 600, 300));
    }

    @Test public void dialogUsesTheUsableWindowWithSmallMarginsAndSurvivesResizing() {
        for (float density : new float[]{1, 1.5f, 2, 2.625f, 3.5f}) {
            for (int[] window : new int[][]{{320, 480}, {678, 1392}, {1080, 2200}, {1536, 580}, {1600, 2400}}) {
                var size = HistoryViewport.dialogSize(window[0], window[1], density);
                assertTrue(size.width() > window[0] * .94 && size.width() < window[0]);
                assertTrue(size.height() > window[1] * .95 && size.height() < window[1]);
                assertTrue((window[0] - size.width()) / 2f <= 24 * density + 1);
                assertTrue((window[1] - size.height()) / 2f <= 24 * density + 1);
            }
        }
        assertEquals(new HistoryViewport.DialogSize(644, 1336), HistoryViewport.dialogSize(678, 1392, 2));
        assertEquals(new HistoryViewport.DialogSize(1460, 556), HistoryViewport.dialogSize(1536, 580, 2));
    }

    @Test public void invalidWindowsFailBeforeNativeWindowSizing() {
        for (float density : new float[]{0, -1, Float.NaN, Float.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class, () -> HistoryViewport.dialogSize(678, 1392, density));
        assertThrows(IllegalArgumentException.class, () -> HistoryViewport.dialogSize(0, 1392, 2));
        assertThrows(IllegalArgumentException.class, () -> HistoryViewport.dialogSize(678, -1, 2));
    }

    @Test public void aPartialRowSwitchesToWholeContentScrollingAndAGrowingWindowRestoresPinnedHeaders() {
        assertTrue(HistoryViewport.needsWholeScroll(380, 540, 199, 40));
        assertFalse(HistoryViewport.needsWholeScroll(380, 540, 200, 40));
        assertFalse(HistoryViewport.needsWholeScroll(380, 540, 900, 40));
        assertTrue(HistoryViewport.needsWholeScroll(380, 640, 280, 60));
        assertThrows(IllegalArgumentException.class, () -> HistoryViewport.needsWholeScroll(380, 540, 199, 0));
    }

    @Test public void densityAndTextGrowthNeverConsumeTheLegendBudget() {
        for (float density : new float[]{1, 1.5f, 2.25f, 2.625f, 2.75f, 3.5f}) {
            int preferred = Math.round(380 * density), available = Math.round(420 * density);
            int previous = Integer.MAX_VALUE;
            for (float scale : new float[]{1, 1.15f, 1.3f, 1.5f}) {
                int headers = Math.round(80 * density * scale), legend = Math.round(64 * density * scale);
                int body = HistoryViewport.bodyHeight(preferred, preferred + headers + legend, available);
                assertTrue(body + headers + legend <= available);
                assertTrue(body <= previous);
                assertTrue(body > 0);
                previous = body;
            }
        }
    }

    @Test public void zeroAndMaximumPixelDimensionsRemainBoundedWithoutOverflow() {
        assertEquals(0, HistoryViewport.bodyHeight(0, 0, 0));
        assertEquals(Integer.MAX_VALUE, HistoryViewport.bodyHeight(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(0, HistoryViewport.bodyHeight(1, Integer.MAX_VALUE, 0));
    }

    @Test public void invalidMeasurementsFailBeforeLayout() {
        for (int[] values : new int[][]{{-1, 1, 1}, {2, 1, 1}, {1, 1, -1}})
            assertThrows(IllegalArgumentException.class, () -> HistoryViewport.bodyHeight(values[0], values[1], values[2]));
    }
}
