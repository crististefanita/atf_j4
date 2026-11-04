package ro.whistlab.app;

import java.util.ArrayList;
import java.util.List;

/** Stable, seat-oriented trick slots; the full number of rows is reserved before play. */
final class TrickCardMetrics {
    static final int PREVIOUS_CARD_HEIGHT_DP = 52;
    static final int CARD_HEIGHT_DP = PREVIOUS_CARD_HEIGHT_DP * 2;
    static final int BASE_CARD_WIDTH_DP = 44;
    static final int MIN_CARD_WIDTH_DP = 32;
    static final int MAX_CARD_WIDTH_DP = 88;
    static final int PLAYER_LABEL_HEIGHT_DP = 14;
    static final int ITEM_VERTICAL_PADDING_DP = 4;
    static final int ITEM_HORIZONTAL_INSET_DP = 6;

    record Slot(int seat, int row, int leftDp) { }

    record Layout(int columns, int rows, int cardWidthDp, int cardHeightDp,
            int playerLabelHeightDp, int centerWidthDp, int safeCenterWidthDp,
            int cellHeightDp, int centerHeightDp, List<Slot> slots) {
        Layout {
            slots = List.copyOf(slots);
        }

        Slot slotForSeat(int seat) {
            if (seat < 0 || seat >= slots.size()) {
                throw new IllegalArgumentException("Seat is outside the trick layout");
            }
            return slots.get(seat);
        }

        int cardFaceTextSizeSp() {
            return Math.max(13, Math.min(18, cardWidthDp / 4));
        }

        int playerLabelTextSizeSp() {
            return cardWidthDp < 48 ? 8 : 10;
        }
    }

    private TrickCardMetrics() { }

    static Layout forTable(int players, int tableWidthDp) {
        return forTable(players, tableWidthDp, 1f);
    }

    static Layout forTable(int players, int tableWidthDp, float fontScale) {
        if (players < 3 || players > 6) {
            throw new IllegalArgumentException("A trick requires 3–6 players");
        }
        if (tableWidthDp < 240) {
            throw new IllegalArgumentException("Table width must be at least 240 dp");
        }
        if (!Float.isFinite(fontScale) || fontScale <= 0) {
            throw new IllegalArgumentException("Font scale must be positive and finite");
        }

        int safeCenterWidth = TableLayoutMetrics.centerPlayWidthDp(tableWidthDp);
        int maxEdgeCardWidth = Math.max(MIN_CARD_WIDTH_DP,
                tableWidthDp / 3 - ITEM_HORIZONTAL_INSET_DP);
        int cardWidth = Math.min(MAX_CARD_WIDTH_DP, maxEdgeCardWidth);
        if (players == 4) {
            int maxSideCardWidth = Math.max(MIN_CARD_WIDTH_DP,
                    (safeCenterWidth - ITEM_HORIZONTAL_INSET_DP) / 2);
            cardWidth = Math.min(cardWidth, maxSideCardWidth);
        }

        int rows = players == 4 ? 3 : 2;
        int labelHeight = (int) Math.ceil(PLAYER_LABEL_HEIGHT_DP * fontScale);
        int cellHeight = CARD_HEIGHT_DP + labelHeight + ITEM_VERTICAL_PADDING_DP;
        List<Slot> slots = seatSlots(players, tableWidthDp, safeCenterWidth, cardWidth, rows);
        return new Layout(3, rows, cardWidth, CARD_HEIGHT_DP, labelHeight,
                tableWidthDp, safeCenterWidth, cellHeight, rows * cellHeight, slots);
    }

    private static List<Slot> seatSlots(int players, int width, int safeWidth,
            int cardWidth, int rows) {
        TableSeatLayout.Anchor[] anchors = TableSeatLayout.positions(players);
        List<Slot> slots = new ArrayList<>(java.util.Collections.nCopies(players, null));
        for (int row = 0; row < rows; row++) {
            List<Integer> seats = new ArrayList<>();
            for (int seat = 0; seat < anchors.length; seat++) {
                if (rowFor(anchors[seat], rows) == row) seats.add(seat);
            }
            seats.sort((first, second) -> Integer.compare(
                    horizontalOrder(anchors[first]), horizontalOrder(anchors[second])));
            for (int index = 0; index < seats.size(); index++) {
                int seat = seats.get(index);
                double center;
                if (players == 4 && row == 1) {
                    center = (width - safeWidth) / 2.0
                            + safeWidth * (index + 0.5) / seats.size();
                } else if (seats.size() == 1) {
                    center = width / 2.0;
                } else if (seats.size() == 2) {
                    center = width * (index + 1) / 3.0;
                } else {
                    center = width * (index * 2 + 1) / 6.0;
                }
                int left = (int) Math.round(center - cardWidth / 2.0);
                left = Math.max(0, Math.min(width - cardWidth, left));
                slots.set(seat, new Slot(seat, row, left));
            }
        }
        return List.copyOf(slots);
    }

    private static int rowFor(TableSeatLayout.Anchor anchor, int rows) {
        if (anchor == TableSeatLayout.Anchor.NORTH
                || anchor == TableSeatLayout.Anchor.NORTH_WEST
                || anchor == TableSeatLayout.Anchor.NORTH_EAST) return 0;
        if (anchor == TableSeatLayout.Anchor.WEST || anchor == TableSeatLayout.Anchor.EAST) return 1;
        return rows - 1;
    }

    private static int horizontalOrder(TableSeatLayout.Anchor anchor) {
        return switch (anchor) {
            case NORTH_WEST, WEST, SOUTH_WEST -> 0;
            case NORTH, SOUTH -> 1;
            case NORTH_EAST, EAST, SOUTH_EAST -> 2;
        };
    }
}
