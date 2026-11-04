package ro.whistlab.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public final class NormalContractPursuitTest {
    @Test public void normalLeadsWithStrengthWhileAContractRemains() {
        var game = position(1, 0, 0, List.of(card(Suit.SPADES, 7), card(Suit.SPADES, 14)), List.of());
        assertEquals(card(Suit.SPADES, 14), AiPolicy.chooseCard(game, 0, AiDifficulty.NORMAL));
    }

    @Test public void normalShedsTheLowestRiskWhenNoTrickIsNeeded() {
        var game = position(0, 0, 0, List.of(card(Suit.SPADES, 7), card(Suit.SPADES, 14)), List.of());
        assertEquals(card(Suit.SPADES, 7), AiPolicy.chooseCard(game, 0, AiDifficulty.NORMAL));
    }

    @Test public void normalTakesAReachableTrickAndUsesTheSmallestCertainWinnerWhenLast() {
        List<PlayedCard> table = List.of(new PlayedCard(0, card(Suit.SPADES, 11)),
                new PlayedCard(1, card(Suit.SPADES, 9)), new PlayedCard(2, card(Suit.SPADES, 10)));
        var game = position(1, 0, 3, List.of(card(Suit.SPADES, 12), card(Suit.SPADES, 14)), table);
        assertEquals(card(Suit.SPADES, 12), AiPolicy.chooseCard(game, 3, AiDifficulty.NORMAL));
    }

    @Test public void normalObservationKeepsItsPublicSeatSnapshotImmutable() {
        var game = GameSession.newGame(new GameConfig(4, GameMode.ROMANIAN_WHIST,
                AiDifficulty.NORMAL, 20261104L));

        NormalPlayPolicy.Observation observation = NormalPlayPolicy.observe(game.snapshot(), 0);

        assertEquals(4, observation.seats().size());
        assertEquals(game.snapshot().players().get(0).hand().size(), observation.seats().get(0).remaining());
        assertThrows(UnsupportedOperationException.class, () -> observation.seats().clear());
    }

    private static GameSession.Snapshot position(int bid, int won, int seat,
            List<Card> hand, List<PlayedCard> trick) {
        List<GameSession.PlayerSnapshot> players = new ArrayList<>();
        for (int current = 0; current < 4; current++) {
            List<Card> own = current == seat ? hand : List.of(card(Suit.HEARTS, 7 + current), card(Suit.CLUBS, 7 + current));
            players.add(new GameSession.PlayerSnapshot(current, "Seat " + current, current == 0, own,
                    current == seat ? bid : 0, current == seat ? won : 0, 0));
        }
        return new GameSession.Snapshot(GamePhase.PLAYING, 0, 1, 2, Trump.NO_TRUMP,
                3, 0, seat, 4, bid, players, trick, List.of(), -1, false,
                GameMode.ROMANIAN_WHIST, List.of(), null, false, 10);
    }

    private static Card card(Suit suit, int rank) { return new Card(suit, rank); }
}
