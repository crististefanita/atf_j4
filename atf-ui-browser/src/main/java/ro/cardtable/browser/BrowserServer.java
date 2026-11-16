package ro.cardtable.browser;

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
            "/viewport.mjs", new Asset("viewport.mjs", "text/javascript; charset=utf-8"));
    private final HttpServer server;
    private final java.util.concurrent.ExecutorService tasks = Executors.newFixedThreadPool(4);
    private final String access;
    private final BrowserGame game;
    private final Path assets;
    private final Map<String, BrowserSession> sessions = new HashMap<>();

    public BrowserServer(BrowserGame game, Path assets, InetSocketAddress address) throws IOException {
        this.game = game;
        this.assets = assets.toAbsolutePath();
        for (var asset : ASSETS.values()) {
            if (!Files.isRegularFile(this.assets.resolve(asset.file()))) throw new IOException("Browser assets are incomplete");
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
        if (args.length != 3) throw new IllegalArgumentException("Expected assets directory, listen address and port");
        var host = new BrowserServer(game, Path.of(args[0]), new InetSocketAddress(args[1], Integer.parseInt(args[2])));
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
            var asset = ASSETS.get(path);
            if (asset != null && method.equals("GET")) {
                send(http, 200, asset.mime(), Files.readAllBytes(assets.resolve(asset.file())));
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
        String client = headers.getFirst("X-Table-Client");
        if (token == null || !MessageDigest.isEqual(access.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8)) || client == null || !client.matches("[0-9a-f]{32}")) {
            error(http, 403, "Open the link printed by the launcher");
            return;
        }
        switch (path) {
            case "/api/setup" -> {
                if (!requireMethod(http, method, "GET")) return;
                json(http, 200, new Setup(game.title(), game.fields()));
            }
            case "/api/state" -> {
                if (!requireMethod(http, method, "GET")) return;
                var session = session(client, false);
                json(http, 200, session == null ? new BrowserSession.State(0, null) : session.state());
            }
            case "/api/layout" -> {
                if (!requireMethod(http, method, "POST")) return;
                try {
                    json(http, 200, BrowserGeometry.measure(decode(http.getRequestBody().readNBytes(MAX_FORM + 1))));
                } catch (IllegalArgumentException invalid) {
                    error(http, 400, "Invalid layout measurements");
                }
            }
            case "/api/start", "/api/action" -> {
                if (!requireMethod(http, method, "POST")) return;
                mutate(http, path, client);
            }
            default -> error(http, 404, "Unknown command");
        }
    }

    private void mutate(HttpExchange http, String path, String client) throws IOException {
        try {
            var form = decode(http.getRequestBody().readNBytes(MAX_FORM + 1));
            long revision = Long.parseLong(form.getOrDefault("revision", ""));
            boolean start = path.equals("/api/start");
            var session = session(client, start);
            if (session == null) {
                error(http, start ? 503 : 409, start ? "Too many open matches" : "Match unavailable");
                return;
            }
            json(http, 200, start ? session.start(game, revision, form)
                    : session.act(revision, form.getOrDefault("action", "")));
        } catch (BrowserSession.StaleRevision stale) {
            error(http, 409, "The table has advanced. Reload its state.");
        } catch (IllegalArgumentException bad) {
            error(http, 400, "Invalid command or configuration");
        } catch (RuntimeException failure) {
            System.err.println("Browser game request failed: " + failure);
            error(http, 500, "The game could not process the request");
        }
    }

    private synchronized BrowserSession session(String client, boolean create) {
        if (!create || sessions.containsKey(client) || sessions.size() >= MAX_TABS) return sessions.get(client);
        var session = new BrowserSession();
        sessions.put(client, session);
        return session;
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

    private static void json(HttpExchange http, int status, Record data) throws IOException {
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

    record Setup(String title, java.util.List<BrowserGame.Field> fields) { }
    record ErrorMessage(String error) { }
    record Asset(String file, String mime) { }
}
