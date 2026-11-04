package atf.android.e2e.display.flow;

import atf.android.e2e.activation.AndroidE2eTest;
import atf.android.e2e.display.profile.ResponsiveDisplayProfile;
import atf.android.e2e.driver.AndroidUiDriver;

import atf.android.AdbClient;
import atf.android.e2e.support.AndroidUiFlowReportPublisher;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.android.whist.evidence.HandIndexAudit;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AndroidE2eTest
class WhistResponsivePlaySanityE2eTest {
    private static final String APP = "ro.whistlab.app";
    private static final Duration WAIT = Duration.ofSeconds(35);
    private static final int HAND_CARD_FACE_HEIGHT_DP = 98;
    private static final int PLAYED_CARD_LABEL_AND_PADDING_DP = 18;
    private static final String OUTPUT = "atf.android.artifacts";
    private static final Pattern HAND_SIZE = Pattern.compile("Your hand, (\\d+) cards\\.");
    private static final Pattern DEAL_NUMBER = Pattern.compile("Hand (\\d+) / \\d+");

    @Test
    void configuredProfilesPassMenuAndDisplaySanity() throws Exception {
        AdbClient adb = AdbClient.fromSystemProperties();
        List<ResponsiveDisplayProfile> profiles = ResponsiveDisplayProfile.parse(
                System.getProperty("atf.android.sanity.profiles"));
        int hands = ResponsiveDisplayProfile.handsToPlay(
                System.getProperty("atf.android.sanity.hands", "1"));
        boolean includeGameFlow = Boolean.getBoolean("atf.android.sanity.includeGameFlow");
        String configured = System.getProperty(OUTPUT);
        Path root = Path.of(configured == null || configured.isBlank()
                ? Path.of(System.getProperty("java.io.tmpdir"), "atf-j4-responsive-sanity").toString()
                : configured).resolve("responsive-ui-sanity");
        AndroidUiDriver ui = new AndroidUiDriver(adb, root);
        AndroidUiFlowTrace trace = new AndroidUiFlowTrace(root, "responsive-ui-sanity",
                "Whist Lab — responsive menu, table and card display checks",
                getClass().getSimpleName(), "atf.android.e2e", APP,
                List.of("android", "e2e", "ui", "responsive", "sanity"));
        ui.recordCapturesAs(trace::capture);
        trace.beginPhase("SETUP");
        ui.recordActionsAs(trace::recordAction);
        List<Path> evidence = new ArrayList<>();
        AtfReportStatus outcome = AtfReportStatus.FAIL;
        Throwable failure = null;
        String originalSize = null;
        String originalDensity = null;
        boolean hardAiCompatibilityRequested = Boolean.getBoolean(
                "atf.android.sanity.hardAiCompatibility");

        try {
            originalSize = adb.run("shell", "wm", "size").output();
            originalDensity = adb.run("shell", "wm", "density").output();
            trace.beginPhase("TEST BODY");
            boolean hardAiCompatibilityChecked = !hardAiCompatibilityRequested;
            boolean primaryProfile = true;
            for (ResponsiveDisplayProfile profile : profiles) {
                applyProfile(adb, ui, profile);
                Thread.sleep(700);
                if (!hardAiCompatibilityChecked) {
                    inspectHardAiBid(adb, ui, trace, evidence, profile);
                    hardAiCompatibilityChecked = true;
                }
                inspectPlayedCard(adb, ui, trace, evidence, profile);
                if (primaryProfile && includeGameFlow) {
                    playShortGame(adb, ui, trace, evidence, profile, hands);
                } else {
                    inspectHomeAndSetup(ui, adb, trace, evidence, profile);
                }
                inspectThirteenCardFan(adb, ui, trace, evidence, profile);
                inspectNineBidTable(adb, ui, trace, evidence, profile);
                primaryProfile = false;
            }
            outcome = AtfReportStatus.PASS;
        } catch (Throwable caught) {
            failure = caught;
            trace.fail(caught);
        }

        trace.beginPhase("TEARDOWN");
        try {
            if (originalSize != null && originalDensity != null) restoreDisplay(adb, ui, originalSize, originalDensity);
            adb.run("shell", "am", "force-stop", APP).requireSuccess();
        } catch (Throwable cleanup) {
            trace.fail(cleanup);
            if (failure == null) failure = cleanup;
            else failure.addSuppressed(cleanup);
        }

        if (failure != null) outcome = AtfReportStatus.FAIL;
        trace.finish(outcome);
        evidence.add(trace.manifest());
        try {
            AndroidUiFlowReportPublisher.publish(root.resolve("responsive-ui-sanity.html"), APP,
                    System.getProperty("atf.android.serial", System.getenv("ATF_ANDROID_SERIAL")),
                    System.getProperty("atf.android.api", "read from Android at runtime"),
                    "Configured portrait profiles use display fixtures; completed game flow runs once "
                            + "on the first profile only when enabled. Hands=" + hands + "; game flow="
                            + includeGameFlow + "; Hard-AI compatibility=" + hardAiCompatibilityRequested + ".",
                    evidence, trace);
        } catch (Throwable publishing) {
            if (failure == null) failure = publishing;
            else failure.addSuppressed(publishing);
        }
        if (failure instanceof Exception exception) throw exception;
        if (failure instanceof Error error) throw error;
        if (failure != null) throw new RuntimeException(failure);
        assertEquals(AtfReportStatus.PASS, outcome);
    }

    private static void inspectHomeAndSetup(AndroidUiDriver ui, AdbClient adb,
            AndroidUiFlowTrace trace, List<Path> evidence, ResponsiveDisplayProfile profile) throws Exception {
        String home = ui.restartToActivity(APP, ".MainActivity", "--es", "debug_language", "EN");
        record(ui, adb, trace, evidence, profile, "Open the normal home menu",
                List.of(check("New game and language are visible",
                                "New game and one language selector",
                                ui.hasText(home, "New game") && ui.hasClass(home, "android.widget.Spinner")),
                        check("Home fits the first viewport",
                                "all visible home content inside its scroll viewport",
                                ui.allContentFitsViewport(home))));

        ui.tapText("New game");
        String setup = ui.awaitText("Start game", WAIT);
        ui.selectNamedSpinnerOption("player-count", "4");
        ui.selectNamedSpinnerOption("game-type", "Romanian (1–8–1)");
        setup = ui.awaitText("Start game", WAIT);
        record(ui, adb, trace, evidence, profile, "Set up a four-player Romanian table",
                List.of(check("Selected game type", "Romanian (1–8–1)",
                                ui.spinnerValue(setup, "Game type").equals("Romanian (1–8–1)")),
                        check("Start remains visible without scrolling",
                                "Start game and full setup inside viewport",
                                ui.hasText(setup, "Start game") && ui.allContentFitsViewport(setup))));
    }

