import com.google.gson.Gson;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Browser acceptance of the compiled adapter, served from an owned loopback origin. */
class BrowserUi {
    private static final String SITE_PATH = "/atf_j4/";
    private static final Set<String> ASSETS = Set.of("index.html", "table.css", "table.mjs", "viewport.mjs",
            "profile.mjs", "records.mjs", "auto-game.mjs", "collection.mjs", "hand-feedback.mjs",
            "local-transport.mjs", "engine-worker.mjs", "whist-engine.mjs");
    private static final String MOBILE_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36";
    private static final Gson JSON = new Gson();

    record Image(String file, String title) { }
    record Result(String id, String name, String status, String expected, Object observed, List<Image> images) { }

    public static void main(String[] args) throws IOException {
        Path site = Path.of(args[0]).toRealPath();
        String checkpoint = Files.readString(Path.of(args[1]));
        Path output = Path.of(args[2]).toRealPath();
        Path resultsFile = output.resolve("browser-ui-checks.json");
        if (Files.exists(resultsFile)) throw new IllegalArgumentException("Use a new Playwright evidence directory");
        for (String asset : ASSETS) if (!Files.isRegularFile(site.resolve(asset))) {
            throw new IllegalArgumentException("Missing site asset: " + asset);
        }
        HttpServer server = serve(site);
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + SITE_PATH;
        List<Result> results = new ArrayList<>();
        try (Playwright playwright = Playwright.create()) {
            var launch = new BrowserType.LaunchOptions().setHeadless(true);
            if (args.length == 4) launch.setExecutablePath(Path.of(args[3]));
            try (Browser browser = playwright.chromium().launch(launch)) {
                results.add(run(browser, mobile(), output, "WEB-UI-17", "Evantaiul și licitarea la 414×896",
                        "17 cărți accesibile și aceleași locuri după licitarea de la tastatură",
                        (page, images) -> responsiveHand(page, url)));
                results.add(run(browser, mobile(), output, "WEB-UI-AUTO", "Atingere respinsă și AutoGame în Whist",
                        "Textul accesibil și marcajele duc la patru colectări și la scorul așteptat",
                        (page, images) -> autoGame(page, url, checkpoint, output, images)));
                results.add(run(browser, new Browser.NewContextOptions().setViewportSize(1280, 720), output,
                        "WEB-UI-DESKTOP", "Profil local și centrare pe desktop",
                        "Anularea și salvarea se văd în meniu, iar cadrul portrait se centrează la redimensionare",
                        (page, images) -> desktop(page, url)));
            }
        } finally {
            server.stop(0);
        }
        Files.writeString(resultsFile, JSON.toJson(results));
        if (results.stream().anyMatch(result -> !result.status().equals("PASS"))) System.exit(1);
    }

    private static Browser.NewContextOptions mobile() {
        return new Browser.NewContextOptions().setViewportSize(414, 896).setDeviceScaleFactor(1)
                .setIsMobile(true).setHasTouch(true).setUserAgent(MOBILE_AGENT);
    }

    @FunctionalInterface
    private interface Scenario { Map<String, Object> run(Page page, List<Image> images); }

    private static Result run(Browser browser, Browser.NewContextOptions options, Path output,
                              String id, String title, String expected, Scenario scenario) {
        try (BrowserContext context = browser.newContext(options)) {
            Page page = context.newPage();
            page.setDefaultTimeout(12_000);
            List<String> errors = new ArrayList<>();
            page.onPageError(errors::add);
            List<Image> images = new ArrayList<>();
            try {
                var observations = scenario.run(page, images);
                require(errors.isEmpty(), "JavaScript errors: " + errors);
                images.add(screenshot(page, output, id, title));
                System.out.println("PASS " + title);
                return new Result(id, title, "PASS", expected,
                        Map.of("browser", browser.version(), "observations", observations), images);
            } catch (RuntimeException | AssertionError failure) {
                System.out.println("FAIL " + title);
                try {
                    images.add(screenshot(page, output, id + "-failure", title));
                } catch (PlaywrightException captureFailure) {
                    failure.addSuppressed(captureFailure);
                }
                var diagnostic = new java.io.StringWriter();
                failure.printStackTrace(new java.io.PrintWriter(diagnostic));
                return new Result(id, title, "FAIL", expected, diagnostic.toString(), images);
            }
        }
    }

    private static Map<String, Object> responsiveHand(Page page, String url) {
        page.navigate(url);
        button(page, "Joc nou").click();
        page.locator("select[name=players]").selectOption("3");
        page.locator("select[name=mode]").selectOption("DOWN_AND_UP");
        button(page, "Începe partida").click();
        page.locator("[data-bid][aria-pressed=true]").waitFor();
        page.locator(".board[data-layout=ready]").waitFor();
        require("phone".equals(page.locator("#frame").getAttribute("data-viewport")), "Expected mobile layout");
        var frame = page.locator("#frame").boundingBox();
        near(414, frame.width);
        near(896, frame.height);
        Locator hand = page.locator(".hand");
        Locator cards = hand.locator(".fan .card");
        require(cards.count() == 17, "Expected 17 cards");
        cards.last().scrollIntoViewIfNeeded();
        var end = cards.last().boundingBox();
        var holder = hand.boundingBox();
        require(end.x >= holder.x - 1 && end.x + end.width <= holder.x + holder.width + 1,
                "Last card cannot be shown in full");
        var biddingSeats = seats(page);
        button(page, "Licitează").press("Space");
        assertThat(page.locator("[data-bid]")).hasCount(0);
        page.locator("#table[aria-busy=false] .board[data-layout=ready]").waitFor();
        var playingSeats = seats(page);
        require(biddingSeats.equals(playingSeats), "Player boxes moved: " + biddingSeats + " -> " + playingSeats);
        return Map.of("viewport", List.of(414, 896), "cards", cards.count(), "seats", playingSeats,
                "lastCardRight", end.x + end.width);
    }

