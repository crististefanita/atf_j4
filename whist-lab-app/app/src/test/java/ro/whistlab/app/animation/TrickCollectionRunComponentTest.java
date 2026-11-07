package ro.whistlab.app.animation;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;

public class TrickCollectionRunComponentTest extends atf.gradle.audit.GradleAuditedTest {
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

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "card", card, "count", count, "draws", draws, "game", game, "last", last, "request", request, "run", run, "surface", surface, "winner", winner);
                assertEquals(surface.geometry.target().x(), surface.geometry.cards().get(card).x() + last.get(card).translationX(), .001f);
                assertEquals(surface.geometry.target().y(), surface.geometry.cards().get(card).y() + last.get(card).translationY(), .001f);
                assertEquals(0, last.get(card).alpha(), 0);
            }
            run.cancel();
            surface.frame.accept(.5f);
            surface.finished.run();
            surface.invalidated.run();
            assertEquals("surface.draws.size() must equal draws", draws, surface.draws.size());
            assertTrue("surface.completed must hold", surface.completed);
            assertEquals("surface.animations must equal 1", 1, surface.animations);
            assertEquals("surface.watches must equal 1", 1, surface.watches);
            assertEquals("surface.stopped must equal 1", 1, surface.stopped);
            assertEquals("surface.unwatched must equal 1", 1, surface.unwatched);
            assertEquals("surface.released must equal 1", 1, surface.released);
            assertEquals("game.snapshot() must equal before", before, game.snapshot());
        }
    }

    @Test public void anUnmeasuredLayoutCanBecomeReadyWithoutStartingTwiceOrExtendingTime() {
        var clock = new AtomicLong(0);
        var surface = new ControlledCollectionSurface();
        surface.ready = false;
        var run = new TrickCollectionRun(request(), 1500, clock::get, surface);
        run.start();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "clock", clock, "run", run, "surface", surface);
        assertEquals("surface.animations must equal 0", 0, surface.animations);
        clock.set(400);
        surface.ready = true;
        surface.becameReady.run();
        surface.becameReady.run();
        assertEquals("surface.timing must equal new TrickCollectionMotion.Timing(600, 500, 0)", new TrickCollectionMotion.Timing(600, 500, 0), surface.timing);
        assertEquals("surface.animations must equal 1", 1, surface.animations);
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

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "clock", clock, "run", run, "surface", surface);
        assertEquals("surface.animations must equal 0", 0, surface.animations);
        assertTrue("surface.draws.isEmpty() must hold", surface.draws.isEmpty());
        assertEquals("surface.unwatched must equal 1", 1, surface.unwatched);
        assertEquals("surface.released must equal 1", 1, surface.released);
        assertFalse("surface.completed must remain false", surface.completed);
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

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "run", run, "surface", surface);
        assertEquals("surface.animations must equal 0", 0, surface.animations);
        assertTrue("surface.draws.isEmpty() must hold", surface.draws.isEmpty());
        assertEquals("surface.unwatched must equal 1", 1, surface.unwatched);
        assertEquals("surface.released must equal 1", 1, surface.released);
    }

    @Test public void activeSceneInvalidationCancelsWithoutTreatingCancelAsSuccessfulCollection() {
        var surface = new ControlledCollectionSurface();
        new TrickCollectionRun(request(), 1500, () -> 0, surface).start();
        surface.frame.accept(.5f);
        surface.invalidated.run();
        int draws = surface.draws.size();
        surface.frame.accept(1f);
        surface.finished.run();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "draws", draws, "surface", surface);
        assertEquals("surface.draws.size() must equal draws", draws, surface.draws.size());
        assertFalse("surface.completed must remain false", surface.completed);
        assertEquals("surface.stopped must equal 1", 1, surface.stopped);
        assertEquals("surface.unwatched must equal 1", 1, surface.unwatched);
        assertEquals("surface.released must equal 1", 1, surface.released);
    }

    @Test public void resumedCollectionStartsAtItsRemainingFractionAndNeverMovesBackwards() {
        var surface = new ControlledCollectionSurface();
        var run = new TrickCollectionRun(request(), 300, () -> 0, surface);
        run.start();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "run", run, "surface", surface);
        assertEquals(.4f, surface.timing.startFraction(), .00001f);
        surface.frame.accept(.1f);
        surface.frame.accept(.4f);
        assertEquals("surface.draws.size() must equal 1", 1, surface.draws.size());
        surface.frame.accept(.8f);
        surface.frame.accept(.6f);
        assertEquals("surface.draws.size() must equal 2", 2, surface.draws.size());
        surface.finished.run();
        assertEquals("surface.draws.size() must equal 3", 3, surface.draws.size());
        assertTrue("surface.completed must hold", surface.completed);
    }

    @Test public void disabledAnimationsLeaveTheSceneStaticAndReleaseTheWatcher() {
        var surface = new ControlledCollectionSurface();
        surface.enabled = false;
        new TrickCollectionRun(request(), 1500, () -> 0, surface).start();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "surface", surface);
        assertTrue("surface.draws.isEmpty() must hold", surface.draws.isEmpty());
        assertEquals("surface.animations must equal 0", 0, surface.animations);
        assertEquals("surface.unwatched must equal 1", 1, surface.unwatched);
        assertEquals("surface.released must equal 1", 1, surface.released);
    }

    @Test public void synchronousCancellationWhileRegisteringStillClosesTheReturnedWatcher() {
        var surface = new ControlledCollectionSurface();
        surface.cancelWhenWatching = true;
        new TrickCollectionRun(request(), 1500, () -> 0, surface).start();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "surface", surface);
        assertEquals("surface.unwatched must equal 1", 1, surface.unwatched);
        assertEquals("surface.released must equal 1", 1, surface.released);
        assertEquals("surface.animations must equal 0", 0, surface.animations);
    }

    @Test public void synchronousReadyAndCompletionReleaseBothHandlesReturnedAfterCompletion() {
        var surface = new ControlledCollectionSurface();
        surface.readyWhenWatching = true;
        surface.finishInline = true;
        new TrickCollectionRun(request(), 1500, () -> 0, surface).start();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "surface", surface);
        assertTrue("surface.completed must hold", surface.completed);
        assertEquals("surface.draws.size() must equal 2", 2, surface.draws.size());
        assertEquals("surface.animations must equal 1", 1, surface.animations);
        assertEquals("surface.stopped must equal 1", 1, surface.stopped);
        assertEquals("surface.unwatched must equal 1", 1, surface.unwatched);
        assertEquals("surface.released must equal 1", 1, surface.released);
    }

    private static TrickCollectionRequest request() { return new TrickCollectionRequest(0, List.of(2, 0, 1)); }
}
