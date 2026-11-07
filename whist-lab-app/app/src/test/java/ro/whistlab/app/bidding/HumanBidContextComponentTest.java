package ro.whistlab.app.bidding;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.session.GameSession;

public final class HumanBidContextComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void everySupportedTableUsesTheBestEstimateEvenAgainstNormalOpponents() {
        for (int players = 3; players <= 6; players++) {
            var game = BidSelectionFixtures.deal(List.of(new Card(Suit.SPADES, 14)), Trump.SPADES, players - 1, players);
            var selection = new HumanBidSelection(); var request = selection.request(game).orElseThrow();
            
            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "players", players, "request", request, "selection", selection);
            assertEquals("request.observation().seats().size() must equal players", players, request.observation().seats().size());
            assertEquals("request.evaluate() must equal 1", 1, request.evaluate());
            assertTrue("selection.apply(game, request, 1) must hold", selection.apply(game, request, 1));
            assertEquals("selection.selected(game).orElseThrow() must equal 1", 1, selection.selected(game).orElseThrow());
        }
    }
    @Test public void theDealerCannotSelectTheOtherwiseCertainWinningBidWhenItIsForbidden() {
        var game = BidSelectionFixtures.deal(List.of(new Card(Suit.SPADES, 14)), Trump.SPADES, 0);
        for (int seat = 1; seat < 4; seat++) game.submitBid(seat, 0);
        var selection = new HumanBidSelection(); var request = selection.request(game).orElseThrow();
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "request", request, "selection", selection);
        assertEquals("request.evaluate() must equal 0", 0, request.evaluate());
        assertTrue("selection.apply(game, request, 0) must hold", selection.apply(game, request, 0));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> selection.select(game, 1));
    }

    @Test public void aZeroEstimateBecomesOneWhenTheLastBiddersZeroWouldCompleteTheTotal() {
        var game = BidSelectionFixtures.deal(List.of(new Card(Suit.CLUBS, 7)), Trump.SPADES, 0);
        game.submitBid(1, 1); game.submitBid(2, 0); game.submitBid(3, 0);
        var selection = new HumanBidSelection(); var request = selection.request(game).orElseThrow();
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "request", request, "selection", selection);
        assertEquals("request.evaluate() must equal 1", 1, request.evaluate());
        assertTrue("selection.apply(game, request, 1) must hold", selection.apply(game, request, 1));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> selection.select(game, 0));
    }

    @Test public void certainTrumpWinnersAreEvaluatedAtTwoToEightCardsIncludingNoTrumpAtEight() {
        for (int size = 2; size <= 8; size++) {
            var hand = new ArrayList<Card>();
            for (int card = 0; card < size; card++) hand.add(new Card(Suit.SPADES, 14 - card));
            var game = BidSelectionFixtures.deal(hand, size == 8 ? Trump.NO_TRUMP : Trump.SPADES, 3);
            var selection = new HumanBidSelection(); var request = selection.request(game).orElseThrow();
            
            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "hand", hand, "request", request, "selection", selection, "size", size);
            assertEquals("The human leads with all the highest trumps, or the entire led suit without trump", size, request.evaluate());
            assertNull("game.snapshot().players().get(0).bid() remains absent", game.snapshot().players().get(0).bid());
        }
    }

    @Test public void hiddenOpponentCardsCannotChangeTheObservationOrTheRecommendation() {
        var game = BidSelectionFixtures.ace(); var state = game.snapshot();
        var hands = new ArrayList<List<Card>>(); hands.add(state.players().get(0).hand());
        for (int seat = 3; seat > 0; seat--) hands.add(state.players().get(seat).hand());
        var permuted = GameSession.fromDeal(game.config(), hands, state.trump(), state.dealerSeat());
        var first = new HumanBidSelection().request(game).orElseThrow();
        var second = new HumanBidSelection().request(permuted).orElseThrow();
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "first", first, "game", game, "hands", hands, "permuted", permuted, "second", second, "state", state);
        assertEquals("second.observation() must equal first.observation()", first.observation(), second.observation());
        assertEquals("first.evaluate() must equal 1", 1, first.evaluate());
        assertEquals("second.evaluate() must equal 1", 1, second.evaluate());
    }
}
