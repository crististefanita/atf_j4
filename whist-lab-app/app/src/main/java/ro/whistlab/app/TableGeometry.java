package ro.whistlab.app;

import java.util.ArrayList;
import java.util.List;

/** Measured regions, shared by Android layout and independent rectangle contracts. */
final class TableGeometry {
    record Rect(int left, int top, int right, int bottom) {
        boolean overlaps(Rect other) {
            return left < other.right && right > other.left && top < other.bottom && bottom > other.top;
        }
    }

    record Plan(List<Rect> seats, Rect center) { }

    static int minimumHeight(int players, int seatHeight, int centerHeight, int gap) {
        requirePlayers(players);
        return 2 * seatHeight + centerHeight + (players >= 5 ? 6 : 4) * gap;
    }

    static Plan arrange(int width, int height, int players, int seatWidth, int seatHeight,
            int centerWidth, int centerHeight, int gap) {
        return arrange(width, height, players, seatWidth, seatHeight, centerWidth,
                centerHeight, gap, false);
    }

    static Plan arrange(int width, int height, int players, int seatWidth, int seatHeight,
            int centerWidth, int centerHeight, int gap, boolean fullWidthCardRows) {
        requirePlayers(players);
        if (gap < 1 || seatWidth < 1 || seatHeight < 1 || centerWidth < 1 || centerHeight < 1
                || width < 3 * seatWidth + 2 * gap || centerWidth > width
                || height < minimumHeight(players, seatHeight, centerHeight, gap)) {
            throw new IllegalArgumentException("Table regions do not fit the measured viewport");
        }
        boolean wideCenter = centerWidth + 2 * seatWidth + 2 * gap > width;
        TableSeatLayout.Anchor[] anchors = TableSeatLayout.positions(players);
        if (players == 4 && wideCenter) anchors = new TableSeatLayout.Anchor[]{
                TableSeatLayout.Anchor.SOUTH, TableSeatLayout.Anchor.NORTH_WEST,
                TableSeatLayout.Anchor.NORTH, TableSeatLayout.Anchor.NORTH_EAST};
        int centerTop = (height - centerHeight) / 2;
        int flankClearance = wideCenter ? centerTop - seatHeight - gap
                : (height - 2 * seatHeight - gap) / 2;
        int stagger = fullWidthCardRows ? 0 : Math.min(height / 5, flankClearance);
        List<Rect> seats = new ArrayList<>();
        for (var anchor : anchors) {
            int x = switch (anchor) {
                case NORTH_WEST, WEST, SOUTH_WEST -> 0;
                case NORTH, SOUTH -> (width - seatWidth) / 2;
                case NORTH_EAST, EAST, SOUTH_EAST -> width - seatWidth;
            };
            int y = switch (anchor) {
                case NORTH -> 0;
                case NORTH_WEST, NORTH_EAST -> stagger;
                case WEST, EAST -> (height - seatHeight) / 2;
                case SOUTH_WEST, SOUTH_EAST -> height - seatHeight - stagger;
                case SOUTH -> height - seatHeight;
            };
            seats.add(new Rect(x, y, x + seatWidth, y + seatHeight));
        }
        Rect center = new Rect((width - centerWidth) / 2, (height - centerHeight) / 2,
                (width + centerWidth) / 2, (height + centerHeight) / 2);
        return new Plan(List.copyOf(seats), center);
    }

    private static void requirePlayers(int players) {
        if (players < 3 || players > 6) throw new IllegalArgumentException("A table requires 3–6 players");
    }
}
