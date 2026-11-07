package ro.whistlab.core.ai.zero;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ro.whistlab.core.ai.engine.MonteCarloAi.Seat;
import ro.whistlab.core.ai.engine.MonteCarloAi.View;
import ro.whistlab.core.ai.simulation.SearchCancellation;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.rules.RomanianScoring;
import ro.whistlab.core.rules.RomanianScoring.Result;

/** Public score bounds and an abstract continuation model for reaching exactly zero. */
final class ZeroMatchObjective {
    private final View view;
    private final double[][] suffix;
    private final int radius;
    private final int finalHandSize;

    private ZeroMatchObjective(View view, List<Integer> future, int radius) {
        this.view = view;
        this.radius = radius;
        finalHandSize = future.size() == 1 ? future.get(0) : 0;
        suffix = buildSuffix(future);
    }

    public static Optional<ZeroMatchObjective> forTurn(View view, int handIndex, List<Integer> legalBids) {
        List<Integer> schedule = GameRules.handSizes(view.mode(), view.seats().size());
        List<Integer> future = schedule.subList(handIndex + 1, schedule.size());
        int unscored = view.handSize() - view.seats().stream().mapToInt(Seat::won).sum();
        var tail = new ScheduleTail(future.size(), future.stream().mapToInt(Integer::intValue).sum(),
                (int) future.stream().filter(size -> size > 1).count(),
                view.streakBonuses() ? view.streakBonusPoints() : 0,
                view.streakBonuses() ? view.streakPenaltyPoints() : 0);
        var own = view.seats().get(view.seat());
        int ceiling = own.total() + bound(view, own, unscored, legalBids, tail, true);
        if (ceiling < 0 || own.total() + bound(view, own, unscored, legalBids, tail, false) > 0) {
            return Optional.empty();
        }
        int ahead = 0;
        for (int index = 0; index < view.seats().size() && ahead < 2; index++) {
            if (index == view.seat()) continue;
            var rival = view.seats().get(index);
            // For an unpublished rival bid, an endpoint always permits the largest miss.
            if (rival.total() + bound(view, rival, unscored, List.of(0, view.handSize()), tail, false) > ceiling)
                ahead++;
        }
        if (ahead < 2) return Optional.empty();

        var objective = new ZeroMatchObjective(view, future, Math.max(tail.maximum(4), -tail.minimum(-4)));
        var bids = own.bid() == null ? legalBids : List.of(own.bid());
        return bids.stream().anyMatch(bid -> objective.weight(bid, objective.desiredTricks(bid)) > 0)
                ? Optional.of(objective) : Optional.empty();
    }

    private static int bound(View view, Seat seat, int unscored, List<Integer> legalBids,
            ScheduleTail tail, boolean upper) {
        List<Integer> bids = seat.bid() == null ? legalBids : List.of(seat.bid());
        int bound = upper ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (int bid : bids) {
            for (int won = seat.won(); won <= seat.won() + unscored; won++) {
                Result result = score(view, view.handSize(), seat.streak(), won, bid);
                int delta = result.totalPoints() + (upper ? tail.maximum(result.nextStreak())
                        : tail.minimum(result.nextStreak()));
                bound = upper ? Math.max(bound, delta) : Math.min(bound, delta);
            }
        }
        return bound;
    }

    private record ScheduleTail(int hands, int tricks, int eligible, int reward, int penalty) {
        int maximum(int streak) {
            return 5 * hands + tricks + (eligible + Math.max(0, streak)) / 5 * reward;
        }

        int minimum(int streak) {
            return -tricks - (eligible + Math.max(0, -streak)) / 5 * penalty;
        }
    }

