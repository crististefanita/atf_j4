package ro.cardtable.ui.profile;

import android.text.InputFilter;
import android.text.Spanned;
import java.util.Objects;
import java.util.function.ToIntFunction;

/** Applies the game's length rule to a complete proposed name. */
public final class PlayerNameLengthFilter implements InputFilter {
    private final int maximumLength;
    private final ToIntFunction<String> length;

    public PlayerNameLengthFilter(int maximumLength, ToIntFunction<String> length) {
        if (maximumLength < 1) throw new IllegalArgumentException("The name limit must be positive");
        this.maximumLength = maximumLength;
        this.length = Objects.requireNonNull(length);
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        if (start == end) return null;

        String proposed = new StringBuilder()
                .append(dest, 0, dstart)
                .append(source, start, end)
                .append(dest, dend, dest.length())
                .toString();
        // Keep the original selection when the whole edit would exceed the game's limit.
        return length.applyAsInt(proposed) <= maximumLength ? null : dest.subSequence(dstart, dend);
    }
}
