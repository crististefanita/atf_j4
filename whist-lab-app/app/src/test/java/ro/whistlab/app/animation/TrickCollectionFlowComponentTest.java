package ro.whistlab.app.animation;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import ro.whistlab.app.lifecycle.CompletedTrickPause;
import ro.whistlab.app.navigation.AppNavigation;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

public class TrickCollectionFlowComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void closingTheScoreDialogResumesEvenWhenTheWinnerIsTheHuman() {
        for (int count = 3; count <= 6; count++) {
            var h = new Harness(CollectionFixtures.completed(count, 0, 2, true));
            var before = h.game.saveState();
            h.enter();

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "count", count, "h", h);
            assertEquals("h.game.snapshot().currentSeat() must equal 0", 0, h.game.snapshot().currentSeat());
            h.now = 1200;
            h.surface().frame.accept(.4f);
            var old = h.surface();
            var score = h.navigation.openDialog();
            assertEquals("old.released must equal 1", 1, old.released);
            h.now = 50_000;
            old.frame.accept(1f);
            old.finished.run();
            assertTrue("h.navigation.dismiss(score) must hold", h.navigation.dismiss(score));
            assertEquals("h.surfaces.size() must equal 2", 2, h.surfaces.size());
            assertEquals("h.display.remainingMillis() must equal 300", 300, h.display.remainingMillis());
            assertEquals(.4f, h.surface().timing.startFraction(), .00001f);
            h.now = 50_300;
            h.show();
            assertFalse("h.display.active() must remain false", h.display.active());
            assertEquals("h.nextPhase must equal GamePhase.PLAYING", GamePhase.PLAYING, h.nextPhase);
            assertEquals("h.game.saveState() must equal before", before, h.game.saveState());
        }
    }

    @Test public void popupToNestedDialogNeverResumesBetweenClosuresOrOnDuplicateDismissal() {
        var h = new Harness(CollectionFixtures.completed(4, 0, 2, false));
        h.enter();
        h.now = 700;
        var menu = h.navigation.openDialog();
        var score = h.navigation.openDialog();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "h", h, "menu", menu, "score", score);
        assertFalse("h.navigation.dismiss(menu) must remain false", h.navigation.dismiss(menu));
        assertEquals("h.surfaces.size() must equal 1", 1, h.surfaces.size());
        h.now = 60_000;
        assertTrue("h.navigation.dismiss(score) must hold", h.navigation.dismiss(score));
        assertEquals("h.display.remainingMillis() must equal 800", 800, h.display.remainingMillis());
        assertEquals("h.surfaces.size() must equal 2", 2, h.surfaces.size());
        assertFalse("h.navigation.dismiss(score) must remain false", h.navigation.dismiss(score));
        assertEquals("h.surfaces.size() must equal 2", 2, h.surfaces.size());
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

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "draws", draws, "h", h, "old", old);
        assertEquals("old.draws.size() must equal draws", draws, old.draws.size());
        assertFalse("old.completed must remain false", old.completed);
        h.enter();
        assertEquals("h.display.remainingMillis() must equal 200", 200, h.display.remainingMillis());
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

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "h", h, "score", score);
        assertTrue("h.navigation.dismiss(score) must hold", h.navigation.dismiss(score));
        assertEquals("h.surfaces.size() must equal 1", 1, h.surfaces.size());
        h.foreground = true;
        h.show();
        assertEquals("h.display.remainingMillis() must equal 500", 500, h.display.remainingMillis());
        assertEquals("h.surfaces.size() must equal 2", 2, h.surfaces.size());
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

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "count", count, "h", h);
            assertEquals("h.display.remainingMillis() must equal 1", 1, h.display.remainingMillis());
            assertNull("h.nextPhase remains absent", h.nextPhase);
            h.now++;
            h.show();
            assertEquals("h.nextPhase must equal GamePhase.SCORE", GamePhase.SCORE, h.nextPhase);
            assertTrue("h.game.snapshot().finalHand() must hold", h.game.snapshot().finalHand());
            int countBefore = h.surfaces.size();
            h.now++;
            h.show();
            assertEquals("h.surfaces.size() must equal countBefore", countBefore, h.surfaces.size());
            assertEquals("h.game.saveState() must equal before", before, h.game.saveState());
        }
    }

    @Test public void redrawingGameDoesNotInvalidateAnOpenDialogOrRestartAnExpiredTrick() {
        var h = new Harness(CollectionFixtures.completed(4, 0, 2, false));
        h.enter();
        var dialog = h.navigation.openDialog();
        h.navigation.show(AppNavigation.Screen.GAME);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "dialog", dialog, "h", h);
        assertTrue("h.navigation.blocksAutomation() must hold", h.navigation.blocksAutomation());
        assertTrue("h.navigation.dismiss(dialog) must hold", h.navigation.dismiss(dialog));
        h.now = 1500;
        h.show();
        int surfaces = h.surfaces.size();
        h.show();
        assertEquals("h.surfaces.size() must equal surfaces", surfaces, h.surfaces.size());
        assertFalse("h.display.active() must remain false", h.display.active());
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
