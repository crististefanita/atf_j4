package ro.whistlab.core.ai.zero;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.ai.engine.MonteCarloAi.Seat;
import ro.whistlab.core.ai.engine.MonteCarloAi.View;
import ro.whistlab.core.ai.simulation.PossibleDeals;
import ro.whistlab.core.ai.simulation.SearchCancellation;
import ro.whistlab.core.ai.simulation.SearchWork;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.rules.GameRules;

/** Keeps a future zero route only when the complete public endgame protects conventional play. */
public final class ZeroMatchReserve {
    private static final int MAX_PROOF_WORLDS = 64;
    private static final int THREE_TRICK_PROOF_NODES = 16_384;
    private static final int FOUR_TRICK_ENUMERATION_NODES = 16_384;
    private static final int FOUR_TRICK_PROOF_NODES = 32_768;

    private ZeroMatchReserve() { }

    public static Card choose(View view, int handIndex, Card conventional) {
        int remaining = view.hand().size();
        if (remaining < 2 || remaining > 4) return conventional;
        var legal = GameRules.legalCards(view.hand(), view.trick().isEmpty() ? null : view.trick().get(0).card().suit(),
                view.trump(), view.mode());
        int maxUnknown = switch (remaining) {
            case 2 -> 7;
            case 3 -> 10;
            default -> 20;
        };
        if (legal.size() < 2 || MonteCarloAi.unknownCards(view).size() > maxUnknown) return conventional;

        var schedule = GameRules.handSizes(view.mode(), view.seats().size());
        var future = schedule.subList(handIndex + 1, schedule.size());
        if (future.isEmpty()) return conventional;
        var tail = ZeroMatchObjective.ScheduleTail.of(view, future);
        var objective = new ZeroMatchObjective(view, future, Math.max(tail.maximum(4), -tail.minimum(-4)));
        var own = view.seats().get(view.seat());
        boolean[] routes = new boolean[view.handSize() + 1];
        int reachable = 0;
        for (int won = own.won(); won <= own.won() + remaining; won++) {
            routes[won] = objective.weight(own.bid(), won) > 0;
            if (routes[won]) reachable++;
        }
        if (reachable == 0 || reachable == remaining + 1) return conventional;

        var worlds = remaining == 4
                ? PossibleDeals.enumerateSmallDeals(view, remaining, maxUnknown, MAX_PROOF_WORLDS,
                        FOUR_TRICK_ENUMERATION_NODES)
                : PossibleDeals.enumerateSmallDeals(view, remaining, MAX_PROOF_WORLDS);
        var reference = new Outcome[worlds.size()];
        var budget = new Budget(switch (remaining) {
            case 3 -> THREE_TRICK_PROOF_NODES;
            case 4 -> FOUR_TRICK_PROOF_NODES;
            default -> Integer.MAX_VALUE;
        }, remaining == 4);
        try {
            for (var alternative : legal) {
                if (!alternative.equals(conventional)
                        && preservesMore(view, conventional, alternative, worlds, reference, tail, routes, budget)) {
                    SearchWork.event(SearchWork.Event.ZERO_RESERVED);
                    return alternative;
                }
            }
        } catch (ProofBudgetExceeded ignored) {
            return conventional;
        }
        return conventional;
    }

    private static boolean preservesMore(View view, Card conventional, Card alternative,
            List<List<List<Card>>> worlds, Outcome[] reference, ZeroMatchObjective.ScheduleTail tail,
            boolean[] routes, Budget budget) {
        boolean preservesMore = false;
        for (int i = 0; i < worlds.size(); i++) {
            var world = worlds.get(i);
            if (reference[i] == null) reference[i] = after(view, world, conventional, tail, routes, budget);
            var baseline = reference[i];
            if (budget.prune && baseline.podium && !baseline.invariant) return false;
            var candidate = after(view, world, alternative, tail, routes, budget);
            if (baseline.podium || candidate.podium) {
                // Identical terminal trick vectors also preserve every score and series counter.
                if (!baseline.invariant || !candidate.invariant || !Arrays.equals(baseline.won, candidate.won)) {
                    return false;
                }
            } else {
                if (baseline.someRoute && !candidate.allRoutes) return false;
                preservesMore |= !(budget.prune ? baseline.allRoutes : baseline.someRoute) && candidate.allRoutes;
            }
        }
        return preservesMore;
    }

