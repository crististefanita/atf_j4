package ro.whistlab.core.rules.documentation;

import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.rules.RomanianScoring;

import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;
import static atf.gradle.audit.assertions.GradleAssertions.assertTrue;

public class DocumentedScoringComponentTest extends GradleAuditedTest {
    private static final String DOCUMENT = "docs/game/README.md";
    private static final String HEADER = "| Regulă | 1–8–1 | 8–1–8 |";
    private static final String EXACT = "Puncte adăugate la levatele unui contract exact";
    private static final String LEGACY = "Penalizare de serie în profilul istoric asimetric";

    @Test public void theRomanianScoringTableMatchesTheEngine() throws Exception {
        var table = readTable();
        var checks = compare(table);
        GradleInputs.fixture("Documented scoring table and engine profiles",
                "document", DOCUMENT, "table", table.rows(), "variants", List.of(GameMode.ROMANIAN_WHIST, GameMode.LONG_ROMANIAN));
        for (var check : checks) {
            assertEquals(check.label(), check.observed(), check.declared());
        }
    }

    @Test public void aWrongExactScoreAndAReversedHistoricalPenaltyAreLocalized() throws Exception {
        var table = readTable();
        var altered = table.rows().stream().map(line -> line.startsWith("| " + EXACT + " |")
                ? "| " + EXACT + " | 4 | 5 |"
                : line.startsWith("| " + LEGACY + " |") ? "| " + LEGACY + " | -5 | 5 |" : line).toList();
        GradleInputs.fixture("Two independently altered scoring claims", "document", DOCUMENT, "table", altered);
        var mismatches = mismatches(new Table(table.firstLine(), altered));
        assertEquals("Only the altered rules are refused", List.of(EXACT, LEGACY),
                mismatches.stream().map(Comparison::rule).toList());
        assertEquals("The exact-contract mismatch retains both documented values",
                List.of("4", "5"), mismatches.get(0).declared());
        assertEquals("The historic miss retains its signed result", List.of("-5", "-5"), mismatches.get(1).observed());
        assertTrue("The correct neighbouring table passes", mismatches(table).isEmpty());
    }

    @Test public void aMissingOrDuplicatedRuleDoesNotSilentlyPass() throws Exception {
        var table = readTable();
        var exact = table.rows().stream().filter(row -> row.startsWith("| " + EXACT + " |")).findFirst().orElseThrow();
        var missing = table.rows().stream().filter(row -> !row.equals(exact)).toList();
        var duplicate = new java.util.ArrayList<>(table.rows());
        duplicate.add(exact);
        GradleInputs.fixture("Malformed versions of the declared table", "missing", missing, "duplicate", duplicate);
        assertEquals("Missing row is detected", List.of("missing row"), mismatches(new Table(table.firstLine(), missing)).get(0).declared());
        assertEquals("Duplicate row is detected", List.of("duplicate row"), mismatches(new Table(table.firstLine(), duplicate)).get(0).declared());
    }

    private static Table readTable() throws Exception {
        var lines = Files.readAllLines(Path.of("..", "..", DOCUMENT));
        int start = lines.indexOf(HEADER);
        if (start < 0 || start != lines.lastIndexOf(HEADER)) {
            throw new IllegalStateException(DOCUMENT + " requires one Romanian scoring table");
        }
        return new Table(start + 3, lines.stream().skip(start + 2L).takeWhile(line -> line.startsWith("|")).toList());
    }

    private static List<Comparison> mismatches(Table table) {
        return compare(table).stream().filter(row -> !row.observed().equals(row.declared())).toList();
    }

    private static List<Comparison> compare(Table table) {
        var standard = observed(GameMode.ROMANIAN_WHIST);
        var longGame = observed(GameMode.LONG_ROMANIAN);
        return standard.entrySet().stream().map(rule -> {
            var rows = table.rows().stream().filter(line -> line.startsWith("| " + rule.getKey() + " |")).toList();
            List<String> declared = declaredValues(rows);
            int line = rows.isEmpty() ? table.firstLine() : table.firstLine() + table.rows().indexOf(rows.get(0));
            return new Comparison(rule.getKey(), line, List.of(rule.getValue(), longGame.get(rule.getKey())), declared);
        }).toList();
    }

    private static List<String> declaredValues(List<String> rows) {
        if (rows.size() != 1) return List.of(rows.isEmpty() ? "missing row" : "duplicate row");
        String[] cells = rows.get(0).split("\\|", -1);
        if (cells.length != 5 || !cells[4].isBlank()) return List.of("malformed row");
        return List.of(cells[2].strip(), cells[3].strip());
    }

    private static Map<String, String> observed(GameMode mode) {
        var facts = new LinkedHashMap<String, String>();
        facts.put(EXACT, Integer.toString(GameRules.scoreDelta(3, 3, mode) - 3));
        int shortfall = GameRules.scoreDelta(2, 3, mode);
        int surplus = GameRules.scoreDelta(3, 2, mode);
        facts.put("Puncte pe levată de abatere", shortfall == surplus
                ? Integer.toString(shortfall) : shortfall + ", " + surplus);
        var ten = GameConfig.newMatch(4, mode, AiDifficulty.NORMAL, 31, true, 10);
        int exactSeries = firstPayout(ten, 2);
        int missedSeries = firstPayout(ten, 0);
        facts.put("Mâini eligibile pentru plata seriei", exactSeries == missedSeries
                ? Integer.toString(exactSeries) : exactSeries + ", " + missedSeries);
        facts.put("Bonus și penalizare la opțiunea 5",
                adjustments(GameConfig.newMatch(4, mode, AiDifficulty.NORMAL, 31, true, 5)));
        facts.put("Bonus și penalizare la opțiunea 10", adjustments(ten));
        var historical = new GameConfig(4, mode, AiDifficulty.NORMAL, 31, true, 10);
        facts.put(LEGACY, Integer.toString(missedSeries(historical)));
        return facts;
    }

    private static int firstPayout(GameConfig config, int won) {
        int streak = 0;
        for (int hand = 1; hand <= 8; hand++) {
            var result = RomanianScoring.score(won, 2, 4, streak, config.streakBonusPoints(), config.streakPenaltyPoints());
            if (result.streakPoints() != 0) return hand;
            streak = result.nextStreak();
        }
        return 0;
    }

    private static String adjustments(GameConfig config) {
        int exact = RomanianScoring.score(2, 2, 4, 4, config.streakBonusPoints(), config.streakPenaltyPoints()).streakPoints();
        return exact + ", " + missedSeries(config);
    }

    private static int missedSeries(GameConfig config) {
        return RomanianScoring.score(0, 2, 4, -4, config.streakBonusPoints(), config.streakPenaltyPoints()).streakPoints();
    }

    private record Table(int firstLine, List<String> rows) { }

    private record Comparison(String rule, int line, List<String> observed, List<String> declared) {
        String label() {
            return DOCUMENT + ":" + line + " · " + rule + " · GameRules / GameConfig / RomanianScoring";
        }
    }
}
