package atf.android.e2e;

import static org.junit.jupiter.api.Assertions.assertTrue;

import atf.android.AdbClient;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.android.e2e.support.AndroidUiFlowReportPublisher;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import org.junit.jupiter.api.Test;

/** Runs the new app through the same Android adapter and inline-screenshot report contract. */
@AndroidE2eTest
public class WhistLabGameplayE2eTest {
    private static final String PACKAGE = "ro.whistlab.app";
    private static final Pattern OVERRIDE_DENSITY = Pattern.compile("(?m)^Override density:\\s*(\\d+)");
    private static final Pattern PHYSICAL_DENSITY = Pattern.compile("(?m)^Physical density:\\s*(\\d+)");
    private static final Pattern OVERRIDE_SIZE = Pattern.compile("(?m)^Override size:\\s*(\\d+)x(\\d+)");
    private static final Pattern PHYSICAL_SIZE = Pattern.compile("(?m)^Physical size:\\s*(\\d+)x(\\d+)");
    private static final Pattern SIX_PLAYER_EIGHT_CARD_BID_SUMMARY = Pattern.compile(
            "Bids submitted (\\d+)/6 · Bid total (-?\\d+)/8");

    @Test
    void sixPlayerEightCardHandFitsOneRowAndMenuKeepsScoreAndHomeAccessible() throws Exception {
        AdbClient adb = AdbClient.fromSystemProperties();
        var packagePath = adb.run("shell", "pm", "path", PACKAGE);
        packagePath.requireSuccess();
        assertTrue(packagePath.output().contains("package:"), "Whist Lab is not installed: " + packagePath.output());

        Path artifacts = artifacts("table-presentation");
        AndroidUiDriver ui = new AndroidUiDriver(adb, artifacts);
        AndroidUiFlowTrace trace = new AndroidUiFlowTrace(artifacts, "whist-lab-table-presentation",
                "Whist Lab — eight-card table, compact bidding, and hamburger navigation",
                getClass().getSimpleName(), "atf.android.e2e", PACKAGE,
                List.of("android", "e2e", "ui", "table-layout", "navigation", "sanity"));
        ui.recordActionsAs(trace::recordAction);
        List<Path> evidence = new ArrayList<>();
        AtfReportStatus outcome = AtfReportStatus.FAIL;
        String checks = "Presentation flow did not complete.";
        try {
            long started = System.nanoTime();
            String home = ui.restartToActivity(PACKAGE, ".MainActivity");
            assertTrue(ui.hasText(home, "English"), "English language option is not available on home");
            ui.tapText("English");
            ui.tapText("New game");
            String setup = ui.awaitText("Start game", Duration.ofSeconds(10));
            ui.selectNamedSpinnerOption("player-count", "6");
            setup = ui.awaitText("Start game", Duration.ofSeconds(10));
            String selectedPlayerCount = ui.spinnerValue(setup, "Players");
            List<Path> setupEvidence = ui.capture("whist-lab-presentation-six-player-setup");
            evidence.addAll(setupEvidence);
            checkpoint(trace, "SETUP", AtfGherkinKeyword.GIVEN,
                    "Configure six players to produce the eight-card hand",
                    "New game; Players = 6", setupEvidence, "select six players and start a new game",
                    "six-player setup is ready", "visible labels=" + ui.texts(setup),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    List.of(new AtfAssertion("Six-player option is selected", "6 players",
                                    selectedPlayerCount + " players", selectedPlayerCount.equals("6")),
                            new AtfAssertion("Start action remains available", "Start game",
                                    Boolean.toString(ui.hasText(setup, "Start game")), ui.hasText(setup, "Start game"))),
                    AtfReportStatus.PASS);

            started = System.nanoTime();
            ui.tapText("Start game");
            String bidding = ui.awaitText("Place bid", Duration.ofSeconds(15));
            String initialBidSummary = ui.texts(bidding).stream()
                    .filter(label -> label.startsWith("Bids submitted "))
                    .findFirst().orElseThrow(() -> new IllegalStateException(
                            "Bid summary was not visible before opening the score sheet"));
            List<AndroidUiDriver.CardLabel> handRow = ui.bottomRowCardLabels(bidding);
            List<String> handLabels = handRow.stream().map(AndroidUiDriver.CardLabel::text).toList();
            boolean oneRow = handRow.size() == 8 && handRow.stream()
                    .map(AndroidUiDriver.CardLabel::top).distinct().count() == 1;
            Matcher bidSummaryMatch = SIX_PLAYER_EIGHT_CARD_BID_SUMMARY.matcher(initialBidSummary);
            assertTrue(bidSummaryMatch.matches(), "Unexpected bidding summary: " + initialBidSummary);
            int submittedBids = Integer.parseInt(bidSummaryMatch.group(1));
            int currentBidTotal = Integer.parseInt(bidSummaryMatch.group(2));
            LinkedHashSet<Integer> expectedBids = new LinkedHashSet<>(
                    java.util.stream.IntStream.rangeClosed(0, 8).boxed().toList());
            if (submittedBids == 5) {
                int forbiddenFinalBid = 8 - currentBidTotal;
                if (forbiddenFinalBid >= 0 && forbiddenFinalBid <= 8) expectedBids.remove(forbiddenFinalBid);
            }
            List<Integer> visibleBids = ui.numericButtonValues(bidding);
            boolean allBidsVisible = visibleBids.size() == expectedBids.size()
                    && new LinkedHashSet<>(visibleBids).equals(expectedBids);
            List<AndroidUiDriver.ElementSize> bidSizes = ui.numericButtonSizes(bidding);
            double densityScale = effectiveDensityScale(adb.run("shell", "wm", "density").output());
            int minimumBidWidth = bidSizes.stream().mapToInt(AndroidUiDriver.ElementSize::widthPixels).min().orElse(0);
            int minimumBidHeight = bidSizes.stream().mapToInt(AndroidUiDriver.ElementSize::heightPixels).min().orElse(0);
            boolean touchTargetsRemain48Dp = bidSizes.size() == expectedBids.size()
                    && minimumBidWidth >= Math.round(48 * densityScale * 0.95)
                    && minimumBidHeight >= Math.round(48 * densityScale * 0.95);
            AndroidUiDriver.ElementSize submitBidSize = ui.buttonSize(bidding, "Place bid");
            boolean submitBidIsDiscreetAndAccessible = submitBidSize.widthPixels()
                    <= Math.round(144 * densityScale) && submitBidSize.heightPixels()
                    >= Math.round(48 * densityScale * 0.95);
            AndroidUiDriver.Bounds submitBounds = ui.boundsForText(bidding, "Place bid");
            AndroidUiDriver.Bounds handBounds = ui.boundsForDescription(bidding, "Your hand, 8 cards.");
            boolean actionAnchoredAboveHand = submitBounds.bottom() < handBounds.top();
            boolean turnedTrumpCardVisible = ui.hasText(bidding, "TURNED CARD")
                    && ui.descriptions(bidding).stream()
                            .anyMatch(value -> value.startsWith("Trump set by turned card: "));
            boolean gameIdentityAndTrumpVisible = ui.hasText(bidding, "Whist · Oh Hell")
                    && turnedTrumpCardVisible;
            var boardBounds = ui.boundsForDescription(bidding, "Playing surface");
            List<AndroidUiDriver.Bounds> seatBounds = ui.descriptions(bidding).stream()
                    .filter(value -> value.contains(". Bid ") && value.contains(". Score "))
                    .map(description -> {
                        try { return ui.boundsForDescription(bidding, description); }
                        catch (Exception failure) { throw new IllegalStateException(failure); }
                    }).toList();
            List<AndroidUiDriver.Bounds> biddingControlBounds = new ArrayList<>();
            for (int bid : visibleBids) biddingControlBounds.add(ui.boundsForText(bidding, Integer.toString(bid)));
            biddingControlBounds.add(submitBounds);
            long biddingSeatOverlaps = biddingControlBounds.stream()
                    .flatMap(control -> seatBounds.stream().filter(seat -> overlaps(control, seat)))
                    .count();
            boolean biddingControlsAvoidSeats = biddingSeatOverlaps == 0;
            List<String> seatPositions = seatBounds.stream().map(bounds -> String.format(java.util.Locale.ROOT,
                    "x=%.2f,y=%.2f", (centerX(bounds) - boardBounds.left()) / (double) width(boardBounds),
                    (bounds.top() - boardBounds.top()) / (double) height(boardBounds))).toList();
            double tableCenterX = boardBounds.left() + width(boardBounds) / 2.0;
            AndroidUiDriver.Bounds humanSeat = ui.boundsForDescriptionContaining(bidding, "You. ");
            boolean humanSeatIsSouth = Math.abs(centerX(humanSeat) - tableCenterX) <= width(boardBounds) * 0.16
                    && (humanSeat.top() - boardBounds.top()) >= height(boardBounds) * 0.78;
            long northSeats = seatBounds.stream().filter(bounds ->
                    (bounds.top() - boardBounds.top()) <= height(boardBounds) * 0.12
                    && Math.abs(centerX(bounds) - tableCenterX) <= width(boardBounds) * 0.16).count();
            long upperFlankSeats = seatBounds.stream().filter(bounds -> {
                double y = (bounds.top() - boardBounds.top()) / (double) height(boardBounds);
                double x = (centerX(bounds) - boardBounds.left()) / (double) width(boardBounds);
                return y >= 0.13 && y <= 0.40 && (x <= 0.32 || x >= 0.68);
            }).count();
            long lowerFlankSeats = seatBounds.stream().filter(bounds -> {
                double y = (bounds.top() - boardBounds.top()) / (double) height(boardBounds);
                double x = (centerX(bounds) - boardBounds.left()) / (double) width(boardBounds);
                return y >= 0.55 && y <= 0.82 && (x <= 0.32 || x >= 0.68);
            }).count();
            boolean mockSeatRingRendered = seatBounds.size() == 6 && humanSeatIsSouth
                    && northSeats == 1 && upperFlankSeats == 2 && lowerFlankSeats == 2;
            assertTrue(oneRow, "Expected all eight hand cards on one row; visible bottom-row cards=" + handLabels);
            assertTrue(allBidsVisible, "Visible choices differ from the legal bid set " + expectedBids
                    + "; actual=" + visibleBids);
            assertTrue(touchTargetsRemain48Dp, "Bid choices fell below 48 x 48 dp touch bounds: " + bidSizes);
            assertTrue(submitBidIsDiscreetAndAccessible,
                    "Place bid should stay compact (<=144 dp wide) with a 48 dp touch height: " + submitBidSize);
            assertTrue(actionAnchoredAboveHand,
                    "Place bid should sit inside the table above the fixed hand row: submit="
                            + submitBounds + "; hand=" + handBounds);
            assertTrue(gameIdentityAndTrumpVisible,
                    "Variant title and the persistent trump tile must remain visible: " + ui.texts(bidding)
                            + "; descriptions=" + ui.descriptions(bidding));
            assertTrue(mockSeatRingRendered,
                    "Six player seats must match the mock's north, upper flanks, lower flanks, and south positions: "
                            + seatPositions + "; human south=" + humanSeatIsSouth);
            assertTrue(biddingControlsAvoidSeats, "Bidding controls overlap a player seat: controls="
                    + biddingControlBounds + "; seats=" + seatBounds);
            List<Path> biddingEvidence = ui.capture("whist-lab-presentation-eight-card-table");
            evidence.addAll(biddingEvidence);
            Path biddingScreenshot = biddingEvidence.stream()
                    .filter(path -> path.toString().endsWith(".png")).findFirst().orElseThrow();
            String biddingDisplaySize = adb.run("shell", "wm", "size").output();
            boolean feltContinuesBelowTable = ui.sameBackgroundAcrossTableMargin(
                    biddingScreenshot, 0.55, 0.87, biddingDisplaySize);
            assertTrue(feltContinuesBelowTable,
                    "The game surface changes color between the table and space below the action");
            String turnedTrumpDescription = ui.descriptions(bidding).stream()
                    .filter(value -> value.startsWith("Trump set by turned card: "))
                    .findFirst().orElse("<missing>");
            String visibleVariant = ui.texts(bidding).stream()
                    .filter(value -> value.startsWith("Whist · ")).findFirst().orElse("<missing>");
            checkpoint(trace, "TEST BODY", AtfGherkinKeyword.THEN,
                    "Render all eight cards and the complete compact bidding grid",
                    "Six-player Oh Hell bidding table", biddingEvidence,
                    "inspect all hand labels, legal bid values, and button bounds",
                    "eight cards share one row; legal bids and 48 dp targets remain visible; identity and trump remain in the header; Place bid sits in the table above the hand",
                    "hand=" + handLabels + "; expected bids=" + expectedBids + "; actual bids=" + visibleBids
                            + "; bid button bounds px=" + bidSizes + "; Place bid bounds px=" + submitBidSize
                            + "; table action bounds=" + submitBounds + "; hand bounds=" + handBounds
                            + "; identity and trump visible=" + gameIdentityAndTrumpVisible
                            + "; continuous felt background=" + feltContinuesBelowTable
                            + "; visible labels=" + ui.texts(bidding),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    List.of(new AtfAssertion("All eight hand cards occupy one row", "8 cards; 1 row",
                                    handLabels.size() + " cards; rows="
                                            + handRow.stream().map(AndroidUiDriver.CardLabel::top).distinct().count(), oneRow),
                            new AtfAssertion("Six player seats follow the approved oval", "N; NW/NE; SW/SE; S",
                                    "seats=" + seatBounds.size() + "; N=" + northSeats + "; upper=" + upperFlankSeats
                                            + "; lower=" + lowerFlankSeats + "; south=" + humanSeatIsSouth,
                                    mockSeatRingRendered),
                            new AtfAssertion("Bidding controls stay clear of player seats",
                                    "0 intersections (1 px bounds tolerance)",
                                    "intersections=" + biddingSeatOverlaps,
                                    biddingControlsAvoidSeats),
                            new AtfAssertion("All currently legal bids are visible", expectedBids.toString(),
                                    visibleBids.toString(), allBidsVisible),
                            new AtfAssertion("Bid hit targets remain at least 48 dp", "48 dp minimum",
                                    "minimum=" + minimumBidWidth + "x" + minimumBidHeight + " px; density="
                                            + String.format(java.util.Locale.ROOT, "%.2f", densityScale),
                                    touchTargetsRemain48Dp),
                            new AtfAssertion("Place bid is compact without shrinking its hit target",
                                    "<=144 dp wide; >=48 dp high", submitBidSize.toString()
                                            + " px at density " + densityScale, submitBidIsDiscreetAndAccessible),
                            new AtfAssertion("Place bid remains inside the table above the hand",
                                    "submit bottom < hand top", submitBounds.bottom() + " < " + handBounds.top(),
                                    actionAnchoredAboveHand),
                            new AtfAssertion("Variant identity and trump remain visible in the header",
                                    "Whist · Oh Hell; turned card rank and suit", visibleVariant + "; "
                                            + turnedTrumpDescription, gameIdentityAndTrumpVisible),
                            new AtfAssertion("Trump header distinguishes the actual turned card",
                                    "TURNED CARD; full accessible card identity",
                                    "TURNED CARD=" + ui.hasText(bidding, "TURNED CARD") + "; "
                                            + turnedTrumpDescription, turnedTrumpCardVisible),
                            new AtfAssertion("Felt remains continuous from the table frame to the lower viewport",
                                    "same table-margin color at 55% and 87% height",
                                    Boolean.toString(feltContinuesBelowTable), feltContinuesBelowTable)),
                    AtfReportStatus.PASS);

            started = System.nanoTime();
            ui.selectBid(3);
            ui.tapDescription("Game menu");
            String menu = ui.awaitText("Score sheet", Duration.ofSeconds(8));
            assertTrue(ui.hasText(menu, "Main menu"), "Game menu omitted Main menu");
            List<Path> menuEvidence = ui.capture("whist-lab-presentation-hamburger-menu");
            evidence.addAll(menuEvidence);
            checkpoint(trace, "TEST BODY", AtfGherkinKeyword.WHEN,
                    "Open the top-left hamburger after selecting a bid",
                    "Game menu", menuEvidence, "select bid 3 and activate Game menu",
                    "Score sheet and Main menu are available", "visible labels=" + ui.texts(menu),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    List.of(new AtfAssertion("Score sheet is in the game menu", "Score sheet",
                                    Boolean.toString(ui.hasText(menu, "Score sheet")), ui.hasText(menu, "Score sheet")),
                            new AtfAssertion("Main menu is in the game menu", "Main menu",
                                    Boolean.toString(ui.hasText(menu, "Main menu")), ui.hasText(menu, "Main menu"))),
                    AtfReportStatus.PASS);

            started = System.nanoTime();
            ui.tapText("Score sheet");
            String score = ui.awaitText("CLOSE", Duration.ofSeconds(8));
            boolean selectionWasNotSubmitted = ui.hasTextContaining(score, "Bid not placed");
            assertTrue(selectionWasNotSubmitted,
                    "Opening Score sheet submitted the unconfirmed bid; visible=" + ui.texts(score));
            boolean scoreColumnsVisible = List.of("Player", "Bid", "Won", "Hand", "Total").stream()
                    .allMatch(label -> {
                        try { return ui.hasText(score, label); }
                        catch (Exception failure) { throw new IllegalStateException(failure); }
                    });
            assertTrue(scoreColumnsVisible, "Current score table omitted a column: " + ui.texts(score));
            List<Path> scoreEvidence = ui.capture("whist-lab-presentation-live-score");
            evidence.addAll(scoreEvidence);
            checkpoint(trace, "TEST BODY", AtfGherkinKeyword.THEN,
                    "Open the read-only score sheet without submitting the selected bid",
                    "Current scores · Hand 1", scoreEvidence, "open Score sheet and inspect the human score row",
                    "unsubmitted bid remains unsubmitted; Player/Bid/Won/Hand/Total columns are aligned",
                    "visible labels=" + ui.texts(score),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    List.of(new AtfAssertion("Score view does not commit the pending bid", "Bid not placed",
                                    Boolean.toString(selectionWasNotSubmitted), selectionWasNotSubmitted),
                            new AtfAssertion("Score sheet presents aligned player and score columns",
                                    "Player · Bid · Won · Hand · Total", String.join(" · ", ui.texts(score)),
                                    scoreColumnsVisible)),
                    AtfReportStatus.PASS);

            started = System.nanoTime();
            ui.tapText("CLOSE");
            String biddingAfterScore = ui.awaitText("Place bid", Duration.ofSeconds(8));
            boolean selectedBidPersisted = ui.isSelected(biddingAfterScore, "3");
            String summaryAfterScore = ui.texts(biddingAfterScore).stream()
                    .filter(label -> label.startsWith("Bids submitted "))
                    .findFirst().orElse("<missing bid summary>");
            boolean bidSummaryPersisted = initialBidSummary.equals(summaryAfterScore);
            assertTrue(selectedBidPersisted, "Closing Score sheet lost the unconfirmed bid selection");
            assertTrue(bidSummaryPersisted, "Score navigation changed submitted bids or bid total: before='"
                    + initialBidSummary + "', after='" + summaryAfterScore + "'");
            List<Path> restoredTableEvidence = ui.capture("whist-lab-presentation-restored-bidding");
            evidence.addAll(restoredTableEvidence);
            checkpoint(trace, "TEST BODY", AtfGherkinKeyword.AND,
                    "Close Score sheet and verify the same bid remains selected",
                    "Bidding table; value 3", restoredTableEvidence, "close Score sheet and inspect selected button 3",
                    "bid 3 remains selected and no bid has been submitted",
                    "selected=" + selectedBidPersisted + "; summary=" + summaryAfterScore
                            + "; visible labels=" + ui.texts(biddingAfterScore),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    List.of(new AtfAssertion("Score view preserves the pending selection", "Bid 3 selected",
                                    Boolean.toString(selectedBidPersisted), selectedBidPersisted),
                            new AtfAssertion("Score navigation leaves the bid summary unchanged", initialBidSummary,
                                    summaryAfterScore, bidSummaryPersisted)),
                    AtfReportStatus.PASS);

            started = System.nanoTime();
            ui.tapDescription("Game menu");
            ui.tapText("Main menu");
            String returnedHome = ui.awaitText("New game", Duration.ofSeconds(10));
            List<Path> homeEvidence = ui.capture("whist-lab-presentation-return-home");
            evidence.addAll(homeEvidence);
            boolean homeReady = ui.hasText(returnedHome, "Resume game") && ui.hasText(returnedHome, "How to play");
            assertTrue(homeReady, "Main menu did not restore the home navigation options");
            checkpoint(trace, "TEARDOWN", AtfGherkinKeyword.THEN,
                    "Return to home from the game menu without advancing play",
                    "Main menu", homeEvidence, "close Score sheet; open Game menu; choose Main menu",
                    "home screen offers New game, Resume game, and How to play",
                    "visible labels=" + ui.texts(returnedHome),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    List.of(new AtfAssertion("Home navigation is restored", "New game; Resume game; How to play",
                                    ui.texts(returnedHome).toString(), homeReady)),
                    AtfReportStatus.PASS);
            checks = "Six-player Oh Hell opened an eight-card human hand. All eight cards rendered on one row; every bid permitted by the current final-bid rule remained visible and each hit target measured at least 48 dp. The hamburger exposed Score sheet and Main menu. A selected but unconfirmed bid remained unsubmitted in the score view. The flow returned home without submitting a bid or playing a card.";
            outcome = AtfReportStatus.PASS;
            trace.finish(outcome);
        } catch (Throwable failure) {
            outcome = AtfReportStatus.FAIL;
            trace.fail(failure);
            if (failure instanceof Exception exception) throw exception;
            if (failure instanceof Error error) throw error;
            throw new RuntimeException(failure);
        } finally {
            try { adb.run("shell", "am", "force-stop", PACKAGE).requireSuccess(); }
            catch (Exception teardownFailure) {
                boolean alreadyFailed = outcome == AtfReportStatus.FAIL;
                trace.fail(teardownFailure);
                if (!alreadyFailed) {
                    outcome = AtfReportStatus.FAIL;
                    throw teardownFailure;
                }
            }
            trace.finish(outcome);
            List<Path> reportEvidence = new ArrayList<>(evidence);
            reportEvidence.add(trace.manifest());
            AndroidUiFlowReportPublisher.publish(artifacts.resolve("whist-lab-table-presentation.html"),
                    packagePath.output(), reportSetting("atf.android.serial", "ATF_ANDROID_SERIAL", "emulator-5554"),
                    System.getProperty("atf.android.api", "device reported at runtime"), checks, reportEvidence, trace);
        }
    }

