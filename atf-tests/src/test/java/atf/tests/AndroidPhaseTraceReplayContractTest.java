package atf.tests;

import atf.testkit.AtfComponentTest;

import static org.junit.jupiter.api.Assertions.*;
import atf.android.evidence.timing.AndroidPhaseTiming;
import atf.android.evidence.timing.AndroidPhaseTimingCodec;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@AtfComponentTest
public class AndroidPhaseTraceReplayContractTest {
    @ParameterizedTest @ValueSource(strings = {"extent", "fallback"})
    void republicationKeepsOriginalIntervalsRatherThanDispatchOrPublication(String renderer, @TempDir Path output) throws Exception {
        Path manifest = fixture(output);
        byte[] original = Files.readAllBytes(manifest);
        Path report = output.resolve(renderer + ".html");
        try (var session = session(renderer, report)) { DeepSuiteExtentReportTest.publishUiFlow(session, manifest, output); }
        assertArrayEquals(original, Files.readAllBytes(manifest));
        var html = Jsoup.parse(Files.readString(report));
        assertEquals(List.of("3700000000", "800000000"), html.select("[data-atf-execution-duration-nanos]").eachAttr("data-atf-execution-duration-nanos"));
        assertTrue(html.text().toLowerCase(java.util.Locale.ROOT).contains("original trace persistence"));
        var comparisons = html.select(renderer.equals("extent") ? ".atf-ui-assertions tbody tr" : "table.assertions tbody tr");
        assertEquals(2, comparisons.size());
        for (var comparison : comparisons) assertTrue(comparison.text().contains("Captured state") && comparison.text().contains("ready"));
        String review = System.getProperty("atf.android.timingReviewDirectory", "");
        if (!review.isBlank()) {
            Path directory = Path.of(review); Files.createDirectories(directory);
            Files.copy(report, directory.resolve("replay-" + renderer + ".html"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @ParameterizedTest
    @CsvSource({"extent,owner", "fallback,owner", "extent,scalar", "fallback,scalar", "extent,flow", "fallback,flow"})
    void mutatedTimingCannotAcquireAnotherPhaseOrSilentlyBecomeLegacy(String renderer, String mutation, @TempDir Path output) throws Exception {
        Path manifest = fixture(output); var properties = load(manifest);
        String key = "phase.1.timing.";
        if (mutation.equals("owner")) properties.setProperty(key + "phaseName", "SETUP");
        else if (mutation.equals("flow")) properties.setProperty(key + "flowId", "unrelated-flow");
        else properties.remove(key + "interval.0.endedNanos");
        store(properties, manifest);
        try (var session = session(renderer, output.resolve("refused.html"))) {
            assertThrows(IllegalArgumentException.class, () -> DeepSuiteExtentReportTest.publishUiFlow(session, manifest, output));
        }
    }

    private static AtfReportPorts.Session session(String renderer, Path report) {
        var settings = AtfReportConfigurationLoader.load().reportingSettings();
        return renderer.equals("extent") ? new ExtentReportSession(report, settings) : new HtmlReportSession(report, settings);
    }

    private static Path fixture(Path output) throws Exception {
        var p = new Properties(); p.setProperty("format.version", "1");
        p.setProperty("flow.id", "captured-timing"); p.setProperty("flow.name", "Controlled captured phase replay");
        p.setProperty("flow.testClass", "AndroidPhaseTraceReplayContractTest"); p.setProperty("flow.testPackage", "atf.tests");
        p.setProperty("flow.appPackage", "controlled.synthetic"); p.setProperty("flow.status", "PASS"); p.setProperty("flow.phaseCount", "2");
        for (int index = 0; index < 2; index++) {
            String prefix = "phase." + index, name = index == 0 ? "SETUP" : "TEST BODY";
            p.setProperty(prefix + ".name", name); p.setProperty(prefix + ".stepCount", "1");
            String step = prefix + ".step.0";
            p.setProperty(step + ".keyword", "THEN"); p.setProperty(step + ".title", "Same captured checkpoint title");
            p.setProperty(step + ".target", "Captured state"); p.setProperty(step + ".status", "PASS");
            p.setProperty(step + ".artifactCount", "0"); p.setProperty(step + ".actionCount", "1"); p.setProperty(step + ".assertionCount", "1");
            String action = step + ".action.0";
            p.setProperty(action + ".sequence", "1"); p.setProperty(action + ".operation", "Tap"); p.setProperty(action + ".target", "Continue");
            p.setProperty(action + ".expected", "accepted"); p.setProperty(action + ".observed", "accepted");
            p.setProperty(action + ".durationMillis", "7"); p.setProperty(action + ".status", "PASS"); p.setProperty(action + ".source", "ACTION");
            String assertion = step + ".assertion.0";
            p.setProperty(assertion + ".name", "Captured state"); p.setProperty(assertion + ".expected", "ready"); p.setProperty(assertion + ".actual", "ready");
            p.setProperty(assertion + ".passed", "true"); p.setProperty(assertion + ".status", "PASS");
            var interval = index == 0 ? new AndroidPhaseTiming.Interval(0, 4_000_000_000L, 200_000_000L, 100_000_000L)
                    : new AndroidPhaseTiming.Interval(4_000_000_000L, 5_000_000_000L, 100_000_000L, 100_000_000L);
            AndroidPhaseTimingCodec.write(p, prefix + ".timing", new AndroidPhaseTiming("captured-timing", name, index, List.of(interval), false));
        }
        Path manifest = output.resolve("captured-flow.properties"); store(p, manifest); return manifest;
    }
    private static Properties load(Path path) throws Exception {
        var p = new Properties(); try (var input = Files.newInputStream(path)) { p.load(input); } return p;
    }
    private static void store(Properties p, Path path) throws Exception {
        try (var output = Files.newOutputStream(path)) { p.store(output, "Controlled captured timing fixture; no device"); }
    }
}
