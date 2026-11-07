package ro.cardtable.ui.table;

/** Shared seat footprint and perimeter inset; no game scoring or trump policy. */
public final class TableSizing {
    private TableSizing() { }
    public static int topInsetPx(int boardHeightPx) { return Math.round(boardHeightPx * 0.05f); }

    public static int seatWidthDp(int tableWidthDp, boolean compact) {
        if (tableWidthDp < 240) throw new IllegalArgumentException("Table width must be at least 240 dp");
        return Math.min(116, Math.min(Math.round(tableWidthDp * (compact ? 0.25f : 0.29f)),
                (tableWidthDp - 16) / 3));
    }
}
