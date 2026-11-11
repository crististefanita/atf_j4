package ro.cardtable.ui.cards;

/** Visual emphasis for a visible card selected by the game. */
public enum CardHighlight {
    IN_PLACE(1.5f, 9, 0),
    RAISED(1f, 6, 8);

    private final float strokeDp;
    private final int radiusDp;
    private final int elevationDp;

    CardHighlight(float strokeDp, int radiusDp, int elevationDp) {
        this.strokeDp = strokeDp;
        this.radiusDp = radiusDp;
        this.elevationDp = elevationDp;
    }

    public int fill() { return 0xfffff6d7; }
    public int outline() { return 0xffc99537; }
    public float radiusPx(float density) {
        return this == RAISED ? Math.round(radiusDp * density) : radiusDp * density;
    }
    public int elevationDp() { return elevationDp; }
    public int strokeWidthPx(float density) { return Math.max(1, Math.round(strokeDp * density)); }

    public int liftDp(int cardHeightDp) {
        if (cardHeightDp < 1) throw new IllegalArgumentException("Card height must be positive");
        return this == RAISED ? Math.max(4, Math.round(cardHeightDp * .08f)) : 0;
    }
}
