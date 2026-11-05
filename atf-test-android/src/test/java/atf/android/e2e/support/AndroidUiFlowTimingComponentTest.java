package atf.android.e2e.support;

import atf.testkit.AtfComponentTest;

import static org.junit.jupiter.api.Assertions.*;
import atf.android.evidence.timing.AndroidPhaseTimingCodec;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@AtfComponentTest
class AndroidUiFlowTimingComponentTest {
    private static final String PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";

    @Test void individualPublicationUsesCapturedPhaseIntervalsAndPreservesFullPngs(@TempDir Path output) throws Exception {
        var ticks = new AtomicLong();
        var trace = trace(output, ticks);
        Path png = output.resolve("whole-fixture.png"); Files.write(png, Base64.getDecoder().decode(PNG));
        checkpoint(trace, ticks, png, "SETUP", 100, 40, 5);
        checkpoint(trace, ticks, png, "TEST BODY", 1100, 60, 8);
        checkpoint(trace, ticks, png, "TEARDOWN", 70, 20, 0);
        trace.finish(AtfReportStatus.PASS);
        var stored = load(trace.manifest());
        assertEquals(100_000_000L, AndroidPhaseTimingCodec.read(stored, "phase.0.timing", trace.id(), "SETUP", 0).orElseThrow().executionNanos());
        assertEquals(1_100_000_000L, AndroidPhaseTimingCodec.read(stored, "phase.1.timing", trace.id(), "TEST BODY", 1).orElseThrow().executionNanos());
        assertEquals(70_000_000L, AndroidPhaseTimingCodec.read(stored, "phase.2.timing", trace.id(), "TEARDOWN", 2).orElseThrow().executionNanos());
        byte[] original = Files.readAllBytes(trace.manifest()); ticks.addAndGet(90_000_000_000L);
        var projection = new AtomicLong();
        Path report = AndroidUiFlowReportPublisher.publish(output.resolve("timed.html"), "fixture", "mock-device", "28",
                "Controlled component fixture; no Android execution", List.of(png), trace, () -> projection.getAndAdd(7_000_000L));
        assertArrayEquals(original, Files.readAllBytes(trace.manifest()));
        var document = Jsoup.parse(Files.readString(report));
        assertEquals(List.of("100000000", "1100000000", "70000000"), document.select("[data-atf-execution-duration-nanos]").eachAttr("data-atf-execution-duration-nanos"));
        assertEquals(3, document.select("[data-atf-execution-duration-nanos]").size());
        String text = document.text().toLowerCase(java.util.Locale.ROOT);
        assertTrue(text.contains("checkpoint projection before final html flush"));
        assertTrue(text.contains("original png and hierarchy capture"));
        assertEquals(3, document.select("a[data-atf-screenshot],a[data-atf-screenshot-ref]").size());
        String inline = document.selectFirst("a[data-atf-screenshot]").attr("href");
        assertArrayEquals(Base64.getDecoder().decode(PNG), Base64.getDecoder().decode(inline.substring(inline.indexOf(',') + 1)));
        assertEquals(3, document.select(".atf-ui-assertions tbody tr").size(), "Timing observations add no comparisons");
        String configuredPreview = Integer.toString(AtfReportConfigurationLoader.load()
                .reportingSettings().assertionMaxDisplayCharacters());
        assertEquals(List.of(configuredPreview, configuredPreview, configuredPreview),
                document.select(".atf-ui-assertions").eachAttr("data-atf-preview-limit"),
                "Every checkpoint must publish the active assertion preview limit");
        preserve(report, "individual.html");
    }

    @Test void movingOnlyATimingMarkerBetweenSameTitleSiblingsIsRejected(@TempDir Path output) throws Exception {
        var ticks = new AtomicLong(); var trace = trace(output, ticks);
        Path png = output.resolve("original.png"); Files.write(png, Base64.getDecoder().decode(PNG));
        checkpoint(trace, ticks, png, "SETUP", 100, 40, 5);
        checkpoint(trace, ticks, png, "TEST BODY", 1100, 60, 8); trace.finish(AtfReportStatus.PASS);
        Path report = AndroidUiFlowReportPublisher.publish(output.resolve("mutation.html"), "fixture", "mock-device", "28", "Controlled fixture", List.of(png), trace);
        var document = Jsoup.parse(Files.readString(report)); var markers = document.select("[data-atf-execution-duration-nanos]");
        markers.get(0).attr("data-atf-execution-duration-nanos", "1100000000"); markers.get(1).attr("data-atf-execution-duration-nanos", "100000000");
        Files.writeString(report, document.outerHtml());
        var error = assertThrows(IllegalStateException.class, () -> AndroidUiFlowReportPublisher.auditPublishedReport(report, trace,
                AtfReportConfigurationLoader.load().reportingSettings()));
        assertTrue(error.getMessage().contains("execution interval differs"));
    }

