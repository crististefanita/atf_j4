package ro.whistlab.coach;

import ro.whistlab.coach.IndependentPlayer.Play;
import ro.whistlab.coach.IndependentPlayer.Position;

import java.util.*;

/** Feasible hidden deals inferred only from the independent player's own cards and public facts. */
final class IndependentDeals {
    private IndependentDeals() {}

    static List<List<List<Integer>>> smallDeals(Position p) {
        PlannerControl.check();
        if (p.seats().size() != 4 || p.seats().stream().anyMatch(s -> s.remaining() > 2))
            return List.of();
        var pool = unknownCards(p);
        if (pool.size() > 10) return List.of();
        boolean[][] voids = knownVoids(p);
        var hands = new ArrayList<List<Integer>>();
        for (int s = 0; s < p.seats().size(); s++) hands.add(new ArrayList<>());
        hands.set(p.seat(), new ArrayList<>(p.hand()));
        var results = new ArrayList<List<List<Integer>>>();
        enumerate(p, pool, voids, hands, 0, results);
        return results.size() > 256 ? List.of() : List.copyOf(results);
    }

    static void enumerate(
            Position p,
            List<Integer> available,
            boolean[][] voids,
            List<List<Integer>> hands,
            int seat,
            List<List<List<Integer>>> out) {
        PlannerControl.check();
        PlannerControl.event(PlannerControl.Event.ENUMERATION);
        if (out.size() > 256) return;
        if (seat == hands.size()) {
            out.add(copy(hands));
            return;
        }
        if (seat == p.seat()) {
            enumerate(p, available, voids, hands, seat + 1, out);
            return;
        }
        choose(
                p,
                available,
                voids,
                hands,
                seat,
                0,
                p.seats().get(seat).remaining(),
                new ArrayList<>(),
                out);
    }

    static void choose(
            Position p,
            List<Integer> available,
            boolean[][] voids,
            List<List<Integer>> hands,
            int seat,
            int at,
            int need,
            List<Integer> chosen,
            List<List<List<Integer>>> out) {
        PlannerControl.check();
        PlannerControl.event(PlannerControl.Event.ENUMERATION);
        if (out.size() > 256) return;
        if (need == 0) {
            hands.set(seat, new ArrayList<>(chosen));
            var rest = new ArrayList<>(available);
            rest.removeAll(chosen);
            enumerate(p, rest, voids, hands, seat + 1, out);
            return;
        }
        for (int i = at; i <= available.size() - need; i++)
            if (!voids[seat][available.get(i) / 13]) {
                chosen.add(available.get(i));
                choose(p, available, voids, hands, seat, i + 1, need - 1, chosen, out);
                chosen.remove(chosen.size() - 1);
            }
    }

    static List<List<Integer>> deal(Position p, Random r) {
        PlannerControl.check();
        var pool = unknownCards(p);
        boolean[][] voids = knownVoids(p);
        // Preserve the established distribution on easy states; the Hall fallback guarantees
        // an assignment when a feasible late deal defeats bounded rejection sampling.
        for (int attempt = 0; attempt < 64; attempt++) {
            PlannerControl.check();
            PlannerControl.event(PlannerControl.Event.REJECTION);
            Collections.shuffle(pool, r);
            PlannerControl.check();
            var hands = new ArrayList<List<Integer>>();
            for (int s = 0; s < p.seats().size(); s++) hands.add(new ArrayList<>());
            hands.set(p.seat(), new ArrayList<>(p.hand()));
            var available = new ArrayList<>(pool);
            boolean valid = true;
            for (int s = 0; s < p.seats().size() && valid; s++)
                if (s != p.seat())
                    for (int j = 0; j < p.seats().get(s).remaining(); j++) {
                        int at = -1;
                        for (int i = 0; i < available.size(); i++)
                            if (!voids[s][available.get(i) / 13]) {
                                at = i;
                                break;
                            }
                        if (at < 0) {
                            valid = false;
                            break;
                        }
                        hands.get(s).add(available.remove(at));
                    }
            if (valid) return hands;
        }
        return feasibleDeal(p, r);
    }

