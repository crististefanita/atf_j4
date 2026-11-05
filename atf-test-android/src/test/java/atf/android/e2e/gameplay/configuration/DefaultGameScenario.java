package atf.android.e2e.gameplay.configuration;

import static atf.android.e2e.support.flow.UiFlowCheckpoint.checkpoint;
import static atf.android.e2e.support.flow.WhistLabScenario.PACKAGE;
import static atf.android.e2e.support.flow.WhistLabScenario.artifacts;
import static atf.android.e2e.support.flow.WhistLabScenario.installedApp;

import static org.junit.jupiter.api.Assertions.assertTrue;

import atf.android.AdbClient;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/** Verifică configurația implicită și starea observată după pornire. */
public final class DefaultGameScenario {
    private DefaultGameScenario() {}

    public static void run(String ownerName) throws Exception {
        var installation = installedApp();
        AdbClient adb = installation.adb();

        Path artifacts = artifacts("default-oh-hell");
        AndroidUiDriver ui = new AndroidUiDriver(adb, artifacts);
        AndroidUiFlowTrace trace =
                new AndroidUiFlowTrace(
                        artifacts,
                        "whist-lab-default-oh-hell-deal",
                        "Whist Lab — default Oh Hell setup matches the observed four-player deal",
                        ownerName,
                        "atf.android.e2e",
                        PACKAGE,
                        List.of(
                                "android",
                                "api-28",
                                "parallel-app",
                                "e2e",
                                "oh-hell",
                                "deal-size"));
        ui.recordCapturesAs(trace::capture);
        trace.beginPhase("SETUP");
        ui.recordActionsAs(trace::recordAction);
        AtfReportStatus result = AtfReportStatus.FAIL;
        try {
            long started = System.nanoTime();
            String home = ui.restartToActivity(PACKAGE, ".MainActivity");
            assertTrue(ui.hasText(home, "New game"), "Whist Lab home screen was not exposed");
            ui.selectNamedSpinnerOption("interface-language", "English");
            ui.tapText("New game");
            String setup = ui.awaitText("Start game", Duration.ofSeconds(10));
            ui.tapDescription("Game type");
            String modeOptions = ui.awaitText("Descending", Duration.ofSeconds(10));
            List<String> modeChoiceLabels = ui.texts(modeOptions);
            boolean otherModesAvailable =
                    ui.hasText(modeOptions, "Descending")
                            && modeChoiceLabels.containsAll(
                                    List.of(
                                            "Oh Hell",
                                            "Romanian (1–8–1)",
                                            "Down & Up",
                                            "Long Romanian (8-1-8)",
                                            "Reverse Oh Hell"))
                            && !modeChoiceLabels.contains("Practice hand")
                            && !modeChoiceLabels.contains("Maximum");
            ui.back();
            setup = ui.awaitText("Start game", Duration.ofSeconds(10));
            List<String> setupLabels = ui.texts(setup);
            boolean defaultMode = setupLabels.contains("Oh Hell");
            List<Path> setupEvidence = ui.capture("whist-lab-default-oh-hell-setup");
            checkpoint(
                    trace,
                    "SETUP",
                    AtfGherkinKeyword.GIVEN,
                    "Verify Oh Hell is the local app's default and alternatives remain selectable",
                    "New game setup",
                    setupEvidence,
                    "open New game without changing the selected mode",
                    "Oh Hell selected; six public modes; Maximum and Practice hand absent",
                    "selected setup=" + setupLabels + "; opened mode options=" + modeChoiceLabels,
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    List.of(
                            new AtfAssertion(
                                    "Default ruleset matches the original app selection",
                                    "Oh Hell",
                                    Boolean.toString(defaultMode),
                                    defaultMode),
                            new AtfAssertion(
                                    "Alternative modes remain selectable",
                                    "Six public game types",
                                    modeChoiceLabels.toString(),
                                    otherModesAvailable)),
                    defaultMode && otherModesAvailable
                            ? AtfReportStatus.PASS
                            : AtfReportStatus.FAIL);
            trace.beginPhase("TEST BODY");
            assertTrue(
                    defaultMode && otherModesAvailable,
                    "Local game setup did not expose the expected modes; labels=" + setupLabels);

            started = System.nanoTime();
            ui.tapText("Start game");
            String bidding = ui.awaitText("Place bid", Duration.ofSeconds(15));
            String trumpDescription =
                    ui.descriptions(bidding).stream()
                            .filter(label -> label.startsWith("Trump"))
                            .findFirst()
                            .orElse("");
            boolean recognizedTrump =
                    trumpDescription.matches(
                            "Trump (?:suit only, no turned card: (?:spades|hearts|clubs|diamonds)|set by turned card: .+)");
            boolean tenCardHand = ui.hasDescription(bidding, "Your hand, 10 cards.");
            boolean biddingReady =
                    ui.hasText(bidding, "Hand 1 / 19")
                            && ui.hasText(bidding, "Place bid")
                            && ui.hasText(bidding, "Whist · Oh Hell")
                            && recognizedTrump
                            && tenCardHand;
            List<Path> biddingEvidence = ui.capture("whist-lab-default-oh-hell-bidding");
            checkpoint(
                    trace,
                    "TEST BODY",
                    AtfGherkinKeyword.THEN,
                    "Start the default four-player Oh Hell deal",
                    "4 players; Oh Hell; Normal AI",
                    biddingEvidence,
                    "tap Start game",
                    "Whist · Oh Hell; Hand 1 / 19; ten human cards; header trump tile; awaits a human bid",
                    "visible labels="
                            + ui.texts(bidding)
                            + "; original samples by seats 3/4/5/6 showed Diamonds/Hearts/Clubs/Spades",
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    List.of(
                            new AtfAssertion(
                                    "Default hand schedule is visible",
                                    "Hand 1 / 19",
                                    Boolean.toString(ui.hasText(bidding, "Hand 1 / 19")),
                                    ui.hasText(bidding, "Hand 1 / 19")),
                            new AtfAssertion(
                                    "Ten-card hand is exposed accessibly",
                                    "Your hand, 10 cards.",
                                    Boolean.toString(tenCardHand),
                                    tenCardHand),
                            new AtfAssertion(
                                    "An Oh Hell trump is visible in the persistent header tile",
                                    "accessible Trump descriptor",
                                    trumpDescription,
                                    recognizedTrump),
                            new AtfAssertion(
                                    "Human can place the first bid",
                                    "Place bid",
                                    Boolean.toString(ui.hasText(bidding, "Place bid")),
                                    ui.hasText(bidding, "Place bid"))),
                    biddingReady ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
            assertTrue(
                    biddingReady,
                    "Default Oh Hell first hand did not reach the current human bidding UI; labels="
                            + ui.texts(bidding)
                            + "; trump="
                            + trumpDescription
                            + "; hand="
                            + tenCardHand);
            result = AtfReportStatus.PASS;
            trace.finish(result);
        } catch (Throwable failure) {
            result = AtfReportStatus.FAIL;
            trace.fail(failure);
            if (failure instanceof Exception exception) throw exception;
            if (failure instanceof Error error) throw error;
            throw new RuntimeException(failure);
        } finally {
            trace.beginPhase("TEARDOWN");
            try {
                adb.run("shell", "am", "force-stop", PACKAGE).requireSuccess();
            } catch (Exception teardownFailure) {
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
}
