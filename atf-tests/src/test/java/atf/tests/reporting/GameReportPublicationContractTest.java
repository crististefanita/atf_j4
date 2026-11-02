package atf.tests.reporting;

import atf.tests.learning.EvidencePolicy;

import static org.junit.jupiter.api.Assertions.*;
import atf.reporting.contract.*;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.*;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GameReportPublicationContractTest {
    @TempDir Path folder;
    static Stream<Arguments> selections() {
        return Arrays.stream(EvidencePolicy.DetailLevel.values()).flatMap(mode -> Stream.of(true, false)
                .flatMap(extent -> Stream.of(1, 2).map(owners -> Arguments.of(mode, extent, owners))));
    }
    @ParameterizedTest @MethodSource("selections")
    void individualAndSuiteRetainEveryComparisonAndPng(EvidencePolicy.DetailLevel mode, boolean extent, int owners) throws Exception {
        String retained = System.getProperty("atf.game.contractOutput", "");
        if (!retained.isBlank()) { folder = Path.of(retained); Files.createDirectories(folder); }
        Path report = folder.resolve(mode + "-" + extent + "-" + owners + ".html");
        var policy = new GameReportPolicy(mode, 2, 20, 512, 10, mode == EvidencePolicy.DetailLevel.FULL ? 4096 : 32);
        var config = new GameReportConfiguration(policy); byte[] png = png();
        AtfReportPorts.Session renderer = extent ? new ExtentReportSession(report, config.settings()) : new HtmlReportSession(report, config.settings());
        try (var session = new GameReportSession(renderer, config)) {
            for (int owner = 0; owner < owners; owner++) publish(session, owner, png);
        }
        var doc = Jsoup.parse(Files.readString(report));
        var scenarios = extent ? doc.select("#test-view .test-list-item > li.test-item") : doc.select("article.scenario");
        assertEquals(owners, scenarios.size());
        for (int owner = 0; owner < owners; owner++) {
            var scenario = scenarios.get(owner);
            var rows = scenario.select(extent ? "table.helm-validation-table > tbody > tr" : "table.assertions > tbody > tr");
            assertEquals(5, rows.size(), "All captured comparisons, including the last FAIL, must remain");
            assertEquals("Final comparison / owner " + owner, rows.last().child(0).wholeText());
            assertTrue(rows.last().child(3).text().contains(extent ? "❌" : "Fail"));
            assertTrue(scenario.select(extent ? "table.helm-validation-table" : "table.assertions").stream()
                    .allMatch(t -> t.select("tbody > tr").size() <= 2), "Configured component table size");
            if(extent) {
                var tables=scenario.select("table.helm-validation-table");
                assertEquals("Pass",tables.get(0).closest(".card").selectFirst("> .card-header .badge.log").text());
                assertEquals("Pass",tables.get(1).closest(".card").selectFirst("> .card-header .badge.log").text());
                assertEquals("Fail",tables.get(2).closest(".card").selectFirst("> .card-header .badge.log").text());
            }
            assertTrue(rows.stream().allMatch(r -> r.childrenSize() == 4 && r.child(1).wholeText().length() <= 240
                    && r.child(2).wholeText().length() <= 240));
            String complete = scenario.select("pre").stream().map(e -> e.wholeText()).reduce("", String::concat);
            assertTrue(complete.contains("expected-final-" + owner)); assertTrue(complete.contains("actual-final-" + owner));
            assertTrue(new GameReportTextAudit(scenario.outerHtml()).contains(multiline(owner)), "Complete TEXT across native disclosure wrappers");
            assertTrue(scenario.text().toUpperCase(Locale.ROOT).contains("ORIGINAL / RETAINED / OMITTED ITEMS"));
            assertEquals(mode != EvidencePolicy.DetailLevel.SUMMARY, scenario.text().contains("SMALL-OPTIONAL"));
            assertEquals(mode == EvidencePolicy.DetailLevel.FULL, scenario.text().contains("LARGE-OPTIONAL"));
            assertFalse(scenario.outerHtml().contains("never-publish-this-secret"));
        }
        assertTrue(doc.select("a[href^=file:],script[src],link[rel=stylesheet][href]").isEmpty());
        assertTrue(Files.readString(report).contains(Base64.getEncoder().encodeToString(png)), "Original full-screen PNG bytes");
        assertFalse(doc.select("details").stream().anyMatch(e -> e.hasAttr("open")), "Evidence defaults to collapsed");
    }
    @Test void fullOverflowAndIndivisibleRequiredValuesDeclareIncompleteWithoutChangingCapturedFailure() throws Exception {
        Path html = folder.resolve("incomplete.html");
        var config = new GameReportConfiguration(new GameReportPolicy(EvidencePolicy.DetailLevel.FULL, 2, 20, 256, 1, 8));
        assertThrows(IllegalStateException.class, () -> {
        try (var session = GameReportSession.open(html, config)) {
            var scenario = session.startScenario("Controlled failed invocation", "fixture", List.of("component"));
            try (var phase = scenario.startPhase("TEST BODY")) {
                try (var step = phase.startAssertionStep("Original failed comparison")) {
                    step.assertions(List.of(new AtfAssertion("Independent constant", "41", "42", false))); step.complete(AtfReportStatus.FAIL);
                }
                assertThrows(IllegalArgumentException.class, () -> GameReportPhase.optional(phase,
                        observation("Too much optional context", "x".repeat(9))));
                assertThrows(IllegalArgumentException.class, () -> phase.observation(new ReportObservation("Required JSON", List.of(),
                        List.of(new ReportObservation.Body("Required state", "{\"data\":\"" + "x".repeat(300) + "\"}", ReportObservation.Format.JSON)), List.of())));
                phase.complete(AtfReportStatus.FAIL);
            }
            scenario.complete(AtfReportStatus.FAIL);
        }
        });
        var doc = Jsoup.parse(Files.readString(html)); assertTrue(doc.text().contains("INCOMPLETE"));
        assertEquals("fail", doc.select("#test-view .test-list-item > li.test-item").first().attr("status"));
        assertEquals(1, doc.select("#test-view table.helm-validation-table > tbody > tr").size());
    }
    @Test void interruptedCallerRetainsCapturedRowsAndOriginalException() throws Exception {
        Path report = folder.resolve("interrupted.html");
        var original = new IllegalStateException("Controlled original caller failure");
        try (var session = GameReportSession.open(report)) {
            var scenario = session.startScenario("Interrupted scope", "fixture", List.of("component"));
            try (var phase = scenario.startPhase("TEST BODY")) {
                var observed = assertThrows(IllegalStateException.class, () -> {
                    try (var step = phase.startAssertionStep("Evaluated before caller failure")) {
                        step.assertions(List.of(new AtfAssertion("Captured mismatch", "41", "42", false)));
                        throw original;
                    }
                });
                assertSame(original, observed);
                phase.complete(AtfReportStatus.FAIL);
            }
            scenario.complete(AtfReportStatus.FAIL);
        }
        var doc = Jsoup.parse(Files.readString(report));
        assertEquals(1, doc.select("#test-view table.helm-validation-table > tbody > tr").size());
        assertTrue(doc.select("#test-view table.helm-validation-table").text().contains("42"));
    }
    @Test void selectionAndPresentationShareOneOptionalBudget() throws Exception {
        Path report=folder.resolve("shared-budget.html");
        var config=new GameReportConfiguration(new GameReportPolicy(EvidencePolicy.DetailLevel.AUTO,2,20,512,2,8));
        try(var session=GameReportSession.open(report,config)){
            var captured=new GameReportSession.OptionalContext(2,1,11,7);session.reserveOptionalContext(captured);
            assertThrows(IllegalStateException.class,()->session.reserveOptionalContext(captured));
            var scenario=session.startScenario("Shared optional budget","controlled",List.of("reporting-contract"));
            try(var phase=scenario.startPhase("TEST BODY")){
                GameReportPhase.optional(phase,observation("First optional context","A"));
                GameReportPhase.optional(phase,observation("Second optional context","SHOULD-OMIT"));
                phase.complete(AtfReportStatus.INFO);
            }
            scenario.complete(AtfReportStatus.INFO);
        }
        var doc=Jsoup.parse(Files.readString(report));assertFalse(doc.text().contains("SHOULD-OMIT"));
        assertTrue(doc.text().contains("4 / 2 / 2"));assertTrue(doc.text().contains("23 / 8 / 15"));
    }
    @Test void fieldGroupsKeepEveryOriginalRowAndTheFullLastValue() throws Exception {
        Path report=folder.resolve("fields.html");String last="x".repeat(300)+"LAST-FIELD-TAIL";
        var config=new GameReportConfiguration(new GameReportPolicy(EvidencePolicy.DetailLevel.AUTO,2,2,512,2,64));
        try(var session=GameReportSession.open(report,config)){
            var scenario=session.startScenario("Field projection","controlled",List.of("reporting-contract"));
            try(var phase=scenario.startPhase("TEST BODY")){
                var fields=new ArrayList<ReportField>();for(int i=0;i<4;i++)fields.add(new ReportField("Field "+i,"Value "+i));
                fields.add(new ReportField("Field 4",last));phase.observation(new ReportObservation("Captured fixed fields",fields,List.of(),List.of()));
                phase.complete(AtfReportStatus.INFO);
            }
            scenario.complete(AtfReportStatus.INFO);
        }
        String html=Files.readString(report);var doc=Jsoup.parse(html);var canonical=doc.select("#test-view .test-list-item > li.test-item").first();
        for(int i=0;i<5;i++){
            String name="FIELD "+i;var cells=canonical.select("table.helm-evidence-table > tbody > tr > td:first-child").stream()
                    .filter(cell->cell.text().equals(name)).toList();assertEquals(1,cells.size(),name);
            assertTrue(cells.get(0).closest("table").select("tbody > tr").size()<=2);
        }
        assertTrue(new GameReportTextAudit(html).contains(last));
    }
    private static void publish(GameReportSession session, int owner, byte[] png) {
        var scenario = session.startScenario("Controlled game " + owner, "controlled.GameComponentTest", "Offline fixture", List.of("component", "mock"));
        try (var phase = scenario.startPhase("TEST BODY")) {
            phase.observation(new ReportObservation("Setup and independent oracle", List.of(
                    new ReportField("Purpose", "Controlled publication contract; no game or Android execution"),
                    new ReportField("Expected source", "Independent fixed constants, with a deliberate final mismatch"),
                    new ReportField("Owner", Integer.toString(owner))), List.of(), List.of()));
            phase.observation(new ReportObservation("Required multi-line trace",List.of(),List.of(
                    new ReportObservation.Body("Full ordered trace",multiline(owner),ReportObservation.Format.TEXT)),List.of()));
            GameReportPhase.optional(phase, observation("Small optional note", "SMALL-OPTIONAL"));
            GameReportPhase.optional(phase, observation("Large optional note", "LARGE-OPTIONAL-" + "x".repeat(100)));
            var rows = new ArrayList<AtfAssertion>();
            for (int i = 0; i < 4; i++) rows.add(new AtfAssertion("Captured comparison " + i, "-" + i, "-" + i, true));
            rows.add(new AtfAssertion("Final comparison / owner " + owner, "x".repeat(300) + "expected-final-" + owner,
                    "x".repeat(300) + "actual-final-" + owner, false));
            try (var step = phase.startAssertionStep("Captured game comparisons")) { step.assertions(rows); step.complete(AtfReportStatus.FAIL); }
            phase.observation(new ReportObservation("Redacted setup", List.of(new ReportField("password", "never-publish-this-secret")), List.of(), List.of()));
            phase.complete(AtfReportStatus.FAIL);
        }
        try (var ui = UiExtentReportSession.adaptScenario(scenario,
                ReportEvidenceProfile.forScreenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT), "Controlled full-screen PNG")) {
            try (var phase = ui.startPhase("Captured media")) {
                phase.uiValidationAsMedia("Complete 320×480 display", "Synthetic viewer contract", Base64.getEncoder().encodeToString(png),
                        List.of(), List.of(), AtfReportStatus.INFO);
                phase.complete(AtfReportStatus.INFO);
            }
            ui.complete(AtfReportStatus.FAIL);
        }
    }
    private static ReportObservation observation(String title, String value) {
        return new ReportObservation(title, List.of(new ReportField("Note", value)), List.of(), List.of());
    }
    private static String multiline(int owner) {
        return ("owner="+owner+" before=41 actual=42; literal & &#13; ♠\r\n").repeat(37)+"final-owner="+owner;
    }
    private static byte[] png() throws Exception {
        var image = new BufferedImage(320, 480, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 480; y++) for (int x = 0; x < 320; x++)
            image.setRGB(x, y, y < 240 ? (x < 160 ? 0xCB2D3E : 0x2980B9) : (x < 160 ? 0x27AE60 : 0xF1C40F));
        var bytes = new ByteArrayOutputStream(); ImageIO.write(image, "png", bytes); return bytes.toByteArray();
    }
}
