package ro.whistlab.app.highscores;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.rules.GameMode;

/** Versioned file format shared by the app and offline component tests. */
final class HighScoreCodec {
    private HighScoreCodec() { }

    static String encode(HighScores scores) throws IOException {
        try {
            JSONArray boards = new JSONArray();
            for (var board : scores.boards.values()) {
                var profile = board.profile();
                JSONObject item = new JSONObject().put("mode", profile.mode().name())
                        .put("players", profile.players()).put("difficulty", profile.difficulty().name())
                        .put("series", profile.series()).put("reward", profile.reward()).put("penalty", profile.penalty())
                        .put("allowZeroGame", profile.allowZeroGame());
                JSONArray entries = new JSONArray();
                for (var entry : board.entries()) entries.put(new JSONObject().put("name", entry.name())
                        .put("score", entry.score()).put("ai", entry.ai()).put("sample", entry.sample())
                        .put("matchId", entry.matchId()).put("seat", entry.seat()).put("recordedAt", entry.recordedAt()));
                item.put("entries", entries).put("processedMatches", new JSONArray(board.processedMatches().stream().sorted().collect(java.util.stream.Collectors.toList())));
                boards.put(item);
            }
            return new JSONObject().put("schemaVersion", 2).put("capacity", HighScores.CAPACITY)
                    .put("profiles", boards).toString(2);
        } catch (JSONException error) { throw new IOException("Could not encode high scores", error); }
    }

    static HighScores decode(String encoded) throws IOException {
        try {
            JSONObject root = new JSONObject(encoded);
            int version = integer(root, "schemaVersion");
            if ((version != 1 && version != 2) || integer(root, "capacity") != HighScores.CAPACITY)
                throw new IllegalArgumentException("Unsupported high-score format");
            var result = new HighScores();
            JSONArray profiles = root.getJSONArray("profiles");
            for (int index = 0; index < profiles.length(); index++) readBoard(profiles.getJSONObject(index), result, version);
            return result;
        } catch (JSONException | IllegalArgumentException error) { throw new IOException("Invalid high-score file", error); }
    }

    private static void readBoard(JSONObject item, HighScores result, int version) throws JSONException {
        Object zeroGame = version == 1 ? Boolean.FALSE : item.get("allowZeroGame");
        if (!(zeroGame instanceof Boolean)) throw new JSONException("Boolean required: allowZeroGame");
        var profile = new HighScores.Profile(GameMode.valueOf(item.getString("mode")), integer(item, "players"),
                AiDifficulty.valueOf(item.getString("difficulty")), item.getBoolean("series"),
                integer(item, "reward"), integer(item, "penalty"), (Boolean) zeroGame);
        var entries = new ArrayList<HighScores.Entry>();
        JSONArray rows = item.getJSONArray("entries");
        for (int index = 0; index < rows.length(); index++) {
            JSONObject row = rows.getJSONObject(index);
            Object timestamp = row.get("recordedAt");
            if (!(timestamp instanceof Long) && !(timestamp instanceof Integer)) throw new JSONException("Integer timestamp required");
            entries.add(new HighScores.Entry(row.getString("name"), integer(row, "score"), row.getBoolean("ai"),
                    row.getBoolean("sample"), row.getString("matchId"), integer(row, "seat"), ((Number)timestamp).longValue()));
        }
        var processed = new LinkedHashSet<String>();
        JSONArray matches = item.getJSONArray("processedMatches");
        for (int index = 0; index < matches.length(); index++) {
            if (!processed.add(matches.getString(index))) throw new IllegalArgumentException("Duplicate processed match");
        }
        entries.sort(HighScores.ORDER);
        if (result.boards.put(profile.key(), new HighScores.Board(profile, entries, processed)) != null)
            throw new IllegalArgumentException("Duplicate profile");
    }

    private static int integer(JSONObject object, String key) throws JSONException {
        Object value = object.get(key);
        if (!(value instanceof Integer)) throw new JSONException("Integer required: " + key);
        return (Integer)value;
    }
}
