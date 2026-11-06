package atf.tests.governance.documentation.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Generic testing guides own contracts; run-specific totals and release paths belong to evidence. */
final class TestingStrategyRules {
    static final String OWNER = "<!-- atf-testing-strategy-owner -->";
    private static final Pattern RESULTS = Pattern.compile("(?iu)\\b[0-9]+\\s*/\\s*[0-9]+\\s+(?:trecute|PASS|verzi|passed)\\b"
            + "|\\b[0-9]+\\s+(?:teste|tests)\\s*[,;:]?\\s*(?:trecute|verzi|passed|PASS\\b|fără\\s+(?:erori|omisiuni))");
    private static final Pattern VERSION_PATH = Pattern.compile("(?i)(?:release|artifacts/reports)/[0-9]+\\.[0-9]+\\.[0-9]+(?:/|\\b)");
    static List<String> violations(String text) {
        var result = new ArrayList<String>();
        if (RESULTS.matcher(text).find()) result.add("run-specific test outcome");
        if (VERSION_PATH.matcher(text).find()) result.add("concrete release destination");
        return List.copyOf(result);
    }
    private TestingStrategyRules() { }
}
