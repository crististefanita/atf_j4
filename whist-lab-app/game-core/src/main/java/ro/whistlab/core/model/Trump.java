package ro.whistlab.core.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public enum Trump {
    SPADES(Suit.SPADES),
    HEARTS(Suit.HEARTS),
    CLUBS(Suit.CLUBS),
    DIAMONDS(Suit.DIAMONDS),
    NO_TRUMP(null);

    private static final List<Trump> APP_SEQUENCE =
            Collections.unmodifiableList(Arrays.asList(SPADES, HEARTS, CLUBS, DIAMONDS, NO_TRUMP));

    private final Suit suit;

    Trump(Suit suit) {
        this.suit = suit;
    }

    public Optional<Suit> suit() {
        return Optional.ofNullable(suit);
    }

    public static Trump forHandIndex(int zeroBasedHandIndex) {
        if (zeroBasedHandIndex < 0) {
            throw new IllegalArgumentException("Hand index cannot be negative");
        }
        return APP_SEQUENCE.get(zeroBasedHandIndex % APP_SEQUENCE.size());
    }
}
