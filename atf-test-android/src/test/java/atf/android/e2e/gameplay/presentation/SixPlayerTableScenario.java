package atf.android.e2e.gameplay.presentation;

import static atf.android.e2e.display.support.GameplayDisplay.effectiveDensityScale;
import static atf.android.e2e.display.support.GameplayGeometry.overlaps;
import static atf.android.e2e.display.support.ScreenGeometry.centerX;
import static atf.android.e2e.display.support.ScreenGeometry.height;
import static atf.android.e2e.display.support.ScreenGeometry.width;
import static atf.android.e2e.support.flow.UiFlowCheckpoint.checkpoint;
import static atf.android.e2e.support.flow.WhistLabScenario.PACKAGE;
import static atf.android.e2e.support.flow.WhistLabScenario.artifacts;
import static atf.android.e2e.support.flow.WhistLabScenario.installedApp;
import static atf.android.e2e.support.flow.WhistLabScenario.reportSetting;

import static org.junit.jupiter.api.Assertions.assertTrue;

import atf.android.AdbClient;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.support.AndroidUiFlowReportPublisher;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Scenariul de prezentare și navigare al mesei cu șase jucători. */
public final class SixPlayerTableScenario {
    private SixPlayerTableScenario() {}

    private static final Pattern SIX_PLAYER_EIGHT_CARD_BID_SUMMARY =
            Pattern.compile("Bids submitted (\\d+)/6\\RBid total (-?\\d+)/8");

