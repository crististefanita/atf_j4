import com.google.gson.Gson;
import com.google.gson.JsonParser;
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
        String wideCheckpoint = JsonParser.parseString(checkpoint).getAsJsonObject().get("wideCheckpoint").getAsString();
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
            var launch = new BrowserType.LaunchOptions().setHeadless(true).setArgs(List.of("--mute-audio"));
            if (args.length == 4) launch.setExecutablePath(Path.of(args[3]));
            try (Browser browser = playwright.chromium().launch(launch)) {
                results.add(run(browser, mobile(), output, "WEB-COMPONENT-PLAYBACK-PAINT",
                        "Tabla rămâne stabilă când adversarul redă rândul jucătorului",
                        "După colectare și cartea adversarului, tabla rămâne așezată până devine disponibilă mutarea legală.",
                        (page, images) -> stablePlaybackPaint(page, url, checkpoint, output, images)));
                if (!Boolean.getBoolean("whist.refreshOnly")) {
                    results.add(run(browser, mobile().setViewportSize(360, 800), output, "WEB-COMPONENT-TEXT",
                            "Fonturile Whist pe un panou de probă",
                            "Toate cele 13 ranguri sunt mărite cu 50%. Simbolurile și atuul își păstrează mărimea.",
                            (page, images) -> rankTypography(page, url)));
                    results.add(run(browser, mobile(), output, "WEB-UI-17", "Evantaiul și licitarea la 414×896",
                            "17 cărți accesibile și aceleași locuri după licitarea de la tastatură",
                            (page, images) -> responsiveHand(page, url)));
                    results.add(run(browser, mobile().setViewportSize(360, 800), output, "WEB-UI-NUMBERS",
                            "Rangul 10 în mâna de 17 cărți, la text 200%",
                            "Cifrele și figurile păstrează aceeași ancoră. Antetul respectă scara de text.",
                            (page, images) -> enlargedNumbers(page, url, wideCheckpoint, output, images)));
                    results.add(run(browser, mobile(), output, "WEB-UI-AUTO", "Atingere respinsă și AutoGame în Whist",
                            "Textul accesibil și marcajele duc la patru colectări și la scorul așteptat",
                            (page, images) -> autoGame(page, url, checkpoint, output, images)));
                    results.add(run(browser, new Browser.NewContextOptions().setViewportSize(535, 760), output,
                            "WEB-UI-HEADER-DESKTOP", "Antet Oh Hell în cadrul scalat pe desktop",
                            "Titlul și progresul au aceeași ancoră, iar atuul rămâne la marginea utilă",
                            (page, images) -> desktopHeader(page, url)));
                    results.add(run(browser, new Browser.NewContextOptions().setViewportSize(1280, 720), output,
                            "WEB-UI-DESKTOP", "Profil local și centrare pe desktop",
                            "Anularea și salvarea se văd în meniu, iar cadrul portrait se centrează la redimensionare",
                            (page, images) -> desktop(page, url)));
                }
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

    private static Map<String, Object> rankTypography(Page page, String url) {
        var ranks = List.of("2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A");
        String cards = ranks.stream().map(rank -> """
                <button class="card"><span class="card-index"><span class="card-rank">%s</span><span>♦</span></span></button>
                """.formatted(rank)).collect(java.util.stream.Collectors.joining());
        String fixture = url + "presentation-fixture.html";
        page.route(fixture, route -> route.fulfill(new Route.FulfillOptions().setContentType("text/html; charset=utf-8")
                .setBody("""
                        <!doctype html><html lang="ro"><meta charset="utf-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1">
                        <link rel="stylesheet" href="table.css">
                        <body data-surface="whist" style="--whist-hand-label: .8125rem; --whist-hand-rank-cap: 26px;
                          --whist-hand-face-width: 63px; --whist-hand-face-height: 88px">
                        <section class="whist-match"><div class="whist-trump"><span class="card"><span class="card-rank">A</span></span></div>
                        <div class="hand"><div class="fan">%s</div></div></section></body></html>
                        """.formatted(cards))));
        page.navigate(fixture);
        var labels = page.locator(".fan .card-rank");
        assertThat(labels).hasText(ranks.toArray(String[]::new));
        for (int scale : List.of(100, 200)) {
            page.evaluate("(scale) => document.documentElement.style.fontSize = scale + '%'", scale);
            for (var label : labels.all()) assertThat(label).hasCSS("font-size", scale == 100 ? "19.5px" : "39px");
            assertThat(page.locator(".fan .card-index span:last-child").first()).hasCSS("font-size", scale == 100 ? "13px" : "26px");
            assertThat(page.locator(".fan .card").first()).hasCSS("width", "63px");
        }
        page.evaluate("() => document.documentElement.style.fontSize = '100%'");
        assertThat(page.locator(".whist-trump .card-rank")).hasCSS("font-size", "20px");
        return Map.of("ranks", ranks, "fontPx", List.of(19.5, 39), "suitPx", List.of(13, 26), "trumpPx", 20);
    }

    private static Map<String, Object> responsiveHand(Page page, String url) {
        page.navigate(url);
        assertThat(page.locator(".whist-home h1")).hasCSS("font-size", "37.44px");
        assertThat(button(page, "Joc nou")).hasCSS("font-size", "24px");
        assertThat(page.locator(".whist-home select")).hasCSS("font-size", "18px");
        button(page, "Joc nou").click();
        assertThat(page.locator(".setup h1")).hasCSS("font-size", "37.44px");
        verifySetupDropdowns(page);
        assertThat(button(page, "Începe partida")).hasCSS("font-size", "24px");
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
        assertHeader(page, false);
        assertThat(page.locator(".seat.south")).hasAttribute("aria-current", "true");
        assertThat(page.locator(".seat.south .seat-turn")).isHidden();
        assertThat(button(page, "Licitează")).isVisible();
        button(page, "Licitează").press("Space");
        assertThat(page.locator("[data-bid]")).hasCount(0);
        page.locator("#table[aria-busy=false] .board[data-layout=ready]").waitFor();
        var playingSeats = seats(page);
        assertHeader(page, false);
        require(biddingSeats.equals(playingSeats), "Player boxes moved: " + biddingSeats + " -> " + playingSeats);
        return Map.of("viewport", List.of(414, 896), "cards", cards.count(), "seats", playingSeats,
                "lastCardRight", end.x + end.width);
    }

    private static void verifySetupDropdowns(Page page) {
        for (var select : page.locator(".setup select").all()) {
            boolean scaled = (Boolean) select.evaluate("""
                    node => {
                      const size = element => parseFloat(getComputedStyle(element).fontSize);
                      return Math.abs(size(node) - .75 * size(node.parentElement)) < .01 &&
                        [...node.options].every(option => Math.abs(size(option) - size(node)) < .01);
                    }
                    """);
            require(scaled, "Expected a 25% smaller font for " + select.getAttribute("name"));
        }
        for (int width : List.of(360, 414)) {
            page.setViewportSize(width, width == 360 ? 800 : 896);
            assertThat(page.locator("#frame")).hasCSS("width", width + "px");
            boolean difficultyOnOneLine = (Boolean) page.locator(".difficulty-field").evaluate("""
                    label => {
                      const range = document.createRange();
                      range.selectNodeContents(label.firstChild);
                      const text = range.getBoundingClientRect();
                      const choice = label.querySelector("select").getBoundingClientRect();
                      return choice.left >= text.right && choice.top <= text.top && choice.bottom >= text.bottom;
                    }
                    """);
            require(difficultyOnOneLine, "The Romanian difficulty label and selector must share one row at " + width);
        }
    }

    private static Map<String, Object> enlargedNumbers(Page page, String url, String checkpoint,
                                                       Path output, List<Image> images) {
        page.addInitScript("""
                document.addEventListener("DOMContentLoaded", () => {
                  document.documentElement.style.fontSize = "200%";
                }, { once: true });
                """);
        resumeHand(page, url, JSON.toJson(Map.of("checkpoint", checkpoint)));
        var hand = page.locator(".hand");
        var cards = hand.locator(".fan .card");
        assertThat(cards).hasCount(17);
        var ten = cards.and(page.locator("[data-face^='10']")).first();
        String face = ten.getAttribute("data-face");
        var rank = ten.locator(".card-index:not(.inverted) .card-rank");
        var suit = ten.locator(".card-index:not(.inverted) span:last-child");
        assertThat(rank).hasText("10");
        assertThat(rank).hasCSS("font-size", "39px");
        assertThat(suit).hasCSS("font-size", "26px");
        assertHeader(page, true);
        var tenCard = ten.boundingBox();
        var tenRank = rank.boundingBox();
        for (String faceRank : List.of("A", "K", "Q", "J")) {
            var figure = cards.and(page.locator("[data-face^='" + faceRank + "']")).first();
            var figureRank = figure.locator(".card-index:not(.inverted) .card-rank");
            assertThat(figureRank).hasCSS("font-size", "39px");
            var rect = figure.boundingBox();
            var glyph = figureRank.boundingBox();
            near(tenRank.x + tenRank.width / 2 - tenCard.x, glyph.x + glyph.width / 2 - rect.x);
            near(tenRank.y - tenCard.y, glyph.y - rect.y);
        }
        rank.scrollIntoViewIfNeeded();
        var next = ten.locator("xpath=following-sibling::button[1]").boundingBox();
        var glyphs = rank.boundingBox();
        var scrollport = hand.boundingBox();
        require(glyphs.x >= scrollport.x - 1 && glyphs.x + glyphs.width <= scrollport.x + scrollport.width + 1,
                "The scrollport clips " + face);
        require(glyphs.x + glyphs.width <= next.x, "The next card covers " + face);
        require(ten.boundingBox().y + ten.boundingBox().height <= scrollport.y + scrollport.height + 1,
                "The scrollport clips the card vertically");
        images.add(screenshot(page, output, "WEB-UI-NUMBERS-10", "Rangul 10 înaintea cărții următoare"));
        cards.last().scrollIntoViewIfNeeded();
        var last = cards.last().boundingBox();
        require(last.x >= scrollport.x - 1 && last.x + last.width <= scrollport.x + scrollport.width + 1,
                "The last card cannot be shown in full at 200%");
        return Map.of("viewport", List.of(360, 800), "textScale", 2, "cards", 17, "card", face,
                "rankPixels", 39, "suitPixels", 26,
                "rankWidth", glyphs.width, "clearanceToNextCard", next.x - glyphs.x - glyphs.width);
    }

    private static Map<String, Object> desktopHeader(Page page, String url) {
        page.navigate(url);
        button(page, "Joc nou").click();
        page.locator("select[name=mode]").selectOption("OH_HELL");
        button(page, "Începe partida").click();
        page.locator(".board[data-layout=ready]").waitFor();
        assertThat(page.locator(".whist-title-row h1")).hasText("Whist · Oh Hell");
        assertThat(page.locator(".whist-trump .card")).isVisible();
        double scale = page.locator("#frame").boundingBox().width / 414.0;
        assertHeader(page, false, scale);
        return Map.of("viewport", List.of(535, 760), "scale", scale, "title", "Whist · Oh Hell");
    }

    private static void assertHeader(Page page, boolean enlarged) {
        assertHeader(page, enlarged, 1);
    }

    private static void assertHeader(Page page, boolean enlarged, double scale) {
        var header = page.locator(".whist-header");
        var row = header.locator(".whist-title-row");
        var title = row.locator("h1");
        var menu = row.locator("summary");
        var progress = header.locator(".whist-progress");
        var trump = header.locator(".whist-trump");
        assertThat(title).hasCSS("font-size", enlarged ? "28px" : "14px");
        assertThat(progress).hasCSS("font-size", enlarged ? "33px" : "16.5px");
        double bidsFont = ((Number) header.locator(".whist-bids")
                .evaluate("node => parseFloat(getComputedStyle(node).fontSize)")).doubleValue();
        require(Math.abs(bidsFont - (enlarged ? 31.59 : 15.795)) < .005, "Wrong bid summary size");
        assertThat(trump).hasCSS("width", "57px");
        assertThat(trump).hasCSS("height", enlarged ? "89px" : "49.5px");
        assertThat(menu).hasCSS("width", "48px");
        var bounds = header.boundingBox();
        var rowBounds = row.boundingBox();
        var trumpBounds = trump.boundingBox();
        near(bounds.x + 56 * scale, title.boundingBox().x);
        near(title.boundingBox().x, progress.boundingBox().x);
        near(bounds.x + bounds.width, trumpBounds.x + trumpBounds.width);
        var details = header.locator(".whist-header-details").boundingBox();
        near(rowBounds.y + rowBounds.height, details.y);
        near(details.y, trumpBounds.y);
    }

    private static void resumeHand(Page page, String url, String checkpoint) {
        page.addInitScript("""
                if (location.hostname === "127.0.0.1") {
                  localStorage.setItem("whistlab:pages:profile:v1", %s);
                }
                """.formatted(JSON.toJson(checkpoint)));
        page.navigate(url);
        button(page, "Reia partida").click();
        page.locator(".board[data-layout=ready]").waitFor();
    }

    private static Map<String, Object> stablePlaybackPaint(Page page, String url, String checkpoint,
                                                            Path output, List<Image> images) {
        page.addInitScript("""
                const send = Worker.prototype.postMessage;
                let actionSent = false;
                Worker.prototype.postMessage = function (message) {
                  if (message.path === "/api/action") actionSent = true;
                  if (message.path === "/api/layout" &&
                      actionSent && document.querySelector("#table .board:not([data-layout])")) {
                    actionSent = false;
                    window.releaseLayout = () => send.call(this, message);
                    document.documentElement.dataset.layoutHeld = "true";
                    return;
                  }
                  send.call(this, message);
                };
                """);
        resumeHand(page, url, checkpoint);
        var trump = page.locator(".whist-trump:visible");
        var manualTrump = trumpAppearance(trump);
        page.locator(".fan button[data-face='8♦']").tap();
        var collection = page.locator(".collection .compact-play-face");
        assertThat(collection).hasText(new String[]{"9♦", "Q♥", "K♦", "8♦"});
        var collectingTrump = trumpAppearance(trump);
        page.locator("html[data-layout-held]").waitFor();
        try {
            var visibleBoard = page.locator(".board:visible");
            assertThat(visibleBoard).hasCount(1);
            require("ready".equals(visibleBoard.getAttribute("data-layout")),
                    "Playback showed a board with fallback positions while the Worker layout was pending");
            assertThat(visibleBoard.locator(".compact-play-face")).hasText("A♠");
            images.add(screenshot(page, output, "WEB-COMPONENT-PLAYBACK-WAIT", "Tabla în timp ce geometria nouă este în așteptare"));
        } finally {
            page.evaluate("() => window.releaseLayout()");
        }
        assertThat(page.locator("#table")).hasAttribute("aria-busy", "false");
        assertThat(page.locator(".board:visible")).hasAttribute("data-layout", "ready");
        assertThat(page.locator(".fan .card")).hasCount(3);
        assertThat(page.locator(".seat.south.active .seat-turn")).hasText("JOACĂ ACUM");
        assertThat(page.locator(".fan .legal-card")).hasAttribute("data-face", "10♦");
        require(manualTrump.equals(collectingTrump), "Collection changed the trump frame: " + collectingTrump);
        return Map.of("collection", List.of("9♦", "Q♥", "K♦", "8♦"), "remainingCards", 3,
                "opponentLead", "A♠", "legalCardOnReturn", "10♦",
                "layoutReadyWhilePending", true, "trumpFrame", manualTrump);
    }

    private static Object trumpAppearance(Locator trump) {
        return trump.evaluate("""
                node => {
                  const { backgroundColor, boxShadow, color } = getComputedStyle(node);
                  const { x, y, width, height } = node.getBoundingClientRect();
                  return { backgroundColor, boxShadow, color, x, y, width, height };
                }
                """);
    }

    private static Map<String, Object> autoGame(Page page, String url, String checkpoint, Path output, List<Image> images) {
        resumeHand(page, url, checkpoint);
        var ten = page.locator(".fan [data-face^='10'] .card-index:not(.inverted) .card-rank");
        assertThat(ten).hasText("10");
        assertThat(ten).hasCSS("font-size", "19.5px");
        assertThat(page.locator(".fan [data-face^='10'] .card-index.inverted .card-rank")).hasCSS("font-size", "13.5px");
        assertThat(page.locator(".fan [data-face^='10'] .suit")).hasCSS("font-size", "19px");
        assertHeader(page, false);
        var trump = page.locator(".whist-trump .card");
        assertThat(trump.locator(".card-index:not(.inverted) .card-rank")).hasText("Q");
        assertThat(trump.locator(".suit")).hasText("♦");
        assertThat(trump).hasCSS("flex-direction", "row");
        assertThat(trump.locator(".card-rank").first()).hasCSS("font-size", "20px");
        assertThat(trump).hasCSS("height", "27px");
        var trumpFrame = page.locator(".whist-trump").boundingBox();
        var trumpFace = trump.boundingBox();
        require(trumpFace.x >= trumpFrame.x + 4 && trumpFace.x + trumpFace.width <= trumpFrame.x + trumpFrame.width - 4,
                "The turned card must fit inside its frame padding");
        images.add(screenshot(page, output, "WEB-UI-HEADER", "Antetul Web după referința Android"));
        var menu = page.locator(".whist-navigation summary");
        menu.click();
        var menuItems = page.locator(".whist-menu-items");
        assertThat(menuItems).isVisible();
        assertThat(menuItems.locator("button").first()).hasCSS("font-size", "24px");
        var board = page.locator(".board");
        board.tap(new Locator.TapOptions().setPosition(board.boundingBox().width - 8, 8));
        assertThat(menuItems).isHidden();
        Locator rejected = page.locator(".fan button[data-face='8♣']");
        rejected.tap();
        assertThat(rejected).hasClass(Pattern.compile("rejected-card"));
        assertThat(page.locator(".fan .suggested-card")).hasCount(2);
        require(page.locator(".fan button").count() == 4, "A rejected card changed the hand");
        var manualTrump = trumpAppearance(trump.locator(".."));
        page.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName("Automat").setExact(true)).tap();
        require(manualTrump.equals(trumpAppearance(trump.locator(".."))), "AutoGame changed the trump frame");
        var collection = page.locator(".collection .compact-play-face");
        var faces = List.of("9♦", "Q♥", "K♦", "8♦");
        assertThat(collection).hasText(faces.toArray(String[]::new));
        require(manualTrump.equals(trumpAppearance(trump.locator(".."))), "AutoGame collection changed the trump frame");
        var eva = page.locator(".seat").filter(new Locator.FilterOptions()
                .setHas(page.getByText("Eva", new Page.GetByTextOptions().setExact(true))));
        assertThat(eva.locator(".seat-won")).hasText("Levate: 1");
        assertThat(page.locator(".fan button")).hasCount(3);
        assertThat(collection.filter(new Locator.FilterOptions().setHasText("9♦")).locator(".card-rank"))
                .hasCSS("font-size", "19.5px");
        assertThat(collection.filter(new Locator.FilterOptions().setHasText("Q♥")).locator(".card-rank"))
                .hasCSS("font-size", "19.5px");
        assertThat(collection.filter(new Locator.FilterOptions().setHasText("Q♥")).locator(".compact-suit"))
                .hasCSS("font-size", "13px");
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
