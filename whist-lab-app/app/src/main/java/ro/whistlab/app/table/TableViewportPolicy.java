package ro.whistlab.app.table;

import ro.whistlab.app.hand.HandLayoutMetrics;

/** Reclaims compact-screen space from decoration before reducing display-only card sizing. */
final class TableViewportPolicy {
    private final boolean compact;
    private final boolean shortWindow;
    TableViewportPolicy(int availableHeightDp) {
        compact = availableHeightDp < 760;
        shortWindow = availableHeightDp < 600;
    }
    boolean isCompact() { return compact; }
    int seatHeadingHeightDp() { return compact ? 20 : 0; }
    int regionGapDp() { return compact ? 2 : 8; }
    int minimumBoardHeightDp() { return compact ? 0 : 320; }
    boolean showPlayHint() { return !compact; }
    int handClearanceDp(boolean bidding) { return compact && bidding ? 12 : HandLayoutMetrics.LIFT_CLEARANCE_DP; }
    int seatVerticalPaddingDp() { return compact ? 1 : 4; }
    int outerVerticalPaddingDp() { return compact ? 2 : 4; }
    int turnBadgeVerticalPaddingDp() { return compact ? 1 : 2; }
    int turnBadgeSlotHeightDp() { return compact ? 14 : 16; }
    int playedCardFaceHeightDp() { return shortWindow ? 94 : TrickCardMetrics.CARD_HEIGHT_DP; }
    int playedCardItemVerticalPaddingDp() {
        return compact ? 2 : TrickCardMetrics.ITEM_VERTICAL_PADDING_DP;
    }
}
