package ro.whistlab.app.score;

/** Round subcolumns once, then reuse their sum for every player header and body group. */
record ScoreHistoryGridMetrics(int handWidthPx, int bidWidthPx, int pointsWidthPx,
        int totalWidthPx, int separatorWidthPx, int outerPaddingPx, int players) {
    static ScoreHistoryGridMetrics forDensity(int players, float density) {
        if (players < 3 || players > 6 || !Float.isFinite(density) || density <= 0) {
            throw new IllegalArgumentException("History requires 3–6 players and a finite positive density");
        }
        return new ScoreHistoryGridMetrics(Math.round(66 * density), Math.round(42 * density),
                Math.round(52 * density), Math.round(50 * density), Math.max(1, Math.round(3 * density)),
                Math.round(8 * density), players);
    }

    int playerWidthPx() { return bidWidthPx + pointsWidthPx + totalWidthPx; }
    int contentWidthPx() { return handWidthPx + players * (separatorWidthPx + playerWidthPx()); }
    int tableWidthPx() { return contentWidthPx() + 2 * outerPaddingPx; }
}
