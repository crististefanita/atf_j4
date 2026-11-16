package ro.whistlab.app.persistence;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;
import ro.whistlab.core.session.GameSession;

/** Versioned, synchronous checkpoint storage for resuming a local match after process death. */
public final class GameSessionStore {
    private static final String PREFERENCES = "whist_lab_game";
    private static final String SAVE_KEY = "current_session_v1";
    private GameSessionStore() { }

    public static void save(Context context, GameSession game) throws IOException {
        save(context, game, true);
    }

    public static void save(Context context, GameSession game, boolean rankingEligible) throws IOException {
        save(context, game, rankingEligible, Long.toUnsignedString(game.config().seed()));
    }

    public static void save(Context context, GameSession game, boolean rankingEligible, String matchId) throws IOException {
        try {
            JSONObject root = encode(game.saveState());
            root.put("rankingEligible", rankingEligible);
            root.put("matchId", matchId);
            boolean stored = preferences(context).edit().putString(SAVE_KEY, root.toString()).commit();
            if (!stored) throw new IOException("Android could not save the current game");
        } catch (JSONException failure) {
            throw new IOException("Could not encode the current game", failure);
        }
    }

    public static boolean rankingEligible(Context context) {
        try {
            String json = preferences(context).getString(SAVE_KEY, null);
            return json == null || new JSONObject(json).optBoolean("rankingEligible", true);
        } catch (JSONException invalid) { throw new IllegalStateException("Invalid saved eligibility", invalid); }
    }

    public static String matchId(Context context) {
        try {
            String encoded = preferences(context).getString(SAVE_KEY, null);
            String id = encoded == null ? java.util.UUID.randomUUID().toString()
                    : new JSONObject(encoded).optString("matchId", java.util.UUID.randomUUID().toString());
            if (id.isBlank() || id.length() > 128) throw new IllegalStateException("Invalid saved match identity");
            return id;
        } catch (JSONException invalid) { throw new IllegalStateException("Invalid saved match identity", invalid); }
    }

    public static GameSession load(Context context) {
        String encoded = preferences(context).getString(SAVE_KEY, null);
        if (encoded == null || encoded.isBlank()) return null;
        return fromJson(encoded);
    }

    public static GameSession fromJson(String encoded) {
        try {
            return GameCheckpointCodec.fromJson(encoded);
        } catch (GameCheckpointCodec.UnsupportedSavedMode unsupported) {
            throw new UnsupportedSavedMode(unsupported.mode(), unsupported);
        }
    }

    public static final class UnsupportedSavedMode extends IllegalStateException {
        public UnsupportedSavedMode(String mode) { super("Unsupported saved mode: " + mode); }
        private UnsupportedSavedMode(String mode, Throwable cause) {
            super("Unsupported saved mode: " + mode, cause);
        }
    }

    public static void clear(Context context) {
        preferences(context).edit().remove(SAVE_KEY).commit();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    public static JSONObject encode(GameSession.SaveState state) throws JSONException {
        return GameCheckpointCodec.encode(state);
    }
}