    public static void run(String ownerName) throws Exception {
        var installation = installedApp();
        AdbClient adb = installation.adb();

        Path artifacts = artifacts("table-presentation");
        AndroidUiDriver ui = new AndroidUiDriver(adb, artifacts);
        AndroidUiFlowTrace trace =
                new AndroidUiFlowTrace(
                        artifacts,
                        "whist-lab-table-presentation",
                        "Whist Lab — eight-card table, compact bidding, and hamburger navigation",
                        ownerName,
                        "atf.android.e2e",
                        PACKAGE,
                        List.of("android", "e2e", "ui", "table-layout", "navigation", "sanity"));
        ui.recordCapturesAs(trace::capture);
        trace.beginPhase("SETUP");
        ui.recordActionsAs(trace::recordAction);
        List<Path> evidence = new ArrayList<>();
        AtfReportStatus outcome = AtfReportStatus.FAIL;
        String checks = "Presentation flow did not complete.";
        try {
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
            trace.beginPhase("TEST BODY");

            started = System.nanoTime();
            ui.tapTextIn(setup, "Start game");
            String bidding = ui.awaitText("Place bid", Duration.ofSeconds(15));
            String initialBidSummary =
                    ui.texts(bidding).stream()
                            .filter(label -> label.startsWith("Bids submitted "))
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "Bid summary was not visible before opening the score sheet"));
            List<AndroidUiDriver.CardLabel> handRow = ui.bottomRowCardLabels(bidding);
            List<String> handLabels =
                    handRow.stream().map(AndroidUiDriver.CardLabel::text).toList();
            boolean oneRow =
                    handRow.size() == 8
                            && handRow.stream()
                                            .map(AndroidUiDriver.CardLabel::top)
                                            .distinct()
                                            .count()
                                    == 1;
            Matcher bidSummaryMatch = SIX_PLAYER_EIGHT_CARD_BID_SUMMARY.matcher(initialBidSummary);
            assertTrue(
                    bidSummaryMatch.matches(), "Unexpected bidding summary: " + initialBidSummary);
            int submittedBids = Integer.parseInt(bidSummaryMatch.group(1));
            int currentBidTotal = Integer.parseInt(bidSummaryMatch.group(2));
            LinkedHashSet<Integer> expectedBids =
                    new LinkedHashSet<>(
                            java.util.stream.IntStream.rangeClosed(0, 8).boxed().toList());
            if (submittedBids == 5) {
                int forbiddenFinalBid = 8 - currentBidTotal;
                if (forbiddenFinalBid >= 0 && forbiddenFinalBid <= 8)
                    expectedBids.remove(forbiddenFinalBid);
            }
            List<Integer> visibleBids = ui.numericButtonValues(bidding);
            boolean allBidsVisible =
                    visibleBids.size() == expectedBids.size()
                            && new LinkedHashSet<>(visibleBids).equals(expectedBids);
            List<AndroidUiDriver.ElementSize> bidSizes = ui.numericButtonSizes(bidding);
            double densityScale = effectiveDensityScale(adb.run("shell", "wm", "density").output());
            int minimumBidWidth =
                    bidSizes.stream()
                            .mapToInt(AndroidUiDriver.ElementSize::widthPixels)
                            .min()
                            .orElse(0);
            int minimumBidHeight =
                    bidSizes.stream()
                            .mapToInt(AndroidUiDriver.ElementSize::heightPixels)
                            .min()
                            .orElse(0);
            boolean touchTargetsRemain48Dp =
                    bidSizes.size() == expectedBids.size()
                            && minimumBidWidth >= Math.round(48 * densityScale * 0.95)
                            && minimumBidHeight >= Math.round(48 * densityScale * 0.95);
            AndroidUiDriver.ElementSize submitBidSize = ui.buttonSize(bidding, "Place bid");
            boolean submitBidIsDiscreetAndAccessible =
                    submitBidSize.widthPixels() <= Math.round(144 * densityScale)
                            && submitBidSize.heightPixels() >= Math.round(48 * densityScale * 0.95);
            AndroidUiDriver.Bounds submitBounds = ui.boundsForText(bidding, "Place bid");
            AndroidUiDriver.Bounds handBounds =
                    ui.boundsForDescription(bidding, "Your hand, 8 cards.");
            boolean actionAnchoredAboveHand = submitBounds.bottom() < handBounds.top();
            boolean turnedTrumpCardVisible = ui.turnedTrumpIdentityMatchesVisibleCard(bidding);
            boolean gameIdentityAndTrumpVisible =
                    ui.hasText(bidding, "Whist · Oh Hell") && turnedTrumpCardVisible;
            var boardBounds = ui.boundsForDescription(bidding, "Playing surface");
            List<AndroidUiDriver.Bounds> seatBounds =
                    ui.descriptions(bidding).stream()
                            .filter(value -> value.contains(". Bid ") && value.contains(". Score "))
                            .map(
                                    description -> {
                                        try {
                                            return ui.boundsForDescription(bidding, description);
                                        } catch (Exception failure) {
                                            throw new IllegalStateException(failure);
                                        }
                                    })
                            .toList();
            List<AndroidUiDriver.Bounds> biddingControlBounds = new ArrayList<>();
            for (int bid : visibleBids)
                biddingControlBounds.add(ui.buttonBounds(bidding, Integer.toString(bid)));
            biddingControlBounds.add(submitBounds);
            long biddingSeatOverlaps =
                    biddingControlBounds.stream()
                            .flatMap(
                                    control ->
                                            seatBounds.stream()
                                                    .filter(seat -> overlaps(control, seat)))
                            .count();
            boolean biddingControlsAvoidSeats = biddingSeatOverlaps == 0;
            List<String> seatPositions =
                    seatBounds.stream()
                            .map(
                                    bounds ->
                                            String.format(
                                                    java.util.Locale.ROOT,
                                                    "x=%.2f,y=%.2f",
                                                    (centerX(bounds) - boardBounds.left())
                                                            / (double) width(boardBounds),
                                                    (bounds.top() - boardBounds.top())
                                                            / (double) height(boardBounds)))
                            .toList();
            double tableCenterX = boardBounds.left() + width(boardBounds) / 2.0;
            AndroidUiDriver.Bounds humanSeat = ui.boundsForDescriptionContaining(bidding, "You. ");
            boolean humanSeatIsSouth =
                    Math.abs(centerX(humanSeat) - tableCenterX) <= width(boardBounds) * 0.16
                            && (humanSeat.top() - boardBounds.top()) >= height(boardBounds) * 0.78;
            long northSeats =
                    seatBounds.stream()
                            .filter(
                                    bounds ->
                                            (bounds.top() - boardBounds.top())
                                                            <= height(boardBounds) * 0.12
                                                    && Math.abs(centerX(bounds) - tableCenterX)
                                                            <= width(boardBounds) * 0.16)
                            .count();
            long upperFlankSeats =
                    seatBounds.stream()
                            .filter(
                                    bounds -> {
                                        double y =
                                                (bounds.top() - boardBounds.top())
                                                        / (double) height(boardBounds);
                                        double x =
                                                (centerX(bounds) - boardBounds.left())
                                                        / (double) width(boardBounds);
                                        return y >= 0.13 && y <= 0.40 && (x <= 0.32 || x >= 0.68);
                                    })
                            .count();
            long lowerFlankSeats =
                    seatBounds.stream()
                            .filter(
                                    bounds -> {
                                        double y =
                                                (bounds.top() - boardBounds.top())
                                                        / (double) height(boardBounds);
                                        double x =
                                                (centerX(bounds) - boardBounds.left())
                                                        / (double) width(boardBounds);
                                        return y >= 0.55 && y <= 0.82 && (x <= 0.32 || x >= 0.68);
                                    })
                            .count();
            boolean mockSeatRingRendered =
                    seatBounds.size() == 6
                            && humanSeatIsSouth
                            && northSeats == 1
                            && upperFlankSeats == 2
                            && lowerFlankSeats == 2;
            assertTrue(
                    oneRow,
                    "Expected all eight hand cards on one row; visible bottom-row cards="
                            + handLabels);
            assertTrue(
                    allBidsVisible,
                    "Visible choices differ from the legal bid set "
                            + expectedBids
                            + "; actual="
                            + visibleBids);
            assertTrue(
                    touchTargetsRemain48Dp,
                    "Bid choices fell below 48 x 48 dp touch bounds: " + bidSizes);
            assertTrue(
                    submitBidIsDiscreetAndAccessible,
                    "Place bid should stay compact (<=144 dp wide) with a 48 dp touch height: "
                            + submitBidSize);
            assertTrue(
                    actionAnchoredAboveHand,
                    "Place bid should sit inside the table above the fixed hand row: submit="
                            + submitBounds
                            + "; hand="
                            + handBounds);
            assertTrue(
                    gameIdentityAndTrumpVisible,
                    "Variant title and the persistent trump tile must remain visible: "
                            + ui.texts(bidding)
                            + "; descriptions="
                            + ui.descriptions(bidding));
            assertTrue(
                    mockSeatRingRendered,
                    "Six player seats must match the mock's north, upper flanks, lower flanks, and south positions: "
                            + seatPositions
                            + "; human south="
                            + humanSeatIsSouth);
            assertTrue(
                    biddingControlsAvoidSeats,
                    "Bidding controls overlap a player seat: controls="
                            + biddingControlBounds
                            + "; seats="
                            + seatBounds);
            List<Path> biddingEvidence = ui.capture("whist-lab-presentation-eight-card-table");
            evidence.addAll(biddingEvidence);
            Path biddingScreenshot =
                    biddingEvidence.stream()
                            .filter(path -> path.toString().endsWith(".png"))
                            .findFirst()
                            .orElseThrow();
            String biddingDisplaySize = adb.run("shell", "wm", "size").output();
            boolean feltContinuesBelowTable =
                    ui.sameBackgroundAcrossTableMargin(
                            biddingScreenshot, 0.55, 0.87, biddingDisplaySize);
            assertTrue(
                    feltContinuesBelowTable,
                    "The game surface changes color between the table and space below the action");
            String turnedTrumpDescription =
                    ui.descriptions(bidding).stream()
                            .filter(value -> value.startsWith("Trump set by turned card: "))
                            .findFirst()
                            .orElse("<missing>");
            String visibleVariant =
                    ui.texts(bidding).stream()
                            .filter(value -> value.startsWith("Whist · "))
                            .findFirst()
                            .orElse("<missing>");
            checkpoint(
                    trace,
                    "TEST BODY",
                    AtfGherkinKeyword.THEN,
                    "Render all eight cards and the complete compact bidding grid",
                    "Six-player Oh Hell bidding table",
                    biddingEvidence,
                    "inspect all hand labels, legal bid values, and button bounds",
                    "eight cards share one row; legal bids and 48 dp targets remain visible; identity and trump remain in the header; Place bid sits in the table above the hand",
                    "hand="
                            + handLabels
                            + "; expected bids="
                            + expectedBids
                            + "; actual bids="
                            + visibleBids
                            + "; bid button bounds px="
                            + bidSizes
                            + "; Place bid bounds px="
                            + submitBidSize
                            + "; table action bounds="
                            + submitBounds
                            + "; hand bounds="
                            + handBounds
                            + "; identity and trump visible="
                            + gameIdentityAndTrumpVisible
                            + "; continuous felt background="
                            + feltContinuesBelowTable
                            + "; visible labels="
                            + ui.texts(bidding),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    List.of(
                            new AtfAssertion(
                                    "All eight hand cards occupy one row",
                                    "8 cards; 1 row",
                                    handLabels.size()
                                            + " cards; rows="
                                            + handRow.stream()
                                                    .map(AndroidUiDriver.CardLabel::top)
                                                    .distinct()
                                                    .count(),
                                    oneRow),
                            new AtfAssertion(
                                    "Six player seats follow the approved oval",
                                    "N; NW/NE; SW/SE; S",
                                    "seats="
                                            + seatBounds.size()
                                            + "; N="
                                            + northSeats
                                            + "; upper="
                                            + upperFlankSeats
                                            + "; lower="
                                            + lowerFlankSeats
                                            + "; south="
                                            + humanSeatIsSouth,
                                    mockSeatRingRendered),
                            new AtfAssertion(
                                    "Bidding controls stay clear of player seats",
                                    "0 intersections (1 px bounds tolerance)",
                                    "intersections=" + biddingSeatOverlaps,
                                    biddingControlsAvoidSeats),
                            new AtfAssertion(
                                    "All currently legal bids are visible",
                                    expectedBids.toString(),
                                    visibleBids.toString(),
                                    allBidsVisible),
                            new AtfAssertion(
                                    "Bid hit targets remain at least 48 dp",
                                    "48 dp minimum",
                                    "minimum="
                                            + minimumBidWidth
                                            + "x"
                                            + minimumBidHeight
                                            + " px; density="
                                            + String.format(
                                                    java.util.Locale.ROOT, "%.2f", densityScale),
                                    touchTargetsRemain48Dp),
                            new AtfAssertion(
                                    "Place bid is compact without shrinking its hit target",
                                    "<=144 dp wide; >=48 dp high",
                                    submitBidSize.toString() + " px at density " + densityScale,
                                    submitBidIsDiscreetAndAccessible),
                            new AtfAssertion(
                                    "Place bid remains inside the table above the hand",
                                    "submit bottom < hand top",
                                    submitBounds.bottom() + " < " + handBounds.top(),
                                    actionAnchoredAboveHand),
                            new AtfAssertion(
                                    "Variant identity and trump remain visible in the header",
                                    "Whist · Oh Hell; turned card rank and suit",
                                    visibleVariant + "; " + turnedTrumpDescription,
                                    gameIdentityAndTrumpVisible),
                            new AtfAssertion(
                                    "Trump header distinguishes the actual turned card",
                                    "Visible rank and suit match full accessible card identity",
                                    "matching face="
                                            + turnedTrumpCardVisible
                                            + "; "
                                            + turnedTrumpDescription,
                                    turnedTrumpCardVisible),
                            new AtfAssertion(
                                    "Felt remains continuous from the table frame to the lower viewport",
                                    "same table-margin color at 55% and 87% height",
                                    Boolean.toString(feltContinuesBelowTable),
                                    feltContinuesBelowTable)),
                    AtfReportStatus.PASS);

