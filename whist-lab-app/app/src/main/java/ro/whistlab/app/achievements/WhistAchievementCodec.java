package ro.whistlab.app.achievements;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Codec versionat pentru progres, evenimente și notificări locale, independent de numele jucătorului. */
final class WhistAchievementCodec {
    private static final int VERSION = 2;
    private WhistAchievementCodec() { }

    static String encode(WhistAchievementState state) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("version", VERSION);
        JSONObject progress = new JSONObject();
        state.progress().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
                .forEach(entry -> progressPut(progress, entry.getKey(), entry.getValue()));
        root.put("progress", progress);
        JSONArray processed = new JSONArray();
        state.processedMatches().forEach(processed::put);
        root.put("processedMatches", processed);
        JSONArray processedHands = new JSONArray();
        state.processedHands().forEach(processedHands::put);
        root.put("processedHands", processedHands);
        JSONArray pending = new JSONArray();
        state.pendingNotifications().forEach(pending::put);
        root.put("pendingNotifications", pending);
        return root.toString();
    }

    private static void progressPut(JSONObject target, String id, int value) {
        try { target.put(id, value); }
        catch (JSONException failure) { throw new IllegalStateException("Could not encode achievement progress", failure); }
    }

    static WhistAchievementState decode(String encoded) throws JSONException {
        JSONObject root = new JSONObject(encoded);
        int version = integer(root, "version");
        if (version < 1 || version > VERSION) throw new JSONException("Unsupported achievement state version: " + version);
        JSONObject encodedProgress = root.getJSONObject("progress");
        var progress = new LinkedHashMap<String, Integer>();
        var keys = encodedProgress.keys();
        while (keys.hasNext()) {
            String id = keys.next();
            WhistAchievementCatalog.goal(id);
            int value = integer(encodedProgress, id);
            if (value < 0) throw new JSONException("Negative progress: " + id);
            if (value > 0) progress.put(id, value);
        }
        var processed = readIds(root.getJSONArray("processedMatches"), 128, "match");
        var processedHands = version == 1 ? new LinkedHashSet<String>()
                : readIds(root.getJSONArray("processedHands"), 256, "hand");
        var pending = version == 1 ? new LinkedHashSet<String>()
                : readAchievementIds(root.getJSONArray("pendingNotifications"));
        return new WhistAchievementState(progress, processed, processedHands, pending);
    }

    private static LinkedHashSet<String> readIds(JSONArray values, int maxLength, String kind) throws JSONException {
        var ids = new LinkedHashSet<String>();
        for (int index = 0; index < values.length(); index++) {
            Object value = values.get(index);
            if (!(value instanceof String id) || id.isBlank() || id.length() > maxLength || !ids.add(id))
                throw new JSONException("Invalid processed " + kind + " identity");
        }
        return ids;
    }

    private static LinkedHashSet<String> readAchievementIds(JSONArray values) throws JSONException {
        var ids = new LinkedHashSet<String>();
        for (int index = 0; index < values.length(); index++) {
            Object value = values.get(index);
            if (!(value instanceof String id) || !ids.add(id))
                throw new JSONException("Invalid pending achievement identity");
            try { WhistAchievementCatalog.goal(id); }
            catch (IllegalArgumentException failure) { throw new JSONException(failure); }
        }
        return ids;
    }

    private static int integer(JSONObject source, String key) throws JSONException {
        Object value = source.get(key);
        if (!(value instanceof Integer number)) throw new JSONException("Integer required: " + key);
        return number;
    }
}
