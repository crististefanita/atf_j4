package ro.cardtable.browser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ro.cardtable.ui.hand.HandInput;
import ro.cardtable.ui.table.TableGeometry;
import ro.cardtable.ui.table.TableSizing;
import ro.cardtable.ui.table.TableViewportPolicy;
import ro.whistlab.app.cards.WhistCardFace;
import ro.whistlab.app.table.TableLayoutMetrics;
import ro.whistlab.browser.presentation.WhistBoardAdapter;
import ro.whistlab.core.model.Card;
import ro.whistlab.core.model.Suit;
import ro.whistlab.core.rules.GameMode;

/** Exercises production ports with measured fixtures; it does not simulate Android View rendering. */
public final class WhistSurfaceContract {
    private static final BrowserGame WEB = new BrowserGame() {
        public String title() { return "Whist"; }
        public List<Field> fields() { return List.of(); }
        public Match start(Map<String, String> configuration) { throw new UnsupportedOperationException("Geometry only"); }
        @Override public BoardProfile boardProfile(String mode, int players, int width, int options,
                                                    TableViewportPolicy viewport, float fontScale) {
            return WhistBoardAdapter.profile(mode, players, width, options, viewport, fontScale);
        }
    };

    public static void main(String[] args) throws Exception {
        var results = new ArrayList<Map<String, Object>>();
        results.add(geometry());
        results.add(faces());
        results.add(feedback(Path.of(args[0])));
        var samples = ro.cardtable.ui.animation.TrickCollectionMotion.keyframes();
        var frames = samples.stream().map(sample -> ro.cardtable.ui.animation.TrickCollectionMotion.frame(
                new ro.cardtable.ui.animation.TrickCollectionMotion.Point(0, 0),
                new ro.cardtable.ui.animation.TrickCollectionMotion.Point(100, -60), sample.offset())).toList();
        results.add(Map.of("name", "Native collection curve", "status", "PASS", "samples", samples, "frames", frames));
        System.out.println(BrowserJson.encode(Map.of("status", "PASS", "cases", results,
                "boundary", "Portable adapters and measured inputs; native pixels and font rasterization are not executed")));
    }

    private static Map<String, Object> geometry() {
        int samples = 0;
        var examples = new ArrayList<Map<String, Object>>();
        for (var mode : GameMode.values()) for (int players = 3; players <= 6; players++)
            for (int width : new int[]{280, 320, 360, 414, 560, 760})
                for (float scale : new float[]{1f, 1.3f, 2f}) for (int cards : new int[]{1, 4, 8, 17})
                    for (int viewportHeight : new int[]{568, 896, 1280}) {
                    var values = new HashMap<String, String>(Map.of("mode", mode.name(), "players", "" + players,
                            "width", "" + width, "height", "" + Math.max(200, viewportHeight - 280), "seatHeight", "" + Math.round(50 * scale),
                            "viewport", "" + viewportHeight, "cards", "" + cards, "handWidth", "" + width,
                            "handIndexWidth", "" + Math.round(22 * scale), "fontScale", "" + scale));
                    values.put("bidOptions", "" + (cards + 1));
                    var bidding = BrowserGeometry.measure(WEB, values);
                    values.put("bidOptions", "0");
                    var playing = BrowserGeometry.measure(WEB, values);
                    equal(bidding.board().seats(), playing.board().seats(), "Phase-stable Web seat rectangles");
                    var viewport = new TableViewportPolicy(viewportHeight, scale);
                    var reservation = TableLayoutMetrics.matchCenterReservation(mode, players, width, scale, viewport);
                    var center = reservation.center(width, scale, false, 0);
                    for (float density : new float[]{1f, 1.5f, 2.75f, 3.5f}) {
                        var nativeCenter = new TableGeometry.CenterSpec(Math.round(center.width() * density),
                                Math.round(center.reservedHeight() * density), center.fullWidth(), center.alignment());
                        var nativeBoard = new TableGeometry.BoardSpec(players,
                                Math.round(TableSizing.seatWidthDp(width, viewport.isCompact()) * density),
                                Math.round(Math.round(50 * scale) * density), nativeCenter,
                                Math.round(viewport.regionGapDp() * density), Math.round(viewport.minimumBoardHeightDp() * density),
                                Math.round(TableLayoutMetrics.HUMAN_HAND_CLEARANCE_DP * density), true);
                        var nativePlan = nativeBoard.plan(Math.round(width * density),
                                Math.round(playing.boardHeight() * density), 0);
                        if (mode == GameMode.ROMANIAN_WHIST && players == 4 && width == 414 && cards == 4
                                && scale == 1f && density == 2.75f)
                            examples.add(Map.of("viewportHeight", viewportHeight, "web", playing.board(), "nativePixels", nativePlan,
                                    "density", density, "biddingSeats", bidding.board().seats()));
                        for (int seat = 0; seat < players; seat++) {
                            var expected = playing.board().seats().get(seat);
                            var observed = nativePlan.seats().get(seat);
                            close(expected.left(), observed.left() / density);
                            close(expected.top(), observed.top() / density);
                            close(expected.right(), observed.right() / density);
                            close(expected.bottom(), observed.bottom() / density);
                        }
                    }
                    var nativeHand = ro.whistlab.app.hand.HandLayoutMetrics.fan(cards, width, 1f, scale, Math.round(22 * scale));
                    equal(nativeHand.width(), playing.hand().width(), "Hand width");
                    equal(nativeHand.advance(), playing.hand().advance(), "Hand advance");
                    equal(nativeHand.cardWidth(), playing.hand().cardWidth(), "Hand card width");
                    samples++;
                }
        var invalid = new HashMap<>(Map.of("width", "414", "height", "600", "seatHeight", "60", "viewport", "896",
                "players", "4", "cards", "4", "handWidth", "414"));
        for (String value : List.of("NaN", "Infinity", "0", "-1", "4")) {
            invalid.put("fontScale", value);
            try { BrowserGeometry.measure(WEB, invalid); throw new AssertionError("Accepted invalid font scale"); }
            catch (IllegalArgumentException expected) { }
        }
        return Map.of("name", "Web reference and native geometry adapter", "samples", samples,
                "viewportHeights", List.of(568, 896, 1280), "widths", List.of(280, 320, 360, 414, 560, 760), "examples", examples,
                "densities", List.of(1, 1.5, 2.75, 3.5), "fontScales", List.of(1, 1.3, 2),
                "expected", "Equal logical regions, phase stability, complete fan and bounded physical rounding", "status", "PASS");
    }