            started = System.nanoTime();
            int pendingBid = expectedBids.contains(3) ? 3 : 2;
            ui.selectBid(pendingBid);
            ui.tapDescription("Game menu");
            String menu = ui.awaitText("Score sheet", Duration.ofSeconds(8));
            assertTrue(ui.hasText(menu, "Main menu"), "Game menu omitted Main menu");
            List<Path> menuEvidence = ui.capture("whist-lab-presentation-hamburger-menu");
            evidence.addAll(menuEvidence);
            checkpoint(
                    trace,
                    "TEST BODY",
                    AtfGherkinKeyword.WHEN,
                    "Open the top-left hamburger after selecting a bid",
                    "Game menu",
                    menuEvidence,
                    "select bid " + pendingBid + " and activate Game menu",
                    "Score sheet and Main menu are available",
                    "visible labels=" + ui.texts(menu),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    List.of(
                            new AtfAssertion(
                                    "Score sheet is in the game menu",
                                    "Score sheet",
                                    Boolean.toString(ui.hasText(menu, "Score sheet")),
                                    ui.hasText(menu, "Score sheet")),
                            new AtfAssertion(
                                    "Main menu is in the game menu",
                                    "Main menu",
                                    Boolean.toString(ui.hasText(menu, "Main menu")),
                                    ui.hasText(menu, "Main menu"))),
                    AtfReportStatus.PASS);