    private static void playShortGame(AdbClient adb, AndroidUiDriver ui, AndroidUiFlowTrace trace,
            List<Path> evidence, ResponsiveDisplayProfile profile, int hands) throws Exception {
        String home = ui.restartToActivity(APP, ".MainActivity", "--es", "debug_language", "EN");
        record(ui, adb, trace, evidence, profile, "Open the normal home menu",
                List.of(check("New game and language are visible",
                                "New game and one language selector",
                                ui.hasText(home, "New game") && ui.hasClass(home, "android.widget.Spinner")),
                        check("Home fits the first viewport",
                                "all visible home content inside its scroll viewport",
                                ui.allContentFitsViewport(home))));

        ui.tapText("New game");
        String setup = ui.awaitText("Start game", WAIT);
        ui.selectNamedSpinnerOption("player-count", "4");
        ui.selectNamedSpinnerOption("game-type", "Romanian (1–8–1)");
        setup = ui.awaitText("Start game", WAIT);
        record(ui, adb, trace, evidence, profile, "Set up a real four-player Romanian game",
                List.of(check("Selected game type", "Romanian (1–8–1)",
                                ui.spinnerValue(setup, "Game type").equals("Romanian (1–8–1)")),
                        check("Start remains visible without scrolling",
                                "Start game and full setup inside viewport",
                                ui.hasText(setup, "Start game") && ui.allContentFitsViewport(setup))));

        ui.tapText("Start game");
        for (int handNumber = 1; handNumber <= hands; handNumber++) {
            String bidding = ui.awaitText("Place bid", WAIT);
            int handSize = handSize(ui, bidding);
            int displayedDealNumber = dealNumber(ui, bidding);
            assertEquals(handNumber, displayedDealNumber, "The Romanian deal sequence did not advance");
            assertEquals(romanianFourPlayerHandSize(handNumber), handSize,
                    "Unexpected 1–8–1 hand size at deal " + handNumber);
            recordBidding(ui, adb, trace, evidence, profile, handNumber, handSize, bidding);

            List<Integer> legalBids = AndroidUiDriver.enabledNumericButtonValues(bidding);
            assertTrue(!legalBids.isEmpty(), "No enabled contract is available");
            int bid = legalBids.getFirst();
            ui.selectBid(bid);
            ui.tapText("Place bid");

            int played = 0;
            while (played < handSize) {
                String turn = awaitPlayableTurn(adb, ui);
                String card = ui.firstLegalCardDescription(turn);
                if (played == 0) {
                    record(ui, adb, trace, evidence, profile,
                            "Play hand " + handNumber + " with the displayed legal card",
                            List.of(check("Human turn is marked", "PLAY NOW", ui.hasText(turn, "PLAY NOW")),
                                    check("A legal hand card is offered", "one or more legal cards",
                                            !ui.legalCardDescriptions(turn).isEmpty())));
                }
                int remaining = handSize - played - 1;
                ui.tapDescription(card);
                played++;
                if (remaining > 0) {
                    ui.awaitDescriptionContaining("Your hand, " + remaining + " cards.", WAIT);
                }
            }
            String settled = ui.awaitText("Next hand", WAIT);
            record(ui, adb, trace, evidence, profile,
                    "Finish real deal " + handNumber + " with " + handSize + " tricks",
                    List.of(check("Completed deal advances normally", "Next hand",
                            ui.hasText(settled, "Next hand")),
                            check("Score sheet lists all four players", "You, Ava, Miles, Grace",
                                    ui.hasText(settled, "You") && ui.hasText(settled, "Ava")
                                            && ui.hasText(settled, "Miles") && ui.hasText(settled, "Grace"))));
            if (handNumber < hands) {
                ui.tapText("Next hand");
            }
        }

        ui.restartToActivity(APP, ".MainActivity", "--es", "debug_language", "EN");
        String resumed = ui.awaitText("Resume game", WAIT);
        ui.tapText("Resume game");
        resumed = ui.awaitContains("Score history", WAIT);
        ui.revealText("Score history");
        ui.tapText("Score history");
        String history = ui.awaitText("Total", WAIT);
        record(ui, adb, trace, evidence, profile, "Review the score after restarting the app",
                List.of(check("Score history exposes bid, hand and total",
                                "Bid; Hand; Total",
                                ui.hasText(history, "Bid") && ui.hasText(history, "Hand")
                                        && ui.hasText(history, "Total")),
                        check("Score history remains selectable", "player selector",
                                ui.hasClass(history, "android.widget.Spinner"))));
        ui.tapText("CLOSE");
        ui.awaitText("Next hand", WAIT);
    }

