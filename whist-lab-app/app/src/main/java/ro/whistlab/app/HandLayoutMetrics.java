package ro.whistlab.app;

/** Pure geometry for the one-row, overlapping hand fan. */
final class HandLayoutMetrics {
    static final int CARD_WIDTH_DP = 66;
    static final int CARD_HEIGHT_DP = 98;
    static final int MIN_VISIBLE_ADVANCE_DP = 22;
    static final int LIFT_CLEARANCE_DP = 28;
    static final int CARD_LIFT_DP = 20;
    static final int NEIGHBOR_Z_DP = 1;
    static final int SELECTED_Z_DP = -1;
    static final float CARD_LIFT_SCALE = 1.12f;
    private static final int FULL_SIZE_HAND_CARD_LIMIT = 8;
    private static final int MAX_PLAYABLE_HAND_SIZE = 13;
    private static final int MIN_DENSE_CARD_WIDTH_DP = 40;
    static final int DENSE_HAND_MIN_ADVANCE_DP = 19;
    private static final int MIN_CARD_COUNT = 1;
    private static final int MAX_CARD_COUNT = 17;

    private HandLayoutMetrics() { }

    static boolean usesSingleRow(int cardCount) {
        return cardCount >= MIN_CARD_COUNT && cardCount <= MAX_CARD_COUNT;
    }

    static int cardWidthDp(float fontScale) {
        return Math.max(CARD_WIDTH_DP, Math.round(CARD_WIDTH_DP * Math.min(fontScale, 1.5f)));
    }

    static int cardWidthDp(int cardCount, int availableWidthDp, float fontScale) {
        requireHand(cardCount, availableWidthDp, fontScale);
        int preferredWidth = cardWidthDp(fontScale);
        if (cardCount <= FULL_SIZE_HAND_CARD_LIMIT || cardCount > MAX_PLAYABLE_HAND_SIZE) {
            return preferredWidth;
        }
        int readableWidth = Math.round(MIN_DENSE_CARD_WIDTH_DP * Math.min(fontScale, 1.5f));
        int halfOverlapWidth = (2 * availableWidthDp) / (cardCount + 1);
        return Math.min(preferredWidth, Math.max(readableWidth, halfOverlapWidth));
    }

    static int cardLabelSizeSp() {
        return 13;
    }

    static int cardHeightDp() {
        return CARD_HEIGHT_DP;
    }

    static int horizontalAdvanceDp(int cardCount, int availableWidthDp, float fontScale) {
        return horizontalAdvanceDp(cardCount, availableWidthDp, fontScale,
                Math.round(DENSE_HAND_MIN_ADVANCE_DP * fontScale));
    }

    static int horizontalAdvanceDp(int cardCount, int availableWidthDp, float fontScale, int measuredIndexWidthDp) {
        requireHand(cardCount, availableWidthDp, fontScale);
        if (measuredIndexWidthDp < 1) throw new IllegalArgumentException("Card index must have a measured width");
        int cardWidth = cardWidthDp(cardCount, availableWidthDp, fontScale);
        if (cardCount == 1) return cardWidth;
        int fitAdvance = (availableWidthDp - cardWidth) / (cardCount - 1);
        int minimumAdvance = cardCount <= MAX_PLAYABLE_HAND_SIZE
                ? Math.round(DENSE_HAND_MIN_ADVANCE_DP * Math.min(fontScale, 1.5f))
                : minVisibleAdvanceDp(fontScale);
        return Math.max(measuredIndexWidthDp, Math.max(minimumAdvance, fitAdvance));
    }

    static int contentWidthDp(int cardCount, int availableWidthDp, float fontScale) {
        int advance = horizontalAdvanceDp(cardCount, availableWidthDp, fontScale);
        return cardCount == 1 ? advance
                : (cardCount - 1) * advance + cardWidthDp(cardCount, availableWidthDp, fontScale);
    }

    static boolean requiresHorizontalScroll(int cardCount, int availableWidthDp, float fontScale) {
        return contentWidthDp(cardCount, availableWidthDp, fontScale) > availableWidthDp;
    }

    static int reservedHandHeightDp() {
        return CARD_HEIGHT_DP + LIFT_CLEARANCE_DP;
    }

    static int minVisibleAdvanceDp(float fontScale) {
        if (fontScale <= 0f) throw new IllegalArgumentException("Font scale must be positive");
        return Math.round(MIN_VISIBLE_ADVANCE_DP * Math.min(fontScale, 1.5f));
    }

    static int maxViewportWidthDp() {
        return 760;
    }

    private static void requireHand(int cardCount, int availableWidthDp, float fontScale) {
        if (!usesSingleRow(cardCount)) throw new IllegalArgumentException("Unsupported hand size: " + cardCount);
        if (availableWidthDp <= 0 || fontScale <= 0f) {
            throw new IllegalArgumentException("Hand width and font scale must be positive");
        }
    }
}
