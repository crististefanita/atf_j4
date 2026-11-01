package atf.android.e2e;

import static org.junit.jupiter.api.Assertions.assertTrue;

import atf.android.AdbClient;
import atf.android.evidence.AndroidScreenshotEvidence;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import atf.reporting.model.ReportScreenshot;
import atf.reporting.ui.UiExtentReportSession;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Explicit opt-in: never installs software or launches an app during the default Maven build. */
@AndroidE2eTest
public class ContractWhistGameplayE2eTest {
    private static final String PACKAGE = "com.qsoft.android.whist";

    @Test
    void menusGameSetupAndBiddingFlowWork() throws Exception {
        AdbClient adb = AdbClient.fromSystemProperties();
        var packagePath = adb.run("shell", "pm", "path", PACKAGE);
        packagePath.requireSuccess();
        assertTrue(packagePath.output().contains("package:"), "Package is not installed: " + packagePath.output());

        Path artifacts = Path.of(System.getProperty("atf.android.artifacts", "target/android-artifacts"));
        AndroidUiDriver ui = new AndroidUiDriver(adb, artifacts);
        List<Path> evidence = new ArrayList<>();

        ui.restartToMainMenu(PACKAGE);
        String menu = ui.dump();
        for (String label : List.of("New Game", "Resume Game", "Instructions", "High Scores", "Multiplayer"))
            assertTrue(ui.hasText(menu, label), "Main menu is missing " + label);
        evidence.addAll(ui.capture("main-menu"));

        ui.tapText("Instructions");
        String instructions = ui.awaitContains("Bidding Phase", Duration.ofSeconds(10));
        assertTrue(instructions.contains("Play Phase"), "Instructions do not describe the play phase");
        assertTrue(instructions.contains("additional 10 points"), "Instructions do not describe scoring");
        evidence.addAll(ui.capture("instructions"));

        ui.ensureMainMenu();
        ui.tapText("High Scores");
        String nameDialog = null;
        try {
            nameDialog = ui.awaitText("Edit Your Name", Duration.ofSeconds(10));
        } catch (IllegalStateException noFirstRunPrompt) {
            // This prompt is one-time state: Cancel may persist the default player name across test runs.
        }
        if (nameDialog != null) {
            assertTrue(ui.hasText(nameDialog, "Enter your player name") && ui.hasText(nameDialog, "Player") && ui.hasText(nameDialog, "CANCEL"),
                    "High Scores showed an incomplete player-name prompt");
            evidence.addAll(ui.capture("high-scores-name-prompt"));
            ui.tapText("CANCEL");
        } else {
            // This prompt is one-time state: Cancel may persist the default player name across test runs.
            String highScoresState = ui.dump();
            assertTrue(ui.hasText(highScoresState, "New Game") && ui.hasText(highScoresState, "Resume Game"),
                    "High Scores did not show its first-run name prompt and did not return to the menu");
            evidence.addAll(ui.capture("high-scores-existing-player"));
        }
        ui.restartToMainMenu(PACKAGE);

        ui.ensureMainMenu();
        ui.tapText("New Game");
        String configuration = ui.awaitText("START", Duration.ofSeconds(10));
        String initialPlayerCount = selectedChoice(ui, configuration, Set.of("3", "4", "5", "6"), "player count");
        String initialGameType = selectedChoice(ui, configuration,
                Set.of("Descending", "Down & Up", "Maximum", "Romanian", "Long Romanian", "Oh Hell", "Reverse Oh Hell"),
                "game type");
        String initialAiLevel = selectedChoice(ui, configuration, Set.of("Normal", "Hard"), "AI level");

        ui.tapSpinner(0);
        String players = ui.awaitText("6", Duration.ofSeconds(10));
        assertTrue(Set.copyOf(ui.texts(players)).containsAll(Set.of("3", "4", "5", "6")), "Player choices must be 3–6");
        ui.back();

        ui.tapSpinner(1);
        String gameTypes = ui.awaitText("Reverse Oh Hell", Duration.ofSeconds(10));
        assertTrue(Set.copyOf(ui.texts(gameTypes)).containsAll(Set.of("Descending", "Down & Up", "Maximum", "Romanian", "Long Romanian", "Oh Hell", "Reverse Oh Hell")),
                "A game type is missing from its selector");
        ui.back();

        ui.tapSpinner(2);
        String aiLevels = ui.awaitText("Hard", Duration.ofSeconds(10));
        assertTrue(Set.copyOf(ui.texts(aiLevels)).containsAll(Set.of("Normal", "Hard")), "AI level choices are incomplete");
        ui.back();
        configuration = ui.dump();
        evidence.addAll(ui.capture("new-game-configuration"));
        assertTrue(selectedChoice(ui, configuration, Set.of("3", "4", "5", "6"), "player count")
                        .equals(initialPlayerCount)
                        && selectedChoice(ui, configuration,
                                Set.of("Descending", "Down & Up", "Maximum", "Romanian", "Long Romanian", "Oh Hell", "Reverse Oh Hell"),
                                "game type").equals(initialGameType)
                        && selectedChoice(ui, configuration, Set.of("Normal", "Hard"), "AI level").equals(initialAiLevel),
                "Opening setup selectors changed the saved choices; visible texts=" + ui.texts(configuration));

        ui.tapText("START");
        String bidding = ui.awaitText("Bid 0", Duration.ofSeconds(20));
        long visibleBidChoices = ui.texts(bidding).stream().filter(text -> text.matches("Bid \\d+")).count();
        assertTrue(visibleBidChoices >= 2 && ui.hasClass(bidding, "android.widget.Button"),
                "Bidding screen did not expose multiple numeric bid choices; visible texts=" + ui.texts(bidding));
        evidence.addAll(ui.capture("bidding-phase"));

        ui.tapText("Bid 0");
        String play = ui.awaitAbsent("Bid 0", Duration.ofSeconds(20));
        assertTrue(!ui.hasText(play, "Bid Other"), "An alternate bidding control remained after submitting a bid");
        Thread.sleep(4_000); // Let the app finish initial play animations before taking the comparison baseline.
        List<Path> playEvidence = ui.capture("play-phase");
        evidence.addAll(playEvidence);
        long visibleCardPixels = ui.countBrightNeutralPixels(artifacts.resolve("play-phase.png"), 0.55);
        if (visibleCardPixels <= 5_000) {
            publishReport(artifacts, packagePath.output(), evidence,
                    "android-gameplay.html",
                    "Bidding ended, but the play screen did not contain the expected visible card surfaces; bright card pixels=" + visibleCardPixels,
                    AtfReportStatus.FAIL);
            ui.back();
        }
        assertTrue(visibleCardPixels > 5_000, "Play phase did not visibly render cards; neutral bright pixel count=" + visibleCardPixels);

        long handAreaChangedPixels = ui.tapUntilHandChanges(
                artifacts.resolve("play-phase.png"), artifacts.resolve("after-card-tap.png"));
        List<Path> cardTapEvidence = ui.capture("after-card-tap");
        evidence.addAll(cardTapEvidence);
        long cardPixelsAfterTap = ui.countBrightNeutralPixels(artifacts.resolve("after-card-tap.png"), 0.55);
        Thread.sleep(8_000); // Allow the AI seats to finish the trick and the next player turn to settle.
        evidence.addAll(ui.capture("ai-turns-settled"));
        long secondHandAreaChangedPixels = ui.tapUntilHandChanges(
                artifacts.resolve("ai-turns-settled.png"), artifacts.resolve("after-second-card-tap.png"));
        evidence.addAll(ui.capture("after-second-card-tap"));
        long cardPixelsAfterSecondTap = ui.countBrightNeutralPixels(artifacts.resolve("after-second-card-tap.png"), 0.55);
        String checks = "Verified menu navigation, instructions/scoring text, high scores, all setup choices, restoring the starting settings, starting a game, numeric bid choices into play, one human card play, the intervening AI turn, and a second human card play. Bright card pixels before the first card tap="
                + visibleCardPixels + "; after the tap=" + cardPixelsAfterTap
                + "; changed hand/play-area pixels after the first tap=" + handAreaChangedPixels
                + "; changed hand/play-area pixels after the second tap=" + secondHandAreaChangedPixels
                + "; remaining bright card pixels=" + cardPixelsAfterSecondTap + ".";
        AtfReportStatus outcome = cardPixelsAfterTap > 5_000 && handAreaChangedPixels > 3_000
                && secondHandAreaChangedPixels > 3_000 && cardPixelsAfterSecondTap > 5_000
                ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
        publishReport(artifacts, packagePath.output(), evidence, "android-gameplay.html", checks, outcome);
        ui.restartToMainMenu(PACKAGE);
        assertTrue(cardPixelsAfterTap > 5_000 && handAreaChangedPixels > 3_000
                        && secondHandAreaChangedPixels > 3_000 && cardPixelsAfterSecondTap > 5_000,
                "Could not verify two human card plays through the AI game flow; first bright pixels=" + cardPixelsAfterTap
                        + ", first changed pixels=" + handAreaChangedPixels + ", second changed pixels="
                        + secondHandAreaChangedPixels + ", second bright pixels=" + cardPixelsAfterSecondTap);
    }

