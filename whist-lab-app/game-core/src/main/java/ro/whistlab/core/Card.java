package ro.whistlab.core;

import java.util.Objects;

public record Card(Suit suit, int rank) {
    public Card {
        Objects.requireNonNull(suit, "suit");
        if (rank < 2 || rank > 14) {
            throw new IllegalArgumentException("Card rank must be between 2 and 14: " + rank);
        }
    }

    public String rankLabel() {
        return switch (rank) {
            case 14 -> "A";
            case 13 -> "K";
            case 12 -> "Q";
            case 11 -> "J";
            default -> Integer.toString(rank);
        };
    }

    public String shortLabel() {
        return rankLabel() + suit.symbol();
    }

    public String accessibleLabel() {
        String rankName = switch (rank) {
            case 14 -> "Ace";
            case 13 -> "King";
            case 12 -> "Queen";
            case 11 -> "Jack";
            default -> Integer.toString(rank);
        };
        return rankName + " of " + suit.label();
    }
}
