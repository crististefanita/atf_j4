package ro.whistlab.app.achievements;

import java.util.List;
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
        boolean climbOneRank, boolean lastToTopTwo, ContractPatterns contracts) {

    WhistMatchFacts(int playerCount, GameMode mode, int exactContracts, int exactOneCard,
            int exactZeroLarge, int exactMatchRate, int exactSmallHandRate, int exactMiddleHandRate,
            int exactLargeHandRate, int longestExactStreak, boolean won, boolean topTwo,
            boolean streaksEnabled, int positiveSeriesAwards, boolean zeroGameAward) {
        this(playerCount, mode, exactContracts, exactOneCard, exactZeroLarge, exactMatchRate,
                exactSmallHandRate, exactMiddleHandRate, exactLargeHandRate, longestExactStreak,
                won, topTwo, streaksEnabled, positiveSeriesAwards, zeroGameAward,
                0, false, false, false, false, false, false, false, false, ContractPatterns.from(List.of()));
    }

    static Optional<WhistMatchFacts> from(GameSession game) {
        return from(game, WhistHandFacts.from(game));
    }

    static Optional<WhistMatchFacts> from(GameSession game, List<WhistHandFacts> hands) {
        if (game == null) return Optional.empty();
        var state = game.snapshot();
        if (state.phase() != GamePhase.SCORE || !state.finalHand() || state.players().isEmpty()
                || !state.players().get(0).human()) return Optional.empty();

        var schedule = GameRules.handSizes(game.config().mode(), game.config().playerCount());
        ScoreHistory history = game.scoreHistory();
        if (hands.size() != schedule.size()) return Optional.empty();

        int exact = 0, oneCard = 0, zeroLarge = 0, bestStreak = 0, positiveAwards = 0;
        int exactSmall = 0, smallHands = 0, exactMiddle = 0, middleHands = 0, exactLarge = 0, largeHands = 0;
        Set<Integer> exactSizes = new HashSet<>();
        for (var hand : hands) {
            int size = hand.size();
            boolean exactHand = hand.exact();
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
                    if (hand.exactZeroLarge()) zeroLarge++;
                }
            }
            if (exactHand) {
                exact++;
                exactSizes.add(size);
                bestStreak = Math.max(bestStreak, hand.exactStreak());
            }
            if (hand.positiveSeriesAward()) positiveAwards++;
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
                oneCard, zeroLarge, percentage(exact, schedule.size()), percentage(exactSmall, smallHands),
                percentage(exactMiddle, middleHands), percentage(exactLarge, largeHands), bestStreak, won, topTwo,
                game.config().streakBonuses() && game.config().mode().isRomanian(), positiveAwards,
                state.earnedZeroGame(0), exactSizes.size(), exactFinalHand, exactFinalThree, exactFinalFive,
                finalRank <= midpointRank, game.config().playerCount() == 4 && finalRank <= 3,
                game.config().playerCount() == 4 && finalRank <= 2, midpointRank - finalRank >= 1,
                midpointRank == game.config().playerCount() && finalRank <= 2, ContractPatterns.from(hands)));
    }

    private static boolean exactInRange(ScoreHistory history, int firstHand, int lastHand) {
        for (int hand = firstHand; hand <= lastHand; hand++)
            if (history.outcome(hand, 0) != ScoreHistory.Outcome.EXACT) return false;
        return true;
    }

    static int percentage(int value, int total) {
        return total == 0 ? 0 : Math.toIntExact(100L * value / total);
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
            case EXACT_ZERO_BANDS -> contracts.zeroBands();
            case EXACT_POSITIVE_RATE -> contracts.positiveRate();
            case RECOVERY_AFTER_MISS -> contracts.recovery().starts();
            case RECOVERY_AFTER_TWO_MISSES -> contracts.recovery().afterTwoMisses();
            case RECOVERY_TWO_EXACT -> contracts.recovery().twoExact();
            case RECOVERY_BOTH_MISS_TYPES -> contracts.recovery().balancedRecoveries();
            case RECOVERY_THREE_EXACT -> contracts.recovery().threeExact();
            case EXACT_ZERO_MULTICARD, EXACT_ZERO_SHORT, EXACT_ZERO_MIDDLE, EXACT_ZERO_EIGHT,
                    EXACT_ONE_TRICK, EXACT_TWO_PLUS, EXACT_THREE_PLUS, EXACT_POSITIVE_LARGE -> 0;
        };
    }

    record ContractPatterns(int zeroBands, int positiveOpportunities, int positiveRate, Recovery recovery) {
        static ContractPatterns from(List<WhistHandFacts> hands) {
            Set<Integer> zeroBands = new HashSet<>();
            int opportunities = 0;
            int exact = 0;
            for (var hand : hands) {
                if (!hand.inLearningRange()) continue;
                if (hand.bid() > 0) {
                    opportunities++;
                    if (hand.exact()) exact++;
                } else if (hand.exact()) {
                    zeroBands.add(Math.min(2, (hand.size() - 2) / 2));
                }
            }
            return new ContractPatterns(zeroBands.size(), opportunities,
                    opportunities < 8 ? 0 : percentage(exact, opportunities), Recovery.from(hands));
        }
    }

    record Recovery(int starts, int afterTwoMisses, int twoExact, int balancedRecoveries, int threeExact) {
        static Recovery from(List<WhistHandFacts> hands) {
            int misses = 0, run = 0, starts = 0, afterTwoMisses = 0, twoExact = 0, threeExact = 0;
            int lastMiss = 0;
            int recoveredOvershoots = 0, recoveredShortfalls = 0;
            for (var hand : hands) {
                if (!hand.exact()) {
                    misses++;
                    lastMiss = Integer.signum(hand.tricks() - hand.bid());
                    run = 0;
                    continue;
                }
                if (misses > 0) {
                    starts++;
                    if (misses >= 2) afterTwoMisses++;
                    if (lastMiss > 0) recoveredOvershoots++;
                    else recoveredShortfalls++;
                    misses = 0;
                    run = 1;
                } else if (run > 0) {
                    run++;
                    if (run == 2) twoExact++;
                    if (run == 3) threeExact++;
                }
            }
            return new Recovery(starts, afterTwoMisses, twoExact, Math.min(recoveredOvershoots, recoveredShortfalls), threeExact);
        }
    }
}
