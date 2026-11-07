package ro.whistlab.app.animation;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;

public class TrickCollectionRequestComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void everySeatCanCollectACompleteRomanianTrickWithOrWithoutATrumpWinner() {
        for (int count = 3; count <= 6; count++) for (int winner = 0; winner < count; winner++)
            for (boolean trump : new boolean[]{false, true}) {
                var game = CollectionFixtures.completed(count, winner, 2, trump);
                var before = game.snapshot();
                var request = TrickCollectionRequest.from(before, before.lastTrick()).orElseThrow();
                String context = "players=" + count + ", winner=" + winner + ", trump=" + trump;

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "context", context, "count", count, "game", game, "request", request, "trump", trump, "winner", winner);
                assertEquals(context, winner, request.winner());
                assertEquals(context, winner, GameRules.winningSeat(before.lastTrick(), before.trump()));
                var order = new ArrayList<Integer>();
                for (int step = 1; step <= count; step++) order.add((winner + step) % count);
                assertEquals(context, order, request.seats());
                assertEquals("game.snapshot() must equal before", before, game.snapshot());
            }
    }

    @Test public void theActualLastMatchTrickRemainsEligibleAtEveryTableSize() {
        for (int count = 3; count <= 6; count++) {
            var game = CollectionFixtures.finishedMatch(count);
            var state = game.snapshot();

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "count", count, "game", game, "state", state);
            assertEquals("state.phase() must equal GamePhase.SCORE", GamePhase.SCORE, state.phase());
            assertTrue("state.finalHand() must hold", state.finalHand());
            var request = TrickCollectionRequest.from(state, state.lastTrick()).orElseThrow();
            assertEquals("request.seats().size() must equal count", count, request.seats().size());
            assertEquals("request.winner() must equal GameRules.winningSeat(state.lastTrick(), state.trump())", GameRules.winningSeat(state.lastTrick(), state.trump()), request.winner());
        }
    }

    @Test public void partialForeignOrStalePublicCardsCannotStartACollection() {
        var game = CollectionFixtures.completed(4, 0, 2, false);
        var s = game.snapshot();
        var cards = s.lastTrick();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "cards", cards, "game", game, "s", s);
        assertFalse("TrickCollectionRequest.from(s, cards.subList(0, 3)).isPresent() must remain false", TrickCollectionRequest.from(s, cards.subList(0, 3)).isPresent());
        assertFalse("TrickCollectionRequest.from(CollectionFixtures.observation(s, GamePhase.BIDDING, s.currentTrick(), cards, s.history(), s.lastTrickWinnerSeat()), cards).isPresent() must remain false", TrickCollectionRequest.from(CollectionFixtures.observation(s, GamePhase.BIDDING,
                s.currentTrick(), cards, s.history(), s.lastTrickWinnerSeat()), cards).isPresent());
        assertFalse("TrickCollectionRequest.from(CollectionFixtures.observation(s, s.phase(), List.of(cards.get(0)), cards, s.history(), s.lastTrickWinnerSeat()), cards).isPresent() must remain false", TrickCollectionRequest.from(CollectionFixtures.observation(s, s.phase(), List.of(cards.get(0)),
                cards, s.history(), s.lastTrickWinnerSeat()), cards).isPresent());
        var reversed = new ArrayList<>(s.history());
        java.util.Collections.reverse(reversed);
        assertFalse("TrickCollectionRequest.from(CollectionFixtures.observation(s, s.phase(), List.of(), cards, reversed, s.lastTrickWinnerSeat()), cards).isPresent() must remain false", TrickCollectionRequest.from(CollectionFixtures.observation(s, s.phase(), List.of(), cards,
                reversed, s.lastTrickWinnerSeat()), cards).isPresent());
        CollectionFixtures.playLegal(game);
        assertFalse("TrickCollectionRequest.from(game.snapshot(), cards).isPresent() must remain false", TrickCollectionRequest.from(game.snapshot(), cards).isPresent());
    }

    @Test public void duplicateFacesMissingSeatsAndInvalidWinnersAreRejected() {
        var s = CollectionFixtures.completed(4, 0, 2, false).snapshot();
        for (int winner : new int[]{-1, 4}) {
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "s", s, "winner", winner);
assertFalse("TrickCollectionRequest.from(CollectionFixtures.observation(s, s.phase(), List.of(), s.lastTrick(), s.history(), winner), s.lastTrick()).isPresent() must remain false", TrickCollectionRequest.from(
                CollectionFixtures.observation(s, s.phase(), List.of(), s.lastTrick(), s.history(), winner), s.lastTrick()).isPresent()); }
        var duplicateSeat = new ArrayList<>(s.lastTrick());
        duplicateSeat.set(1, new PlayedCard(duplicateSeat.get(0).seat(), duplicateSeat.get(1).card()));
        assertFalse("TrickCollectionRequest.from(CollectionFixtures.observation(s, s.phase(), List.of(), duplicateSeat, duplicateSeat, s.lastTrickWinnerSeat()), duplicateSeat).isPresent() must remain false", TrickCollectionRequest.from(CollectionFixtures.observation(s, s.phase(), List.of(), duplicateSeat,
                duplicateSeat, s.lastTrickWinnerSeat()), duplicateSeat).isPresent());
        var duplicateCard = new ArrayList<>(s.lastTrick());
        duplicateCard.set(1, new PlayedCard(duplicateCard.get(1).seat(), duplicateCard.get(0).card()));
        assertFalse("TrickCollectionRequest.from(CollectionFixtures.observation(s, s.phase(), List.of(), duplicateCard, duplicateCard, s.lastTrickWinnerSeat()), duplicateCard).isPresent() must remain false", TrickCollectionRequest.from(CollectionFixtures.observation(s, s.phase(), List.of(), duplicateCard,
                duplicateCard, s.lastTrickWinnerSeat()), duplicateCard).isPresent());
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> new TrickCollectionRequest(0, List.of(0, 0, 2)));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> new TrickCollectionRequest(3, List.of(0, 1, 2)));
    }

    @Test public void capturedSeatOrderIsImmutable() {
        var seats = new ArrayList<>(List.of(2, 0, 1));
        var request = new TrickCollectionRequest(1, seats);
        seats.clear();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "request", request, "seats", seats);
        assertEquals("request.seats() must equal List.of(2, 0, 1)", List.of(2, 0, 1), request.seats());
        assertThrows("The operation rejects its input with UnsupportedOperationException.class", UnsupportedOperationException.class, () -> request.seats().clear());
    }
}
