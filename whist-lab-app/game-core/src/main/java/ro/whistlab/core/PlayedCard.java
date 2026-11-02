package ro.whistlab.core;

import java.util.Objects;

public record PlayedCard(int seat, Card card) {
    public PlayedCard {
        if (seat < 0) {
            throw new IllegalArgumentException("Seat cannot be negative");
        }
        Objects.requireNonNull(card, "card");
    }
}
