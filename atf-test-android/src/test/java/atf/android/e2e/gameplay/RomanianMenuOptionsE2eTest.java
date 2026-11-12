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
    void originalFiveNewDefaultTenAndClassicOptionPersistFromTheNormalSetupMenu() throws Exception {
        Path out = Path.of(System.getProperty("atf.match.evidenceDir")).toAbsolutePath();
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
            ui.selectNamedSpinnerOption("interface-language", "English");
            ui.tapText("How to play");
            verifyLocalizedHelp(ui, trace, out, "English", "New game", "How to play",
                    "Mandatory trump when void", "HELP — HOW TO PLAY", "help-en");
            verifyLocalizedHelp(ui, trace, out, "Română", "Joc nou", "Reguli de joc",
                    "Atu obligatoriu când nu ai culoarea", "AJUTOR — CUM SE JOACĂ", "help-ro");
            ui.back();
            ui.selectNamedSpinnerOption("interface-language", "English");
            ui.awaitText("New game", MENU_TIMEOUT);

            JsonNode fivePointGame = startWithSeriesSelection(adb, ui, trace, out, true);
            verifyResume(adb, ui, trace, out, fivePointGame);
            ui.back();
            ui.awaitText("New game", MENU_TIMEOUT);
            startWithSeriesSelection(adb, ui, trace, out, false);
            ui.back();
            ui.awaitText("New game", MENU_TIMEOUT);
            trace.finish(AtfReportStatus.PASS);
        } catch (Throwable failure) {
            trace.fail(failure);
            throw failure;
        }
    }

    private static void verifyLocalizedHelp(AndroidUiDriver ui, AndroidUiFlowTrace trace, Path out,
            String language, String newGame, String helpEntry, String ruleHeading, String helpTitle,
            String label) throws Exception {
        ui.selectNamedSpinnerOption("interface-language", language);
        ui.revealText(ruleHeading);
        String help = ui.dump();
        var checks = new ArrayList<AtfAssertion>();
        RomanianMatchOracle.truth(checks, "Help scrolls to the localized rules section", ruleHeading,
                ui.texts(help).toString(), ui.hasText(help, ruleHeading));
        ui.capture(label);
        WhistTestEvidence.checkpoint(trace, out, "SETUP", "Open the rules section in " + language,
                label, "Normal Help menu", checks, List.of());

        ui.back();
        String home = ui.awaitText(newGame, MENU_TIMEOUT);
        ui.tapText(helpEntry);
        String reopened = ui.awaitText(helpTitle, MENU_TIMEOUT);
        checks = new ArrayList<>();
        RomanianMatchOracle.truth(checks, "The home menu uses the selected language", newGame,
                ui.texts(home).toString(), ui.hasText(home, newGame));
        RomanianMatchOracle.truth(checks, "Help keeps the language when reopened", helpTitle,
                ui.texts(reopened).toString(), ui.hasText(reopened, helpTitle));
        ui.capture(label + "-reopen");
        WhistTestEvidence.checkpoint(trace, out, "SETUP", "Reopen Help in " + language,
                label + "-reopen", "Back to menu and reopen Help", checks, List.of());
    }

    private static JsonNode startWithSeriesSelection(AdbClient adb, AndroidUiDriver ui,
            AndroidUiFlowTrace trace, Path out, boolean seriesEnabled) throws Exception {
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

        int points = seriesEnabled ? 5 : 10;
        String label = seriesEnabled ? "menu-series-5" : "menu-series-0";
        if (seriesEnabled) {
            ui.selectNamedSpinnerOption("romanian-streak-value", "±5 points");
        } else {
            ui.tapDescription(SERIES_TOGGLE);
        }
        ui.revealText("Start game");
        ui.capture(label + "-setup");
        WhistTestEvidence.checkpoint(trace, out, "SETUP", "Select Romanian series settings",
                label + "-setup", "No checkpoint editing or injected cards", checks, List.of());

        trace.beginPhase("TEST BODY");
        ui.tapText("Start game");
        JsonNode state = CompleteRomanianExpertE2eTest.stable(adb);
        checks = new ArrayList<>();
        RomanianMatchOracle.equal(checks, "The toggle reaches the engine", seriesEnabled,
                state.get("streakBonuses"));
        RomanianMatchOracle.equal(checks, "The series value reaches the engine", points,
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
