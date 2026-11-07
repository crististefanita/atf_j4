package ro.cardtable.ui.seat;
import java.util.List;
import java.util.Objects;
/** Ordered public facts; labels and values have no scoring semantics in the renderer. */
public record SeatContent(int seat, String name, List<Fact> facts, boolean active, String description) {
    public record Fact(String label, String value) {
        public Fact { Objects.requireNonNull(label); Objects.requireNonNull(value); }
        public String inlineText() { return label + " " + value; }
    }
    public SeatContent {
        if (seat < 0) throw new IllegalArgumentException("Seat identity must be nonnegative");
        Objects.requireNonNull(name); Objects.requireNonNull(description); facts = List.copyOf(facts);
    }
}