    private static Outcome after(View view, List<List<Card>> world, Card first,
            ZeroMatchObjective.ScheduleTail tail, boolean[] routes, Budget budget) {
        var hands = PossibleDeals.copy(world);
        var won = view.seats().stream().mapToInt(Seat::won).toArray();
        var outcome = new Outcome();
        play(view, hands, view.trick(), view.seat(), first, won, tail, routes, outcome, budget);
        return outcome;
    }

    private static void play(View view, List<List<Card>> hands, List<PlayedCard> trick, int seat, Card card,
            int[] won, ZeroMatchObjective.ScheduleTail tail, boolean[] routes, Outcome outcome, Budget budget) {
        SearchCancellation.check();
        if (budget.nodes-- <= 0) throw new ProofBudgetExceeded();
        SearchWork.event(SearchWork.Event.PROOF);
        if (!hands.get(seat).remove(card)) throw new IllegalStateException("Unheld card in public zero proof");
        var table = new ArrayList<>(trick);
        table.add(new PlayedCard(seat, card));
        int next = (seat + 1) % hands.size();
        int winner = -1;
        if (table.size() == hands.size()) {
            winner = GameRules.winningSeat(table, view.trump());
            won[winner]++;
            next = winner;
            table.clear();
        }
        if (hands.stream().allMatch(List::isEmpty)) {
            if (!table.isEmpty()) throw new IllegalStateException("Incomplete trick in public zero proof");
            outcome.add(view, won, tail, routes[won[view.seat()]]);
        } else {
            var lead = table.isEmpty() ? null : table.get(0).card().suit();
            for (var reply : GameRules.legalCards(hands.get(next), lead, view.trump(), view.mode())) {
                play(view, hands, table, next, reply, won, tail, routes, outcome, budget);
                if (budget.prune && outcome.podium && !outcome.invariant) break;
            }
        }
        if (winner >= 0) won[winner]--;
        hands.get(seat).add(card);
    }

    private static final class Budget {
        private int nodes;
        private final boolean prune;
        private Budget(int nodes, boolean prune) {
            this.nodes = nodes;
            this.prune = prune;
        }
    }

    private static final class ProofBudgetExceeded extends RuntimeException { }

    private static final class Outcome {
        private int[] won;
        private boolean invariant = true;
        private boolean podium;
        private boolean someRoute;
        private boolean allRoutes = true;

        private void add(View view, int[] tricks, ZeroMatchObjective.ScheduleTail tail, boolean zeroRoute) {
            if (won == null) won = tricks.clone();
            else invariant &= Arrays.equals(won, tricks);
            someRoute |= zeroRoute;
            allRoutes &= zeroRoute;
            if (podium) return;
            var own = view.seats().get(view.seat());
            var result = ZeroMatchObjective.score(view, view.handSize(), own.streak(), tricks[view.seat()], own.bid());
            int ceiling = own.total() + result.totalPoints() + tail.maximum(result.nextStreak());
            int ahead = 0;
            for (int seat = 0; seat < tricks.length; seat++) {
                if (seat == view.seat()) continue;
                var rival = view.seats().get(seat);
                var scored = ZeroMatchObjective.score(view, view.handSize(), rival.streak(), tricks[seat], rival.bid());
                if (rival.total() + scored.totalPoints() + tail.minimum(scored.nextStreak()) > ceiling) ahead++;
            }
            podium = ahead < 2;
        }
    }
}
