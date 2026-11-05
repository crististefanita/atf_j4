package atf.android.e2e.display.flow.responsive;

import static atf.android.e2e.display.flow.responsive.ResponsiveEvidence.check;
import static atf.android.e2e.display.flow.responsive.ResponsiveEvidence.checkCaptureSize;
import static atf.android.e2e.display.flow.responsive.ResponsiveEvidence.publishCheckpoint;
import static atf.android.e2e.display.flow.responsive.ResponsiveEvidence.record;
import static atf.android.e2e.display.flow.responsive.ResponsiveGeometry.box;
import static atf.android.e2e.display.flow.responsive.ResponsiveGeometry.contains;
import static atf.android.e2e.display.flow.responsive.ResponsiveGeometry.overlaps;
import static atf.android.e2e.display.flow.responsive.ResponsiveSettings.APP;
import static atf.android.e2e.display.flow.responsive.ResponsiveSettings.HAND_CARD_FACE_HEIGHT_DP;
import static atf.android.e2e.display.flow.responsive.ResponsiveSettings.WAIT;
import static atf.android.e2e.display.flow.responsive.ResponsiveTableState.awaitPlayableTurn;
import static atf.android.e2e.display.flow.responsive.ResponsiveTableState.scrollUntilVisible;
import static atf.android.e2e.display.flow.responsive.ResponsiveTableState.seatBounds;
import static atf.android.e2e.display.flow.responsive.ResponsiveTableState.seatCount;

import atf.android.AdbClient;
import atf.android.e2e.display.profile.ResponsiveDisplayProfile;
import atf.android.e2e.display.support.ScreenGeometry;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.android.whist.evidence.HandIndexAudit;
import atf.reporting.contract.AtfAssertion;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;

/** Inspecții ale meniurilor, mâinii și controalelor de licitare. */
public final class ResponsiveMenuChecks {
    private ResponsiveMenuChecks() {}

    public static void inspectHomeAndSetup(
            AndroidUiDriver ui,
            AdbClient adb,
            AndroidUiFlowTrace trace,
            List<Path> evidence,
            ResponsiveDisplayProfile profile)
            throws Exception {
        String home = ui.restartToActivity(APP, ".MainActivity", "--es", "debug_language", "EN");
        record(
                ui,
                adb,
                trace,
                evidence,
                profile,
                "Open the normal home menu",
                List.of(
                        check(
                                "New game and language are visible",
                                "New game and one language selector",
                                ui.hasText(home, "New game")
                                        && ui.hasClass(home, "android.widget.Spinner")),
                        check(
                                "Home has no scroll overflow",
                                "all home content inside the first viewport",
                                ui.allContentFitsViewport(home))));

        ui.tapText("High scores");
        String rankings = ui.awaitText("High scores", WAIT);
        rankings = ui.awaitDescriptionContaining("Top 10 / Top 20", WAIT);
        String rankingLimit = ui.spinnerValue(rankings, "High scores · Top 10 / Top 20");
        record(
                ui,
                adb,
                trace,
                evidence,
                profile,
                "Open the local high-score list",
                List.of(
                        check(
                                "Ranking dialog opens on the installed Android API",
                                "High scores title and a supported Top 10 / Top 20 selection",
                                ui.hasText(rankings, "High scores")
                                        && List.of("Top 10", "Top 20").contains(rankingLimit))));
        ui.tapText("CLOSE");

        ui.tapText("New game");
        String setup = ui.awaitText("Start game", WAIT);
        ui.selectNamedSpinnerOption("player-count", "4");
        ui.selectNamedSpinnerOption("game-type", "Romanian (1–8–1)");
        setup = ui.awaitText("Start game", WAIT);
        record(
                ui,
                adb,
                trace,
                evidence,
                profile,
                "Set up a four-player Romanian table",
                List.of(
                        check(
                                "Selected game type",
                                "Romanian (1–8–1)",
                                ui.spinnerValue(setup, "Game type").equals("Romanian (1–8–1)")),
                        check(
                                "Start remains visible without scrolling",
                                "Start game and full setup inside viewport",
                                ui.hasText(setup, "Start game")
                                        && ui.allContentFitsViewport(setup))));
    }

