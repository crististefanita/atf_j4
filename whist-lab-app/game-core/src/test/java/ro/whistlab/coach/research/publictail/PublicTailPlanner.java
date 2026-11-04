// Research-only R12 proposal: not part of the accepted production policy.
package ro.whistlab.coach.research.publictail;

import ro.whistlab.coach.IndependentPlayer;

import java.util.*;

/** A late continuation can observe played cards, never the outer sample's private hands. */
final class PublicTailPlanner {
    private PublicTailPlanner() { }

    static int choose(IndependentPlayer.Position position, List<Integer> hand,
                      List<IndependentPlayer.Play> trick, int actor, int needed,
                      boolean[] seen, int[] remaining) {
        int trump = position.trump();
        var legal = IndependentPlayer.legal(hand, trick.isEmpty() ? -1 : trick.get(0).card() / 13, trump);
        Comparator<Integer> cost = Comparator.comparingInt(c -> c % 13 + (c / 13 == trump ? 13 : 0));
        if (trick.isEmpty()) {
            if (needed <= 0) return legal.stream().min(Comparator
                    .comparingDouble((Integer c) -> unopposedLeadScore(c, trump, seen, remaining, actor))
                    .thenComparing(cost)).orElseThrow();
            var certain = legal.stream().filter(c -> unopposedLeadScore(c, trump, seen, remaining, actor) >= 1 - 1e-12).toList();
            return (certain.isEmpty() ? legal.stream().max(cost) : certain.stream().min(cost)).orElseThrow();
        }
        var preferred = new ArrayList<Integer>();
        for (int c : legal) {
            var table = new ArrayList<>(trick); table.add(new IndependentPlayer.Play(actor, c));
            if ((IndependentPlayer.winner(table, trump) == actor) == (needed > 0)) preferred.add(c);
        }
        var choices = preferred.isEmpty() ? legal : preferred;
        return (needed > 0 ? choices.stream().min(cost) : choices.stream().max(cost)).orElseThrow();
    }

    /** Product of uniform marginal no-threat rates: a ranking score, not a calibrated win probability.
     * A side suit is beaten by a higher follower OR a void with a mandatory trump.
     * The categories are disjoint; actual opponent holdings never enter this calculation. */
    static double unopposedLeadScore(int own, int trump, boolean[] seen, int[] remaining, int actor) {
        int pool = 0, led = 0, higher = 0, trumps = 0;
        for (int suit = 0; suit < 4; suit++) for (int rank = 13 - 2 * remaining.length; rank < 13; rank++) {
            int c = suit * 13 + rank;
            if (seen[c]) continue;
            pool++;
            if (suit == own / 13) { led++; if (rank > own % 13) higher++; }
            if (suit == trump) trumps++;
        }
        double result = 1;
        for (int seat = 0; seat < remaining.length; seat++) if (seat != actor && remaining[seat] > 0) {
            int count = remaining[seat]; double denominator = combinations(pool, count);
            if (denominator == 0) return 0;
            double threat = 1 - combinations(pool - higher, count) / denominator;
            if (own / 13 != trump && trump < 4)
                threat += (combinations(pool - led, count) - combinations(pool - led - trumps, count)) / denominator;
            result *= 1 - Math.max(0, Math.min(1, threat));
        }
        return result;
    }

    private static double combinations(int available, int count) {
        if (available < count || available < 0) return 0;
        double result = 1;
        for (int i = 1; i <= count; i++) result = result * (available - i + 1) / i;
        return result;
    }
}
