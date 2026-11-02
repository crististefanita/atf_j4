package ro.whistlab.core;

/** Original Romanian five-hand rule. A signed counter records up to four eligible results. */
public final class RomanianScoring {
    private RomanianScoring() { }

    public record Result(int basePoints, int streakPoints, int nextStreak) {
        public int totalPoints() { return basePoints + streakPoints; }
    }

    public static Result score(int won, int bid, int handSize, int previousStreak) {
        return score(won,bid,handSize,previousStreak,5);
    }

    public static Result score(int won, int bid, int handSize, int previousStreak, int positiveBonus) {
        if (positiveBonus != 5 && positiveBonus != 10) {
            throw new IllegalArgumentException("Positive series bonus must be 5 or 10");
        }
        if (handSize < 1 || handSize > 8 || won < 0 || won > handSize || bid < 0 || bid > handSize) {
            throw new IllegalArgumentException("Romanian result must fit a one-to-eight-card hand");
        }
        if (previousStreak < -4 || previousStreak > 4) {
            throw new IllegalArgumentException("A completed five-result streak must already have been paid and reset");
        }
        boolean exact = won == bid;
        int next = exact ? Math.max(0, previousStreak) : Math.min(0, previousStreak);
        // One-card outcomes clear an opposite streak, but never advance a matching one.
        if (handSize != 1) next += exact ? 1 : -1;
        int bonus = next == 5 ? positiveBonus : next == -5 ? -5 : 0;
        return new Result(GameRules.scoreDelta(won, bid, GameMode.ROMANIAN_WHIST), bonus,
                bonus == 0 ? next : 0);
    }
}
