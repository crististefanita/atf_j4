package ro.cardtable.ui.menu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Map;

/** Public menu intentions; renderers own popup lifetime and games own navigation. */
public final class TableMenuContent {
    public static final String HISTORY = "score-history";
    public record Entry(String id, String label) {
        public Entry {
            if (Objects.requireNonNull(id).isBlank() || Objects.requireNonNull(label).isBlank())
                throw new IllegalArgumentException("A menu entry needs an identity and label");
        }
    }
    public record Item(String id, String label, Runnable action) {
        public Item {
            if (Objects.requireNonNull(id).isBlank() || Objects.requireNonNull(label).isBlank())
                throw new IllegalArgumentException("A menu item needs a stable identity and a visible label");
            Objects.requireNonNull(action);
        }
    }

    public static List<Item> historyFirst(String label, Runnable history, List<Item> others) {
        var items = new ArrayList<Item>();
        items.add(new Item(HISTORY, label, history));
        items.addAll(List.copyOf(others));
        var ids = new HashSet<String>();
        for (var item : items) if (!ids.add(item.id()))
            throw new IllegalArgumentException("Duplicate table menu identity: " + item.id());
        return List.copyOf(items);
    }

    public static List<Item> bind(List<Entry> entries, Map<String, Runnable> actions) {
        var ids = new HashSet<String>();
        var items = new ArrayList<Item>();
        for (var entry : List.copyOf(entries)) {
            if (!ids.add(entry.id())) throw new IllegalArgumentException("Duplicate table menu identity: " + entry.id());
            Runnable action = actions.get(entry.id());
            if (action == null) throw new IllegalArgumentException("Missing table menu action: " + entry.id());
            items.add(new Item(entry.id(), entry.label(), action));
        }
        return List.copyOf(items);
    }

    private TableMenuContent() { }
}
