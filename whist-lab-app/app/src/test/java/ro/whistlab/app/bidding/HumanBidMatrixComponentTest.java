package ro.whistlab.app.bidding;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.model.Trump;

@RunWith(Parameterized.class)
public final class HumanBidMatrixComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Parameterized.Parameters(name = "{0} players, {1} cards")
    public static Collection<Object[]> configurations() { return BiddingMatrixFixtures.tableSizes(); }

    private final int players;
    private final int size;

    public HumanBidMatrixComponentTest(int players, int size) {
        this.players = players;
        this.size = size;
    }

    @Test public void aLeaderHoldingTheHighestSuitRunsPrefersTheFullContract() {
        var game = BiddingMatrixFixtures.certainWinners(players, size);
        var before = game.saveState(); var selection = new HumanBidSelection();
        var request = selection.request(game).orElseThrow();
        // All trumps are held when the short deck requires a second suit. At eight cards,
        // there is no trump and every card is part of a top sequence in its own suit.
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "game", game, "request", request, "selection", selection);
        assertEquals("request.evaluate() must equal size", size, request.evaluate());
        assertTrue("selection.apply(game, request, size) must hold", selection.apply(game, request, size));
        assertEquals("selection.selected(game).orElseThrow() must equal size", size, selection.selected(game).orElseThrow());
        assertEquals("game.saveState() must equal before", before, game.saveState());
    }

    @Test public void eachBiddingPositionUsesTheCurrentObservationAndKeepsItsRecommendationLegal() {
        for (int position = 0; position < players; position++) {
            var game = BiddingMatrixFixtures.humanTurn(players, size, position);
            var before = game.saveState(); var state = game.snapshot();
            
            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "game", game, "position", position, "state", state);
            assertEquals("state.bidsSubmitted() must equal position", position, state.bidsSubmitted());
            assertEquals("state.handSize() must equal size", size, state.handSize());
            assertEquals("state.trump() == Trump.NO_TRUMP must equal size == 8", size == 8, state.trump() == Trump.NO_TRUMP);
            assertEquals("state.turnedCard() == null must equal size == 8", size == 8, state.turnedCard() == null);
            var selection = new HumanBidSelection(); var request = selection.request(game).orElseThrow();
            var observed = request.observation();
            assertEquals("observed must equal MonteCarloAi.observe(state, 0)", MonteCarloAi.observe(state, 0), observed);
            assertEquals("observed.hand() must equal state.players().get(0).hand()", state.players().get(0).hand(), observed.hand());
            assertEquals("observed.turnedCard() must equal state.turnedCard()", state.turnedCard(), observed.turnedCard());
            assertEquals("observed.streakBonusPoints() must equal state.streakBonusPoints()", state.streakBonusPoints(), observed.streakBonusPoints());
            assertEquals("observed.streakPenaltyPoints() must equal state.streakPenaltyPoints()", state.streakPenaltyPoints(), observed.streakPenaltyPoints());
            assertTrue("observed.streakBonuses() must hold", observed.streakBonuses());
            int recommendation = request.evaluate();
            var legal = BiddingMatrixFixtures.legalBids(state);
            assertTrue("Position " + position + ": " + recommendation + " must belong to " + legal, legal.contains(recommendation));
            assertTrue("selection.apply(game, request, recommendation) must hold", selection.apply(game, request, recommendation));
            assertEquals("selection.selected(game).orElseThrow() must equal recommendation", recommendation, selection.selected(game).orElseThrow());
            assertEquals("game.saveState() must equal before", before, game.saveState());
            int manual = legal.stream().filter(bid -> bid != recommendation).findFirst().orElse(recommendation);
            selection.select(game, manual);
            selection.cancel();
            assertEquals("selection.selected(game).orElseThrow() must equal manual", manual, selection.selected(game).orElseThrow());
            assertTrue("selection.request(game).isEmpty() must hold", selection.request(game).isEmpty());
            assertEquals("game.saveState() must equal before", before, game.saveState());
        }
    }

    @Test public void aManualChoiceBeforeDeliveryRejectsLateSuccessAndFailureInEveryPosition() {
        for (int position = 0; position < players; position++) {
            var game = BiddingMatrixFixtures.humanTurn(players, size, position);
            var before = game.saveState(); var selection = new HumanBidSelection();
            var request = selection.request(game).orElseThrow();
            var legal = BiddingMatrixFixtures.legalBids(game.snapshot());
            int manual = legal.get(legal.size() - 1);
            selection.select(game, manual);
            
            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "game", game, "legal", legal, "manual", manual, "position", position, "request", request, "selection", selection);
            assertFalse("selection.apply(game, request, legal.get(0)) must remain false", selection.apply(game, request, legal.get(0)));
            assertFalse("selection.failed(game, request) must remain false", selection.failed(game, request));
            assertEquals("selection.selected(game).orElseThrow() must equal manual", manual, selection.selected(game).orElseThrow());
            assertTrue("selection.request(game).isEmpty() must hold", selection.request(game).isEmpty());
            if (position == players - 1) {
                int forbidden = size - game.snapshot().bidTotal();
                assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> selection.select(game, forbidden));
                assertEquals("selection.selected(game).orElseThrow() must equal manual", manual, selection.selected(game).orElseThrow());
            }
            assertEquals("game.saveState() must equal before", before, game.saveState());
        }
    }
}
