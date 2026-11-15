package ro.whistlab.core.session.scoring;

import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.rules.RomanianScoring;

/** Seat score carried between hands, with the series adjustment for the current hand. */
public record PlayerScore(int total, int streak, int streakPoints) {
    public PlayerScore beginHand() {
        return new PlayerScore(total, streak, 0);
    }

    public PlayerScore settle(GameConfig config, int handSize, Integer bid, int tricksWon) {
        if (bid == null) throw new IllegalStateException("Cannot score a hand before all bids are submitted");
        if (!config.streakBonuses()) {
            return new PlayerScore(total + GameRules.scoreDelta(tricksWon, bid, config.mode()), streak, streakPoints);
        }
        RomanianScoring.Result result = RomanianScoring.score(tricksWon, bid, handSize, streak,
                config.streakBonusPoints(), config.streakPenaltyPoints());
        return new PlayerScore(total + result.totalPoints(), result.nextStreak(), result.streakPoints());
    }
}
