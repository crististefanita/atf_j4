package ro.whistlab.app.bidding;

/** Pure layout policy for the selectable bid grid and its table clearance. */
public final class BidGridMetrics {
    public static final int COMPACT_COLUMNS = 3;
    public static final int DENSE_COLUMNS = 5;
    public static final int WIDE_COLUMNS = 6;
    public static final int CHOICE_TARGET_DP = 48;
    public static final int CHOICE_ROW_DP = 50;
    public static final int SUBMIT_TARGET_DP = 48;
    public static final int DENSE_MIN_WIDTH_DP = DENSE_COLUMNS * (CHOICE_TARGET_DP + 4);
    public static final int DENSE_MAX_WIDTH_DP = 400;

    private BidGridMetrics() { }

    public static int rowCount(int optionCount, int availableWidthDp) {
        if (optionCount < 0) {
            throw new IllegalArgumentException("Bid option count cannot be negative");
        }
        int columns = columnCount(optionCount, availableWidthDp);
        return columns == 0 ? 0 : (optionCount + columns - 1) / columns;
    }

    public static int columnCount(int optionCount, int availableWidthDp) {
        if (optionCount <= 0) return 0;
        if (optionCount > COMPACT_COLUMNS
                && optionCount <= 9
                && availableWidthDp >= DENSE_MIN_WIDTH_DP
                && availableWidthDp < DENSE_MAX_WIDTH_DP) {
            return DENSE_COLUMNS;
        }
        return optionCount > COMPACT_COLUMNS * COMPACT_COLUMNS
                ? Math.max(1, Math.min(WIDE_COLUMNS, availableWidthDp / (CHOICE_TARGET_DP + 4)))
                : COMPACT_COLUMNS;
    }

}
