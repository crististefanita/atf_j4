package ro.cardtable.ui.hand;
import ro.cardtable.ui.cards.CardFace;
import java.util.Objects;
/** One visible card and its snapshot-bound interaction state; no game command is owned here. */
public record HandCard(String id, CardFace face, String description, boolean playable) {
    public HandCard {
        Objects.requireNonNull(id); Objects.requireNonNull(face); Objects.requireNonNull(description);
        if (id.isBlank()) throw new IllegalArgumentException("A visible card needs a stable identity");
    }
}
