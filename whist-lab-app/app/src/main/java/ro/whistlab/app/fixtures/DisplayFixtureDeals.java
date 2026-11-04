package ro.whistlab.app.fixtures;



import java.util.ArrayList;
import java.util.List;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;

/** Deterministic display states, reached only through MainActivity's debug intent gate. */
public final class DisplayFixtureDeals {
    private DisplayFixtureDeals() { }

    public static List<List<Card>> fourPlayerHands(int cards) {
        if (cards < 4 || cards > 13) throw new IllegalArgumentException("Display hand must contain 4–13 cards");
        List<Card> deck = new ArrayList<>(GameRules.deck(GameMode.DESCENDING, 4));
        List<Card> human = new ArrayList<>();
        for (Card card : deck) if (card.rank() == 10) human.add(card);
        deck.removeAll(human);
        while (human.size() < cards) human.add(deck.remove(0));
        List<List<Card>> hands = new ArrayList<>();
        hands.add(List.copyOf(human));
        for (int seat = 1; seat < 4; seat++) {
            hands.add(List.copyOf(deck.subList(0, cards)));
            deck.subList(0, cards).clear();
        }
        return List.copyOf(hands);
    }
}
