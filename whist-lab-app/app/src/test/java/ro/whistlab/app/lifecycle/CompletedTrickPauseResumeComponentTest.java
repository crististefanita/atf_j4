package ro.whistlab.app.lifecycle;

import static org.junit.Assert.*;
import org.junit.Test;
import ro.whistlab.app.animation.CollectionFixtures;

public class CompletedTrickPauseResumeComponentTest {
    @Test public void suspensionRetainsTheExactRemainingBudgetAtThreeToSixPlaces() {
        for (int count = 3; count <= 6; count++) {
            var game = CollectionFixtures.completed(count, 0, 2, true);
            var before = game.snapshot();
            var pause = new CompletedTrickPause();
            assertEquals(1500, pause.evaluate(game, 0).remainingMillis());
            pause.suspend(1200);
            assertEquals(300, pause.evaluate(game, 60_000).remainingMillis());
            pause.suspend(60_000);
            pause.resume(60_100);
            assertEquals(300, pause.evaluate(game, 60_100).remainingMillis());
            assertEquals(1, pause.evaluate(game, 60_399).remainingMillis());
            assertFalse(pause.evaluate(game, 60_400).active());
            assertEquals(before, game.snapshot());
        }
    }

    @Test public void repeatedSuspendResumeAndRedrawNeverResetOrSpendThePausedInterval() {
        var game = CollectionFixtures.completed(4, 0, 2, false);
        var pause = new CompletedTrickPause();
        pause.evaluate(game, 0);
        pause.suspend(100);
        pause.suspend(900);
        pause.resume(1000);
        pause.resume(1000);
        pause.suspend(1100);
        pause.resume(5000);
        assertEquals(1, pause.evaluate(game, 6299).remainingMillis());
        assertFalse(pause.evaluate(game, 6300).active());
        pause.suspend(6400);
        pause.resume(100_000);
        assertFalse(pause.evaluate(game, 100_000).active());
    }

    @Test public void stoppingBeforeTheFirstPresentationStillLeavesTheFullWindowOnResume() {
        var pause = new CompletedTrickPause();
        pause.suspend(100);
        var game = CollectionFixtures.finishedMatch(3);
        assertTrue(game.snapshot().finalHand());
        assertEquals(1500, pause.evaluate(game, 10_000).remainingMillis());
        pause.resume(20_000);
        assertEquals(1500, pause.evaluate(game, 20_000).remainingMillis());
        assertFalse(pause.evaluate(game, 21_500).active());
    }

    @Test public void theLastMatchTrickCannotBeSkippedByAStopOrAccrueExtraScoreDuringPause() {
        for (int count = 3; count <= 6; count++) {
            var game = CollectionFixtures.finishedMatch(count);
            var before = game.saveState();
            var pause = new CompletedTrickPause();
            pause.evaluate(game, 0);
            pause.suspend(1499);
            pause.resume(1_000_000);
            assertEquals(1, pause.evaluate(game, 1_000_000).remainingMillis());
            assertFalse(pause.evaluate(game, 1_000_001).active());
            assertFalse(pause.evaluate(game, 2_000_000).active());
            assertEquals(before, game.saveState());
        }
    }

    @Test public void invalidTimeDoesNotChangeTheOwnerOrRemainingWindow() {
        var game = CollectionFixtures.completed(3, 0, 2, false);
        var other = CollectionFixtures.finishedMatch(3);
        var pause = new CompletedTrickPause();
        pause.evaluate(game, 100);
        assertThrows(IllegalArgumentException.class, () -> pause.evaluate(other, 99));
        assertThrows(IllegalArgumentException.class, () -> pause.suspend(-1));
        assertEquals(1499, pause.evaluate(game, 101).remainingMillis());
        pause.suspend(102);
        assertThrows(IllegalArgumentException.class, () -> pause.resume(101));
        pause.resume(1000);
        assertEquals(1498, pause.evaluate(game, 1000).remainingMillis());
    }

    @Test public void aRemainingBudgetDoesNotOverflowWhenTheClockIsNearItsMaximum() {
        var game = CollectionFixtures.completed(3, 0, 2, false);
        var pause = new CompletedTrickPause();
        assertEquals(1500, pause.evaluate(game, Long.MAX_VALUE - 100).remainingMillis());
        pause.suspend(Long.MAX_VALUE - 50);
        pause.resume(Long.MAX_VALUE);
        assertEquals(1450, pause.evaluate(game, Long.MAX_VALUE).remainingMillis());
    }
}
