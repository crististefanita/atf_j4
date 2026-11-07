package ro.whistlab.app.animation;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import ro.whistlab.app.lifecycle.CompletedTrickPause;
import ro.whistlab.app.navigation.AppNavigation;
import ro.whistlab.app.presentation.PresentationRefresh;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

public class TrickCollectionResizeComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void resizeCleansTheOldSceneBeforeRequestingAnotherAndNotifiesOnlyOnce() {
        var surface = new ControlledCollectionSurface();
        int[] notified = {0};
        var run = new TrickCollectionRun(new TrickCollectionRequest(1, List.of(0, 1, 2)), 1500, () -> 0, surface, () -> {

            assertEquals("surface.released must equal 1", 1, surface.released);
            assertEquals("surface.unwatched must equal 1", 1, surface.unwatched);
            assertEquals("surface.stopped must equal 1", 1, surface.stopped);
            notified[0]++;
        });
        run.start();
        surface.resized.run();
        surface.resized.run();
        surface.invalidated.run();
        surface.finished.run();
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "notified", notified, "run", run, "surface", surface);
        assertEquals("notified[0] must equal 1", 1, notified[0]);
        assertFalse("surface.completed must remain false", surface.completed);
    }

    @Test public void resizeCleanupFailurePreservesItsCauseAndNeverRequestsAnotherScene() {
        var surface = new ControlledCollectionSurface();
        var failure = new IllegalStateException("Resize cleanup failed");
        surface.stopFailure = failure;
        new TrickCollectionRun(new TrickCollectionRequest(1, List.of(0, 1, 2)), 1500, () -> 0,
                surface, () -> fail("A failed cleanup cannot replace the scene")).start();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "failure", failure, "surface", surface);
        assertSame("assertThrows(IllegalStateException.class, surface.resized::run) keeps the identity of failure", failure, assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class, surface.resized::run));
        assertEquals("surface.released must equal 1", 1, surface.released);
        assertEquals("surface.unwatched must equal 1", 1, surface.unwatched);
        surface.resized.run();
        assertEquals("surface.stopped must equal 1", 1, surface.stopped);
    }

    @Test public void failedResizeNotificationCannotRetainOrReopenTheOldScene() {
        var surface = new ControlledCollectionSurface();
        var failure = new IllegalStateException("Presentation unavailable");
        new TrickCollectionRun(new TrickCollectionRequest(1, List.of(0, 1, 2)), 1500, () -> 0,
                surface, () -> { throw failure; }).start();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "failure", failure, "surface", surface);
        assertSame("assertThrows(IllegalStateException.class, surface.resized::run) keeps the identity of failure", failure, assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class, surface.resized::run));
        int draws = surface.draws.size();
        surface.frame.accept(1f);
        surface.resized.run();
        surface.becameReady.run();
        surface.finished.run();
        assertEquals("surface.draws.size() must equal draws", draws, surface.draws.size());
        assertEquals("surface.released must equal 1", 1, surface.released);
        assertEquals("surface.stopped must equal 1", 1, surface.stopped);
        assertEquals("surface.unwatched must equal 1", 1, surface.unwatched);
    }

    @Test public void portraitLandscapeAndResizeBurstsUseOnlyTheLatestGeometryForEveryWinningSeat() {
        for (int count = 3; count <= 6; count++) for (int winner = 0; winner < count; winner++) {
            var h = new Harness(CollectionFixtures.completed(count, winner, 2, true));
            var before = h.game.saveState();
            h.enter();
            var old = h.surface();
            h.now = 1200;
            h.viewport = new Viewport(1400, 800);
            old.resized.run();
            h.viewport = new Viewport(720, 1280);
            h.refresh.request();
            h.viewport = new Viewport(1600, 900);
            h.refresh.request();

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "count", count, "h", h, "old", old, "winner", winner);
            assertEquals("h.queue.size() must equal 1", 1, h.queue.size());
            h.drain();
            assertEquals("h.surfaces.size() must equal 2", 2, h.surfaces.size());
            assertEquals("h.display.remainingMillis() must equal 300", 300, h.display.remainingMillis());
            assertEquals(.4f, h.surface().timing.startFraction(), .00001f);
            var current = h.surface();
            int freshDraws = current.draws.size();
            old.frame.accept(1f);
            old.becameReady.run();
            old.finished.run();
            old.resized.run();
            assertEquals("current.draws.size() must equal freshDraws", freshDraws, current.draws.size());
            assertEquals("current.released must equal 0", 0, current.released);
            current.frame.accept(1f);
            current.finished.run();
            assertTarget(current, h.viewport, winner, count);
            assertEquals("h.game.saveState() must equal before", before, h.game.saveState());
        }
    }

    @Test public void orientationInsideNestedDialogsDefersUntilTheLastValidClosureInEitherOrder() {
        for (boolean parentFirst : new boolean[]{false, true}) {
            var h = new Harness(CollectionFixtures.completed(4, 0, 2, false));
            h.enter();
            h.now = 1000;
            var menu = h.navigation.openDialog();
            var history = h.navigation.openDialog();
            h.viewport = new Viewport(1400, 800);
            h.refresh.request();
            h.now = 60_000;
            h.drain();

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "h", h, "history", history, "menu", menu, "parentFirst", parentFirst);
            assertEquals("h.surfaces.size() must equal 1", 1, h.surfaces.size());
            assertFalse("h.navigation.dismiss(parentFirst ? menu : history) must remain false", h.navigation.dismiss(parentFirst ? menu : history));
            assertTrue("h.navigation.blocksAutomation() must hold", h.navigation.blocksAutomation());
            assertTrue("h.navigation.dismiss(parentFirst ? history : menu) must hold", h.navigation.dismiss(parentFirst ? history : menu));
            assertEquals("h.surfaces.size() must equal 2", 2, h.surfaces.size());
            assertEquals("h.display.remainingMillis() must equal 500", 500, h.display.remainingMillis());
            assertEquals("h.drawnViewport must equal new Viewport(1400, 800)", new Viewport(1400, 800), h.drawnViewport);
            assertFalse("h.navigation.dismiss(menu) must remain false", h.navigation.dismiss(menu));
            assertFalse("h.navigation.dismiss(history) must remain false", h.navigation.dismiss(history));
            assertEquals("h.surfaces.size() must equal 2", 2, h.surfaces.size());
        }
    }

    @Test public void aQueuedResizeCannotRenderAfterNavigationOrCancelANewlyResumedScene() {
        var h = new Harness(CollectionFixtures.completed(6, 5, 2, true));
        h.enter();
        h.now = 1100;
        h.viewport = new Viewport(1500, 800);
        h.surface().resized.run();
        var obsolete = h.queue.get(0);
        h.navigation.show(AppNavigation.Screen.HOME);
        obsolete.run();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "h", h, "obsolete", obsolete);
        assertEquals("h.surfaces.size() must equal 1", 1, h.surfaces.size());
        h.now = 90_000;
        h.enter();
        var current = h.surface();
        obsolete.run();
        assertEquals("h.surfaces.size() must equal 2", 2, h.surfaces.size());
        assertEquals("h.display.remainingMillis() must equal 400", 400, h.display.remainingMillis());
        assertEquals("current.released must equal 0", 0, current.released);
    }

    @Test public void backgroundAndConfigurationCallbacksPreserveTheLastMatchTricksFinalMillisecond() {
        for (int count = 3; count <= 6; count++) {
            var h = new Harness(CollectionFixtures.finishedMatch(count));
            var before = h.game.saveState();
            h.enter();
            h.now = 1499;
            h.foreground = false;
            h.cancel();
            h.viewport = new Viewport(1400, 800);
            h.refresh.request();
            h.now = 900_000;
            h.drain();

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "count", count, "h", h);
            assertEquals("h.surfaces.size() must equal 1", 1, h.surfaces.size());
            assertNull("h.nextPhase remains absent", h.nextPhase);
            h.foreground = true;
            h.show();
            assertEquals("h.display.remainingMillis() must equal 1", 1, h.display.remainingMillis());
            h.now++;
            h.show();
            assertEquals("h.nextPhase must equal GamePhase.SCORE", GamePhase.SCORE, h.nextPhase);
            int scenes = h.surfaces.size();
            h.viewport = new Viewport(720, 1280);
            h.refresh.request();
            h.drain();
            assertEquals("h.surfaces.size() must equal scenes", scenes, h.surfaces.size());
            assertEquals("h.game.saveState() must equal before", before, h.game.saveState());
        }
    }

    @Test public void delayedMeasurementUsesTheChangedViewportAndIgnoresReadinessAfterSceneReplacement() {
        var h = new Harness(CollectionFixtures.completed(3, 0, 2, false));
        h.measurementReady = false;
        h.enter();
        var waiting = h.surface();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "h", h, "waiting", waiting);
        assertEquals("waiting.animations must equal 0", 0, waiting.animations);
        h.now = 200;
        h.viewport = new Viewport(1400, 800);
        h.refresh.request();
        h.measurementReady = true;
        h.drain();
        assertEquals("h.display.remainingMillis() must equal 1300", 1300, h.display.remainingMillis());
        assertEquals("h.drawnViewport must equal new Viewport(1400, 800)", new Viewport(1400, 800), h.drawnViewport);
        waiting.ready = true;
        waiting.becameReady.run();
        assertEquals("waiting.animations must equal 0", 0, waiting.animations);
        assertEquals("waiting.released must equal 1", 1, waiting.released);
        assertEquals("h.surface().animations must equal 1", 1, h.surface().animations);
    }

    @Test public void detachAndLateResizeAfterCompletionDoNotRequestAReplacement() {
        for (boolean finish : new boolean[]{false, true}) {
            var surface = new ControlledCollectionSurface();
            int[] refreshes = {0};
            new TrickCollectionRun(new TrickCollectionRequest(0, List.of(0, 1, 2)), 1500, () -> 0,
                    surface, () -> refreshes[0]++).start();
            if (finish) surface.finished.run(); else surface.invalidated.run();
            int draws = surface.draws.size();
            surface.resized.run();
            surface.frame.accept(1f);

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "draws", draws, "finish", finish, "refreshes", refreshes, "surface", surface);
            assertEquals("refreshes[0] must equal 0", 0, refreshes[0]);
            assertEquals("surface.draws.size() must equal draws", draws, surface.draws.size());
        }
    }

    private static void assertTarget(ControlledCollectionSurface surface, Viewport viewport, int winner, int count) {
        var frames = surface.draws.get(surface.draws.size() - 1);
        var target = viewport.target(winner);
        assertEquals("frames.size() must equal count", count, frames.size());
        for (int card = 0; card < count; card++) {
            assertEquals(target.x(), surface.geometry.cards().get(card).x() + frames.get(card).translationX(), .001f);
            assertEquals(target.y(), surface.geometry.cards().get(card).y() + frames.get(card).translationY(), .001f);
        }
    }

    private record Viewport(int width, int height) {
        TrickCollectionMotion.Point target(int winner) {
            return new TrickCollectionMotion.Point(width * (winner + 1) / 8f, winner == 0 ? height * .9f : height * .1f);
        }
    }

    private static final class Harness {
        final GameSession game;
        final CompletedTrickPause timeline = new CompletedTrickPause();
        final List<Runnable> queue = new ArrayList<>();
        final AppNavigation navigation = new AppNavigation(this::cancel, this::show);
        final PresentationRefresh refresh = new PresentationRefresh(this::suspend, this::show, queue::add,
                () -> this.foreground && navigation.gameVisible() && !navigation.blocksAutomation());
        final List<ControlledCollectionSurface> surfaces = new ArrayList<>();
        Viewport viewport = new Viewport(720, 1280), drawnViewport;
        long now;
        boolean foreground = true, measurementReady = true;
        TrickCollectionRun run;
        CompletedTrickPause.DisplayState display;
        GamePhase nextPhase;
        Harness(GameSession game) { this.game = game; }
        void enter() { navigation.show(AppNavigation.Screen.GAME); show(); }
        void cancel() { refresh.cancel(); suspend(); }
        void suspend() { timeline.suspend(now); if (run != null) run.cancel(); }
        void drain() { for (var event : List.copyOf(queue)) event.run(); }
        ControlledCollectionSurface surface() { return surfaces.get(surfaces.size() - 1); }
        void show() {
            if (!foreground) return;
            refresh.cancel();
            if (navigation.blocksAutomation()) return;
            timeline.resume(now);
            display = timeline.evaluate(game, now);
            if (run != null) run.cancel();
            if (!display.active()) { nextPhase = game.snapshot().phase(); return; }
            drawnViewport = viewport;
            var request = TrickCollectionRequest.from(game.snapshot(), display.cards()).orElseThrow();
            var surface = new ControlledCollectionSurface();
            surface.ready = measurementReady;
            var origins = new ArrayList<TrickCollectionMotion.Point>();
            for (int seat : request.seats()) origins.add(new TrickCollectionMotion.Point(viewport.width * (seat + 1) / 8f, viewport.height * .5f));
            surface.geometry = new TrickCollectionRun.Geometry(origins, viewport.target(request.winner()));
            surfaces.add(surface);
            run = new TrickCollectionRun(request, display.remainingMillis(), () -> now, surface, refresh::request);
            run.start();
        }
    }
}
