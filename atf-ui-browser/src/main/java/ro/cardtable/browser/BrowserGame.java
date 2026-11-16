package ro.cardtable.browser;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import ro.cardtable.ui.table.TableSeatLayout;
import ro.cardtable.ui.table.TableViewportPolicy;
import ro.cardtable.ui.table.PlayedCardMetrics;

/** A game exposes the human's cards and the public table. Commands are opaque to the web shell. */
public interface BrowserGame {
    String title();
    default String surface() { return "table"; }
    default List<Token> palette() { return List.of(); }
    default BoardProfile boardProfile(String mode, int players, int width, TableViewportPolicy viewport) {
        return new BoardProfile(188, 20, null);
    }
    List<Field> fields();
    Match start(Map<String, String> configuration);
    default boolean hasProfile() { return false; }
    default BrowserExperience openProfile(String client) { return null; }

    interface Match {
        View view();
        void act(String action);
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

    record Played(int seat, Card card) { }
    record PlayStep(Played play, boolean startsTrick, long delayMillis, CollectionCue collection,
                    String bids, List<String> seatFacts) { }

    record CollectionCue(int winningSeat, long displayMillis, long travelMillis, List<Played> cards) {
        public CollectionCue { cards = List.copyOf(cards); }
    }

    record History(List<String> headings, List<List<String>> rows) {
        public static final History EMPTY = new History(List.of(), List.of());

        public History {
            headings = List.copyOf(headings);
            rows = rows.stream().map(List::copyOf).toList();
        }
    }

    record BoardProfile(int centerHeight, int handClearance, PlayedCardMetrics.Layout playedCards) { }

    record Header(String mode, String title, String progress, String bids, String trump, String trumpDescription,
                  float titleSize, float progressSize, float bidSize) { }

    record Score(String title, History table) { }

    record View(String heading, String subtitle, String prompt, List<Seat> seats, List<Card> hand,
                List<Played> played, List<Card> center, List<Action> actions, History history, Header header, Score score) {
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
