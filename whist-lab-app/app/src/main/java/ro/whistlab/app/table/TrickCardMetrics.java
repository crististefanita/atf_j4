package ro.whistlab.app.table;


import java.util.ArrayList;
import java.util.List;
import ro.whistlab.app.cards.CardFaceMetrics;

/** Stable, seat-oriented trick slots; the full number of rows is reserved before play. */
final class TrickCardMetrics {
    static final int CARD_HEIGHT_DP = ro.cardtable.ui.table.PlayedCardMetrics.CARD_HEIGHT_DP;
    static final int SHORT_CARD_HEIGHT_DP = ro.cardtable.ui.table.PlayedCardMetrics.SHORT_CARD_HEIGHT_DP;
    static final int MIN_CARD_WIDTH_DP = ro.cardtable.ui.table.PlayedCardMetrics.MIN_CARD_WIDTH_DP;
    static final int MAX_CARD_WIDTH_DP = ro.cardtable.ui.table.PlayedCardMetrics.MAX_CARD_WIDTH_DP;
    static final int PLAYER_LABEL_HEIGHT_DP = ro.cardtable.ui.table.PlayedCardMetrics.PLAYER_LABEL_HEIGHT_DP;
    static final int ITEM_VERTICAL_PADDING_DP = ro.cardtable.ui.table.PlayedCardMetrics.ITEM_VERTICAL_PADDING_DP;

    record Slot(int seat, int row, int leftDp) { }

    record Layout(int columns, int rows, int cardWidthDp, int cardHeightDp,
            int playerLabelHeightDp, int itemVerticalPaddingDp, int centerWidthDp, int safeCenterWidthDp,
            int cellHeightDp, int rowStrideDp, int centerHeightDp, List<Slot> slots) {
        Layout {
            slots = List.copyOf(slots);
        }

        ro.cardtable.ui.table.PlayedCardMetrics.Layout shared() {
            return new ro.cardtable.ui.table.PlayedCardMetrics.Layout(columns, rows, cardWidthDp, cardHeightDp,
                    playerLabelHeightDp, itemVerticalPaddingDp, centerWidthDp, safeCenterWidthDp,
                    cellHeightDp, rowStrideDp, centerHeightDp, slots.stream()
                            .map(s -> new ro.cardtable.ui.table.PlayedCardMetrics.Slot(s.seat(), s.row(), s.leftDp()))
                            .collect(java.util.stream.Collectors.toList()));
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
    private static Layout local(ro.cardtable.ui.table.PlayedCardMetrics.Layout p) {
        return new Layout(p.columns(), p.rows(), p.cardWidthDp(), p.cardHeightDp(), p.playerLabelHeightDp(),
                p.itemVerticalPaddingDp(), p.centerWidthDp(), p.safeCenterWidthDp(), p.cellHeightDp(),
                p.rowStrideDp(), p.centerHeightDp(), p.slots().stream().map(s -> new Slot(s.seat(), s.row(), s.leftDp()))
                        .collect(java.util.stream.Collectors.toList()));
    }
    static Layout forTable(int players, int tableWidthDp) {
        return local(ro.cardtable.ui.table.PlayedCardMetrics.forTable(players, tableWidthDp));
    }
    static Layout forTable(int players, int tableWidthDp, float fontScale) {
        return local(ro.cardtable.ui.table.PlayedCardMetrics.forTable(players, tableWidthDp, fontScale));
    }
    static Layout forTable(int players, int tableWidthDp, float fontScale, int cardHeightDp) {
        return local(ro.cardtable.ui.table.PlayedCardMetrics.forTable(players, tableWidthDp, fontScale, cardHeightDp));
    }
    static Layout forTable(int players, int tableWidthDp, float fontScale, int cardHeightDp, int paddingDp) {
        return local(ro.cardtable.ui.table.PlayedCardMetrics.forTable(players, tableWidthDp, fontScale, cardHeightDp, paddingDp));
    }
    static Layout forTable(int players, int tableWidthDp, float fontScale, TableViewportPolicy viewport) {
        return local(ro.cardtable.ui.table.PlayedCardMetrics.forTable(players, tableWidthDp, fontScale, viewport));
    }
}
