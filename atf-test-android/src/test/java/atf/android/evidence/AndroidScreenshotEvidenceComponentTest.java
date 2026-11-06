package atf.android.evidence;

import atf.android.component.AndroidComponentAudit;
import org.junit.jupiter.api.extension.RegisterExtension;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

import atf.reporting.model.ReportScreenshot;
import atf.testkit.AtfComponentTest;
import java.util.Base64;
import org.junit.jupiter.api.Test;

@AtfComponentTest
public class AndroidScreenshotEvidenceComponentTest {
    @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
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