    @Test
    void resumedGameRemainsVisibleAfterSettling() throws Exception {
        AdbClient adb = AdbClient.fromSystemProperties();
        var packagePath = adb.run("shell", "pm", "path", PACKAGE);
        packagePath.requireSuccess();
        assertTrue(packagePath.output().contains("package:"), "Package is not installed: " + packagePath.output());

        Path artifacts = Path.of(System.getProperty("atf.android.artifacts", "target/android-artifacts"));
        AndroidUiDriver ui = new AndroidUiDriver(adb, artifacts);
        List<Path> evidence = new ArrayList<>();
        ui.restartToMainMenu(PACKAGE);
        ui.tapText("Resume Game");
        Thread.sleep(2_000);
        evidence.addAll(ui.capture("resume-immediate"));
        long immediatePixels = ui.countBrightNeutralPixels(artifacts.resolve("resume-immediate.png"), 0.55);
        Thread.sleep(6_000);
        evidence.addAll(ui.capture("resume-settled"));
        long settledPixels = ui.countBrightNeutralPixels(artifacts.resolve("resume-settled.png"), 0.55);

        String checks = "Resume immediate bright card pixels=" + immediatePixels
                + "; after settling for six seconds=" + settledPixels + ".";
        AtfReportStatus outcome = immediatePixels > 5_000 && settledPixels > 5_000
                ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
        publishReport(artifacts, packagePath.output(), evidence, "android-resume-rendering.html", checks, outcome);
        assertTrue(immediatePixels > 5_000 && settledPixels > 5_000,
                "Resumed game lost its visible cards after settling; immediate bright pixels=" + immediatePixels
                        + ", after six seconds=" + settledPixels);
    }

