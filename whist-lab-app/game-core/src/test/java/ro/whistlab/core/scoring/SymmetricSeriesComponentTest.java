package ro.whistlab.core.scoring;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.Test;
import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.RomanianScoring;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;

public class SymmetricSeriesComponentTest {
    @Test public void everyLegalOutcomePaysTheSelectedMagnitudeInBothDirections() {
        for (int magnitude : List.of(5, 10)) for (int size = 1; size <= 8; size++)
            for (int won = 0; won <= size; won++) for (int bid = 0; bid <= size; bid++)
                for (int prior = -4; prior <= 4; prior++) {
                    int wins = Math.max(prior, 0), misses = Math.max(-prior, 0);
                    if (won == bid) { misses = 0; if (size > 1) wins++; }
                    else { wins = 0; if (size > 1) misses++; }
                    int award = wins == 5 ? magnitude : misses == 5 ? -magnitude : 0;
                    int next = award == 0 ? wins - misses : 0;
                    var actual = RomanianScoring.score(won, bid, size, prior, magnitude, magnitude);
                    assertEquals(won == bid ? 5 + won : -Math.abs(won - bid), actual.basePoints());
                    assertEquals(award, actual.streakPoints());
                    assertEquals(next, actual.nextStreak());
                }
    }

    @Test public void newConfigurationIsSymmetricWhileAnExplicitLegacyProfileIsRetained() {
        for (int points : List.of(5, 10)) {
            var config = GameConfig.newMatch(4, GameMode.ROMANIAN_WHIST, AiDifficulty.HARD, 1, true, points);
            assertEquals(points, config.streakPenaltyPoints());
            var state = GameSession.newGame(config).snapshot();
            assertEquals(points, state.streakPenaltyPoints());
            assertEquals(points, MonteCarloAi.observe(state, 0).streakPenaltyPoints());
        }
        var legacy = new GameConfig(4, GameMode.ROMANIAN_WHIST, AiDifficulty.NORMAL, 1, true, 10, 5);
        assertEquals(legacy, new GameConfig(4, GameMode.ROMANIAN_WHIST, AiDifficulty.NORMAL, 1, true, 10));
        assertEquals(5, GameSession.newGame(legacy).snapshot().streakPenaltyPoints());
        assertEquals(-6, RomanianScoring.score(0, 1, 2, -4, 10, 5).totalPoints());
        assertEquals(-11, RomanianScoring.score(0, 1, 2, -4, 10, 10).totalPoints());
    }

    @Test public void invalidPenaltyCannotEnterAConfigurationOrScoringResult() {
        for (int bad : List.of(-10, 0, 7, 15)) {
            assertThrows(IllegalArgumentException.class, () -> RomanianScoring.score(0, 1, 2, -4, 10, bad));
            assertThrows(IllegalArgumentException.class, () -> new GameConfig(4, GameMode.ROMANIAN_WHIST, AiDifficulty.NORMAL, 0, true, 10, bad));
        }
    }

    @Test public void theLastCardAwardsSymmetricSeriesOnceAndCheckpointRestoreKeepsTheNegativePayment() {
        for (int magnitude : List.of(5, 10)) {
            var config = GameConfig.newMatch(4, GameMode.ROMANIAN_WHIST, AiDifficulty.HARD, 11, true, magnitude);
            var hands = List.of(List.of(new Card(Suit.SPADES,14), new Card(Suit.SPADES,13)),
                    List.of(new Card(Suit.HEARTS,12), new Card(Suit.HEARTS,11)),
                    List.of(new Card(Suit.HEARTS,10), new Card(Suit.HEARTS,9)),
                    List.of(new Card(Suit.HEARTS,8), new Card(Suit.HEARTS,7)));
            var deal = GameSession.fromDeal(config, hands, Trump.SPADES, 3).saveState();
            var players = deal.players().stream().map(player -> new GameSession.PlayerState(player.seat(), player.name(),
                    player.human(), player.hand(), null, 0, 0, player.seat() < 2 ? 4 : -4, 0)).toList();
            var game = GameSession.restoreComplete(new GameSession.SaveState(config, GamePhase.BIDDING, 4, 2,
                    3, 0, 0, 0, 0, Trump.SPADES, -1, players, List.of(), List.of(), List.of(), new Card(Suit.SPADES,7)));
            var view = MonteCarloAi.observe(game.snapshot(), 0);
            assertEquals(-1 - magnitude, MonteCarloAi.points(view, 3, 0, 1));
            for (int seat = 0; seat < 4; seat++) game.submitBid(seat, new int[]{2,0,0,1}[seat]);
            for (int card = 0; card < 8; card++) {
                var state = game.snapshot();
                assertEquals(0, state.players().get(3).totalScore());
                game.playCard(state.currentSeat(), state.players().get(state.currentSeat()).hand().get(0));
            }
            assertEquals(List.of(7+magnitude,5+magnitude,5,-1-magnitude), game.snapshot().players().stream().map(GameSession.PlayerSnapshot::totalScore).toList());
            assertEquals(-magnitude, game.snapshot().players().get(3).streakPoints());
            assertEquals(game.snapshot(), GameSession.restoreComplete(game.saveState()).snapshot());
            var complete = game;
            assertThrows(IllegalStateException.class, () -> complete.playCard(0, new Card(Suit.SPADES,14)));
            assertEquals(-1-magnitude, game.scoreHistory().points(5,3));
        }
    }
}
