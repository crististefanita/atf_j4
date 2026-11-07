package ro.whistlab.app.animation;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;

public class TrickCollectionCleanupComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void failedFramePreservesItsCauseAndAttemptsEveryCleanupDespiteCleanupFailures() {
        var surface = new ControlledCollectionSurface();
        var run = run(surface);
        run.start();
        var primary = new IllegalStateException("Frame failed");
        surface.renderFailure = primary;
        surface.stopFailure = new IllegalStateException("Stop failed");
        surface.unwatchFailure = new IllegalStateException("Unwatch failed");
        surface.releaseFailure = new IllegalStateException("Restore failed");

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "primary", primary, "run", run, "surface", surface);
        assertSame("assertThrows(IllegalStateException.class, ()->surface.frame.accept(0.5F)) keeps the identity of primary", primary, assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class, () -> surface.frame.accept(.5f)));
        assertArrayEquals(new Throwable[]{surface.stopFailure, surface.unwatchFailure, surface.releaseFailure}, primary.getSuppressed());
        assertEquals("surface.stopped must equal 1", 1, surface.stopped);
        assertEquals("surface.unwatched must equal 1", 1, surface.unwatched);
        assertEquals("surface.released must equal 1", 1, surface.released);
        assertFalse("surface.completed must remain false", surface.completed);
        run.cancel();
        surface.finished.run();
        assertEquals("surface.released must equal 1", 1, surface.released);
    }

    @Test public void failedCancellationStillUnwatchesAndRestoresBeforeRethrowingTheFirstFailure() {
        var surface = new ControlledCollectionSurface();
        var run = run(surface);
        run.start();
        surface.stopFailure = new IllegalStateException("Stop failed");
        surface.unwatchFailure = new IllegalStateException("Unwatch failed");

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "run", run, "surface", surface);
        assertSame("assertThrows(IllegalStateException.class, run::cancel) keeps the identity of surface.stopFailure", surface.stopFailure, assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class, run::cancel));
        assertArrayEquals(new Throwable[]{surface.unwatchFailure}, surface.stopFailure.getSuppressed());
        assertEquals("surface.released must equal 1", 1, surface.released);
        assertFalse("surface.completed must remain false", surface.completed);
        run.cancel();
        assertEquals("surface.released must equal 1", 1, surface.released);
    }

    @Test public void aMalformedMeasurementFailsBeforeAnyFrameOrAnimator() {
        var surface = new ControlledCollectionSurface();
        surface.geometry = new TrickCollectionRun.Geometry(List.of(new TrickCollectionMotion.Point(0, 0)),
                new TrickCollectionMotion.Point(100, 100));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "surface", surface);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> run(surface).start());
        assertTrue("surface.draws.isEmpty() must hold", surface.draws.isEmpty());
        assertEquals("surface.animations must equal 0", 0, surface.animations);
        assertEquals("surface.unwatched must equal 1", 1, surface.unwatched);
        assertEquals("surface.released must equal 1", 1, surface.released);
    }

    @Test public void aBadClockOrNonFiniteFrameClosesTheSceneWithoutReplacingTheOriginalFault() {
        var clock = new AtomicLong(100);
        var waiting = new ControlledCollectionSurface();
        waiting.ready = false;
        var run = new TrickCollectionRun(new TrickCollectionRequest(0, List.of(0, 1, 2)), 1500, clock::get, waiting);
        run.start();
        clock.set(99);
        waiting.ready = true;

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "clock", clock, "run", run, "waiting", waiting);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, waiting.becameReady::run);
        assertEquals("waiting.released must equal 1", 1, waiting.released);
        var active = new ControlledCollectionSurface();
        run(active).start();
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> active.frame.accept(Float.NaN));
        assertEquals("active.stopped must equal 1", 1, active.stopped);
        assertEquals("active.released must equal 1", 1, active.released);
    }

    @Test public void navigationReenteringDuringTheFirstRenderCannotStartAnAnimatorAfterCancellation() {
        var surface = new ControlledCollectionSurface();
        var run = run(surface);
        surface.onRender = run::cancel;
        run.start();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "run", run, "surface", surface);
        assertEquals("surface.draws.size() must equal 1", 1, surface.draws.size());
        assertEquals("surface.animations must equal 0", 0, surface.animations);
        assertEquals("surface.unwatched must equal 1", 1, surface.unwatched);
        assertEquals("surface.released must equal 1", 1, surface.released);
    }

    private static TrickCollectionRun run(ControlledCollectionSurface surface) {
        return new TrickCollectionRun(new TrickCollectionRequest(0, List.of(0, 1, 2)), 1500, () -> 0, surface);
    }
}
