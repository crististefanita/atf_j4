package ro.whistlab.app.achievements;

import java.util.ArrayList;
import java.util.List;
import ro.whistlab.app.achievements.WhistAchievementCatalog.Metric;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.session.ScoreHistory;

/** Immutable, ledger-derived facts for every completed hand observed in a saved session. */
record WhistHandFacts(int number, int size, boolean exact, boolean exactZeroLarge,
        boolean positiveSeriesAward, int exactStreak) {

    static List<WhistHandFacts> from(GameSession game) {
        if (game == null || game.snapshot().players().isEmpty()
                || !game.snapshot().players().get(0).human()) return List.of();
        ScoreHistory history = game.scoreHistory();
        var schedule = GameRules.handSizes(game.config().mode(), game.config().playerCount());
        int completed = history.completedHands();
        if (history.priorHands() != 0 || completed == 0 || completed > schedule.size()
                || history.recordedBids().size() != completed || history.recordedTricks().size() != completed)
            return List.of();

        List<WhistHandFacts> facts = new ArrayList<>(completed);
        int streak = 0;
        for (int number = 1; number <= completed; number++) {
            Integer bid = history.bid(number, 0);
            Integer tricks = history.tricks(number, 0);
            if (bid == null || tricks == null) return List.of();
            boolean exact = bid.equals(tricks);
            streak = exact ? streak + 1 : 0;
            int size = schedule.get(number - 1);
            int base = GameRules.scoreDelta(tricks, bid, game.config().mode());
            int seriesDelta = history.points(number, 0) - base;
            facts.add(new WhistHandFacts(number, size, exact,
                    exact && bid == 0 && size >= 6 && size <= 8,
                    seriesDelta > 0 && game.config().mode().isRomanian(), streak));
        }
        return List.copyOf(facts);
    }

    String eventId(String matchId) { return matchId + "#hand:" + number; }

    int measure(Metric metric) {
        return switch (metric) {
            case EXACT_CONTRACTS -> exact ? 1 : 0;
            case EXACT_ONE_CARD -> exact && size == 1 ? 1 : 0;
            case EXACT_ZERO_LARGE -> exactZeroLarge ? 1 : 0;
            case POSITIVE_SERIES_AWARDS -> positiveSeriesAward ? 1 : 0;
            case EXACT_STREAK -> exactStreak;
            default -> 0;
        };
    }
}
