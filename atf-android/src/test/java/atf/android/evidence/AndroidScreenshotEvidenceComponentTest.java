package atf.android.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import atf.reporting.model.ReportScreenshot;
import atf.testkit.AtfComponentTest;
import java.util.Base64;
import org.junit.jupiter.api.Test;

@AtfComponentTest
public class AndroidScreenshotEvidenceComponentTest {
    @Test
    void mapsPngBytesToTheSharedInlineScreenshotContract() {
        byte[] png = new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 1};

        ReportScreenshot screenshot = AndroidScreenshotEvidence.from("main-menu.png", png);

        assertEquals("main-menu.png", screenshot.title());
        assertEquals(Base64.getEncoder().encodeToString(png), screenshot.base64Png());
        assertTrue(ReportScreenshot.hasPngSignature(screenshot.base64Png()));
    }

    @Test
    void rejectsPayloadsThatAreNotPng() {
        assertThrows(IllegalArgumentException.class,
                () -> AndroidScreenshotEvidence.from("broken.png", new byte[] {1, 2, 3}));
    }
}
