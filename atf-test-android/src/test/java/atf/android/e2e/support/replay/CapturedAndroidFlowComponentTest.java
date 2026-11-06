package atf.android.e2e.support.replay;

import atf.android.component.AndroidComponentAudit;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.testkit.AtfComponentTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest
public class CapturedAndroidFlowComponentTest {
    @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
    @TempDir Path temporary;
    @Test void immutableReplayRetainsOriginalOwnersValuesVerdictsActionsAndTiming() throws Exception {
        var trace = fixture();byte[] original = Files.readAllBytes(trace.manifest());
        var captured = CapturedAndroidFlow.read(trace.manifest());
        assertEquals(trace.phaseSnapshots(), captured.phaseSnapshots(), "Captured intervals and every typed checkpoint remain original");
        assertEquals(trace.status(), captured.status(), "Original verdict retained");
        assertTrue(captured.tags().contains("offline-republication"), "Republication is explicitly labelled");
        assertArrayEquals(original, Files.readAllBytes(trace.manifest()));
    }
    @Test void missingOrMalformedVerdictsCannotAcquireImplicitPass() throws Exception {
        var trace = fixture();var input = load(trace.manifest());
        for (String key : List.of("flow.status", "phase.0.step.0.assertion.0.passed", "phase.0.step.0.status")) {
            var mutated = new Properties();mutated.putAll(input);mutated.setProperty(key, "unknown");store(mutated, trace.manifest());
            assertThrows(IllegalArgumentException.class, () -> CapturedAndroidFlow.read(trace.manifest()), "Malformed original verdict: " + key);
        }
    }
    @Test void escapingArtifactsAndForeignTimingOwnersAreRefused() throws Exception {
        var trace = fixture();var input = load(trace.manifest());
        input.setProperty("phase.0.step.0.artifact.0", "../outside.xml");store(input, trace.manifest());
        assertThrows(java.io.IOException.class, () -> CapturedAndroidFlow.read(trace.manifest()), "Artifacts stay with their original captured owner");
        input.setProperty("phase.0.step.0.artifact.0", "observed.xml");
        input.setProperty("phase.0.timing.flowId", "another-flow");store(input, trace.manifest());
        assertThrows(IllegalArgumentException.class, () -> CapturedAndroidFlow.read(trace.manifest()), "Timing cannot move between owners");
    }
    private AndroidUiFlowTrace fixture() throws Exception {
        Path png = Files.write(temporary.resolve("observed.png"), new byte[]{1});
        Path xml = Files.writeString(temporary.resolve("observed.xml"), "<hierarchy/>");
        var tick = new java.util.concurrent.atomic.AtomicLong();
        var trace = new AndroidUiFlowTrace(temporary, "captured", "Controlled replay", getClass().getSimpleName(),
                getClass().getPackageName(), "fixture.app", List.of("component"), () -> tick.addAndGet(1000));
        trace.beginPhase("TEST BODY");
        trace.checkpoint("TEST BODY", "THEN", "Captured result", "Fixture", png,
                List.of(new AtfUiActionRecord(1, "Observed", "Fixture", "ready", "ready", 7, AtfReportStatus.PASS, AtfUiEvidenceSource.OBSERVE)),
                List.of(new AtfAssertion("Original comparison", "ready", "ready", true)), AtfReportStatus.PASS, List.of(xml));
        trace.finish(AtfReportStatus.PASS);return trace;
    }
    private static Properties load(Path file) throws Exception {
        var input = new Properties();try (var stream = Files.newInputStream(file)) { input.load(stream); }return input;
    }
    private static void store(Properties input, Path file) throws Exception {
        try (var output = Files.newOutputStream(file)) { input.store(output, "Controlled negative input"); }
    }
}