    public static void inspectThirteenCardFan(
            AdbClient adb,
            AndroidUiDriver ui,
            AndroidUiFlowTrace trace,
            List<Path> evidence,
            ResponsiveDisplayProfile profile)
            throws Exception {
        ui.restartToActivity(
                APP,
                ".MainActivity",
                "--ez",
                "debug_hand_fan_fixture",
                "true",
                "--ez",
                "debug_hold_ai_turns",
                "true",
                "--es",
                "debug_language",
                "EN");
        String initial = ui.awaitText("Whist · Descending", WAIT);
        record(
                ui,
                adb,
                trace,
                evidence,
                profile,
                "Check the game title before revealing the hand",
                List.of(check("Fixture title", "Whist · Descending",
                        ui.hasText(initial, "Whist · Descending"))));
        String xml = scrollUntilVisible(
                adb, ui, profile, "Your hand, 13 cards.", true, HAND_CARD_FACE_HEIGHT_DP);
        List<Path> capture = ui.capture(profile.id() + "-thirteen-card-fan");
        evidence.addAll(capture);
        var faces =
                ui.handCardFaceBounds(xml).stream()
                        .sorted(Comparator.comparingInt(AndroidUiDriver.Bounds::left))
                        .toList();
        var labels = ui.bottomRowCardLabels(xml);
        var viewport = ui.boundsForDescriptionContaining(xml, "Your hand,");
        int centerError =
                faces.size() == 13
                        ? Math.abs(
                                faces.getFirst().left()
                                        + faces.getLast().right()
                                        - viewport.left()
                                        - viewport.right())
                        : Integer.MAX_VALUE;
        int expectedFaceHeight =
                (int) Math.round(HAND_CARD_FACE_HEIGHT_DP * profile.density() / 160.0);
        int shortestFaceHeight = faces.stream().mapToInt(ScreenGeometry::height).min().orElse(0);
        boolean fullFacesVisible =
                faces.size() == 13
                        && faces.stream()
                                .allMatch(
                                        face ->
                                                face.top() >= viewport.top()
                                                        && face.bottom() <= viewport.bottom()
                                                        && ScreenGeometry.height(face)
                                                                >= expectedFaceHeight);
        long labelBaselines =
                labels.stream().map(AndroidUiDriver.CardLabel::top).distinct().count();
        List<AtfAssertion> checks =
                new ArrayList<>(
                        List.of(
                                check(
                                        "All thirteen cards fit on one row",
                                        "13",
                                        faces.size(),
                                        faces.size() == 13 && labels.size() == 13),
                                check(
                                        "All thirteen card faces are fully visible after recorded scrolling",
                                        expectedFaceHeight + " px minimum",
                                        shortestFaceHeight + " px minimum; full faces="
                                                + fullFacesVisible,
                                        fullFacesVisible),
                                check(
                                        "Dense hand is centered",
                                        "within one pixel",
                                        centerError / 2.0,
                                        centerError <= 2),
                                check(
                                        "Indices share one baseline",
                                        "one row",
                                        labelBaselines,
                                        labels.size() == 13 && labelBaselines == 1)));
        Path screenshot =
                capture.stream()
                        .filter(path -> path.toString().endsWith(".png"))
                        .findFirst()
                        .orElseThrow();
        BufferedImage image = ImageIO.read(screenshot.toFile());
        int[] display = AndroidUiDriver.displayDimensions(adb.run("shell", "wm", "size").output());
        List<AndroidUiDriver.Bounds> indices =
                ui.playingCardLabelBounds(xml).stream()
                        .sorted(Comparator.comparingInt(AndroidUiDriver.Bounds::left))
                        .toList();
        for (int i = 0; i < Math.min(indices.size(), faces.size()); i++) {
            HandIndexAudit.Result pixelAudit =
                    HandIndexAudit.inspect(
                            image, display[0], display[1], box(indices.get(i)), box(faces.get(i)));
            checks.add(
                    check(
                            "Rank and suit remain exposed " + (i + 1),
                            "readable",
                            pixelAudit.readable(),
                            pixelAudit.readable()));
        }
        if (indices.size() != 13 || faces.size() != 13) {
            checks.add(
                    check(
                            "Thirteen card rank and suit pixels are auditable",
                            "13 visible indices",
                            "only " + Math.min(indices.size(), faces.size()) + " complete cards",
                            false));
        }
        checkCaptureSize(adb, capture, profile, checks);
        publishCheckpoint(
                ui, trace, profile, "Inspect the full thirteen-card fan", capture, checks);
    }

