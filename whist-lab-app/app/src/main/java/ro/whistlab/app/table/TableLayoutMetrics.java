package ro.whistlab.app.table;

/** Seat dimensions; played-card sizing belongs to TrickCardMetrics. */
final class TableLayoutMetrics {
    static final int TRUMP_WIDTH_DP = 57;

    /** Scale the freshly measured content, never a previously enlarged frame. */
    static int seatHeightPx(int naturalHeightPx) {
        if (naturalHeightPx < 1) throw new IllegalArgumentException("Seat content must have a height");
        return Math.toIntExact((naturalHeightPx * 5L + 3) / 4);
    }

    static int topInsetPx(int boardHeightPx) { return Math.round(boardHeightPx * 0.05f); }

    static int boardHeightPx(int contentHeightPx) {
        return Math.toIntExact((contentHeightPx * 20L + 18) / 19);
    }

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
        if (tableWidthDp < 240) throw new IllegalArgumentException("Table width must be at least 240 dp");
        return Math.min(116, Math.min(Math.round(tableWidthDp * (compact ? 0.25f : 0.29f)),
                (tableWidthDp - 16) / 3));
    }
}