    private static void recordBidding(AndroidUiDriver ui, AdbClient adb, AndroidUiFlowTrace trace,
            List<Path> evidence, ResponsiveDisplayProfile profile, int handNumber, int handSize,
            String bidding) throws Exception {
        var hand = ui.boundsForDescriptionContaining(bidding, "Your hand,");
        var faces = ui.handCardFaceBounds(bidding).stream()
                .sorted(Comparator.comparingInt(AndroidUiDriver.Bounds::left)).toList();
        var seat = ui.boundsForDescriptionContaining(bidding, "You. Bid ");
        var board = ui.boundsForDescription(bidding, "Game table, 4 players.");
        List<Integer> bids = ui.numericButtonValues(bidding);
        List<Integer> enabledBids = AndroidUiDriver.enabledNumericButtonValues(bidding);
        List<AndroidUiDriver.Bounds> targets = new ArrayList<>();
        for (int bid : bids) targets.add(ui.buttonBounds(bidding, Integer.toString(bid)));
        targets.add(ui.buttonBounds(bidding, "Place bid"));
        List<AndroidUiDriver.Bounds> seats = seatBounds(ui, bidding);
        boolean distinctTargets = noOverlap(targets) && targets.stream().allMatch(target -> seats.stream()
                .noneMatch(other -> overlaps(target, other)));
        int controlTop = targets.stream().mapToInt(AndroidUiDriver.Bounds::top).min().orElse(Integer.MAX_VALUE);
        int controlBottom = targets.stream().mapToInt(AndroidUiDriver.Bounds::bottom).max().orElse(Integer.MIN_VALUE);
        int upperSeatBottom = seats.stream().filter(other -> other.bottom() <= seat.top())
                .mapToInt(AndroidUiDriver.Bounds::bottom).max().orElse(Integer.MAX_VALUE);
        int upperClearance = controlTop - upperSeatBottom;
        int humanClearance = seat.top() - controlBottom;
        boolean bidZoneFavorsHuman = upperSeatBottom < controlTop && controlBottom < seat.top()
                && humanClearance < upperClearance;
        boolean insideBoard = targets.stream().allMatch(target -> contains(board, target))
                && faces.stream().allMatch(face -> contains(board, face));
        int centerError = faces.isEmpty() ? Integer.MAX_VALUE
                : Math.abs(faces.getFirst().left() + faces.getLast().right() - hand.left() - hand.right());

        List<AtfAssertion> checks = new ArrayList<>(List.of(
                check("Current deal size", handSize + " cards", faces.size() + " cards",
                        faces.size() == handSize),
                check("Hand remains centered in the lower table", "within one pixel",
                        centerError / 2.0, centerError <= 2),
                check("Cards share one baseline below the player",
                        "one row, below You", faces.size() + " faces",
                        faces.stream().allMatch(face -> face.top() > seat.bottom()
                                && face.bottom() == faces.getLast().bottom())),
                check("Selectable bids stay within the deal range", "enabled value 0.." + handSize,
                        enabledBids.toString(),
                        !enabledBids.isEmpty()
                                && bids.stream().allMatch(value -> value >= 0 && value <= handSize)),
                check("Bids and hand stay inside the game table", "inside", insideBoard,
                        insideBoard),
                check("Bid controls do not cover the four player seats", "no overlap",
                        distinctTargets, distinctTargets),
                check("Bid controls sit closer to the human seat than the opponents",
                        "upper gap > human gap", "upper=" + upperClearance + " px; human="
                                + humanClearance + " px", bidZoneFavorsHuman)));
        List<Path> capture = ui.capture(profile.id() + "-deal-" + handNumber + "-bidding");
        evidence.addAll(capture);
        checkCaptureSize(adb, capture, profile, checks);
        publishCheckpoint(ui, trace, profile, "Inspect bidding and centered hand for deal " + handNumber,
                capture, checks);
    }

    private static void inspectThirteenCardFan(AdbClient adb, AndroidUiDriver ui, AndroidUiFlowTrace trace,
            List<Path> evidence, ResponsiveDisplayProfile profile) throws Exception {
        ui.restartToActivity(APP, ".MainActivity", "--ez", "debug_hand_fan_fixture", "true",
                "--ez", "debug_hold_ai_turns", "true", "--es", "debug_language", "EN");
        String xml = scrollUntilVisible(adb, ui, profile, "Your hand, 13 cards.", true,
                HAND_CARD_FACE_HEIGHT_DP);
        List<Path> capture = ui.capture(profile.id() + "-thirteen-card-fan");
        evidence.addAll(capture);
        var faces = ui.handCardFaceBounds(xml).stream()
                .sorted(Comparator.comparingInt(AndroidUiDriver.Bounds::left)).toList();
        var labels = ui.bottomRowCardLabels(xml);
        var viewport = ui.boundsForDescriptionContaining(xml, "Your hand,");
        int centerError = faces.size() == 13
                ? Math.abs(faces.getFirst().left() + faces.getLast().right()
                        - viewport.left() - viewport.right()) : Integer.MAX_VALUE;
        int expectedFaceHeight = (int) Math.round(HAND_CARD_FACE_HEIGHT_DP * profile.density() / 160.0);
        int shortestFaceHeight = faces.stream().mapToInt(WhistResponsivePlaySanityE2eTest::height)
                .min().orElse(0);
        long labelBaselines = labels.stream().map(AndroidUiDriver.CardLabel::top).distinct().count();
        List<AtfAssertion> checks = new ArrayList<>(List.of(
                check("All thirteen cards fit on one row", "13", faces.size(),
                        faces.size() == 13 && labels.size() == 13),
                check("All thirteen card faces are fully visible", expectedFaceHeight + " px minimum",
                        shortestFaceHeight + " px minimum", faces.size() == 13
                                && shortestFaceHeight >= expectedFaceHeight),
                check("Dense hand is centered", "within one pixel", centerError / 2.0,
                        centerError <= 2),
                check("Indices share one baseline", "one row", labelBaselines,
                        labels.size() == 13 && labelBaselines == 1)));
        Path screenshot = capture.stream().filter(path -> path.toString().endsWith(".png")).findFirst().orElseThrow();
        BufferedImage image = ImageIO.read(screenshot.toFile());
        int[] display = AndroidUiDriver.displayDimensions(adb.run("shell", "wm", "size").output());
        List<AndroidUiDriver.Bounds> indices = ui.playingCardLabelBounds(xml).stream()
                .sorted(Comparator.comparingInt(AndroidUiDriver.Bounds::left)).toList();
        for (int i = 0; i < Math.min(indices.size(), faces.size()); i++) {
            HandIndexAudit.Result pixelAudit = HandIndexAudit.inspect(image, display[0], display[1],
                    box(indices.get(i)), box(faces.get(i)));
            checks.add(check("Rank and suit remain exposed " + (i + 1),
                    "readable", pixelAudit.readable(), pixelAudit.readable()));
        }
        if (indices.size() != 13 || faces.size() != 13) {
            checks.add(check("Thirteen card rank and suit pixels are auditable", "13 visible indices",
                    "only " + Math.min(indices.size(), faces.size()) + " complete cards",
                    false));
        }
        checkCaptureSize(adb, capture, profile, checks);
        publishCheckpoint(ui, trace, profile, "Inspect the full thirteen-card fan", capture, checks);
    }

