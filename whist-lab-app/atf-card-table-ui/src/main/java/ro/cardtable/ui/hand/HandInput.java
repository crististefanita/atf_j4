package ro.cardtable.ui.hand;

import java.util.List;

/** Input and transient feedback for one rendered snapshot. */
final class HandInput {
    enum Action { IGNORE, PLAY, HINT }

    private final List<HandCard> cards;
    private final boolean hintEnabled;
    private HandCard rejected;
    private boolean selected;
    private long revision;

    HandInput(List<HandCard> cards, boolean allowRejectedTap) {
        this.cards = List.copyOf(cards);
        this.hintEnabled = allowRejectedTap && this.cards.stream().anyMatch(HandCard::playable);
    }

    boolean acceptsTap(HandCard card) {
        return !selected && cards.contains(card) && (card.playable() || hintEnabled);
    }

    Action tap(HandCard card) {
        if (!acceptsTap(card)) return Action.IGNORE;
        revision++;
        if (card.playable()) {
            selected = true;
            rejected = null;
            return Action.PLAY;
        }
        rejected = card;
        return Action.HINT;
    }

    boolean rejects(HandCard card) { return card.equals(rejected); }
    boolean suggests(HandCard card) { return rejected != null && card.playable(); }
    long revision() { return revision; }

    boolean clearHint(long expectedRevision) {
        if (rejected == null || revision != expectedRevision) return false;
        rejected = null;
        return true;
    }
}
