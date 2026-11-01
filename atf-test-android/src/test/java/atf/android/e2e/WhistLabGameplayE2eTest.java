package atf.android.e2e;

import static org.junit.jupiter.api.Assertions.assertTrue;

import atf.android.AdbClient;
import atf.android.evidence.AndroidScreenshotEvidence;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import atf.reporting.model.ReportScreenshot;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.android.e2e.support.AndroidUiFlowTrace;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Runs the new app through the same Android adapter and inline-screenshot report contract. */
@AndroidE2eTest
public class WhistLabGameplayE2eTest {
    private static final String PACKAGE = "ro.whistlab.app";

    @Test
    void defaultFourPlayerOhHellMatchesTheObservedTenCardHandAndFlagsTrumpMismatch() throws Exception {
        AdbClient adb = AdbClient.fromSystemProperties();
        var packagePath = adb.run("shell", "pm", "path", PACKAGE);
        packagePath.requireSuccess();
        assertTrue(packagePath.output().contains("package:"), "Whist Lab is not installed: " + packagePath.output());

        Path artifacts = Path.of(System.getProperty("atf.android.artifacts", "target/android-artifacts"));
        AndroidUiDriver ui = new AndroidUiDriver(adb, artifacts);
        AndroidUiFlowTrace trace = new AndroidUiFlowTrace(artifacts, "whist-lab-default-oh-hell-deal",
                "Whist Lab — default Oh Hell setup matches the observed four-player deal",
                getClass().getSimpleName(), "atf.android.e2e", PACKAGE,
                List.of("android", "api-28", "parallel-app", "e2e", "oh-hell", "deal-size"));
        AtfReportStatus result = AtfReportStatus.FAIL;
        try {
            long started = System.nanoTime();
            String home = ui.restartToActivity(PACKAGE, ".MainActivity");
            assertTrue(ui.hasText(home, "New game"), "Whist Lab home screen was not exposed");
            ui.tapText("New game");
            String setup = ui.awaitText("Start game", Duration.ofSeconds(10));
            ui.tapSpinner(1);
            String modeOptions = ui.awaitText("Descending", Duration.ofSeconds(10));
            List<String> modeChoiceLabels = ui.texts(modeOptions);
            boolean otherModesAvailable = ui.hasText(modeOptions, "Descending")
                    && ui.hasText(modeOptions, "Practice hand");
            ui.back();
            setup = ui.awaitText("Start game", Duration.ofSeconds(10));
            List<String> setupLabels = ui.texts(setup);
            boolean defaultMode = setupLabels.contains("Oh Hell");
            List<Path> setupEvidence = ui.capture("whist-lab-default-oh-hell-setup");
            checkpoint(trace, "SETUP", AtfGherkinKeyword.GIVEN,
                    "Verify Oh Hell is the local app's default and alternatives remain selectable",
                    "New game setup", setupEvidence, "open New game without changing the selected mode",
                    "Oh Hell selected; Descending and Practice hand available",
                    "selected setup=" + setupLabels + "; opened mode options=" + modeChoiceLabels,
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    List.of(new AtfAssertion("Default ruleset matches the original app selection", "Oh Hell",
                                    Boolean.toString(defaultMode), defaultMode),
                            new AtfAssertion("Alternative modes remain selectable", "Descending and Practice hand",
                                    modeChoiceLabels.toString(), otherModesAvailable)),
                    defaultMode && otherModesAvailable ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
            assertTrue(defaultMode && otherModesAvailable,
                    "Local game setup did not expose the expected modes; labels=" + setupLabels);

            started = System.nanoTime();
            ui.tapText("Start game");
            String bidding = ui.awaitText("10 cards in this hand", Duration.ofSeconds(15));
            List<String> visibleTrumpLabels = ui.texts(bidding).stream()
                    .filter(label -> label.startsWith("Trump:")).toList();
            boolean recognizedTrump = visibleTrumpLabels.size() == 1 && Set.of(
                    "Trump: Spades", "Trump: Hearts", "Trump: Clubs", "Trump: Diamonds", "Trump: No trump")
                    .contains(visibleTrumpLabels.getFirst());
            boolean biddingReady = ui.hasText(bidding, "Place your bid")
                    && recognizedTrump;
            List<Path> biddingEvidence = ui.capture("whist-lab-default-oh-hell-bidding");
            checkpoint(trace, "TEST BODY", AtfGherkinKeyword.THEN,
                    "Start the default four-player Oh Hell deal",
                    "4 players; Oh Hell; Normal AI", biddingEvidence,
                    "tap Start game", "Hand 1 has 10 cards, a recognized trump value, and awaits a human bid; parity is unresolved",
                    "visible labels=" + ui.texts(bidding)
                            + "; original samples by seats 3/4/5/6 showed Diamonds/Hearts/Clubs/Spades",
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    List.of(new AtfAssertion("First hand size matches the observed original", "10 cards in this hand",
                                    Boolean.toString(ui.hasText(bidding, "10 cards in this hand")),
                                    ui.hasText(bidding, "10 cards in this hand")),
                            new AtfAssertion("A supported trump value is rendered", "one of Spades, Hearts, Clubs, Diamonds, No trumps",
                                    visibleTrumpLabels.toString(), recognizedTrump),
                            new AtfAssertion("Human can place the first bid", "Place your bid",
                                    Boolean.toString(ui.hasText(bidding, "Place your bid")), ui.hasText(bidding, "Place your bid")),
                            new AtfAssertion("Trump sequence parity remains an open rule question",
                                    "Repeat fresh sessions and observe a complete five-value cycle",
                                    "original first-hand samples for 3/4/5/6 seats: Diamonds/Hearts/Clubs/Spades; one 4-seat next hand: Diamonds; instructions list Spades, Hearts, Clubs, Diamonds, No trumps",
                                    false, AtfReportStatus.WARNING)),
                    biddingReady ? AtfReportStatus.WARNING : AtfReportStatus.FAIL);
            assertTrue(biddingReady, "Default Oh Hell first hand did not reach the human bidding state");
            result = AtfReportStatus.WARNING;
            trace.finish(result);
        } catch (Throwable failure) {
            result = AtfReportStatus.FAIL;
            trace.fail(failure);
            if (failure instanceof Exception exception) throw exception;
            if (failure instanceof Error error) throw error;
            throw new RuntimeException(failure);
        } finally {
            try { adb.run("shell", "am", "force-stop", PACKAGE).requireSuccess(); }
            catch (Exception teardownFailure) {
                boolean alreadyFailed = result == AtfReportStatus.FAIL;
                trace.fail(teardownFailure);
                if (!alreadyFailed) {
                    result = AtfReportStatus.FAIL;
                    throw teardownFailure;
                }
            }
            trace.finish(result);
        }
    }

    @Test
    void deterministicPracticeTrickScoresExactBidAndResumesAfterProcessDeathOnApi28() throws Exception {
        AdbClient adb = AdbClient.fromSystemProperties();
        var packagePath = adb.run("shell", "pm", "path", PACKAGE);
        packagePath.requireSuccess();
        assertTrue(packagePath.output().contains("package:"), "Whist Lab is not installed: " + packagePath.output());

        Path artifacts = Path.of(System.getProperty("atf.android.artifacts", "target/android-artifacts"));
        AndroidUiDriver ui = new AndroidUiDriver(adb, artifacts);
        List<Path> evidence = new ArrayList<>();
        AndroidUiFlowTrace trace = new AndroidUiFlowTrace(artifacts, "whist-lab-resume-exact-bid-trick",
                "Whist Lab — deterministic bid, process death, resume, and exact-bid score",
                getClass().getSimpleName(), "atf.android.e2e", PACKAGE,
                List.of("android", "api-28", "parallel-app", "e2e", "resume", "scoring"));
        AtfReportStatus outcome = AtfReportStatus.FAIL;
        String checks = "The deterministic game flow did not complete.";
        try {
            long actionStarted = System.nanoTime();
            String bidding = ui.restartToActivity(PACKAGE, ".MainActivity", "--ez", "debug_fixture", "true",
                    "--ez", "persist_fixture", "true");
            bidding = ui.awaitText("Place your bid", Duration.ofSeconds(10));
            assertTrue(ui.hasText(bidding, "Place your bid"), "Fixture did not open at its human bidding turn");
            assertTrue(ui.hasText(bidding, "Trump: Spades"), "Fixture trump should be Spades");
            List<Path> biddingEvidence = ui.capture("whist-lab-bidding");
            evidence.addAll(biddingEvidence);
            checkpoint(trace, "SETUP", AtfGherkinKeyword.GIVEN, "Launch deterministic 4-player fixture at bidding",
                    "debug_fixture=true; persist_fixture=true", biddingEvidence,
                    "force-stop and launch MainActivity with deterministic fixture",
                    "Place your bid; Trump: Spades", "visible labels=" + ui.texts(bidding),
                    Duration.ofNanos(System.nanoTime() - actionStarted).toMillis(),
                    List.of(new AtfAssertion("Fixture opens at human bidding turn", "Place your bid",
                                    Boolean.toString(ui.hasText(bidding, "Place your bid")), ui.hasText(bidding, "Place your bid")),
                            new AtfAssertion("Fixture trump is Spades", "Trump: Spades",
                                    Boolean.toString(ui.hasText(bidding, "Trump: Spades")), ui.hasText(bidding, "Trump: Spades"))),
                    AtfReportStatus.PASS);

            actionStarted = System.nanoTime();
            ui.selectSpinnerOption(0, "1");
            ui.tapText("Place bid");
            String play = ui.awaitText("A♥", Duration.ofSeconds(10));
            assertTrue(ui.hasText(play, "Your turn"), "Fixture did not reach the human play turn");
            assertTrue(play.contains("You  ·  Bid 1"), "Human bid was not retained into play");
            List<Path> playEvidence = ui.capture("whist-lab-play");
            evidence.addAll(playEvidence);
            checkpoint(trace, "TEST BODY", AtfGherkinKeyword.WHEN, "Select Bid 1 and reach the human A♥ turn",
                    "bid selector=1; Place bid", playEvidence, "select bid 1 and tap Place bid",
                    "Your turn with bid 1", "visible labels=" + ui.texts(play),
                    Duration.ofNanos(System.nanoTime() - actionStarted).toMillis(),
                    List.of(new AtfAssertion("Human reaches play turn", "Your turn",
                                    Boolean.toString(ui.hasText(play, "Your turn")), ui.hasText(play, "Your turn")),
                            new AtfAssertion("Bid remains attached to human seat", "You · Bid 1",
                                    Boolean.toString(play.contains("You  ·  Bid 1")), play.contains("You  ·  Bid 1"))),
                    AtfReportStatus.PASS);

            actionStarted = System.nanoTime();
            adb.run("shell", "am", "force-stop", PACKAGE).requireSuccess();
            adb.run("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivity").requireSuccess();
            String home = ui.awaitText("Resume game", Duration.ofSeconds(10));
            assertTrue(ui.hasText(home, "New game"), "App did not return to its home screen after process restart");
            ui.tapText("Resume game");
            String resumed = ui.awaitText("A♥", Duration.ofSeconds(10));
            assertTrue(ui.hasText(resumed, "Your turn"), "Saved game did not resume at the human play turn");
            assertTrue(resumed.contains("You  ·  Bid 1"), "Saved human bid was lost after process restart");
            List<Path> resumeEvidence = ui.capture("whist-lab-resume");
            evidence.addAll(resumeEvidence);
            checkpoint(trace, "TEST BODY", AtfGherkinKeyword.AND, "Force-stop the process and resume the same saved turn",
                    "Resume game", resumeEvidence, "force-stop; relaunch; tap Resume game",
                    "same human turn and Bid 1 restored", "visible labels=" + ui.texts(resumed),
                    Duration.ofNanos(System.nanoTime() - actionStarted).toMillis(),
                    List.of(new AtfAssertion("Resume restores human turn", "Your turn",
                                    Boolean.toString(ui.hasText(resumed, "Your turn")), ui.hasText(resumed, "Your turn")),
                            new AtfAssertion("Resume preserves bid", "You · Bid 1",
                                    Boolean.toString(resumed.contains("You  ·  Bid 1")), resumed.contains("You  ·  Bid 1"))),
                    AtfReportStatus.PASS);

            actionStarted = System.nanoTime();
            ui.tapText("A♥");
            String score = ui.awaitText("Score sheet", Duration.ofSeconds(12));
            String exactScore = "You | Bid 1 | Won 1 | +11 | Total: 11";
            assertTrue(ui.hasText(score, exactScore), "Exact-bid score row is incorrect; visible texts=" + ui.texts(score));
            List<Path> scoreEvidence = ui.capture("whist-lab-score");
            evidence.addAll(scoreEvidence);
            checkpoint(trace, "TEST BODY", AtfGherkinKeyword.THEN, "Play A♥ and verify exact-bid scoring",
                    "A♥; Score sheet", scoreEvidence, "play A♥; let AI finish the trick",
                    exactScore, "visible labels=" + ui.texts(score),
                    Duration.ofNanos(System.nanoTime() - actionStarted).toMillis(),
                    List.of(new AtfAssertion("Exact bid earns 10 bonus plus one trick point", exactScore,
                            String.join(" | ", ui.texts(score)), ui.hasText(score, exactScore))),
                    AtfReportStatus.PASS);
            checks = "API 28 fixture: human bids 1, the game process is force-stopped, and Resume game restores "
                    + "the human play turn and Bid 1; the human then plays A♥, wins the trick, and receives +11 "
                    + "for Total: 11. AI seats completed their turns and the final score sheet was visible.";
            outcome = AtfReportStatus.PASS;
        } catch (Throwable failure) {
            outcome = AtfReportStatus.FAIL;
            trace.fail(failure);
            if (failure instanceof Exception exception) throw exception;
            if (failure instanceof Error error) throw error;
            throw new RuntimeException(failure);
        } finally {
            try {
                List<Path> teardown = ui.capture("whist-lab-teardown");
                evidence.addAll(teardown);
                checkpoint(trace, "TEARDOWN", AtfGherkinKeyword.THEN,
                        "Retain the final local-app screen and accessibility tree", "Whist Lab Android app",
                        teardown, "capture final app state", "final score or failure screen retained",
                        "files=" + teardown.stream().map(file -> file.getFileName().toString()).toList(),
                        0, List.of(new AtfAssertion("Final UI evidence is retained", "PNG and XML", "2 files",
                                teardown.size() == 2)), AtfReportStatus.PASS);
            } catch (Exception teardownFailure) {
                boolean alreadyFailed = outcome == AtfReportStatus.FAIL;
                trace.fail(teardownFailure);
                if (!alreadyFailed) {
                    outcome = AtfReportStatus.FAIL;
                    throw teardownFailure;
                }
            }
            trace.finish(outcome);
            publishReport(artifacts, packagePath.output(), evidence, checks, outcome);
        }
    }

    private static void checkpoint(AndroidUiFlowTrace trace, String phase, AtfGherkinKeyword keyword,
            String title, String target, List<Path> evidence, String operation, String expected,
            String observed, long durationMillis, List<AtfAssertion> assertions, AtfReportStatus status) throws Exception {
        Path screenshot = evidence.stream().filter(file -> file.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".png"))
                .findFirst().orElse(null);
        List<Path> supporting = evidence.stream().filter(file -> file.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".xml")).toList();
        AtfUiActionRecord action = new AtfUiActionRecord(trace.nextActionSequence(), operation, target,
                expected, observed, Math.max(0, durationMillis), status, AtfUiEvidenceSource.ACTION);
        trace.checkpoint(phase, keyword.text(), title, target, screenshot, List.of(action), assertions, status, supporting);
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
