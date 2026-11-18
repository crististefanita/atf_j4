package ro.cardtable.browser;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import ro.cardtable.ui.hand.HandInput;
import ro.cardtable.ui.cards.CardFaceMetrics;
import ro.cardtable.ui.hand.HandLayoutMetrics;
import ro.cardtable.ui.table.TableSeatLayout;
import ro.cardtable.ui.table.TableGeometry;
import ro.cardtable.ui.table.TableViewportPolicy;
import ro.cardtable.ui.table.PlayedCardMetrics;

/** A game exposes the human's cards and the public table. Commands are opaque to the web shell. */
public interface BrowserGame {
    String title();
    default String surface() { return "table"; }
    default List<Token> palette() { return List.of(); }
    default BoardProfile boardProfile(String mode, int players, int width, int bidOptions,
            TableViewportPolicy viewport, float fontScale) {
        return boardProfile(mode, players, width, bidOptions, viewport);
    }
    default BoardProfile boardProfile(String mode, int players, int width, int bidOptions,
            TableViewportPolicy viewport) {
        var center = new TableGeometry.CenterSpec(width, 188, true, TableGeometry.CenterAlignment.BALANCED);
        return new BoardProfile(center, 20, null);
    }
    List<Field> fields();
    default List<Field> fields(String language) { return fields(); }
    default Map<String, Object> interfaceCopy(String language) { return Map.of(); }
    default int autoDelayMillis() { return 0; }
    default Map<String, Object> setup(List<?> collectionSamples) {
        return Map.of("title", title(), "fields", fields(), "englishFields", fields("en"),
                "surface", surface(), "palette", palette(), "collectionSamples", collectionSamples,
                "copy", Map.of("ro", interfaceCopy("ro"), "en", interfaceCopy("en")),
                "autoDelayMillis", autoDelayMillis(),
                "cardFace", Map.of("rankScale", CardFaceMetrics.RANK_SCALE),
                "handMotion", Map.of("hintMillis", HandInput.HINT_DURATION_MILLIS,
                        "liftMillis", HandInput.PLAY_LIFT_MILLIS, "liftDp", HandLayoutMetrics.CARD_LIFT_DP,
                        "liftScale", HandLayoutMetrics.CARD_LIFT_SCALE,
                        "restLayer", HandLayoutMetrics.NEIGHBOR_Z_DP, "liftLayer", HandLayoutMetrics.SELECTED_Z_DP,
                        "progress", java.util.stream.IntStream.rangeClosed(0, 10)
                                .mapToObj(step -> HandLayoutMetrics.liftProgress(step / 10f)).toList()));
    }
    Match start(Map<String, String> configuration);
    default boolean hasProfile() { return false; }
    default BrowserExperience openProfile(String client) { return null; }

    interface Match {
        View view();
        void act(String action);
        default void setLanguage(String language) { }
        default List<CollectionCue> collections() { return List.of(); }
        default List<PlayStep> playback() { return List.of(); }
    }

    record Option(String value, String label) { }
    record Token(String name, String color) { }

    record Field(String key, String label, String initial, List<Option> options) {
        public Field { options = List.copyOf(options); }
    }

    record Action(String key, String label, boolean emphasized, Integer minimum, Integer maximum) {
        public Action(String key, String label) { this(key, label, false); }
        public Action(String key, String label, boolean emphasized) { this(key, label, emphasized, null, null); }

        boolean accepts(String command) {
            if (minimum == null) return key.equals(command);
            if (!command.startsWith(key + "/")) return false;
            try {
                int amount = Integer.parseInt(command.substring(key.length() + 1));
                return amount >= minimum && amount <= maximum;
            } catch (NumberFormatException invalid) {
                return false;
            }
        }
    }

    record Seat(String anchor, String name, String firstFact, String secondFact, boolean active) {
        public static Seat at(int seat, int count, String name, String first, String second, boolean active) {
            String anchor = count == 2 ? (seat == 0 ? "south" : "north")
                    : TableSeatLayout.positions(count)[seat].name().toLowerCase(Locale.ROOT).replace('_', '-');
            return new Seat(anchor, name, first, second, active);
        }
    }

