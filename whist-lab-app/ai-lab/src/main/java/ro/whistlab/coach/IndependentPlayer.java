package ro.whistlab.coach;

import java.util.*;

import static ro.whistlab.coach.IndependentContinuations.roll;
import static ro.whistlab.coach.IndependentContinuations.score;

/** Test-only independent player. No engine imports and no representation of opponents' private cards. */
public final class IndependentPlayer {
    public record Seat(int remaining, Integer bid, int won, int total, int streak) {
        public Seat(int remaining, Integer bid, int won, int total) {
            this(remaining, bid, won, total, 0);
        }

        @Override
        public int hashCode() {
            int h = ((remaining * 31 + Objects.hashCode(bid)) * 31 + won) * 31 + total;
            return streak == 0 ? h : 31 * h + streak;
        }
    }

    public record Play(int seat, int card) {}

    public record Position(
            int seat,
            int dealer,
            int size,
            int trump,
            List<Integer> hand,
            List<Seat> seats,
            List<Play> trick,
            List<Play> history,
            int turnedCard,
            boolean streakBonuses,
            int streakBonusPoints, int streakPenaltyPoints) {
        /** Legacy projections retain their historical five-point penalty. */
        public Position(
            int seat,
            int dealer,
            int size,
            int trump,
            List<Integer> hand,
            List<Seat> seats,
            List<Play> trick,
            List<Play> history,
            int turnedCard,
            boolean streakBonuses,
            int streakBonusPoints) {
            this(seat, dealer, size, trump, hand, seats, trick, history, turnedCard, streakBonuses, streakBonusPoints, 5);
        }
        public Position(
                int seat,
                int dealer,
                int size,
                int trump,
                List<Integer> hand,
                List<Seat> seats,
                List<Play> trick,
                List<Play> history,
                int turnedCard) {
            this(seat, dealer, size, trump, hand, seats, trick, history, turnedCard, false, 5);
        }

        public Position {
            hand = hand.stream().sorted().toList();
            seats = List.copyOf(seats);
            trick = List.copyOf(trick);
            history = List.copyOf(history);
        }

        // Preserve the frozen V1 random stream for the classic profile.
        @Override
        public int hashCode() {
            int h = ((seat * 31 + dealer) * 31 + size) * 31 + trump;
            h = h * 31 + hand.hashCode();
            h = h * 31 + seats.hashCode();
            h = h * 31 + trick.hashCode();
            h = h * 31 + history.hashCode();
            h = h * 31 + turnedCard;
            if (!streakBonuses) return h;
            int classic = h * 31 + streakBonusPoints;
            return streakPenaltyPoints == 5 ? classic : classic * 31 + streakPenaltyPoints;
        }
    }

    public record Choice(int value, String explanation) {}

    private IndependentPlayer() {}

    public static Choice bid(Position p) {
        PlannerControl.check();
        int prior = p.seats.stream().filter(s -> s.bid != null).mapToInt(s -> s.bid).sum();
        boolean last = p.seats.stream().filter(s -> s.bid != null).count() == p.seats.size() - 1;
        if (p.seats.size() == 4
                && p.size == 1
                && p.hand.size() == 1
                && p.turnedCard >= 0
                && p.history.isEmpty()
                && p.trick.isEmpty()) {
            double chance = OneCardBidBelief.probability(p), value = Double.NEGATIVE_INFINITY;
            int best = -1;
            for (int b = 0; b <= 1; b++)
                if (!last || prior + b != 1) {
                    double expected =
                            chance * score(p, p.seat, 1, b) + (1 - chance) * score(p, p.seat, 0, b);
                    if (expected > value) {
                        value = expected;
                        best = b;
                    }
                }
            return new Choice(
                    best,
                    "Public-bid-conditioned one-card posterior="
                            + chance
                            + "; no private opponent cards");
        }
        double[] values = new double[p.size + 1];
        int samples = 0;
        for (int k = 0; k < 24; k++) {
            PlannerControl.check();
            PlannerControl.event(PlannerControl.Event.SAMPLE);
            var world = deal(p, new Random(seed(p, k)));
            if (world == null) continue;
            samples++;
            PlannerControl.event(PlannerControl.Event.WORLD);
            for (int b = 0; b <= p.size; b++)
                if (!last || b + prior != p.size) {
                    int[] goals = goals(p, world, b);
                    int[] won = roll(p, copy(world), -1, goals, new Random(seed(p, k) ^ 0xC0AC4L));
                    values[b] += score(p, p.seat, won[p.seat], b);
                }
        }
        int best = -1;
        for (int b = 0; b <= p.size; b++)
            if ((!last || b + prior != p.size) && (best < 0 || values[b] > values[best])) best = b;
        return new Choice(
                best,
                "Independent planner; own cards/public facts; "
                        + samples
                        + " possible deals; counterfactual contract values="
                        + Arrays.toString(values));
    }

