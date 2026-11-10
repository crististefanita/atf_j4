package ro.whistlab.app.achievements;

import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import ro.whistlab.app.achievements.WhistAchievementCatalog.Metric;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.session.ScoreHistory;

/** Fapte verificabile extrase din bilanțul complet al partidei, numai pentru locul uman. */
record WhistMatchFacts(int playerCount, GameMode mode, int exactContracts, int exactOneCard,
        int exactZeroLarge, int exactMatchRate, int exactSmallHandRate, int exactMiddleHandRate,
        int exactLargeHandRate, int longestExactStreak, boolean won, boolean topTwo,
        boolean streaksEnabled, int positiveSeriesAwards, boolean zeroGameAward,
        int exactHandSizeCount, boolean exactFinalHand, boolean exactFinalThree, boolean exactFinalFive,
        boolean holdOrImproveRank, boolean topThreeAtFour, boolean topTwoAtFour,
        boolean climbOneRank, boolean lastToTopTwo) {

    WhistMatchFacts(int playerCount, GameMode mode, int exactContracts, int exactOneCard,
            int exactZeroLarge, int exactMatchRate, int exactSmallHandRate, int exactMiddleHandRate,
            int exactLargeHandRate, int longestExactStreak, boolean won, boolean topTwo,
            boolean streaksEnabled, int positiveSeriesAwards, boolean zeroGameAward) {
        this(playerCount, mode, exactContracts, exactOneCard, exactZeroLarge, exactMatchRate,
                exactSmallHandRate, exactMiddleHandRate, exactLargeHandRate, longestExactStreak,
                won, topTwo, streaksEnabled, positiveSeriesAwards, zeroGameAward,
                0, false, false, false, false, false, false, false, false);
    }

    static Optional<WhistMatchFacts> from(GameSession game) {
        if (game == null) return Optional.empty();
        var state = game.snapshot();
        if (state.phase() != GamePhase.SCORE || !state.finalHand() || state.players().isEmpty()
                || !state.players().get(0).human()) return Optional.empty();

        var schedule = GameRules.handSizes(game.config().mode(), game.config().playerCount());
        ScoreHistory history = game.scoreHistory();
        if (history.priorHands() != 0 || history.completedHands() != schedule.size()
                || history.recordedBids().size() != schedule.size()
                || history.recordedTricks().size() != schedule.size()) return Optional.empty();

        int exact = 0, oneCard = 0, zeroLarge = 0, streak = 0, bestStreak = 0, positiveAwards = 0;
        int exactSmall = 0, smallHands = 0, exactMiddle = 0, middleHands = 0, exactLarge = 0, largeHands = 0;
        Set<Integer> exactSizes = new HashSet<>();
        for (int index = 0; index < schedule.size(); index++) {
            int handNumber = index + 1;
            int size = schedule.get(index);
            Integer bid = history.bid(handNumber, 0);
            Integer tricks = history.tricks(handNumber, 0);
            if (bid == null || tricks == null) return Optional.empty();
            boolean exactHand = bid.equals(tricks);
            if (size == 1) {
                if (exactHand) oneCard++;
            } else if (size >= 2 && size <= 3) {
                smallHands++;
                if (exactHand) exactSmall++;
            } else if (size >= 4 && size <= 5) {
                middleHands++;
                if (exactHand) exactMiddle++;
            } else if (size >= 6 && size <= 8) {
                largeHands++;
                if (exactHand) {
                    exactLarge++;
                    if (bid == 0) zeroLarge++;
                }
            }
            if (exactHand) {
                exact++;
                exactSizes.add(size);
                streak++;
                bestStreak = Math.max(bestStreak, streak);
            } else {
                streak = 0;
            }
            if (game.config().mode().isRomanian()) {
                int seriesDelta = history.points(handNumber, 0)
                        - GameRules.scoreDelta(tricks, bid, game.config().mode());
                if (seriesDelta > 0) positiveAwards++;
            }
        }

        int humanScore = state.matchScore(0);
        long higherScores = state.players().stream()
                .filter(player -> state.matchScore(player.seat()) > humanScore).count();
        boolean won = higherScores == 0;
        boolean topTwo = higherScores <= 1;
        int finalRank = Math.toIntExact(higherScores) + 1;
        int midpointHand = Math.max(1, schedule.size() / 2);
        int midpointScore = history.total(midpointHand, 0);
        long midHigherScores = state.players().stream()
                .filter(player -> history.total(midpointHand, player.seat()) > midpointScore).count();
        int midpointRank = Math.toIntExact(midHigherScores) + 1;
        boolean exactFinalHand = history.outcome(schedule.size(), 0) == ScoreHistory.Outcome.EXACT;
        boolean exactFinalThree = schedule.size() >= 3
                && exactInRange(history, schedule.size() - 2, schedule.size());
        boolean exactFinalFive = schedule.size() >= 5
                && exactInRange(history, schedule.size() - 4, schedule.size());
        return Optional.of(new WhistMatchFacts(game.config().playerCount(), game.config().mode(), exact,
                oneCard, zeroLarge, percent(exact, schedule.size()), percent(exactSmall, smallHands),
                percent(exactMiddle, middleHands), percent(exactLarge, largeHands), bestStreak, won, topTwo,
                game.config().streakBonuses() && game.config().mode().isRomanian(), positiveAwards,
                state.earnedZeroGame(0), exactSizes.size(), exactFinalHand, exactFinalThree, exactFinalFive,
                finalRank <= midpointRank, game.config().playerCount() == 4 && finalRank <= 3,
                game.config().playerCount() == 4 && finalRank <= 2, midpointRank - finalRank >= 1,
                midpointRank == game.config().playerCount() && finalRank <= 2));
    }

    private static boolean exactInRange(ScoreHistory history, int firstHand, int lastHand) {
        for (int hand = firstHand; hand <= lastHand; hand++)
            if (history.outcome(hand, 0) != ScoreHistory.Outcome.EXACT) return false;
        return true;
    }

    private static int percent(int value, int total) {
        return total == 0 ? 0 : Math.round(100f * value / total);
    }

    int measure(Metric metric) {
        return switch (metric) {
            case COMPLETED_MATCHES -> 1;
            case EXACT_CONTRACTS -> exactContracts;
            case MATCH_WINS -> won ? 1 : 0;
            case EXACT_ONE_CARD -> exactOneCard;
            case EXACT_ZERO_LARGE -> exactZeroLarge;
            case EXACT_SMALL_RATE -> exactSmallHandRate;
            case EXACT_MIDDLE_RATE -> exactMiddleHandRate;
            case EXACT_LARGE_RATE -> exactLargeHandRate;
            case EXACT_STREAK -> longestExactStreak;
            case EXACT_MATCH_RATE -> exactMatchRate;
            case EXACT_HAND_SIZE_COUNT -> mode.isRomanian() ? exactHandSizeCount : 0;
            case EXACT_FINAL_HAND -> exactFinalHand ? 1 : 0;
            case EXACT_FINAL_THREE -> exactFinalThree ? 1 : 0;
            case EXACT_FINAL_FIVE -> exactFinalFive ? 1 : 0;
            case SERIES_ENABLED_MATCHES -> streaksEnabled ? 1 : 0;
            case POSITIVE_SERIES_AWARDS -> positiveSeriesAwards;
            case POSITIVE_SERIES_MATCHES -> positiveSeriesAwards > 0 ? 1 : 0;
            case WIN_WITH_SERIES_AWARD -> won && positiveSeriesAwards > 0 ? 1 : 0;
            case SHORT_ROMANIAN_MATCHES -> mode == GameMode.ROMANIAN_WHIST ? 1 : 0;
            case LONG_ROMANIAN_MATCHES -> mode == GameMode.LONG_ROMANIAN ? 1 : 0;
            case SHORT_ROMANIAN_WINS -> mode == GameMode.ROMANIAN_WHIST && won ? 1 : 0;
            case LONG_ROMANIAN_WINS -> mode == GameMode.LONG_ROMANIAN && won ? 1 : 0;
            case ZERO_GAME_MATCHES -> zeroGameAward ? 1 : 0;
            case PLAYER_COUNT_THREE -> playerCount == 3 ? 1 : 0;
            case PLAYER_COUNT_SIX -> playerCount == 6 ? 1 : 0;
            case TOP_TWO_AT_FIVE -> playerCount == 5 && topTwo ? 1 : 0;
            case TOP_TWO_AT_SIX -> playerCount == 6 && topTwo ? 1 : 0;
            case WINS_AT_SIX -> playerCount == 6 && won ? 1 : 0;
            case HOLD_OR_IMPROVE_RANK -> holdOrImproveRank ? 1 : 0;
            case TOP_THREE_AT_FOUR -> topThreeAtFour ? 1 : 0;
            case TOP_TWO_AT_FOUR -> topTwoAtFour ? 1 : 0;
            case CLIMB_ONE_RANK -> climbOneRank ? 1 : 0;
            case LAST_TO_TOP_TWO -> lastToTopTwo ? 1 : 0;
        };
    }
}
