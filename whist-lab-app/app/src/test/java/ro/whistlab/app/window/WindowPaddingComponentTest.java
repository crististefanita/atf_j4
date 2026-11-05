package ro.whistlab.app.window;

import org.junit.Test;
import static org.junit.Assert.*;

public class WindowPaddingComponentTest {
    @Test public void repeatedInsetsPreserveBaseSpacingWithoutAccumulation() {
        var base = new WindowPadding(12, 2, 12, 2);
        var bars = new WindowPadding(0, 24, 0, 48);
        for (int dispatch = 0; dispatch < 100; dispatch++) {
            assertEquals(new WindowPadding(12, 26, 12, 50), base.plus(bars));
        }
        assertEquals(new WindowPadding(12, 2, 12, 2), base);
    }

    @Test public void cutoutAndKeyboardProtectionKeepTheOriginalDecorativePadding() {
        var base = new WindowPadding(12, 2, 12, 2);
        assertEquals(new WindowPadding(56, 26, 12, 302), base.plus(new WindowPadding(44, 24, 0, 300)));
        assertEquals(new WindowPadding(12, 26, 12, 50), base.plus(new WindowPadding(0, 24, 0, 48)));
    }

    @Test public void compactWindowBudgetExcludesSystemBarsBeforeChoosingTheTablePolicy() {
        assertEquals(497, WindowPadding.usableDp(1280, 54, 108, 2.25f));
        assertEquals(320, WindowPadding.usableDp(720, 0, 0, 2.25f));
        assertEquals(328, WindowPadding.usableDp(1080, 96, 0, 3f));
    }

    @Test public void invalidObstructionsAndOverflowAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new WindowPadding(-1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> WindowPadding.usableDp(100, 60, 40, 1f));
        assertThrows(IllegalArgumentException.class, () -> WindowPadding.usableDp(100, 0, 0, Float.NaN));
        assertThrows(ArithmeticException.class,
                () -> new WindowPadding(Integer.MAX_VALUE, 0, 0, 0).plus(new WindowPadding(1, 0, 0, 0)));
    }
}