    private static Map<String, Object> faces() {
        int count = 0;
        for (var suit : Suit.values()) for (int rank = 2; rank <= 14; rank++) {
            var card = new Card(suit, rank);
            var nativeFace = WhistCardFace.from(card);
            var web = new BrowserGame.Card(card.rankLabel(), suit.symbol(), card.shortLabel(), "play");
            equal(nativeFace, BrowserHandAdapter.cards(List.of(web)).getFirst().face(), "All 52 native/Web card marks");
            for (int height : new int[]{40, 62, 88}) {
                equal(ro.whistlab.app.cards.CardFaceMetrics.atHeight(height).widthDp(),
                        ro.cardtable.ui.cards.CardFaceMetrics.atHeight(height).widthDp(), "Role changes scale, not face proportion");
            }
            count++;
        }
        return Map.of("name", "Card-face adapter", "faces", count, "status", "PASS",
                "expected", "2–A and four suits retain rank, symbol, ink and proportions across roles");
    }

    private static Map<String, Object> feedback(Path scenario) throws Exception {
        var cards = List.of(new BrowserGame.Card("A", "♠", "First", "play:1"), new BrowserGame.Card("10", "♥", "Second", "play:2"),
                new BrowserGame.Card("2", "♦", "Wrong", ""), new BrowserGame.Card("J", "♣", "Other", ""));
        var aliases = Map.of("first", "A♠", "second", "10♥", "wrong", "2♦", "other", "J♣");
        var header = new BrowserGame.Header("ROMANIAN_WHIST", "Whist", "", "", "", "", 16, 12, 12);
        var view = new BrowserGame.View("", "", "", List.of(), cards, List.of(), List.of(), List.of(), BrowserGame.History.EMPTY, header, null);
        var nativeInput = new HandInput(BrowserHandAdapter.cards(cards), true);
        var state = new HandInput.State("", false, 0);
        int commands = 0;
        for (String line : Files.readAllLines(scenario)) {
            if (line.isBlank() || line.startsWith("#")) continue;
            var row = line.split("\\|", -1);
            String card = row[0].equals("tap") ? aliases.get(row[1]) : "";
            String action;
            if (row[0].equals("tap")) action = nativeInput.tap(card).name();
            else { nativeInput.clearHint(Long.parseLong(row[1])); action = "IGNORE"; }
            var fields = Map.of("operation", row[0], "card", card, "expires", row[0].equals("clear") ? row[1] : "-1",
                    "rejected", state.rejected(), "selected", "" + state.selected(), "hintRevision", "" + state.revision());
            var web = BrowserHandAdapter.apply(view, fields);
            equal(row[2], action, "Independent TSV action");equal(action, web.action(), "Same adapter command");
            equal(nativeInput.state().rejected(), web.rejected(), "Rejected face");
            equal(nativeInput.state().selected(), web.selected(), "Single accepted deposit");
            equal(nativeInput.state().revision(), web.revision(), "Stale callback protection");
            equal(nativeInput.suggestions(), web.suggestions(), "Legal-card highlights");
            state = new HandInput.State(web.rejected(), web.selected(), web.revision());commands++;
        }
        return Map.of("name", "Shared native/Web gesture fixture", "commands", commands, "status", "PASS",
                "expected", "Rejected taps, stale expiry, legal highlights and single deposit follow the same state machine");
    }

    private static void close(float expected, float actual) {
        if (Math.abs(expected - actual) > 2f) throw new AssertionError("Density conversion: " + expected + " != " + actual);
    }
    private static void equal(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) throw new AssertionError(label + ": " + expected + " != " + actual);
    }
}
