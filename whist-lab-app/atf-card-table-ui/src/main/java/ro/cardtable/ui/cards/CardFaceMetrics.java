package ro.cardtable.ui.cards;

/** One portrait proportion for held and played faces; only the role's scale changes. */
public record CardFaceMetrics(int heightDp) {
    public CardFaceMetrics {
        if (heightDp < 1) throw new IllegalArgumentException("Card height must be positive");
    }

    public int widthDp() {
        return Math.max(1, Math.round(heightDp * 5f / 7));
    }

    public static final int NUMBER_SCALE = 2;
    public static final int HAND_NUMBER_CAP_DP = 40;
    public static final int PLAYED_NUMBER_CAP_DP = 34;
    public static final int PLAYED_SUIT_CAP_DP = 18;
    public static final int BADGE_NUMBER_CAP_DP = 22;

    /** Length of a rank from 2 to 10 at the start of a face label. */
    public static int numberLength(String label) {
        if (label.isEmpty()) return 0;
        int length = label.startsWith("10") ? 2 : label.charAt(0) >= '2' && label.charAt(0) <= '9' ? 1 : 0;
        if (length == 0 || length == label.length()) return length;
        return "♠♥♣♦".indexOf(label.charAt(length)) >= 0 ? length : 0;
    }

    /** Fits the number independently of the suit using the renderer's measured glyphs. */
    public static float fitNumber(float requested, float width, float height, int availableWidth, int availableHeight) {
        if (!Float.isFinite(requested) || !Float.isFinite(width) || !Float.isFinite(height)
                || requested <= 0 || width <= 0 || height <= 0 || availableWidth <= 0 || availableHeight <= 0) {
            throw new IllegalArgumentException("Card number dimensions must be positive and finite");
        }
        return Math.min(requested, Math.min(availableWidth / width, availableHeight / height));
    }

    public static CardFaceMetrics atHeight(int heightDp) {
        return new CardFaceMetrics(heightDp);
    }
}
