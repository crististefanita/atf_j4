package ro.cardtable.browser;

import java.util.List;
import java.util.Map;
import ro.cardtable.ui.cards.CardFace;
import ro.cardtable.ui.hand.HandCard;
import ro.cardtable.ui.hand.HandInput;

/** Maps one public hand to the same snapshot-bound tap policy as Android. */
final class BrowserHandAdapter {
    record Feedback(String action, String rejected, boolean selected, long revision, List<String> suggestions) { }

    private BrowserHandAdapter() { }

    static Feedback apply(BrowserGame.View view, Map<String, String> fields) {
        if (view == null || view.header() == null || view.score() != null)
            throw new IllegalArgumentException("Hand feedback is unavailable");
        var cards = cards(view.hand());
        var state = new HandInput.State(fields.getOrDefault("rejected", ""),
                "true".equals(fields.get("selected")), Long.parseLong(fields.getOrDefault("hintRevision", "0")));
        var hand = new HandInput(cards, true, state);
        var result = switch (fields.getOrDefault("operation", "")) {
            case "tap" -> hand.tap(fields.getOrDefault("card", ""));
            case "clear" -> {
                hand.clearHint(Long.parseLong(fields.getOrDefault("expires", "-1")));
                yield HandInput.Action.IGNORE;
            }
            default -> throw new IllegalArgumentException("Unknown hand operation");
        };
        var next = hand.state();
        return new Feedback(result.name(), next.rejected(), next.selected(), next.revision(), hand.suggestions());
    }

    static List<HandCard> cards(List<BrowserGame.Card> cards) {
        return cards.stream().map(card -> new HandCard(card.rank() + card.suit(),
                new CardFace(card.rank(), card.suit(), card.suit().equals("♥") || card.suit().equals("♦")),
                card.label(), !card.action().isEmpty())).toList();
    }
}
