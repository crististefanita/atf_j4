package ro.cardtable.browser;

import ro.cardtable.ui.animation.TrickCollectionMotion;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/** Same-origin HTTP host for a local tabletop session. No browser receives the engine's hidden state. */
public final class BrowserServer implements AutoCloseable {
    private static final int MAX_TABS = 32;
    private static final int MAX_FORM = 4096;
    private static final Map<String, Asset> ASSETS = Map.of(
            "/", new Asset("index.html", "text/html; charset=utf-8"),
            "/table.css", new Asset("table.css", "text/css; charset=utf-8"),
            "/table.mjs", new Asset("table.mjs", "text/javascript; charset=utf-8"),
            "/viewport.mjs", new Asset("viewport.mjs", "text/javascript; charset=utf-8"),
            "/profile.mjs", new Asset("profile.mjs", "text/javascript; charset=utf-8"),
            "/auto-game.mjs", new Asset("auto-game.mjs", "text/javascript; charset=utf-8"),
            "/records.mjs", new Asset("records.mjs", "text/javascript; charset=utf-8"),
            "/hand-feedback.mjs", new Asset("hand-feedback.mjs", "text/javascript; charset=utf-8"),
            "/collection.mjs", new Asset("collection.mjs", "text/javascript; charset=utf-8"),
            "/card-suits.woff2", new Asset("card-suits.woff2", "font/woff2"));
    private static final Asset MENU_FOOTER = new Asset("menu-footer.png", "image/png");
    private final Map<String, Asset> routes = new HashMap<>(ASSETS);
    private final HttpServer server;
    private final java.util.concurrent.ExecutorService tasks = Executors.newFixedThreadPool(4);
    private final String access;
    private final BrowserGame game;
    private final Map<String, byte[]> assets = new HashMap<>();
    private final Map<Client, BrowserSession> sessions = new HashMap<>();
    private final Map<String, BrowserExperience> profiles = new HashMap<>();

    public BrowserServer(BrowserGame game, Path assets, InetSocketAddress address) throws IOException {
        this(game, assets, address, null);
    }

    private BrowserServer(BrowserGame game, Path assets, InetSocketAddress address, Path menuFooter) throws IOException {
        this.game = game;
        for (var asset : ASSETS.values()) {
            this.assets.put(asset.file(), readAsset(assets, asset.file()));
        }
        if (menuFooter != null) {
            routes.put("/" + MENU_FOOTER.file(), MENU_FOOTER);
            this.assets.put(MENU_FOOTER.file(), Files.readAllBytes(menuFooter));
        }
        var bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        access = HexFormat.of().formatHex(bytes);
        server = HttpServer.create(address, 16);
        server.setExecutor(tasks);
        server.createContext("/", this::handle);
        server.start();
    }

    public static void launch(BrowserGame game, String[] args) throws IOException {
        launch(game, args, null);
    }

    public static void launch(BrowserGame game, String[] args, Path menuFooter) throws IOException {
        if (args.length != 3) throw new IllegalArgumentException("Expected assets directory, listen address and port");
        Path assets = args[0].equals("-") ? null : Path.of(args[0]);
        var host = new BrowserServer(game, assets, new InetSocketAddress(args[1], Integer.parseInt(args[2])), menuFooter);
        Runtime.getRuntime().addShutdownHook(new Thread(host::close));
        System.out.println(host.url());
    }

    public String url() {
        var address = server.getAddress();
        String host = address.getHostString();
        if (host.contains(":")) host = "[" + host + "]";
        return "http://" + host + ":" + address.getPort() + "/#" + access;
    }

    private void handle(HttpExchange http) throws IOException {
        try (http) {
            http.getResponseHeaders().set("Cache-Control", "no-store");
            http.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            http.getResponseHeaders().set("Referrer-Policy", "no-referrer");
            http.getResponseHeaders().set("Content-Security-Policy",
                    "default-src 'self'; style-src 'self' 'unsafe-inline'; frame-ancestors 'none'; base-uri 'none'");
            String path = http.getRequestURI().getRawPath();
            String method = http.getRequestMethod();
            var asset = routes.get(path);
            if (asset != null && method.equals("GET")) {
                send(http, 200, asset.mime(), assets.get(asset.file()));
            } else if (path.startsWith("/api/")) {
                api(http, path, method);
            } else {
                error(http, 404, "Unknown resource");
            }
        }
    }

