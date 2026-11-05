package atf.android.e2e.gameplay.presentation;

import static atf.android.e2e.display.support.GameplayDisplay.DisplayProfile;
import static atf.android.e2e.display.support.GameplayDisplay.OVERRIDE_SIZE;
import static atf.android.e2e.display.support.GameplayDisplay.PHYSICAL_SIZE;
import static atf.android.e2e.display.support.GameplayDisplay.effectiveDensityScale;
import static atf.android.e2e.display.support.GameplayDisplay.effectiveDisplaySize;
import static atf.android.e2e.display.support.GameplayDisplay.matchesRestoredDisplay;
import static atf.android.e2e.display.support.GameplayDisplay.overrideDensityValue;
import static atf.android.e2e.display.support.GameplayDisplay.overrideDisplay;
import static atf.android.e2e.display.support.GameplayDisplay.overrideValue;
import static atf.android.e2e.display.support.GameplayDisplay.restoreDisplay;
import static atf.android.e2e.display.support.GameplayGeometry.overlaps;
import static atf.android.e2e.display.support.GameplayGeometry.singleCardCenterDelta;
import static atf.android.e2e.display.support.GameplayGeometry.singleCardCentered;
import static atf.android.e2e.support.flow.UiFlowCheckpoint.checkpoint;
import static atf.android.e2e.support.flow.WhistLabScenario.PACKAGE;
import static atf.android.e2e.support.flow.WhistLabScenario.artifacts;
import static atf.android.e2e.support.flow.WhistLabScenario.installedApp;
import static atf.android.e2e.support.flow.WhistLabScenario.reportSetting;

import static org.junit.jupiter.api.Assertions.assertTrue;

import atf.android.AdbClient;
import atf.android.e2e.display.support.GameplayDisplay.DisplayProfile;
import atf.android.e2e.display.support.ScreenGeometry;
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

/** Scenariul licitării și geometriei unei mâini lungi pe profiluri portret. */
public final class ThirteenCardDisplayScenario {
    private ThirteenCardDisplayScenario() {}

