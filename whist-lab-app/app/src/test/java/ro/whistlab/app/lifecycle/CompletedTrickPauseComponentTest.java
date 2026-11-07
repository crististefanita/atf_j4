package ro.whistlab.app.lifecycle;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;
import static atf.gradle.audit.assertions.GradleAssertions.assertFalse;
import static atf.gradle.audit.assertions.GradleAssertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;

public final class CompletedTrickPauseComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test
    public void completedTrickRemainsVisibleForExactlyFifteenHundredMilliseconds() {
        GameSession game = completedFirstTrick();
        List<PlayedCard> completed = game.snapshot().lastTrick();
        CompletedTrickPause pause = new CompletedTrickPause();

        CompletedTrickPause.DisplayState start = pause.evaluate(game.snapshot(), 10_000L);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "completed", completed, "game", game, "pause", pause, "start", start);
        assertTrue("start.active() must hold", start.active());
        assertEquals("start.remainingMillis() must equal CompletedTrickPause.DISPLAY_MILLIS", CompletedTrickPause.DISPLAY_MILLIS, start.remainingMillis());
        assertEquals("start.cards() must equal completed", completed, start.cards());

        CompletedTrickPause.DisplayState beforeDeadline = pause.evaluate(game.snapshot(), 11_499L);
        assertTrue("beforeDeadline.active() must hold", beforeDeadline.active());
        assertEquals("beforeDeadline.remainingMillis() must equal 1L", 1L, beforeDeadline.remainingMillis());
        assertEquals("beforeDeadline.cards() must equal completed", completed, beforeDeadline.cards());

        assertFalse("pause.evaluate(game.snapshot(), 11500L).active() must remain false", pause.evaluate(game.snapshot(), 11_500L).active());
        assertFalse("pause.evaluate(game.snapshot(), 12000L).active() must remain false", pause.evaluate(game.snapshot(), 12_000L).active());
    }

    @Test
    public void theNextCompletedTrickStartsANewDisplayWindow() {
        GameSession game = completedFirstTrick();
        CompletedTrickPause pause = new CompletedTrickPause();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "pause", pause);
        assertTrue("pause.evaluate(game.snapshot(), 100L).active() must hold", pause.evaluate(game.snapshot(), 100L).active());
        game.playCard(game.snapshot().currentSeat(), new Card(Suit.CLUBS, 14));
        assertFalse("pause.evaluate(game.snapshot(), 200L).active() must remain false", pause.evaluate(game.snapshot(), 200L).active());

        for (int seat = 1; seat < 4; seat++) {
            GameSession.Snapshot state = game.snapshot();
            Card card = state.players().get(state.currentSeat()).hand().stream()
                    .filter(candidate -> candidate.suit() == Suit.CLUBS).findFirst().orElseThrow();
            game.playCard(state.currentSeat(), card);
        }

        CompletedTrickPause.DisplayState next = pause.evaluate(game.snapshot(), 500L);
        assertTrue("next.active() must hold", next.active());
        assertEquals("next.remainingMillis() must equal CompletedTrickPause.DISPLAY_MILLIS", CompletedTrickPause.DISPLAY_MILLIS, next.remainingMillis());
        assertEquals("next.cards().size() must equal 4", 4, next.cards().size());
    }

    private static GameSession completedFirstTrick() {
        GameConfig config = new GameConfig(4, GameMode.OH_HELL, AiDifficulty.NORMAL, 20261103L);
        List<List<Card>> hands = Arrays.asList(
                Arrays.asList(new Card(Suit.HEARTS, 14), new Card(Suit.CLUBS, 14)),
                Arrays.asList(new Card(Suit.HEARTS, 13), new Card(Suit.CLUBS, 13)),
                Arrays.asList(new Card(Suit.HEARTS, 12), new Card(Suit.CLUBS, 12)),
                Arrays.asList(new Card(Suit.HEARTS, 11), new Card(Suit.CLUBS, 11)));
        GameSession game = GameSession.fromDeal(config, hands, Trump.SPADES, 3);
        while (game.snapshot().phase() == GamePhase.BIDDING) {
            game.submitBid(game.snapshot().biddingSeat(), 0);
        }
        for (int play = 0; play < 4; play++) {
            GameSession.Snapshot state = game.snapshot();
            Suit led = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
            Card card = GameRules.legalCards(state.players().get(state.currentSeat()).hand(), led,
                    state.trump(), state.mode()).get(0);
            if (state.currentTrick().isEmpty()) card = new Card(Suit.HEARTS, 14);
            game.playCard(state.currentSeat(), card);
        }
        return game;
    }
}