    private static void inspectPlayedCard(AdbClient adb, AndroidUiDriver ui, AndroidUiFlowTrace trace,
            List<Path> evidence, ResponsiveDisplayProfile profile) throws Exception {
        ui.restartToActivity(APP, ".MainActivity", "--ez", "debug_played_card_fixture", "true",
                "--ez", "debug_hold_ai_turns", "true", "--es", "debug_language", "EN");
        String xml = ui.awaitDescriptionContaining("You played ", WAIT);
        List<String> playedCards = ui.descriptions(xml).stream()
                .filter(description -> description.startsWith("You played ")).toList();
        if (playedCards.size() != 1) {
            throw new IllegalStateException("Expected one deterministic card in the current trick: " + playedCards);
        }
        xml = revealCompletePlayedCard(adb, ui, playedCards.getFirst(), profile, xml);
        AndroidUiDriver.Bounds cardSlot = ui.boundsForDescription(xml, playedCards.getFirst());
        AndroidUiDriver.Bounds cardFace = ui.boundsForText(xml, "A♥");
        AndroidUiDriver.Bounds board = ui.boundsForDescription(xml, "Playing surface");
        AndroidUiDriver.Bounds currentSeat = ui.boundsForDescriptionContaining(xml, "Current turn.");
        AndroidUiDriver.Bounds trump = ui.boundsForDescriptionContaining(xml, "Trump");
        List<String> initialSeatDescriptions = seatDescriptions(ui, xml);
        double density = profile.density() / 160.0;
        int playedCardFaceHeightDp = profile.playedCardFaceHeightDp();
        int playedCardFaceHeight = (int) Math.round(playedCardFaceHeightDp * density);
        boolean faceHeightReserved = height(cardFace) >= playedCardFaceHeight;
        boolean cardInsideTable = contains(board, cardSlot);
        boolean centered = Math.abs(centerX(cardSlot) - centerX(board)) <= width(board) * 0.12;
        List<Path> styleCapture = ui.capture(profile.id() + "-responsive-played-card-style");
        evidence.addAll(styleCapture);
        Path screenshot = styleCapture.stream().filter(path -> path.toString().endsWith(".png"))
                .findFirst().orElseThrow();
        BufferedImage image = ImageIO.read(screenshot.toFile());
        int seatFill = sampleFrameFill(image, currentSeat, profile);
        int trumpFill = sampleFrameFill(image, trump, profile);
        boolean trumpMatchesCurrentSeat = seatFill == trumpFill;
        int trumpHeadingInk = darkTextPixels(image, ui.boundsForText(xml, "TRUMP"), profile);
        int trumpSuitInk = darkTextPixels(image, ui.boundsForText(xml, "♠"), profile);
        int trumpStatusInk = darkTextPixels(image, ui.boundsForText(xml, "NO TURNED CARD"), profile);
        List<AtfAssertion> styleChecks = new ArrayList<>(List.of(
                check("Played-card slot reserves its responsive face height",
                        playedCardFaceHeightDp + " dp / " + playedCardFaceHeight + " px minimum at "
                                + profile.density() + " dpi",
                        height(cardFace) + " px", faceHeightReserved),
                check("Played card stays centered on the table", "within 12% of table width",
                        Math.abs(centerX(cardSlot) - centerX(board)) + " px",
                        centered),
                check("Played card remains inside the table surface", "contained", cardSlot,
                        cardInsideTable),
                check("Trump frame matches the current player's frame fill", "same rendered color",
                        String.format("seat #%06X, trump #%06X", seatFill, trumpFill),
                        trumpMatchesCurrentSeat),
                check("Active trump heading has readable contrast", "at least 3 dark text pixels",
                        trumpHeadingInk, trumpHeadingInk >= 3),
                check("Active trump suit has readable contrast", "at least 3 dark suit pixels",
                        trumpSuitInk, trumpSuitInk >= 3),
                check("Active trump status has readable contrast", "at least 3 dark text pixels",
                        trumpStatusInk, trumpStatusInk >= 3)));
        checkCaptureSize(adb, styleCapture, profile, styleChecks);
        publishCheckpoint(ui, trace, profile,
                "Inspect responsive card height and matching active trump frame", styleCapture, styleChecks);

        xml = revealSeatCross(adb, ui, playedCards.getFirst(), profile, xml);
        cardSlot = ui.boundsForDescription(xml, playedCards.getFirst());
        cardFace = ui.boundsForText(xml, "A♥");
        board = ui.boundsForDescription(xml, "Playing surface");
        List<String> visibleSeatDescriptions = seatDescriptions(ui, xml);
        List<AndroidUiDriver.Bounds> seats = seatBounds(ui, xml);
        boolean fullFaceStillVisible = height(cardFace) >= playedCardFaceHeight;
        int fullCardRowHeight = playedCardFaceHeightDp + PLAYED_CARD_LABEL_AND_PADDING_DP;
        boolean completeCardRowVisible = height(cardSlot) >= (int) Math.round(fullCardRowHeight * density);
        boolean humanSeatVisible = visibleSeatDescriptions.stream()
                .anyMatch(description -> description.startsWith("You. Bid "));
        java.util.Set<String> observedPlayers = new java.util.LinkedHashSet<>(initialSeatDescriptions);
        observedPlayers.addAll(visibleSeatDescriptions);
        boolean allSeatsObserved = observedPlayers.size() == 4 && humanSeatVisible;
        AndroidUiDriver.Bounds auditedCardSlot = cardSlot;
        int overlapCount = (int) seats.stream().filter(seat -> overlaps(auditedCardSlot, seat)).count();
        boolean clearOfSeats = humanSeatVisible && !seats.isEmpty() && overlapCount == 0;
        AndroidUiDriver.Bounds humanSeat = ui.boundsForDescription(xml, visibleSeatDescriptions.stream()
                .filter(description -> description.startsWith("You. Bid ")).findFirst().orElseThrow());
        int horizontalOffset = Math.abs(centerX(cardSlot) - centerX(humanSeat));
        boolean alignedWithHuman = horizontalOffset <= width(humanSeat) * 0.15;
        List<Path> seatCapture = ui.capture(profile.id() + "-seat-oriented-card-clearance");
        evidence.addAll(seatCapture);
        List<AtfAssertion> seatChecks = new ArrayList<>(List.of(
                check("Complete responsive-height card remains visible beside the player cross",
                        playedCardFaceHeight + " px minimum", height(cardFace) + " px",
                        fullFaceStillVisible),
                check("Card face and player label are fully inside the table viewport",
                        (int) Math.round(fullCardRowHeight * density) + " px full row", height(cardSlot) + " px",
                        completeCardRowVisible),
                check("All four player frames are accounted for across the scrollable table",
                        "4 player frames",
                        observedPlayers.size() + "/4 unique seats including You", allSeatsObserved),
                check("The bottom card remains aligned with the human seat", "same horizontal center",
                        "center offset=" + horizontalOffset + " px; human seat visible=" + humanSeatVisible,
                        humanSeatVisible && alignedWithHuman),
                check("Played card and player labels do not cover a seat frame",
                        "no rectangle intersections", "visible seats=" + seats.size()
                                + "; overlapping frames=" + overlapCount,
                        clearOfSeats),
                check("Played card remains within the visible table surface", "contained", cardSlot,
                        contains(board, auditedCardSlot))));
        checkCaptureSize(adb, seatCapture, profile, seatChecks);
        publishCheckpoint(ui, trace, profile, "Audit card clearance around all four player frames",
                seatCapture, seatChecks);
    }

