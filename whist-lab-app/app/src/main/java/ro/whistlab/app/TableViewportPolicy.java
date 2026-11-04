package ro.whistlab.app;

/** Space is reclaimed from decoration, never from card faces or 48 dp controls. */
final class TableViewportPolicy {
    private final boolean compact;
    TableViewportPolicy(int availableHeightDp) { compact = availableHeightDp < 600; }
    int regionGapDp() { return compact ? 3 : 8; }
    int minimumBoardHeightDp() { return compact ? 0 : 320; }
    boolean showPlayHint() { return !compact; }
    int handClearanceDp(boolean bidding) { return compact && bidding ? 12 : HandLayoutMetrics.LIFT_CLEARANCE_DP; }
}
