package atf.android.e2e;

import atf.android.AdbClient;
import atf.android.e2e.support.AndroidUiFlowReportPublisher;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AndroidE2eTest
class WhistTableAndScoreSanityE2eTest {
    private static final String APP = "ro.whistlab.app";
    private static final Duration WAIT = Duration.ofSeconds(30);
    private static final Pattern SCORE = Pattern.compile("(.+) \\| Bid: (\\d+) \\| Tricks won: (\\d+) \\| Hand: ([+-]?\\d+) \\| Total: (-?\\d+)");

    @Test void menuTrumpHandAndResumedContracts() throws Exception {
        run("table-score", "Whist Lab — table and resumed score history",
                "Normal four-seat Romanian setup, one real hand and process restart.", (ui, trace, evidence) -> {
            ui.restartToActivity(APP, ".MainActivity", "--es", "debug_language", "EN");
            ui.tapText("New game");
            ui.tapDescription("Game type");
            String options = ui.awaitText("Descending", WAIT);
            var modes = ui.texts(options);
            var expected = List.of("Oh Hell", "Descending", "Romanian (1–8–1)", "Down & Up", "Long Romanian (8-1-8)", "Reverse Oh Hell");
            record(ui, trace, evidence, "GIVEN", "Six new-game choices", List.of(check("Available modes", expected, modes,
                    modes.containsAll(expected) && !modes.contains("Practice hand") && !modes.contains("Maximum"))));
            ui.tapText("Romanian (1–8–1)");
            ui.tapText("Start game");
            String bid = ui.awaitText("Place bid", WAIT);
            var hand = ui.boundsForDescriptionContaining(bid, "Your hand, 1 cards.");
            var faces = ui.handCardFaceBounds(bid);
            var tile = ui.boundsForDescriptionContaining(bid, "Trump set by turned card:");
            var title = ui.boundsForText(bid, "Whist · Romanian");
            var board = ui.boundsForDescription(bid, "Playing surface");
            record(ui, trace, evidence, "WHEN", "Inspect the turned card and the one-card hand", List.of(
                    check("Single card centered", "center within one pixel", faces,
                            faces.size() == 1 && Math.abs(faces.getFirst().left() + faces.getFirst().right() - hand.left() - hand.right()) <= 2),
                    check("Trump identity", "visible rank and suit match the accessible name", ui.descriptions(bid).stream().filter(value -> value.startsWith("Trump set by turned card:")).toList(), ui.turnedTrumpIdentityMatchesVisibleCard(bid)),
                    check("Trump border clear of title and board", "two positive gaps", List.of(tile, title, board),
                            title.right() < tile.left() && tile.bottom() < board.top())));
            int contract = ui.numericButtonValues(bid).getFirst();
            ui.selectBid(contract);
            ui.tapText("Place bid");
            String play = ui.awaitText("PLAY NOW", WAIT);
            String legal = ui.descriptions(play).stream().filter(s -> s.endsWith(". Legal card")).findFirst().orElseThrow();
            ui.tapDescription(legal);
            ui.awaitText("Next hand", WAIT);
            var rows = new LinkedHashMap<String, String>();
            for (String description : ui.descriptions(ui.dump())) {
                var score = SCORE.matcher(description);
                if (!score.matches()) continue;
                int offered = Integer.parseInt(score.group(2)), won = Integer.parseInt(score.group(3));
                int points = won == offered ? 5 + won : -Math.abs(won - offered);
                assertEquals(points, Integer.parseInt(score.group(4)));
                assertEquals(points, Integer.parseInt(score.group(5)));
                rows.put(score.group(1), "Hand 1, Bid " + offered + ", Points " + score.group(4) + ", Total " + points);
            }
            assertEquals(4, rows.size());
            assertTrue(rows.get("You").startsWith("Hand 1, Bid " + contract + ","));
            record(ui, trace, evidence, "THEN", "Settle four contracts from bids and tricks", List.of(
                    check("Scored seats", 4, rows.size(), rows.size() == 4)));
            ui.restartToActivity(APP, ".MainActivity", "--es", "debug_language", "EN");
            ui.tapText("Resume game");
            ui.revealText("Score history");
            ui.tapText("Score history");
            for (var seat : rows.entrySet()) {
                if (!seat.getKey().equals("You")) ui.selectNamedSpinnerOption("Player", seat.getKey());
                String history = ui.awaitDescriptionContaining(seat.getValue(), WAIT);
                record(ui, trace, evidence, "AND", "Read resumed history for " + seat.getKey(), List.of(
                        check("Bid, points and total after restart", seat.getValue(), ui.descriptions(history),
                                ui.hasDescription(history, seat.getValue()) && ui.hasText(history, "Bid") && ui.hasText(history, "Total"))));
            }
        });
    }

