package atf.android.e2e.gameplay;

import atf.android.AdbClient;
import atf.android.e2e.activation.AndroidE2eTest;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.evidence.WhistTestEvidence;
import atf.android.e2e.oracle.romanian.RomanianMatchOracle;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

@AndroidE2eTest
class RomanianMenuOptionsE2eTest {
    private static final String APP = "ro.whistlab.app";
    private static final String SERIES_TOGGLE = "Romanian five-hand streaks (equal reward and penalty)";
    private static final Duration MENU_TIMEOUT = Duration.ofSeconds(12);

    @Test
    void localizedHelpAndFivePointSetupResumeThroughNativeRoutes() throws Exception {
        Path out = atf.android.e2e.support.flow.WhistLabScenario.configuredExternalPath("atf.match.evidenceDir");
        Files.createDirectories(out);
        var adb = AdbClient.fromSystemProperties();
        var ui = new AndroidUiDriver(adb, out);
        var trace = new AndroidUiFlowTrace(out, "romanian-menu-options",
                "Romanian series controls and exact resume after process restart",
                getClass().getSimpleName(), "atf.android.e2e", APP,
                List.of("android", "menu", "series", "persistence"));
        ui.recordActionsAs(trace::recordAction);
        ui.recordCapturesAs(trace::capture);
        trace.beginPhase("SETUP");
        try {
            restartApp(adb);
            ui.selectNamedSpinnerOption("interface-language", "Română");
            ui.awaitText("Joc nou", MENU_TIMEOUT);
            ui.tapText("Reguli de joc");
            ui.awaitText("AJUTOR — CUM SE JOACĂ", MENU_TIMEOUT);
            ui.revealText("Atu obligatoriu când nu ai culoarea");
            String help = ui.dump();
            var checks = new ArrayList<AtfAssertion>();
            RomanianMatchOracle.truth(checks, "Help scrolls to the localized rules section",
                    "Atu obligatoriu când nu ai culoarea", ui.texts(help).toString(),
                    ui.hasText(help, "Atu obligatoriu când nu ai culoarea"));
            ui.capture("help-ro");
            WhistTestEvidence.checkpoint(trace, out, "SETUP", "Open and scroll the native Help menu",
                    "help-ro", "Romanian Help entry and scroll", checks, List.of());

            ui.back();
            ui.awaitText("Joc nou", MENU_TIMEOUT);
            ui.selectNamedSpinnerOption("interface-language", "English");
            ui.awaitText("New game", MENU_TIMEOUT);
            JsonNode fivePointGame = startWithSeriesSelection(adb, ui, trace, out);
            verifyResume(adb, ui, trace, out, fivePointGame);
            ui.back();
            ui.awaitText("New game", MENU_TIMEOUT);
            trace.finish(AtfReportStatus.PASS);
        } catch (Throwable failure) {
            trace.fail(failure);
            throw failure;
        }
    }

    private static JsonNode startWithSeriesSelection(AdbClient adb, AndroidUiDriver ui,
            AndroidUiFlowTrace trace, Path out) throws Exception {
        trace.beginPhase("SETUP");
        ui.tapText("New game");
        ui.selectNamedSpinnerOption("game-type", "Romanian (1–8–1)");
        ui.selectNamedSpinnerOption("ai-level", "Hard");
        ui.revealText("±10 points");
        String setup = ui.dump();
        var checks = new ArrayList<AtfAssertion>();
        RomanianMatchOracle.truth(checks, "New game displays the ten-point default",
                "±10 points", ui.texts(setup).toString(), ui.hasText(setup, "±10 points"));
        RomanianMatchOracle.truth(checks, "Streak controls are separately labeled",
                "Five-hand streaks + named bonus selector", ui.texts(setup).toString(),
                ui.hasText(setup, "Five-hand streaks")
                        && ui.hasDescription(setup, SERIES_TOGGLE)
                        && ui.hasDescription(setup, "Five-hand reward / penalty"));

        String label = "menu-series-5";
        ui.selectNamedSpinnerOption("romanian-streak-value", "±5 points");
        ui.revealText("Start game");
        ui.capture(label + "-setup");
        WhistTestEvidence.checkpoint(trace, out, "SETUP", "Select Romanian series settings",
                label + "-setup", "No checkpoint editing or injected cards", checks, List.of());

        trace.beginPhase("TEST BODY");
        ui.tapText("Start game");
        JsonNode state = CompleteRomanianExpertE2eTest.stable(adb);
        checks = new ArrayList<>();
        RomanianMatchOracle.equal(checks, "The enabled series rule reaches the engine", true,
                state.get("streakBonuses"));
        RomanianMatchOracle.equal(checks, "The series value reaches the engine", 5,
                state.get("streakBonusPoints"));
        ui.capture(label + "-game");
        Files.writeString(out.resolve(label + "-state.json"), state.toPrettyString());
        WhistTestEvidence.checkpoint(trace, out, "TEST BODY", "Read the first saved match state",
                label + "-game", state.toPrettyString(), checks, List.of());
        return state;
    }

    private static void verifyResume(AdbClient adb, AndroidUiDriver ui, AndroidUiFlowTrace trace,
            Path out, JsonNode before) throws Exception {
        restartApp(adb);
        ui.tapText("Resume game");
        JsonNode resumed = CompleteRomanianExpertE2eTest.stable(adb);
        var checks = new ArrayList<AtfAssertion>();
        RomanianMatchOracle.equal(checks, "Process restart preserves the complete match state",
                before, resumed);
        ui.capture("menu-series-5-resume");
        WhistTestEvidence.checkpoint(trace, out, "TEST BODY", "Resume the same five-point match",
                "menu-series-5-resume", resumed.toPrettyString(), checks, List.of());
    }

    private static void restartApp(AdbClient adb) throws Exception {
        adb.run("shell", "am", "force-stop", APP).requireSuccess();
        adb.run("shell", "am", "start", "-W", "-n", APP + "/.MainActivity").requireSuccess();
    }
}