    private void api(HttpExchange http, String path, String method) throws IOException {
        var headers = http.getRequestHeaders();
        String token = headers.getFirst("X-Table-Access");
        String tab = headers.getFirst("X-Table-Client");
        String profile = headers.getFirst("X-Table-Profile");
        if (profile == null) profile = tab;
        if (token == null || !MessageDigest.isEqual(access.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8)) || !validId(tab) || !validId(profile)) {
            error(http, 403, "Open the link printed by the launcher");
            return;
        }
        var client = new Client(tab, profile);
        switch (path) {
            case "/api/setup" -> {
                if (!requireMethod(http, method, "GET")) return;
                json(http, 200, game.setup(TrickCollectionMotion.keyframes()));
            }
            case "/api/state" -> {
                if (!requireMethod(http, method, "GET")) return;
                var session = session(client, game.hasProfile());
                if (session == null && game.hasProfile()) error(http, 503, "Too many open profiles");
                else json(http, 200, session == null ? new BrowserSession.State(0, null, null, List.of(), List.of()) : session.state());
            }
            case "/api/rankings" -> {
                if (!requireMethod(http, method, "GET")) return;
                var session = session(client, game.hasProfile());
                if (session == null) error(http, 404, "Rankings unavailable");
                else json(http, 200, session.rankings());
            }
            case "/api/layout" -> {
                if (!requireMethod(http, method, "POST")) return;
                try {
                    json(http, 200, BrowserGeometry.measure(game, decode(http.getRequestBody().readNBytes(MAX_FORM + 1))));
                } catch (IllegalArgumentException invalid) {
                    error(http, 400, "Invalid layout measurements");
                }
            }
            case "/api/start", "/api/action", "/api/profile", "/api/notice", "/api/language", "/api/hand-feedback" -> {
                if (!requireMethod(http, method, "POST")) return;
                mutate(http, path, client);
            }
            default -> error(http, 404, "Unknown command");
        }
    }

    private void mutate(HttpExchange http, String path, Client client) throws IOException {
        try {
            var form = decode(http.getRequestBody().readNBytes(MAX_FORM + 1));
            long revision = Long.parseLong(form.getOrDefault("revision", ""));
            boolean start = path.equals("/api/start");
            var session = session(client, start || game.hasProfile());
            if (session == null) {
                error(http, start ? 503 : 409, start ? "Too many open matches" : "Match unavailable");
                return;
            }
            var state = switch (path) {
                case "/api/start" -> session.start(game, revision, form);
                case "/api/hand-feedback" -> session.handFeedback(revision, form);
                case "/api/profile" -> session.rename(revision, form.getOrDefault("name", ""));
                case "/api/language" -> session.language(revision, form.getOrDefault("language", ""));
                case "/api/notice" -> session.dismissNotice(revision);
                default -> session.act(revision, form.getOrDefault("action", ""));
            };
            json(http, 200, state);
        } catch (BrowserSession.StaleRevision stale) {
            error(http, 409, "The table has advanced. Reload its state.");
        } catch (IllegalArgumentException bad) {
            error(http, 400, "Invalid command or configuration");
        } catch (RuntimeException failure) {
            System.err.println("Browser game request failed: " + failure);
            error(http, 500, "The game could not process the request");
        }
    }

    private synchronized BrowserSession session(Client client, boolean create) {
        if (!create || sessions.containsKey(client) || sessions.size() >= MAX_TABS) return sessions.get(client);
        var profile = game.hasProfile() ? profiles.computeIfAbsent(client.profile(), game::openProfile) : null;
        var session = new BrowserSession(profile);
        sessions.put(client, session);
        return session;
    }

    private static boolean validId(String value) {
        return value != null && value.matches("[0-9a-f]{32}");
    }

    private static byte[] readAsset(Path directory, String file) throws IOException {
        if (directory != null) return Files.readAllBytes(directory.resolve(file));
        try (var resource = BrowserServer.class.getResourceAsStream("web/" + file)) {
            if (resource == null) throw new IOException("Missing browser asset: " + file);
            return resource.readAllBytes();
        }
    }

    private static Map<String, String> decode(byte[] body) {
        if (body.length > MAX_FORM) throw new IllegalArgumentException("Form too large");
        var fields = new HashMap<String, String>();
        for (String pair : new String(body, StandardCharsets.UTF_8).split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length != 2) throw new IllegalArgumentException("Malformed form");
            var name = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            var value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            if (fields.putIfAbsent(name, value) != null) throw new IllegalArgumentException("Duplicate form key");
        }
        return fields;
    }

    private static boolean requireMethod(HttpExchange http, String actual, String expected) throws IOException {
        if (actual.equals(expected)) return true;
        http.getResponseHeaders().set("Allow", expected);
        error(http, 405, "Method not allowed");
        return false;
    }

    private static void error(HttpExchange http, int status, String message) throws IOException {
        json(http, status, new ErrorMessage(message));
    }

    private static void json(HttpExchange http, int status, Object data) throws IOException {
        send(http, status, "application/json; charset=utf-8", BrowserJson.encode(data).getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange http, int status, String mime, byte[] data) throws IOException {
        http.getResponseHeaders().set("Content-Type", mime);
        http.sendResponseHeaders(status, data.length);
        http.getResponseBody().write(data);
    }

    @Override public void close() {
        server.stop(0);
        tasks.shutdownNow();
    }

    record ErrorMessage(String error) { }
    record Asset(String file, String mime) { }
    private record Client(String tab, String profile) { }
}
