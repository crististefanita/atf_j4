package atf.android.e2e;

import atf.android.AdbClient;
import atf.android.e2e.support.AndroidUiFlowReportPublisher;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AndroidE2eTest
class WhistMenuLayoutE2eTest {
    @Test void normalMenusFitAndLanguageRulesAndScoresRemainReachable() throws Exception {
        var adb = AdbClient.fromSystemProperties();
        var output = Path.of(System.getProperty("atf.android.artifacts")).resolve("menu-layout");
        var ui = new AndroidUiDriver(adb, output);
        var trace = new AndroidUiFlowTrace(output, "menu-layout", "Whist — normal navigation, EN/RO and compact setup",
                getClass().getSimpleName(), "atf.android.e2e", "ro.whistlab.app", List.of("android", "e2e", "menu", "EN", "RO"));
        ui.recordActionsAs(trace::recordAction);
        String size = prior(adb.run("shell", "wm", "size").output(), "size");
        String density = prior(adb.run("shell", "wm", "density").output(), "density");
        var evidence = new ArrayList<Path>();
        var outcome = AtfReportStatus.FAIL;
        try {
            for (String profile : List.of("720x1280", "794x1412", "884x1572")) {
                adb.run("shell", "wm", "size", profile).requireSuccess();
                adb.run("shell", "wm", "density", "360").requireSuccess();
                ui.restartToActivity("ro.whistlab.app", ".MainActivity", "--es", "debug_language", "EN");
                for (boolean ro : List.of(false, true)) {
                    ui.selectNamedSpinnerOption("interface-language", ro ? "Română" : "English");
                    String newGame = ro ? "Joc nou" : "New game";
                    String xml = ui.awaitText(newGame, Duration.ofSeconds(10));
                    String id = profile + (ro ? "-RO" : "-EN");
                    record(ui, trace, evidence, id + "-home", List.of(check("Home without scrolling", ui.allContentFitsViewport(xml)),
                            check("Language is a named dropdown", ui.spinnerValue(xml, ro ? "Limba" : "Language").equals(ro ? "Română" : "English"))));
                    ui.tapText(newGame);
                    ui.selectNamedSpinnerOption("game-type", ro ? "Românesc (1–8–1)" : "Romanian (1–8–1)");
                    xml = ui.dump();
                    String start = ro ? "Începe partida" : "Start game";
                    String back = ro ? "Înapoi" : "Back";
                    ui.capture(id + "-setup");
                    var backBounds = ui.buttonBounds(xml, back);
                    record(ui, trace, evidence, id + "-setup", List.of(
                            check("Start and Back visible together", ui.hasText(xml, start) && ui.hasText(xml, back)),
                            check("Back has its full 48 dp hit target", backBounds.bottom() - backBounds.top() >= 108),
                            check("Setup content fits the first viewport", ui.allContentFitsViewport(xml)),
                            check("Series selector retains default ten", ui.spinnerValue(xml, ro ? "Bonus pentru cinci contracte exacte eligibile" : "Five-hand exact-contract bonus")
                                    .equals(ro ? "10 puncte" : "10 points"))));
                    ui.tapText(ro ? "Reguli și program" : "Rules & schedule");
                    xml = ui.dump();
                    record(ui, trace, evidence, id + "-rules", List.of(check("Full schedule available on demand",
                            ui.hasTextContaining(xml, ro ? "Cărți pe mână" : "Cards per hand") && ui.hasTextContaining(xml, "24"))));
                    ui.tapText(ro ? "ÎNCHIDE" : "CLOSE");
                    ui.tapText(start);
                    ui.awaitText(ro ? "Licitează" : "Place bid", Duration.ofSeconds(15));
                    ui.tapDescription(ro ? "Meniu joc" : "Game menu");
                    ui.tapText(ro ? "Scorul partidei" : "Score sheet");
                    xml = ui.dump();
                    record(ui, trace, evidence, id + "-score", List.of(
                            check("Bid, hand points and total are localized", ui.hasText(xml, ro ? "Lic." : "Bid")
                                    && ui.hasText(xml, ro ? "Pct." : "Hand") && ui.hasText(xml, "Total")),
                            check("The score sheet can be closed", ui.hasText(xml, ro ? "ÎNCHIDE" : "CLOSE"))));
                    ui.tapText(ro ? "ÎNCHIDE" : "CLOSE");
                    ui.tapDescription(ro ? "Meniu joc" : "Game menu");
                    ui.tapText(ro ? "Meniu principal" : "Main menu");
                    xml = ui.awaitText(newGame, Duration.ofSeconds(10));
                    assertTrue(ui.hasText(xml, ro ? "Reia partida" : "Resume game"));
                }
            }
            outcome = AtfReportStatus.PASS;
        } catch (Throwable failure) { trace.fail(failure); throw failure; }
        finally {
            try {
                adb.run("shell", "wm", "size", size).requireSuccess();
                adb.run("shell", "wm", "density", density).requireSuccess();
                adb.run("shell", "am", "force-stop", "ro.whistlab.app").requireSuccess();
            } catch (Throwable failure) { outcome = AtfReportStatus.FAIL; trace.fail(failure); throw failure; }
            finally {
                trace.finish(outcome); evidence.add(trace.manifest());
                AndroidUiFlowReportPublisher.publish(output.resolve("menu-layout.html"), "ro.whistlab.app", System.getProperty("atf.android.serial"),
                        System.getProperty("atf.android.api"), "Normal menu navigation at 320, 353 and 393 dp; no injected game state.", evidence, trace);
            }
        }
    }

    private static void record(AndroidUiDriver ui, AndroidUiFlowTrace trace, List<Path> evidence, String id, List<AtfAssertion> checks) throws Exception {
        var capture = ui.capture(id); evidence.addAll(capture);
        boolean passed = checks.stream().allMatch(AtfAssertion::passed);
        var status = passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
        trace.checkpoint("TEST BODY", trace.phaseSnapshots().isEmpty() ? "GIVEN" : "AND", id, "Current screen",
                capture.stream().filter(path -> path.toString().endsWith(".png")).findFirst().orElseThrow(),
                List.of(new AtfUiActionRecord(trace.nextActionSequence(), "Inspect visible menu", id,
                        "Readable controls, complete content and working navigation", passed ? "All comparisons passed" : "Failed comparison retained",
                        0, status, AtfUiEvidenceSource.OBSERVE)), checks, status,
                capture.stream().filter(path -> path.toString().endsWith(".xml")).toList());
        for (var check : checks) assertTrue(check.passed(), id + ": " + check.name());
    }
    private static AtfAssertion check(String name, boolean value) { return new AtfAssertion(name, "true", Boolean.toString(value), value); }
    private static String prior(String output, String name) {
        var match = Pattern.compile("(?m)^Override " + name + ":\\s*(\\S+)").matcher(output);
        return match.find() ? match.group(1) : "reset";
    }
}
