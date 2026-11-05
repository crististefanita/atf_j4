package ro.whistlab.app.profile;

import android.content.Context;
import android.content.SharedPreferences;
import ro.whistlab.core.player.PlayerNames;

/** Local display preferences are independent of match rules and historical ranking names. */
public final class PlayerPreferences {
    private final SharedPreferences preferences;
    public PlayerPreferences(Context context) {
        preferences = context.getSharedPreferences("whist_lab_profile", Context.MODE_PRIVATE);
    }
    public String name() { return preferences.getString("name", ""); }
    public String sessionName() { return name().isBlank() ? PlayerNames.DEFAULT_HUMAN : name(); }
    public void saveName(String value) {
        String name = value.isBlank() ? "" : PlayerNames.normalize(value);
        if (!preferences.edit().putString("name", name).commit()) throw new IllegalStateException("Profile could not be saved");
    }
    public int rankingLimit() { return preferences.getInt("rankingLimit", 20) == 10 ? 10 : 20; }
    public void rankingLimit(int value) {
        if (value != 10 && value != 20) throw new IllegalArgumentException("Expected top 10 or top 20");
        if (!preferences.edit().putInt("rankingLimit", value).commit()) throw new IllegalStateException("Preference could not be saved");
    }
}