    @Test void shortAndDenseHandsStayCentered() throws Exception {
        run("hand-fan", "Whist Lab — centered two-card and thirteen-card hands",
                "Two deterministic portrait layouts. No hand submitted.", (ui, trace, evidence) -> {
            for (int count : List.of(2, 13)) {
                ui.restartToActivity(APP, ".MainActivity", "--es", "debug_language", "EN",
                        "--ez", count == 2 ? "debug_trick_pause_fixture" : "debug_hand_fan_fixture", "true",
                        "--ez", "debug_hold_ai_turns", "true");
                String xml = ui.awaitDescriptionContaining("Your hand, " + count + " cards.", WAIT);
                var viewport = ui.boundsForDescriptionContaining(xml, "Your hand,");
                var faces = ui.handCardFaceBounds(xml).stream()
                        .sorted(java.util.Comparator.comparingInt(AndroidUiDriver.Bounds::left)).toList();
                assertEquals(count, faces.size());
                var left = faces.getFirst();
                var right = faces.getLast();
                var seat = ui.boundsForDescriptionContaining(xml, "You. Bid ");
                int centerError = Math.abs(left.left() + right.right() - viewport.left() - viewport.right());
                int stride = faces.get(1).left() - left.left();
                record(ui, trace, evidence, count == 2 ? "GIVEN" : "THEN", count + " cards centered below the player", List.of(
                        check("Visual center", "within one pixel", centerError / 2.0, centerError <= 2),
                        check("Adjacent faces overlap", "positive stride below face width", stride,
                                stride > 0 && stride < right.right() - right.left()),
                        check("Single row below seat", "same baseline, clear of seat", "faces=" + faces.size() + ", seat bottom=" + seat.bottom() + ", first=" + left + ", last=" + right,
                                faces.stream().allMatch(f -> f.bottom() == right.bottom() && f.top() > seat.bottom()))));
            }
        });
    }

    @FunctionalInterface private interface Scenario {
        void execute(AndroidUiDriver ui, AndroidUiFlowTrace trace, List<Path> evidence) throws Exception;
    }

    private void run(String id, String name, String scope, Scenario scenario) throws Exception {
        var adb = AdbClient.fromSystemProperties();
        Path out = Path.of(System.getProperty("atf.android.artifacts")).resolve(id);
        var ui = new AndroidUiDriver(adb, out);
        var trace = new AndroidUiFlowTrace(out, id, name, getClass().getSimpleName(), "atf.android.e2e",
                APP, List.of("android", "sanity", "display"));
        ui.recordActionsAs(trace::recordAction);
        var evidence = new ArrayList<Path>();
        var result = AtfReportStatus.FAIL;
        try {
            scenario.execute(ui, trace, evidence);
            result = AtfReportStatus.PASS;
        } catch (Throwable failure) {
            trace.fail(failure);
            try { evidence.addAll(ui.capture("failure")); } catch (Exception diagnostic) { failure.addSuppressed(diagnostic); }
            throw failure;
        } finally {
            try { adb.run("shell", "am", "force-stop", APP).requireSuccess(); }
            finally {
                trace.finish(result);
                evidence.add(trace.manifest());
                AndroidUiFlowReportPublisher.publish(out.resolve(id + ".html"), APP,
                        System.getProperty("atf.android.serial"), System.getProperty("atf.android.api"),
                        scope, evidence, trace);
            }
        }
    }

    private static AtfAssertion check(String name, Object expected, Object actual, boolean passed) {
        return new AtfAssertion(name, expected.toString(), actual.toString(), passed);
    }

    private static void record(AndroidUiDriver ui, AndroidUiFlowTrace trace, List<Path> evidence,
            String keyword, String title, List<AtfAssertion> checks) throws Exception {
        var files = ui.capture(title.replace(' ', '-'));
        evidence.addAll(files);
        boolean passed = checks.stream().allMatch(AtfAssertion::passed);
        trace.checkpoint("TEST BODY", keyword, title, "Whist Lab", files.stream().filter(p -> p.toString().endsWith(".png")).findFirst().orElseThrow(),
                List.of(new atf.reporting.ui.model.AtfUiActionRecord(trace.nextActionSequence(), "Inspect Android hierarchy", title,
                        "Compare rendered state with contract", passed ? "Checks passed" : "Comparison failed", 0,
                        passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL, atf.reporting.ui.model.AtfUiEvidenceSource.OBSERVE)),
                checks, passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL, files.stream().filter(p -> p.toString().endsWith(".xml")).toList());
        for (var check : checks) assertTrue(check.passed(), check.name() + ": " + check.actual());
    }
}
