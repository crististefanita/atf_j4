package atf.android.e2e;

import static org.junit.jupiter.api.Assertions.assertTrue;

import atf.android.AdbClient;
import atf.android.evidence.AndroidScreenshotEvidence;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.contract.AtfReportPorts;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Runs the new app through the same Android adapter and inline-screenshot report contract. */
@AndroidE2eTest
public class WhistLabGameplayE2eTest {
    private static final String PACKAGE = "ro.whistlab.app";

    @Test
    void deterministicPracticeTrickScoresExactBidAndResumesAfterProcessDeathOnApi28() throws Exception {
        AdbClient adb = AdbClient.fromSystemProperties();
        var packagePath = adb.run("shell", "pm", "path", PACKAGE);
        packagePath.requireSuccess();
        assertTrue(packagePath.output().contains("package:"), "Whist Lab is not installed: " + packagePath.output());

        Path artifacts = Path.of(System.getProperty("atf.android.artifacts", "target/android-artifacts"));
        AndroidUiDriver ui = new AndroidUiDriver(adb, artifacts);
        List<Path> evidence = new ArrayList<>();
        AtfReportStatus outcome = AtfReportStatus.FAIL;
        String checks = "The deterministic game flow did not complete.";
        try {
            String bidding = ui.restartToActivity(PACKAGE, ".MainActivity", "--ez", "debug_fixture", "true",
                    "--ez", "persist_fixture", "true");
            assertTrue(ui.hasText(bidding, "Place your bid"), "Fixture did not open at its human bidding turn");
            assertTrue(ui.hasText(bidding, "Trump: Spades"), "Fixture trump should be Spades");
            evidence.addAll(ui.capture("whist-lab-bidding"));

            ui.selectSpinnerOption(0, "1");
            ui.tapText("Place bid");
            String play = ui.awaitText("A♥", Duration.ofSeconds(10));
            assertTrue(ui.hasText(play, "Your turn"), "Fixture did not reach the human play turn");
            assertTrue(play.contains("You  ·  Bid 1"), "Human bid was not retained into play");
            evidence.addAll(ui.capture("whist-lab-play"));

            adb.run("shell", "am", "force-stop", PACKAGE).requireSuccess();
            adb.run("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivity").requireSuccess();
            String home = ui.awaitText("Resume game", Duration.ofSeconds(10));
            assertTrue(ui.hasText(home, "New game"), "App did not return to its home screen after process restart");
            ui.tapText("Resume game");
            String resumed = ui.awaitText("A♥", Duration.ofSeconds(10));
            assertTrue(ui.hasText(resumed, "Your turn"), "Saved game did not resume at the human play turn");
            assertTrue(resumed.contains("You  ·  Bid 1"), "Saved human bid was lost after process restart");
            evidence.addAll(ui.capture("whist-lab-resume"));

            ui.tapText("A♥");
            String score = ui.awaitText("Score sheet", Duration.ofSeconds(12));
            String exactScore = "You | Bid 1 | Won 1 | +11 | Total: 11";
            assertTrue(ui.hasText(score, exactScore), "Exact-bid score row is incorrect; visible texts=" + ui.texts(score));
            evidence.addAll(ui.capture("whist-lab-score"));
            checks = "API 28 fixture: human bids 1, the game process is force-stopped, and Resume game restores "
                    + "the human play turn and Bid 1; the human then plays A♥, wins the trick, and receives +11 "
                    + "for Total: 11. AI seats completed their turns and the final score sheet was visible.";
            outcome = AtfReportStatus.PASS;
        } finally {
            publishReport(artifacts, packagePath.output(), evidence, checks, outcome);
        }
    }

    private static void publishReport(Path artifacts, String installedPackage, List<Path> files,
            String checks, AtfReportStatus outcome) throws Exception {
        Path report = artifacts.resolve("whist-lab-gameplay.html");
        var settings = AtfReportConfigurationLoader.load().reportingSettings();
        try (var session = new ExtentReportSession(report, settings)) {
            AtfReportPorts.Scenario raw = session.startScenario(
                    "Whist Lab Android exact-bid gameplay", "atf.android.e2e", PACKAGE,
                    List.of("android", "api-28", "practice-trick", "scoring"));
            try (var details = raw.startPhase("Device details, game assertions, and UI hierarchy")) {
                details.observation(new ReportObservation("Deterministic practice-hand acceptance",
                        List.of(new ReportField("Device", System.getProperty("atf.android.serial", "emulator-5554")),
                                new ReportField("Android API", "28"),
                                new ReportField("Package", PACKAGE),
                                new ReportField("Installed APK", installedPackage.trim()),
                                new ReportField("Verified behavior", checks)),
                        List.of(), files.stream()
                                .filter(file -> !file.getFileName().toString().toLowerCase().endsWith(".png"))
                                .map(WhistLabGameplayE2eTest::artifactUnchecked).toList()));
                details.complete(outcome);
            }
            var ui = UiExtentReportSession.adaptScenario(raw,
                    ReportEvidenceProfile.forScreenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT),
                    "Whist Lab Android exact-bid gameplay");
            try (var phase = ui.startPhase("Inline Android UI screenshots")) {
                for (Path image : files.stream()
                        .filter(file -> file.getFileName().toString().toLowerCase().endsWith(".png")).toList()) {
                    ReportScreenshot screenshot = AndroidScreenshotEvidence.from(image);
                    phase.uiValidation(screenshot.title(), "Android emulator", screenshot.base64Png(),
                            List.of(), outcome);
                }
                phase.complete(outcome);
            }
            raw.complete(outcome);
        }
        assertTrue(Files.isRegularFile(report) && Files.size(report) > 2_000,
                "Whist Lab Extent report was not published: " + report);
        String html = Files.readString(report);
        if (outcome == AtfReportStatus.PASS) {
            assertTrue(html.contains("Total: 11"), "The Extent report omitted the verified final score");
            assertTrue(html.contains("data:image/png;base64,"), "Screenshots must be inline in the Extent report");
            for (Path image : files.stream()
                    .filter(file -> file.getFileName().toString().toLowerCase().endsWith(".png")).toList()) {
                ReportScreenshot screenshot = AndroidScreenshotEvidence.from(image);
                assertTrue(html.contains(screenshot.base64Png()),
                        "Extent report is missing screenshot payload " + screenshot.title());
            }
        }
    }

    private static ReportObservation.Artifact artifactUnchecked(Path file) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
            return new ReportObservation.Artifact(file.getFileName().toString(), file.toUri(), HexFormat.of().formatHex(digest));
        } catch (Exception failure) {
            throw new IllegalStateException("Could not attach Whist Lab evidence " + file, failure);
        }
    }
}
