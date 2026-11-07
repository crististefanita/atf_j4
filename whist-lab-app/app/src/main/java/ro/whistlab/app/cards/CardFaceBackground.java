package ro.whistlab.app.cards;

import ro.cardtable.ui.cards.CardFaceDrawable;
import ro.whistlab.core.model.Card;

/** Whist projection; the shared drawable never receives a domain card. */
public final class CardFaceBackground extends CardFaceDrawable {
    public CardFaceBackground(Card card, float density, int border) {
        super(WhistCardFace.from(card), density, border);
    }
}
