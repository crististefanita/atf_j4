package ro.whistlab.app.cards;

import ro.cardtable.ui.cards.CardFace;
import ro.whistlab.core.model.Card;

/** Projects only visible card marks; the shared UI does not consume a Whist domain card. */
public final class WhistCardFace {
    private WhistCardFace() { }
    public static CardFace from(Card card) {
        return new CardFace(card.rankLabel(),card.suit().symbol(),card.suit().red());
    }
}