    private static String revealSeatCross(AdbClient adb, AndroidUiDriver ui, String cardDescription,
            ResponsiveDisplayProfile profile, String latest) throws Exception {
        int[] dimensions = AndroidUiDriver.displayDimensions(adb.run("shell", "wm", "size").output());
        for (int attempt = 0; attempt < 6; attempt++) {
            AndroidUiDriver.Bounds cardSlot = ui.boundsForDescription(latest, cardDescription);
            boolean fullCardRowVisible = height(cardSlot)
                    >= (int) Math.round(122 * profile.density() / 160.0);
            if (fullCardRowVisible && humanSeatFullyVisible(ui, latest)) return latest;
            int distance = Math.max(96, Math.round(dimensions[1] * 0.12f));
            int startY = dimensions[1] * 90 / 100;
            int endY = Math.max(1, startY - distance);
            long started = System.nanoTime();
            adb.run("shell", "input", "swipe", Integer.toString(dimensions[0] / 2),
                    Integer.toString(startY), Integer.toString(dimensions[0] / 2),
                    Integer.toString(endY), "300").requireSuccess();
            ui.recordAction("Swipe", "Scrollable game table", "Expose the full player cross",
                    "Scrolled the table viewport by " + distance + " px for profile " + profile.id(),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(), AtfReportStatus.PASS);
            Thread.sleep(250);
            latest = ui.dump();
            if (!ui.hasDescription(latest, cardDescription)) {
                throw new IllegalStateException("Played card left the visible table while exposing seats");
            }
        }
        return latest;
    }

    private static String awaitPlayableTurn(AdbClient adb, AndroidUiDriver ui) throws Exception {
        int[] dimensions = AndroidUiDriver.displayDimensions(adb.run("shell", "wm", "size").output());
        boolean playControlObserved = false;
        for (int attempt = 0; attempt < 8; attempt++) {
            String latest = ui.dump();
            playControlObserved |= ui.hasText(latest, "PLAY NOW");
            if (playControlObserved && !ui.legalCardDescriptions(latest).isEmpty()) return latest;
            int distance = Math.max(96, Math.round(dimensions[1] * 0.12f));
            int startY = dimensions[1] * 88 / 100;
            int endY = Math.max(1, startY - distance);
            long started = System.nanoTime();
            adb.run("shell", "input", "swipe", Integer.toString(dimensions[0] / 2),
                    Integer.toString(startY), Integer.toString(dimensions[0] / 2),
                    Integer.toString(endY), "300").requireSuccess();
            ui.recordAction("Swipe", "Scrollable game table", "Reveal the active player and legal hand",
                    "Scrolled the compact viewport by " + distance + " px",
                    Duration.ofNanos(System.nanoTime() - started).toMillis(), AtfReportStatus.PASS);
            Thread.sleep(250);
        }
        throw new IllegalStateException("Active play control remained outside the viewport after scrolling");
    }

    private static String scrollUntilVisible(AdbClient adb, AndroidUiDriver ui,
            ResponsiveDisplayProfile profile, String expected, boolean description,
            int minimumVisibleHeightDp) throws Exception {
        int[] dimensions = AndroidUiDriver.displayDimensions(adb.run("shell", "wm", "size").output());
        int distance = Math.max(96, Math.round(dimensions[1] * 0.12f));
        for (int attempt = 0; attempt < 8; attempt++) {
            String latest = ui.dump();
            boolean visible = description
                    ? ui.hasDescriptionContaining(latest, expected) : ui.hasText(latest, expected);
            if (visible) {
                int requiredHeight = (int) Math.round(minimumVisibleHeightDp
                        * profile.density() / 160.0);
                int visibleHeight = description
                        ? height(ui.boundsForDescriptionContaining(latest, expected)) : requiredHeight;
                if (visibleHeight >= requiredHeight) return latest;
            }
            if (attempt == 7) break;
            int startY = dimensions[1] * 90 / 100;
            int endY = Math.max(1, startY - distance);
            long started = System.nanoTime();
            adb.run("shell", "input", "swipe", Integer.toString(dimensions[0] / 2),
                    Integer.toString(startY), Integer.toString(dimensions[0] / 2),
                    Integer.toString(endY), "300").requireSuccess();
            ui.recordAction("Swipe", "Scrollable game table", "Reveal " + expected,
                    "Scrolled the " + profile.id() + " viewport by " + distance + " px",
                    Duration.ofNanos(System.nanoTime() - started).toMillis(), AtfReportStatus.PASS);
            Thread.sleep(250);
        }
        throw new IllegalStateException("UI did not expose the requested game control after scrolling: "
                + expected);
    }

    private static boolean humanSeatFullyVisible(AndroidUiDriver ui, String xml) throws Exception {
        List<String> descriptions = seatDescriptions(ui, xml);
        String human = descriptions.stream().filter(description -> description.startsWith("You. Bid "))
                .findFirst().orElse(null);
        if (human == null) return false;
        int tallestOtherSeat = 0;
        for (String description : descriptions) {
            if (!description.startsWith("You. Bid ")) {
                tallestOtherSeat = Math.max(tallestOtherSeat,
                        height(ui.boundsForDescription(xml, description)));
            }
        }
        return tallestOtherSeat > 0
                && height(ui.boundsForDescription(xml, human)) >= tallestOtherSeat;
    }

