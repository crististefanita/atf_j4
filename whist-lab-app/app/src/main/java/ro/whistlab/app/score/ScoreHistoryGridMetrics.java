package ro.whistlab.app.score;

/** Round subcolumns once, then reuse their sum for every player header and body group. */
record ScoreHistoryGridMetrics(int handWidthPx, int bidWidthPx, int pointsWidthPx,
        int totalWidthPx, int separatorWidthPx, int outerPaddingPx, int players) {
    static ScoreHistoryGridMetrics forDensity(int players, float density) {
        if (players < 3 || players > 6 || !Float.isFinite(density) || density <= 0) {
            throw new IllegalArgumentException("History requires 3–6 players and a finite positive density");
        }
        return new ScoreHistoryGridMetrics(Math.round(29.7f * density), Math.round(18.9f * density),
                Math.round(23.4f * density), Math.round(22.5f * density), Math.max(1, Math.round(3 * density)),
                Math.round(7.2f * density), players);
    }

    ScoreHistoryGridMetrics fitContent(int hand, int bid, int points, int total, int name) {
        if (hand < 0 || bid < 0 || points < 0 || total < 0 || name < 0)
            throw new IllegalArgumentException("Measured widths cannot be negative");
        int b = Math.max(bidWidthPx, bid), p = Math.max(pointsWidthPx, points), t = Math.max(totalWidthPx, total);
        t += Math.max(0, name - b - p - t);
        return new ScoreHistoryGridMetrics(Math.max(handWidthPx, hand), b, p, t,
                separatorWidthPx, outerPaddingPx, players);
    }

    int playerWidthPx() { return bidWidthPx + pointsWidthPx + totalWidthPx; }
    int contentWidthPx() { return handWidthPx + players * (separatorWidthPx + playerWidthPx()); }
    int tableWidthPx() { return contentWidthPx() + 2 * outerPaddingPx; }
}