    private static Map<String, Object> autoGame(Page page, String url, String checkpoint, Path output, List<Image> images) {
        page.addInitScript("""
                if (location.hostname === "127.0.0.1") {
                  localStorage.setItem("whistlab:pages:profile:v1", %s);
                }
                """.formatted(JSON.toJson(checkpoint)));
        page.navigate(url);
        button(page, "Reia partida").click();
        page.locator(".board[data-layout=ready]").waitFor();
        Locator rejected = page.locator(".fan button[data-face='8♣']");
        rejected.tap();
        assertThat(rejected).hasClass(Pattern.compile("rejected-card"));
        assertThat(page.locator(".fan .suggested-card")).hasCount(2);
        require(page.locator(".fan button").count() == 4, "A rejected card changed the hand");
        page.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName("Automat").setExact(true)).tap();
        var collection = page.locator(".collection .compact-play-face");
        var faces = List.of("9♦", "Q♥", "K♦", "8♦");
        assertThat(collection).hasText(faces.toArray(String[]::new));
        var eva = page.locator(".seat").filter(new Locator.FilterOptions()
                .setHas(page.getByText("Eva", new Page.GetByTextOptions().setExact(true))));
        assertThat(eva.locator(".seat-won")).hasText("Levate: 1");
        assertThat(page.locator(".fan button")).hasCount(3);
        images.add(screenshot(page, output, "WEB-UI-AUTO-collection", "AutoGame după prima levată"));
        page.locator(".whist-score").waitFor(new Locator.WaitForOptions().setTimeout(45_000));
        var notice = page.locator("dialog.achievement-notice[open]");
        if (notice.isVisible()) notice.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Închide").setExact(true)).click();
        var totals = page.locator(".whist-score tbody tr td:last-child").allTextContents();
        require(totals.equals(List.of("20", "6", "24", "10")), "Unexpected score: " + totals);
        require(page.locator(".whist-auto").count() == 0, "AutoGame remains available at score");
        return Map.of("firstCollection", faces, "winner", "Eva", "totals", totals, "rejectedCard", "8♣");
    }

    private static Map<String, Object> desktop(Page page, String url) {
        page.navigate(url);
        var profile = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(Pattern.compile("^Numele tău:")));
        String initial = profile.innerText();
        profile.click();
        Locator dialog = page.getByRole(AriaRole.DIALOG);
        dialog.getByRole(AriaRole.TEXTBOX).fill("Bianca");
        dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Anulează").setExact(true)).click();
        button(page, initial).waitFor();
        profile.click();
        dialog.getByRole(AriaRole.TEXTBOX).fill("Bianca");
        dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Salvează").setExact(true)).click();
        button(page, "Numele tău: Bianca").waitFor();
        page.reload();
        button(page, "Numele tău: Bianca").waitFor();
        var first = centered(page, 1280, 720);
        page.setViewportSize(1920, 1080);
        var second = centered(page, 1920, 1080);
        return Map.of("profile", "Bianca", "frame1280", first, "frame1920", second);
    }

    private static List<Long> centered(Page page, int width, int height) {
        var frame = page.locator("#frame[data-viewport=desktop]");
        assertThat(frame).hasCSS("height", height + "px");
        var box = frame.boundingBox();
        double expectedWidth = 414.0 * height / 896;
        near(expectedWidth, box.width);
        near((width - expectedWidth) / 2, box.x);
        near(0, box.y);
        return dimensions(box);
    }

    private static List<List<Long>> seats(Page page) {
        return page.locator(".board .seat").all().stream().map(Locator::boundingBox).map(BrowserUi::dimensions).toList();
    }

    private static List<Long> dimensions(BoundingBox box) {
        return List.of(Math.round(box.x), Math.round(box.y), Math.round(box.width), Math.round(box.height));
    }

    private static Locator button(Page page, String name) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
    }

    private static Image screenshot(Page page, Path output, String id, String title) {
        String file = id + ".png";
        page.screenshot(new Page.ScreenshotOptions().setPath(output.resolve(file)));
        return new Image(file, title);
    }

    private static void near(double expected, double observed) {
        require(Math.abs(expected - observed) <= 1, "Expected " + expected + ", observed " + observed);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static HttpServer serve(Path site) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(SITE_PATH, exchange -> {
            try (exchange) {
                String name = exchange.getRequestURI().getRawPath().substring(SITE_PATH.length());
                if (name.isEmpty()) name = "index.html";
                if (!ASSETS.contains(name) || !exchange.getRequestMethod().equals("GET")) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                String type = name.endsWith(".mjs") ? "text/javascript" :
                        name.endsWith(".css") ? "text/css" : "text/html";
                exchange.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
                exchange.getResponseHeaders().set("Cache-Control", "no-store");
                byte[] bytes = Files.readAllBytes(site.resolve(name));
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        });
        server.start();
        return server;
    }
}