    public static Choice card(Position p) {
        PlannerControl.check();
        var model = modelCard(p);
        var proof = ContractGuarantee.choose(p, model.value());
        return proof.certified() && proof.card() != model.value()
                ? new Choice(
                        proof.card(),
                        model.explanation()
                                + "; public contract certificate across"
                                + proof.worlds()
                                + " complete feasible deals and all legal continuations")
                : model;
    }

    private static Choice modelCard(Position p) {
        var allowed = legal(p.hand, p.trick.isEmpty() ? -1 : p.trick.get(0).card / 13, p.trump);
        if (allowed.isEmpty()) throw new IllegalArgumentException("No playable card");
        if (allowed.size() == 1)
            return new Choice(
                    allowed.get(0),
                    "Only legal card under independent follow-suit/mandatory-trump rules");
        Integer dominant = lateDiscard(p);
        if (dominant != null)
            return new Choice(
                    dominant,
                    "Two-card same-suit dominance; retain the rank matching the remaining contract; public information only");
        double[] values = new double[allowed.size()], rivals = new double[allowed.size()];
        int samples = 0;
        int[] goals = p.seats.stream().mapToInt(s -> s.bid == null ? 0 : s.bid).toArray();
        for (int k = 0; k < 48; k++) {
            PlannerControl.check();
            PlannerControl.event(PlannerControl.Event.SAMPLE);
            var world = deal(p, new Random(seed(p, k)));
            if (world == null) continue;
            samples++;
            PlannerControl.event(PlannerControl.Event.WORLD);
            for (int c = 0; c < allowed.size(); c++) {
                int[] won =
                        roll(
                                p,
                                copy(world),
                                allowed.get(c),
                                goals,
                                new Random(seed(p, k) ^ 0xC0AC4L));
                values[c] += score(p, p.seat, won[p.seat], goals[p.seat]);
                rivals[c] += IndependentContinuations.rival(p, won, goals, p.seat);
            }
        }
        int best = 0;
        for (int c = 1; c < allowed.size(); c++)
            if (values[c] > values[best] + 1e-8
                    || Math.abs(values[c] - values[best]) < 1e-8 && rivals[c] < rivals[best])
                best = c;
        return new Choice(
                allowed.get(best),
                "Independent planner; "
                        + samples
                        + " constrained sampled deals; candidates="
                        + allowed
                        + "; contract sums="
                        + Arrays.toString(values)
                        + "; strongest-rival sums="
                        + Arrays.toString(rivals));
    }

    /** Every feasible unordered hand assignment, including undealt cards, or an explicit fallback. Never a partial prefix. */
    static List<List<List<Integer>>> smallDeals(Position p) {
        return IndependentDeals.smallDeals(p);
    }

    private static Integer lateDiscard(Position p) {
        if (p.hand.size() != 2 || p.trick.isEmpty() || p.seats.get(p.seat).bid == null) return null;
        int low = p.hand.get(0), high = p.hand.get(1);
        if (low / 13 != high / 13) return null;
        var a = new ArrayList<>(p.trick);
        a.add(new Play(p.seat, low));
        var b = new ArrayList<>(p.trick);
        b.add(new Play(p.seat, high));
        boolean first = winner(a, p.trump) == p.seat, second = winner(b, p.trump) == p.seat;
        int gain;
        if (!first && !second) gain = 0;
        else if (first && second && p.trick.size() == p.seats.size() - 1) gain = 1;
        else return null;
        return p.seats.get(p.seat).bid - p.seats.get(p.seat).won - gain > 0 ? low : high;
    }

