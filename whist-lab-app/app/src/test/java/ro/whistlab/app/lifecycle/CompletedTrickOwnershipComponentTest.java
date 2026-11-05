package ro.whistlab.app.lifecycle;

import static org.junit.Assert.*;
import java.util.ArrayList;
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

public class CompletedTrickOwnershipComponentTest {
    @Test public void collectionRetainsEveryCardAndTheEnginesWinnerAtEveryTableSize() {
        for (int count = 3; count <= 6; count++) for (int winner = 0; winner < count; winner++) {
            var game = completedTrick(count, winner, 2);
            var before = game.snapshot();
            var pause = new CompletedTrickPause();
            var display = pause.evaluate(game, 1000);
            assertTrue(display.active());
            assertEquals(1500, display.remainingMillis());
            assertEquals(count, display.cards().size());
            assertEquals(winner, before.lastTrickWinnerSeat());
            assertEquals(before.lastTrick(), display.cards());
            assertFalse(pause.evaluate(game, 2500).active());
            assertFalse(pause.evaluate(game, 2501).active());
            assertEquals(before, game.snapshot());
        }
    }

    @Test public void anEqualStateFromAnotherSessionStartsItsOwnCollectionWindow() {
        var game = completedGeneratedTrick();
        var other = GameSession.restoreComplete(game.saveState());
        assertEquals(game.snapshot(), other.snapshot());
        var pause = new CompletedTrickPause();
        assertTrue(pause.evaluate(game, 100).active());
        assertFalse(pause.evaluate(game, 1600).active());
        assertEquals(1500, pause.evaluate(other, 1700).remainingMillis());
        assertEquals(1, pause.evaluate(other, 3199).remainingMillis());
        assertFalse(pause.evaluate(other, 3200).active());
    }

    @Test public void finalCardStillHasACollectionWindowBeforeTheScorePage() {
        for (int count = 3; count <= 6; count++) {
            var game = completedTrick(count, count - 1, 1);
            assertEquals(GamePhase.SCORE, game.snapshot().phase());
            var pause = new CompletedTrickPause();
            assertTrue(pause.evaluate(game, 0).active());
            assertEquals(count, pause.evaluate(game, 1499).cards().size());
            assertFalse(pause.evaluate(game, 1500).active());
        }
    }

    private static GameSession completedGeneratedTrick() {
        var game = GameSession.newGame(new GameConfig(4, GameMode.OH_HELL, AiDifficulty.NORMAL, 31));
        while (game.snapshot().phase() == GamePhase.BIDDING) {
            var state = game.snapshot();
            game.submitBid(state.biddingSeat(), GameRules.allowedBids(state.handSize(), state.bidTotal(),
                    state.bidsSubmitted() == state.players().size() - 1).get(0));
        }
        while (game.snapshot().history().size() < 4) {
            var state = game.snapshot();
            var led = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
            var legal = GameRules.legalCards(state.players().get(state.currentSeat()).hand(), led, state.trump(), state.mode());
            game.playCard(state.currentSeat(), legal.get(0));
        }
        return game;
    }

    private static GameSession completedTrick(int players, int winner, int size) {
        var hands = new ArrayList<List<Card>>();
        for (int seat = 0; seat < players; seat++) {
            var cards = new ArrayList<Card>();
            cards.add(new Card(Suit.HEARTS, seat == winner ? 14 : 13 - seat));
            if (size == 2) cards.add(new Card(Suit.CLUBS, 13 - seat));
            hands.add(cards);
        }
        var game = GameSession.fromDeal(new GameConfig(players, GameMode.OH_HELL, AiDifficulty.NORMAL, 31),
                hands, Trump.SPADES, winner);
        while (game.snapshot().phase() == GamePhase.BIDDING) game.submitBid(game.snapshot().biddingSeat(), 0);
        for (int play = 0; play < players; play++) {
            int seat = game.snapshot().currentSeat();
            game.playCard(seat, hands.get(seat).get(0));
        }
        return game;
    }
}
