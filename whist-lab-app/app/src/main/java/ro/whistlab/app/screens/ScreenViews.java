package ro.whistlab.app.screens;

import android.app.Activity;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.app.window.WindowContent;

/** Whist text and window policy adapt the unchanged shared widget factory. */
public final class ScreenViews extends ro.cardtable.ui.screens.ScreenViews {
    public ScreenViews(Activity activity, Supplier<WhistUiText> language) {
        super(activity, labels(language), WindowContent::protect);
    }
    private static Function<String, String> labels(Supplier<WhistUiText> language) {
        Objects.requireNonNull(language);
        return key -> language.get().spinnerLabel(key);
    }
}