    private static String revealCompletePlayedCard(AdbClient adb, AndroidUiDriver ui, String description,
            ResponsiveDisplayProfile profile, String latest) throws Exception {
        int expectedFaceHeight = (int) Math.round(
                profile.playedCardFaceHeightDp() * profile.density() / 160.0);
        int[] dimensions = AndroidUiDriver.displayDimensions(adb.run("shell", "wm", "size").output());
        for (int attempt = 0; attempt < 3; attempt++) {
            AndroidUiDriver.Bounds face = ui.boundsForText(latest, "A♥");
            if (height(face) >= expectedFaceHeight) return latest;
            int distance = Math.max(64, Math.round(dimensions[1] * 0.05f));
            int startY = dimensions[1] * 88 / 100;
            int endY = Math.max(1, startY - distance);
            long started = System.nanoTime();
            adb.run("shell", "input", "swipe", Integer.toString(dimensions[0] / 2),
                    Integer.toString(startY), Integer.toString(dimensions[0] / 2),
                    Integer.toString(endY), "300").requireSuccess();
            ui.recordAction("Swipe", "Scrollable game table", "Reveal the complete played-card face",
                    "Scrolled the table viewport by " + distance + " px for profile " + profile.id(),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(), AtfReportStatus.PASS);
            Thread.sleep(250);
            latest = ui.dump();
            if (!ui.hasDescription(latest, description)) {
                throw new IllegalStateException("Played card left the visible table after scrolling");
            }
        }
        throw new IllegalStateException("Played-card face remained clipped after three small table scrolls: "
                + ui.boundsForText(latest, "A♥"));
    }

    private static void inspectHardAiBid(AdbClient adb, AndroidUiDriver ui, AndroidUiFlowTrace trace,
            List<Path> evidence, ResponsiveDisplayProfile profile) throws Exception {
        ui.restartToActivity(APP, ".MainActivity", "--ez", "debug_hard_ai_bid_fixture", "true",
                "--ez", "debug_hold_ai_turns", "true", "--es", "debug_language", "EN");
        String bidding = ui.awaitText("Place bid", WAIT);
        boolean startsWithTwoCards = ui.hasDescription(bidding, "Your hand, 2 cards.");
        if (!startsWithTwoCards) {
            throw new IllegalStateException("Hard-AI compatibility fixture must use two-card deals: "
                    + ui.descriptions(bidding));
        }
        record(ui, adb, trace, evidence, profile, "Open the two-card Hard-AI bidding fixture",
                List.of(check("Hard-AI fixture starts with two cards", "Your hand, 2 cards.",
                        startsWithTwoCards)));

        ui.tapText("Place bid");
        String play = awaitPlayableTurn(adb, ui);
        int visibleSeats = seatCount(ui, play);
        record(ui, adb, trace, evidence, profile, "Complete Hard-AI bids on the Android runtime",
                List.of(check("All four seats finish bidding without a runtime crash", "4 seats",
                                visibleSeats, visibleSeats == 4),
                        check("Human receives a playable turn", "PLAY NOW and two cards",
                                ui.hasText(play, "PLAY NOW") && ui.hasDescription(play, "Your hand, 2 cards."),
                                ui.hasText(play, "PLAY NOW") && ui.hasDescription(play, "Your hand, 2 cards."))));
    }

    private static void inspectNineBidTable(AdbClient adb, AndroidUiDriver ui, AndroidUiFlowTrace trace,
            List<Path> evidence, ResponsiveDisplayProfile profile) throws Exception {
        ui.restartToActivity(APP, ".MainActivity", "--ez", "debug_nine_bids_fixture", "true",
                "--ez", "debug_hold_ai_turns", "true", "--es", "debug_language", "EN");
        String xml = scrollUntilVisible(adb, ui, profile, "Place bid", false, 0);
        List<Path> capture = ui.capture(profile.id() + "-nine-bid-controls");
        evidence.addAll(capture);
        List<Integer> bids = ui.numericButtonValues(xml);
        List<AndroidUiDriver.Bounds> seats = seatBounds(ui, xml);
        List<AndroidUiDriver.Bounds> controls = new ArrayList<>();
        for (int bid : bids) controls.add(ui.buttonBounds(xml, Integer.toString(bid)));
        AndroidUiDriver.Bounds submit = ui.buttonBounds(xml, "Place bid");
        controls.add(submit);
        int overlaps = 0;
        List<AndroidUiDriver.Bounds> occupied = new ArrayList<>(seats);
        occupied.addAll(controls);
        for (int first = 0; first < occupied.size(); first++) {
            for (int second = first + 1; second < occupied.size(); second++) {
                if (overlaps(occupied.get(first), occupied.get(second))) overlaps++;
            }
        }
        var board = ui.boundsForDescription(xml, "Playing surface");
        AndroidUiDriver.Bounds human = ui.boundsForDescriptionContaining(xml, "You. Bid ");
        int controlsTop = controls.stream().mapToInt(AndroidUiDriver.Bounds::top).min().orElse(Integer.MAX_VALUE);
        int controlsBottom = controls.stream().mapToInt(AndroidUiDriver.Bounds::bottom).max().orElse(Integer.MIN_VALUE);
        int nearestOpponentBottom = seats.stream().filter(seat -> seat.bottom() <= human.top())
                .mapToInt(AndroidUiDriver.Bounds::bottom).max().orElse(Integer.MAX_VALUE);
        int opponentGap = controlsTop - nearestOpponentBottom;
        int humanGap = human.top() - controlsBottom;
        boolean favorsHuman = nearestOpponentBottom < controlsTop && controlsBottom < human.top()
                && humanGap < opponentGap;
        boolean inside = occupied.stream().allMatch(region -> contains(board, region));
        double scale = profile.density() / 160.0;
        boolean touch = controls.stream().allMatch(region -> region.right() - region.left()
                >= Math.floor(48 * scale) && region.bottom() - region.top() >= Math.floor(48 * scale));
        List<AtfAssertion> checks = new ArrayList<>(List.of(
                check("Nine legal bids fit", "0..8", bids.toString(),
                        bids.equals(java.util.stream.IntStream.rangeClosed(0, 8).boxed().toList())),
                check("Four players remain individually visible", "You, Ava, Miles, Grace", seats.size(),
                        seats.size() == 4),
                check("No player or bid controls overlap", "0", overlaps, overlaps == 0),
                check("Nine-bid zone is closer to the human seat", "opponent gap > human gap",
                        "opponent=" + opponentGap + " px; human=" + humanGap + " px", favorsHuman),
                check("Bid controls remain inside the table", "inside", inside, inside),
                check("Bid hit targets remain at least 48 dp", "accessible", touch, touch)));
        checkCaptureSize(adb, capture, profile, checks);
        publishCheckpoint(ui, trace, profile, "Inspect all nine bid controls", capture, checks);
    }