    public static void run(String ownerName) throws Exception {
        var installation = installedApp();
        AdbClient adb = installation.adb();

        Path artifacts = artifacts("thirteen-card-bidding-resolutions");
        AndroidUiDriver ui = new AndroidUiDriver(adb, artifacts);
        AndroidUiFlowTrace trace =
                new AndroidUiFlowTrace(
                        artifacts,
                        "whist-lab-thirteen-card-bidding-resolutions",
                        "Whist · Descending — thirteen-card bid screen across portrait display sizes",
                        ownerName,
                        "atf.android.e2e",
                        PACKAGE,
                        List.of(
                                "android",
                                "e2e",
                                "ui",
                                "13-cards",
                                "bidding",
                                "resolution",
                                "responsive"));
        ui.recordCapturesAs(trace::capture);
        trace.beginPhase("TEST BODY");
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
            List<DisplayProfile> profiles =
                    List.of(
                            new DisplayProfile("1080x1920 · 440 dpi", 1080, 1920, 440),
                            new DisplayProfile("720x1280 · 320 dpi", 720, 1280, 320),
                            new DisplayProfile(
                                    "ROG 7 resolution · 1080x2448 · simulated 440 dpi",
                                    1080,
                                    2448,
                                    440),
                            new DisplayProfile(
                                    "600 dp tablet · 1200x1920 · 320 dpi", 1200, 1920, 320));
            int index = 0;
            for (DisplayProfile profile : profiles) {
                long started = System.nanoTime();
                overrideDisplay(adb, ui, profile);
                String home =
                        ui.restartToActivity(
                                PACKAGE, ".MainActivity", "--es", "debug_language", "EN");
                assertTrue(ui.hasText(home, "New game"), "Home did not open at " + profile.name());
                ui.tapText("New game");
                ui.awaitText("Players", Duration.ofSeconds(12));
                ui.selectNamedSpinnerOption("player-count", "4");
                ui.selectNamedSpinnerOption("game-type", "Descending");
                String setup = ui.awaitText("Players", Duration.ofSeconds(10));
                String selectedPlayers = ui.spinnerValue(setup, "Players");
                String selectedMode = ui.spinnerValue(setup, "Game type");
                assertTrue(
                        selectedPlayers.equals("4") && selectedMode.equals("Descending"),
                        "The 13-card deal requires 4 players and Descending at "
                                + profile.name()
                                + "; observed "
                                + selectedPlayers
                                + "/"
                                + selectedMode);
                ui.revealText("Start game");
                setup = ui.awaitText("Start game", Duration.ofSeconds(10));
                ui.tapText("Start game");
                ui.awaitText("Place bid", Duration.ofSeconds(20));
                String bidding =
                        ui.awaitDescriptionContaining(
                                "Your hand, 13 cards.", Duration.ofSeconds(10));

                AndroidUiDriver.Bounds handBounds =
                        ui.boundsForDescription(
                                bidding,
                                "Your hand, 13 cards.",
                                "android.widget.HorizontalScrollView");
                double densityScale =
                        effectiveDensityScale(adb.run("shell", "wm", "density").output());
                List<AndroidUiDriver.CardLabel> cardLabels = ui.bottomRowCardLabels(bidding);
                List<AndroidUiDriver.Bounds> cardBounds = ui.handCardFaceBounds(bidding);
                long handRows =
                        cardLabels.stream().map(AndroidUiDriver.CardLabel::top).distinct().count();
                boolean fullHandVisible =
                        cardLabels.size() == 13
                                && cardBounds.size() == 13
                                && cardBounds.stream()
                                        .allMatch(
                                                card ->
                                                        card.left() >= handBounds.left()
                                                                && card.right()
                                                                        <= handBounds.right()
                                                                && card.top() >= handBounds.top()
                                                                && card.bottom()
                                                                        <= handBounds.bottom());
                int minimumCardFaceHeight =
                        cardBounds.stream().mapToInt(ScreenGeometry::height).min().orElse(0);
                boolean fullCardFacesVisible =
                        cardBounds.size() == 13
                                && minimumCardFaceHeight >= Math.round(98 * densityScale * 0.95);
                boolean handIsOneRow =
                        cardLabels.size() == 13
                                && handRows == 1
                                && fullHandVisible
                                && fullCardFacesVisible;

                List<Integer> expectedBids =
                        java.util.stream.IntStream.rangeClosed(0, 13).boxed().toList();
                List<Integer> visibleBids = ui.numericButtonValues(bidding);
                boolean allBidsVisible =
                        visibleBids.size() == expectedBids.size()
                                && new LinkedHashSet<>(visibleBids)
                                        .equals(new LinkedHashSet<>(expectedBids));
                List<AndroidUiDriver.Bounds> bidBounds = new ArrayList<>();
                List<AndroidUiDriver.ElementSize> bidSizes = ui.numericButtonSizes(bidding);
                for (int bid : visibleBids)
                    bidBounds.add(ui.buttonBounds(bidding, Integer.toString(bid)));
                AndroidUiDriver.ElementSize submitSize = ui.buttonSize(bidding, "Place bid");
                AndroidUiDriver.Bounds submitBounds = ui.boundsForText(bidding, "Place bid");
                AndroidUiDriver.Bounds boardBounds =
                        ui.boundsForDescription(bidding, "Playing surface");
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
                boolean bidTargetsRemainAccessible =
                        bidSizes.size() == 14
                                && minimumBidWidth >= Math.round(48 * densityScale * 0.95)
                                && minimumBidHeight >= Math.round(48 * densityScale * 0.95);
                boolean submitRemainsCompact =
                        submitSize.widthPixels() <= Math.round(144 * densityScale)
                                && submitSize.heightPixels()
                                        >= Math.round(48 * densityScale * 0.95);
                AndroidUiDriver.Bounds humanSeat =
                        ui.boundsForDescriptionContaining(bidding, "You. ");
                List<AndroidUiDriver.Bounds> seats =
                        ui.descriptions(bidding).stream()
                                .filter(
                                        value ->
                                                value.contains(". Bid ")
                                                        && value.contains(". Score "))
                                .map(
                                        description -> {
                                            try {
                                                return ui.boundsForDescription(
                                                        bidding, description);
                                            } catch (Exception failure) {
                                                throw new IllegalStateException(failure);
                                            }
                                        })
                                .toList();
                boolean seatsFit =
                        seats.size() == 4
                                && seats.stream()
                                        .allMatch(
                                                seat ->
                                                        seat.left() >= boardBounds.left()
                                                                && seat.right()
                                                                        <= boardBounds.right()
                                                                && seat.top() >= boardBounds.top()
                                                                && seat.bottom()
                                                                        <= boardBounds.bottom());
                boolean allControlsInsideTable =
                        bidBounds.stream()
                                        .allMatch(
                                                control ->
                                                        control.left() >= boardBounds.left()
                                                                && control.right()
                                                                        <= boardBounds.right()
                                                                && control.top()
                                                                        >= boardBounds.top()
                                                                && control.bottom()
                                                                        <= boardBounds.bottom())
                                && submitBounds.left() >= boardBounds.left()
                                && submitBounds.right() <= boardBounds.right()
                                && submitBounds.top() >= boardBounds.top()
                                && submitBounds.bottom() <= boardBounds.bottom();
                boolean controlsAvoidSeats =
                        java.util.stream.Stream.concat(
                                        bidBounds.stream(),
                                        java.util.stream.Stream.of(submitBounds))
                                .noneMatch(
                                        control ->
                                                seats.stream()
                                                        .anyMatch(seat -> overlaps(control, seat)));
                boolean submitAboveHand = submitBounds.bottom() < handBounds.top();
                boolean headerVisible =
                        ui.hasText(bidding, "Whist · Descending")
                                && ui.hasText(bidding, "TRUMP")
                                && ui.hasText(bidding, "NO TURNED CARD");
                List<Path> checkpointEvidence =
                        ui.capture("whist-lab-thirteen-card-bidding-" + profile.fileKey());
                evidence.addAll(checkpointEvidence);
                Path screenshot =
                        checkpointEvidence.stream()
                                .filter(
                                        path ->
                                                path.toString()
                                                        .toLowerCase(java.util.Locale.ROOT)
                                                        .endsWith(".png"))
                                .findFirst()
                                .orElseThrow();
                java.awt.image.BufferedImage captured =
                        javax.imageio.ImageIO.read(screenshot.toFile());
                boolean fullResolutionCapture =
                        captured != null
                                && physicalCaptureWidth > 0
                                && captured.getWidth() == physicalCaptureWidth
                                && captured.getHeight() == physicalCaptureHeight;
                String displayOutput = adb.run("shell", "wm", "size").output().trim();
                boolean logicalDisplayApplied =
                        (profile.width() + "x" + profile.height())
                                .equals(effectiveDisplaySize(displayOutput));
                boolean densityApplied = Math.abs(densityScale * 160 - profile.density()) < 0.01;
                String diagnostic =
                        "profile="
                                + profile.name()
                                + "; display="
                                + displayOutput
                                + "; logicalProfileApplied="
                                + logicalDisplayApplied
                                + "; hand labels="
                                + cardLabels
                                + "; hand bounds="
                                + handBounds
                                + "; minimum card-face height="
                                + minimumCardFaceHeight
                                + " px"
                                + "; bid values="
                                + visibleBids
                                + "; bid bounds="
                                + bidBounds
                                + "; submit="
                                + submitBounds
                                + "; seats="
                                + seats
                                + "; table="
                                + boardBounds
                                + "; physicalScreenshot="
                                + (captured == null
                                        ? "unreadable"
                                        : captured.getWidth() + "x" + captured.getHeight());
                boolean profilePass =
                        handIsOneRow
                                && allBidsVisible
                                && bidTargetsRemainAccessible
                                && submitRemainsCompact
                                && submitAboveHand
                                && seatsFit
                                && allControlsInsideTable
                                && controlsAvoidSeats
                                && headerVisible
                                && logicalDisplayApplied
                                && densityApplied
                                && fullResolutionCapture;
                checkpoint(
                        trace,
                        "TEST BODY",
                        index == 0 ? AtfGherkinKeyword.GIVEN : AtfGherkinKeyword.AND,
                        "Show and audit the full thirteen-card bidding table at " + profile.name(),
                        "Four-player Descending · hand 13 · legal bids 0–13",
                        checkpointEvidence,
                        "Inspect rendered hand, bid grid, fixed player seats, header, and physical display capture",
                        "13 card faces in one visible row; bids 0–13 stay visible; all targets remain usable and within the table",
                        diagnostic,
                        Duration.ofNanos(System.nanoTime() - started).toMillis(),
                        List.of(
                                new AtfAssertion(
                                        "All thirteen card faces remain in one visible row",
                                        "13 cards; 1 row; complete 98 dp faces inside viewport",
                                        cardLabels.size()
                                                + " cards; rows="
                                                + handRows
                                                + "; inside="
                                                + fullHandVisible
                                                + "; min face="
                                                + minimumCardFaceHeight
                                                + " px",
                                        handIsOneRow),
                                new AtfAssertion(
                                        "The complete bid domain remains visible",
                                        "0 through 13",
                                        visibleBids.toString(),
                                        allBidsVisible),
                                new AtfAssertion(
                                        "All bid choices preserve 48 dp hit targets",
                                        "minimum 48 dp",
                                        minimumBidWidth
                                                + "x"
                                                + minimumBidHeight
                                                + " px; density="
                                                + String.format(
                                                        java.util.Locale.ROOT,
                                                        "%.2f",
                                                        densityScale),
                                        bidTargetsRemainAccessible),
                                new AtfAssertion(
                                        "Seat and bid geometry stays clear inside the table",
                                        "four seats; controls inside bounds; zero overlaps",
                                        "seats="
                                                + seats.size()
                                                + "; controlsInside="
                                                + allControlsInsideTable
                                                + "; overlaps="
                                                + (controlsAvoidSeats ? 0 : 1),
                                        seatsFit && allControlsInsideTable && controlsAvoidSeats),
                                new AtfAssertion(
                                        "Place bid stays compact and above the hand",
                                        "<=144 dp wide; >=48 dp high; above hand",
                                        submitSize.widthPixels()
                                                + "x"
                                                + submitSize.heightPixels()
                                                + " px; above="
                                                + submitAboveHand,
                                        submitRemainsCompact && submitAboveHand),
                                new AtfAssertion(
                                        "Variant and trump stay visible",
                                        "Whist · Descending; TRUMP; NO TURNED CARD",
                                        "variant/trump=" + headerVisible,
                                        headerVisible),
                                new AtfAssertion(
                                        "Requested logical display and density are effective",
                                        profile.width()
                                                + "x"
                                                + profile.height()
                                                + "; "
                                                + profile.density()
                                                + " dpi",
                                        effectiveDisplaySize(displayOutput)
                                                + "; "
                                                + Math.round(densityScale * 160)
                                                + " dpi",
                                        logicalDisplayApplied && densityApplied),
                                new AtfAssertion(
                                        "Report image preserves the complete display resolution",
                                        "physical "
                                                + physicalCaptureWidth
                                                + "x"
                                                + physicalCaptureHeight
                                                + "; logical "
                                                + profile.width()
                                                + "x"
                                                + profile.height(),
                                        captured == null
                                                ? "unreadable"
                                                : captured.getWidth() + "x" + captured.getHeight(),
                                        fullResolutionCapture)),
                        profilePass ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
                assertTrue(
                        handIsOneRow,
                        "13-card bidding hand is clipped, hidden, or wrapped at "
                                + profile.name()
                                + "; labels="
                                + cardLabels
                                + "; card bounds="
                                + cardBounds
                                + "; viewport="
                                + handBounds);
                assertTrue(
                        allBidsVisible,
                        "The complete 0–13 bid domain is not visible at "
                                + profile.name()
                                + "; actual="
                                + visibleBids);
                assertTrue(
                        bidTargetsRemainAccessible,
                        "A bid target is below 48 dp at "
                                + profile.name()
                                + "; min="
                                + minimumBidWidth
                                + "x"
                                + minimumBidHeight
                                + " px; density="
                                + densityScale);
                assertTrue(
                        submitRemainsCompact && submitAboveHand,
                        "Place bid must be compact, accessible, and above the 13-card hand at "
                                + profile.name()
                                + "; bounds="
                                + submitBounds
                                + "; size="
                                + submitSize
                                + "; hand="
                                + handBounds);
                assertTrue(
                        seatsFit && allControlsInsideTable && controlsAvoidSeats,
                        "13-card bid controls or seats escape/overlap the table at "
                                + profile.name()
                                + "; seats="
                                + seats
                                + "; bids="
                                + bidBounds
                                + "; submit="
                                + submitBounds
                                + "; table="
                                + boardBounds);
                assertTrue(
                        headerVisible,
                        "Game variant or trump is missing at "
                                + profile.name()
                                + "; labels="
                                + ui.texts(bidding));
                assertTrue(
                        logicalDisplayApplied && densityApplied,
                        "Requested display profile was not applied: "
                                + profile.name()
                                + "; "
                                + displayOutput
                                + "; density="
                                + densityScale * 160);
                assertTrue(
                        fullResolutionCapture,
                        "Checkpoint PNG is not full-display for "
                                + profile.name()
                                + "; actual="
                                + (captured == null
                                        ? "unreadable"
                                        : captured.getWidth() + "x" + captured.getHeight()));
                index++;
            }
            DisplayProfile oneCardProfile = profiles.getFirst();
            long oneCardStarted = System.nanoTime();
            overrideDisplay(adb, ui, oneCardProfile);
            String home =
                    ui.restartToActivity(PACKAGE, ".MainActivity", "--es", "debug_language", "EN");
            assertTrue(
                    ui.hasText(home, "New game"),
                    "Home did not open for the one-card layout check");
            ui.tapText("New game");
            ui.awaitText("Start game", Duration.ofSeconds(12));
            ui.selectNamedSpinnerOption("player-count", "4");
            ui.selectNamedSpinnerOption("game-type", "Romanian (1–8–1)");
            String oneCardSetup = ui.awaitText("Start game", Duration.ofSeconds(10));
            assertTrue(
                    ui.spinnerValue(oneCardSetup, "Players").equals("4")
                            && ui.spinnerValue(oneCardSetup, "Game type")
                                    .equals("Romanian (1–8–1)"),
                    "The centered one-card hand check must use the normal four-player Romanian Whist setup");
            ui.revealText("Start game");
            ui.tapText("Start game");
            String oneCardBidding = ui.awaitText("Place bid", Duration.ofSeconds(20));
            oneCardBidding =
                    ui.awaitDescriptionContaining("Your hand, 1 cards.", Duration.ofSeconds(10));
            AndroidUiDriver.Bounds oneCardBidViewport =
                    ui.boundsForDescription(
                            oneCardBidding,
                            "Your hand, 1 cards.",
                            "android.widget.HorizontalScrollView");
            List<AndroidUiDriver.Bounds> oneCardBidBounds = ui.handCardFaceBounds(oneCardBidding);
            boolean oneCardBidCentered = singleCardCentered(oneCardBidViewport, oneCardBidBounds);
            List<Path> oneCardBidEvidence = ui.capture("whist-lab-single-card-bidding-center");
            evidence.addAll(oneCardBidEvidence);
            checkpoint(
                    trace,
                    "TEST BODY",
                    AtfGherkinKeyword.AND,
                    "Center the single-card hand at the bottom of the Romanian Whist bidding screen",
                    "Four-player Romanian Whist · one card",
                    oneCardBidEvidence,
                    "open normal Romanian Whist setup and inspect the one-card hand bounds",
                    "the only card is centered within its full-width bottom hand viewport",
                    "hand=" + oneCardBidViewport + "; card bounds=" + oneCardBidBounds,
                    Duration.ofNanos(System.nanoTime() - oneCardStarted).toMillis(),
                    List.of(
                            new AtfAssertion(
                                    "One-card bidding hand is centered at the bottom",
                                    "1 card; horizontal center delta <= 2% of hand width",
                                    "cards="
                                            + oneCardBidBounds.size()
                                            + "; center delta="
                                            + singleCardCenterDelta(
                                                    oneCardBidViewport, oneCardBidBounds)
                                            + " px",
                                    oneCardBidCentered)),
                    oneCardBidCentered ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
            assertTrue(
                    oneCardBidCentered,
                    "The single card is not centered at the bottom of the bid screen: hand="
                            + oneCardBidViewport
                            + "; card="
                            + oneCardBidBounds);

            oneCardStarted = System.nanoTime();
            ui.selectBid(ui.numericButtonValues(oneCardBidding).getFirst());
            ui.tapText("Place bid");
            ui.awaitText("PLAY NOW", Duration.ofSeconds(15));
            String oneCardPlay =
                    ui.awaitDescriptionContaining("Your hand, 1 cards.", Duration.ofSeconds(10));
            AndroidUiDriver.Bounds oneCardPlayViewport =
                    ui.boundsForDescription(
                            oneCardPlay,
                            "Your hand, 1 cards.",
                            "android.widget.HorizontalScrollView");
            List<AndroidUiDriver.Bounds> oneCardPlayBounds = ui.handCardFaceBounds(oneCardPlay);
            boolean oneCardPlayCentered =
                    singleCardCentered(oneCardPlayViewport, oneCardPlayBounds);
            List<Path> oneCardPlayEvidence = ui.capture("whist-lab-single-card-playing-center");
            evidence.addAll(oneCardPlayEvidence);
            checkpoint(
                    trace,
                    "TEST BODY",
                    AtfGherkinKeyword.THEN,
                    "Keep the single card centered at the bottom after bidding enters play",
                    "Romanian Whist · PLAY NOW · one card",
                    oneCardPlayEvidence,
                    "submit one bid and inspect the human turn before playing the card",
                    "PLAY NOW is visible and the single card remains centered in the bottom hand row",
                    "hand=" + oneCardPlayViewport + "; card bounds=" + oneCardPlayBounds,
                    Duration.ofNanos(System.nanoTime() - oneCardStarted).toMillis(),
                    List.of(
                            new AtfAssertion(
                                    "One-card playing hand remains centered at the bottom",
                                    "PLAY NOW; 1 card; horizontal center delta <= 2% of hand width",
                                    "PLAY NOW="
                                            + ui.hasText(oneCardPlay, "PLAY NOW")
                                            + "; cards="
                                            + oneCardPlayBounds.size()
                                            + "; center delta="
                                            + singleCardCenterDelta(
                                                    oneCardPlayViewport, oneCardPlayBounds)
                                            + " px",
                                    oneCardPlayCentered && ui.hasText(oneCardPlay, "PLAY NOW"))),
                    oneCardPlayCentered && ui.hasText(oneCardPlay, "PLAY NOW")
                            ? AtfReportStatus.PASS
                            : AtfReportStatus.FAIL);
            assertTrue(
                    oneCardPlayCentered,
                    "The single card is not centered at the bottom of the play screen: hand="
                            + oneCardPlayViewport
                            + "; card="
                            + oneCardPlayBounds);
            outcome = AtfReportStatus.PASS;
            checks =
                    "Four-player Descending presents a genuine thirteen-card first-hand bid screen. The same layout "
                            + "was exercised at "
                            + String.join(
                                    ", ", profiles.stream().map(DisplayProfile::name).toList())
                            + ". Each checkpoint "
                            + "audited every card face, every legal bid, table/seat/button geometry, 48 dp targets, and a "
                            + "full-resolution inline screenshot; no thirteen-card bid was submitted. A normal one-card "
                            + "Romanian Whist was then checked at bidding and play, with its card centered at the bottom.";
        } catch (Throwable failure) {
            outcome = AtfReportStatus.FAIL;
            try {
                List<Path> failureEvidence =
                        ui.capture("whist-lab-thirteen-card-bidding-failure-state");
                evidence.addAll(failureEvidence);
                checkpoint(
                        trace,
                        "TEST BODY",
                        AtfGherkinKeyword.THEN,
                        "Capture the current Android screen after a display check fails",
                        "Current emulator state",
                        failureEvidence,
                        "capture current screenshot and accessibility hierarchy before restoring display",
                        "The failed UI state is preserved in this checkpoint",
                        failure.getClass().getSimpleName()
                                + ": "
                                + String.valueOf(failure.getMessage()),
                        0,
                        List.of(
                                new AtfAssertion(
                                        "Failure screen is retained for audit",
                                        "failure PNG and accessibility XML",
                                        "files=" + failureEvidence.size(),
                                        false)),
                        AtfReportStatus.FAIL);
            } catch (Throwable captureFailure) {
                failure.addSuppressed(captureFailure);
            }
            trace.fail(failure);
            if (failure instanceof Exception exception) throw exception;
            if (failure instanceof Error error) throw error;
            throw new RuntimeException(failure);
        } finally {
            trace.beginPhase("TEARDOWN");
            try {
                restoreDisplay(adb, ui, originalSizeOverride, originalDensityOverride);
                String home =
                        ui.restartToActivity(
                                PACKAGE, ".MainActivity", "--es", "debug_language", "EN");
                List<Path> homeEvidence =
                        ui.capture("whist-lab-thirteen-card-bidding-restored-display-home");
                evidence.addAll(homeEvidence);
                boolean homeRestored =
                        ui.hasText(home, "New game")
                                && matchesRestoredDisplay(
                                        adb, originalSizeOutput, originalDensityOutput);
                checkpoint(
                        trace,
                        "TEARDOWN",
                        AtfGherkinKeyword.THEN,
                        "Restore the original emulator display and leave the game at home",
                        "Original Android display settings",
                        homeEvidence,
                        "reset temporary wm size/density overrides",
                        "original physical display settings restored; Whist Lab home is available",
                        "home visible="
                                + ui.hasText(home, "New game")
                                + "; size="
                                + adb.run("shell", "wm", "size").output().trim()
                                + "; density="
                                + adb.run("shell", "wm", "density").output().trim(),
                        0,
                        List.of(
                                new AtfAssertion(
                                        "Emulator display settings are restored",
                                        "original size and density",
                                        "restored=" + homeRestored,
                                        homeRestored),
                                new AtfAssertion(
                                        "Whist Lab is left at its main menu",
                                        "New game visible",
                                        Boolean.toString(ui.hasText(home, "New game")),
                                        ui.hasText(home, "New game"))),
                        homeRestored ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
                if (!homeRestored) {
                    outcome = AtfReportStatus.FAIL;
                    throw new IllegalStateException(
                            "Emulator display or app home was not restored after resolution checks");
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
            AndroidUiFlowReportPublisher.publish(
                    artifacts.resolve("whist-lab-thirteen-card-bidding-resolutions.html"),
                    installation.packagePath(),
                    reportSetting("atf.android.serial", "ATF_ANDROID_SERIAL", "emulator-5554"),
                    System.getProperty("atf.android.api", "device reported at runtime"),
                    checks,
                    reportEvidence,
                    trace);
        }
    }
}
