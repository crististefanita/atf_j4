package ro.whistlab.app.animation;

import static org.junit.Assert.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;

public class TrickCollectionRunComponentTest {
    @Test public void everyFaceReachesTheEnginesWinnerAndCompletesOnlyOnce() {
        for (int count = 3; count <= 6; count++) for (int winner = 0; winner < count; winner++) {
            var game = CollectionFixtures.completed(count, winner, 2, true);
            var before = game.snapshot();
            var request = TrickCollectionRequest.from(before, before.lastTrick()).orElseThrow();
            var surface = new ControlledCollectionSurface();
            var run = new TrickCollectionRun(request, 1500, () -> 0, surface);
            run.start();
            run.start();
            surface.frame.accept(.25f);
            surface.frame.accept(.75f);
            surface.frame.accept(1f);
            surface.finished.run();
            int draws = surface.draws.size();
            var last = surface.draws.get(draws - 1);
            for (int card = 0; card < count; card++) {
                assertEquals(surface.geometry.target().x(), surface.geometry.cards().get(card).x() + last.get(card).translationX(), .001f);
                assertEquals(surface.geometry.target().y(), surface.geometry.cards().get(card).y() + last.get(card).translationY(), .001f);
                assertEquals(0, last.get(card).alpha(), 0);
            }
            run.cancel();
            surface.frame.accept(.5f);
            surface.finished.run();
            surface.invalidated.run();
            assertEquals(draws, surface.draws.size());
            assertTrue(surface.completed);
            assertEquals(1, surface.animations);
            assertEquals(1, surface.watches);
            assertEquals(1, surface.stopped);
            assertEquals(1, surface.unwatched);
            assertEquals(1, surface.released);
            assertEquals(before, game.snapshot());
        }
    }

    @Test public void anUnmeasuredLayoutCanBecomeReadyWithoutStartingTwiceOrExtendingTime() {
        var clock = new AtomicLong(0);
        var surface = new ControlledCollectionSurface();
        surface.ready = false;
        var run = new TrickCollectionRun(request(), 1500, clock::get, surface);
        run.start();
        assertEquals(0, surface.animations);
        clock.set(400);
        surface.ready = true;
        surface.becameReady.run();
        surface.becameReady.run();
        assertEquals(new TrickCollectionMotion.Timing(600, 500, 0), surface.timing);
        assertEquals(1, surface.animations);
        run.cancel();
    }

    @Test public void layoutReadyAfterExpiryNeverDrawsOrStartsAnAnimator() {
        var clock = new AtomicLong();
        var surface = new ControlledCollectionSurface();
        surface.ready = false;
        var run = new TrickCollectionRun(request(), 1500, clock::get, surface);
        run.start();
        clock.set(1500);
        surface.ready = true;
        surface.becameReady.run();
        assertEquals(0, surface.animations);
        assertTrue(surface.draws.isEmpty());
        assertEquals(1, surface.unwatched);
        assertEquals(1, surface.released);
        assertFalse(surface.completed);
    }

    @Test public void cancelledWaitingLayoutIgnoresPostedReadyAndDetachEvents() {
        var surface = new ControlledCollectionSurface();
        surface.ready = false;
        var run = new TrickCollectionRun(request(), 1500, () -> 0, surface);
        run.start();
        run.cancel();
        surface.ready = true;
        surface.becameReady.run();
        surface.invalidated.run();
        run.start();
        assertEquals(0, surface.animations);
        assertTrue(surface.draws.isEmpty());
        assertEquals(1, surface.unwatched);
        assertEquals(1, surface.released);
    }

    @Test public void activeSceneInvalidationCancelsWithoutTreatingCancelAsSuccessfulCollection() {
        var surface = new ControlledCollectionSurface();
        new TrickCollectionRun(request(), 1500, () -> 0, surface).start();
        surface.frame.accept(.5f);
        surface.invalidated.run();
        int draws = surface.draws.size();
        surface.frame.accept(1f);
        surface.finished.run();
        assertEquals(draws, surface.draws.size());
        assertFalse(surface.completed);
        assertEquals(1, surface.stopped);
        assertEquals(1, surface.unwatched);
        assertEquals(1, surface.released);
    }

    @Test public void resumedCollectionStartsAtItsRemainingFractionAndNeverMovesBackwards() {
        var surface = new ControlledCollectionSurface();
        var run = new TrickCollectionRun(request(), 300, () -> 0, surface);
        run.start();
        assertEquals(.4f, surface.timing.startFraction(), .00001f);
        surface.frame.accept(.1f);
        surface.frame.accept(.4f);
        assertEquals(1, surface.draws.size());
        surface.frame.accept(.8f);
        surface.frame.accept(.6f);
        assertEquals(2, surface.draws.size());
        surface.finished.run();
        assertEquals(3, surface.draws.size());
        assertTrue(surface.completed);
    }

    @Test public void disabledAnimationsLeaveTheSceneStaticAndReleaseTheWatcher() {
        var surface = new ControlledCollectionSurface();
        surface.enabled = false;
        new TrickCollectionRun(request(), 1500, () -> 0, surface).start();
        assertTrue(surface.draws.isEmpty());
        assertEquals(0, surface.animations);
        assertEquals(1, surface.unwatched);
        assertEquals(1, surface.released);
    }

    @Test public void synchronousCancellationWhileRegisteringStillClosesTheReturnedWatcher() {
        var surface = new ControlledCollectionSurface();
        surface.cancelWhenWatching = true;
        new TrickCollectionRun(request(), 1500, () -> 0, surface).start();
        assertEquals(1, surface.unwatched);
        assertEquals(1, surface.released);
        assertEquals(0, surface.animations);
    }

    @Test public void synchronousReadyAndCompletionReleaseBothHandlesReturnedAfterCompletion() {
        var surface = new ControlledCollectionSurface();
        surface.readyWhenWatching = true;
        surface.finishInline = true;
        new TrickCollectionRun(request(), 1500, () -> 0, surface).start();
        assertTrue(surface.completed);
        assertEquals(2, surface.draws.size());
        assertEquals(1, surface.animations);
        assertEquals(1, surface.stopped);
        assertEquals(1, surface.unwatched);
        assertEquals(1, surface.released);
    }

    private static TrickCollectionRequest request() { return new TrickCollectionRequest(0, List.of(2, 0, 1)); }
}
