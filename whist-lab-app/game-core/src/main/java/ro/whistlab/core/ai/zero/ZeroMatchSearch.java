package ro.whistlab.core.ai.zero;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.IntFunction;
import ro.whistlab.core.ai.engine.MonteCarloAi;
import ro.whistlab.core.ai.engine.MonteCarloAi.Seat;
import ro.whistlab.core.ai.engine.MonteCarloAi.View;
import ro.whistlab.core.ai.engine.OneCardBidBelief;
import ro.whistlab.core.ai.simulation.PossibleDeals;
import ro.whistlab.core.ai.simulation.RolloutPolicy;
import ro.whistlab.core.ai.simulation.SearchCancellation;
import ro.whistlab.core.ai.simulation.SearchWork;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.rules.GameRules;

/** Ranks legal actions on paired public-information worlds when the match objective changes. */
public final class ZeroMatchSearch {
    private static final int SAMPLES = 32;

    private ZeroMatchSearch() { }

    public static Optional<Integer> bid(View view, int handIndex) {
        int prior = view.seats().stream().filter(seat -> seat.bid() != null).mapToInt(Seat::bid).sum();
        boolean last = view.seats().stream().filter(seat -> seat.bid() == null).count() == 1;
        List<Integer> legal = GameRules.allowedBids(view.handSize(), prior, last);
        if (legal.size() == 1) return Optional.of(legal.get(0));
        return ZeroMatchObjective.forTurn(view, handIndex, legal).flatMap(objective -> {
            SearchWork.event(SearchWork.Event.ZERO_ELIGIBLE);
            var actions = legal.stream().flatMap(bid -> objective.continuationTargets(bid).stream()
                            .map(target -> new Action<>(bid, null, bid, target)))
                    .collect(java.util.stream.Collectors.toList());
            if (view.handSize() == 1) {
                double win = OneCardBidBelief.probability(view);
                double[] outcomes = {1 - win, win};
                return select(view, objective, actions, ignored -> outcomes);
            }
            return evaluate(view, objective, actions, true);
        });
    }

    public static Optional<Card> card(View view, int handIndex) {
        return card(view, handIndex, false);
    }

    public static Optional<Card> card(View view, int handIndex, boolean jointPodium) {
        var led = view.trick().isEmpty() ? null : view.trick().get(0).card().suit();
        List<Card> legal = GameRules.legalCards(view.hand(), led, view.trump(), view.mode());
        if (legal.size() == 1) return Optional.of(legal.get(0));
        return ZeroMatchObjective.forTurn(view, handIndex, List.of(), jointPodium).flatMap(objective -> {
            SearchWork.event(SearchWork.Event.ZERO_ELIGIBLE);
            int bid = view.seats().get(view.seat()).bid();
            var targets = objective.continuationTargets(bid);
            var actions = legal.stream().flatMap(card -> targets.stream()
                            .map(target -> new Action<>(card, card, bid, target)))
                    .collect(java.util.stream.Collectors.toList());
            return evaluate(view, objective, actions, false);
        });
    }

    private static <T> Optional<T> evaluate(View view, ZeroMatchObjective objective, List<Action<T>> actions,
            boolean bidding) {
        List<List<List<Card>>> enumerated = MonteCarloAi.unknownCards(view).size() <= 7
                ? MonteCarloAi.enumerateSmallDeals(view) : List.of();
        int worlds = enumerated.isEmpty() ? SAMPLES : enumerated.size();
        double[][] outcomes = new double[actions.size()][view.handSize() + 1];
        int[] publicBids = view.seats().stream().mapToInt(seat -> seat.bid() == null ? 0 : seat.bid()).toArray();
        for (int sample = 0; sample < worlds; sample++) {
            SearchCancellation.check();
            long seed = MonteCarloAi.seed(view, sample);
            List<List<Card>> hands = enumerated.isEmpty()
                    ? PossibleDeals.sample(view, new Random(seed)) : enumerated.get(sample);
            if (hands == null) continue;
            SearchWork.event(SearchWork.Event.WORLD);
            for (int i = 0; i < actions.size(); i++) {
                var action = actions.get(i);
                int[] targets = bidding ? MonteCarloAi.biddingTargets(view, hands, action.bid())
                        : publicBids.clone();
                targets[view.seat()] = action.desired();
                int won = RolloutPolicy.rollout(view, PossibleDeals.copy(hands), action.card(), targets,
                        new Random(seed ^ 0x0F1A120L))[view.seat()];
                outcomes[i][won]++;
            }
        }
        return select(view, objective, actions, index -> outcomes[index]);
    }

    private static <T> Optional<T> select(View view, ZeroMatchObjective objective, List<Action<T>> actions,
            IntFunction<double[]> counts) {
        T best = null;
        double bestWeight = 0;
        double bestOptions = 0;
        double bestDistance = Double.POSITIVE_INFINITY;
        double bestPoints = Double.NEGATIVE_INFINITY;
        var own = view.seats().get(view.seat());
        // On a follow, once a zero route already holds, keep the conventional fallback on equal support.
        boolean compareDistance = objective.isFinalHand() && (view.trick().isEmpty()
                || actions.stream().noneMatch(action -> action.desired() == own.won()));
        for (int i = 0; i < actions.size(); i++) {
            var action = actions.get(i);
            double[] distribution = counts.apply(i);
            double weight = 0;
            double options = 0;
            double distance = 0;
            double points = 0;
            for (int won = 0; won < distribution.length; won++) {
                if (distribution[won] == 0) continue;
                int delta = MonteCarloAi.points(view, view.seat(), won, action.bid());
                weight += distribution[won] * objective.weight(action.bid(), won);
                options += distribution[won] * objective.finishingPreference(action.bid(), won);
                if (compareDistance) distance += distribution[won] * Math.abs(own.total() + delta);
                points += distribution[won] * delta;
            }
            boolean betterFallback = distance < bestDistance || distance == bestDistance && points > bestPoints;
            if (weight > bestWeight || weight > 0 && weight == bestWeight
                    && (options > bestOptions || options == bestOptions && betterFallback)) {
                best = action.value();
                bestWeight = weight;
                bestOptions = options;
                bestDistance = distance;
                bestPoints = points;
            }
        }
        if (best != null) SearchWork.event(SearchWork.Event.ZERO_SELECTED);
        return Optional.ofNullable(best);
    }

    private record Action<T>(T value, Card card, int bid, int desired) { }
}
