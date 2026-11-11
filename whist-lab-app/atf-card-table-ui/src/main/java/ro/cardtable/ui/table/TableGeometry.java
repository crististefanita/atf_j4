package ro.cardtable.ui.table;

import java.util.ArrayList;
import java.util.List;

/** Measured regions, shared by Android layout and independent rectangle contracts. */
public final class TableGeometry {
    public enum CenterAlignment { BALANCED, TOWARD_HUMAN }

    public record Rect(int left, int top, int right, int bottom) {
        public boolean overlaps(Rect other) {
            return left < other.right && right > other.left && top < other.bottom && bottom > other.top;
        }
    }

    public record Plan(List<Rect> seats, Rect center) { }

    public record VerticalBand(int top, int bottom) {
        public int height() { return bottom - top; }
        public int centeredTop(int contentHeight) { return top + (height() - contentHeight) / 2; }
    }

    /** Free band between the upper and lower seats of a full-width card layout. */
    public static VerticalBand fullWidthCardBand(Plan plan, int gap) {
        if (gap < 1) throw new IllegalArgumentException("Card band requires a positive gap");
        var anchors = TableSeatLayout.positions(plan.seats().size());
        int upperBottom = 0;
        int lowerTop = plan.seats().get(0).top();
        for (int index = 0; index < anchors.length; index++) {
            var seat = plan.seats().get(index);
            switch (anchors[index]) {
                case NORTH, NORTH_WEST, NORTH_EAST -> upperBottom = Math.max(upperBottom, seat.bottom());
                case SOUTH, SOUTH_WEST, SOUTH_EAST -> lowerTop = Math.min(lowerTop, seat.top());
                default -> throw new IllegalArgumentException("Unexpected flank on a full-width card layout");
            }
        }
        return new VerticalBand(upperBottom + gap, lowerTop - gap);
    }

    /** Reserves the display inset; the overload accepts an optional bottom gap from spare height. */
    public static Plan arrangeDisplay(int width, int height, int players, int seatWidth, int seatHeight,
            int centerWidth, int centerHeight, int gap, boolean fullWidthCardRows,
            CenterAlignment centerAlignment) {
        return arrangeDisplay(width, height, players, seatWidth, seatHeight, centerWidth, centerHeight,
                gap, fullWidthCardRows, centerAlignment, 0);
    }

    /** Uses available vertical slack to separate the table from the hand without shrinking card regions. */
    public static Plan arrangeDisplay(int width, int height, int players, int seatWidth, int seatHeight,
            int centerWidth, int centerHeight, int gap, boolean fullWidthCardRows,
            CenterAlignment centerAlignment, int requestedHandClearance) {
        if (requestedHandClearance < 0) throw new IllegalArgumentException("Hand clearance cannot be negative");
        int minimumHeight = minimumHeight(players, seatHeight, centerHeight, gap);
        int handClearance = Math.min(requestedHandClearance, Math.max(0, height - minimumHeight));
        int geometryHeight = height - handClearance;
        int inset = Math.min(TableSizing.topInsetPx(geometryHeight),
                Math.max(0, geometryHeight - minimumHeight));
        Plan base = arrange(width, geometryHeight - inset, players, seatWidth, seatHeight,
                centerWidth, centerHeight, gap, fullWidthCardRows, centerAlignment);
        List<Rect> seats = new ArrayList<>();
        for (Rect box : base.seats()) seats.add(lower(box, inset));
        return new Plan(List.copyOf(seats), lower(base.center(), inset));
    }

    private static Rect lower(Rect box, int offset) {
        return new Rect(box.left(), box.top() + offset, box.right(), box.bottom() + offset);
    }

    public static int minimumHeight(int players, int seatHeight, int centerHeight, int gap) {
        requirePlayers(players);
        return 2 * seatHeight + centerHeight + (players >= 5 ? 6 : 4) * gap;
    }

    public static Plan arrange(int width, int height, int players, int seatWidth, int seatHeight,
            int centerWidth, int centerHeight, int gap) {
        return arrange(width, height, players, seatWidth, seatHeight, centerWidth,
                centerHeight, gap, false);
    }

    public static Plan arrange(int width, int height, int players, int seatWidth, int seatHeight,
            int centerWidth, int centerHeight, int gap, boolean fullWidthCardRows) {
        return arrange(width, height, players, seatWidth, seatHeight, centerWidth,
                centerHeight, gap, fullWidthCardRows, CenterAlignment.BALANCED);
    }

    public static Plan arrange(int width, int height, int players, int seatWidth, int seatHeight,
            int centerWidth, int centerHeight, int gap, boolean fullWidthCardRows,
            CenterAlignment centerAlignment) {
        requirePlayers(players);
        if (gap < 1 || seatWidth < 1 || seatHeight < 1 || centerWidth < 1 || centerHeight < 1
                || width < 3 * seatWidth + 2 * gap || centerWidth > width
                || height < minimumHeight(players, seatHeight, centerHeight, gap)) {
            throw new IllegalArgumentException("Table regions do not fit the measured viewport");
        }
        boolean wideCenter = fullWidthCardRows || centerWidth + 2 * seatWidth + 2 * gap > width;
        TableSeatLayout.Anchor[] anchors = TableSeatLayout.positions(players);
        int centerTop = (height - centerHeight) / 2;
        int flankClearance = wideCenter || players == 4 ? centerTop - seatHeight - gap
                : (height - 2 * seatHeight - gap) / 2;
        int stagger = fullWidthCardRows && players != 4 ? 0 : Math.min(height / 5, Math.max(0, flankClearance));
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
        int centerLeft = (width - centerWidth) / 2;
        if (centerAlignment == CenterAlignment.TOWARD_HUMAN && !fullWidthCardRows) {
            int humanSideLimit = height - centerHeight;
            for (Rect seat : seats) {
                boolean sharesCenterColumn = seat.left() < centerLeft + centerWidth
                        && seat.right() > centerLeft;
                boolean belowCenter = seat.top() + seat.bottom() > height;
                if (sharesCenterColumn && belowCenter) {
                    humanSideLimit = Math.min(humanSideLimit, seat.top() - gap - centerHeight);
                }
            }
            int safeShift = Math.max(0, humanSideLimit - centerTop);
            centerTop += Math.round(safeShift * 0.62f);
        }
        Rect center = new Rect(centerLeft, centerTop, centerLeft + centerWidth,
                centerTop + centerHeight);
        return new Plan(List.copyOf(seats), center);
    }

    private static void requirePlayers(int players) {
        if (players < 3 || players > 6) throw new IllegalArgumentException("A table requires 3–6 players");
    }
}
