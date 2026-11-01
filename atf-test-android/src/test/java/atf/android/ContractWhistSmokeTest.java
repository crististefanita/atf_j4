package atf.android;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
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
class ContractWhistSmokeTest {
    private static final String PACKAGE = "com.qsoft.android.whist";

    @Test
    void menusGameSetupAndBiddingFlowWork() throws Exception {
        assumeTrue(Boolean.getBoolean("atf.android.enabled"), "Enable device checks with -Datf.android.enabled=true");
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
        String nameDialog = ui.awaitText("Edit Your Name", Duration.ofSeconds(10));
        assertTrue(ui.hasText(nameDialog, "Enter your player name") && ui.hasText(nameDialog, "Player") && ui.hasText(nameDialog, "CANCEL"),
                "High Scores did not show the expected player-name prompt");
        evidence.addAll(ui.capture("high-scores-name-prompt"));
        ui.tapText("CANCEL");
        ui.restartToMainMenu(PACKAGE);

        ui.ensureMainMenu();
        ui.tapText("New Game");
        String configuration = ui.awaitText("START", Duration.ofSeconds(10));
        assertTrue(ui.hasText(configuration, "4"), "Default player count is not 4");
        assertTrue(ui.hasText(configuration, "Descending"), "Default game type is not Descending");
        assertTrue(ui.hasText(configuration, "Normal"), "Default AI level is not Normal");

        ui.tapSpinner(0);
        String players = ui.awaitText("6", Duration.ofSeconds(10));
        assertTrue(Set.copyOf(ui.texts(players)).containsAll(Set.of("3", "4", "5", "6")), "Player choices must be 3–6");
        ui.tapText("5");
        ui.awaitSpinnerSelection("5", Duration.ofSeconds(8));
        ui.tapSpinner(0); ui.awaitText("4", Duration.ofSeconds(8)); ui.tapText("4");
        ui.awaitSpinnerSelection("4", Duration.ofSeconds(8));

        ui.tapSpinner(1);
        String gameTypes = ui.awaitText("Reverse Oh Hell", Duration.ofSeconds(10));
        assertTrue(Set.copyOf(ui.texts(gameTypes)).containsAll(Set.of("Descending", "Down & Up", "Maximum", "Romanian", "Long Romanian", "Oh Hell", "Reverse Oh Hell")),
                "A game type is missing from its selector");
        ui.tapText("Oh Hell");
        ui.awaitSpinnerSelection("Oh Hell", Duration.ofSeconds(8));
        ui.tapSpinner(1); ui.awaitText("Descending", Duration.ofSeconds(8)); ui.tapText("Descending");
        ui.awaitSpinnerSelection("Descending", Duration.ofSeconds(8));

        ui.tapSpinner(2);
        String aiLevels = ui.awaitText("Hard", Duration.ofSeconds(10));
        assertTrue(Set.copyOf(ui.texts(aiLevels)).containsAll(Set.of("Normal", "Hard")), "AI level choices are incomplete");
        ui.tapText("Hard");
        ui.awaitSpinnerSelection("Hard", Duration.ofSeconds(8));
        ui.tapSpinner(2); ui.awaitText("Normal", Duration.ofSeconds(8)); ui.tapText("Normal");
        ui.awaitSpinnerSelection("Normal", Duration.ofSeconds(8));
        configuration = ui.dump();
        evidence.addAll(ui.capture("new-game-configuration"));
        assertTrue(ui.hasText(configuration, "4") && ui.hasText(configuration, "Descending") && ui.hasText(configuration, "Normal"),
                "The defaults could not be restored after exploring all selectors; visible texts=" + ui.texts(configuration));

        ui.tapText("START");
        String bidding = ui.awaitText("Bid 0", Duration.ofSeconds(20));
        assertTrue(ui.hasText(bidding, "Bid Other"), "Bidding controls are incomplete");
        evidence.addAll(ui.capture("bidding-phase"));

        ui.tapText("Bid 0");
        String play = ui.awaitAbsent("Bid 0", Duration.ofSeconds(20));
        assertTrue(!ui.hasText(play, "Bid Other"), "Bidding controls remained after submitting a bid");
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
        String checks = "Verified menu navigation, instructions/scoring text, high scores, all setup choices, starting a four-player game, bidding into play, one human card play, the intervening AI turn, and a second human card play. Bright card pixels before the first card tap="
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
        assumeTrue(Boolean.getBoolean("atf.android.enabled"), "Enable device checks with -Datf.android.enabled=true");
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
        ui.restartToMainMenu(PACKAGE);
        assertTrue(immediatePixels > 5_000 && settledPixels > 5_000,
                "Resumed game lost its visible cards after settling; immediate bright pixels=" + immediatePixels
                        + ", after six seconds=" + settledPixels);
    }

    private static void publishReport(Path artifacts, String installedPackagePath, List<Path> files,
            String reportName, String checks, AtfReportStatus outcome) throws Exception {
        Path report = artifacts.resolve(reportName);
        var settings = AtfReportConfigurationLoader.load().reportingSettings();
        try (var session = new HtmlReportSession(report, settings);
             var scenario = session.startScenario("Contract Whist Android acceptance", "atf.android", PACKAGE,
                     List.of("android", "emulator", "gameplay", "configuration"));
             var phase = scenario.startPhase("Menu, setup, bidding, and play transition")) {
            phase.observation(new ReportObservation("Device evidence",
                    List.of(new ReportField("Device serial", System.getProperty("atf.android.serial", System.getenv().getOrDefault("ATF_ANDROID_SERIAL", "emulator-5554"))),
                            new ReportField("Package", PACKAGE),
                            new ReportField("Installed APK", installedPackagePath.trim()),
                            new ReportField("Checks", checks)),
                    List.of(), files.stream().map(file -> {
                        try { return artifact(file); }
                        catch (Exception failure) { throw new java.io.UncheckedIOException(new java.io.IOException(failure)); }
                    }).toList()));
            phase.complete(outcome);
            scenario.complete(outcome);
        }
    }

    private static ReportObservation.Artifact artifact(Path file) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
        return new ReportObservation.Artifact(file.getFileName().toString(), file.toUri(), HexFormat.of().formatHex(digest));
    }
}