    @Test
    void thirteenCardBiddingTableFitsAcrossPortraitDisplayProfiles() throws Exception {
        AdbClient adb = AdbClient.fromSystemProperties();
        var packagePath = adb.run("shell", "pm", "path", PACKAGE);
        packagePath.requireSuccess();
        assertTrue(packagePath.output().contains("package:"), "Whist Lab is not installed: " + packagePath.output());

        Path artifacts = artifacts("thirteen-card-bidding-resolutions");
        AndroidUiDriver ui = new AndroidUiDriver(adb, artifacts);
        AndroidUiFlowTrace trace = new AndroidUiFlowTrace(artifacts, "whist-lab-thirteen-card-bidding-resolutions",
                "Whist · Descending — thirteen-card bid screen across portrait display sizes",
                getClass().getSimpleName(), "atf.android.e2e", PACKAGE,
                List.of("android", "e2e", "ui", "13-cards", "bidding", "resolution", "responsive"));
        ui.recordActionsAs(trace::recordAction);
        List<Path> evidence = new ArrayList<>();
        AtfReportStatus outcome = AtfReportStatus.FAIL;
        String checks = "The thirteen-card bidding display review did not complete.";
        String originalSizeOutput = adb.run("shell", "wm", "size").output();
        String originalDensityOutput = adb.run("shell", "wm", "density").output();
        Matcher physicalSize = PHYSICAL_SIZE.matcher(originalSizeOutput);
        int physicalCaptureWidth = 0;
        int physicalCaptureHeight = 0;
        if (physicalSize.find()) {
            physicalCaptureWidth = Integer.parseInt(physicalSize.group(1));
            physicalCaptureHeight = Integer.parseInt(physicalSize.group(2));
        }
        String originalSizeOverride = overrideValue(OVERRIDE_SIZE, originalSizeOutput);
        String originalDensityOverride = overrideDensityValue(originalDensityOutput);
        try {
            List<DisplayProfile> profiles = List.of(
                    new DisplayProfile("1080x1920 · 440 dpi", 1080, 1920, 440),
                    new DisplayProfile("720x1280 · 320 dpi", 720, 1280, 320),
                    new DisplayProfile("1080x2400 · 440 dpi", 1080, 2400, 440));
            int index = 0;
            for (DisplayProfile profile : profiles) {
                long started = System.nanoTime();
                overrideDisplay(adb, ui, profile);
                String home = ui.restartToActivity(PACKAGE, ".MainActivity",
                        "--es", "debug_language", "EN");
                assertTrue(ui.hasText(home, "New game"), "Home did not open at " + profile.name());
                ui.tapText("New game");
                ui.awaitText("Players", Duration.ofSeconds(12));
                ui.selectNamedSpinnerOption("player-count", "4");
                ui.selectNamedSpinnerOption("game-type", "Descending");
                String setup = ui.awaitText("Players", Duration.ofSeconds(10));
                String selectedPlayers = ui.spinnerValue(setup, "Players");
                String selectedMode = ui.spinnerValue(setup, "Game type");
                assertTrue(selectedPlayers.equals("4") && selectedMode.equals("Descending"),
                        "The 13-card deal requires 4 players and Descending at " + profile.name()
                                + "; observed " + selectedPlayers + "/" + selectedMode);
                ui.revealText("Start game");
                setup = ui.awaitText("Start game", Duration.ofSeconds(10));
                ui.tapText("Start game");
                ui.awaitText("Place bid", Duration.ofSeconds(20));
                String bidding = ui.awaitDescriptionContaining("Your hand, 13 cards.", Duration.ofSeconds(10));

                AndroidUiDriver.Bounds handBounds = ui.boundsForDescription(bidding, "Your hand, 13 cards.",
                        "android.widget.HorizontalScrollView");
                double densityScale = effectiveDensityScale(adb.run("shell", "wm", "density").output());
                List<AndroidUiDriver.CardLabel> cardLabels = ui.bottomRowCardLabels(bidding);
                List<AndroidUiDriver.Bounds> cardBounds = ui.playingCardLabelBounds(bidding);
                long handRows = cardLabels.stream().map(AndroidUiDriver.CardLabel::top).distinct().count();
                boolean fullHandVisible = cardLabels.size() == 13 && cardBounds.size() == 13
                        && cardBounds.stream().allMatch(card -> card.left() >= handBounds.left()
                                && card.right() <= handBounds.right()
                                && card.top() >= handBounds.top() && card.bottom() <= handBounds.bottom());
                int minimumCardFaceHeight = cardBounds.stream().mapToInt(WhistLabGameplayE2eTest::height)
                        .min().orElse(0);
                boolean fullCardFacesVisible = cardBounds.size() == 13
                        && minimumCardFaceHeight >= Math.round(98 * densityScale * 0.95);
                boolean handIsOneRow = cardLabels.size() == 13 && handRows == 1 && fullHandVisible
                        && fullCardFacesVisible;

                List<Integer> expectedBids = java.util.stream.IntStream.rangeClosed(0, 13).boxed().toList();
                List<Integer> visibleBids = ui.numericButtonValues(bidding);
                boolean allBidsVisible = visibleBids.size() == expectedBids.size()
                        && new LinkedHashSet<>(visibleBids).equals(new LinkedHashSet<>(expectedBids));
                List<AndroidUiDriver.Bounds> bidBounds = new ArrayList<>();
                List<AndroidUiDriver.ElementSize> bidSizes = ui.numericButtonSizes(bidding);
                for (int bid : visibleBids) bidBounds.add(ui.boundsForText(bidding, Integer.toString(bid)));
                AndroidUiDriver.ElementSize submitSize = ui.buttonSize(bidding, "Place bid");
                AndroidUiDriver.Bounds submitBounds = ui.boundsForText(bidding, "Place bid");
                AndroidUiDriver.Bounds boardBounds = ui.boundsForDescription(bidding, "Playing surface");
                int minimumBidWidth = bidSizes.stream().mapToInt(AndroidUiDriver.ElementSize::widthPixels)
                        .min().orElse(0);
                int minimumBidHeight = bidSizes.stream().mapToInt(AndroidUiDriver.ElementSize::heightPixels)
                        .min().orElse(0);
                boolean bidTargetsRemainAccessible = bidSizes.size() == 14
                        && minimumBidWidth >= Math.round(48 * densityScale * 0.95)
                        && minimumBidHeight >= Math.round(48 * densityScale * 0.95);
                boolean submitRemainsCompact = submitSize.widthPixels() <= Math.round(144 * densityScale)
                        && submitSize.heightPixels() >= Math.round(48 * densityScale * 0.95);
                AndroidUiDriver.Bounds humanSeat = ui.boundsForDescriptionContaining(bidding, "You. ");
                List<AndroidUiDriver.Bounds> seats = ui.descriptions(bidding).stream()
                        .filter(value -> value.contains(". Bid ") && value.contains(". Score "))
                        .map(description -> {
                            try { return ui.boundsForDescription(bidding, description); }
                            catch (Exception failure) { throw new IllegalStateException(failure); }
                        }).toList();
                boolean seatsFit = seats.size() == 4 && seats.stream().allMatch(seat ->
                        seat.left() >= boardBounds.left() && seat.right() <= boardBounds.right()
                                && seat.top() >= boardBounds.top() && seat.bottom() <= boardBounds.bottom());
                boolean allControlsInsideTable = bidBounds.stream().allMatch(control ->
                        control.left() >= boardBounds.left() && control.right() <= boardBounds.right()
                                && control.top() >= boardBounds.top() && control.bottom() <= boardBounds.bottom())
                        && submitBounds.left() >= boardBounds.left() && submitBounds.right() <= boardBounds.right()
                        && submitBounds.top() >= boardBounds.top() && submitBounds.bottom() <= boardBounds.bottom();
                boolean controlsAvoidSeats = java.util.stream.Stream.concat(bidBounds.stream(),
                                java.util.stream.Stream.of(submitBounds))
                        .noneMatch(control -> seats.stream().anyMatch(seat -> overlaps(control, seat)));
                boolean submitAboveHand = submitBounds.bottom() < handBounds.top();
                boolean headerVisible = ui.hasText(bidding, "Whist · Descending")
                        && ui.hasText(bidding, "TRUMP") && ui.hasText(bidding, "NO TURNED CARD");
                List<Path> checkpointEvidence = ui.capture("whist-lab-thirteen-card-bidding-" + profile.fileKey());
                evidence.addAll(checkpointEvidence);
                Path screenshot = checkpointEvidence.stream()
                        .filter(path -> path.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".png"))
                        .findFirst().orElseThrow();
                java.awt.image.BufferedImage captured = javax.imageio.ImageIO.read(screenshot.toFile());
                boolean fullResolutionCapture = captured != null && physicalCaptureWidth > 0
                        && captured.getWidth() == physicalCaptureWidth
                        && captured.getHeight() == physicalCaptureHeight;
                String displayOutput = adb.run("shell", "wm", "size").output().trim();
                boolean logicalDisplayApplied = (profile.width() + "x" + profile.height())
                        .equals(overrideValue(OVERRIDE_SIZE, displayOutput));
                String diagnostic = "profile=" + profile.name() + "; display=" + displayOutput
                        + "; logicalProfileApplied=" + logicalDisplayApplied
                        + "; hand labels=" + cardLabels + "; hand bounds=" + handBounds
                        + "; minimum card-face height=" + minimumCardFaceHeight + " px"
                        + "; bid values=" + visibleBids + "; bid bounds=" + bidBounds
                        + "; submit=" + submitBounds + "; seats=" + seats + "; table=" + boardBounds
                        + "; physicalScreenshot=" + (captured == null ? "unreadable"
                                : captured.getWidth() + "x" + captured.getHeight());
                boolean profilePass = handIsOneRow && allBidsVisible && bidTargetsRemainAccessible
                        && submitRemainsCompact && submitAboveHand && seatsFit && allControlsInsideTable
                        && controlsAvoidSeats && headerVisible && logicalDisplayApplied && fullResolutionCapture;
                checkpoint(trace, "TEST BODY", index == 0 ? AtfGherkinKeyword.GIVEN : AtfGherkinKeyword.AND,
                        "Show and audit the full thirteen-card bidding table at " + profile.name(),
                        "Four-player Descending · hand 13 · legal bids 0–13", checkpointEvidence,
                        "Inspect rendered hand, bid grid, fixed player seats, header, and physical display capture",
                        "13 card faces in one visible row; bids 0–13 stay visible; all targets remain usable and within the table",
                        diagnostic, Duration.ofNanos(System.nanoTime() - started).toMillis(),
                        List.of(new AtfAssertion("All thirteen card faces remain in one visible row",
                                        "13 cards; 1 row; complete 98 dp faces inside viewport",
                                        cardLabels.size() + " cards; rows=" + handRows + "; inside=" + fullHandVisible
                                                + "; min face=" + minimumCardFaceHeight + " px",
                                        handIsOneRow),
                                new AtfAssertion("The complete bid domain remains visible", "0 through 13",
                                        visibleBids.toString(), allBidsVisible),
                                new AtfAssertion("All bid choices preserve 48 dp hit targets", "minimum 48 dp",
                                        minimumBidWidth + "x" + minimumBidHeight + " px; density="
                                                + String.format(java.util.Locale.ROOT, "%.2f", densityScale),
                                        bidTargetsRemainAccessible),
                                new AtfAssertion("Seat and bid geometry stays clear inside the table",
                                        "four seats; controls inside bounds; zero overlaps",
                                        "seats=" + seats.size() + "; controlsInside=" + allControlsInsideTable
                                                + "; overlaps=" + (controlsAvoidSeats ? 0 : 1),
                                        seatsFit && allControlsInsideTable && controlsAvoidSeats),
                                new AtfAssertion("Place bid stays compact and above the hand",
                                        "<=144 dp wide; >=48 dp high; above hand",
                                        submitSize.widthPixels() + "x" + submitSize.heightPixels() + " px; above="
                                                + submitAboveHand, submitRemainsCompact && submitAboveHand),
                                new AtfAssertion("Variant and trump stay visible", "Whist · Descending; TRUMP; NO TURNED CARD",
                                        "variant/trump=" + headerVisible, headerVisible),
                                new AtfAssertion("Report image preserves the complete display resolution",
                                        "physical " + physicalCaptureWidth + "x" + physicalCaptureHeight
                                                + "; logical " + profile.width() + "x" + profile.height(),
                                        captured == null ? "unreadable"
                                                : captured.getWidth() + "x" + captured.getHeight(), fullResolutionCapture)),
                        profilePass ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
                assertTrue(handIsOneRow, "13-card bidding hand is clipped, hidden, or wrapped at " + profile.name()
                        + "; labels=" + cardLabels + "; card bounds=" + cardBounds + "; viewport=" + handBounds);
                assertTrue(allBidsVisible, "The complete 0–13 bid domain is not visible at " + profile.name()
                        + "; actual=" + visibleBids);
                assertTrue(bidTargetsRemainAccessible, "A bid target is below 48 dp at " + profile.name()
                        + "; min=" + minimumBidWidth + "x" + minimumBidHeight + " px; density=" + densityScale);
                assertTrue(submitRemainsCompact && submitAboveHand,
                        "Place bid must be compact, accessible, and above the 13-card hand at " + profile.name()
                                + "; bounds=" + submitBounds + "; size=" + submitSize + "; hand=" + handBounds);
                assertTrue(seatsFit && allControlsInsideTable && controlsAvoidSeats,
                        "13-card bid controls or seats escape/overlap the table at " + profile.name()
                                + "; seats=" + seats + "; bids=" + bidBounds + "; submit=" + submitBounds
                                + "; table=" + boardBounds);
                assertTrue(headerVisible, "Game variant or trump is missing at " + profile.name()
                        + "; labels=" + ui.texts(bidding));
                assertTrue(fullResolutionCapture, "Checkpoint PNG is not full-display for " + profile.name()
                        + "; actual=" + (captured == null ? "unreadable"
                                : captured.getWidth() + "x" + captured.getHeight()));
                index++;
            }
            DisplayProfile oneCardProfile = profiles.getFirst();
            long oneCardStarted = System.nanoTime();
            overrideDisplay(adb, ui, oneCardProfile);
            String home = ui.restartToActivity(PACKAGE, ".MainActivity", "--es", "debug_language", "EN");
            assertTrue(ui.hasText(home, "New game"), "Home did not open for the one-card layout check");
            ui.tapText("New game");
            ui.awaitText("Start game", Duration.ofSeconds(12));
            ui.selectNamedSpinnerOption("player-count", "4");
            ui.selectNamedSpinnerOption("game-type", "Practice hand");
            String oneCardSetup = ui.awaitText("Start game", Duration.ofSeconds(10));
            assertTrue(ui.spinnerValue(oneCardSetup, "Players").equals("4")
                            && ui.spinnerValue(oneCardSetup, "Game type").equals("Practice hand"),
                    "The centered one-card hand check must use the normal four-player Practice hand setup");
            ui.revealText("Start game");
            ui.tapText("Start game");
            String oneCardBidding = ui.awaitText("Place bid", Duration.ofSeconds(20));
            oneCardBidding = ui.awaitDescriptionContaining("Your hand, 1 cards.", Duration.ofSeconds(10));
            AndroidUiDriver.Bounds oneCardBidViewport = ui.boundsForDescription(oneCardBidding,
                    "Your hand, 1 cards.", "android.widget.HorizontalScrollView");
            List<AndroidUiDriver.Bounds> oneCardBidBounds = ui.playingCardLabelBounds(oneCardBidding);
            boolean oneCardBidCentered = singleCardCentered(oneCardBidViewport, oneCardBidBounds);
            List<Path> oneCardBidEvidence = ui.capture("whist-lab-single-card-bidding-center");
            evidence.addAll(oneCardBidEvidence);
            checkpoint(trace, "TEST BODY", AtfGherkinKeyword.AND,
                    "Center the single-card hand at the bottom of the Practice hand bidding screen",
                    "Four-player Practice hand · one card", oneCardBidEvidence,
                    "open normal Practice hand setup and inspect the one-card hand bounds",
                    "the only card is centered within its full-width bottom hand viewport",
                    "hand=" + oneCardBidViewport + "; card bounds=" + oneCardBidBounds,
                    Duration.ofNanos(System.nanoTime() - oneCardStarted).toMillis(),
                    List.of(new AtfAssertion("One-card bidding hand is centered at the bottom",
                            "1 card; horizontal center delta <= 2% of hand width",
                            "cards=" + oneCardBidBounds.size() + "; center delta="
                                    + singleCardCenterDelta(oneCardBidViewport, oneCardBidBounds) + " px",
                            oneCardBidCentered)), oneCardBidCentered ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
            assertTrue(oneCardBidCentered, "The single card is not centered at the bottom of the bid screen: hand="
                    + oneCardBidViewport + "; card=" + oneCardBidBounds);

            oneCardStarted = System.nanoTime();
            ui.selectBid(1);
            ui.tapText("Place bid");
            ui.awaitText("PLAY NOW", Duration.ofSeconds(15));
            String oneCardPlay = ui.awaitDescriptionContaining("Your hand, 1 cards.", Duration.ofSeconds(10));
            AndroidUiDriver.Bounds oneCardPlayViewport = ui.boundsForDescription(oneCardPlay,
                    "Your hand, 1 cards.", "android.widget.HorizontalScrollView");
            List<AndroidUiDriver.Bounds> oneCardPlayBounds = ui.playingCardLabelBounds(oneCardPlay);
            boolean oneCardPlayCentered = singleCardCentered(oneCardPlayViewport, oneCardPlayBounds);
            List<Path> oneCardPlayEvidence = ui.capture("whist-lab-single-card-playing-center");
            evidence.addAll(oneCardPlayEvidence);
            checkpoint(trace, "TEST BODY", AtfGherkinKeyword.THEN,
                    "Keep the single card centered at the bottom after bidding enters play",
                    "Practice hand · PLAY NOW · one card", oneCardPlayEvidence,
                    "submit one bid and inspect the human lead before playing the card",
                    "PLAY NOW is visible and the single card remains centered in the bottom hand row",
                    "hand=" + oneCardPlayViewport + "; card bounds=" + oneCardPlayBounds,
                    Duration.ofNanos(System.nanoTime() - oneCardStarted).toMillis(),
                    List.of(new AtfAssertion("One-card playing hand remains centered at the bottom",
                            "PLAY NOW; 1 card; horizontal center delta <= 2% of hand width",
                            "PLAY NOW=" + ui.hasText(oneCardPlay, "PLAY NOW") + "; cards="
                                    + oneCardPlayBounds.size() + "; center delta="
                                    + singleCardCenterDelta(oneCardPlayViewport, oneCardPlayBounds) + " px",
                            oneCardPlayCentered && ui.hasText(oneCardPlay, "PLAY NOW"))),
                    oneCardPlayCentered && ui.hasText(oneCardPlay, "PLAY NOW")
                            ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
            assertTrue(oneCardPlayCentered, "The single card is not centered at the bottom of the play screen: hand="
                    + oneCardPlayViewport + "; card=" + oneCardPlayBounds);
            outcome = AtfReportStatus.PASS;
            checks = "Four-player Descending presents a genuine thirteen-card first-hand bid screen. The same layout "
                    + "was exercised at 1080x1920@440 dpi, 720x1280@320 dpi, and 1080x2400@440 dpi. Each checkpoint "
                    + "audited every card face, every legal bid, table/seat/button geometry, 48 dp targets, and a "
                    + "full-resolution inline screenshot; no thirteen-card bid was submitted. A normal one-card "
                    + "Practice hand was then checked at bidding and play, with its card centered at the bottom.";
        } catch (Throwable failure) {
            outcome = AtfReportStatus.FAIL;
            try {
                List<Path> failureEvidence = ui.capture("whist-lab-thirteen-card-bidding-failure-state");
                evidence.addAll(failureEvidence);
                checkpoint(trace, "TEST BODY", AtfGherkinKeyword.THEN,
                        "Capture the current Android screen after a display check fails",
                        "Current emulator state", failureEvidence,
                        "capture current screenshot and accessibility hierarchy before restoring display",
                        "The failed UI state is preserved in this checkpoint",
                        failure.getClass().getSimpleName() + ": " + String.valueOf(failure.getMessage()),
                        0, List.of(new AtfAssertion("Failure screen is retained for audit",
                                "failure PNG and accessibility XML", "files=" + failureEvidence.size(), false)),
                        AtfReportStatus.FAIL);
            } catch (Throwable captureFailure) {
                failure.addSuppressed(captureFailure);
            }
            trace.fail(failure);
            if (failure instanceof Exception exception) throw exception;
            if (failure instanceof Error error) throw error;
            throw new RuntimeException(failure);
        } finally {
            try {
                restoreDisplay(adb, ui, originalSizeOverride, originalDensityOverride);
                String home = ui.restartToActivity(PACKAGE, ".MainActivity", "--es", "debug_language", "EN");
                List<Path> homeEvidence = ui.capture("whist-lab-thirteen-card-bidding-restored-display-home");
                evidence.addAll(homeEvidence);
                boolean homeRestored = ui.hasText(home, "New game")
                        && matchesRestoredDisplay(adb, originalSizeOutput, originalDensityOutput);
                checkpoint(trace, "TEARDOWN", AtfGherkinKeyword.THEN,
                        "Restore the original emulator display and leave the game at home",
                        "Original Android display settings", homeEvidence, "reset temporary wm size/density overrides",
                        "original physical display settings restored; Whist Lab home is available",
                        "home visible=" + ui.hasText(home, "New game") + "; size="
                                + adb.run("shell", "wm", "size").output().trim() + "; density="
                                + adb.run("shell", "wm", "density").output().trim(),
                        0, List.of(new AtfAssertion("Emulator display settings are restored",
                                        "original size and density", "restored=" + homeRestored, homeRestored),
                                new AtfAssertion("Whist Lab is left at its main menu", "New game visible",
                                        Boolean.toString(ui.hasText(home, "New game")), ui.hasText(home, "New game"))),
                        homeRestored ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
                if (!homeRestored) {
                    outcome = AtfReportStatus.FAIL;
                    throw new IllegalStateException("Emulator display or app home was not restored after resolution checks");
                }
            } catch (Throwable teardownFailure) {
                boolean alreadyFailed = outcome == AtfReportStatus.FAIL;
                trace.fail(teardownFailure);
                if (!alreadyFailed) {
                    outcome = AtfReportStatus.FAIL;
                    if (teardownFailure instanceof Exception exception) throw exception;
                    if (teardownFailure instanceof Error error) throw error;
                    throw new RuntimeException(teardownFailure);
                }
            }
            trace.finish(outcome);
            List<Path> reportEvidence = new ArrayList<>(evidence);
            reportEvidence.add(trace.manifest());
            AndroidUiFlowReportPublisher.publish(artifacts.resolve("whist-lab-thirteen-card-bidding-resolutions.html"),
                    packagePath.output(), reportSetting("atf.android.serial", "ATF_ANDROID_SERIAL", "emulator-5554"),
                    System.getProperty("atf.android.api", "device reported at runtime"), checks, reportEvidence, trace);
        }
    }

    @Test
    void thirteenCardFanKeepsOneRowAndMovesTheChosenCardIntoTheTable() throws Exception {
        AdbClient adb = AdbClient.fromSystemProperties();
        var packagePath = adb.run("shell", "pm", "path", PACKAGE);
        packagePath.requireSuccess();
        assertTrue(packagePath.output().contains("package:"), "Whist Lab is not installed: " + packagePath.output());

        Path artifacts = artifacts("thirteen-card-fan");
        AndroidUiDriver ui = new AndroidUiDriver(adb, artifacts);
        AndroidUiFlowTrace trace = new AndroidUiFlowTrace(artifacts, "whist-lab-thirteen-card-fan",
                "Whist · Descending — thirteen-card fan and one-card play",
                getClass().getSimpleName(), "atf.android.e2e", PACKAGE,
                List.of("android", "e2e", "ui", "card-fan", "single-row", "click-flow"));
        ui.recordActionsAs(trace::recordAction);
        List<Path> evidence = new ArrayList<>();
        List<AtfAssertion> assertions = new ArrayList<>();
        AtfReportStatus outcome = AtfReportStatus.FAIL;
        String checks = "The thirteen-card click flow did not complete.";
        try {
            long started = System.nanoTime();
            String before = ui.restartToActivity(PACKAGE, ".MainActivity",
                    "--ez", "debug_hand_fan_fixture", "true",
                    "--ez", "debug_hold_ai_turns", "true",
                    "--es", "debug_language", "EN");
            before = ui.awaitDescriptionContaining("Your hand, 13 cards.", Duration.ofSeconds(12));
            List<String> legalCards = ui.legalCardDescriptions(before);
            List<AndroidUiDriver.Bounds> cardSlots = new ArrayList<>();
            for (String description : legalCards) cardSlots.add(ui.boundsForDescription(before, description));
            boolean oneVisibleRow = legalCards.size() == 13
                    && cardSlots.stream().map(AndroidUiDriver.Bounds::top).distinct().count() == 1;
            AndroidUiDriver.Bounds handBoundsBefore = ui.boundsForDescription(before, "Your hand, 13 cards.",
                    "android.widget.HorizontalScrollView");
            boolean finalCardFitsViewport = cardSlots.size() == 13
                    && cardSlots.getLast().right() <= handBoundsBefore.right()
                    && width(cardSlots.getLast()) > width(cardSlots.getFirst());
            String trumpDescription = ui.descriptions(before).stream()
                    .filter(value -> value.startsWith("Trump"))
                    .findFirst().orElse("");
            boolean suitOnlyTrumpIsExplicit = ui.hasText(before, "NO TURNED CARD")
                    && trumpDescription.startsWith("Trump suit only, no turned card: ");
            boolean identityAndTrump = ui.hasText(before, "Whist · Descending") && suitOnlyTrumpIsExplicit;
            boolean seatSignalsAreVisible = ui.hasText(before, "PLAY NOW")
                    && ui.hasTextContaining(before, "Bid 0")
                    && ui.hasTextContaining(before, "Won 0");
            AndroidUiDriver.Bounds playingSurfaceBefore = ui.boundsForDescription(before, "Playing surface");
            AndroidUiDriver.Bounds gamePanelBefore = ui.boundsForDescription(before, "Game table, 4 players.");
            double presentationCenterY = (playingSurfaceBefore.top() + handBoundsBefore.bottom()) / 2.0;
            double gamePanelCenterY = centerY(gamePanelBefore);
            double verticalBalanceTolerance = height(playingSurfaceBefore) * 0.08;
            boolean tableAndHandVerticallyBalanced = Math.abs(presentationCenterY - gamePanelCenterY)
                    <= verticalBalanceTolerance;
            AndroidUiDriver.Bounds humanSeatBefore = ui.boundsForDescriptionContaining(before, "You. ");
            String tableSnapshot = before;
            List<AndroidUiDriver.Bounds> fourPlayerSeats = ui.descriptions(before).stream()
                    .filter(value -> value.contains(". Bid ") && value.contains(". Score "))
                    .map(description -> {
                        try { return ui.boundsForDescription(tableSnapshot, description); }
                        catch (Exception failure) { throw new IllegalStateException(failure); }
                    }).toList();
            double fourPlayerCenterX = playingSurfaceBefore.left() + width(playingSurfaceBefore) / 2.0;
            boolean humanSeatIsSouth = Math.abs(centerX(humanSeatBefore) - fourPlayerCenterX)
                    <= width(playingSurfaceBefore) * 0.16
                    && (humanSeatBefore.top() - playingSurfaceBefore.top())
                    >= height(playingSurfaceBefore) * 0.78;
            long northSeats = fourPlayerSeats.stream().filter(bounds ->
                    (bounds.top() - playingSurfaceBefore.top()) <= height(playingSurfaceBefore) * 0.12
                    && Math.abs(centerX(bounds) - fourPlayerCenterX) <= width(playingSurfaceBefore) * 0.16).count();
            long westSeats = fourPlayerSeats.stream().filter(bounds -> {
                double x = (centerX(bounds) - playingSurfaceBefore.left()) / (double) width(playingSurfaceBefore);
                double y = (bounds.top() - playingSurfaceBefore.top()) / (double) height(playingSurfaceBefore);
                return x <= 0.32 && y >= 0.30 && y <= 0.65;
            }).count();
            long eastSeats = fourPlayerSeats.stream().filter(bounds -> {
                double x = (centerX(bounds) - playingSurfaceBefore.left()) / (double) width(playingSurfaceBefore);
                double y = (bounds.top() - playingSurfaceBefore.top()) / (double) height(playingSurfaceBefore);
                return x >= 0.68 && y >= 0.30 && y <= 0.65;
            }).count();
            boolean fourPlayerMockRingRendered = fourPlayerSeats.size() == 4 && humanSeatIsSouth
                    && northSeats == 1 && westSeats == 1 && eastSeats == 1;
            List<Path> beforeEvidence = ui.capture("whist-lab-thirteen-card-fan-before-play");
            evidence.addAll(beforeEvidence);
            assertions.add(new AtfAssertion("All thirteen cards are exposed on one row", "13 clickable cards; 1 y-coordinate",
                    legalCards.size() + " cards; rows=" + cardSlots.stream()
                            .map(AndroidUiDriver.Bounds::top).distinct().count(), oneVisibleRow));
            assertions.add(new AtfAssertion("The final card face is not clipped by the hand viewport",
                    "last slot fits viewport and remains wider than overlap",
                    "last width=" + width(cardSlots.getLast()) + " px; first width="
                            + width(cardSlots.getFirst()) + " px; right=" + cardSlots.getLast().right()
                            + "/" + handBoundsBefore.right() + " px", finalCardFitsViewport));
            assertions.add(new AtfAssertion("Variant and trump stay visible in the header", "Whist · Descending; trump tile",
                    (ui.hasText(before, "Whist · Descending") ? "Whist · Descending; " : "variant missing; ")
                            + trumpDescription, identityAndTrump));
            assertions.add(new AtfAssertion("A full-deck hand identifies that no turned card exists",
                    "NO TURNED CARD; suit-based trump",
                    "NO TURNED CARD=" + ui.hasText(before, "NO TURNED CARD") + "; " + trumpDescription,
                    suitOnlyTrumpIsExplicit));
            assertions.add(new AtfAssertion("Turn badge and seat bid/result stay visible", "PLAY NOW; Bid 0; Won 0",
                    "PLAY NOW=" + ui.hasText(before, "PLAY NOW") + "; Bid 0="
                            + ui.hasTextContaining(before, "Bid 0") + "; Won 0="
                            + ui.hasTextContaining(before, "Won 0"), seatSignalsAreVisible));
            assertions.add(new AtfAssertion("Four player seats follow the approved oval", "4 seats; N/W/E/S",
                    "seats=" + fourPlayerSeats.size() + "; N=" + northSeats
                            + "; W=" + westSeats + "; E=" + eastSeats + "; south=" + humanSeatIsSouth,
                    fourPlayerMockRingRendered));
            assertions.add(new AtfAssertion("Table and hand are vertically balanced in the game viewport",
                    "surface-to-hand center within " + Math.round(verticalBalanceTolerance) + " px of panel center",
                    "center delta=" + Math.round(Math.abs(presentationCenterY - gamePanelCenterY))
                            + " px; limit=" + Math.round(verticalBalanceTolerance) + " px",
                    tableAndHandVerticallyBalanced));
            checkpoint(trace, "SETUP", AtfGherkinKeyword.GIVEN,
                    "Open a deterministic thirteen-card hand with the human on lead",
                    "debug_hand_fan_fixture; AI turns held", beforeEvidence,
                    "launch the debug deal and inspect each clickable card bound",
                    "13 cards on one row; four seats match N/W/E/S anchors; PLAY NOW and Bid/Won facts visible; header identity and trump persist",
                    "legal cards=" + legalCards + "; seat=" + humanSeatBefore
                            + "; seat positions=" + fourPlayerSeats + "; hand viewport=" + handBoundsBefore
                            + "; final slot=" + cardSlots.getLast() + "; descriptions=" + ui.descriptions(before),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    List.copyOf(assertions), oneVisibleRow && finalCardFitsViewport
                            && identityAndTrump && fourPlayerMockRingRendered && tableAndHandVerticallyBalanced
                            ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
            assertTrue(oneVisibleRow, "Expected thirteen accessible card slots in one row: " + legalCards);
            assertTrue(finalCardFitsViewport, "The terminal card is clipped by the hand viewport: "
                    + cardSlots.getLast() + " in " + handBoundsBefore);
            assertTrue(identityAndTrump, "Variant title or trump indicator was missing");
            assertTrue(seatSignalsAreVisible, "Turn badge or seat bid/result facts were missing");
            assertTrue(fourPlayerMockRingRendered, "Four player seats differ from the approved oval: "
                    + fourPlayerSeats + "; human south=" + humanSeatIsSouth);
            assertTrue(tableAndHandVerticallyBalanced,
                    "The table and hand are unbalanced vertically: surface=" + playingSurfaceBefore
                            + "; hand=" + handBoundsBefore + "; panel=" + gamePanelBefore);

            started = System.nanoTime();
            String selectedCard = ui.firstLegalCardDescription(before);
            String cardName = selectedCard.substring(0, selectedCard.lastIndexOf(". "));
            ui.tapDescription(selectedCard);
            String played = ui.awaitDescriptionContaining("You played " + cardName, Duration.ofSeconds(8));
            played = ui.awaitDescriptionContaining("Your hand, 12 cards.", Duration.ofSeconds(8));
            List<Path> afterEvidence = ui.capture("whist-lab-thirteen-card-fan-after-play");
            evidence.addAll(afterEvidence);
            AndroidUiDriver.Bounds handBoundsAfter = ui.boundsForDescription(played, "Your hand, 12 cards.");
            Path afterScreenshot = afterEvidence.stream()
                    .filter(path -> path.getFileName().toString().endsWith(".png"))
                    .findFirst().orElseThrow();
            String displaySize = adb.run("shell", "wm", "size").output();
            int remainingHandRgb = ui.sampleHandFace(afterScreenshot, handBoundsAfter, displaySize);
            boolean remainingCardsStayOpaque = isIvoryCardSurface(remainingHandRgb);
            AndroidUiDriver.Bounds humanSeatAfter = ui.boundsForDescriptionContaining(played, "You. ");
            boolean seatStayedFixed = humanSeatBefore.equals(humanSeatAfter);
            AndroidUiDriver.Bounds playedCardBounds = ui.boundsForDescription(played, "You played " + cardName);
            AndroidUiDriver.Bounds playingSurfaceBounds = ui.boundsForDescription(played, "Playing surface");
            boolean cardMovedToCenter = Math.abs(centerX(playedCardBounds) - centerX(playingSurfaceBounds))
                    <= width(playingSurfaceBounds) * 0.08
                    && Math.abs(centerY(playedCardBounds) - centerY(playingSurfaceBounds))
                    <= height(playingSurfaceBounds) * 0.20;
            int playedCardCenterDeltaX = Math.abs(centerX(playedCardBounds) - centerX(playingSurfaceBounds));
            int playedCardCenterDeltaY = Math.abs(centerY(playedCardBounds) - centerY(playingSurfaceBounds));
            boolean trumpRemainsVisible = ui.descriptions(played).stream()
                    .anyMatch(value -> value.startsWith("Trump"));
            assertions.add(new AtfAssertion("Tap plays exactly the selected card into the current trick",
                    "selected card centered within 8% x / 20% y of table",
                    cardName + "; dx=" + playedCardCenterDeltaX + "/" + Math.round(width(playingSurfaceBounds) * 0.08)
                            + " px; dy=" + playedCardCenterDeltaY + "/"
                            + Math.round(height(playingSurfaceBounds) * 0.20) + " px",
                    cardMovedToCenter));
            assertions.add(new AtfAssertion("One card leaves the hand after the animation", "Your hand, 12 cards.",
                    ui.descriptions(played).stream().filter(value -> value.startsWith("Your hand"))
                            .findFirst().orElse("<missing>"),
                    ui.hasDescription(played, "Your hand, 12 cards.")));
            assertions.add(new AtfAssertion("The remaining hand stays opaque during the AI turn",
                    "ivory card face (R≥220, G≥220, B≥210)",
                    String.format(java.util.Locale.ROOT, "#%06X", remainingHandRgb), remainingCardsStayOpaque));
            assertions.add(new AtfAssertion("Human seat bounds remain fixed after play", humanSeatBefore.toString(),
                    humanSeatAfter.toString(), seatStayedFixed));
            assertions.add(new AtfAssertion("Trump stays in the header after play", "accessible trump tile remains",
                    ui.descriptions(played).stream().filter(value -> value.startsWith("Trump"))
                            .findFirst().orElse("<missing>"), trumpRemainsVisible));
            checkpoint(trace, "TEST BODY", AtfGherkinKeyword.WHEN,
                    "Tap one legal card and verify the central trick and unchanged seat position",
                    selectedCard, afterEvidence, "tap the accessible card slot and let its lift animation finish",
                    "the chosen card is centered in the current trick; hand shows 12 cards; south seat stays fixed",
                    "selected=" + selectedCard + "; trick=" + ui.descriptions(played).stream()
                            .filter(value -> value.contains(" played ")).toList()
                            + "; hand=" + ui.descriptions(played).stream()
                            .filter(value -> value.startsWith("Your hand")).toList()
                            + "; remaining card sample="
                            + String.format(java.util.Locale.ROOT, "#%06X", remainingHandRgb)
                            + "; display size=" + displaySize.trim()
                            + "; selected card bounds=" + playedCardBounds + "; table=" + playingSurfaceBounds
                            + "; seat before=" + humanSeatBefore + "; after=" + humanSeatAfter,
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    List.copyOf(assertions), cardMovedToCenter && seatStayedFixed
                            && ui.hasDescription(played, "Your hand, 12 cards.")
                            && remainingCardsStayOpaque && trumpRemainsVisible
                            ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
            assertTrue(cardMovedToCenter, "Selected card did not appear in the center trick");
            assertTrue(seatStayedFixed,
                    "Human seat moved during card play: " + humanSeatBefore + " -> " + humanSeatAfter);
            assertTrue(ui.hasDescription(played, "Your hand, 12 cards."),
                    "Playing one card did not leave a twelve-card hand");
            assertTrue(remainingCardsStayOpaque, "Remaining cards were dimmed while the AI holds the turn: "
                    + String.format(java.util.Locale.ROOT, "#%06X", remainingHandRgb));
            assertTrue(trumpRemainsVisible, "Trump tile disappeared after play");
            checks = "A deterministic 13-card Descending hand rendered as one accessible row. Tapping one legal card "
                    + "ran the lift animation, moved that exact card into the central trick, reduced the hand to 12, "
                    + "kept the human seat at the same bounds, retained the trump tile, and left the remaining cards "
                    + "fully opaque during the AI turn.";
            outcome = AtfReportStatus.PASS;
            trace.finish(outcome);
        } catch (Throwable failure) {
            outcome = AtfReportStatus.FAIL;
            trace.fail(failure);
            if (failure instanceof Exception exception) throw exception;
            if (failure instanceof Error error) throw error;
            throw new RuntimeException(failure);
        } finally {
            try { adb.run("shell", "am", "force-stop", PACKAGE).requireSuccess(); }
            catch (Exception teardownFailure) {
                boolean alreadyFailed = outcome == AtfReportStatus.FAIL;
                trace.fail(teardownFailure);
                if (!alreadyFailed) {
                    outcome = AtfReportStatus.FAIL;
                    throw teardownFailure;
                }
            }
            trace.finish(outcome);
            List<Path> reportEvidence = new ArrayList<>(evidence);
            reportEvidence.add(trace.manifest());
            AndroidUiFlowReportPublisher.publish(artifacts.resolve("whist-lab-thirteen-card-fan.html"),
                    packagePath.output(), reportSetting("atf.android.serial", "ATF_ANDROID_SERIAL", "emulator-5554"),
                    System.getProperty("atf.android.api", "device reported at runtime"), checks, reportEvidence, trace);
        }
    }

    @Test
    void defaultFourPlayerOhHellMatchesTheObservedTenCardHandAndSuitedTrump() throws Exception {
        AdbClient adb = AdbClient.fromSystemProperties();
        var packagePath = adb.run("shell", "pm", "path", PACKAGE);
        packagePath.requireSuccess();
        assertTrue(packagePath.output().contains("package:"), "Whist Lab is not installed: " + packagePath.output());

        Path artifacts = artifacts("default-oh-hell");
        AndroidUiDriver ui = new AndroidUiDriver(adb, artifacts);
        AndroidUiFlowTrace trace = new AndroidUiFlowTrace(artifacts, "whist-lab-default-oh-hell-deal",
                "Whist Lab — default Oh Hell setup matches the observed four-player deal",
                getClass().getSimpleName(), "atf.android.e2e", PACKAGE,
                List.of("android", "api-28", "parallel-app", "e2e", "oh-hell", "deal-size"));
        ui.recordActionsAs(trace::recordAction);
        AtfReportStatus result = AtfReportStatus.FAIL;
        try {
            long started = System.nanoTime();
            String home = ui.restartToActivity(PACKAGE, ".MainActivity");
            assertTrue(ui.hasText(home, "New game"), "Whist Lab home screen was not exposed");
            ui.tapText("English");
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
            String bidding = ui.awaitText("Place bid", Duration.ofSeconds(15));
            String trumpDescription = ui.descriptions(bidding).stream()
                    .filter(label -> label.startsWith("Trump"))
                    .findFirst().orElse("");
            boolean recognizedTrump = trumpDescription.matches(
                    "Trump (?:: (?:spades|hearts|clubs|diamonds)|set by turned card: .+)");
            boolean tenCardHand = ui.hasDescription(bidding, "Your hand, 10 cards.");
            boolean biddingReady = ui.hasText(bidding, "Hand 1 / 19")
                    && ui.hasText(bidding, "Place bid") && ui.hasText(bidding, "Whist · Oh Hell")
                    && recognizedTrump && tenCardHand;
            List<Path> biddingEvidence = ui.capture("whist-lab-default-oh-hell-bidding");
            checkpoint(trace, "TEST BODY", AtfGherkinKeyword.THEN,
                    "Start the default four-player Oh Hell deal",
                    "4 players; Oh Hell; Normal AI", biddingEvidence,
                    "tap Start game", "Whist · Oh Hell; Hand 1 / 19; ten human cards; header trump tile; awaits a human bid",
                    "visible labels=" + ui.texts(bidding)
                            + "; original samples by seats 3/4/5/6 showed Diamonds/Hearts/Clubs/Spades",
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    List.of(new AtfAssertion("Default hand schedule is visible", "Hand 1 / 19",
                                    Boolean.toString(ui.hasText(bidding, "Hand 1 / 19")), ui.hasText(bidding, "Hand 1 / 19")),
                            new AtfAssertion("Ten-card hand is exposed accessibly", "Your hand, 10 cards.",
                                    Boolean.toString(tenCardHand), tenCardHand),
                            new AtfAssertion("An Oh Hell trump is visible in the persistent header tile",
                                    "accessible Trump descriptor", trumpDescription, recognizedTrump),
                            new AtfAssertion("Human can place the first bid", "Place bid",
                                    Boolean.toString(ui.hasText(bidding, "Place bid")), ui.hasText(bidding, "Place bid"))),
                    biddingReady ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
            assertTrue(biddingReady, "Default Oh Hell first hand did not reach the current human bidding UI; labels="
                    + ui.texts(bidding) + "; trump=" + trumpDescription + "; hand=" + tenCardHand);
            result = AtfReportStatus.PASS;
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

        Path artifacts = artifacts("practice-trick");
        AndroidUiDriver ui = new AndroidUiDriver(adb, artifacts);
        List<Path> evidence = new ArrayList<>();
        AndroidUiFlowTrace trace = new AndroidUiFlowTrace(artifacts, "whist-lab-resume-exact-bid-trick",
                "Whist Lab — deterministic bid, process death, resume, and exact-bid score",
                getClass().getSimpleName(), "atf.android.e2e", PACKAGE,
                List.of("android", "api-28", "parallel-app", "e2e", "resume", "scoring"));
        ui.recordActionsAs(trace::recordAction);
        AtfReportStatus outcome = AtfReportStatus.FAIL;
        String checks = "The deterministic game flow did not complete.";
        try {
            long actionStarted = System.nanoTime();
            String bidding = ui.restartToActivity(PACKAGE, ".MainActivity", "--ez", "debug_fixture", "true",
                    "--ez", "persist_fixture", "true");
            bidding = ui.awaitText("Place bid", Duration.ofSeconds(10));
            assertTrue(ui.hasText(bidding, "Place bid"), "Fixture did not open at its human bidding turn");
            assertTrue(ui.hasDescriptionContaining(bidding, "Trump: spades"), "Fixture trump should be Spades");
            List<Path> biddingEvidence = ui.capture("whist-lab-bidding");
            evidence.addAll(biddingEvidence);
            checkpoint(trace, "SETUP", AtfGherkinKeyword.GIVEN, "Launch deterministic 4-player fixture at bidding",
                    "debug_fixture=true; persist_fixture=true", biddingEvidence,
                    "force-stop and launch MainActivity with deterministic fixture",
                    "Place bid; header Trump: spades", "visible labels=" + ui.texts(bidding),
                    Duration.ofNanos(System.nanoTime() - actionStarted).toMillis(),
                    List.of(new AtfAssertion("Fixture opens at human bidding turn", "Place bid",
                                    Boolean.toString(ui.hasText(bidding, "Place bid")), ui.hasText(bidding, "Place bid")),
                            new AtfAssertion("Fixture trump is Spades", "accessible Trump: spades tile",
                                    Boolean.toString(ui.hasDescriptionContaining(bidding, "Trump: spades")),
                                    ui.hasDescriptionContaining(bidding, "Trump: spades"))),
                    AtfReportStatus.PASS);

            actionStarted = System.nanoTime();
            ui.selectBid(1);
            ui.tapText("Place bid");
            String play = ui.awaitText("A♥", Duration.ofSeconds(10));
            assertTrue(ui.hasText(play, "PLAY NOW"), "Fixture did not show the active play seat");
            boolean bidAndResultStayAtHumanSeat = ui.hasDescriptionContaining(play, "You. Bid 1.");
            assertTrue(bidAndResultStayAtHumanSeat, "Human bid was not retained into play");
            List<Path> playEvidence = ui.capture("whist-lab-play");
            evidence.addAll(playEvidence);
            checkpoint(trace, "TEST BODY", AtfGherkinKeyword.WHEN, "Select Bid 1 and reach the human A♥ turn",
                    "central bid button=1; Place bid", playEvidence, "select bid 1 at table center and tap Place bid",
                    "PLAY NOW with bid 1", "visible labels=" + ui.texts(play),
                    Duration.ofNanos(System.nanoTime() - actionStarted).toMillis(),
                    List.of(new AtfAssertion("Active play seat is marked", "PLAY NOW",
                                    Boolean.toString(ui.hasText(play, "PLAY NOW")), ui.hasText(play, "PLAY NOW")),
                            new AtfAssertion("Bid and won count remain attached to human seat", "You. Bid 1.",
                                    Boolean.toString(bidAndResultStayAtHumanSeat), bidAndResultStayAtHumanSeat)),
                    AtfReportStatus.PASS);

            actionStarted = System.nanoTime();
            long processStopStarted = System.nanoTime();
            try {
                adb.run("shell", "am", "force-stop", PACKAGE).requireSuccess();
                ui.recordAction("Force-stop Android app", PACKAGE, "The active game process stops",
                        "ADB accepted force-stop", Duration.ofNanos(System.nanoTime() - processStopStarted).toMillis(),
                        AtfReportStatus.PASS);
            } catch (Exception failure) {
                ui.recordAction("Force-stop Android app", PACKAGE, "The active game process stops",
                        failure.toString(), Duration.ofNanos(System.nanoTime() - processStopStarted).toMillis(),
                        AtfReportStatus.FAIL);
                throw failure;
            }
            long relaunchStarted = System.nanoTime();
            try {
                adb.run("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivity").requireSuccess();
                ui.recordAction("Relaunch Android activity", PACKAGE + "/.MainActivity",
                        "The app returns to its home screen", "ADB accepted activity start",
                        Duration.ofNanos(System.nanoTime() - relaunchStarted).toMillis(), AtfReportStatus.PASS);
            } catch (Exception failure) {
                ui.recordAction("Relaunch Android activity", PACKAGE + "/.MainActivity",
                        "The app returns to its home screen", failure.toString(),
                        Duration.ofNanos(System.nanoTime() - relaunchStarted).toMillis(), AtfReportStatus.FAIL);
                throw failure;
            }
            String home = ui.awaitText("Resume game", Duration.ofSeconds(10));
            assertTrue(ui.hasText(home, "New game"), "App did not return to its home screen after process restart");
            ui.tapText("Resume game");
            String resumed = ui.awaitText("A♥", Duration.ofSeconds(10));
            assertTrue(ui.hasText(resumed, "PLAY NOW"), "Saved game did not restore the active play seat");
            boolean resumedBidAndResult = ui.hasDescriptionContaining(resumed, "You. Bid 1.");
            assertTrue(resumedBidAndResult, "Saved human bid was lost after process restart");
            List<Path> resumeEvidence = ui.capture("whist-lab-resume");
            evidence.addAll(resumeEvidence);
            checkpoint(trace, "TEST BODY", AtfGherkinKeyword.AND, "Force-stop the process and resume the same saved turn",
                    "Resume game", resumeEvidence, "force-stop; relaunch; tap Resume game",
                    "same human turn and Bid 1 restored", "visible labels=" + ui.texts(resumed),
                    Duration.ofNanos(System.nanoTime() - actionStarted).toMillis(),
                    List.of(new AtfAssertion("Resume restores active play seat", "PLAY NOW",
                                    Boolean.toString(ui.hasText(resumed, "PLAY NOW")), ui.hasText(resumed, "PLAY NOW")),
                            new AtfAssertion("Resume preserves bid and won count", "You. Bid 1.",
                                    Boolean.toString(resumedBidAndResult), resumedBidAndResult)),
                    AtfReportStatus.PASS);

            actionStarted = System.nanoTime();
            ui.tapText("A♥");
            String score = ui.awaitText("Score sheet", Duration.ofSeconds(12));
            String exactScore = "You | Bid: 1 | Tricks won: 1 | Hand: +11 | Total: 11";
            assertTrue(ui.hasDescription(score, exactScore),
                    "Exact-bid score row is incorrect; visible texts=" + ui.texts(score));
            List<Path> scoreEvidence = ui.capture("whist-lab-score");
            evidence.addAll(scoreEvidence);
            checkpoint(trace, "TEST BODY", AtfGherkinKeyword.THEN, "Play A♥ and verify exact-bid scoring",
                    "A♥; Score sheet", scoreEvidence, "play A♥; let AI finish the trick",
                    exactScore, "visible labels=" + ui.texts(score),
                    Duration.ofNanos(System.nanoTime() - actionStarted).toMillis(),
                    List.of(new AtfAssertion("Exact bid earns 10 bonus plus one trick point", exactScore,
                            Boolean.toString(ui.hasDescription(score, exactScore)), ui.hasDescription(score, exactScore))),
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
            List<Path> reportEvidence = new ArrayList<>(evidence);
            reportEvidence.add(trace.manifest());
            AndroidUiFlowReportPublisher.publish(artifacts.resolve("whist-lab-gameplay.html"),
                    packagePath.output(), reportSetting("atf.android.serial", "ATF_ANDROID_SERIAL", "emulator-5554"),
                    System.getProperty("atf.android.api", "device reported at runtime"), checks, reportEvidence, trace);
        }
    }

    @Test
    void completedTrickRemainsRenderedBeforeTheNextTrickBecomesInteractive() throws Exception {
        AdbClient adb = AdbClient.fromSystemProperties();
        var packagePath = adb.run("shell", "pm", "path", PACKAGE);
        packagePath.requireSuccess();
        assertTrue(packagePath.output().contains("package:"), "Whist Lab is not installed: " + packagePath.output());

        Path artifacts = artifacts("completed-trick-display-pause");
        AndroidUiDriver ui = new AndroidUiDriver(adb, artifacts);
        AndroidUiFlowTrace trace = new AndroidUiFlowTrace(artifacts, "whist-lab-completed-trick-display-pause",
                "Whist Lab — four played cards remain visible before the next trick",
                getClass().getSimpleName(), "atf.android.e2e", PACKAGE,
                List.of("android", "api-28", "parallel-app", "e2e", "trick-display", "timing"));
        ui.recordActionsAs(trace::recordAction);
        List<Path> evidence = new ArrayList<>();
        AtfReportStatus outcome = AtfReportStatus.FAIL;
        String checks = "The completed trick did not remain visible before the next trick.";
        FrameSampler sampler = null;
        try {
            long started = System.nanoTime();
            String bidding = ui.restartToActivity(PACKAGE, ".MainActivity",
                    "--ez", "debug_trick_pause_fixture", "true", "--es", "debug_language", "EN");
            bidding = ui.awaitText("Place bid", Duration.ofSeconds(12));
            assertTrue(ui.hasDescription(bidding, "Your hand, 2 cards."),
                    "Two-trick fixture did not open at the human bidding turn");
            List<Path> biddingEvidence = ui.capture("whist-lab-trick-pause-bidding");
            evidence.addAll(biddingEvidence);
            checkpoint(trace, "SETUP", AtfGherkinKeyword.GIVEN,
                    "Open a deterministic two-trick deal at the human bidding turn",
                    "debug_trick_pause_fixture=true", biddingEvidence,
                    "launch the two-trick fixture", "Place bid with two cards in hand",
                    "visible labels=" + ui.texts(bidding), Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    List.of(new AtfAssertion("Fixture opens with two cards", "Your hand, 2 cards.",
                                    Boolean.toString(ui.hasDescription(bidding, "Your hand, 2 cards.")),
                                    ui.hasDescription(bidding, "Your hand, 2 cards.")),
                            new AtfAssertion("Bidding control is available", "Place bid",
                                    Boolean.toString(ui.hasText(bidding, "Place bid")), ui.hasText(bidding, "Place bid"))),
                    AtfReportStatus.PASS);

            started = System.nanoTime();
            ui.selectBid(0);
            ui.tapText("Place bid");
            String play = ui.awaitText("PLAY NOW", Duration.ofSeconds(12));
            assertTrue(ui.hasDescription(play, "Your hand, 2 cards."),
                    "Human did not retain both cards for the first trick");
            assertTrue(ui.hasText(play, "A♥"), "The deterministic lead card A♥ is not visible");
            List<Path> playEvidence = ui.capture("whist-lab-trick-pause-first-turn");
            evidence.addAll(playEvidence);
            checkpoint(trace, "TEST BODY", AtfGherkinKeyword.WHEN,
                    "Submit Bid 0 and reach the human lead with A♥",
                    "bid button 0; Place bid", playEvidence,
                    "select Bid 0; submit the bid", "PLAY NOW with A♥ and two cards",
                    "visible labels=" + ui.texts(play), Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    List.of(new AtfAssertion("Human can lead the deterministic first trick", "A♥ and PLAY NOW",
                            Boolean.toString(ui.hasText(play, "A♥") && ui.hasText(play, "PLAY NOW")),
                            ui.hasText(play, "A♥") && ui.hasText(play, "PLAY NOW"))),
                    AtfReportStatus.PASS);

            started = System.nanoTime();
            sampler = startFrameSampler(adb, ui, artifacts);
            ui.tapTextIn(play, "A♥");
            List<TrickFrame> sampledFrames = finishFrameSampler(adb, sampler);
            sampler = null;
            long cardFaceFrameCount = countCardFaceFrames(sampledFrames);
            TrickFramePair renderedPause = findRenderedPause(sampledFrames);
            boolean heldForObservationWindow = renderedPause != null;
            long visibleMillis = renderedPause == null ? 0 : renderedPause.elapsedMillis();
            assertTrue(heldForObservationWindow, "No pair of device-side frames showed all four rendered cards "
                    + "at least 900 ms apart within the 1.5-second pause; sampled=" + sampledFrames.size()
                    + "; card-face frames=" + cardFaceFrameCount);
            Path firstTrickImage = artifacts.resolve("whist-lab-completed-four-card-trick-at-start.png");
            Path laterTrickImage = artifacts.resolve("whist-lab-completed-four-card-trick-after-900ms.png");
            Files.copy(renderedPause.first().image(), firstTrickImage,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            reencodePngWithUptime(renderedPause.later().image(), laterTrickImage,
                    renderedPause.later().uptimeSeconds());
            List<Path> trickEvidence = List.of(firstTrickImage);
            evidence.add(firstTrickImage);
            evidence.add(laterTrickImage);
            ui.recordAction("Capture device-side full-display frame", firstTrickImage.getFileName().toString(),
                    "Preserve a frame with all four completed-trick cards", "Frame sampled on Android",
                    0, AtfReportStatus.PASS);
            checkpoint(trace, "TEST BODY", AtfGherkinKeyword.THEN,
                    "Show all four cards at the start of the 1.5-second trick pause",
                    "the completed trick at the center of the table", trickEvidence,
                    "play A♥ and sample full-display PNG frames on the Android device",
                    "four separate ivory card faces with red heart markings remain in the center-table slots",
                    "device-side sampled frames=" + sampledFrames.size() + "; four-card frames="
                            + cardFaceFrameCount,
                    visibleMillis,
                    List.of(new AtfAssertion("All four completed-trick faces are rendered",
                                    "four ivory card faces in the 2×2 center-table layout",
                                    Boolean.toString(cardFacesAreRendered(firstTrickImage)),
                                    cardFacesAreRendered(firstTrickImage)),
                            new AtfAssertion("Each completed card shows its heart rank mark",
                                    "visible red heart/rank pixels on all four card faces",
                                    Boolean.toString(cardFacesHaveHeartMarks(firstTrickImage)),
                                    cardFacesHaveHeartMarks(firstTrickImage))), AtfReportStatus.PASS);

            ui.recordAction("Capture device-side full-display frame", laterTrickImage.getFileName().toString(),
                    "Retain the same four card faces at least 900 ms later with lossless capture-time metadata",
                    "Frame sampled " + visibleMillis + " ms later; decoded pixels preserved exactly", visibleMillis,
                    AtfReportStatus.PASS);
            checkpoint(trace, "TEST BODY", AtfGherkinKeyword.AND,
                    "Confirm the four-card display remains unchanged 900 ms later",
                    "center-table card regions", List.of(laterTrickImage),
                    "compare two independently sampled full-display PNG frames and the live hierarchy",
                    "the same four card faces remain visible before the 1.5-second pause expires",
                    "all four rendered card faces remain equivalent; elapsed=" + visibleMillis + " ms",
                    visibleMillis,
                    List.of(new AtfAssertion("Four-card display persists through the observation window",
                            "same visible center-table cards after >=900 ms and before 1.5 s",
                            Boolean.toString(heldForObservationWindow),
                            heldForObservationWindow)), AtfReportStatus.PASS);

            started = System.nanoTime();
            String nextTurn = awaitNextTrickReady(ui, Duration.ofSeconds(5));
            boolean nextTrickReady = ui.hasText(nextTurn, "PLAY NOW") && playedCardCount(ui, nextTurn) == 0;
            assertTrue(nextTrickReady, "The next trick did not start cleanly after the display pause; descriptions="
                    + ui.descriptions(nextTurn));
            List<Path> nextEvidence = ui.capture("whist-lab-next-trick-ready");
            evidence.addAll(nextEvidence);
            ui.recordAction("Capture next-trick Android UI state", PACKAGE,
                    "Retain the cleared table and human lead before the next card is played",
                    "Captured " + nextEvidence.size() + " final-turn evidence files", 0, AtfReportStatus.PASS);
            checkpoint(trace, "TEST BODY", AtfGherkinKeyword.THEN,
                    "Start the next trick only after the completed cards clear",
                    "human lead seat with one card remaining", nextEvidence,
                    "wait for the 1.5-second table pause", "PLAY NOW, one card, and no prior trick cards",
                    "visible labels=" + ui.texts(nextTurn) + "; played-card count=" + playedCardCount(ui, nextTurn),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    List.of(new AtfAssertion("Next trick begins with the winner leading", "PLAY NOW",
                                    Boolean.toString(ui.hasText(nextTurn, "PLAY NOW")), ui.hasText(nextTurn, "PLAY NOW")),
                            new AtfAssertion("Previous four cards have cleared before the next trick", "0 played cards",
                                    Integer.toString(playedCardCount(ui, nextTurn)), playedCardCount(ui, nextTurn) == 0)),
                    AtfReportStatus.PASS);
            checks = "The deterministic two-trick fixture showed all four heart-card faces in Android-side sampled frames "
                    + visibleMillis + " ms apart during the configured 1.5-second pause, then cleared the completed trick before the human "
                    + "winner received the lead for the next trick.";
            outcome = AtfReportStatus.PASS;
        } catch (Throwable failure) {
            outcome = AtfReportStatus.FAIL;
            trace.fail(failure);
            if (failure instanceof Exception exception) throw exception;
            if (failure instanceof Error error) throw error;
            throw new RuntimeException(failure);
        } finally {
            try {
                if (sampler != null) adb.run("shell", "rm", "-rf", sampler.remoteDirectory()).requireSuccess();
                adb.run("shell", "am", "force-stop", PACKAGE).requireSuccess();
                List<Path> teardown = ui.capture("whist-lab-trick-pause-teardown");
                evidence.addAll(teardown);
                ui.recordAction("Capture final Android UI state", PACKAGE,
                        "Retain the final screenshot and hierarchy after the E2E flow",
                        "Captured " + teardown.size() + " final evidence files", 0, AtfReportStatus.PASS);
                checkpoint(trace, "TEARDOWN", AtfGherkinKeyword.THEN,
                        "Retain the final table state", "Whist Lab Android app",
                        teardown, "force-stop and capture the final screen",
                        "final UI evidence remains attached to the flow", "captured=" + teardown.size() + " files",
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
            List<Path> reportEvidence = new ArrayList<>(evidence);
            reportEvidence.add(trace.manifest());
            AndroidUiFlowReportPublisher.publish(artifacts.resolve("whist-lab-trick-display-pause.html"),
                    packagePath.output(), reportSetting("atf.android.serial", "ATF_ANDROID_SERIAL", "emulator-5554"),
                    System.getProperty("atf.android.api", "device reported at runtime"), checks, reportEvidence, trace);
        }
    }

    private static String awaitPlayedCardCount(AndroidUiDriver ui, int expectedCount, Duration timeout)
            throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        String latest;
        do {
            latest = ui.dump();
            if (playedCardCount(ui, latest) == expectedCount) return latest;
            Thread.sleep(100);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException("Expected " + expectedCount + " played cards together; descriptions="
                + ui.descriptions(latest));
    }

    private static int playedCardCount(AndroidUiDriver ui, String xml) throws Exception {
        return (int) ui.descriptions(xml).stream()
                .filter(description -> description.contains(" played "))
                .filter(description -> !description.startsWith("No cards have been played "))
                .count();
    }

    private static String awaitNextTrickReady(AndroidUiDriver ui, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        String latest;
        do {
            latest = ui.dump();
            if (ui.hasDescription(latest, "Your hand, 1 cards.") && ui.hasText(latest, "PLAY NOW")
                    && playedCardCount(ui, latest) == 0) return latest;
            Thread.sleep(100);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException("Next trick did not become interactive; texts=" + ui.texts(latest)
                + "; descriptions=" + ui.descriptions(latest));
    }

    private static boolean hasPlayedCardText(AndroidUiDriver ui, String xml, String cardLabel) throws Exception {
        return ui.texts(xml).stream().anyMatch(text -> text.equals(cardLabel));
    }

    private static FrameSampler startFrameSampler(AdbClient adb, AndroidUiDriver ui, Path artifacts)
            throws Exception {
        String remote = "/sdcard/atf-whist-trick-" + UUID.randomUUID().toString().replace("-", "");
        Path local = artifacts.resolve("device-trick-frames-" + remote.substring(remote.lastIndexOf('-') + 1));
        adb.run("shell", "mkdir", "-p", remote).requireSuccess();
        String loop = "i=0; while [ \"$i\" -lt 36 ]; do screencap -p " + remote
                + "/frame-$i.png; cat /proc/uptime > " + remote + "/frame-$i.time; "
                + "i=$((i + 1)); sleep 0.06; done; touch " + remote + "/complete";
        long started = System.nanoTime();
        adb.run("shell", "nohup sh -c '" + loop + "' </dev/null >/dev/null 2>&1 &").requireSuccess();
        ui.recordAction("Start Android-side display sampling", remote,
                "Capture PNG frames without waiting for host-side ADB screenshots",
                "36-frame sampler launched on device", Duration.ofNanos(System.nanoTime() - started).toMillis(),
                AtfReportStatus.PASS);
        return new FrameSampler(remote, local);
    }

    private static List<TrickFrame> finishFrameSampler(AdbClient adb, FrameSampler sampler) throws Exception {
        try {
            long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
            while (!adb.run("shell", "test", "-f", sampler.remoteDirectory() + "/complete").succeeded()) {
                if (System.nanoTime() >= deadline) {
                    throw new IllegalStateException("Android-side screenshot sampler did not finish within 20 seconds");
                }
                Thread.sleep(250);
            }
            Files.createDirectories(sampler.localDirectory());
            adb.run("pull", sampler.remoteDirectory(), sampler.localDirectory().toString()).requireSuccess();
            List<Path> images;
            try (var files = Files.walk(sampler.localDirectory())) {
                images = files.filter(path -> path.getFileName().toString().matches("frame-\\d+\\.png"))
                        .sorted(Comparator.comparingInt(WhistLabGameplayE2eTest::frameNumber)).toList();
            }
            List<TrickFrame> frames = new ArrayList<>();
            for (Path image : images) {
                Path timeFile = image.resolveSibling(image.getFileName().toString().replace(".png", ".time"));
                String[] uptime = Files.readString(timeFile).trim().split("\\s+");
                frames.add(new TrickFrame(image, Double.parseDouble(uptime[0])));
            }
            return List.copyOf(frames);
        } finally {
            adb.run("shell", "rm", "-rf", sampler.remoteDirectory()).requireSuccess();
        }
    }

    private static int frameNumber(Path path) {
        String name = path.getFileName().toString();
        return Integer.parseInt(name.substring("frame-".length(), name.length() - ".png".length()));
    }

    private static TrickFramePair findRenderedPause(List<TrickFrame> frames) throws Exception {
        for (int firstIndex = 0; firstIndex < frames.size(); firstIndex++) {
            TrickFrame first = frames.get(firstIndex);
            if (!cardFacesAreRendered(first.image())) continue;
            for (int laterIndex = firstIndex + 1; laterIndex < frames.size(); laterIndex++) {
                TrickFrame later = frames.get(laterIndex);
                long elapsedMillis = Math.round((later.uptimeSeconds() - first.uptimeSeconds()) * 1_000);
                if (elapsedMillis > 1_400) break;
                if (elapsedMillis >= 900 && cardFacesAreRendered(later.image())
                        && cardPixelsRemainEquivalent(first.image(), later.image())) {
                    return new TrickFramePair(first, later, elapsedMillis);
                }
            }
        }
        return null;
    }

    private static long countCardFaceFrames(List<TrickFrame> frames) throws Exception {
        long count = 0;
        for (TrickFrame frame : frames) if (cardFacesAreRendered(frame.image())) count++;
        return count;
    }

    private static boolean cardFacesAreRendered(Path imagePath) throws Exception {
        BufferedImage image = ImageIO.read(imagePath.toFile());
        if (image == null) return false;
        return cardFaceMetrics(image).stream().allMatch(metrics -> metrics.ivoryFraction() >= 0.55
                && metrics.heartMarkFraction() >= 0.01);
    }

    private static boolean cardFacesHaveHeartMarks(Path imagePath) throws Exception {
        BufferedImage image = ImageIO.read(imagePath.toFile());
        return image != null && cardFaceMetrics(image).stream().allMatch(metrics -> metrics.heartMarkFraction() >= 0.01);
    }

    private static List<CardFaceMetrics> cardFaceMetrics(BufferedImage image) {
        int width = Math.max(1, (int) Math.round(image.getWidth() * 0.12));
        int height = Math.max(1, (int) Math.round(image.getHeight() * 0.085));
        List<CardFaceMetrics> result = new ArrayList<>();
        for (double centerY : List.of(0.35, 0.45)) {
            for (double centerX : List.of(0.436, 0.564)) {
                int centerXpx = (int) Math.round(image.getWidth() * centerX);
                int centerYpx = (int) Math.round(image.getHeight() * centerY);
                PixelRegion region = new PixelRegion(Math.max(0, centerXpx - width / 2),
                        Math.max(0, centerYpx - height / 2),
                        Math.min(image.getWidth(), centerXpx + (width + 1) / 2),
                        Math.min(image.getHeight(), centerYpx + (height + 1) / 2));
                long ivory = 0;
                long red = 0;
                long sampled = 0;
                for (int y = region.top(); y < region.bottom(); y += 3) {
                    for (int x = region.left(); x < region.right(); x += 3) {
                        int rgb = image.getRGB(x, y);
                        int r = (rgb >>> 16) & 0xFF;
                        int g = (rgb >>> 8) & 0xFF;
                        int b = rgb & 0xFF;
                        sampled++;
                        if (isIvoryCardSurface(rgb)) ivory++;
                        if (r >= 145 && g <= 115 && b <= 115 && r - g >= 45) red++;
                    }
                }
                result.add(new CardFaceMetrics(sampled == 0 ? 0 : ivory / (double) sampled,
                        sampled == 0 ? 0 : red / (double) sampled));
            }
        }
        return List.copyOf(result);
    }

    private static boolean cardPixelsRemainEquivalent(Path firstPath, Path laterPath) throws Exception {
        BufferedImage first = ImageIO.read(firstPath.toFile());
        BufferedImage later = ImageIO.read(laterPath.toFile());
        if (first == null || later == null || first.getWidth() != later.getWidth()
                || first.getHeight() != later.getHeight()) return false;
        for (PixelRegion region : cardFaceRegions(first)) {
            long matching = 0;
            long sampled = 0;
            for (int y = region.top(); y < region.bottom(); y += 3) {
                for (int x = region.left(); x < region.right(); x += 3) {
                    sampled++;
                    if (first.getRGB(x, y) == later.getRGB(x, y)) matching++;
                }
            }
            if (sampled == 0 || matching < Math.ceil(sampled * 0.98)) return false;
        }
        return true;
    }

    private static void reencodePngWithUptime(Path source, Path destination, double uptimeSeconds)
            throws IOException {
        BufferedImage original = ImageIO.read(source.toFile());
        if (original == null) throw new IOException("Cannot decode sampled Android PNG: " + source);
        var writers = ImageIO.getImageWritersByFormatName("png");
        if (!writers.hasNext()) throw new IOException("No PNG writer is available for audited screenshot metadata");
        ImageWriter writer = writers.next();
        try (var output = ImageIO.createImageOutputStream(destination.toFile())) {
            writer.setOutput(output);
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            IIOMetadata metadata = writer.getDefaultImageMetadata(
                    ImageTypeSpecifier.createFromRenderedImage(original), parameters);
            String format = metadata.getNativeMetadataFormatName();
            if (!"javax_imageio_png_1.0".equals(format)) {
                throw new IOException("Unexpected PNG metadata format: " + format);
            }
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);
            IIOMetadataNode text = childNode(root, "tEXt");
            if (text == null) {
                text = new IIOMetadataNode("tEXt");
                root.appendChild(text);
            }
            IIOMetadataNode captureTime = new IIOMetadataNode("tEXtEntry");
            captureTime.setAttribute("keyword", "atfCaptureUptimeMs");
            captureTime.setAttribute("value", Long.toString(Math.round(uptimeSeconds * 1_000)));
            text.appendChild(captureTime);
            metadata.setFromTree(format, root);
            writer.write(null, new IIOImage(original, null, metadata), parameters);
        } finally {
            writer.dispose();
        }
        BufferedImage decoded = ImageIO.read(destination.toFile());
        if (!sameImagePixels(original, decoded)) {
            throw new IOException("Lossless screenshot re-encoding changed rendered pixels: " + destination);
        }
    }

    private static IIOMetadataNode childNode(IIOMetadataNode parent, String name) {
        for (int index = 0; index < parent.getLength(); index++) {
            org.w3c.dom.Node child = parent.item(index);
            if (child instanceof IIOMetadataNode metadataNode && metadataNode.getNodeName().equals(name)) {
                return metadataNode;
            }
        }
        return null;
    }

    private static boolean sameImagePixels(BufferedImage first, BufferedImage second) {
        if (second == null || first.getWidth() != second.getWidth() || first.getHeight() != second.getHeight()) {
            return false;
        }
        for (int y = 0; y < first.getHeight(); y++) {
            for (int x = 0; x < first.getWidth(); x++) {
                if (first.getRGB(x, y) != second.getRGB(x, y)) return false;
            }
        }
        return true;
    }

    private static List<PixelRegion> cardFaceRegions(BufferedImage image) {
        int width = Math.max(1, (int) Math.round(image.getWidth() * 0.12));
        int height = Math.max(1, (int) Math.round(image.getHeight() * 0.085));
        List<PixelRegion> result = new ArrayList<>();
        for (double centerY : List.of(0.35, 0.45)) {
            for (double centerX : List.of(0.436, 0.564)) {
                int x = (int) Math.round(image.getWidth() * centerX);
                int y = (int) Math.round(image.getHeight() * centerY);
                result.add(new PixelRegion(Math.max(0, x - width / 2), Math.max(0, y - height / 2),
                        Math.min(image.getWidth(), x + (width + 1) / 2),
                        Math.min(image.getHeight(), y + (height + 1) / 2)));
            }
        }
        return List.copyOf(result);
    }

    private record FrameSampler(String remoteDirectory, Path localDirectory) { }
    private record TrickFrame(Path image, double uptimeSeconds) { }
    private record TrickFramePair(TrickFrame first, TrickFrame later, long elapsedMillis) { }
    private record CardFaceMetrics(double ivoryFraction, double heartMarkFraction) { }
    private record PixelRegion(int left, int top, int right, int bottom) {
        int width() { return right - left; }
        int height() { return bottom - top; }
    }

    private static void checkpoint(AndroidUiFlowTrace trace, String phase, AtfGherkinKeyword keyword,
            String title, String target, List<Path> evidence, String operation, String expected,
            String observed, long durationMillis, List<AtfAssertion> assertions, AtfReportStatus status) throws Exception {
        Path screenshot = evidence.stream().filter(file -> file.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".png"))
                .findFirst().orElse(null);
        List<Path> supporting = evidence.stream().filter(file -> file.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".xml")).toList();
        AtfUiActionRecord action = new AtfUiActionRecord(trace.nextActionSequence(), operation, target,
                expected, observed, Math.max(0, durationMillis), status, AtfUiEvidenceSource.OBSERVE);
        trace.checkpoint(phase, keyword.text(), title, target, screenshot, List.of(action), assertions, status, supporting);
    }

    private static boolean isIvoryCardSurface(int rgb) {
        return ((rgb >>> 16) & 0xFF) >= 220
                && ((rgb >>> 8) & 0xFF) >= 220
                && (rgb & 0xFF) >= 210;
    }

    private static boolean overlaps(AndroidUiDriver.Bounds first, AndroidUiDriver.Bounds second) {
        int pixelRoundingTolerance = 1;
        return first.left() < second.right() - pixelRoundingTolerance
                && first.right() > second.left() + pixelRoundingTolerance
                && first.top() < second.bottom() - pixelRoundingTolerance
                && first.bottom() > second.top() + pixelRoundingTolerance;
    }

    private static boolean singleCardCentered(AndroidUiDriver.Bounds viewport,
            List<AndroidUiDriver.Bounds> cardBounds) {
        return cardBounds.size() == 1
                && singleCardCenterDelta(viewport, cardBounds) <= Math.max(1, Math.round(width(viewport) * 0.02))
                && cardBounds.getFirst().left() >= viewport.left()
                && cardBounds.getFirst().right() <= viewport.right()
                && cardBounds.getFirst().bottom() <= viewport.bottom();
    }

    private static int singleCardCenterDelta(AndroidUiDriver.Bounds viewport,
            List<AndroidUiDriver.Bounds> cardBounds) {
        return cardBounds.size() == 1
                ? Math.abs(centerX(viewport) - centerX(cardBounds.getFirst())) : -1;
    }

    private static double effectiveDensityScale(String output) {
        Matcher override = OVERRIDE_DENSITY.matcher(output);
        if (override.find()) return Integer.parseInt(override.group(1)) / 160.0;
        Matcher physical = PHYSICAL_DENSITY.matcher(output);
        if (physical.find()) return Integer.parseInt(physical.group(1)) / 160.0;
        throw new IllegalStateException("Android display density was not reported: " + output);
    }

    private static void overrideDisplay(AdbClient adb, AndroidUiDriver ui, DisplayProfile profile) throws Exception {
        runDisplaySetting(adb, ui, "Set emulator display resolution", "Android display resolution",
                "shell", "wm", "size", profile.width() + "x" + profile.height());
        runDisplaySetting(adb, ui, "Set emulator display density", "Android display density",
                "shell", "wm", "density", Integer.toString(profile.density()));
        Thread.sleep(700);
    }

    private static void restoreDisplay(AdbClient adb, AndroidUiDriver ui, String sizeOverride,
            String densityOverride) throws Exception {
        if (sizeOverride == null) {
            runDisplaySetting(adb, ui, "Restore emulator display resolution", "Android display resolution",
                    "shell", "wm", "size", "reset");
        } else {
            runDisplaySetting(adb, ui, "Restore emulator display resolution", "Android display resolution",
                    "shell", "wm", "size", sizeOverride);
        }
        if (densityOverride == null) {
            runDisplaySetting(adb, ui, "Restore emulator display density", "Android display density",
                    "shell", "wm", "density", "reset");
        } else {
            runDisplaySetting(adb, ui, "Restore emulator display density", "Android display density",
                    "shell", "wm", "density", densityOverride);
        }
        Thread.sleep(700);
    }

    private static void runDisplaySetting(AdbClient adb, AndroidUiDriver ui, String operation,
            String target, String... arguments) throws Exception {
        long started = System.nanoTime();
        try {
            var result = adb.run(arguments);
            result.requireSuccess();
            ui.recordAction(operation, target, String.join(" ", arguments),
                    result.output().isBlank() ? "ADB accepted display setting" : result.output().trim(),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(), AtfReportStatus.PASS);
        } catch (Exception failure) {
            ui.recordAction(operation, target, String.join(" ", arguments), failure.toString(),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(), AtfReportStatus.FAIL);
            throw failure;
        }
    }

    private static String overrideValue(Pattern pattern, String commandOutput) {
        Matcher matcher = pattern.matcher(commandOutput);
        return matcher.find() ? matcher.group(1) + "x" + matcher.group(2) : null;
    }

    private static String overrideDensityValue(String commandOutput) {
        Matcher matcher = OVERRIDE_DENSITY.matcher(commandOutput);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static boolean matchesRestoredDisplay(AdbClient adb, String originalSize, String originalDensity)
            throws Exception {
        String currentSize = adb.run("shell", "wm", "size").output();
        String currentDensity = adb.run("shell", "wm", "density").output();
        return displayState(currentSize, currentDensity).equals(displayState(originalSize, originalDensity));
    }

    private static String displayState(String sizeOutput, String densityOutput) {
        return "size=" + firstState(OVERRIDE_SIZE, PHYSICAL_SIZE, sizeOutput, true)
                + ";density=" + firstState(OVERRIDE_DENSITY, PHYSICAL_DENSITY, densityOutput, false);
    }

    private static String firstState(Pattern overridePattern, Pattern physicalPattern, String output,
            boolean dimensions) {
        Matcher override = overridePattern.matcher(output);
        if (override.find()) return "override:" + override.group(1)
                + (dimensions ? "x" + override.group(2) : "");
        Matcher physical = physicalPattern.matcher(output);
        if (physical.find()) return "physical:" + physical.group(1)
                + (dimensions ? "x" + physical.group(2) : "");
        return "unavailable";
    }

    private record DisplayProfile(String name, int width, int height, int density) {
        String fileKey() { return width + "x" + height + "-" + density + "dpi"; }
    }

    private static int width(AndroidUiDriver.Bounds bounds) { return bounds.right() - bounds.left(); }
    private static int height(AndroidUiDriver.Bounds bounds) { return bounds.bottom() - bounds.top(); }
    private static int centerX(AndroidUiDriver.Bounds bounds) { return (bounds.left() + bounds.right()) / 2; }
    private static int centerY(AndroidUiDriver.Bounds bounds) { return (bounds.top() + bounds.bottom()) / 2; }

    private static String reportSetting(String property, String environment, String fallback) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) value = System.getenv(environment);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private Path artifacts(String flow) {
        String configured = System.getProperty("atf.android.artifacts");
        Path root = configured == null || configured.isBlank()
                ? Path.of(System.getProperty("java.io.tmpdir"), "atf-j4-android-artifacts")
                : Path.of(configured);
        return root.resolve(flow);
    }
}
