package ro.whistlab.app.score.layout;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import org.junit.Test;

public final class HistoryViewportComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void wrappedLegendAndHeadersRetainTheirWholeMeasuredHeight() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 280, "declared constant 2", 500, "declared constant 3", 920, "declared constant 4", 700, "declared constant 5", 980, "declared constant 6", 1400, "declared constant 7", 0, "declared constant 8", 300);
        assertEquals("HistoryViewport.bodyHeight(500, 920, 700) must equal 280", 280, HistoryViewport.bodyHeight(500, 920, 700));
        assertEquals("HistoryViewport.bodyHeight(500, 920, 1400) must equal 980", 980, HistoryViewport.bodyHeight(500, 920, 1400));
        assertEquals("HistoryViewport.bodyHeight(500, 920, 300) must equal 0", 0, HistoryViewport.bodyHeight(500, 920, 300));
    }

    @Test public void eachNewMeasurementCanGrowAgainAfterACompactWindow() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 120, "declared constant 2", 380, "declared constant 3", 560, "declared constant 4", 300, "declared constant 5", 720, "declared constant 6", 900, "declared constant 7", 80, "declared constant 8", 600);
        assertEquals("HistoryViewport.bodyHeight(380, 560, 300) must equal 120", 120, HistoryViewport.bodyHeight(380, 560, 300));
        assertEquals("HistoryViewport.bodyHeight(380, 560, 900) must equal 720", 720, HistoryViewport.bodyHeight(380, 560, 900));
        assertEquals("HistoryViewport.bodyHeight(380, 600, 300) must equal 80", 80, HistoryViewport.bodyHeight(380, 600, 300));
    }

    @Test public void dialogUsesTheUsableWindowWithSmallMarginsAndSurvivesResizing() {
        for (float density : new float[]{1, 1.5f, 2, 2.625f, 3.5f}) {
            for (int[] window : new int[][]{{320, 480}, {678, 1392}, {1080, 2200}, {1536, 580}, {1600, 2400}}) {
                var size = HistoryViewport.dialogSize(window[0], window[1], density);

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "density", density, "size", size, "window", window);
                assertTrue("size.width() > window[0] * 0.94 && size.width() < window[0] must hold", size.width() > window[0] * .94 && size.width() < window[0]);
                assertTrue("size.height() > window[1] * 0.95 && size.height() < window[1] must hold", size.height() > window[1] * .95 && size.height() < window[1]);
                assertTrue("(window[0] - size.width()) / 2.0F <= 24 * density + 1 must hold", (window[0] - size.width()) / 2f <= 24 * density + 1);
                assertTrue("(window[1] - size.height()) / 2.0F <= 24 * density + 1 must hold", (window[1] - size.height()) / 2f <= 24 * density + 1);
            }
        }
        assertEquals("HistoryViewport.dialogSize(678, 1392, 2) must equal new HistoryViewport.DialogSize(644, 1336)", new HistoryViewport.DialogSize(644, 1336), HistoryViewport.dialogSize(678, 1392, 2));
        assertEquals("HistoryViewport.dialogSize(1536, 580, 2) must equal new HistoryViewport.DialogSize(1460, 556)", new HistoryViewport.DialogSize(1460, 556), HistoryViewport.dialogSize(1536, 580, 2));
    }

    @Test public void invalidWindowsFailBeforeNativeWindowSizing() {
        for (float density : new float[]{0, -1, Float.NaN, Float.POSITIVE_INFINITY})
            {
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "density", density);
assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> HistoryViewport.dialogSize(678, 1392, density)); }
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> HistoryViewport.dialogSize(0, 1392, 2));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> HistoryViewport.dialogSize(678, -1, 2));
    }

    @Test public void aPartialRowSwitchesToWholeContentScrollingAndAGrowingWindowRestoresPinnedHeaders() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 380, "declared constant 2", 540, "declared constant 3", 199, "declared constant 4", 40, "declared constant 5", 200, "declared constant 6", 900, "declared constant 7", 640, "declared constant 8", 280, "declared constant 9", 60, "declared constant 10", 0);
        assertTrue("HistoryViewport.needsWholeScroll(380, 540, 199, 40) must hold", HistoryViewport.needsWholeScroll(380, 540, 199, 40));
        assertFalse("HistoryViewport.needsWholeScroll(380, 540, 200, 40) must remain false", HistoryViewport.needsWholeScroll(380, 540, 200, 40));
        assertFalse("HistoryViewport.needsWholeScroll(380, 540, 900, 40) must remain false", HistoryViewport.needsWholeScroll(380, 540, 900, 40));
        assertTrue("HistoryViewport.needsWholeScroll(380, 640, 280, 60) must hold", HistoryViewport.needsWholeScroll(380, 640, 280, 60));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> HistoryViewport.needsWholeScroll(380, 540, 199, 0));
    }

    @Test public void densityAndTextGrowthNeverConsumeTheLegendBudget() {
        for (float density : new float[]{1, 1.5f, 2.25f, 2.625f, 2.75f, 3.5f}) {
            int preferred = Math.round(380 * density), available = Math.round(420 * density);
            int previous = Integer.MAX_VALUE;
            for (float scale : new float[]{1, 1.15f, 1.3f, 1.5f}) {
                int headers = Math.round(80 * density * scale), legend = Math.round(64 * density * scale);
                int body = HistoryViewport.bodyHeight(preferred, preferred + headers + legend, available);

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "available", available, "body", body, "density", density, "headers", headers, "legend", legend, "preferred", preferred, "previous", previous, "scale", scale);
                assertTrue("body + headers + legend <= available must hold", body + headers + legend <= available);
                assertTrue("body <= previous must hold", body <= previous);
                assertTrue("body > 0 must hold", body > 0);
                previous = body;
            }
        }
    }

    @Test public void zeroAndMaximumPixelDimensionsRemainBoundedWithoutOverflow() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 0, "declared constant 2", 1);
        assertEquals("HistoryViewport.bodyHeight(0, 0, 0) must equal 0", 0, HistoryViewport.bodyHeight(0, 0, 0));
        assertEquals("HistoryViewport.bodyHeight(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE) must equal Integer.MAX_VALUE", Integer.MAX_VALUE, HistoryViewport.bodyHeight(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals("HistoryViewport.bodyHeight(1, Integer.MAX_VALUE, 0) must equal 0", 0, HistoryViewport.bodyHeight(1, Integer.MAX_VALUE, 0));
    }

    @Test public void invalidMeasurementsFailBeforeLayout() {
        for (int[] values : new int[][]{{-1, 1, 1}, {2, 1, 1}, {1, 1, -1}})
            {
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "values", values);
assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> HistoryViewport.bodyHeight(values[0], values[1], values[2])); }
    }
}