    record Card(String rank, String suit, String label, String action) {
        public Card(String rank, String suit, String label) { this(rank, suit, label, ""); }
    }

    record Played(int seat, Card card, String faceLabel, String description) {
        public Played(int seat, Card card) { this(seat, card, null, null); }
    }
    record PlayStep(Played play, boolean startsTrick, long delayMillis, CollectionCue collection,
                    String bids, List<String> seatFacts) { }

    record CollectionCue(int winningSeat, long displayMillis, long travelMillis, List<Played> cards, long holdMillis,
                         List<String> seatFacts) {
        public CollectionCue(int winningSeat, long displayMillis, long travelMillis, List<Played> cards) {
            this(winningSeat, displayMillis, travelMillis, cards, displayMillis - travelMillis);
        }
        public CollectionCue(int winningSeat, long displayMillis, long travelMillis, List<Played> cards, long holdMillis) {
            this(winningSeat, displayMillis, travelMillis, cards, holdMillis, List.of());
        }
        public CollectionCue {
            cards = List.copyOf(cards);
            seatFacts = List.copyOf(seatFacts);
        }
    }

    record HistoryCell(String bid, String total, String description, boolean missed, int series) { }
    record HistoryRow(int cards, String status, List<HistoryCell> cells) { }
    record HistoryGrid(List<String> players, List<HistoryRow> rows, String cardsLabel,
                       String bidLabel, String pointsLabel, String legend) { }

    record History(List<String> headings, List<List<String>> rows, HistoryGrid grid) {
        public History(List<String> headings, List<List<String>> rows) {
            this(headings, rows, null);
        }
        public static final History EMPTY = new History(List.of(), List.of());

        public History {
            headings = List.copyOf(headings);
            rows = rows.stream().map(List::copyOf).toList();
        }
    }

    record BidControls(int columns, boolean strip, int choiceTarget, int rowHeight, int choiceInset,
                       int submitWidth, int submitTarget, int submitInsetX, int submitInsetY, int textSize) { }

    record BoardProfile(TableGeometry.CenterSpec center, int handClearance, PlayedCardMetrics.Layout playedCards,
                        BidControls bids) {
        public BoardProfile(TableGeometry.CenterSpec center, int handClearance, PlayedCardMetrics.Layout playedCards) {
            this(center, handClearance, playedCards, null);
        }
    }

    record Header(String mode, String title, String progress, String bids, String trump, String trumpDescription,
                  float titleSize, float progressSize, float bidSize) { }

    record Score(String title, History table) { }

    record ScoreDetails(String liveTitle, String summary, String pendingBid, String winner, String awards,
                        List<String> rules, History liveTable) { }

    record View(String heading, String subtitle, String prompt, List<Seat> seats, List<Card> hand,
                List<Played> played, List<Card> center, List<Action> actions, History history, Header header, Score score, ScoreDetails scoreDetails) {
        public View(String heading, String subtitle, String prompt, List<Seat> seats, List<Card> hand,
                List<Played> played, List<Card> center, List<Action> actions, History history, Header header, Score score) {
            this(heading, subtitle, prompt, seats, hand, played, center, actions, history, header, score, null);
        }

        public View(String heading, String subtitle, String prompt, List<Seat> seats, List<Card> hand,
                List<Played> played, List<Card> center, List<Action> actions, History history) {
            this(heading, subtitle, prompt, seats, hand, played, center, actions, history, null, null);
        }

        public View {
            seats = List.copyOf(seats);
            hand = List.copyOf(hand);
            played = List.copyOf(played);
            center = List.copyOf(center);
            actions = List.copyOf(actions);
        }

        boolean accepts(String key) {
            return !key.isEmpty() && (actions.stream().anyMatch(action -> action.accepts(key))
                    || hand.stream().anyMatch(card -> key.equals(card.action())));
        }
    }
}
