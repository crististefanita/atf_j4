package ro.whistlab.app.table;

import ro.whistlab.app.hand.HandLayoutMetrics;

/** Reclaims compact-screen space from decoration before reducing display-only card sizing. */
final class TableViewportPolicy {
    private final boolean compact;
    private final boolean shortWindow;
    TableViewportPolicy(int availableHeightDp) {
        this(availableHeightDp,1f);
    }
    TableViewportPolicy(int availableHeightDp,float fontScale) {
        if(!Float.isFinite(fontScale)||fontScale<=0)throw new IllegalArgumentException("Font scale must be positive and finite");
        compact = availableHeightDp < 760 * Math.max(1f,fontScale);
        shortWindow = availableHeightDp < 600;
    }
    boolean isCompact() { return compact; }
    int regionGapDp() { return compact ? 1 : 8; }
    int minimumBoardHeightDp() { return compact ? 0 : 320; }
    boolean showPlayHint() { return !compact; }
    int handClearanceDp(boolean bidding) { return compact && bidding ? 12 : HandLayoutMetrics.LIFT_CLEARANCE_DP; }
    int seatVerticalPaddingDp() { return compact ? 1 : 4; }
    int outerVerticalPaddingDp() { return compact ? 0 : 4; }
    int playedCardFaceHeightDp() { return shortWindow ? TrickCardMetrics.SHORT_CARD_HEIGHT_DP : TrickCardMetrics.CARD_HEIGHT_DP; }
    int playedCardItemVerticalPaddingDp() {
        return compact ? 2 : TrickCardMetrics.ITEM_VERTICAL_PADDING_DP;
    }
}
