package ro.whistlab.app.lifecycle;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;
import ro.whistlab.core.ai.policy.AiPolicy;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

public class AiTurnRequestComponentTest {
    @Test public void bothLevelsComputeFromSnapshotAndApplyTheSameBidOnce() {
        for (AiDifficulty level : AiDifficulty.values()) for (int size = 2; size <= 8; size++) {
            var game = deal(size, level);
            var state = game.snapshot();
            var request = new AiTurnRequest(game);
            var expected = AiPolicy.chooseBid(state, state.biddingSeat(), level);
            var decision = request.evaluate();
            assertEquals(state, game.snapshot());
            assertEquals(new AiTurnRequest.Bid(state.biddingSeat(), expected), decision);
            assertTrue(request.applyTo(game, decision));
            assertFalse(request.applyTo(game, decision));
            assertEquals(1, game.snapshot().bidsSubmitted());
        }
    }

    @Test public void cardCalculationDoesNotMutateAndApplicationUsesTheCapturedActor() {
        for (AiDifficulty level : AiDifficulty.values()) {
            var game = deal(2, level);
            while (game.snapshot().phase() == GamePhase.BIDDING) {
                var state = game.snapshot();
                game.submitBid(state.biddingSeat(), GameRules.allowedBids(state.handSize(), state.bidTotal(),
                        state.bidsSubmitted() == 3).get(0));
            }
            var state = game.snapshot();
            assertNotEquals(0, state.currentSeat());
            var request = new AiTurnRequest(game);
            var decision = request.evaluate();
            assertEquals(state, game.snapshot());
            var expected = AiPolicy.chooseCard(state, state.currentSeat(), level);
            assertEquals(new AiTurnRequest.Play(state.currentSeat(), expected), decision);
            assertTrue(request.applyTo(game, decision));
            assertEquals(1, game.snapshot().history().size());
            assertFalse(request.applyTo(game, decision));
        }
    }

    @Test public void anEqualSnapshotFromAnotherSessionCannotReceiveTheResult() {
        var game = deal(2, AiDifficulty.NORMAL);
        var replacement = deal(2, AiDifficulty.NORMAL);
        assertEquals(game.snapshot(), replacement.snapshot());
        var request = new AiTurnRequest(game);
        assertFalse(request.applyTo(replacement, request.evaluate()));
        assertEquals(0, replacement.snapshot().bidsSubmitted());
    }

    @Test public void changedStateRejectsLateResultEvenWhenTheSessionIdentityMatches() {
        var game = deal(2, AiDifficulty.NORMAL);
        var request = new AiTurnRequest(game);
        var decision = request.evaluate();
        game.submitBid(game.snapshot().biddingSeat(), 0);
        var newer = game.snapshot();
        assertFalse(request.applyTo(game, decision));
        assertEquals(newer, game.snapshot());
    }

    @Test public void wrongActorOrDecisionKindIsRefusedBeforeMutation() {
        var game = deal(2, AiDifficulty.NORMAL);
        var state = game.snapshot();
        var request = new AiTurnRequest(game);
        assertThrows(IllegalArgumentException.class, () -> request.applyTo(game, new AiTurnRequest.Bid(2, 0)));
        assertThrows(IllegalArgumentException.class, () -> request.applyTo(game,
                new AiTurnRequest.Play(1, new Card(Suit.SPADES, 14))));
        assertEquals(state, game.snapshot());
    }

    @Test public void humanTurnCannotBeScheduledAsAutomatedWork() {
        var game = deal(2, AiDifficulty.NORMAL);
        while (game.snapshot().biddingSeat() != 0) game.submitBid(game.snapshot().biddingSeat(), 0);
        assertThrows(IllegalArgumentException.class, () -> new AiTurnRequest(game));
    }

    private static GameSession deal(int size, AiDifficulty level) {
        var config = GameConfig.newMatch(4, GameMode.ROMANIAN_WHIST, level, 317, true, 10);
        var cards = GameRules.deck(config.mode(), 4);
        var hands = new java.util.ArrayList<List<Card>>();
        for (int seat = 0; seat < 4; seat++) hands.add(List.copyOf(cards.subList(seat * size, (seat + 1) * size)));
        return GameSession.fromDeal(config, hands, Trump.NO_TRUMP, 0);
    }
}
