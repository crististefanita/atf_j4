package atf.android.evidence.component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** Invocation-owned fixture context, distinct from assertions; repeated identical inputs keep one canonical copy. */
public final class ComponentInputs {
    public record Entry(String name, String value) { }
    private final LinkedHashSet<Entry> entries = new LinkedHashSet<>();
    public void add(String name, String value) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Input name is required");
        entries.add(new Entry(name, Objects.requireNonNull(value)));
    }
    public List<Entry> snapshot() { return List.copyOf(entries); }
}
