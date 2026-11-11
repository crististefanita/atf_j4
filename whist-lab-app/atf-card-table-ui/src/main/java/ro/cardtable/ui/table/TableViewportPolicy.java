package ro.cardtable.ui.table;

import ro.cardtable.ui.hand.HandLayoutMetrics;

/** Reclaims compact-screen space from decoration before reducing display-only card sizing. */
public class TableViewportPolicy {
    private final boolean compact;
    private final boolean shortWindow;
    public TableViewportPolicy(int availableHeightDp) {
        this(availableHeightDp,1f);
    }
    public TableViewportPolicy(int availableHeightDp,float fontScale) {
        if(!Float.isFinite(fontScale)||fontScale<=0)throw new IllegalArgumentException("Font scale must be positive and finite");
        compact = availableHeightDp < 760 * Math.max(1f,fontScale);
        shortWindow = availableHeightDp < 600;
    }
    public boolean isCompact() { return compact; }
    public int regionGapDp() { return compact ? 1 : 8; }
    public int minimumBoardHeightDp() { return compact ? 0 : 320; }
    public boolean showPlayHint() { return !compact; }
    public int handClearanceDp(boolean controlsVisible) { return compact && controlsVisible ? 12 : HandLayoutMetrics.LIFT_CLEARANCE_DP; }
    public int seatVerticalPaddingDp() { return compact ? 1 : 4; }
    public int outerVerticalPaddingDp() { return compact ? 0 : 4; }
    public int playedCardFaceHeightDp() { return shortWindow ? PlayedCardMetrics.SHORT_CARD_HEIGHT_DP : PlayedCardMetrics.CARD_HEIGHT_DP; }
    public int playedCardItemVerticalPaddingDp() {
        return compact ? 2 : PlayedCardMetrics.ITEM_VERTICAL_PADDING_DP;
    }
}
