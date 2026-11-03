package ro.whistlab.app;

/** Fixed table geometry keeps the human seat stable while the hand changes. */
final class TableLayoutMetrics {
    private static final int PLAYING_BASE_HEIGHT_DP = 360;
    private static final int EXTRA_PLAYER_HEIGHT_DP = 8;
    private static final int SEAT_ROW_RESERVE_DP = 16;
    private static final int BID_SEAT_PERIMETER_RESERVE_DP = 132;
    private static final float PLAY_CENTER_ROW_WEIGHT = 2.3f;
    static final int TRICK_CARD_WIDTH_DP = 44;
    static final int TRICK_CARD_HEIGHT_DP = 52;

    private TableLayoutMetrics() { }

    static int playingHeightDp(int playerCount, float fontScale) {
        requirePlayerCount(playerCount);
        return PLAYING_BASE_HEIGHT_DP + (playerCount - 3) * EXTRA_PLAYER_HEIGHT_DP
                + accessibilityAllowance(fontScale);
    }

    static int playingCenterRowHeightDp(int playerCount, float fontScale) {
        int tableHeight = playingHeightDp(playerCount, fontScale);
        return (int) Math.ceil(tableHeight * PLAY_CENTER_ROW_WEIGHT
                / (PLAY_CENTER_ROW_WEIGHT + 2f));
    }

    static int playingOuterRowHeightDp(int playerCount, float fontScale) {
        return (int) Math.ceil(playingHeightDp(playerCount, fontScale)
                / (PLAY_CENTER_ROW_WEIGHT + 2f));
    }

    static int centerPlayWidthDp(int tableWidthDp) {
        if (tableWidthDp <= 0) throw new IllegalArgumentException("Table width must be positive");
        return Math.min(320, Math.max(148, Math.round(tableWidthDp * 0.46f)));
    }

    static int seatWidthDp(int tableWidthDp) {
        if (tableWidthDp <= 0) throw new IllegalArgumentException("Table width must be positive");
        int proportionalWidth = Math.max(76, Math.round(tableWidthDp * 0.29f));
        return Math.min(proportionalWidth, Math.max(76, tableWidthDp / 3 - 4));
    }

    static int biddingHeightDp(int playerCount, int optionCount, int availableWidthDp, float fontScale) {
        requirePlayerCount(playerCount);
        int centerRow = biddingCenterRowHeightDp(optionCount, availableWidthDp);
        int totalForCenter = centerRow + BID_SEAT_PERIMETER_RESERVE_DP;
        return Math.max(playingHeightDp(playerCount, 1f), totalForCenter)
                + accessibilityAllowance(fontScale);
    }

    static int biddingCenterRowHeightDp(int optionCount, int availableWidthDp) {
        return BidGridMetrics.centerHeightDp(optionCount, availableWidthDp) + 20 + SEAT_ROW_RESERVE_DP;
    }

    private static int accessibilityAllowance(float fontScale) {
        if (fontScale <= 0f) throw new IllegalArgumentException("Font scale must be positive");
        return Math.round(Math.max(0f, fontScale - 1f) * 90f);
    }

    private static void requirePlayerCount(int playerCount) {
        if (playerCount < 3 || playerCount > 6) {
            throw new IllegalArgumentException("Whist tables support 3–6 players: " + playerCount);
        }
    }
}