    private static int[] goals(Position p, List<List<Integer>> world, int own) {
        int[] targets = new int[p.seats.size()];
        int sum = 0;
        for (int i = 1; i <= targets.length; i++) {
            int s = (p.dealer + i) % targets.length;
            Integer known = p.seats.get(s).bid;
            int bid = s == p.seat ? own : known != null ? known : estimate(world.get(s), p.trump);
            if (i == targets.length && known == null && s != p.seat && sum + bid == p.size)
                bid = bid > 0 ? bid - 1 : 1;
            targets[s] = bid;
            sum += bid;
        }
        return targets;
    }

    // Offline diagnostic solver; not selected by the accepted policy.
    static int[] exactAfter(Position p, List<List<Integer>> world, int first, int[] goals) {
        return IndependentContinuations.exactAfter(p, world, first, goals);
    }

    private static int estimate(List<Integer> hand, int trump) {
        double value = 0;
        for (int c : hand)
            value +=
                    (c % 13 == 12
                                    ? 0.92
                                    : c % 13 == 11
                                            ? 0.60
                                            : c % 13 == 10 ? 0.33 : c % 13 == 9 ? 0.15 : 0.02)
                            + (c / 13 == trump ? 0.19 : 0);
        return Math.min(hand.size(), (int) Math.round(value));
    }

    static List<List<Integer>> deal(Position p, Random r) {
        return IndependentDeals.deal(p, r);
    }

    static List<List<Integer>> feasibleDeal(Position p, Random r) {
        return IndependentDeals.feasibleDeal(p, r);
    }

    /** Public suit inferences come from completed history and the unfinished table independently. */
    static boolean[][] knownVoids(Position p) {
        return IndependentDeals.knownVoids(p);
    }

    /** Hall conditions over groups of identical seat slots; at most 63 subsets for six seats. */
    static boolean feasible(int[] need, int[] counts, boolean[][] voids) {
        return IndependentDeals.feasible(need, counts, voids);
    }

    static double oneCardWinChance(Position p) {
        int own = p.hand.get(0), pool = 0, threats = 0, lower = 0;
        boolean trump = own / 13 == p.trump;
        for (int suit = 0; suit < 4; suit++)
            for (int rank = 13 - 2 * p.seats.size(); rank < 13; rank++) {
                int c = suit * 13 + rank;
                if (c == own || c == p.turnedCard) continue;
                pool++;
                if (suit == own / 13 && rank < own % 13) lower++;
                if (trump
                        ? suit == own / 13 && rank > own % 13
                        : suit == p.trump || suit == own / 13 && rank > own % 13) threats++;
            }
        int others = p.seats.size() - 1;
        long winning =
                trump || p.seat == (p.dealer + 1) % p.seats.size()
                        ? ordered(pool - threats, others)
                        : lower * ordered(pool - 1 - threats, others - 1);
        return winning / (double) ordered(pool, others);
    }

    private static long ordered(int available, int count) {
        if (available < count) return 0;
        long value = 1;
        for (int i = 0; i < count; i++) value *= available - i;
        return value;
    }

    public static List<Integer> legal(List<Integer> hand, int led, int trump) {
        if (led < 0) return hand;
        var follows = hand.stream().filter(c -> c / 13 == led).toList();
        if (!follows.isEmpty()) return follows;
        var cuts = hand.stream().filter(c -> c / 13 == trump).toList();
        return cuts.isEmpty() ? hand : cuts;
    }

    public static int winner(List<Play> t, int trump) {
        Play best = t.get(0);
        int led = best.card / 13;
        for (Play p : t)
            if (strength(p.card, led, trump) > strength(best.card, led, trump)) best = p;
        return best.seat;
    }

    private static int strength(int c, int led, int trump) {
        return (c / 13 == trump ? 200 : c / 13 == led ? 100 : 0) + c % 13;
    }

    private static List<List<Integer>> copy(List<List<Integer>> w) {
        return IndependentDeals.copy(w);
    }

    private static long seed(Position p, int sample) {
        int seats = 1;
        for (Seat s : p.seats) {
            int h = ((s.remaining * 31 + Objects.hashCode(s.bid)) * 31 + s.won) * 31 + s.total;
            seats = 31 * seats + h;
        }
        int h = ((p.seat * 31 + p.dealer) * 31 + p.size) * 31 + p.trump;
        h = h * 31 + p.hand.hashCode();
        h = h * 31 + seats;
        h = h * 31 + p.trick.hashCode();
        h = h * 31 + p.history.hashCode();
        h = h * 31 + p.turnedCard;
        return 0xC0AC42026L + 31L * h + sample * 0x9e3779b97f4a7c15L;
    }
}
