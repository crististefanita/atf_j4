package atf.android.e2e.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.testkit.AtfComponentTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@AtfComponentTest
class AndroidUiFlowReportPublisherTest {
    private static final String ONE_PIXEL_PNG =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";

    @Test
    void publishesEachAndroidCheckpointWithPlaywrightUiTablesAndInlineScreenshot(
            @TempDir Path output)
            throws Exception {
        Path screenshot = output.resolve("bidding-state.png");
        Path hierarchy = output.resolve("bidding-state.xml");
        Files.write(screenshot, Base64.getDecoder().decode(ONE_PIXEL_PNG));
        Files.writeString(hierarchy, "<hierarchy><node text=\"Place bid\"/></hierarchy>");
        AndroidUiFlowTrace trace = new AndroidUiFlowTrace(output, "android-report-contract",
                "Android UI action, screenshot, and assertion contract", "AndroidUiFlowReportPublisherTest",
                "atf.android.e2e.support", "ro.whistlab.app", List.of("android", "ui", "component"));
        trace.recordAction(new AtfUiActionRecord(1, "Tap", "text=New game", "New game is opened",
                "ADB accepted tap at 50,25", 34, AtfReportStatus.PASS));
        trace.recordAction(new AtfUiActionRecord(2, "Select option", "text=6", "Six players are selected",
                "ADB accepted tap at 85,45", 41, AtfReportStatus.PASS));
        trace.checkpoint("SETUP", "GIVEN", "Configure the six-player game", "New game setup", screenshot,
                List.of(new AtfUiActionRecord(3, "Inspect setup state", "New game setup",
                        "Players = 6", "visible label: Players = 6\r\nlayout: compact", 0,
                        AtfReportStatus.PASS, AtfUiEvidenceSource.OBSERVE)),
                List.of(new AtfAssertion("Selected player count is six", "6", "6", true)),
                AtfReportStatus.PASS, List.of(hierarchy));
        trace.finish(AtfReportStatus.PASS);

        Path report = AndroidUiFlowReportPublisher.publish(output.resolve("report.html"),
                "package:/data/app/ro.whistlab.app/base.apk", "emulator-5554", "28",
                "Six-player setup is visible and ready", List.of(screenshot, hierarchy, trace.manifest()), trace);

        String html = Files.readString(report);
        assertTrue(html.contains("GIVEN"));
        assertTrue(html.contains("Configure the six-player game"));
        assertTrue(html.contains("class=\"atf-ui-action-sequence\""));
        assertTrue(html.contains("class=\"atf-ui-action-table"));
        assertTrue(html.contains("Playwright action sequence (2 entries)"));
        assertTrue(html.contains("text=New game"));
        assertTrue(html.contains("class=\"atf-ui-observations\""));
        assertTrue(html.contains("class=\"atf-ui-assertions\""));
        assertTrue(html.contains("Selected player count is six"));
        assertTrue(html.contains("<th>EXPECTED</th><th>ACTUAL</th><th>RESULT</th>"));
        assertTrue(html.contains("visible label: Players = 6"));
        assertTrue(html.contains("href=\"data:image/png;base64," + ONE_PIXEL_PNG + "\""));
        assertTrue(html.contains("data-featherlight=\"image\""));
        int actionDisclosure = html.indexOf("<details class=\"atf-ui-action-sequence\"");
        int screenshotLink = html.indexOf("href=\"data:image/png;base64," + ONE_PIXEL_PNG + "\"");
        int actionDisclosureEnd = html.indexOf("</details>", actionDisclosure);
        assertTrue(actionDisclosure >= 0 && screenshotLink > actionDisclosure
                && screenshotLink < actionDisclosureEnd,
                "The inline screenshot must be inside the matching collapsed action table");
        assertTrue(html.contains("bidding-state.xml"));
        assertTrue(html.contains("<td>FLOW ID</td>"));
        assertTrue(html.contains("<td>CHECKPOINT COUNT</td><td>1</td>"));
        assertTrue(html.contains("<td>INLINE CHECKPOINT SCREENSHOTS</td><td>1</td>"));
        assertTrue(html.contains("android-report-contract"));
        assertFalse(html.contains("<details class=\"atf-ui-action-sequence\" open"));
        assertFalse(html.contains("<details class=\"atf-ui-observations\" open"));
        assertFalse(html.contains("<details class=\"atf-ui-assertions\" open"));
    }

