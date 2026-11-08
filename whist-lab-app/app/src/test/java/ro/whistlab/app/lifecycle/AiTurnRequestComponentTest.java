package ro.whistlab.app.lifecycle;

import static atf.gradle.audit.assertions.GradleAssertions.*;

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

public class AiTurnRequestComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void bothLevelsComputeFromSnapshotAndApplyTheSameBidOnce() {
        for (AiDifficulty level : AiDifficulty.values()) for (int size = 2; size <= 8; size++) {
            var game = deal(size, level);
            var state = game.snapshot();
            var request = new AiTurnRequest(game);
            var expected = AiPolicy.chooseBid(state, state.biddingSeat(), level);
            var decision = request.evaluate();

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "decision", decision, "expected", expected, "game", game, "level", level, "request", request, "size", size, "state", state);
            assertEquals("game.snapshot() must equal state", state, game.snapshot());
            assertEquals("decision must equal new AiTurnRequest.Bid(state.biddingSeat(), expected)", new AiTurnRequest.Bid(state.biddingSeat(), expected), decision);
            assertTrue("request.applyTo(game, decision) must hold", request.applyTo(game, decision));
            assertFalse("request.applyTo(game, decision) must remain false", request.applyTo(game, decision));
            assertEquals("game.snapshot().bidsSubmitted() must equal 1", 1, game.snapshot().bidsSubmitted());
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

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "level", level, "state", state);
            assertNotEquals("state.currentSeat() must differ from 0", 0, state.currentSeat());
            var request = new AiTurnRequest(game);
            var decision = request.evaluate();
            assertEquals("game.snapshot() must equal state", state, game.snapshot());
            var expected = AiPolicy.chooseCard(state, state.currentSeat(), level);
            assertEquals("decision must equal new AiTurnRequest.Play(state.currentSeat(), expected)", new AiTurnRequest.Play(state.currentSeat(), expected), decision);
            assertTrue("request.applyTo(game, decision) must hold", request.applyTo(game, decision));
            assertEquals("game.snapshot().history().size() must equal 1", 1, game.snapshot().history().size());
            assertFalse("request.applyTo(game, decision) must remain false", request.applyTo(game, decision));
        }
    }

    @Test public void anEqualSnapshotFromAnotherSessionCannotReceiveTheResult() {
        var game = deal(2, AiDifficulty.NORMAL);
        var replacement = deal(2, AiDifficulty.NORMAL);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "replacement", replacement);
        assertEquals("replacement.snapshot() must equal game.snapshot()", game.snapshot(), replacement.snapshot());
        var request = new AiTurnRequest(game);
        assertFalse("request.applyTo(replacement, request.evaluate()) must remain false", request.applyTo(replacement, request.evaluate()));
        assertEquals("replacement.snapshot().bidsSubmitted() must equal 0", 0, replacement.snapshot().bidsSubmitted());
    }

    @Test public void changedStateRejectsLateResultEvenWhenTheSessionIdentityMatches() {
        var game = deal(2, AiDifficulty.NORMAL);
        var request = new AiTurnRequest(game);
        var decision = request.evaluate();
        game.submitBid(game.snapshot().biddingSeat(), 0);
        var newer = game.snapshot();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "decision", decision, "game", game, "newer", newer, "request", request);
        assertFalse("request.applyTo(game, decision) must remain false", request.applyTo(game, decision));
        assertEquals("game.snapshot() must equal newer", newer, game.snapshot());
    }

    @Test public void wrongActorOrDecisionKindIsRefusedBeforeMutation() {
        var game = deal(2, AiDifficulty.NORMAL);
        var state = game.snapshot();
        var request = new AiTurnRequest(game);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "request", request, "state", state);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> request.applyTo(game, new AiTurnRequest.Bid(2, 0)));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> request.applyTo(game,
                new AiTurnRequest.Play(1, new Card(Suit.SPADES, 14))));
        assertEquals("game.snapshot() must equal state", state, game.snapshot());
    }

    @Test public void humanTurnCannotBeScheduledAsAutomatedWork() {
        var game = deal(2, AiDifficulty.NORMAL);
        while (game.snapshot().biddingSeat() != 0) game.submitBid(game.snapshot().biddingSeat(), 0);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> new AiTurnRequest(game));
    }

    @Test public void autoGameCanEvaluateAndApplyTheHumanBidAndCardOnlyWhenExplicitlyEnabled() {
        var bidding = deal(2, AiDifficulty.NORMAL, 3);
        var bidState = bidding.snapshot();
        var bidRequest = new AiTurnRequest(bidding, true);
        var expectedBid = AiPolicy.chooseBid(bidState, 0, AiDifficulty.NORMAL);
        var bid = bidRequest.evaluate();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "bid", bid,
                "bidRequest", bidRequest, "bidding", bidding, "bidState", bidState);
        assertEquals("bidState.biddingSeat() must equal 0", 0, bidState.biddingSeat());
        assertEquals("bid must equal the Normal policy for the human seat", new AiTurnRequest.Bid(0, expectedBid), bid);
        assertTrue("bidRequest.applyTo(bidding, bid) must hold", bidRequest.applyTo(bidding, bid));
        assertFalse("a repeated human bid must remain unapplied", bidRequest.applyTo(bidding, bid));

        while (bidding.snapshot().phase() == GamePhase.BIDDING) {
            var state = bidding.snapshot();
            int value = GameRules.allowedBids(state.handSize(), state.bidTotal(),
                    state.bidsSubmitted() == state.players().size() - 1).get(0);
            bidding.submitBid(state.biddingSeat(), value);
        }
        while (bidding.snapshot().currentSeat() != 0) {
            var state = bidding.snapshot();
            var player = state.players().get(state.currentSeat());
            var lead = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
            var card = GameRules.legalCards(player.hand(), lead, state.trump(), state.mode()).get(0);
            bidding.playCard(state.currentSeat(), card);
        }
        var playState = bidding.snapshot();
        var playRequest = new AiTurnRequest(bidding, true);
        var expectedCard = AiPolicy.chooseCard(playState, 0, AiDifficulty.NORMAL);
        var play = playRequest.evaluate();
        assertEquals("play must equal the Normal policy for the human seat", new AiTurnRequest.Play(0, expectedCard), play);
        assertTrue("playRequest.applyTo(bidding, play) must hold", playRequest.applyTo(bidding, play));
        assertFalse("a repeated human card must remain unapplied", playRequest.applyTo(bidding, play));
    }

    private static GameSession deal(int size, AiDifficulty level) {
        return deal(size, level, 0);
    }

    private static GameSession deal(int size, AiDifficulty level, int dealerSeat) {
        var config = GameConfig.newMatch(4, GameMode.ROMANIAN_WHIST, level, 317, true, 10);
        var cards = GameRules.deck(config.mode(), 4);
        var hands = new java.util.ArrayList<List<Card>>();
        for (int seat = 0; seat < 4; seat++) hands.add(List.copyOf(cards.subList(seat * size, (seat + 1) * size)));
        return GameSession.fromDeal(config, hands, Trump.NO_TRUMP, dealerSeat);
    }
}
