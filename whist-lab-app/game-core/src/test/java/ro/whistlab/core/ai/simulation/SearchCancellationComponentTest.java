package ro.whistlab.core.ai.simulation;

import static atf.gradle.audit.assertions.GradleAssertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import org.junit.Test;
import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GameSession;

public class SearchCancellationComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void bothPoliciesRefuseInterruptedWorkWithoutClearingTheInterrupt() {
        var state = game().snapshot();
        for (AiDifficulty level : AiDifficulty.values()) {
            try {
                Thread.currentThread().interrupt();

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "level", level, "state", state);
                assertThrows("The operation rejects its input with CancellationException.class", CancellationException.class, () -> AiPolicy.chooseBid(state, 1, level));
                assertThrows("The operation rejects its input with CancellationException.class", CancellationException.class, () -> AiPolicy.chooseCard(state, 1, level));
                assertTrue("Thread.currentThread().isInterrupted() must hold", Thread.currentThread().isInterrupted());
            } finally { Thread.interrupted(); }
        }
    }

    @Test public void samplingDetectsCancellationDuringWorkAndPreservesTheObservation() {
        var game = game();
        var state = game.snapshot();
        var view = MonteCarloAi.observe(state, 1);
        var random = new Random(17) {
            @Override public int nextInt(int bound) {
                Thread.currentThread().interrupt();
                return super.nextInt(bound);
            }
        };
        try {

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "random", random, "state", state, "view", view);
            assertThrows("The operation rejects its input with CancellationException.class", CancellationException.class, () -> PossibleDeals.sample(view, random));
            assertTrue("Thread.currentThread().isInterrupted() must hold", Thread.currentThread().isInterrupted());
            assertEquals("game.snapshot() must equal state", state, game.snapshot());
        } finally { Thread.interrupted(); }
    }

    @Test public void normalSearchIsDeterministicAndDoesNotMarkTheThreadInterrupted() {
        var state = game().snapshot();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "state", state);
        assertEquals("AiPolicy.chooseBid(state, 1, AiDifficulty.HARD) must equal AiPolicy.chooseBid(state, 1, AiDifficulty.HARD)", AiPolicy.chooseBid(state, 1, AiDifficulty.HARD), AiPolicy.chooseBid(state, 1, AiDifficulty.HARD));
        assertFalse("Thread.currentThread().isInterrupted() must remain false", Thread.currentThread().isInterrupted());
    }

    private static GameSession game() {
        var config = GameConfig.newMatch(4, GameMode.ROMANIAN_WHIST, AiDifficulty.HARD, 317, true, 10);
        var cards = GameRules.deck(config.mode(), 4);
        var hands = new ArrayList<List<Card>>();
        for (int seat = 0; seat < 4; seat++) hands.add(List.copyOf(cards.subList(seat * 2, (seat + 1) * 2)));
        return GameSession.fromDeal(config, hands, Trump.NO_TRUMP, 0);
    }
}