    public static void inspectHardAiBid(
            AdbClient adb,
            AndroidUiDriver ui,
            AndroidUiFlowTrace trace,
            List<Path> evidence,
            ResponsiveDisplayProfile profile)
            throws Exception {
        ui.restartToActivity(
                APP,
                ".MainActivity",
                "--ez",
                "debug_hard_ai_bid_fixture",
                "true",
                "--ez",
                "debug_hold_ai_turns",
                "true",
                "--es",
                "debug_language",
                "EN");
        String bidding = ui.awaitText("Place bid", WAIT);
        boolean startsWithTwoCards = ui.hasDescription(bidding, "Your hand, 2 cards.");
        if (!startsWithTwoCards) {
            throw new IllegalStateException(
                    "Hard-AI compatibility fixture must use two-card deals: "
                            + ui.descriptions(bidding));
        }
        record(
                ui,
                adb,
                trace,
                evidence,
                profile,
                "Open the two-card Hard-AI bidding fixture",
                List.of(
                        check(
                                "Hard-AI fixture starts with two cards",
                                "Your hand, 2 cards.",
                                startsWithTwoCards)));

        ui.tapText("Place bid");
        String play = awaitPlayableTurn(adb, ui);
        int visibleSeats = seatCount(ui, play);
        record(
                ui,
                adb,
                trace,
                evidence,
                profile,
                "Complete Hard-AI bids on the Android runtime",
                List.of(
                        check(
                                "All four seats finish bidding without a runtime crash",
                                "4 seats",
                                visibleSeats,
                                visibleSeats == 4),
                        check(
                                "Human receives a playable turn",
                                "PLAY NOW and two cards",
                                ui.hasText(play, "PLAY NOW")
                                        && ui.hasDescription(play, "Your hand, 2 cards."),
                                ui.hasText(play, "PLAY NOW")
                                        && ui.hasDescription(play, "Your hand, 2 cards."))));
    }

    public static void inspectNineBidTable(
            AdbClient adb,
            AndroidUiDriver ui,
            AndroidUiFlowTrace trace,
            List<Path> evidence,
            ResponsiveDisplayProfile profile)
            throws Exception {
        ui.restartToActivity(
                APP,
                ".MainActivity",
                "--ez",
                "debug_nine_bids_fixture",
                "true",
                "--ez",
                "debug_hold_ai_turns",
                "true",
                "--es",
                "debug_language",
                "EN");
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
        int controlsTop =
                controls.stream()
                        .mapToInt(AndroidUiDriver.Bounds::top)
                        .min()
                        .orElse(Integer.MAX_VALUE);
        int controlsBottom =
                controls.stream()
                        .mapToInt(AndroidUiDriver.Bounds::bottom)
                        .max()
                        .orElse(Integer.MIN_VALUE);
        int nearestOpponentBottom =
                seats.stream()
                        .filter(seat -> seat.bottom() <= human.top())
                        .mapToInt(AndroidUiDriver.Bounds::bottom)
                        .max()
                        .orElse(Integer.MAX_VALUE);
        int opponentGap = controlsTop - nearestOpponentBottom;
        int humanGap = human.top() - controlsBottom;
        boolean favorsHuman =
                nearestOpponentBottom < controlsTop
                        && controlsBottom < human.top()
                        && humanGap < opponentGap;
        boolean inside = occupied.stream().allMatch(region -> contains(board, region));
        double scale = profile.density() / 160.0;
        boolean touch =
                controls.stream()
                        .allMatch(
                                region ->
                                        region.right() - region.left() >= Math.floor(48 * scale)
                                                && region.bottom() - region.top()
                                                        >= Math.floor(48 * scale));
        List<AtfAssertion> checks =
                new ArrayList<>(
                        List.of(
                                check(
                                        "Nine legal bids fit",
                                        "0..8",
                                        bids.toString(),
                                        bids.equals(
                                                java.util.stream.IntStream.rangeClosed(0, 8)
                                                        .boxed()
                                                        .toList())),
                                check(
                                        "Four players remain individually visible",
                                        "You, Ava, Miles, Grace",
                                        seats.size(),
                                        seats.size() == 4),
                                check(
                                        "No player or bid controls overlap",
                                        "0",
                                        overlaps,
                                        overlaps == 0),
                                check(
                                        "Nine-bid zone is closer to the human seat",
                                        "opponent gap > human gap",
                                        "opponent="
                                                + opponentGap
                                                + " px; human="
                                                + humanGap
                                                + " px",
                                        favorsHuman),
                                check(
                                        "Bid controls remain inside the table",
                                        "inside",
                                        inside,
                                        inside),
                                check(
                                        "Bid hit targets remain at least 48 dp",
                                        "accessible",
                                        touch,
                                        touch)));
        checkCaptureSize(adb, capture, profile, checks);
        publishCheckpoint(ui, trace, profile, "Inspect all nine bid controls", capture, checks);
    }
}
