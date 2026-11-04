package ro.whistlab.core.ai.simulation;

import ro.whistlab.core.ai.engine.MonteCarloAi.View;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.model.Trump;
import ro.whistlab.core.rules.GameRules;

import java.util.*;

/** Constructs possible worlds; never reads a session or an opponent's actual hidden hand. */
public final class PossibleDeals {
    private PossibleDeals() {}

    public static List<Card> unknownCards(View view) {
        List<Card> pool = new ArrayList<>(GameRules.deck(view.mode(), view.seats().size()));
        pool.removeAll(view.hand());
        for (PlayedCard play : view.history()) pool.remove(play.card());
        for (PlayedCard play : view.trick()) pool.remove(play.card());
        if (view.turnedCard() != null) pool.remove(view.turnedCard());
        return pool;
    }

    public static List<List<Card>> sample(View view, Random random) {
        List<Card> pool = unknownCards(view);
        boolean[][] voids = knownVoids(view);
        Collections.shuffle(pool, random);
        List<Integer> slots = new ArrayList<>();
        for (int seat = 0; seat < view.seats().size(); seat++)
            if (seat != view.seat())
                for (int i = 0; i < view.seats().get(seat).remaining(); i++) slots.add(seat);
        Collections.shuffle(slots, random);
        int[] matched = new int[pool.size()];
        Arrays.fill(matched, -1);
        for (int slot = 0; slot < slots.size(); slot++)
            if (!augment(slot, slots, pool, voids, matched, new boolean[pool.size()])) return null;
        List<List<Card>> hands = new ArrayList<>();
        for (int s = 0; s < view.seats().size(); s++) hands.add(new ArrayList<>());
        hands.set(view.seat(), new ArrayList<>(view.hand()));
        for (int i = 0; i < pool.size(); i++)
            if (matched[i] >= 0) hands.get(slots.get(matched[i])).add(pool.get(i));
        return hands;
    }

    public static boolean[][] knownVoids(View view) {
        boolean[][] voids = new boolean[view.seats().size()][4];
        int index = 0;
        Suit led = null;
        for (PlayedCard play : view.history()) {
            if (index % view.seats().size() == 0) led = play.card().suit();
            markPublicVoid(view, voids, play, led);
            index++;
        }
        if (!view.trick().isEmpty()) {
            Suit currentLead = view.trick().get(0).card().suit();
            for (PlayedCard play : view.trick()) markPublicVoid(view, voids, play, currentLead);
        }
        return voids;
    }

    private static void markPublicVoid(View view, boolean[][] voids, PlayedCard play, Suit led) {
        if (play.card().suit() == led) return;
        voids[play.seat()][led.ordinal()] = true;
        if (view.mode().isRomanian()
                && view.trump() != Trump.NO_TRUMP
                && play.card().suit().ordinal() != view.trump().ordinal())
            voids[play.seat()][view.trump().ordinal()] = true;
    }

    private static boolean augment(
            int slot,
            List<Integer> owners,
            List<Card> pool,
            boolean[][] voids,
            int[] matched,
            boolean[] seen) {
        for (int c = 0; c < pool.size(); c++)
            if (!seen[c] && !voids[owners.get(slot)][pool.get(c).suit().ordinal()]) {
                seen[c] = true;
                if (matched[c] < 0 || augment(matched[c], owners, pool, voids, matched, seen)) {
                    matched[c] = slot;
                    return true;
                }
            }
        return false;
    }

    public static List<List<List<Card>>> enumerateSmallDeals(View v) {
        List<Card> pool = unknownCards(v);
        if (pool.size() > 10 || v.seats().stream().anyMatch(s -> s.remaining() > 2))
            return List.of();
        boolean[][] voids = knownVoids(v);
        List<List<Card>> hands = new ArrayList<>();
        for (int s = 0; s < v.seats().size(); s++) hands.add(new ArrayList<>());
        hands.set(v.seat(), new ArrayList<>(v.hand()));
        List<List<List<Card>>> result = new ArrayList<>();
        enumerate(v, pool, voids, hands, 0, result);
        return result.size() > 256 ? List.of() : result;
    }

    private static void enumerate(
            View v,
            List<Card> available,
            boolean[][] voids,
            List<List<Card>> hands,
            int seat,
            List<List<List<Card>>> result) {
        if (result.size() > 256) return;
        if (seat == hands.size()) {
            result.add(copy(hands));
            return;
        }
        if (seat == v.seat()) {
            enumerate(v, available, voids, hands, seat + 1, result);
            return;
        }
        choose(
                v,
                available,
                voids,
                hands,
                seat,
                0,
                v.seats().get(seat).remaining(),
                new ArrayList<>(),
                result);
    }

    private static void choose(
            View v,
            List<Card> available,
            boolean[][] voids,
            List<List<Card>> hands,
            int seat,
            int at,
            int need,
            List<Card> chosen,
            List<List<List<Card>>> result) {
        if (result.size() > 256) return;
        if (need == 0) {
            hands.set(seat, new ArrayList<>(chosen));
            List<Card> rest = new ArrayList<>(available);
            rest.removeAll(chosen);
            enumerate(v, rest, voids, hands, seat + 1, result);
            return;
        }
        for (int i = at; i <= available.size() - need; i++)
            if (!voids[seat][available.get(i).suit().ordinal()]) {
                chosen.add(available.get(i));
                choose(v, available, voids, hands, seat, i + 1, need - 1, chosen, result);
                chosen.remove(chosen.size() - 1);
            }
    }

    public static List<List<Card>> copy(List<List<Card>> hands) {
        List<List<Card>> result = new ArrayList<>();
        for (List<Card> hand : hands) result.add(new ArrayList<>(hand));
        return result;
    }
}