    private static void publishReport(Path artifacts, String installedPackagePath, List<Path> files,
            String reportName, String checks, AtfReportStatus outcome) throws Exception {
        Path report = artifacts.resolve(reportName);
        var settings = AtfReportConfigurationLoader.load().reportingSettings();
        try (var session = new ExtentReportSession(report, settings)) {
            AtfReportPorts.Scenario rawScenario = session.startScenario(
                    "Contract Whist Android acceptance", "atf.android.e2e", PACKAGE,
                    List.of("android", "emulator", "gameplay", "configuration"));
            try (var details = rawScenario.startPhase("Device metadata and UI hierarchy artifacts")) {
                details.observation(new ReportObservation("Android run details",
                        List.of(new ReportField("Device serial", System.getProperty("atf.android.serial", System.getenv().getOrDefault("ATF_ANDROID_SERIAL", "emulator-5554"))),
                                new ReportField("Package", PACKAGE),
                                new ReportField("Installed APK", installedPackagePath.trim()),
                                new ReportField("Checks", checks)),
                        List.of(), files.stream().filter(file -> !file.getFileName().toString().toLowerCase().endsWith(".png"))
                                .map(file -> {
                                    try { return artifact(file); }
                                    catch (Exception failure) { throw new java.io.UncheckedIOException(new java.io.IOException(failure)); }
                                }).toList()));
                details.complete(outcome);
            }
            var scenario = UiExtentReportSession.adaptScenario(rawScenario,
                    ReportEvidenceProfile.forScreenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT),
                    "Contract Whist Android acceptance");
            try (var phase = scenario.startPhase("Android UI screenshot checkpoints")) {
                for (Path image : files.stream().filter(file -> file.getFileName().toString().toLowerCase().endsWith(".png")).toList()) {
                    ReportScreenshot screenshot = AndroidScreenshotEvidence.from(image);
                    phase.uiValidation(screenshot.title(), "Android emulator", screenshot.base64Png(),
                            List.of(), AtfReportStatus.PASS);
                }
                phase.complete(outcome);
            }
            scenario.complete(outcome);
        }
    }

    private static String selectedChoice(AndroidUiDriver ui, String xml, Set<String> allowed, String field)
            throws Exception {
        List<String> selected = ui.texts(xml).stream().filter(allowed::contains).distinct().toList();
        assertTrue(selected.size() == 1,
                "Could not identify the current " + field + " selection from visible texts " + ui.texts(xml));
        return selected.get(0);
    }

    private static ReportObservation.Artifact artifact(Path file) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
        return new ReportObservation.Artifact(file.getFileName().toString(), file.toUri(), HexFormat.of().formatHex(digest));
    }
}
