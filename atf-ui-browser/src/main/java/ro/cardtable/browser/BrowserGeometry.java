package ro.cardtable.browser;

import java.util.Map;
import ro.cardtable.ui.hand.HandLayoutMetrics;
import ro.cardtable.ui.table.TableGeometry;
import ro.cardtable.ui.table.TableSizing;
import ro.cardtable.ui.table.TableViewportPolicy;

/** The DOM supplies measurements. The same pure geometry lays out Android and browser tables. */
final class BrowserGeometry {
    private BrowserGeometry() { }

    record Layout(TableGeometry.Plan board, HandLayoutMetrics.Fan hand) { }

    static Layout measure(Map<String, String> inputs) {
        int width = number(inputs, "width", 1, 1200);
        int height = number(inputs, "height", 1, 2400);
        int seatHeight = number(inputs, "seatHeight", 1, 240);
        int viewport = number(inputs, "viewport", 1, 2400);
        int players = number(inputs, "players", 2, 6);
        int cards = number(inputs, "cards", 0, 17);
        int handWidth = number(inputs, "handWidth", 1, 1200);
        var policy = new TableViewportPolicy(viewport);
        int gap = policy.regionGapDp();
        int center = 188;
        var board = players < 3 || width < 240 || height < TableGeometry.minimumHeight(players, seatHeight, center, gap)
                ? null : TableGeometry.arrangeDisplay(width, height, players,
                        TableSizing.seatWidthDp(width, policy.isCompact()), seatHeight, width,
                        center, gap, true, TableGeometry.CenterAlignment.BALANCED, 20);
        var hand = cards == 0 ? null : HandLayoutMetrics.fan(cards, handWidth, 1, 1,
                HandLayoutMetrics.DENSE_HAND_MIN_ADVANCE_DP);
        return new Layout(board, hand);
    }

    private static int number(Map<String, String> fields, String key, int minimum, int maximum) {
        int number = Integer.parseInt(fields.getOrDefault(key, ""));
        if (number < minimum || number > maximum) throw new IllegalArgumentException("Geometry input out of range");
        return number;
    }
}
