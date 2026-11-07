package ro.whistlab.app.bidding;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.ArrayDeque;
import java.util.List;
import org.junit.Test;
import ro.whistlab.app.lifecycle.AiTurnCoordinator;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.session.GamePhase;

public final class HumanBidSelectionComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void anAceOfTrumpSelectsOneInsteadOfTheFirstLegalOptionWithoutSubmittingIt() {
        var game = BidSelectionFixtures.ace(); var selection = new HumanBidSelection();
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "selection", selection);
        assertTrue("selection.selected(game).isEmpty() must hold", selection.selected(game).isEmpty());
        var request = selection.request(game).orElseThrow();
        assertTrue("selection.request(game).isEmpty() must hold", selection.request(game).isEmpty());
        var before = game.snapshot();
        int recommended = request.evaluate();
        assertEquals("recommended must equal 1", 1, recommended);
        assertTrue("selection.apply(game, request, recommended) must hold", selection.apply(game, request, recommended));
        assertEquals("selection.selected(game).orElseThrow() must equal 1", 1, selection.selected(game).orElseThrow());
        assertEquals("game.snapshot() must equal before", before, game.snapshot());
        assertNull("game.snapshot().players().get(0).bid() remains absent", game.snapshot().players().get(0).bid());
        assertTrue("selection.request(game).isEmpty() must hold", selection.request(game).isEmpty());
        assertFalse("selection.apply(game, request, 0) must remain false", selection.apply(game, request, 0));
    }

    @Test public void aLowNonTrumpCardStillPrefersZeroWhenBothOptionsAreLegal() {
        var game = BidSelectionFixtures.deal(List.of(new Card(Suit.CLUBS, 7)), Trump.SPADES, 3);
        var selection = new HumanBidSelection(); var request = selection.request(game).orElseThrow();
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "request", request, "selection", selection);
        assertEquals("request.evaluate() must equal 0", 0, request.evaluate());
        assertTrue("selection.apply(game, request, 0) must hold", selection.apply(game, request, 0));
        assertEquals("selection.selected(game).orElseThrow() must equal 0", 0, selection.selected(game).orElseThrow());
    }

    @Test public void manualChoiceCancelsEvenACompletedButNotYetDeliveredRecommendation() {
        var game = BidSelectionFixtures.ace(); var selection = new HumanBidSelection();
        var request = selection.request(game).orElseThrow();
        var worker = new BidSelectionFixtures.Worker(); var owner = new ArrayDeque<Runnable>();
        try (var coordinator = new AiTurnCoordinator(worker, owner::add)) {
            coordinator.submit(request::evaluate, bid -> selection.apply(game, request, bid), failure -> fail(failure.toString()));
            worker.runNext();
            
            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "owner", owner, "request", request, "selection", selection, "worker", worker);
            assertTrue("selection.selected(game).isEmpty() must hold", selection.selected(game).isEmpty());
            selection.select(game, 0);
            coordinator.cancel();
            owner.remove().run();
            assertEquals("selection.selected(game).orElseThrow() must equal 0", 0, selection.selected(game).orElseThrow());
            assertNull("game.snapshot().players().get(0).bid() remains absent", game.snapshot().players().get(0).bid());
            assertTrue("selection.request(game).isEmpty() must hold", selection.request(game).isEmpty());
        }
    }

    @Test public void manualChoiceHasPriorityEvenIfTheCallerDoesNotCancelItsWorker() {
        var game = BidSelectionFixtures.ace(); var selection = new HumanBidSelection();
        var request = selection.request(game).orElseThrow();
        selection.select(game, 0);
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "request", request, "selection", selection);
        assertFalse("selection.apply(game, request, 1) must remain false", selection.apply(game, request, 1));
        assertEquals("selection.selected(game).orElseThrow() must equal 0", 0, selection.selected(game).orElseThrow());
    }

    @Test public void cancelledAdviceCanBeRetriedAndAnOldTicketCannotReplaceTheNewOne() {
        var game = BidSelectionFixtures.ace(); var selection = new HumanBidSelection();
        var first = selection.request(game).orElseThrow();
        selection.cancel();
        var second = selection.request(game).orElseThrow();
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "first", first, "game", game, "second", second, "selection", selection);
        assertNotSame("second is a distinct instance from first", first, second);
        assertEquals("second.observation() must equal first.observation()", first.observation(), second.observation());
        assertFalse("selection.apply(game, first, 0) must remain false", selection.apply(game, first, 0));
        assertTrue("selection.apply(game, second, 1) must hold", selection.apply(game, second, 1));
        selection.cancel();
        assertEquals("selection.selected(game).orElseThrow() must equal 1", 1, selection.selected(game).orElseThrow());
    }

    @Test public void anEqualLookingNewGameRejectsThePreviousGamesAdviceAndManualChoice() {
        var oldGame = BidSelectionFixtures.ace(); var newGame = BidSelectionFixtures.ace();
        var selection = new HumanBidSelection(); var request = selection.request(oldGame).orElseThrow();
        selection.select(oldGame, 0);
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "newGame", newGame, "oldGame", oldGame, "request", request, "selection", selection);
        assertTrue("selection.selected(newGame).isEmpty() must hold", selection.selected(newGame).isEmpty());
        assertFalse("selection.apply(newGame, request, 1) must remain false", selection.apply(newGame, request, 1));
        assertTrue("selection.request(newGame).isPresent() must hold", selection.request(newGame).isPresent());
    }

    @Test public void adviceFromAnEarlierPhaseCannotSetAPlayingSelectionOrAnOpponentsBid() {
        var game = BidSelectionFixtures.ace(); var selection = new HumanBidSelection();
        var request = selection.request(game).orElseThrow();
        game.submitBid(0, 1);
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "request", request, "selection", selection);
        assertFalse("selection.apply(game, request, 1) must remain false", selection.apply(game, request, 1));
        assertTrue("selection.selected(game).isEmpty() must hold", selection.selected(game).isEmpty());
        assertTrue("selection.request(game).isEmpty() must hold", selection.request(game).isEmpty());
        assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class, () -> selection.select(game, 0));
        while (game.snapshot().phase() == GamePhase.BIDDING) game.submitBid(game.snapshot().biddingSeat(), 1);
        assertTrue("selection.request(game).isEmpty() must hold", selection.request(game).isEmpty());
    }

    @Test public void failureLeavesManualBiddingAvailableWithoutAnAutomaticRetryLoop() {
        var game = BidSelectionFixtures.ace(); var selection = new HumanBidSelection();
        var request = selection.request(game).orElseThrow();
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "request", request, "selection", selection);
        assertTrue("selection.failed(game, request) must hold", selection.failed(game, request));
        assertFalse("selection.failed(game, request) must remain false", selection.failed(game, request));
        assertTrue("selection.selected(game).isEmpty() must hold", selection.selected(game).isEmpty());
        assertTrue("selection.request(game).isEmpty() must hold", selection.request(game).isEmpty());
        selection.select(game, 1);
        assertEquals("selection.selected(game).orElseThrow() must equal 1", 1, selection.selected(game).orElseThrow());
    }

    @Test public void invalidResultsAndInvalidManualChoicesNeverReplaceTheLegalSelection() {
        var game = BidSelectionFixtures.ace(); var selection = new HumanBidSelection();
        var request = selection.request(game).orElseThrow();
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "request", request, "selection", selection);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> selection.apply(game, request, 2));
        assertTrue("selection.selected(game).isEmpty() must hold", selection.selected(game).isEmpty());
        assertTrue("selection.apply(game, request, 1) must hold", selection.apply(game, request, 1));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> selection.select(game, -1));
        assertEquals("selection.selected(game).orElseThrow() must equal 1", 1, selection.selected(game).orElseThrow());
    }

    @Test public void renamingTheHumanDoesNotDiscardAChoiceOrRecalculateIdenticalCardInformation() {
        var game = BidSelectionFixtures.ace(); var selection = new HumanBidSelection();
        selection.select(game, 0);
        game.renameHuman("Alt nume");
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "selection", selection);
        assertEquals("selection.selected(game).orElseThrow() must equal 0", 0, selection.selected(game).orElseThrow());
        assertTrue("selection.request(game).isEmpty() must hold", selection.request(game).isEmpty());
    }

    @Test public void aNewHandRequiresAFreshRecommendationEvenInTheSameSession() {
        var game = BidSelectionFixtures.ace(); var selection = new HumanBidSelection();
        var request = selection.request(game).orElseThrow();
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "request", request, "selection", selection);
        assertTrue("selection.apply(game, request, 1) must hold", selection.apply(game, request, 1));
        while (game.snapshot().phase() == GamePhase.BIDDING) {
            var state = game.snapshot();
            game.submitBid(state.biddingSeat(), ro.whistlab.core.rules.GameRules.allowedBids(state.handSize(),
                    state.bidTotal(), state.bidsSubmitted() == state.players().size() - 1).get(0));
        }
        while (game.snapshot().phase() == GamePhase.PLAYING) {
            var state = game.snapshot();
            game.playCard(state.currentSeat(), state.players().get(state.currentSeat()).hand().get(0));
        }
        game.startNextHand();
        while (game.snapshot().biddingSeat() != 0) game.submitBid(game.snapshot().biddingSeat(), 0);
        assertTrue("selection.selected(game).isEmpty() must hold", selection.selected(game).isEmpty());
        assertTrue("selection.request(game).isPresent() must hold", selection.request(game).isPresent());
        assertFalse("selection.apply(game, request, 1) must remain false", selection.apply(game, request, 1));
    }
}
