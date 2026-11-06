package ro.whistlab.app.animation;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import ro.whistlab.app.lifecycle.CompletedTrickPause;
import ro.whistlab.app.navigation.AppNavigation;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

public class TrickCollectionFlowComponentTest {
    @Test public void closingTheScoreDialogResumesEvenWhenTheWinnerIsTheHuman() {
        for (int count = 3; count <= 6; count++) {
            var h = new Harness(CollectionFixtures.completed(count, 0, 2, true));
            var before = h.game.saveState();
            h.enter();
            assertEquals(0, h.game.snapshot().currentSeat());
            h.now = 1200;
            h.surface().frame.accept(.4f);
            var old = h.surface();
            var score = h.navigation.openDialog();
            assertEquals(1, old.released);
            h.now = 50_000;
            old.frame.accept(1f);
            old.finished.run();
            assertTrue(h.navigation.dismiss(score));
            assertEquals(2, h.surfaces.size());
            assertEquals(300, h.display.remainingMillis());
            assertEquals(.4f, h.surface().timing.startFraction(), .00001f);
            h.now = 50_300;
            h.show();
            assertFalse(h.display.active());
            assertEquals(GamePhase.PLAYING, h.nextPhase);
            assertEquals(before, h.game.saveState());
        }
    }

    @Test public void popupToNestedDialogNeverResumesBetweenClosuresOrOnDuplicateDismissal() {
        var h = new Harness(CollectionFixtures.completed(4, 0, 2, false));
        h.enter();
        h.now = 700;
        var menu = h.navigation.openDialog();
        var score = h.navigation.openDialog();
        assertFalse(h.navigation.dismiss(menu));
        assertEquals(1, h.surfaces.size());
        h.now = 60_000;
        assertTrue(h.navigation.dismiss(score));
        assertEquals(800, h.display.remainingMillis());
        assertEquals(2, h.surfaces.size());
        assertFalse(h.navigation.dismiss(score));
        assertEquals(2, h.surfaces.size());
    }

    @Test public void homeNavigationCancelsTheOldSceneAndResumeKeepsItsRemainingTime() {
        var h = new Harness(CollectionFixtures.completed(6, 5, 2, true));
        h.enter();
        var old = h.surface();
        h.now = 1300;
        h.navigation.show(AppNavigation.Screen.HOME);
        int draws = old.draws.size();
        h.now = 30_000;
        old.frame.accept(1f);
        old.finished.run();
        assertEquals(draws, old.draws.size());
        assertFalse(old.completed);
        h.enter();
        assertEquals(200, h.display.remainingMillis());
        assertEquals(.6f, h.surface().timing.startFraction(), .00001f);
    }

    @Test public void aDialogDismissedWhileBackgroundedCannotStartAnAnimationUntilForegroundReturns() {
        var h = new Harness(CollectionFixtures.completed(3, 0, 2, false));
        h.enter();
        h.now = 1000;
        var score = h.navigation.openDialog();
        h.foreground = false;
        h.suspend();
        h.now = 10_000;
        assertTrue(h.navigation.dismiss(score));
        assertEquals(1, h.surfaces.size());
        h.foreground = true;
        h.show();
        assertEquals(500, h.display.remainingMillis());
        assertEquals(2, h.surfaces.size());
    }

    @Test public void finalMatchScoreAppearsOnlyAfterTheRemainingVisibleBudgetAtEveryTableSize() {
        for (int count = 3; count <= 6; count++) {
            var h = new Harness(CollectionFixtures.finishedMatch(count));
            var before = h.game.saveState();
            h.enter();
            h.now = 1499;
            h.foreground = false;
            h.suspend();
            h.now = 900_000;
            h.foreground = true;
            h.show();
            assertEquals(1, h.display.remainingMillis());
            assertNull(h.nextPhase);
            h.now++;
            h.show();
            assertEquals(GamePhase.SCORE, h.nextPhase);
            assertTrue(h.game.snapshot().finalHand());
            int countBefore = h.surfaces.size();
            h.now++;
            h.show();
            assertEquals(countBefore, h.surfaces.size());
            assertEquals(before, h.game.saveState());
        }
    }

    @Test public void redrawingGameDoesNotInvalidateAnOpenDialogOrRestartAnExpiredTrick() {
        var h = new Harness(CollectionFixtures.completed(4, 0, 2, false));
        h.enter();
        var dialog = h.navigation.openDialog();
        h.navigation.show(AppNavigation.Screen.GAME);
        assertTrue(h.navigation.blocksAutomation());
        assertTrue(h.navigation.dismiss(dialog));
        h.now = 1500;
        h.show();
        int surfaces = h.surfaces.size();
        h.show();
        assertEquals(surfaces, h.surfaces.size());
        assertFalse(h.display.active());
    }

    private static final class Harness {
        final GameSession game;
        final CompletedTrickPause timeline = new CompletedTrickPause();
        final AppNavigation navigation = new AppNavigation(this::suspend, this::show);
        final List<ControlledCollectionSurface> surfaces = new ArrayList<>();
        long now;
        boolean foreground = true;
        TrickCollectionRun run;
        CompletedTrickPause.DisplayState display;
        GamePhase nextPhase;

        Harness(GameSession game) { this.game = game; }
        void enter() { navigation.show(AppNavigation.Screen.GAME); show(); }
        void suspend() { timeline.suspend(now); if (run != null) run.cancel(); }
        ControlledCollectionSurface surface() { return surfaces.get(surfaces.size() - 1); }
        void show() {
            if (!foreground || navigation.blocksAutomation()) return;
            timeline.resume(now);
            display = timeline.evaluate(game, now);
            if (run != null) run.cancel();
            if (display.active()) {
                var surface = new ControlledCollectionSurface();
                surfaces.add(surface);
                run = new TrickCollectionRun(TrickCollectionRequest.from(game.snapshot(), display.cards()).orElseThrow(),
                        display.remainingMillis(), () -> now, surface);
                run.start();
            } else nextPhase = game.snapshot().phase();
        }
    }
}
