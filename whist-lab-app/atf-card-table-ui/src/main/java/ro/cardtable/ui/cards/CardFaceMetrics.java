package ro.cardtable.ui.cards;

/** One portrait proportion for held and played faces; only the role's scale changes. */
public record CardFaceMetrics(int heightDp) {
    public static final int HAND_HEIGHT_DP = 88;
    public static final int INDEX_SIZE_SP = 13;
    public static final float RANK_SCALE = 1.5f;
    public static final int INDEX_TOP_DP = 5;
    public static final int INDEX_SIDE_DP = 1;
    public static final int CENTER_SUIT_SIZE_DP = 28;
    public static final int INVERTED_SIZE_DP = 9;
    public static final float INVERTED_RANK_SIZE_DP = INVERTED_SIZE_DP * RANK_SCALE;
    public static final float CENTER_SUIT_BASELINE = 0.66f;

    public CardFaceMetrics {
        if (heightDp < 1) throw new IllegalArgumentException("Card height must be positive");
    }

    public int widthDp() {
        return Math.max(1, Math.round(heightDp * 5f / 7));
    }

    public float scale() {
        return (float) heightDp / HAND_HEIGHT_DP;
    }

    public float indexSizeSp() {
        return INDEX_SIZE_SP * scale();
    }

    public float rankSizeSp() {
        return indexSizeSp() * RANK_SCALE;
    }

    public static CardFaceMetrics atHeight(int heightDp) {
        return new CardFaceMetrics(heightDp);
    }
}
