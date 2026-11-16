package ro.cardtable.ui.cards;

/** One portrait proportion for held and played faces; only the role's scale changes. */
public record CardFaceMetrics(int heightDp) {
    public CardFaceMetrics {
        if (heightDp < 1) throw new IllegalArgumentException("Card height must be positive");
    }

    public int widthDp() {
        return Math.max(1, Math.round(heightDp * 5f / 7));
    }

    public static final int RANK_SCALE = 2;
    public static final int HAND_RANK_CAP_DP = 33;
    public static final int PLAYED_RANK_CAP_DP = 34;
    public static final int PLAYED_SUIT_CAP_DP = 18;
    public static final int BADGE_RANK_CAP_DP = 17;

    public static int compactDimension(int previous) {
        if (previous < 1) throw new IllegalArgumentException("Card dimension must be positive");
        return Math.max(1, Math.round(previous * .75f));
    }

    /** Length of the complete rank in a card label, with or without its suit. */
    public static int rankLength(String label) {
        if (label.isEmpty()) return 0;
        int length = label.startsWith("10") ? 2 : "23456789JQKA".indexOf(label.charAt(0)) >= 0 ? 1 : 0;
        if (length == 0 || length == label.length()) return length;
        return label.length() == length + 1 && "♠♥♣♦".indexOf(label.charAt(length)) >= 0 ? length : 0;
    }

    /** Fits the rank independently of the suit using the renderer's measured glyphs. */
    public static float fitRank(float requested, float width, float height, int availableWidth, int availableHeight) {
        if (!Float.isFinite(requested) || !Float.isFinite(width) || !Float.isFinite(height)
                || requested <= 0 || width <= 0 || height <= 0 || availableWidth <= 0 || availableHeight <= 0) {
            throw new IllegalArgumentException("Card rank dimensions must be positive and finite");
        }
        return Math.min(requested, Math.min(availableWidth / width, availableHeight / height));
    }

    public static CardFaceMetrics atHeight(int heightDp) {
        return new CardFaceMetrics(heightDp);
    }
}
