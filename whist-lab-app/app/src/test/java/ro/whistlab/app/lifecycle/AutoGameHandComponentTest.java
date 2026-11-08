package ro.whistlab.app.lifecycle;

import static atf.gradle.audit.assertions.GradleAssertions.*;

import java.util.List;
import org.junit.Test;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

public class AutoGameHandComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void automationStaysWithinTheStartedHandAndStopsAtItsScoreBoundary() {
        var game = oneCardHand();
        var automation = new AutoGameHand();
        var bidding = game.snapshot();
        automation.start(bidding);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "automation", automation,
                "bidding", bidding, "game", game);
        assertTrue("automation.activeFor(bidding) must hold", automation.activeFor(bidding));
        while (game.snapshot().phase() == GamePhase.BIDDING) {
            var state = game.snapshot();
            int bid = GameRules.allowedBids(state.handSize(), state.bidTotal(),
                    state.bidsSubmitted() == state.players().size() - 1).get(0);
            game.submitBid(state.biddingSeat(), bid);
        }
        assertTrue("automation.activeFor(game.snapshot()) must hold after bidding", automation.activeFor(game.snapshot()));

        while (game.snapshot().phase() == GamePhase.PLAYING) {
            var state = game.snapshot();
            var player = state.players().get(state.currentSeat());
            Suit lead = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
            Card card = GameRules.legalCards(player.hand(), lead, state.trump(), state.mode()).get(0);
            game.playCard(state.currentSeat(), card);
        }
        var score = game.snapshot();
        assertEquals("score.phase() must equal GamePhase.SCORE", GamePhase.SCORE, score.phase());
        assertFalse("automation.activeFor(score) must clear the completed hand", automation.activeFor(score));
        assertThrows("Starting auto-game at the score boundary must throw IllegalStateException",
                IllegalStateException.class, () -> automation.start(score));
        game.startNextHand();
        assertFalse("automation.activeFor(game.snapshot()) must not carry to the next hand",
                automation.activeFor(game.snapshot()));
    }

    @Test public void automationCanStartAfterBiddingWhenTheHumanTurnIsInPlay() {
        var game = oneCardHand();
        while (game.snapshot().phase() == GamePhase.BIDDING) {
            var state = game.snapshot();
            int bid = GameRules.allowedBids(state.handSize(), state.bidTotal(),
                    state.bidsSubmitted() == state.players().size() - 1).get(0);
            game.submitBid(state.biddingSeat(), bid);
        }
        var automation = new AutoGameHand();
        automation.start(game.snapshot());

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "automation", automation,
                "game", game, "playing", game.snapshot());
        assertEquals("game.snapshot().phase() must equal GamePhase.PLAYING", GamePhase.PLAYING, game.snapshot().phase());
        assertTrue("automation.activeFor(game.snapshot()) must hold when started during play",
                automation.activeFor(game.snapshot()));
        automation.stop();
        assertFalse("automation.activeFor(game.snapshot()) must be false after the explicit stop",
                automation.activeFor(game.snapshot()));
    }

    @Test public void manualStopCannotLeakAutomation() {
        var game = oneCardHand();
        var automation = new AutoGameHand();
        automation.start(game.snapshot());

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "automation", automation, "game", game);
        assertTrue("automation.activeFor(game.snapshot()) must be true before manual stop",
                automation.activeFor(game.snapshot()));
        automation.stop();
        assertFalse("automation.activeFor(game.snapshot()) must remain false after stop", automation.activeFor(game.snapshot()));
    }

    private static GameSession oneCardHand() {
        var config = GameConfig.newMatch(4, GameMode.ROMANIAN_WHIST, AiDifficulty.NORMAL, 9081, true, 10);
        return GameSession.fromDeal(config, List.of(
                List.of(new Card(Suit.HEARTS, 14)), List.of(new Card(Suit.HEARTS, 13)),
                List.of(new Card(Suit.HEARTS, 12)), List.of(new Card(Suit.HEARTS, 11))), Trump.NO_TRUMP, 3);
    }
}
