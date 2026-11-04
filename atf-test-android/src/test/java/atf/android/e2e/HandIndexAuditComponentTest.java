package atf.android.e2e;

import atf.android.whist.evidence.HandIndexAudit;
import atf.android.whist.evidence.HandIndexAudit.Box;
import atf.testkit.AtfComponentTest;
import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class HandIndexAuditComponentTest {
    private static final Box INDEX = new Box(10, 10, 30, 50);
    private static final Box SLOT = new Box(10, 10, 32, 90);

    @Test void visibleRankAndSuitRequireInkInBothBandsAndClearRightPadding() {
        var image = sample(true);
        assertTrue(HandIndexAudit.inspect(image, 100, 100, INDEX, SLOT).readable());
        image.setRGB(29, 35, Color.BLACK.getRGB());
        assertFalse(HandIndexAudit.inspect(image, 100, 100, INDEX, SLOT).readable(), "cut glyph edge cannot pass");
    }

    @Test void accessibilityLabelCannotHideMissingSuitOrAnOccludedIndex() {
        assertFalse(HandIndexAudit.inspect(sample(false), 100, 100, INDEX, SLOT).readable());
        assertFalse(HandIndexAudit.inspect(sample(true), 100, 100, INDEX, new Box(10, 10, 24, 90)).readable());
    }

    @Test void letterboxedCaptureMapsTheLogicalIndexBeforeInspectingPixels() {
        var source = sample(true);
        var capture = new BufferedImage(100, 200, BufferedImage.TYPE_INT_RGB);
        var graphics = capture.createGraphics();
        graphics.drawImage(source, 0, 50, null);
        graphics.dispose();
        assertTrue(HandIndexAudit.inspect(capture, 100, 100, INDEX, SLOT).readable());
        assertThrows(IllegalArgumentException.class, () -> HandIndexAudit.inspect(capture, 0, 100, INDEX, SLOT));
    }

    private static BufferedImage sample(boolean suit) {
        var image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        g.setColor(Color.WHITE); g.fillRect(0, 0, 100, 100);
        g.setColor(Color.BLACK); g.fillRect(15, 20, 8, 8);
        if (suit) g.fillRect(16, 37, 6, 6);
        g.dispose();
        return image;
    }
}