    private static void applyProfile(AdbClient adb, AndroidUiDriver ui,
            ResponsiveDisplayProfile profile) throws Exception {
        runDisplayAction(adb, ui, "Set emulator resolution", "Display resolution " + profile.id(),
                "shell", "wm", "size", profile.width() + "x" + profile.height());
        runDisplayAction(adb, ui, "Set emulator density", "Display density " + profile.id(),
                "shell", "wm", "density", Integer.toString(profile.density()));
    }

    private static void restoreDisplay(AdbClient adb, AndroidUiDriver ui,
            String originalSize, String originalDensity) throws Exception {
        restoreSetting(adb, ui, "size", originalSize);
        restoreSetting(adb, ui, "density", originalDensity);
        String afterSize = adb.run("shell", "wm", "size").output();
        String afterDensity = adb.run("shell", "wm", "density").output();
        assertEquals(displayState(originalSize, originalDensity), displayState(afterSize, afterDensity),
                "Emulator display overrides must be restored");
    }

    private static void restoreSetting(AdbClient adb, AndroidUiDriver ui, String property,
            String output) throws Exception {
        Matcher override = Pattern.compile("(?m)^Override " + property + ":\\s*(\\S+)").matcher(output);
        String[] command = override.find()
                ? new String[]{"shell", "wm", property, override.group(1)}
                : new String[]{"shell", "wm", property, "reset"};
        runDisplayAction(adb, ui, "Restore emulator " + property, "Original display " + property,
                command);
    }

    private static String displayState(String size, String density) {
        return state(size, "size") + ";" + state(density, "density");
    }

    private static String state(String output, String property) {
        Matcher override = Pattern.compile("(?m)^Override " + property + ":\\s*(\\S+)").matcher(output);
        if (override.find()) return "override=" + override.group(1);
        Matcher physical = Pattern.compile("(?m)^Physical " + property + ":\\s*(\\S+)").matcher(output);
        if (physical.find()) return "physical=" + physical.group(1);
        return "unavailable";
    }

    private static void runDisplayAction(AdbClient adb, AndroidUiDriver ui, String operation,
            String target, String... command) throws Exception {
        long started = System.nanoTime();
        try {
            var result = adb.run(command);
            result.requireSuccess();
            ui.recordAction(operation, target, String.join(" ", command),
                    result.output().isBlank() ? "ADB accepted display setting" : result.output().trim(),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(), AtfReportStatus.PASS);
        } catch (Exception failure) {
            ui.recordAction(operation, target, String.join(" ", command), failure.toString(),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(), AtfReportStatus.FAIL);
            throw failure;
        }
    }

    private static int handSize(AndroidUiDriver ui, String xml) throws Exception {
        Matcher match = HAND_SIZE.matcher(ui.descriptions(xml).stream()
                .filter(value -> value.startsWith("Your hand, ")).findFirst()
                .orElseThrow(() -> new IllegalStateException("Own hand size is absent")));
        if (!match.matches()) throw new IllegalStateException("Unexpected hand description");
        return Integer.parseInt(match.group(1));
    }

    private static int dealNumber(AndroidUiDriver ui, String xml) throws Exception {
        Matcher match = DEAL_NUMBER.matcher(ui.texts(xml).stream()
                .filter(value -> value.startsWith("Hand ")).findFirst()
                .orElseThrow(() -> new IllegalStateException("Deal progress is absent")));
        if (!match.matches()) throw new IllegalStateException("Unexpected deal progress");
        return Integer.parseInt(match.group(1));
    }

    private static int romanianFourPlayerHandSize(int dealNumber) {
        if (dealNumber < 1 || dealNumber > 24) {
            throw new IllegalArgumentException("Romanian deal number must be between 1 and 24");
        }
        if (dealNumber <= 4 || dealNumber >= 21) return 1;
        if (dealNumber <= 10) return dealNumber - 3;
        if (dealNumber <= 14) return 8;
        return 22 - dealNumber;
    }


    private static int seatCount(AndroidUiDriver ui, String xml) throws Exception {
        return seatBounds(ui, xml).size();
    }

    private static List<AndroidUiDriver.Bounds> seatBounds(AndroidUiDriver ui, String xml)
            throws Exception {
        List<AndroidUiDriver.Bounds> seats = new ArrayList<>();
        for (String description : seatDescriptions(ui, xml)) {
            seats.add(ui.boundsForDescription(xml, description));
        }
        return seats;
    }

    private static List<String> seatDescriptions(AndroidUiDriver ui, String xml) throws Exception {
        return ui.descriptions(xml).stream()
                .filter(description -> description.contains(". Bid ")
                        && description.contains(". Score "))
                .toList();
    }

    private static void record(AndroidUiDriver ui, AdbClient adb, AndroidUiFlowTrace trace,
            List<Path> evidence, ResponsiveDisplayProfile profile, String title,
            List<AtfAssertion> checks) throws Exception {
        List<Path> capture = ui.capture(profile.id() + "-" + title.replace(' ', '-'));
        evidence.addAll(capture);
        List<AtfAssertion> recordedChecks = new ArrayList<>(checks);
        checkCaptureSize(adb, capture, profile, recordedChecks);
        publishCheckpoint(ui, trace, profile, title, capture, recordedChecks);
    }

