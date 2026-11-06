package ro.whistlab.app.score.layout;

import static org.junit.Assert.*;
import org.junit.Test;

public final class HistoryViewportComponentTest {
    @Test public void wrappedLegendAndHeadersRetainTheirWholeMeasuredHeight() {
        assertEquals(280, HistoryViewport.bodyHeight(500, 920, 700));
        assertEquals(500, HistoryViewport.bodyHeight(500, 920, 1400));
        assertEquals(0, HistoryViewport.bodyHeight(500, 920, 300));
    }

    @Test public void eachNewMeasurementCanGrowAgainAfterACompactWindow() {
        assertEquals(120, HistoryViewport.bodyHeight(380, 560, 300));
        assertEquals(380, HistoryViewport.bodyHeight(380, 560, 900));
        assertEquals(80, HistoryViewport.bodyHeight(380, 600, 300));
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
