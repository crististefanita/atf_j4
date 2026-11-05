package ro.whistlab.app.table;

import java.util.ArrayList;
import java.util.List;
import ro.whistlab.app.cards.CardFaceMetrics;

/** Stable, seat-oriented trick slots; the full number of rows is reserved before play. */
final class TrickCardMetrics {
    static final int CARD_HEIGHT_DP = 70;
    static final int SHORT_CARD_HEIGHT_DP = 64;
    static final int MIN_CARD_WIDTH_DP = CardFaceMetrics.atHeight(SHORT_CARD_HEIGHT_DP).widthDp();
    static final int MAX_CARD_WIDTH_DP = CardFaceMetrics.atHeight(CARD_HEIGHT_DP).widthDp();
    static final int PLAYER_LABEL_HEIGHT_DP = 14;
    static final int ITEM_VERTICAL_PADDING_DP = 4;

    record Slot(int seat, int row, int leftDp) { }

    record Layout(int columns, int rows, int cardWidthDp, int cardHeightDp,
            int playerLabelHeightDp, int itemVerticalPaddingDp, int centerWidthDp, int safeCenterWidthDp,
            int cellHeightDp, int rowStrideDp, int centerHeightDp, List<Slot> slots) {
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
        return forTable(players, tableWidthDp, fontScale, CARD_HEIGHT_DP);
    }

    static Layout forTable(int players, int tableWidthDp, float fontScale, int cardHeightDp) {
        return forTable(players, tableWidthDp, fontScale, cardHeightDp, ITEM_VERTICAL_PADDING_DP);
    }

    static Layout forTable(int players, int tableWidthDp, float fontScale, int cardHeightDp,
            int itemVerticalPaddingDp) {
        return forTable(players, tableWidthDp, fontScale, cardHeightDp, itemVerticalPaddingDp, false);
    }

    static Layout forTable(int players, int tableWidthDp, float fontScale, TableViewportPolicy viewport) {
        return forTable(players, tableWidthDp, fontScale, viewport.playedCardFaceHeightDp(),
                viewport.playedCardItemVerticalPaddingDp(), viewport.isCompact());
    }

    private static Layout forTable(int players, int tableWidthDp, float fontScale, int cardHeightDp,
            int itemVerticalPaddingDp, boolean compact) {
        if (players < 3 || players > 6) {
            throw new IllegalArgumentException("A trick requires 3–6 players");
        }
        if (tableWidthDp < 240) {
            throw new IllegalArgumentException("Table width must be at least 240 dp");
        }
        if (!Float.isFinite(fontScale) || fontScale <= 0) {
            throw new IllegalArgumentException("Font scale must be positive and finite");
        }
        if (cardHeightDp < SHORT_CARD_HEIGHT_DP || cardHeightDp > CARD_HEIGHT_DP) {
            throw new IllegalArgumentException("Played-card height must be between " + SHORT_CARD_HEIGHT_DP + " and " + CARD_HEIGHT_DP + " dp");
        }
        if (itemVerticalPaddingDp < 0 || itemVerticalPaddingDp > ITEM_VERTICAL_PADDING_DP) {
            throw new IllegalArgumentException("Played-card row padding must be between 0 and 4 dp");
        }

        int safeCenterWidth = tableWidthDp;
        int cardWidth = CardFaceMetrics.atHeight(cardHeightDp).widthDp();

        int rows = players == 4 ? 3 : 2;
        int labelHeight = compact ? 0 : (int) Math.ceil(PLAYER_LABEL_HEIGHT_DP * fontScale);
        int cellHeight = cardHeightDp + labelHeight + itemVerticalPaddingDp;
        int rowStride = compact && players == 4 ? (cellHeight + 3) / 2 : cellHeight;
        List<Slot> slots = seatSlots(players, tableWidthDp, cardWidth, rows);
        return new Layout(3, rows, cardWidth, cardHeightDp, labelHeight, itemVerticalPaddingDp,
                tableWidthDp, safeCenterWidth, cellHeight, rowStride,
                cellHeight + (rows - 1) * rowStride, slots);
    }

    private static List<Slot> seatSlots(int players, int width, int cardWidth, int rows) {
        TableSeatLayout.Anchor[] anchors = TableSeatLayout.positions(players);
        List<Slot> slots = new ArrayList<>(java.util.Collections.nCopies(players, null));
        for (int row = 0; row < rows; row++) {
            List<Integer> seats = new ArrayList<>();
            for (int seat = 0; seat < anchors.length; seat++) {
                int cardRow = players == 4 ? (seat == 0 ? 2 : seat == 2 ? 0 : 1) : rowFor(anchors[seat], rows);
                if (cardRow == row) seats.add(seat);
            }
            seats.sort((first, second) -> Integer.compare(
                    horizontalOrder(anchors[first]), horizontalOrder(anchors[second])));
            for (int index = 0; index < seats.size(); index++) {
                int seat = seats.get(index);
                double center;
                if (players == 4 && row == 1) {
                    center = width * (index == 0 ? 1.0 / 6 : 5.0 / 6);
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