    @Test void failedPhaseBeforeCheckpointSurvivesLaterTeardownPass(@TempDir Path output) throws Exception {
        var ticks = new AtomicLong(); var trace = trace(output, ticks);
        Path png = output.resolve("original.png"); Files.write(png, Base64.getDecoder().decode(PNG));
        trace.beginPhase("TEST BODY"); ticks.addAndGet(170_000_000L);
        trace.recordAction(new AtfUiActionRecord(1, "Tap", "Continue", "accepted", "ADB rejected", 11, AtfReportStatus.FAIL));
        trace.fail(new IllegalStateException("Original interrupted journey"));
        checkpoint(trace, ticks, png, "TEARDOWN", 300, 20, 7); trace.finish(AtfReportStatus.PASS);
        assertEquals(AtfReportStatus.FAIL, trace.status());
        assertTrue(trace.phaseSnapshots().get(0).timing().orElseThrow().partial());
        assertEquals(170_000_000L, trace.phaseSnapshots().get(0).timing().orElseThrow().executionNanos());
        assertEquals(1, trace.phaseSnapshots().get(1).checkpoints().getFirst().actions().stream().filter(a -> a.source() == AtfUiEvidenceSource.ACTION).count());
        assertEquals("ADB rejected", trace.pendingActions().getFirst().observed());
        Path report = AndroidUiFlowReportPublisher.publish(output.resolve("partial.html"), "fixture", "mock-device", "28", "Interrupted controlled fixture", List.of(png), trace);
        assertTrue(Files.readString(report).contains("Original interrupted journey"));
        preserve(report, "partial-failure.html");
    }

    @Test void legacyCaptureDoesNotInventFullPhaseExecution(@TempDir Path output) throws Exception {
        var ticks = new AtomicLong(); var trace = trace(output, ticks);
        Path png = output.resolve("original.png"); Files.write(png, Base64.getDecoder().decode(PNG));
        trace.checkpoint("TEST BODY", "THEN", "Captured legacy checkpoint", "screen", png,
                List.of(new AtfUiActionRecord(1, "Observe", "screen", "ready", "ready", 0, AtfReportStatus.PASS, AtfUiEvidenceSource.OBSERVE)),
                List.of(new AtfAssertion("State", "ready", "ready", true)), AtfReportStatus.PASS, List.of());
        trace.finish(AtfReportStatus.PASS);
        Path report = AndroidUiFlowReportPublisher.publish(output.resolve("legacy.html"), "fixture", "mock-device", "28", "Legacy controlled fixture", List.of(png), trace);
        var document = Jsoup.parse(Files.readString(report));
        assertTrue(document.select("[data-atf-execution-duration-nanos]").isEmpty());
        assertTrue(document.text().contains("UNAVAILABLE — legacy or unfinished capture"));
        preserve(report, "legacy-unavailable.html");
    }

    private static AndroidUiFlowTrace trace(Path output, AtomicLong ticks) throws Exception {
        return new AndroidUiFlowTrace(output, "phase-timing", "Controlled Android phase timing", "AndroidUiFlowTimingComponentTest",
                "atf.android.e2e.support", "controlled.synthetic", List.of("component", "mock", "timing"), ticks::get);
    }

    @Test void activePhaseCannotBePublishedAsAnUntimedLegacyCapture(@TempDir Path output) throws Exception {
        var trace = trace(output, new AtomicLong()); trace.beginPhase("TEST BODY");
        var failure = assertThrows(IllegalStateException.class, () -> AndroidUiFlowReportPublisher.publish(
                output.resolve("unfinished.html"), "fixture", "mock-device", "28", "Unfinished fixture", List.of(), trace));
        assertTrue(failure.getMessage().contains("unfinished Android phase"));
        assertFalse(Files.exists(output.resolve("unfinished.html")));
    }
    private static void checkpoint(AndroidUiFlowTrace trace, AtomicLong ticks, Path png, String phase, long execution, long capture, long dispatch) throws Exception {
        trace.beginPhase(phase); ticks.addAndGet(execution * 1_000_000L);
        if (dispatch > 0) trace.recordAction(new AtfUiActionRecord(1, "Tap", "Continue", "accepted", "accepted", dispatch, AtfReportStatus.PASS));
        try (var evidence = trace.capture()) { ticks.addAndGet(capture * 1_000_000L); }
        trace.checkpoint(phase, "THEN", "Same checkpoint title", "screen", png,
                List.of(new AtfUiActionRecord(trace.nextActionSequence(), "Inspect", "screen", "ready", "ready", 0, AtfReportStatus.PASS, AtfUiEvidenceSource.OBSERVE)),
                List.of(new AtfAssertion("Same assertion title", "ready", "ready", true)), AtfReportStatus.PASS, List.of());
    }
    private static Properties load(Path file) throws Exception {
        var result = new Properties(); try (var input = Files.newInputStream(file)) { result.load(input); } return result;
    }
    private static void preserve(Path report, String name) throws Exception {
        String directory = System.getProperty("atf.android.timingReviewDirectory", "");
        if (directory.isBlank()) return;
        Path output = Path.of(directory); Files.createDirectories(output);
        Path copy = output.resolve(name); Files.copy(report, copy, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        System.out.println("ANDROID_PHASE_TIMING_REPORT=" + copy);
    }
}
