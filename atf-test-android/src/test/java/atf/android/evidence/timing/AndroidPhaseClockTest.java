package atf.android.evidence.timing;

import atf.testkit.AtfUnitTest;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

@AtfUnitTest
class AndroidPhaseClockTest {
    @Test void originalExecutionCaptureJournalAndCurrentPublicationHaveIndependentAxes() throws Exception {
        var ticks = new AtomicLong();
        var clock = new AndroidPhaseClock("flow", ticks::get);
        clock.enter("SETUP");
        ticks.addAndGet(100_000_000L);
        try (var capture = clock.capture()) { ticks.addAndGet(250_000_000L); }
        clock.journal(() -> ticks.addAndGet(50_000_000L));
        ticks.addAndGet(700_000_000L);
        clock.stop(false);
        var setup = clock.snapshot("SETUP").orElseThrow();
        assertEquals(1_100_000_000L, setup.elapsedNanos());
        assertEquals(800_000_000L, setup.executionNanos());
        assertEquals(250_000_000L, setup.captureNanos());
        assertEquals(50_000_000L, setup.journalNanos());
        assertEquals(Duration.ofMillis(800), setup.reporting(Duration.ofMillis(19)).execution());
        assertEquals(Duration.ofMillis(19), setup.reporting(Duration.ofMillis(19)).evidence().orElseThrow().publication());
        ticks.addAndGet(90_000_000_000L);
        assertEquals(setup, clock.snapshot("SETUP").orElseThrow(), "Publication cannot move the original end");
    }

    @Test void interruptedIntervalAndLaterPassKeepTheirOwnOwners() {
        var ticks = new AtomicLong();
        var clock = new AndroidPhaseClock("flow", ticks::get);
        clock.enter("TEST BODY");
        ticks.addAndGet(2_000_000_000L);
        clock.stop(true);
        clock.enter("TEARDOWN");
        ticks.addAndGet(300_000_000L);
        clock.stop(false);
        assertTrue(clock.snapshot("TEST BODY").orElseThrow().partial());
        assertEquals(2_000_000_000L, clock.snapshot("TEST BODY").orElseThrow().executionNanos());
        assertFalse(clock.snapshot("TEARDOWN").orElseThrow().partial());
        assertEquals(300_000_000L, clock.snapshot("TEARDOWN").orElseThrow().executionNanos());
    }

    @Test void phaseReentryRetainsIntervalsWithoutCountingTheSiblingGap() {
        var ticks = new AtomicLong();
        var clock = new AndroidPhaseClock("flow", ticks::get);
        clock.enter("SETUP"); ticks.addAndGet(100_000_000L);
        clock.enter("TEST BODY"); ticks.addAndGet(2_000_000_000L);
        clock.enter("SETUP"); ticks.addAndGet(500_000_000L); clock.stop(false);
        var timing = clock.snapshot("SETUP").orElseThrow();
        assertEquals(2, timing.intervals().size());
        assertEquals(600_000_000L, timing.executionNanos());
        assertEquals(0, timing.ordinal());
        assertEquals(2_000_000_000L, clock.snapshot("TEST BODY").orElseThrow().executionNanos());
    }

    @Test void throwingCaptureIsMeasuredWithoutReplacingItsOriginalCause() {
        var ticks = new AtomicLong();
        var clock = new AndroidPhaseClock("flow", ticks::get);
        clock.enter("TEST BODY");
        var failure = assertThrows(IllegalStateException.class, () -> {
            try (var capture = clock.capture()) { ticks.addAndGet(30_000_000L); throw new IllegalStateException("original capture"); }
        });
        clock.stop(true);
        assertEquals("original capture", failure.getMessage());
        assertEquals(30_000_000L, clock.snapshot("TEST BODY").orElseThrow().captureNanos());
    }

    @Test void nestedEvidenceAndPhaseChangesInsideCaptureAreRejected() {
        var clock = new AndroidPhaseClock("flow", () -> 0L);
        clock.enter("SETUP");
        try (var capture = clock.capture()) {
            assertThrows(IllegalStateException.class, clock::capture);
            assertThrows(IllegalStateException.class, () -> clock.enter("TEST BODY"));
        }
        clock.stop(false);
    }

    @Test void backwardsClockIsRejectedRatherThanClamped() {
        var ticks = new AtomicLong(100);
        var clock = new AndroidPhaseClock("flow", ticks::get); clock.enter("SETUP");
        ticks.set(99);
        assertThrows(IllegalStateException.class, () -> clock.stop(false));
    }
}
