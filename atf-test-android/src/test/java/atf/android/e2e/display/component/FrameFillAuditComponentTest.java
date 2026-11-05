package atf.android.e2e.display.component;

import atf.android.e2e.display.support.ResponsiveGeometry;
import atf.android.e2e.display.profile.ResponsiveDisplayProfile;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.testkit.AtfComponentTest;
import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class FrameFillAuditComponentTest {
    private static final int IVORY = 0xF1E6C8;
    private static final int GREEN = 0x285C4C;
    private static final ResponsiveDisplayProfile PROFILE = new ResponsiveDisplayProfile(720, 1280, 360);
    private static final AndroidUiDriver.Bounds FRAME = new AndroidUiDriver.Bounds(40, 80, 240, 200);

    @Test void textAtTheFormerSinglePixelDoesNotChangeTheBackgroundVerdict() {
        var image = frame(IVORY, 1);
        var graphics = image.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(58, 120, 12, 40);
        graphics.dispose();
        assertNotEquals(IVORY, image.getRGB(58, 140) & 0xFFFFFF);
        assertEquals(IVORY, ResponsiveGeometry.sampleFrameFill(image, FRAME, PROFILE));
    }

    @Test void differentFrameBackgroundsRemainDifferent() {
        assertNotEquals(ResponsiveGeometry.sampleFrameFill(frame(IVORY, 1), FRAME, PROFILE),
                ResponsiveGeometry.sampleFrameFill(frame(GREEN, 1), FRAME, PROFILE));
    }

    @Test void logicalBoundsMapToThePhysicalCaptureBeforeSampling() {
        assertEquals(IVORY, ResponsiveGeometry.sampleFrameFill(frame(IVORY, 2), FRAME, PROFILE));
    }

    private static BufferedImage frame(int color, int scale) {
        var image = new BufferedImage(720 * scale, 1280 * scale, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(new Color(color));
        graphics.fillRect(FRAME.left() * scale, FRAME.top() * scale,
                (FRAME.right() - FRAME.left()) * scale, (FRAME.bottom() - FRAME.top()) * scale);
        graphics.dispose();
        return image;
    }
}