    private static void checkCaptureSize(AdbClient adb, List<Path> capture,
            ResponsiveDisplayProfile profile, List<AtfAssertion> checks) throws Exception {
        String sizeOutput = adb.run("shell", "wm", "size").output();
        int[] logical = AndroidUiDriver.displayDimensions(sizeOutput);
        int[] physical = physicalDisplayDimensions(sizeOutput);
        BufferedImage image = ImageIO.read(capture.stream()
                .filter(path -> path.toString().endsWith(".png")).findFirst().orElseThrow().toFile());

        boolean profileApplied = logical[0] == profile.width() && logical[1] == profile.height();
        checks.add(check("Logical viewport matches configured profile",
                profile.width() + "x" + profile.height(),
                logical[0] + "x" + logical[1], profileApplied));

        boolean fullSurface = image != null && image.getWidth() == physical[0]
                && image.getHeight() == physical[1];
        checks.add(check("PNG captures the full physical emulator surface",
                physical[0] + "x" + physical[1],
                image == null ? "invalid PNG" : image.getWidth() + "x" + image.getHeight(),
                fullSurface));
    }

    private static int[] physicalDisplayDimensions(String sizeOutput) throws IOException {
        Matcher physical = Pattern.compile("(?m)^Physical size:\\s*(\\d+)x(\\d+)").matcher(sizeOutput);
        if (!physical.find()) throw new IOException("Physical Android display size is unavailable: " + sizeOutput);
        return new int[]{Integer.parseInt(physical.group(1)), Integer.parseInt(physical.group(2))};
    }
    private static void publishCheckpoint(AndroidUiDriver ui, AndroidUiFlowTrace trace,
            ResponsiveDisplayProfile profile, String title, List<Path> capture,
            List<AtfAssertion> checks) throws Exception {
        boolean passed = checks.stream().allMatch(AtfAssertion::passed);
        trace.checkpoint("TEST BODY", trace.phaseSnapshots().stream().allMatch(value -> value.checkpoints().isEmpty()) ? "GIVEN" : "AND",
                profile.id() + " — " + title, "Rendered Android screen",
                capture.stream().filter(path -> path.toString().endsWith(".png")).findFirst().orElseThrow(),
                List.of(new AtfUiActionRecord(trace.nextActionSequence(), "Inspect rendered screen",
                        title, "All named controls and game regions fit their display profile",
                        passed ? "All comparisons passed" : "A layout comparison failed", 0,
                        passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL,
                        AtfUiEvidenceSource.OBSERVE)),
                checks, passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL,
                capture.stream().filter(path -> path.toString().endsWith(".xml")).toList());
        for (AtfAssertion assertion : checks) {
            assertTrue(assertion.passed(),
                    profile.id() + " / " + title + " / " + assertion.name() + ": " + assertion.actual());
        }
    }

    private static boolean noOverlap(List<AndroidUiDriver.Bounds> regions) {
        for (int first = 0; first < regions.size(); first++) {
            for (int second = first + 1; second < regions.size(); second++) {
                if (overlaps(regions.get(first), regions.get(second))) return false;
            }
        }
        return true;
    }

    private static boolean contains(AndroidUiDriver.Bounds parent, AndroidUiDriver.Bounds child) {
        return child.left() >= parent.left() && child.top() >= parent.top()
                && child.right() <= parent.right() && child.bottom() <= parent.bottom();
    }

    private static int width(AndroidUiDriver.Bounds bounds) { return bounds.right() - bounds.left(); }
    private static int height(AndroidUiDriver.Bounds bounds) { return bounds.bottom() - bounds.top(); }
    private static int centerX(AndroidUiDriver.Bounds bounds) { return (bounds.left() + bounds.right()) / 2; }
    private static int sampleFrameFill(BufferedImage image, AndroidUiDriver.Bounds bounds,
            ResponsiveDisplayProfile profile) {
        double density = profile.density() / 160.0;
        int[] point = AndroidUiDriver.screenshotPoint(image,
                bounds.left() + 8 * density,
                bounds.top() + (bounds.bottom() - bounds.top()) / 2.0,
                profile.width(), profile.height());
        return image.getRGB(point[0], point[1]) & 0x00FFFFFF;
    }

    private static int darkTextPixels(BufferedImage image, AndroidUiDriver.Bounds bounds,
            ResponsiveDisplayProfile profile) {
        int[] topLeft = AndroidUiDriver.screenshotPoint(image, bounds.left(), bounds.top(),
                profile.width(), profile.height());
        int[] bottomRight = AndroidUiDriver.screenshotPoint(image, bounds.right(), bounds.bottom(),
                profile.width(), profile.height());
        int left = Math.max(0, Math.min(topLeft[0], bottomRight[0]));
        int top = Math.max(0, Math.min(topLeft[1], bottomRight[1]));
        int right = Math.min(image.getWidth() - 1, Math.max(topLeft[0], bottomRight[0]));
        int bottom = Math.min(image.getHeight() - 1, Math.max(topLeft[1], bottomRight[1]));
        int dark = 0;
        for (int y = top; y <= bottom; y++) {
            for (int x = left; x <= right; x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >>> 16) & 0xFF;
                int green = (rgb >>> 8) & 0xFF;
                int blue = rgb & 0xFF;
                if (red < 110 && green < 125 && blue < 115) dark++;
            }
        }
        return dark;
    }

    private static boolean overlaps(AndroidUiDriver.Bounds left, AndroidUiDriver.Bounds right) {
        return left.left() < right.right() && left.right() > right.left()
                && left.top() < right.bottom() && left.bottom() > right.top();
    }

    private static HandIndexAudit.Box box(AndroidUiDriver.Bounds bounds) {
        return new HandIndexAudit.Box(bounds.left(), bounds.top(), bounds.right(), bounds.bottom());
    }

    private static AtfAssertion check(String name, Object expected, Object actual) {
        return new AtfAssertion(name, String.valueOf(expected), String.valueOf(actual), expected.equals(actual));
    }

    private static AtfAssertion check(String name, String expected, boolean passed) {
        return new AtfAssertion(name, expected, Boolean.toString(passed), passed);
    }

    private static AtfAssertion check(String name, Object expected, Object actual, boolean passed) {
        return new AtfAssertion(name, String.valueOf(expected), String.valueOf(actual), passed);
    }
}
