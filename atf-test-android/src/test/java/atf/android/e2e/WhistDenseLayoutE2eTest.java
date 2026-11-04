package atf.android.e2e;

import atf.android.AdbClient;
import atf.android.e2e.support.AndroidUiFlowReportPublisher;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.android.whist.evidence.HandIndexAudit;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Audits the two previously missed intersections, using the actual rendered glyph bands and hit targets. */
@AndroidE2eTest
class WhistDenseLayoutE2eTest {
    private static final String PACKAGE = "ro.whistlab.app";
    private record Profile(String id, String size, int density) { }

    @Test void thirteenIndicesAndNineBidChoicesRemainDistinctAcrossPortraitProfiles() throws Exception {
        var adb = AdbClient.fromSystemProperties();
        Path output = Path.of(System.getProperty("atf.android.artifacts")).resolve("dense-layout");
        var ui = new AndroidUiDriver(adb, output);
        var trace = new AndroidUiFlowTrace(output, "dense-layout", "Whist — exposed indices and collision-free bidding",
                getClass().getSimpleName(), "atf.android.e2e", PACKAGE, List.of("android", "e2e", "display", "13-cards", "9-bids"));
        ui.recordActionsAs(trace::recordAction);
        String size = override(adb.run("shell", "wm", "size").output(), "size");
        String density = override(adb.run("shell", "wm", "density").output(), "density");
        var evidence = new ArrayList<Path>();
        var outcome = AtfReportStatus.FAIL;
        try {
            for (var profile : List.of(new Profile("320dp", "720x1280", 360),
                    new Profile("353dp", "972x1728", 440), new Profile("ROG7-logical", "1080x2448", 440),
                    new Profile("600dp", "1200x1920", 320))) {
                adb.run("shell", "wm", "size", profile.size).requireSuccess();
                adb.run("shell", "wm", "density", Integer.toString(profile.density)).requireSuccess();
                String xml = ui.restartToActivity(PACKAGE, ".MainActivity", "--ez", "debug_hand_fan_fixture", "true",
                        "--ez", "debug_hold_ai_turns", "true", "--es", "debug_language", "EN");
                xml = ui.awaitDescriptionContaining("Your hand, 13 cards.", Duration.ofSeconds(15));
                var capture = ui.capture(profile.id + "-13-indices"); evidence.addAll(capture);
                var screenshot = capture.stream().filter(path -> path.toString().endsWith(".png")).findFirst().orElseThrow();
                var image = ImageIO.read(screenshot.toFile());
                int[] display = AndroidUiDriver.displayDimensions(adb.run("shell", "wm", "size").output());
                var labels = ui.bottomRowCardLabels(xml);
                var indices = ui.playingCardLabelBounds(xml).stream().sorted(Comparator.comparingInt(AndroidUiDriver.Bounds::left)).toList();
                var faces = ui.handCardFaceBounds(xml).stream().sorted(Comparator.comparingInt(AndroidUiDriver.Bounds::left)).toList();
                var checks = new ArrayList<AtfAssertion>();
                checks.add(check("Effective display profile", profile.size + "@" + profile.density,
                        display[0] + "x" + display[1] + "@" + effectiveDensity(adb),
                        profile.size.equals(display[0] + "x" + display[1]) && effectiveDensity(adb) == profile.density));
                checks.add(check("13 indices include all four tens", "13; 10♠,10♥,10♣,10♦",
                        labels.stream().map(AndroidUiDriver.CardLabel::text).toList().toString(), labels.size() == 13 && labels.stream().map(AndroidUiDriver.CardLabel::text).toList()
                                .containsAll(List.of("10♠", "10♥", "10♣", "10♦"))));
                assertEquals(13, indices.size(), "Missing index geometry"); assertEquals(13, faces.size(), "Missing face geometry");
                for (int i = 0; i < indices.size(); i++) {
                    var result = HandIndexAudit.inspect(image, display[0], display[1], box(indices.get(i)), box(faces.get(i)));
                    checks.add(check("Exposed rank and suit " + labels.get(i).text(), "both glyphs; clear right padding; inside exposed face",
                            result.toString(), result.readable()));
                }
                checks.add(check("Complete card height inside viewport", "98 dp", "minimum px=" + faces.stream().mapToInt(b -> b.bottom() - b.top()).min().orElse(0),
                        faces.stream().allMatch(face -> face.bottom() - face.top() >= Math.round(98 * profile.density / 160.0) - 1)));
                checks.add(check("Single row", "one top coordinate", labels.stream().map(AndroidUiDriver.CardLabel::top).distinct().toList().toString(),
                        labels.stream().map(AndroidUiDriver.CardLabel::top).distinct().count() == 1));
                record(trace, profile.id + " — all thirteen indices", capture, checks);

                xml = ui.restartToActivity(PACKAGE, ".MainActivity", "--ez", "debug_nine_bids_fixture", "true",
                        "--ez", "debug_hold_ai_turns", "true", "--es", "debug_language", "EN");
                xml = ui.awaitText("Place bid", Duration.ofSeconds(15));
                capture = ui.capture(profile.id + "-9-bids"); evidence.addAll(capture);
                var seats = seats(ui, xml);
                var controls = new ArrayList<AndroidUiDriver.Bounds>();
                var bids = ui.numericButtonValues(xml);
                for (int bid : bids) controls.add(ui.buttonBounds(xml, Integer.toString(bid)));
                controls.add(ui.boundsForText(xml, "Place bid"));
                var board = ui.boundsForDescription(xml, "Playing surface");
                var occupied = new ArrayList<>(seats); occupied.addAll(controls);
                int collisions = 0;
                for (int i = 0; i < occupied.size(); i++) for (int j = i + 1; j < occupied.size(); j++) {
                    if (overlaps(occupied.get(i), occupied.get(j))) collisions++;
                }
                double scale = profile.density / 160.0;
                boolean touch = controls.stream().allMatch(b -> b.right() - b.left() >= Math.floor(48 * scale)
                        && b.bottom() - b.top() >= Math.floor(48 * scale));
                checks = new ArrayList<>(List.of(
                        check("Complete legal bid domain", "0..8", bids.toString(), bids.equals(java.util.stream.IntStream.rangeClosed(0, 8).boxed().toList())),
                        check("Four distinct seats", "4", seats.size(), seats.size() == 4),
                        check("No seat/control or control/control overlap", "0", collisions, collisions == 0),
                        check("All occupied regions remain inside the board", "inside", "regions=" + occupied.size(), occupied.stream().allMatch(b -> box(board).contains(box(b)))),
                        check("Accessible bid hit targets", ">=48 dp", "targets=" + controls.size() + "; full target=" + touch, touch)));
                record(trace, profile.id + " — nine bids and four seats", capture, checks);
                ui.selectBid(8);
                String selected = ui.dump();
                capture = ui.capture(profile.id + "-selected-bid"); evidence.addAll(capture);
                record(trace, profile.id + " — select without moving seats", capture, List.of(
                        check("Bid 8 selected before confirmation", "selected", ui.isSelected(selected, "8"), ui.isSelected(selected, "8")),
                        check("Seat bounds stay fixed", digest(seats), digest(seats(ui, selected)), seats.equals(seats(ui, selected)))));
            }
            outcome = AtfReportStatus.PASS;
        } catch (Throwable failure) {
            trace.fail(failure);
            throw failure;
        } finally {
            try {
                adb.run("shell", "wm", "size", size).requireSuccess();
                adb.run("shell", "wm", "density", density).requireSuccess();
                adb.run("shell", "am", "force-stop", PACKAGE).requireSuccess();
            } catch (Throwable cleanup) { outcome = AtfReportStatus.FAIL; trace.fail(cleanup); throw cleanup; }
            finally {
                trace.finish(outcome); evidence.add(trace.manifest());
                AndroidUiFlowReportPublisher.publish(output.resolve("dense-layout.html"), "ro.whistlab.app",
                        System.getProperty("atf.android.serial"), System.getProperty("atf.android.api"),
                        "Deterministic display fixtures; no claim of a completed match or physical ROG testing.", evidence, trace);
            }
        }
    }

