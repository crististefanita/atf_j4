package atf.android.evidence.timing;

import atf.testkit.AtfUnitTest;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@AtfUnitTest
class AndroidPhaseTimingCodecTest {
    @Test void roundTripRetainsOriginalOffsetsAndAllEvidenceAxes() {
        var source = new AndroidPhaseTiming("flow", "TEST BODY", 1,
                List.of(new AndroidPhaseTiming.Interval(1_000_000_000L, 5_000_000_000L, 200_000_000L, 100_000_000L)), true);
        var properties = new Properties(); AndroidPhaseTimingCodec.write(properties, "phase.1.timing", source);
        var actual = AndroidPhaseTimingCodec.read(properties, "phase.1.timing", "flow", "TEST BODY", 1).orElseThrow();
        assertEquals(source, actual);
        assertEquals(3_700_000_000L, actual.executionNanos());
        assertTrue(actual.partial());
    }

    @ParameterizedTest
    @ValueSource(strings = {"flowId", "phaseName", "ordinal", "schema", "state", "intervalCount", "interval.0.endedNanos", "interval.0.captureNanos"})
    void missingDeclaredScalarIsNeverTreatedAsLegacyTiming(String field) {
        var properties = recorded(); properties.remove("phase.0.timing." + field);
        assertThrows(IllegalArgumentException.class, () -> AndroidPhaseTimingCodec.read(properties, "phase.0.timing", "flow", "SETUP", 0));
    }

    @Test void aWholeIntervalMovedToItsSiblingIsRejectedEvenWithIdenticalCheckpointTitles() {
        var properties = recorded();
        assertThrows(IllegalArgumentException.class, () -> AndroidPhaseTimingCodec.read(properties, "phase.0.timing", "flow", "TEST BODY", 1));
        assertThrows(IllegalArgumentException.class, () -> AndroidPhaseTimingCodec.read(properties, "phase.0.timing", "other-flow", "SETUP", 0));
    }

    @Test void impossibleIntervalsAndOverBudgetEvidenceAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AndroidPhaseTiming.Interval(10, 9, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new AndroidPhaseTiming.Interval(0, 10, 8, 3));
        var properties = recorded(); properties.setProperty("phase.0.timing.interval.0.captureNanos", "-1");
        assertThrows(IllegalArgumentException.class, () -> AndroidPhaseTimingCodec.read(properties, "phase.0.timing", "flow", "SETUP", 0));
    }

    @Test void absentLegacyRemainsUnavailableAndUnfinishedCaptureCannotClaimCompletion() {
        var properties = new Properties();
        assertTrue(AndroidPhaseTimingCodec.read(properties, "phase.0.timing", "flow", "SETUP", 0).isEmpty());
        AndroidPhaseTimingCodec.running(properties, "phase.0.timing", "flow", "SETUP", 0);
        assertThrows(IllegalArgumentException.class, () -> AndroidPhaseTimingCodec.read(properties, "phase.0.timing", "flow", "SETUP", 0));
    }

    private static Properties recorded() {
        var properties = new Properties();
        AndroidPhaseTimingCodec.write(properties, "phase.0.timing", new AndroidPhaseTiming("flow", "SETUP", 0,
                List.of(new AndroidPhaseTiming.Interval(0, 100, 20, 10)), false));
        return properties;
    }
}
