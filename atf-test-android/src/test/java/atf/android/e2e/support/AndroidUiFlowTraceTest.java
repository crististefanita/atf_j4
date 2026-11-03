package atf.android.e2e.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AndroidUiFlowTraceTest {
    @ParameterizedTest
    @ValueSource(strings = {"exception", "checkpoint", "assertion", "action", "pending"})
    void successfulFinishCannotEraseRecordedFailure(String source, @TempDir Path artifacts) throws Exception {
        AndroidUiFlowTrace trace = new AndroidUiFlowTrace(artifacts, "failed-trace", "Failure precedence",
                "AndroidUiFlowTraceTest", "atf.android.e2e.support", "ro.whistlab.app", List.of("component"));
        if (source.equals("exception")) trace.fail(new IllegalStateException("Original diagnostic"));
        var failedAction = new AtfUiActionRecord(1, "Tap", "Place bid", "accepted", "rejected", 5, AtfReportStatus.FAIL);
        if (source.equals("pending")) trace.recordAction(failedAction);
        else trace.checkpoint("TEST BODY", "THEN", "Keep the observed verdict", "Bidding table", null,
                source.equals("action") ? List.of(failedAction) : List.of(),
                List.of(new AtfAssertion("Bid remains pending", "pending", "observed", !source.equals("assertion"))),
                source.equals("checkpoint") ? AtfReportStatus.FAIL : AtfReportStatus.PASS, List.of());

        trace.finish(AtfReportStatus.PASS);

        assertEquals(AtfReportStatus.FAIL, trace.status());
        if (source.equals("exception")) assertTrue(trace.failure().contains("Original diagnostic"));
        Properties manifest = new Properties();
        try (var input = Files.newInputStream(trace.manifest())) { manifest.load(input); }
        assertEquals("FAIL", manifest.getProperty("flow.status"));
    }

    @Test
    void assertionsKeepCheckpointOrderContextAndVerdicts(@TempDir Path artifacts) throws Exception {
        AndroidUiFlowTrace trace = new AndroidUiFlowTrace(artifacts, "trace-contract",
                "Trace assertion contract", "AndroidUiFlowTraceTest", "atf.android.e2e.support",
                "ro.whistlab.app", List.of("android", "component"));
        trace.checkpoint("SETUP", "GIVEN", "Open the bidding screen", "New game", null,
                List.of(), List.of(new AtfAssertion("Game setup is visible", "New game", "New game", true)),
                AtfReportStatus.PASS, List.of());
        trace.checkpoint("TEST BODY", "THEN", "Keep the selected bid", "Bidding table", null,
                List.of(), List.of(new AtfAssertion("Selection remains pending", "Bid not placed", "Bid placed", false)),
                AtfReportStatus.FAIL, List.of());
        trace.finish(AtfReportStatus.FAIL);

        List<AtfAssertion> assertions = trace.assertions();
        assertEquals(2, assertions.size());
        assertEquals("SETUP · GIVEN · Open the bidding screen · Game setup is visible",
                assertions.get(0).name());
        assertTrue(assertions.get(0).passed());
        assertEquals(AtfReportStatus.PASS, assertions.get(0).resultStatus());
        assertEquals("TEST BODY · THEN · Keep the selected bid · Selection remains pending",
                assertions.get(1).name());
        assertFalse(assertions.get(1).passed());
        assertEquals("Bid placed", assertions.get(1).actual());
        assertEquals(AtfReportStatus.FAIL, assertions.get(1).resultStatus());

        Properties manifest = new Properties();
        try (var input = Files.newInputStream(trace.manifest())) { manifest.load(input); }
        assertEquals("FAIL", manifest.getProperty("flow.status"));
        assertEquals("false", manifest.getProperty("phase.1.step.0.assertion.0.passed"));
        assertEquals("Bid placed", manifest.getProperty("phase.1.step.0.assertion.0.actual"));
    }
}
