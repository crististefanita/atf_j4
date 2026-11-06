package ro.whistlab.app.persistence;

import org.json.JSONException;
import org.json.JSONObject;

/** Exact JSON schema numbers; domain validation must never receive a narrowed or truncated value. */
final class SavedNumbers {
    private SavedNumbers() { }

    static int integer(JSONObject object, String field) throws JSONException {
        Object value = object.get(field);
        if (!(value instanceof Integer)) throw new JSONException("Saved integer required for " + field);
        return (Integer) value;
    }

    static int optionalInteger(JSONObject object, String field, int fallback) throws JSONException {
        return object.has(field) ? integer(object, field) : fallback;
    }

    static long seed(JSONObject object) throws JSONException {
        Object value = object.get("seed");
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Long) return (Long) value;
        throw new JSONException("Saved integer required for seed");
    }
}
