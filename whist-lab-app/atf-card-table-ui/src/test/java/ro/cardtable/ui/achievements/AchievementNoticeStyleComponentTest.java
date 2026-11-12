package ro.cardtable.ui.achievements;

import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import org.junit.Test;
import static atf.gradle.audit.assertions.GradleAssertions.assertTrue;

public final class AchievementNoticeStyleComponentTest extends GradleAuditedTest {
    @Test public void lightensTheCardWhileKeepingStrongWhiteTextContrast() {
        int originalSurface = 0xFF1D362B;
        int surface = AchievementNoticeView.SURFACE;
        double brightness = luminance(surface);
        GradleInputs.fixture("Achievement notice surface and white text", "previous surface", originalSurface,
                "current surface", surface, "text", 0xFFFFFFFF);
        assertTrue("The new surface is visibly lighter", brightness - luminance(originalSurface) >= 0.04);
        assertTrue("White title and body text retain at least 7 to 1 contrast", 1.05 / (brightness + 0.05) >= 7);
    }

    private static double luminance(int color) {
        return 0.2126 * channel(color >> 16) + 0.7152 * channel(color >> 8) + 0.0722 * channel(color);
    }

    private static double channel(int color) {
        double encoded = (color & 0xFF) / 255.0;
        return encoded <= 0.04045 ? encoded / 12.92 : Math.pow((encoded + 0.055) / 1.055, 2.4);
    }
}
