package ro.cardtable.ui.table.trump;

/** Display contract for the two-line trump tile. Adapters measure glyphs in their own renderer. */
public final class TrumpTileMetrics {
    public static final int WIDTH_DP = 57;
    public static final int MIN_HEIGHT_DP = 48;
    public static final int PADDING_DP = 4;
    public static final int VALUE_GAP_DP = 2;
    public static final int HEADING_SP = 9;
    public static final int VALUE_SP = 20;
    public static final int CARD_INSET_DP = 5;
    public static final int CARD_RADIUS_DP = 5;

    private TrumpTileMetrics() { }

    public static int heightPx(float density, int headingHeightPx, int valueHeightPx) {
        if (!Float.isFinite(density) || density <= 0 || headingHeightPx <= 0 || valueHeightPx <= 0) {
            throw new IllegalArgumentException("Trump dimensions must be positive and finite");
        }
        return Math.max(Math.round(MIN_HEIGHT_DP * density), 2 * Math.round(PADDING_DP * density)
                + headingHeightPx + Math.round(VALUE_GAP_DP * density) + valueHeightPx);
    }

    public static float fitTextSizePx(float nominalSizePx, float glyphWidthPx, int innerWidthPx) {
        return nominalSizePx * Math.min(1f, Math.max(1, innerWidthPx) / Math.max(1f, glyphWidthPx));
    }
}
