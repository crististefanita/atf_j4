package ro.whistlab.app.animation;

import static org.junit.Assert.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;

public class TrickCollectionCleanupComponentTest {
    @Test public void failedFramePreservesItsCauseAndAttemptsEveryCleanupDespiteCleanupFailures() {
        var surface = new ControlledCollectionSurface();
        var run = run(surface);
        run.start();
        var primary = new IllegalStateException("Frame failed");
        surface.renderFailure = primary;
        surface.stopFailure = new IllegalStateException("Stop failed");
        surface.unwatchFailure = new IllegalStateException("Unwatch failed");
        surface.releaseFailure = new IllegalStateException("Restore failed");
        assertSame(primary, assertThrows(IllegalStateException.class, () -> surface.frame.accept(.5f)));
        assertArrayEquals(new Throwable[]{surface.stopFailure, surface.unwatchFailure, surface.releaseFailure}, primary.getSuppressed());
        assertEquals(1, surface.stopped);
        assertEquals(1, surface.unwatched);
        assertEquals(1, surface.released);
        assertFalse(surface.completed);
        run.cancel();
        surface.finished.run();
        assertEquals(1, surface.released);
    }

    @Test public void failedCancellationStillUnwatchesAndRestoresBeforeRethrowingTheFirstFailure() {
        var surface = new ControlledCollectionSurface();
        var run = run(surface);
        run.start();
        surface.stopFailure = new IllegalStateException("Stop failed");
        surface.unwatchFailure = new IllegalStateException("Unwatch failed");
        assertSame(surface.stopFailure, assertThrows(IllegalStateException.class, run::cancel));
        assertArrayEquals(new Throwable[]{surface.unwatchFailure}, surface.stopFailure.getSuppressed());
        assertEquals(1, surface.released);
        assertFalse(surface.completed);
        run.cancel();
        assertEquals(1, surface.released);
    }

    @Test public void aMalformedMeasurementFailsBeforeAnyFrameOrAnimator() {
        var surface = new ControlledCollectionSurface();
        surface.geometry = new TrickCollectionRun.Geometry(List.of(new TrickCollectionMotion.Point(0, 0)),
                new TrickCollectionMotion.Point(100, 100));
        assertThrows(IllegalArgumentException.class, () -> run(surface).start());
        assertTrue(surface.draws.isEmpty());
        assertEquals(0, surface.animations);
        assertEquals(1, surface.unwatched);
        assertEquals(1, surface.released);
    }

    @Test public void aBadClockOrNonFiniteFrameClosesTheSceneWithoutReplacingTheOriginalFault() {
        var clock = new AtomicLong(100);
        var waiting = new ControlledCollectionSurface();
        waiting.ready = false;
        var run = new TrickCollectionRun(new TrickCollectionRequest(0, List.of(0, 1, 2)), 1500, clock::get, waiting);
        run.start();
        clock.set(99);
        waiting.ready = true;
        assertThrows(IllegalArgumentException.class, waiting.becameReady::run);
        assertEquals(1, waiting.released);
        var active = new ControlledCollectionSurface();
        run(active).start();
        assertThrows(IllegalArgumentException.class, () -> active.frame.accept(Float.NaN));
        assertEquals(1, active.stopped);
        assertEquals(1, active.released);
    }

    @Test public void navigationReenteringDuringTheFirstRenderCannotStartAnAnimatorAfterCancellation() {
        var surface = new ControlledCollectionSurface();
        var run = run(surface);
        surface.onRender = run::cancel;
        run.start();
        assertEquals(1, surface.draws.size());
        assertEquals(0, surface.animations);
        assertEquals(1, surface.unwatched);
        assertEquals(1, surface.released);
    }

    private static TrickCollectionRun run(ControlledCollectionSurface surface) {
        return new TrickCollectionRun(new TrickCollectionRequest(0, List.of(0, 1, 2)), 1500, () -> 0, surface);
    }
}
