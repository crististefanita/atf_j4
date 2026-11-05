package ro.whistlab.core.ai.engine;

import java.util.List;
import java.util.Random;
import ro.whistlab.core.ai.engine.MonteCarloAi.View;
import ro.whistlab.core.ai.simulation.PossibleDeals;
import ro.whistlab.core.ai.simulation.RolloutPolicy;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.rules.RomanianScoring;

/** Evaluates hypothetical continuations; it never selects a world or reads real opposing hands. */
final class DecisionEvaluation {
    private DecisionEvaluation() { }

    static double[] evaluate(
            View view,
            List<List<Card>> hands,
            Card first,
            int[] targets,
            long seed,
            double exactWeight) {
        int[] greedy = RolloutPolicy.rollout(view, PossibleDeals.copy(hands), first, targets, new Random(seed));
        double[] result = utility(view, greedy, targets);
        if (exactWeight > 0
                && hands.stream().allMatch(h -> h.size() <= (hands.size() == 4 ? 3 : 2))) {
            double[] exact =
                    utility(view, ContractEndgame.after(view, hands, first, targets), targets);
            for (int i = 0; i < result.length; i++)
                result[i] = (1 - exactWeight) * result[i] + exactWeight * exact[i];
        }
        return result;
    }

    static double[] utility(View view, int[] won, int[] targets) {
        int rival = Integer.MIN_VALUE, misses = 0;
        for (int s = 0; s < won.length; s++)
            if (s != view.seat()) {
                rival =
                        Math.max(
                                rival,
                                view.seats().get(s).total() + points(view, s, won[s], targets[s]));
                if (won[s] != targets[s]) misses++;
            }
        return new double[] {
            points(view, view.seat(), won[view.seat()], targets[view.seat()]),
            rival,
            won[view.seat()] == targets[view.seat()] ? 1 : 0,
            misses
        };
    }

    static int points(View view, int seat, int won, int bid) {
        return view.streakBonuses()
                ? RomanianScoring.score(
                                won,
                                bid,
                                view.handSize(),
                                view.seats().get(seat).streak(),
                                view.streakBonusPoints(), view.streakPenaltyPoints())
                        .totalPoints()
                : GameRules.scoreDelta(won, bid, view.mode());
    }

    static int[] biddingTargets(View view, List<List<Card>> hands, int own) {
        int[] targets = new int[hands.size()];
        int sum = 0;
        for (int i = 1; i <= hands.size(); i++) {
            int seat = (view.dealer() + i) % hands.size();
            Integer known = view.seats().get(seat).bid();
            int target =
                    seat == view.seat()
                            ? own
                            : known != null ? known : estimate(hands.get(seat), view.trump());
            if (i == hands.size()
                    && seat != view.seat()
                    && known == null
                    && sum + target == view.handSize()) target = target > 0 ? target - 1 : 1;
            targets[seat] = target;
            sum += target;
        }
        return targets;
    }

    private static int estimate(List<Card> hand, Trump trump) {
        double value = 0;
        for (Card c : hand)
            value +=
                    (c.rank() == 14
                                    ? 1
                                    : c.rank() == 13
                                            ? .7
                                            : c.rank() == 12 ? .4 : c.rank() == 11 ? .2 : .03)
                            + (c.suit().ordinal() == trump.ordinal() ? .2 : 0);
        return Math.min(hand.size(), (int) Math.round(value));
    }
}
