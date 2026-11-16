package ro.cardtable.browser;

import java.util.Map;
import ro.cardtable.ui.hand.HandLayoutMetrics;
import ro.cardtable.ui.table.TableGeometry;
import ro.cardtable.ui.table.TableSizing;
import ro.cardtable.ui.table.TableViewportPolicy;
import ro.cardtable.ui.table.PlayedCardMetrics;

/** The DOM supplies measurements. The same pure geometry lays out Android and browser tables. */
final class BrowserGeometry {
    private BrowserGeometry() { }

    record Layout(TableGeometry.Plan board, HandLayoutMetrics.Fan hand, int boardHeight,
                  PlayedCardMetrics.Layout played, BrowserGame.BidControls bids, Map<String, Integer> playedStyle) { }

    static Layout measure(BrowserGame game, Map<String, String> inputs) {
        int width = number(inputs, "width", 1, 1200);
        int height = number(inputs, "height", 1, 2400);
        int seatHeight = number(inputs, "seatHeight", 1, 240);
        int viewport = number(inputs, "viewport", 1, 2400);
        int players = number(inputs, "players", 2, 6);
        int cards = number(inputs, "cards", 0, 17);
        int handWidth = number(inputs, "handWidth", 1, 1200);
        int handIndexWidth = inputs.containsKey("handIndexWidth")
                ? number(inputs, "handIndexWidth", 1, HandLayoutMetrics.CARD_WIDTH_DP)
                : HandLayoutMetrics.DENSE_HAND_MIN_ADVANCE_DP;
        var policy = new TableViewportPolicy(viewport);
        int bidOptions = inputs.containsKey("bidOptions") ? number(inputs, "bidOptions", 0, 18) : 0;
        TableGeometry.Plan board = null;
        PlayedCardMetrics.Layout played = null;
        BrowserGame.BidControls bids = null;
        if (players >= 3 && width >= 240) {
            var profile = game.boardProfile(inputs.getOrDefault("mode", "ROMANIAN_WHIST"), players, width, bidOptions, policy);
            var center = profile.center();
            played = profile.playedCards();
            bids = profile.bids();
            var spec = new TableGeometry.BoardSpec(players, TableSizing.seatWidthDp(width, policy.isCompact()),
                    seatHeight, center, policy.regionGapDp(), policy.minimumBoardHeightDp(),
                    profile.handClearance(), true);
            height = Math.max(height, spec.requiredHeight(0));
            var plan = spec.plan(width, height, 0);
            int centerWidth = center.fullWidth() ? width : Math.min(width, center.width());
            board = new TableGeometry.Plan(plan.seats(), spec.content(plan, centerWidth, center.reservedHeight()));
        }
        var hand = cards == 0 ? null : HandLayoutMetrics.fan(cards, handWidth, 1, 1, handIndexWidth);
        var style = played == null ? Map.<String, Integer>of() : Map.of(
                "faceWidth", played.cardWidthDp(),
                "faceHeight", played.cardHeightDp(),
                "faceTextSp", played.cardFaceTextSizeSp(), "playerTextSp", played.playerLabelTextSizeSp(),
                "verticalPaddingDp", played.itemVerticalPaddingDp() / 2);
        return new Layout(board, hand, height, played, bids, style);
    }

    private static int number(Map<String, String> fields, String key, int minimum, int maximum) {
        int number = Integer.parseInt(fields.getOrDefault(key, ""));
        if (number < minimum || number > maximum) throw new IllegalArgumentException("Geometry input out of range");
        return number;
    }
}
