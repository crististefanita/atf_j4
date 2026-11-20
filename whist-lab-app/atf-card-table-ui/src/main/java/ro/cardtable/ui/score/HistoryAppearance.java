package ro.cardtable.ui.score;

import java.util.Locale;
import java.util.Map;
import ro.cardtable.ui.table.ScoreGridStyle;

/** Per-table presentation shared by Android widgets and browser adapters. */
public record HistoryAppearance(boolean boldPlayerNames, int separatorDp, int separatorColor) {
    public static final HistoryAppearance LEGACY = new HistoryAppearance(true, 1, ScoreGridStyle.RULE);
    public static final HistoryAppearance REGULAR_GROUPS = new HistoryAppearance(false, 2, 0xff71846c);

    public HistoryAppearance {
        if (separatorDp < 1 || separatorDp > 4 || (separatorColor >>> 24) != 255)
            throw new IllegalArgumentException("History groups require a 1–4 dp opaque separator");
    }

    public Map<String, String> cssTokens() {
        return Map.of("--history-heading-weight", boldPlayerNames ? "700" : "400",
                "--history-divider-width", separatorDp + "px",
                "--history-divider-color", String.format(Locale.ROOT, "#%06x", separatorColor & 0xffffff));
    }
}
