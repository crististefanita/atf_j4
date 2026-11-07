package ro.whistlab.app.hand;

/** Compatibility boundary: the shared library owns all hand geometry. */
public final class HandLayoutMetrics {
    private HandLayoutMetrics() { }
    public static final int CARD_HEIGHT_DP = ro.cardtable.ui.hand.HandLayoutMetrics.CARD_HEIGHT_DP;
    public static final int CARD_WIDTH_DP = ro.cardtable.ui.hand.HandLayoutMetrics.CARD_WIDTH_DP;
    public static final int MIN_VISIBLE_ADVANCE_DP = ro.cardtable.ui.hand.HandLayoutMetrics.MIN_VISIBLE_ADVANCE_DP;
    public static final int LIFT_CLEARANCE_DP = ro.cardtable.ui.hand.HandLayoutMetrics.LIFT_CLEARANCE_DP;
    public static final int CARD_LIFT_DP = ro.cardtable.ui.hand.HandLayoutMetrics.CARD_LIFT_DP;
    public static final int NEIGHBOR_Z_DP = ro.cardtable.ui.hand.HandLayoutMetrics.NEIGHBOR_Z_DP;
    public static final int SELECTED_Z_DP = ro.cardtable.ui.hand.HandLayoutMetrics.SELECTED_Z_DP;
    public static final float CARD_LIFT_SCALE = ro.cardtable.ui.hand.HandLayoutMetrics.CARD_LIFT_SCALE;
    public static final int DENSE_HAND_MIN_ADVANCE_DP = ro.cardtable.ui.hand.HandLayoutMetrics.DENSE_HAND_MIN_ADVANCE_DP;

    public record Fan(int width, int first, int advance, int cardWidth) {
        public int left(int index) { return first + index * advance; }
    }

    public static int indexBandWidthPx(float glyphWidthPx, int horizontalPaddingPx) {
        return ro.cardtable.ui.hand.HandLayoutMetrics.indexBandWidthPx(glyphWidthPx, horizontalPaddingPx);
    }

    public static boolean usesSingleRow(int cardCount) {
        return ro.cardtable.ui.hand.HandLayoutMetrics.usesSingleRow(cardCount);
    }

    public static int cardWidthDp(float fontScale) {
        return ro.cardtable.ui.hand.HandLayoutMetrics.cardWidthDp(fontScale);
    }

    public static int cardWidthDp(int cardCount, int availableWidthDp, float fontScale) {
        return ro.cardtable.ui.hand.HandLayoutMetrics.cardWidthDp(cardCount, availableWidthDp, fontScale);
    }

    public static int cardLabelSizeSp() {
        return ro.cardtable.ui.hand.HandLayoutMetrics.cardLabelSizeSp();
    }

    public static int cardHeightDp() {
        return ro.cardtable.ui.hand.HandLayoutMetrics.cardHeightDp();
    }

    public static int horizontalAdvanceDp(int cardCount, int availableWidthDp, float fontScale) {
        return ro.cardtable.ui.hand.HandLayoutMetrics.horizontalAdvanceDp(cardCount, availableWidthDp, fontScale);
    }

    public static int horizontalAdvanceDp(int cardCount, int availableWidthDp, float fontScale, int measuredIndexWidthDp) {
        return ro.cardtable.ui.hand.HandLayoutMetrics.horizontalAdvanceDp(cardCount, availableWidthDp, fontScale, measuredIndexWidthDp);
    }

    public static Fan fan(int count, int viewportPx, float density, float fontScale, int indexWidthPx) {
        var fan = ro.cardtable.ui.hand.HandLayoutMetrics.fan(count, viewportPx, density, fontScale, indexWidthPx);
        return new Fan(fan.width(), fan.first(), fan.advance(), fan.cardWidth());
    }

    public static int contentWidthDp(int cardCount, int availableWidthDp, float fontScale) {
        return ro.cardtable.ui.hand.HandLayoutMetrics.contentWidthDp(cardCount, availableWidthDp, fontScale);
    }

    public static boolean requiresHorizontalScroll(int cardCount, int availableWidthDp, float fontScale) {
        return ro.cardtable.ui.hand.HandLayoutMetrics.requiresHorizontalScroll(cardCount, availableWidthDp, fontScale);
    }

    public static int reservedHandHeightDp() {
        return ro.cardtable.ui.hand.HandLayoutMetrics.reservedHandHeightDp();
    }

    public static int minVisibleAdvanceDp(float fontScale) {
        return ro.cardtable.ui.hand.HandLayoutMetrics.minVisibleAdvanceDp(fontScale);
    }

    public static int maxViewportWidthDp() {
        return ro.cardtable.ui.hand.HandLayoutMetrics.maxViewportWidthDp();
    }
}
