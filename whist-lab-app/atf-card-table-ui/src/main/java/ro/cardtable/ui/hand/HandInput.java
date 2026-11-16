package ro.cardtable.ui.hand;

import java.util.List;

/** Input and transient feedback for one rendered snapshot. */
public final class HandInput {
    public static final long HINT_DURATION_MILLIS = 1100L;
    public static final long PLAY_LIFT_MILLIS = 150L;

    public enum Action { IGNORE, PLAY, HINT }
    public record State(String rejected, boolean selected, long revision) {
        public State {
            if (rejected == null || revision < 0 || selected && !rejected.isEmpty())
                throw new IllegalArgumentException("Invalid hand feedback");
        }
    }

    private final List<HandCard> cards;
    private final boolean hintEnabled;
    private HandCard rejected;
    private boolean selected;
    private long revision;

    public HandInput(List<HandCard> cards, boolean allowRejectedTap) {
        this(cards, allowRejectedTap, new State("", false, 0));
    }

    public HandInput(List<HandCard> cards, boolean allowRejectedTap, State state) {
        this.cards = List.copyOf(cards);
        this.hintEnabled = allowRejectedTap && this.cards.stream().anyMatch(HandCard::playable);
        if (!state.rejected().isEmpty()) {
            if (!hintEnabled) throw new IllegalArgumentException("Hand feedback is unavailable");
            rejected = this.cards.stream().filter(card -> !card.playable() && card.id().equals(state.rejected()))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Rejected card is outside the hand"));
        }
        selected = state.selected();
        revision = state.revision();
    }

    public State state() {
        return new State(rejected == null ? "" : rejected.id(), selected, revision);
    }

    public List<String> suggestions() {
        return cards.stream().filter(this::suggests).map(HandCard::id)
                .collect(java.util.stream.Collectors.toList());
    }

    public Action tap(String id) {
        for (var card : cards) {
            if (card.id().equals(id)) return tap(card);
        }
        return Action.IGNORE;
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

    public boolean clearHint(long expectedRevision) {
        if (rejected == null || revision != expectedRevision) return false;
        rejected = null;
        return true;
    }
}