            started = System.nanoTime();
            ui.tapText("Score sheet");
            String score = ui.awaitText("CLOSE", Duration.ofSeconds(8));
            boolean selectionWasNotSubmitted = ui.hasTextContaining(score, "Bid not placed");
            assertTrue(
                    selectionWasNotSubmitted,
                    "Opening Score sheet submitted the unconfirmed bid; visible="
                            + ui.texts(score));
            List<String> expectedScoreColumns = List.of("Player", "Bid", "Won", "Hand", "Total");
            List<String> visibleScoreColumns = new ArrayList<>();
            for (String label : expectedScoreColumns) {
                if (ui.hasText(score, label)) visibleScoreColumns.add(label);
            }
            boolean scoreColumnsVisible = visibleScoreColumns.size() == expectedScoreColumns.size();
            assertTrue(
                    scoreColumnsVisible,
                    "Current score table omitted a column: " + ui.texts(score));
            List<Path> scoreEvidence = ui.capture("whist-lab-presentation-live-score");
            evidence.addAll(scoreEvidence);
            checkpoint(
                    trace,
                    "TEST BODY",
                    AtfGherkinKeyword.THEN,
                    "Open the read-only score sheet without submitting the selected bid",
                    "Current scores · Hand 1",
                    scoreEvidence,
                    "open Score sheet and inspect the human score row",
                    "unsubmitted bid remains unsubmitted; Player/Bid/Won/Hand/Total columns are aligned",
                    "visible labels=" + ui.texts(score),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    List.of(
                            new AtfAssertion(
                                    "Score view does not commit the pending bid",
                                    "Bid not placed",
                                    Boolean.toString(selectionWasNotSubmitted),
                                    selectionWasNotSubmitted),
                            new AtfAssertion(
                                    "Score sheet presents aligned player and score columns",
                                    "Player · Bid · Won · Hand · Total",
                                    String.join(" · ", visibleScoreColumns),
                                    scoreColumnsVisible)),
                    AtfReportStatus.PASS);

