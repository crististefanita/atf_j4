package ro.whistlab.app.bidding;

/** Pure layout policy for selectable bids and the controls that submit them. */
public final class BidGridMetrics {
    public static final int COMPACT_COLUMNS = 3;
    public static final int DENSE_COLUMNS = 5;
    public static final int WIDE_COLUMNS = 6;
    public static final int CHOICE_TARGET_DP = 48;
    public static final int CHOICE_ROW_DP = 50;
    public static final int SUBMIT_TARGET_DP = 48;
    public static final int SUBMIT_WIDTH_DP = 119;
    public static final int SUBMIT_TEXT_SP = 13;
    public static final int SUBMIT_VISUAL_INSET_DP = 2;
    public static final int AUTO_GAME_TOGGLE_BASE_WIDTH_DP = 128;
    public static final int AUTO_GAME_TOGGLE_TARGET_DP = 48;
    public static final int AUTO_GAME_ACTION_GAP_DP = 8;
    public static final int AUTO_GAME_STACK_GAP_DP = 2;
    public static final int DENSE_MIN_WIDTH_DP = DENSE_COLUMNS * (CHOICE_TARGET_DP + 4);
    public static final int DENSE_MAX_WIDTH_DP = 400;

    public record ActionLayout(boolean autoGameInline, int centerWidthDp, int centerHeightDp,
            int controlsWidthDp, int autoGameToggleWidthDp) { }

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

    /** Keeps the AutoGame toggle beside the bid button only when the table still has room for its seats. */
    public static ActionLayout actionLayout(int optionCount, int tableWidthDp, int seatWidthDp,
            int regionGapDp, float fontScale) {
        if (optionCount < 0 || tableWidthDp < 1 || seatWidthDp < 1 || regionGapDp < 0
                || !Float.isFinite(fontScale) || fontScale <= 0) {
            throw new IllegalArgumentException("Bid controls need a valid viewport, seats and font scale");
        }

        int toggleWidth = Math.min(tableWidthDp,
                (int) Math.ceil(AUTO_GAME_TOGGLE_BASE_WIDTH_DP * Math.max(1f, fontScale)));
        int columns = columnCount(optionCount, tableWidthDp);
        int gridWidth = Math.max(132, Math.min(optionCount, columns) * 52);
        int inlineWidth = SUBMIT_WIDTH_DP + AUTO_GAME_ACTION_GAP_DP + toggleWidth;
        int inlineCenterWidth = Math.max(gridWidth, inlineWidth);
        boolean inline = inlineCenterWidth + 2 * seatWidthDp + 2 * regionGapDp <= tableWidthDp;
        int controlsWidth = inline ? inlineWidth : toggleWidth;
        int centerWidth = Math.max(gridWidth, controlsWidth);
        int controlHeight = SUBMIT_TARGET_DP + (inline ? 0
                : AUTO_GAME_STACK_GAP_DP + AUTO_GAME_TOGGLE_TARGET_DP);
        int centerHeight = rowCount(optionCount, tableWidthDp) * CHOICE_ROW_DP + controlHeight;
        return new ActionLayout(inline, centerWidth, centerHeight, controlsWidth, toggleWidth);
    }

}