    private static void record(AndroidUiFlowTrace trace, String title, List<Path> evidence, List<AtfAssertion> checks) throws Exception {
        boolean passed = checks.stream().allMatch(AtfAssertion::passed);
        trace.checkpoint("TEST BODY", trace.phaseSnapshots().isEmpty() ? "GIVEN" : "AND", title, "Rendered Android table",
                evidence.stream().filter(path -> path.toString().endsWith(".png")).findFirst().orElseThrow(), List.of(new atf.reporting.ui.model.AtfUiActionRecord(trace.nextActionSequence(), "Inspect rendered geometry and pixels",
                        "Android table", "Visible, separate and legible regions", passed ? "All recorded comparisons passed" : "See failed comparison",
                        0, passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL, atf.reporting.ui.model.AtfUiEvidenceSource.OBSERVE)), checks,
                passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL,
                evidence.stream().filter(path -> path.toString().endsWith(".xml")).toList());
        for (var check : checks) assertTrue(check.passed(), check.name() + ": " + check.actual());
    }
    private static AtfAssertion check(String name, Object expected, Object actual, boolean passed) {
        return new AtfAssertion(name, String.valueOf(expected), String.valueOf(actual), passed);
    }
    private static List<AndroidUiDriver.Bounds> seats(AndroidUiDriver ui, String xml) throws Exception {
        var seats = new ArrayList<AndroidUiDriver.Bounds>();
        for (String value : ui.descriptions(xml)) if (value.contains(". Bid ") && value.contains(". Score ")) seats.add(ui.boundsForDescription(xml, value));
        return seats;
    }
    private static String digest(List<AndroidUiDriver.Bounds> bounds) {
        return bounds.stream().map(b -> b.left() + "," + b.top() + ":" + b.right() + "," + b.bottom()).collect(java.util.stream.Collectors.joining("; "));
    }
    private static HandIndexAudit.Box box(AndroidUiDriver.Bounds b) { return new HandIndexAudit.Box(b.left(), b.top(), b.right(), b.bottom()); }
    private static boolean overlaps(AndroidUiDriver.Bounds a, AndroidUiDriver.Bounds b) {
        return a.left() < b.right() && a.right() > b.left() && a.top() < b.bottom() && a.bottom() > b.top();
    }
    private static String override(String output, String property) {
        var match = Pattern.compile("(?m)^Override " + property + ":\\s*(\\S+)").matcher(output);
        return match.find() ? match.group(1) : "reset";
    }
    private static int effectiveDensity(AdbClient adb) throws Exception {
        String output = adb.run("shell", "wm", "density").output();
        String value = override(output, "density");
        if (!value.equals("reset")) return Integer.parseInt(value);
        var match = Pattern.compile("Physical density:\\s*(\\d+)").matcher(output);
        if (!match.find()) throw new IllegalStateException(output);
        return Integer.parseInt(match.group(1));
    }
}