    static List<List<Integer>> feasibleDeal(Position p, Random r) {
        var pool = unknownCards(p);
        boolean[][] voids = knownVoids(p);
        var hands = new ArrayList<List<Integer>>();
        int[] need = new int[p.seats().size()], suits = new int[4];
        for (int s = 0; s < p.seats().size(); s++) {
            hands.add(new ArrayList<>());
            if (s != p.seat()) need[s] = p.seats().get(s).remaining();
        }
        hands.set(p.seat(), new ArrayList<>(p.hand()));
        for (int c : pool) suits[c / 13]++;
        if (!feasible(need, suits, voids)) return null;
        while (Arrays.stream(need).sum() > 0) {
            PlannerControl.check();
            int seat = -1, slack = Integer.MAX_VALUE;
            var order = new ArrayList<Integer>();
            for (int s = 0; s < need.length; s++) if (need[s] > 0) order.add(s);
            Collections.shuffle(order, r);
            for (int s : order) {
                int eligible = 0;
                for (int suit = 0; suit < 4; suit++) if (!voids[s][suit]) eligible += suits[suit];
                if (eligible - need[s] < slack) {
                    slack = eligible - need[s];
                    seat = s;
                }
            }
            Collections.shuffle(pool, r);
            need[seat]--;
            boolean placed = false;
            for (int i = 0; i < pool.size(); i++) {
                int c = pool.get(i);
                if (voids[seat][c / 13]) continue;
                suits[c / 13]--;
                if (feasible(need, suits, voids)) {
                    hands.get(seat).add(c);
                    pool.remove(i);
                    placed = true;
                    break;
                }
                suits[c / 13]++;
            }
            if (!placed)
                throw new IllegalStateException("Hall-feasible deal lost a viable assignment");
        }
        return hands;
    }

    static boolean[][] knownVoids(Position p) {
        boolean[][] voids = new boolean[p.seats().size()][4];
        for (int i = 0; i < p.history().size(); i++) {
            Play play = p.history().get(i);
            int led = p.history().get(i - i % p.seats().size()).card() / 13;
            markVoid(p, voids, play, led);
        }
        if (!p.trick().isEmpty()) {
            int led = p.trick().get(0).card() / 13;
            for (Play play : p.trick()) markVoid(p, voids, play, led);
        }
        return voids;
    }

    static void markVoid(Position p, boolean[][] voids, Play play, int led) {
        if (play.card() / 13 == led) return;
        voids[play.seat()][led] = true;
        if (p.trump() >= 0 && p.trump() < 4 && play.card() / 13 != p.trump())
            voids[play.seat()][p.trump()] = true;
    }

    static boolean feasible(int[] need, int[] counts, boolean[][] voids) {
        PlannerControl.check();
        PlannerControl.event(PlannerControl.Event.FEASIBILITY);
        for (int n : need) if (n < 0) return false;
        for (int n : counts) if (n < 0) return false;
        for (int mask = 1; mask < (1 << need.length); mask++) {
            PlannerControl.check();
            int required = 0, allowed = 0;
            for (int s = 0; s < need.length; s++)
                if ((mask & (1 << s)) != 0 && need[s] > 0) {
                    required += need[s];
                    for (int suit = 0; suit < 4; suit++) if (!voids[s][suit]) allowed |= 1 << suit;
                }
            int available = 0;
            for (int suit = 0; suit < 4; suit++)
                if ((allowed & (1 << suit)) != 0) available += counts[suit];
            if (required > available) return false;
        }
        return true;
    }

    static List<List<Integer>> copy(List<List<Integer>> w) {
        var r = new ArrayList<List<Integer>>();
        for (var h : w) r.add(new ArrayList<>(h));
        return r;
    }

    private static List<Integer> unknownCards(Position p) {
        boolean[] used = new boolean[52];
        for (int c : p.hand()) used[c] = true;
        for (Play x : p.history()) used[x.card()] = true;
        for (Play x : p.trick()) used[x.card()] = true;
        if (p.turnedCard() >= 0) used[p.turnedCard()] = true;
        var pool = new ArrayList<Integer>();
        for (int suit = 0; suit < 4; suit++)
            for (int rank = 13 - 2 * p.seats().size(); rank < 13; rank++)
                if (!used[suit * 13 + rank]) pool.add(suit * 13 + rank);
        return pool;
    }
}
