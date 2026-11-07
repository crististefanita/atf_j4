package ro.cardtable.ui.cards;

/** One portrait proportion for held and played faces; only the role's scale changes. */
public record CardFaceMetrics(int heightDp) {
    public CardFaceMetrics {
        if (heightDp < 1) throw new IllegalArgumentException("Card height must be positive");
    }

    public int widthDp() {
        return Math.max(1, Math.round(heightDp * 5f / 7));
    }

    public static CardFaceMetrics atHeight(int heightDp) {
        return new CardFaceMetrics(heightDp);
    }
}
