package ro.whistlab.app.table;

import ro.whistlab.app.hand.HandLayoutMetrics;

/** Reclaims compact-screen space from decoration before reducing display-only card sizing. */
final class TableViewportPolicy {
    private final boolean compact;
    TableViewportPolicy(int availableHeightDp) { compact = availableHeightDp < 600; }
    int regionGapDp() { return compact ? 3 : 8; }
    int minimumBoardHeightDp() { return compact ? 0 : 320; }
    boolean showPlayHint() { return !compact; }
    int handClearanceDp(boolean bidding) { return compact && bidding ? 12 : HandLayoutMetrics.LIFT_CLEARANCE_DP; }
    int seatVerticalPaddingDp() { return compact ? 2 : 4; }
    int turnBadgeVerticalPaddingDp() { return compact ? 1 : 2; }
    int turnBadgeSlotHeightDp() { return compact ? 14 : 16; }
    int playedCardFaceHeightDp() { return compact ? 96 : TrickCardMetrics.CARD_HEIGHT_DP; }
}
