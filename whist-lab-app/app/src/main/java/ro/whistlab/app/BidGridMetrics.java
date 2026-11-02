package ro.whistlab.app;

/** Pure layout policy for the selectable bid grid and its table clearance. */
final class BidGridMetrics {
    static final int CHOICES_PER_ROW = 6;
    private static final int BASELINE_ROWS = 2;
    // Preserves breathing room between a third selector row and perimeter seats.
    private static final int TABLE_HEIGHT_PER_EXTRA_ROW_DP = 96;

    private BidGridMetrics() { }

    static int rowCount(int optionCount) {
        if (optionCount < 0) {
            throw new IllegalArgumentException("Bid option count cannot be negative");
        }
        return (optionCount + CHOICES_PER_ROW - 1) / CHOICES_PER_ROW;
    }

    static int additionalTableHeightDp(int optionCount) {
        int additionalRows = Math.max(0, rowCount(optionCount) - BASELINE_ROWS);
        return additionalRows * TABLE_HEIGHT_PER_EXTRA_ROW_DP;
    }
}
