package ro.whistlab.app.lifecycle;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import org.junit.Test;
import ro.whistlab.app.animation.CollectionFixtures;

public class CompletedTrickPauseResumeComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void suspensionRetainsTheExactRemainingBudgetAtThreeToSixPlaces() {
        for (int count = 3; count <= 6; count++) {
            var game = CollectionFixtures.completed(count, 0, 2, true);
            var before = game.snapshot();
            var pause = new CompletedTrickPause();

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "count", count, "game", game, "pause", pause);
            assertEquals("pause.evaluate(game, 0).remainingMillis() must equal 1500", 1500, pause.evaluate(game, 0).remainingMillis());
            pause.suspend(1200);
            assertEquals("pause.evaluate(game, 60000).remainingMillis() must equal 300", 300, pause.evaluate(game, 60_000).remainingMillis());
            pause.suspend(60_000);
            pause.resume(60_100);
            assertEquals("pause.evaluate(game, 60100).remainingMillis() must equal 300", 300, pause.evaluate(game, 60_100).remainingMillis());
            assertEquals("pause.evaluate(game, 60399).remainingMillis() must equal 1", 1, pause.evaluate(game, 60_399).remainingMillis());
            assertFalse("pause.evaluate(game, 60400).active() must remain false", pause.evaluate(game, 60_400).active());
            assertEquals("game.snapshot() must equal before", before, game.snapshot());
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

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "pause", pause);
        assertEquals("pause.evaluate(game, 6299).remainingMillis() must equal 1", 1, pause.evaluate(game, 6299).remainingMillis());
        assertFalse("pause.evaluate(game, 6300).active() must remain false", pause.evaluate(game, 6300).active());
        pause.suspend(6400);
        pause.resume(100_000);
        assertFalse("pause.evaluate(game, 100000).active() must remain false", pause.evaluate(game, 100_000).active());
    }

    @Test public void stoppingBeforeTheFirstPresentationStillLeavesTheFullWindowOnResume() {
        var pause = new CompletedTrickPause();
        pause.suspend(100);
        var game = CollectionFixtures.finishedMatch(3);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "pause", pause);
        assertTrue("game.snapshot().finalHand() must hold", game.snapshot().finalHand());
        assertEquals("pause.evaluate(game, 10000).remainingMillis() must equal 1500", 1500, pause.evaluate(game, 10_000).remainingMillis());
        pause.resume(20_000);
        assertEquals("pause.evaluate(game, 20000).remainingMillis() must equal 1500", 1500, pause.evaluate(game, 20_000).remainingMillis());
        assertFalse("pause.evaluate(game, 21500).active() must remain false", pause.evaluate(game, 21_500).active());
    }

    @Test public void theLastMatchTrickCannotBeSkippedByAStopOrAccrueExtraScoreDuringPause() {
        for (int count = 3; count <= 6; count++) {
            var game = CollectionFixtures.finishedMatch(count);
            var before = game.saveState();
            var pause = new CompletedTrickPause();
            pause.evaluate(game, 0);
            pause.suspend(1499);
            pause.resume(1_000_000);

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "count", count, "game", game, "pause", pause);
            assertEquals("pause.evaluate(game, 1000000).remainingMillis() must equal 1", 1, pause.evaluate(game, 1_000_000).remainingMillis());
            assertFalse("pause.evaluate(game, 1000001).active() must remain false", pause.evaluate(game, 1_000_001).active());
            assertFalse("pause.evaluate(game, 2000000).active() must remain false", pause.evaluate(game, 2_000_000).active());
            assertEquals("game.saveState() must equal before", before, game.saveState());
        }
    }

    @Test public void invalidTimeDoesNotChangeTheOwnerOrRemainingWindow() {
        var game = CollectionFixtures.completed(3, 0, 2, false);
        var other = CollectionFixtures.finishedMatch(3);
        var pause = new CompletedTrickPause();
        pause.evaluate(game, 100);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "other", other, "pause", pause);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> pause.evaluate(other, 99));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> pause.suspend(-1));
        assertEquals("pause.evaluate(game, 101).remainingMillis() must equal 1499", 1499, pause.evaluate(game, 101).remainingMillis());
        pause.suspend(102);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> pause.resume(101));
        pause.resume(1000);
        assertEquals("pause.evaluate(game, 1000).remainingMillis() must equal 1498", 1498, pause.evaluate(game, 1000).remainingMillis());
    }

    @Test public void aRemainingBudgetDoesNotOverflowWhenTheClockIsNearItsMaximum() {
        var game = CollectionFixtures.completed(3, 0, 2, false);
        var pause = new CompletedTrickPause();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "pause", pause);
        assertEquals("pause.evaluate(game, Long.MAX_VALUE - 100).remainingMillis() must equal 1500", 1500, pause.evaluate(game, Long.MAX_VALUE - 100).remainingMillis());
        pause.suspend(Long.MAX_VALUE - 50);
        pause.resume(Long.MAX_VALUE);
        assertEquals("pause.evaluate(game, Long.MAX_VALUE).remainingMillis() must equal 1450", 1450, pause.evaluate(game, Long.MAX_VALUE).remainingMillis());
    }
}
