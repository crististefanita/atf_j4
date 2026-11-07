package ro.whistlab.app.table;

/** Seat dimensions; played-card sizing belongs to TrickCardMetrics. */
final class TableLayoutMetrics {
    static final int TRUMP_WIDTH_DP = 57;
    static final int TRUMP_MIN_HEIGHT_DP = 48;
    static final int TRUMP_PADDING_DP = 4;
    static final int TRUMP_VALUE_GAP_DP = 2;

    /** Two measured line boxes keep every trump representation in the same frame. */
    static int trumpHeightPx(float density, int headingHeightPx, int valueHeightPx) {
        if (!Float.isFinite(density) || density <= 0 || headingHeightPx <= 0 || valueHeightPx <= 0) {
            throw new IllegalArgumentException("Trump dimensions must be positive and finite");
        }
        return Math.max(Math.round(TRUMP_MIN_HEIGHT_DP * density),
                2 * Math.round(TRUMP_PADDING_DP * density)
                        + headingHeightPx + Math.round(TRUMP_VALUE_GAP_DP * density) + valueHeightPx);
    }

    static int topInsetPx(int boardHeightPx) { return ro.cardtable.ui.table.TableSizing.topInsetPx(boardHeightPx); }

    static float fitTextSizePx(float sizePx, float textWidthPx, int availableWidthPx) {
        return sizePx * Math.min(1f, Math.max(1, availableWidthPx) / Math.max(1f, textWidthPx));
    }

    private TableLayoutMetrics() { }

    static int centerPlayWidthDp(int tableWidthDp) {
        return centerPlayWidthDp(tableWidthDp, false);
    }

    static int centerPlayWidthDp(int tableWidthDp, boolean compact) {
        return Math.min(320, tableWidthDp - 2 * seatWidthDp(tableWidthDp, compact) - 20);
    }

    static int seatWidthDp(int tableWidthDp) {
        return seatWidthDp(tableWidthDp, false);
    }

    static int seatWidthDp(int tableWidthDp, boolean compact) {
        return ro.cardtable.ui.table.TableSizing.seatWidthDp(tableWidthDp, compact);
    }
}
