package ro.whistlab.app.table;

/** Seat dimensions; played-card sizing belongs to TrickCardMetrics. */
final class TableLayoutMetrics {
    private TableLayoutMetrics() { }

    static int centerPlayWidthDp(int tableWidthDp) {
        return Math.min(320, tableWidthDp - 2 * seatWidthDp(tableWidthDp) - 20);
    }

    static int seatWidthDp(int tableWidthDp) {
        if (tableWidthDp < 240) throw new IllegalArgumentException("Table width must be at least 240 dp");
        return Math.min(116, Math.min(Math.round(tableWidthDp * 0.29f), (tableWidthDp - 16) / 3));
    }
}
