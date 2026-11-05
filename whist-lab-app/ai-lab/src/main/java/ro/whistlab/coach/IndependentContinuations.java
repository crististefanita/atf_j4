package ro.whistlab.coach;

import static ro.whistlab.coach.IndependentPlayer.legal;
import static ro.whistlab.coach.IndependentPlayer.winner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import ro.whistlab.coach.IndependentPlayer.Play;
import ro.whistlab.coach.IndependentPlayer.Position;
import ro.whistlab.coach.IndependentPlayer.Seat;

/** Advances hypothetical worlds only; never supplies hidden cards to a public decision. */
final class IndependentContinuations {
    private IndependentContinuations() {}

    // Offline diagnostic solver; not selected by the accepted policy after development regressions.
    static int[] exactAfter(Position p, List<List<Integer>> world, int first, int[] goals) {
        var hands = IndependentDeals.copy(world);
        if (!hands.get(p.seat()).remove(Integer.valueOf(first)))
            throw new IllegalArgumentException("First card not held");
        var trick = new ArrayList<>(p.trick());
        trick.add(new Play(p.seat(), first));
        int[] won = p.seats().stream().mapToInt(Seat::won).toArray();
        int actor = (p.seat() + 1) % hands.size();
        if (trick.size() == hands.size()) {
            actor = winner(trick, p.trump());
            won[actor]++;
            trick.clear();
        }
        return solve(p, hands, trick, actor, won, goals);
    }

    private static int[] solve(
            Position p,
            List<List<Integer>> hands,
            List<Play> trick,
            int actor,
            int[] won,
            int[] goals) {
        PlannerControl.check();
        PlannerControl.event(PlannerControl.Event.ENDGAME);
        if (hands.stream().allMatch(List::isEmpty)) return won;
        var choices =
                new ArrayList<>(
                        legal(
                                hands.get(actor),
                                trick.isEmpty() ? -1 : trick.get(0).card() / 13,
                                p.trump()));
        Collections.sort(choices);
        int[] best = null;
        for (int card : choices) {
            var next = IndependentDeals.copy(hands);
            next.get(actor).remove(Integer.valueOf(card));
            var round = new ArrayList<>(trick);
            round.add(new Play(actor, card));
            int[] count = won.clone();
            int turn = (actor + 1) % hands.size();
            if (round.size() == hands.size()) {
                turn = winner(round, p.trump());
                count[turn]++;
                round.clear();
            }
            int[] value = solve(p, next, round, turn, count, goals);
            if (best == null
                    || score(p, actor, value[actor], goals[actor])
                            > score(p, actor, best[actor], goals[actor])
                    || score(p, actor, value[actor], goals[actor])
                                    == score(p, actor, best[actor], goals[actor])
                            && rival(p, value, goals, actor) < rival(p, best, goals, actor))
                best = value;
        }
        return best;
    }

    static int rival(Position p, int[] won, int[] goals, int actor) {
        int best = Integer.MIN_VALUE;
        for (int s = 0; s < won.length; s++)
            if (s != actor)
                best = Math.max(best, p.seats().get(s).total() + score(p, s, won[s], goals[s]));
        return best;
    }

    static int[] roll(
            Position p, List<List<Integer>> hands, int first, int[] goals, Random r) {
        int n = hands.size(), actor = first < 0 ? (p.dealer() + 1) % n : p.seat();
        int[] won = p.seats().stream().mapToInt(Seat::won).toArray();
        var trick = new ArrayList<>(p.trick());
        int left = hands.stream().mapToInt(List::size).sum();
        boolean initial = true;
        while (left-- > 0) {
            PlannerControl.check();
            PlannerControl.event(PlannerControl.Event.ROLLOUT);
            int c =
                    initial && first >= 0
                            ? first
                            : (actor == p.seat()
                                    ? RolloutPlanner.choose(
                                            p, hands, trick, actor, goals[actor] - won[actor], r)
                                    : greedy(
                                            hands.get(actor),
                                            trick,
                                            p.trump(),
                                            actor,
                                            goals[actor] - won[actor],
                                            r));
            initial = false;
            if (!hands.get(actor).remove(Integer.valueOf(c)))
                throw new IllegalStateException("Independent player invented a card");
            trick.add(new Play(actor, c));
            if (trick.size() == n) {
                actor = winner(trick, p.trump());
                won[actor]++;
                trick.clear();
            } else actor = (actor + 1) % n;
        }
        return won;
    }

    private static int greedy(
            List<Integer> hand, List<Play> trick, int trump, int seat, int needed, Random r) {
        var legal = legal(hand, trick.isEmpty() ? -1 : trick.get(0).card() / 13, trump);
        Comparator<Integer> power =
                Comparator.comparingInt(c -> c % 13 + (c / 13 == trump ? 13 : 0));
        if (trick.isEmpty())
            return needed > 0
                    ? legal.stream().max(power).orElseThrow()
                    : legal.stream().min(power).orElseThrow();
        var preferred = new ArrayList<Integer>();
        for (int c : legal) {
            var test = new ArrayList<>(trick);
            test.add(new Play(seat, c));
            if ((winner(test, trump) == seat) == (needed > 0)) preferred.add(c);
        }
        if (preferred.isEmpty())
            return needed > 0
                    ? legal.stream().min(power).orElseThrow()
                    : legal.stream().max(power).orElseThrow();
        // When avoiding tricks, discard dangerous high losers while another player owns the trick.
        return needed > 0
                ? preferred.stream().min(power).orElseThrow()
                : preferred.stream().max(power).orElseThrow();
    }

    static int score(Position p, int seat, int won, int bid) {
        int base = won == bid ? 5 + won : -Math.abs(won - bid);
        if (!p.streakBonuses() || p.size() == 1) return base;
        int previous = p.seats().get(seat).streak();
        if (won == bid && previous == 4) return base + p.streakBonusPoints();
        if (won != bid && previous == -4) return base - p.streakPenaltyPoints();
        return base;
    }

}
