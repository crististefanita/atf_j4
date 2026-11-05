package ro.whistlab.app.score;

/** Round subcolumns once, then reuse their sum for every player header and body group. */
record ScoreHistoryGridMetrics(int handWidthPx, int bidWidthPx, int pointsWidthPx,
        int separatorWidthPx, int outerPaddingPx, int players) {
    ScoreHistoryGridMetrics {
        if (handWidthPx <= 0 || bidWidthPx <= 0 || pointsWidthPx <= 0
                || separatorWidthPx <= 0 || outerPaddingPx < 0 || players < 3 || players > 6)
            throw new IllegalArgumentException("History grid requires positive columns and 3–6 players");
        try {
            int group = Math.addExact(bidWidthPx, pointsWidthPx);
            Math.addExact(handWidthPx, Math.addExact(Math.multiplyExact(players, Math.addExact(separatorWidthPx, group)),
                    Math.multiplyExact(2, outerPaddingPx)));
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("History grid exceeds the pixel range", overflow);
        }
    }
    static ScoreHistoryGridMetrics forDensity(int players, float density) {
        if (players < 3 || players > 6 || !Float.isFinite(density) || density <= 0) {
            throw new IllegalArgumentException("History requires 3–6 players and a finite positive density");
        }
        return new ScoreHistoryGridMetrics(Math.round(29.7f * density), Math.round(18.9f * density),
                Math.round(23.4f * density), Math.max(1, Math.round(3 * density)),
                Math.round(7.2f * density), players);
    }

    ScoreHistoryGridMetrics fitContent(int hand, int bid, int points, int name) {
        if (hand < 0 || bid < 0 || points < 0 || name < 0)
            throw new IllegalArgumentException("Measured widths cannot be negative");
        int b = Math.max(bidWidthPx, bid), p = Math.max(pointsWidthPx, points);
        try { p = Math.addExact(p, Math.max(0, name - Math.addExact(b, p))); }
        catch (ArithmeticException overflow) { throw new IllegalArgumentException("Measured history content exceeds the pixel range", overflow); }
        return new ScoreHistoryGridMetrics(Math.max(handWidthPx, hand), b, p,
                separatorWidthPx, outerPaddingPx, players);
    }

    int playerWidthPx() { return bidWidthPx + pointsWidthPx; }
    int contentWidthPx() { return handWidthPx + players * (separatorWidthPx + playerWidthPx()); }
    int tableWidthPx() { return contentWidthPx() + 2 * outerPaddingPx; }
}
