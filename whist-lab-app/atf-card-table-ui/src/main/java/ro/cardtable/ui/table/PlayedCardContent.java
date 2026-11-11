package ro.cardtable.ui.table;
import java.util.Objects;
/** Public deposited card, keyed by the actor's seat; not a game state or a scoring result. */
public record PlayedCardContent(int seat, String faceLabel, boolean red, String playerLabel, String description) {
    public PlayedCardContent {
        if (seat < 0) throw new IllegalArgumentException("Seat identity must be nonnegative");
        Objects.requireNonNull(faceLabel); Objects.requireNonNull(playerLabel); Objects.requireNonNull(description);
    }
}
