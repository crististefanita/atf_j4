package ro.whistlab.core.ai.simulation;

import ro.whistlab.core.ai.engine.MonteCarloAi.Seat;
import ro.whistlab.core.ai.engine.MonteCarloAi.View;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.rules.GameRules;

import java.util.*;

/** Continues one hypothetical world using the accepted rollout and its unchanged random stream. */
public final class RolloutPolicy {
    private RolloutPolicy() {}

    public static int[] rollout(
            View view, List<List<Card>> hands, Card first, int[] targets, Random random) {
        int n = hands.size();
        int[] won = view.seats().stream().mapToInt(Seat::won).toArray();
        List<PlayedCard> trick = new ArrayList<>(view.trick());
        int seat = first == null ? (view.dealer() + 1) % n : view.seat();
        boolean initial = true;
        int remaining = hands.stream().mapToInt(List::size).sum();
        while (remaining-- > 0) {
            SearchCancellation.check();
            SearchWork.event(SearchWork.Event.ROLLOUT);
            Card choice =
                    initial && first != null
                            ? first
                            : greedy(
                                    hands.get(seat),
                                    trick,
                                    view,
                                    seat,
                                    targets[seat] - won[seat],
                                    random);
            if (!hands.get(seat).remove(choice))
                throw new IllegalStateException("Simulation attempted an unheld card");
            trick.add(new PlayedCard(seat, choice));
            initial = false;
            if (trick.size() == n) {
                seat = GameRules.winningSeat(trick, view.trump());
                won[seat]++;
                trick.clear();
            } else seat = (seat + 1) % n;
        }
        if (!trick.isEmpty())
            throw new IllegalStateException("Simulation ended before a complete trick");
        return won;
    }

    public static Card greedy(
            List<Card> hand,
            List<PlayedCard> trick,
            View view,
            int seat,
            int needed,
            Random random) {
        Suit led = trick.isEmpty() ? null : trick.get(0).card().suit();
        List<Card> legal = GameRules.legalCards(hand, led, view.trump(), view.mode());
        Comparator<Card> cost =
                Comparator.comparingInt(
                        (Card c) ->
                                c.rank()
                                        + (view.trump()
                                                        .suit()
                                                        .filter(s -> s == c.suit())
                                                        .isPresent()
                                                ? 13
                                                : 0));
        boolean wants = needed > 0 || !view.mode().isRomanian() && needed < 0;
        if (trick.isEmpty())
            return wants
                    ? legal.stream().max(cost).orElseThrow()
                    : legal.stream().min(cost).orElseThrow();
        List<Card> preferred = new ArrayList<>();
        for (Card c : legal) {
            List<PlayedCard> test = new ArrayList<>(trick);
            test.add(new PlayedCard(seat, c));
            if ((GameRules.winningSeat(test, view.trump()) == seat) == wants) preferred.add(c);
        }
        List<Card> options = preferred.isEmpty() ? legal : preferred;
        // Small rollout-policy diversity prevents every sampled opponent using one fixed tie order.
        if (random.nextInt(8) == 0) return options.get(random.nextInt(options.size()));
        return wants
                ? options.stream().min(cost).orElseThrow()
                : options.stream().max(cost).orElseThrow();
    }
}
