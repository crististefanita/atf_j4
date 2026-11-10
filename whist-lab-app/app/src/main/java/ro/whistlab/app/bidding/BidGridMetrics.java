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
    private static final int LEGACY_SUBMIT_VERTICAL_INSET_DP = 2;
    private static final float SUBMIT_FACE_SCALE = .85f;
    public static final int DENSE_MIN_WIDTH_DP = DENSE_COLUMNS * (CHOICE_TARGET_DP + 4);
    public static final int DENSE_MAX_WIDTH_DP = 400;

    public record ActionLayout(int centerWidthDp, int centerHeightDp) { }
    public record SubmitInsets(int horizontalPx, int verticalPx) { }

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

    /** Keeps bid choices and submission inside the center band; hand automation belongs in the header. */
    public static ActionLayout actionLayout(int optionCount, int tableWidthDp, float fontScale) {
        if (optionCount < 0 || tableWidthDp < 1 || !Float.isFinite(fontScale) || fontScale <= 0) {
            throw new IllegalArgumentException("Bid controls need a valid viewport and font scale");
        }

        int columns = columnCount(optionCount, tableWidthDp);
        int gridWidth = Math.max(132, Math.min(optionCount, columns) * 52);
        int centerWidth = Math.max(gridWidth, SUBMIT_WIDTH_DP);
        int centerHeight = rowCount(optionCount, tableWidthDp) * CHOICE_ROW_DP + SUBMIT_TARGET_DP;
        return new ActionLayout(centerWidth, centerHeight);
    }

    /** Shrinks the frame inside the touch target while leaving room for the measured label. */
    public static SubmitInsets submitInsets(float density, int textWidthPx, int textHeightPx) {
        int width = Math.round(SUBMIT_WIDTH_DP * density);
        int height = Math.round(SUBMIT_TARGET_DP * density);
        int legacyHeight = height - 2 * Math.round(LEGACY_SUBMIT_VERTICAL_INSET_DP * density);
        return new SubmitInsets(
                visualInset(width, width, textWidthPx + 2 * Math.round(6 * density)),
                visualInset(height, legacyHeight, textHeightPx + 2 * Math.round(3 * density)));
    }

    private static int visualInset(int target, int legacyFace, int minimumFace) {
        int desiredInset = Math.round((target - legacyFace * SUBMIT_FACE_SCALE) / 2);
        return Math.max(0, Math.min(desiredInset, (target - minimumFace) / 2));
    }
}
