package ro.whistlab.app.cards;

/** One portrait proportion for held and played faces; only the role's scale changes. */
public record CardFaceMetrics(int heightDp) {
    public CardFaceMetrics {
        if (heightDp < 1) throw new IllegalArgumentException("Card height must be positive");
    }

    public int widthDp() {
        return ro.cardtable.ui.cards.CardFaceMetrics.atHeight(heightDp).widthDp();
    }

    public static CardFaceMetrics atHeight(int heightDp) {
        return new CardFaceMetrics(heightDp);
    }
}
