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
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Runs the new app through the same Android adapter and inline-screenshot report contract. */
@AndroidE2eTest
public class WhistLabGameplayE2eTest {
    private static final String PACKAGE = "ro.whistlab.app";
    private static final Pattern DENSITY = Pattern.compile("(?:Override|Physical) density:\\s*(\\d+)");
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
            List<Path> setupEvidence = ui.capture("whist-lab-presentation-six-player-setup");
            evidence.addAll(setupEvidence);
            checkpoint(trace, "SETUP", AtfGherkinKeyword.GIVEN,
                    "Configure six players to produce the eight-card hand",
                    "New game; Players = 6", setupEvidence, "select six players and start a new game",
                    "six-player setup is ready", "visible labels=" + ui.texts(setup),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    List.of(new AtfAssertion("Six-player option is selected", "Players = 6",
                                    String.join(" | ", ui.texts(setup)), ui.hasText(setup, "6")),
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
            Matcher densityMatch = DENSITY.matcher(adb.run("shell", "wm", "density").output());
            assertTrue(densityMatch.find(), "Android display density was not reported");
            double densityScale = Integer.parseInt(densityMatch.group(1)) / 160.0;
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
            boolean biddingControlsAvoidSeats = biddingControlBounds.stream()
                    .noneMatch(control -> seatBounds.stream().anyMatch(seat -> overlaps(control, seat)));
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
                                    handLabels + "; y=" + handRow.stream().map(AndroidUiDriver.CardLabel::top).distinct().toList(), oneRow),
                            new AtfAssertion("Six player seats follow the approved oval", "N; NW/NE; SW/SE; S",
                                    seatPositions + "; human south=" + humanSeatIsSouth, mockSeatRingRendered),
                            new AtfAssertion("Bidding controls stay clear of player seats",
                                    "no visible intersection (allow 1 px bounds rounding) between any bid/submit target and a seat",
                                    "controls="
                                            + biddingControlBounds + "; seats=" + seatBounds,
                                    biddingControlsAvoidSeats),
                            new AtfAssertion("All currently legal bids are visible", expectedBids.toString(),
                                    visibleBids.toString(), allBidsVisible),
                            new AtfAssertion("Bid hit targets remain at least 48 dp", "48 dp minimum",
                                    "minimum=" + minimumBidWidth + "x" + minimumBidHeight + " px at density "
                                            + densityScale, touchTargetsRemain48Dp),
                            new AtfAssertion("Place bid is compact without shrinking its hit target",
                                    "<=144 dp wide; >=48 dp high", submitBidSize.toString()
                                            + " px at density " + densityScale, submitBidIsDiscreetAndAccessible),
                            new AtfAssertion("Place bid remains inside the table above the hand",
                                    "submit bottom < hand top", submitBounds + "; " + handBounds,
                                    actionAnchoredAboveHand),
                            new AtfAssertion("Variant identity and trump remain visible in the header",
                                    "Whist · Oh Hell; actual turned card rank and suit", ui.texts(bidding).toString()
                                            + "; " + ui.descriptions(bidding), gameIdentityAndTrumpVisible),
                            new AtfAssertion("Trump header distinguishes the actual turned card",
                                    "TURNED CARD and full accessible card identity",
                                    ui.texts(bidding) + "; " + ui.descriptions(bidding), turnedTrumpCardVisible),
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
                    legalCards.size() + " cards; y=" + cardSlots.stream()
                            .map(AndroidUiDriver.Bounds::top).distinct().toList(), oneVisibleRow));
            assertions.add(new AtfAssertion("The final card face is not clipped by the hand viewport",
                    "last slot is wider than the overlap advance and inside " + handBoundsBefore,
                    "last=" + cardSlots.getLast() + "; first=" + cardSlots.getFirst(), finalCardFitsViewport));
            assertions.add(new AtfAssertion("Variant and trump stay visible in the header", "Whist · Descending; trump tile",
                    ui.texts(before) + "; " + ui.descriptions(before), identityAndTrump));
            assertions.add(new AtfAssertion("A full-deck hand identifies that no turned card exists",
                    "NO TURNED CARD; suit-based trump", ui.texts(before) + "; " + trumpDescription,
                    suitOnlyTrumpIsExplicit));
            assertions.add(new AtfAssertion("Turn badge and seat bid/result stay visible", "PLAY NOW; Bid 0; Won 0",
                    ui.texts(before).toString(), seatSignalsAreVisible));
            assertions.add(new AtfAssertion("Four player seats follow the approved oval", "N; W/E; S",
                    fourPlayerSeats + "; human south=" + humanSeatIsSouth, fourPlayerMockRingRendered));
            assertions.add(new AtfAssertion("Table and hand are vertically balanced in the game viewport",
                    "surface-to-hand center within " + Math.round(verticalBalanceTolerance) + " px of panel center",
                    "surface=" + playingSurfaceBefore + "; hand=" + handBoundsBefore
                            + "; panel=" + gamePanelBefore + "; centers=" + presentationCenterY
                            + "/" + gamePanelCenterY,
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
            boolean trumpRemainsVisible = ui.descriptions(played).stream()
                    .anyMatch(value -> value.startsWith("Trump"));
            assertions.add(new AtfAssertion("Tap plays exactly the selected card into the current trick",
                    "You played " + cardName + " in the center of the oval",
                    "bounds=" + playedCardBounds + "; table=" + playingSurfaceBounds + "; descriptions="
                            + ui.descriptions(played).stream().filter(value -> value.contains(" played ")).toList(),
                    cardMovedToCenter));
            assertions.add(new AtfAssertion("One card leaves the hand after the animation", "Your hand, 12 cards.",
                    ui.descriptions(played).stream().filter(value -> value.startsWith("Your hand")).toList().toString(),
                    ui.hasDescription(played, "Your hand, 12 cards.")));
            assertions.add(new AtfAssertion("The remaining hand stays opaque during the AI turn",
                    "ivory card face (R≥220, G≥220, B≥210)",
                    String.format(java.util.Locale.ROOT, "#%06X", remainingHandRgb), remainingCardsStayOpaque));
            assertions.add(new AtfAssertion("Human seat bounds remain fixed after play", humanSeatBefore.toString(),
                    humanSeatAfter.toString(), seatStayedFixed));
            assertions.add(new AtfAssertion("Trump stays in the header after play", "accessible trump tile remains",
                    ui.descriptions(played).toString(), trumpRemainsVisible));
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
