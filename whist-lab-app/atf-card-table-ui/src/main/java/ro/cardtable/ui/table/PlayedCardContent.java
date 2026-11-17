package ro.cardtable.ui.table;
import java.util.Objects;
import ro.cardtable.ui.cards.CardFace;
/** Public deposited card, keyed by the actor's seat; not a game state or a scoring result. */
public record PlayedCardContent(int seat, String faceLabel, boolean red, String playerLabel, String description) {
    public PlayedCardContent {
        if (seat < 0) throw new IllegalArgumentException("Seat identity must be nonnegative");
        Objects.requireNonNull(faceLabel); Objects.requireNonNull(playerLabel); Objects.requireNonNull(description);
    }

    public CardFace face() {
        String label = faceLabel.trim();
        int suit = label.length() - 1;
        if (suit < 0 || "\u2660\u2665\u2663\u2666".indexOf(label.charAt(suit)) < 0) {
            return new CardFace(label, "", red);
        }
        return new CardFace(label.substring(0, suit).trim(), label.substring(suit), red);
    }
}
