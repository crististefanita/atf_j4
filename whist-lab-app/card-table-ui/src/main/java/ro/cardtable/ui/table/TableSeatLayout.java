package ro.cardtable.ui.table;

/** Named perimeter anchors keep player seats stable while dense controls claim the center. */
public final class TableSeatLayout {


    public enum Anchor {
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

    public static Anchor[] positions(int playerCount) {
        return switch (playerCount) {
            case 3 -> new Anchor[]{Anchor.SOUTH, Anchor.NORTH_WEST, Anchor.NORTH_EAST};
            case 4 -> new Anchor[]{Anchor.SOUTH, Anchor.NORTH_WEST, Anchor.NORTH, Anchor.NORTH_EAST};
            case 5 -> new Anchor[]{Anchor.SOUTH, Anchor.SOUTH_WEST, Anchor.NORTH_WEST,
                    Anchor.NORTH_EAST, Anchor.SOUTH_EAST};
            case 6 -> new Anchor[]{Anchor.SOUTH, Anchor.SOUTH_WEST, Anchor.NORTH_WEST,
                    Anchor.NORTH, Anchor.NORTH_EAST, Anchor.SOUTH_EAST};
            default -> throw new IllegalArgumentException("Tables support 3–6 players: " + playerCount);
        };
    }

}
