package atf.android.e2e.gameplay.presentation;

import static atf.android.e2e.support.flow.UiFlowCheckpoint.checkpoint;
import static atf.android.e2e.support.flow.WhistLabScenario.PACKAGE;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/** Configurează masa cu șase jucători și păstrează ierarhia folosită la pornire. */
final class SixPlayerSetupPhase {
    private SixPlayerSetupPhase() {}

    static String run(AndroidUiDriver ui, AndroidUiFlowTrace trace, List<Path> evidence) throws Exception {
        long started = System.nanoTime();
        String home = ui.restartToActivity(PACKAGE, ".MainActivity");
        boolean homeTitleVisible = ui.hasText(home, "Whist");
        String languageValue =
                ui.hasDescription(home, "Language")
                        ? ui.spinnerValue(home, "Language")
                        : ui.hasDescription(home, "Limba")
                                ? ui.spinnerValue(home, "Limba")
                                : "";
        boolean languageDropdownVisible =
                ui.hasClass(home, "android.widget.Spinner")
                        && List.of("English", "Română").contains(languageValue);
        boolean homeFitsWithoutScrolling = ui.allContentFitsViewport(home);
        List<Path> homeEvidence = ui.capture("whist-lab-presentation-home");
        evidence.addAll(homeEvidence);
        checkpoint(
                trace,
                "SETUP",
                AtfGherkinKeyword.GIVEN,
                "Open the concise home screen with language selection and all navigation visible",
                "Whist; Language; New game; Resume game; How to play",
                homeEvidence,
                "launch app and inspect current language selector",
                "home actions fit inside the first viewport",
                "title="
                        + homeTitleVisible
                        + "; language dropdown="
                        + languageDropdownVisible
                        + "; fits="
                        + homeFitsWithoutScrolling,
                Duration.ofNanos(System.nanoTime() - started).toMillis(),
                List.of(
                        new AtfAssertion(
                                "Home uses the concise Whist title",
                                "Whist",
                                Boolean.toString(homeTitleVisible),
                                homeTitleVisible),
                        new AtfAssertion(
                                "Interface language is selected from one dropdown",
                                "English or Română in one spinner",
                                languageValue,
                                languageDropdownVisible),
                        new AtfAssertion(
                                "Home navigation fits without scrolling",
                                "all content inside viewport",
                                Boolean.toString(homeFitsWithoutScrolling),
                                homeFitsWithoutScrolling)),
                AtfReportStatus.PASS);
        ui.selectNamedSpinnerOption("interface-language", "English");
        ui.tapText("New game");
        String setup = ui.awaitText("Start game", Duration.ofSeconds(10));
        boolean setupTitleVisible = ui.hasText(setup, "Game setup");
        ui.selectNamedSpinnerOption("player-count", "6");
        setup = ui.awaitText("Start game", Duration.ofSeconds(10));
        String selectedPlayerCount = ui.spinnerValue(setup, "Players");
        boolean setupFitsWithoutScrolling = ui.allContentFitsViewport(setup);
        List<Path> setupEvidence = ui.capture("whist-lab-presentation-six-player-setup");
        evidence.addAll(setupEvidence);
        checkpoint(
                trace,
                "SETUP",
                AtfGherkinKeyword.GIVEN,
                "Configure six players to produce the eight-card hand",
                "New game; Players = 6",
                setupEvidence,
                "select six players and start a new game",
                "six-player setup is ready",
                "visible labels=" + ui.texts(setup),
                Duration.ofNanos(System.nanoTime() - started).toMillis(),
                List.of(
                        new AtfAssertion(
                                "Six-player option is selected",
                                "6 players",
                                selectedPlayerCount + " players",
                                selectedPlayerCount.equals("6")),
                        new AtfAssertion(
                                "Start action remains available",
                                "Start game",
                                Boolean.toString(ui.hasText(setup, "Start game")),
                                ui.hasText(setup, "Start game")),
                        new AtfAssertion(
                                "Setup title is concise",
                                "Game setup",
                                Boolean.toString(setupTitleVisible),
                                setupTitleVisible),
                        new AtfAssertion(
                                "Setup controls fit without scrolling",
                                "all content inside viewport",
                                Boolean.toString(setupFitsWithoutScrolling),
                                setupFitsWithoutScrolling)),
                AtfReportStatus.PASS);
        return setup;
    }
}
