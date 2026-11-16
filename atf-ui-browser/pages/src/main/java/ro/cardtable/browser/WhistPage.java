package ro.cardtable.browser;

import org.json.JSONObject;
import org.teavm.jso.JSExport;
import ro.whistlab.app.achievements.WhistAchievementRepository.Storage;
import ro.whistlab.browser.WhistBrowser;

import java.util.HashMap;
import java.util.Random;

/** Entry point compiled to JavaScript and called by the page's worker. */
public final class WhistPage {
    private static final Value name = new Value();
    private static final Value progress = new Value();
    private static WhistBrowser game;
    private static BrowserSession session;

    private WhistPage() { }

    @JSExport
    public static void initialize(int highSeed, int lowSeed) {
        var random = new Random((long) highSeed << 32 | Integer.toUnsignedLong(lowSeed));
        game = new WhistBrowser(random::nextLong);
        session = new BrowserSession(WhistBrowser.localProfile(name, progress));
    }

    @JSExport
    public static String command(String path, String body, String savedName, String savedProgress) {
        name.value = savedName;
        progress.value = savedProgress;
        try {
            var input = new JSONObject(body);
            long revision = input.optLong("revision", -1);
            Object result = switch (path) {
                case "/api/setup" -> PageJson.fields("title", game.title(), "fields", game.fields());
                case "/api/state" -> session.state();
                case "/api/layout" -> BrowserGeometry.measure(parameters(input));
                case "/api/start" -> session.start(game, revision, parameters(input));
                case "/api/action" -> session.act(revision, input.optString("action"));
                case "/api/profile" -> session.rename(revision, input.optString("name"));
                case "/api/notice" -> session.dismissNotice(revision);
                default -> throw new IllegalArgumentException("Unsupported operation");
            };
            return PageJson.encode(PageJson.fields("result", result, "name", name.value, "progress", progress.value));
        } catch (BrowserSession.StaleRevision stale) {
            return PageJson.encode(PageJson.fields("status", 409, "error", "Masa a avansat. Am reîncărcat starea."));
        } catch (IllegalArgumentException invalid) {
            return PageJson.encode(PageJson.fields("status", 400, "error", invalid.getMessage()));
        } catch (RuntimeException failure) {
            failure.printStackTrace();
            return PageJson.encode(PageJson.fields("status", 500, "error", "Jocul nu a putut executa comanda."));
        }
    }

    private static HashMap<String, String> parameters(JSONObject input) {
        var values = new HashMap<String, String>();
        for (String key : input.keySet()) values.put(key, input.getString(key));
        return values;
    }

    private static final class Value implements Storage {
        private String value;
        @Override public String read() { return value; }
        @Override public void write(String next) { value = next; }
    }
}