            started = System.nanoTime();
            ui.tapTextIn(score, "CLOSE");
            String biddingAfterScore = ui.awaitText("Place bid", Duration.ofSeconds(8));
            boolean selectedBidPersisted =
                    ui.isSelected(biddingAfterScore, Integer.toString(pendingBid));
            String summaryAfterScore =
                    ui.texts(biddingAfterScore).stream()
                            .filter(label -> label.startsWith("Bids submitted "))
                            .findFirst()
                            .orElse("<missing bid summary>");
            boolean bidSummaryPersisted = initialBidSummary.equals(summaryAfterScore);
            assertTrue(
                    selectedBidPersisted, "Closing Score sheet lost the unconfirmed bid selection");
            assertTrue(
                    bidSummaryPersisted,
                    "Score navigation changed submitted bids or bid total: before='"
                            + initialBidSummary
                            + "', after='"
                            + summaryAfterScore
                            + "'");
            List<Path> restoredTableEvidence =
                    ui.capture("whist-lab-presentation-restored-bidding");
            evidence.addAll(restoredTableEvidence);
            checkpoint(
                    trace,
                    "TEST BODY",
                    AtfGherkinKeyword.AND,
                    "Close Score sheet and verify the same bid remains selected",
                    "Bidding table; value " + pendingBid,
                    restoredTableEvidence,
                    "close Score sheet and inspect the selected bid",
                    "bid " + pendingBid + " remains selected and no bid has been submitted",
                    "selected="
                            + selectedBidPersisted
                            + "; summary="
                            + summaryAfterScore
                            + "; visible labels="
                            + ui.texts(biddingAfterScore),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    List.of(
                            new AtfAssertion(
                                    "Score view preserves the pending selection",
                                    "Bid " + pendingBid + " selected",
                                    Boolean.toString(selectedBidPersisted),
                                    selectedBidPersisted),
                            new AtfAssertion(
                                    "Score navigation leaves the bid summary unchanged",
                                    initialBidSummary,
                                    summaryAfterScore,
                                    bidSummaryPersisted)),
                    AtfReportStatus.PASS);

