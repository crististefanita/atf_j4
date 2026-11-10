package ro.whistlab.app.achievements;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.IOException;
import java.util.Objects;

/** Stocare locală instalării; redenumirea jucătorului nu șterge progresul. */
public final class AndroidWhistAchievementStorage implements WhistAchievementRepository.Storage {
    private static final String PREFERENCES = "whist_lab_achievements";
    private static final String STATE = "catalog_v1";
    private final SharedPreferences preferences;

    public AndroidWhistAchievementStorage(Context context) {
        preferences = Objects.requireNonNull(context).getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    @Override public String read() { return preferences.getString(STATE, null); }

    @Override public void write(String json) throws IOException {
        if (json == null || !preferences.edit().putString(STATE, json).commit())
            throw new IOException("Android could not save Whist achievements");
    }
}
