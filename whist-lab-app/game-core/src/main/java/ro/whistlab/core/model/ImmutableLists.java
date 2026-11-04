package ro.whistlab.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable snapshots preserve null placeholders used by incomplete public game state. */
public final class ImmutableLists {
    private ImmutableLists() { }
    public static <T> List<T> copy(List<T> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