    /** Distinct scoring outcomes are equally weighted; this is a heuristic for future hands. */
    private double[][] buildSuffix(List<Integer> future) {
        int states = view.streakBonuses() ? 9 : 1;
        double[][] next = new double[states][2 * radius + 1];
        for (double[] row : next) row[radius] = 1;
        for (int index = future.size() - 1; index >= 0; index--) {
            SearchCancellation.check();
            int size = future.get(index);
            double[][] current = new double[states][2 * radius + 1];
            for (int state = 0; state < states; state++) {
                int streak = states == 1 ? 0 : state - 4;
                var results = new ArrayList<Result>(2 * size + 1);
                for (int tricks = 0; tricks <= size; tricks++) {
                    results.add(score(view, size, streak, tricks, tricks));
                    if (tricks > 0) results.add(score(view, size, streak, 0, tricks));
                }
                for (Result result : results) {
                    double[] continuation = next[streakIndex(result.nextStreak())];
                    int delta = result.totalPoints();
                    for (int slot = Math.max(0, -delta); slot < Math.min(continuation.length, continuation.length - delta); slot++) {
                        current[state][slot + delta] += continuation[slot] / results.size();
                    }
                }
            }
            next = current;
        }
        return next;
    }

    public int desiredTricks(int bid) {
        int lower = view.seats().get(view.seat()).won();
        int upper = lower + view.handSize() - view.seats().stream().mapToInt(Seat::won).sum();
        int best = lower;
        double value = -1;
        for (int won = lower; won <= upper; won++) {
            double candidate = weight(bid, won);
            if (candidate > value || candidate == value && Math.abs(won - bid) < Math.abs(best - bid)) {
                best = won;
                value = candidate;
            }
        }
        return best;
    }

    List<Integer> continuationTargets(int bid) {
        if (!isFinalHand()) return List.of(desiredTricks(bid));
        int won = view.seats().get(view.seat()).won();
        int remaining = view.handSize() - view.seats().stream().mapToInt(Seat::won).sum();
        var targets = new ArrayList<Integer>(2);
        for (int count = won; count <= won + remaining; count++) {
            if (weight(bid, count) > 0) targets.add(count);
        }
        return targets;
    }

    boolean isFinalHand() {
        return radius == 0;
    }

    public double weight(int bid, int won) {
        var own = view.seats().get(view.seat());
        var result = score(view, view.handSize(), own.streak(), won, bid);
        int slot = radius - own.total() - result.totalPoints();
        return slot < 0 || slot >= suffix[0].length ? 0 : suffix[streakIndex(result.nextStreak())][slot];
    }

    /** Count final-hand bid/result pairs that settle the remainder, after applying both streak transitions. */
    int finishingOptions(int bid, int won) {
        return finishingOptions(bid, won, false);
    }

    int finishingPreference(int bid, int won) {
        boolean closesNextAuction = finalHandSize > 1 && (view.dealer() + 1) % view.seats().size() == view.seat();
        return finishingOptions(bid, won, closesNextAuction);
    }

    private int finishingOptions(int bid, int won, boolean countLosingNeighbors) {
        if (finalHandSize == 0) return 0;
        var own = view.seats().get(view.seat());
        var current = score(view, view.handSize(), own.streak(), won, bid);
        int required = -own.total() - current.totalPoints();
        int oneTrickMiss = score(view, finalHandSize, current.nextStreak(), 0, 1).totalPoints();
        int gap = oneTrickMiss + 1 - required;
        if (gap >= 1 && gap <= finalHandSize) {
            int pairs = 2 * (finalHandSize + 1 - gap);
            if (!countLosingNeighbors) return pairs;
            // Count adjacent trick totals that still miss, scaled by two. At either end one neighbor is absent.
            // When the gap is one, the neighbor toward the bid scores as an exact contract instead.
            return (gap == 1 ? pairs / 2 : pairs) - 1;
        }
        int exactZero = score(view, finalHandSize, current.nextStreak(), 0, 0).totalPoints();
        int exactTricks = required - exactZero;
        return exactTricks >= 0 && exactTricks <= finalHandSize ? 1 : 0;
    }

    private int streakIndex(int streak) {
        return view.streakBonuses() ? streak + 4 : 0;
    }

    private static Result score(View view, int size, int streak, int won, int bid) {
        return view.streakBonuses()
                ? RomanianScoring.score(won, bid, size, streak, view.streakBonusPoints(), view.streakPenaltyPoints())
                : new Result(GameRules.scoreDelta(won, bid, view.mode()), 0, 0);
    }
}
