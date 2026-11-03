package ro.whistlab.app;

/** Named perimeter anchors keep player seats on the approved oval in every phase. */
final class TableSeatLayout {
    enum Anchor {
        NORTH,
        NORTH_WEST,
        NORTH_EAST,
        WEST,
        EAST,
        SOUTH_WEST,
        SOUTH_EAST,
        SOUTH
    }

    private TableSeatLayout() { }

    static Anchor[] positions(int playerCount) {
        return switch (playerCount) {
            case 3 -> new Anchor[]{Anchor.SOUTH, Anchor.NORTH_WEST, Anchor.NORTH_EAST};
            case 4 -> new Anchor[]{Anchor.SOUTH, Anchor.WEST, Anchor.NORTH, Anchor.EAST};
            case 5 -> new Anchor[]{Anchor.SOUTH, Anchor.NORTH_WEST, Anchor.NORTH_EAST,
                    Anchor.SOUTH_EAST, Anchor.SOUTH_WEST};
            case 6 -> new Anchor[]{Anchor.SOUTH, Anchor.NORTH_WEST, Anchor.NORTH,
                    Anchor.NORTH_EAST, Anchor.SOUTH_WEST, Anchor.SOUTH_EAST};
            default -> throw new IllegalArgumentException("Whist tables support 3–6 players: " + playerCount);
        };
    }

    static float sideInsetFraction(boolean bidding) {
        return bidding ? 0.18f : 0.23f;
    }

    static float sideInsetFraction(boolean bidding, int tableWidthDp) {
        if (tableWidthDp <= 0) throw new IllegalArgumentException("Table width must be positive");
        if (!bidding) return 0.23f;
        return tableWidthDp < BidGridMetrics.DENSE_MAX_WIDTH_DP ? 0.16f : 0.18f;
    }
}
