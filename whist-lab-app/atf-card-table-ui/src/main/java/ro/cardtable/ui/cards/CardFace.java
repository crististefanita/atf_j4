package ro.cardtable.ui.cards;

import java.util.Objects;

/** Visible card marks only; rank validity and hidden cards belong to the game. */
public record CardFace(String rankLabel, String suitSymbol, boolean red) {
    public CardFace { Objects.requireNonNull(rankLabel); Objects.requireNonNull(suitSymbol); }
}