    @Test
    void refusesAssertionValuesThatWouldBeTruncatedInExtent(@TempDir Path output) throws Exception {
        int previewLimit = AtfReportConfigurationLoader.load().reportingSettings().assertionMaxDisplayCharacters();
        Path screenshot = output.resolve("overlong.png");
        Files.write(screenshot, Base64.getDecoder().decode(ONE_PIXEL_PNG));
        AndroidUiFlowTrace trace = validPassingTrace(output, "overlong-assertion", screenshot,
                "x".repeat(previewLimit + 1));
        Path report = output.resolve("overlong-assertion.html");

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> AndroidUiFlowReportPublisher.publish(report, "package:/installed.apk", "emulator-5554",
                        "28", "An assertion would be visually truncated", List.of(), trace));

        assertTrue(failure.getMessage().contains("configured " + previewLimit + " character preview"));
        assertFalse(Files.exists(report), "An unauditable preview must not leave a candidate Extent report");
    }

    @Test
    void refusesPassingCheckpointWithoutItsOwnScreenshot(@TempDir Path output) throws Exception {
        AndroidUiFlowTrace trace = validPassingTrace(output, "missing-step-screenshot", null, "6");
        Path report = output.resolve("missing-step-screenshot.html");

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> AndroidUiFlowReportPublisher.publish(report, "package:/installed.apk", "emulator-5554",
                        "28", "Checkpoint screenshot is absent", List.of(), trace));

        assertTrue(failure.getMessage().contains("has no captured PNG"));
        assertFalse(Files.exists(report), "A screenshot-less checkpoint must not publish as a complete UI flow");
    }

    @Test
    void keepsFailureVisibleWhenNoCheckpointWasCaptured(@TempDir Path output) throws Exception {
        AndroidUiFlowTrace trace = new AndroidUiFlowTrace(output, "android-failed-before-checkpoint",
                "Android UI failure before first checkpoint", "AndroidUiFlowReportPublisherTest",
                "atf.android.e2e.support", "ro.whistlab.app", List.of("android", "ui", "component"));
        trace.recordAction(new AtfUiActionRecord(1, "Tap", "text=Start game", "The game starts",
                "ADB rejected the tap: device disconnected", 27, AtfReportStatus.FAIL));
        trace.fail(new IllegalStateException("device returned no hierarchy"));
        trace.finish(AtfReportStatus.FAIL);

        Path report = AndroidUiFlowReportPublisher.publish(output.resolve("failed-report.html"),
                "package:/data/app/ro.whistlab.app/base.apk", "emulator-5554", "28",
                "Journey did not reach the first UI checkpoint", List.of(trace.manifest()), trace);

        String html = Files.readString(report);
        assertTrue(html.contains("EXECUTION DIAGNOSTICS"));
        assertTrue(html.contains("device returned no hierarchy"));
        assertTrue(html.contains("Android flow stopped after an uncaught failure"));
        assertTrue(html.contains("text=Start game"));
        assertTrue(html.contains("device disconnected"));
        assertFalse(html.contains("Total: 0"));
    }

    private AndroidUiFlowTrace validPassingTrace(Path output, String id, Path screenshot, String actual)
            throws Exception {
        AndroidUiFlowTrace trace = new AndroidUiFlowTrace(output, id,
                "Android checkpoint report preflight", "AndroidUiFlowReportPublisherTest",
                "atf.android.e2e.support", "ro.whistlab.app", List.of("android", "ui", "component"));
        trace.recordAction(new AtfUiActionRecord(1, "Tap", "text=Start game", "Open the game",
                "ADB accepted tap", 23, AtfReportStatus.PASS));
        trace.checkpoint("TEST BODY", "THEN", "Start game is visible", "Whist Lab", screenshot,
                List.of(new AtfUiActionRecord(2, "Inspect state", "Whist Lab", "Start game",
                        "visible: Start game", 0, AtfReportStatus.PASS, AtfUiEvidenceSource.OBSERVE)),
                List.of(new AtfAssertion("Start action remains available", "Start game", actual, true)),
                AtfReportStatus.PASS, List.of());
        trace.finish(AtfReportStatus.PASS);
        return trace;
    }
}
