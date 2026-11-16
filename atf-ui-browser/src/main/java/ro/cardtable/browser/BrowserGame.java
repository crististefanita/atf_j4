package ro.cardtable.browser;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import ro.cardtable.ui.table.TableSeatLayout;

/** A game exposes the human's cards and the public table. Commands are opaque to the web shell. */
public interface BrowserGame {
    String title();
    List<Field> fields();
    Match start(Map<String, String> configuration);

    interface Match {
        View view();
        void act(String action);
    }

    record Option(String value, String label) { }

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

    record History(List<String> headings, List<List<String>> rows) {
        public static final History EMPTY = new History(List.of(), List.of());

        public History {
            headings = List.copyOf(headings);
            rows = rows.stream().map(List::copyOf).toList();
        }
    }

    record View(String heading, String subtitle, String prompt, List<Seat> seats, List<Card> hand,
                List<Played> played, List<Card> center, List<Action> actions, History history) {
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
