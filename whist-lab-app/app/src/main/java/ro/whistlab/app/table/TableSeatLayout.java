package ro.whistlab.app.table;

/** Named perimeter anchors keep player seats stable while dense controls claim the center. */
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
        if (playerCount < 3 || playerCount > 6) throw new IllegalArgumentException("Whist tables support 3–6 players: " + playerCount);
        return java.util.Arrays.stream(ro.cardtable.ui.table.TableSeatLayout.positions(playerCount))
                .map(anchor -> Anchor.valueOf(anchor.name())).toArray(Anchor[]::new);
    }

}
