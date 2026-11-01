package atf.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import atf.android.evidence.AndroidScreenshotEvidence;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.ui.UiExtentReportSession;
import atf.testkit.AtfComponentTest;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

@AtfComponentTest
class AndroidInlineScreenshotReportContractTest {
    @Test
    void rendersAndroidPngAsAnInlineUiCheckpoint() throws Exception {
        byte[] png;
        try (var bytes = new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB), "png", bytes);
            png = bytes.toByteArray();
        }
        var screenshot = AndroidScreenshotEvidence.from("contract-checkpoint.png", png);
        Path report = Path.of("target", "android-inline-checkpoint.html").toAbsolutePath();
        Files.createDirectories(report.getParent());
        var settings = AtfReportConfigurationLoader.load().reportingSettings();

        try (var session = new ExtentReportSession(report, settings)) {
            var rawScenario = session.startScenario("Android screenshot reporting contract", "atf.tests",
                    "inline PNG", java.util.List.of("component", "android", "reporting"));
            try (var scenario = UiExtentReportSession.adaptScenario(rawScenario,
                    ReportEvidenceProfile.forScreenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT),
                    "inline PNG")) {
                try (var phase = scenario.startPhase("UI screenshot checkpoint")) {
                    phase.uiValidation(screenshot.title(), "Android emulator", screenshot.base64Png(),
                            java.util.List.of(), AtfReportStatus.PASS);
                    phase.complete(AtfReportStatus.PASS);
                }
                scenario.complete(AtfReportStatus.PASS);
            }
        }

        String html = Files.readString(report);
        assertTrue(html.contains("data:image/png;base64,"),
                "Reporting must emit the standard inline UI image data URI");
        assertTrue(html.contains(screenshot.base64Png()),
                "The screenshot payload must be carried by the rendered report");
        assertTrue(html.contains("contract-checkpoint.png"),
                "The checkpoint title must stay attached to its screenshot");
    }
}
