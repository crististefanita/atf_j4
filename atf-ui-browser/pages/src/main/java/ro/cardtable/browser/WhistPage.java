package ro.cardtable.browser;

import ro.cardtable.ui.animation.TrickCollectionMotion;

import org.json.JSONException;
import org.json.JSONObject;
import org.teavm.jso.JSExport;
import ro.whistlab.app.achievements.WhistAchievementRepository.Storage;
import ro.whistlab.browser.WhistBrowser;

import java.util.HashMap;
import java.util.Objects;
import java.util.Random;

/** Entry point compiled to JavaScript and called by the page's worker. */
public final class WhistPage {
    private static final Value name = new Value();
    private static final Value progress = new Value();
    private static final Value rankings = new Value();
    private static String checkpoint;
    private static WhistBrowser game;
    private static BrowserSession session;

    private WhistPage() { }

    @JSExport
    public static void initialize(int highSeed, int lowSeed) {
        var random = new Random((long) highSeed << 32 | Integer.toUnsignedLong(lowSeed));
        game = new WhistBrowser(random::nextLong);
        session = new BrowserSession(WhistBrowser.localProfile(name, progress, rankings));
        checkpoint = null;
        rankings.value = null;
    }

    @JSExport
    public static String command(String path, String body, String savedName, String savedProgress) {
        return execute(path, body, savedName, savedProgress, null, rankings.value, false);
    }

    @JSExport
    public static String commandLocal(String path, String body, String savedName, String savedProgress,
                                      String savedCheckpoint, String savedRankings) {
        return execute(path, body, savedName, savedProgress, savedCheckpoint, savedRankings, true);
    }

    private static String execute(String path, String body, String savedName, String savedProgress,
                                  String savedCheckpoint, String savedRankings, boolean persist) {
        name.value = savedName;
        progress.value = savedProgress;
        rankings.value = savedRankings;
        boolean gameCommand = !path.equals("/api/setup") && !path.equals("/api/layout");
        boolean rejected = persist && gameCommand && synchronize(savedCheckpoint);
        try {
            var input = new JSONObject(body);
            long revision = input.optLong("revision", -1);
            Object result = switch (path) {
                case "/api/setup" -> game.setup(TrickCollectionMotion.keyframes());
                case "/api/state" -> session.state();
                case "/api/rankings" -> session.rankings();
                case "/api/layout" -> BrowserGeometry.measure(game, parameters(input));
                case "/api/start" -> session.start(game, revision, parameters(input));
                case "/api/action" -> session.act(revision, input.optString("action"));
                case "/api/profile" -> session.rename(revision, input.optString("name"));
                case "/api/language" -> session.language(revision, input.optString("language"));
                case "/api/notice" -> session.dismissNotice(revision);
                default -> throw new IllegalArgumentException("Unsupported operation");
            };
            if (persist && (path.equals("/api/start") || path.equals("/api/action") || path.equals("/api/profile"))) {
                var match = session.match();
                checkpoint = match == null ? null : game.checkpoint(match);
            }
            return PageJson.encode(PageJson.fields("result", result, "name", name.value, "progress", progress.value,
                    "rankings", rankings.value, "checkpoint", gameCommand && persist ? checkpoint : savedCheckpoint,
                    "rejectedCheckpoint", rejected));
        } catch (BrowserSession.StaleRevision stale) {
            return PageJson.encode(PageJson.fields("status", 409, "error", "Masa a avansat. Am reîncărcat starea."));
        } catch (IllegalArgumentException invalid) {
            return PageJson.encode(PageJson.fields("status", 400, "error", invalid.getMessage()));
        } catch (RuntimeException failure) {
            if (persist && gameCommand) {
                session.restore(checkpoint == null ? null : game.restore(checkpoint));
            }
            failure.printStackTrace();
            return PageJson.encode(PageJson.fields("status", 500, "error", "Jocul nu a putut executa comanda."));
        }
    }

    private static boolean synchronize(String saved) {
        if (Objects.equals(checkpoint, saved)) return false;
        try {
            session.restore(saved == null ? null : game.restore(saved));
            checkpoint = saved;
            return false;
        } catch (JSONException | IllegalArgumentException | IllegalStateException invalid) {
            session.restore(null);
            checkpoint = null;
            return true;
        }
    }

    private static HashMap<String, String> parameters(JSONObject input) {
        var values = new HashMap<String, String>();
        for (String key : input.keySet()) values.put(key, input.getString(key));
        return values;
    }

    private static final class Value implements Storage, ro.whistlab.app.highscores.HighScoreRepository.Storage {
        private String value;
        @Override public String read() { return value; }
        @Override public void write(String next) { value = next; }
    }
}
