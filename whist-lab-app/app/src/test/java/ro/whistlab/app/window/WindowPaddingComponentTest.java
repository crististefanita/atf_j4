package ro.whistlab.app.window;

import org.junit.Test;
import static atf.gradle.audit.assertions.GradleAssertions.*;

public class WindowPaddingComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void repeatedInsetsPreserveBaseSpacingWithoutAccumulation() {
        var base = new WindowPadding(12, 2, 12, 2);
        var bars = new WindowPadding(0, 24, 0, 48);
        for (int dispatch = 0; dispatch < 100; dispatch++) {

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "bars", bars, "base", base, "dispatch", dispatch);
            assertEquals("base.plus(bars) must equal new WindowPadding(12, 26, 12, 50)", new WindowPadding(12, 26, 12, 50), base.plus(bars));
        }
        assertEquals("base must equal new WindowPadding(12, 2, 12, 2)", new WindowPadding(12, 2, 12, 2), base);
    }

    @Test public void cutoutAndKeyboardProtectionKeepTheOriginalDecorativePadding() {
        var base = new WindowPadding(12, 2, 12, 2);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "base", base);
        assertEquals("base.plus(new WindowPadding(44, 24, 0, 300)) must equal new WindowPadding(56, 26, 12, 302)", new WindowPadding(56, 26, 12, 302), base.plus(new WindowPadding(44, 24, 0, 300)));
        assertEquals("base.plus(new WindowPadding(0, 24, 0, 48)) must equal new WindowPadding(12, 26, 12, 50)", new WindowPadding(12, 26, 12, 50), base.plus(new WindowPadding(0, 24, 0, 48)));
    }

    @Test public void compactWindowBudgetExcludesSystemBarsBeforeChoosingTheTablePolicy() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 497, "declared constant 2", 1280, "declared constant 3", 54, "declared constant 4", 108, "declared constant 5", 2.25f, "declared constant 6", 320, "declared constant 7", 720, "declared constant 8", 0, "declared constant 9", 328, "declared constant 10", 1080, "declared constant 11", 96, "declared constant 12", 3f);
        assertEquals("WindowPadding.usableDp(1280, 54, 108, 2.25F) must equal 497", 497, WindowPadding.usableDp(1280, 54, 108, 2.25f));
        assertEquals("WindowPadding.usableDp(720, 0, 0, 2.25F) must equal 320", 320, WindowPadding.usableDp(720, 0, 0, 2.25f));
        assertEquals("WindowPadding.usableDp(1080, 96, 0, 3.0F) must equal 328", 328, WindowPadding.usableDp(1080, 96, 0, 3f));
    }

    @Test public void invalidObstructionsAndOverflowAreRejected() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", -1, "declared constant 2", 0, "declared constant 3", 100, "declared constant 4", 60, "declared constant 5", 40, "declared constant 6", 1f, "declared constant 7", 1);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> new WindowPadding(-1, 0, 0, 0));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> WindowPadding.usableDp(100, 60, 40, 1f));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> WindowPadding.usableDp(100, 0, 0, Float.NaN));
        assertThrows("The operation rejects its input with ArithmeticException.class", ArithmeticException.class,
                () -> new WindowPadding(Integer.MAX_VALUE, 0, 0, 0).plus(new WindowPadding(1, 0, 0, 0)));
    }
}
