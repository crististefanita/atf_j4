package ro.cardtable.ui.profile;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/** Routes editing once; the product owns normalization, persistence and the original exception. */
public final class NameEdit {
    private final UnaryOperator<String> normalize;
    private final Consumer<String> save;
    public NameEdit(UnaryOperator<String> normalize, Consumer<String> save) {
        this.normalize=Objects.requireNonNull(normalize); this.save=Objects.requireNonNull(save);
    }
    public void save(String value) { save.accept(normalize.apply(value)); }
    public void reset() { save.accept(""); }
}