            trace.beginPhase("TEARDOWN");
            started = System.nanoTime();
            ui.tapDescription("Game menu");
            ui.tapText("Main menu");
            String returnedHome = ui.awaitText("New game", Duration.ofSeconds(10));
            List<Path> returnedHomeEvidence = ui.capture("whist-lab-presentation-return-home");
            evidence.addAll(returnedHomeEvidence);
            List<String> expectedHomeItems = List.of("New game", "Resume game", "How to play");
            List<String> visibleHomeItems = new ArrayList<>();
            for (String label : expectedHomeItems) {
                if (ui.hasText(returnedHome, label)) visibleHomeItems.add(label);
            }
            boolean homeReady = visibleHomeItems.size() == expectedHomeItems.size();
            assertTrue(homeReady, "Main menu did not restore the home navigation options");
            checkpoint(
                    trace,
                    "TEARDOWN",
                    AtfGherkinKeyword.THEN,
                    "Return to home from the game menu without advancing play",
                    "Main menu",
                    returnedHomeEvidence,
                    "close Score sheet; open Game menu; choose Main menu",
                    "home screen offers New game, Resume game, and How to play",
                    "visible labels=" + ui.texts(returnedHome),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    List.of(
                            new AtfAssertion(
                                    "Home navigation is restored",
                                    String.join("; ", expectedHomeItems),
                                    String.join("; ", visibleHomeItems),
                                    homeReady)),
                    AtfReportStatus.PASS);
            checks =
                    "Six-player Oh Hell opened an eight-card human hand. All eight cards rendered on one row; every bid permitted by the current final-bid rule remained visible and each hit target measured at least 48 dp. The hamburger exposed Score sheet and Main menu. A selected but unconfirmed bid remained unsubmitted in the score view. The flow returned home without submitting a bid or playing a card.";
            outcome = AtfReportStatus.PASS;
            trace.finish(outcome);
        } catch (Throwable failure) {
            outcome = AtfReportStatus.FAIL;
            trace.fail(failure);
            if (failure instanceof Exception exception) throw exception;
            if (failure instanceof Error error) throw error;
            throw new RuntimeException(failure);
        } finally {
            trace.beginPhase("TEARDOWN");
            try {
                adb.run("shell", "am", "force-stop", PACKAGE).requireSuccess();
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
            AndroidUiFlowReportPublisher.publish(
                    artifacts.resolve("whist-lab-table-presentation.html"),
                    installation.packagePath(),
                    reportSetting("atf.android.serial", "ATF_ANDROID_SERIAL", "emulator-5554"),
                    System.getProperty("atf.android.api", "device reported at runtime"),
                    checks,
                    reportEvidence,
                    trace);
        }
    }
}
