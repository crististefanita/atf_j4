package atf.tests.documentation.policy;

import atf.tests.documentation.DocumentationGovernanceSupport.Violation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static atf.tests.documentation.DocumentationGovernanceSupport.violation;

/** Editorial structure and owner checks for the project's active feedback register. */
public final class FeedbackIndexRules {
    private static final Pattern LOCAL_LINK = Pattern.compile("\\[[^]]+]\\(([^)]+)\\)");
    private static final Pattern PRIORITY = Pattern.compile("^\\| ([A-Z]+-[0-9]{2}) \\| P[12] \\| ([0-9]{1,3}) \\| .+ \\|$");

    public static void inspectRows(List<String> lines, Path file, Path root, List<Violation> violations) {
        Path feedbackFolder = file.getParent();
        boolean foundHeader = false;
        boolean foundRows = false;
        Integer previousScore = null;
        java.util.Set<String> ids = new java.util.HashSet<>();

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            int number = index + 1;
            if (!line.trim().startsWith("|")) {
                if (foundHeader && foundRows) break;
                continue;
            }
            String[] cells = line.trim().split("\\|", -1);
            if (cells.length < 6) continue;
            String idHeader = cells[1].trim();
            if (!foundHeader) {
                if (idHeader.equals("ID") && List.of("Score", "Scor").contains(cells[3].trim())) foundHeader = true;
                continue;
            }
            String id = cells[1].trim();
            if (id.matches(":?-{3,}:?")) continue;
            foundRows = true;
            if (!id.matches("[A-Z][A-Z0-9]*-[0-9]{2}")) {
                row(violations, root, file, number, "feedback row has invalid ID: " + id);
                continue;
            }
            if (!ids.add(id)) row(violations, root, file, number, "feedback row has duplicate ID " + id);
            String scoreText = cells[3].trim();
            int score;
            try {
                score = Integer.parseInt(scoreText);
            } catch (NumberFormatException error) {
                row(violations, root, file, number, "feedback row " + id + " has a non-integer score");
                continue;
            }
            if (score < 1 || score > 100) row(violations, root, file, number, "feedback row " + id + " score must be between 1 and 100");
            if (previousScore != null && score > previousScore) {
                row(violations, root, file, number, "feedback scores must be in descending order at " + id);
            }
            previousScore = score;

            Matcher link = LOCAL_LINK.matcher(cells[4]);
            if (!link.find()) {
                row(violations, root, file, number, "feedback row " + id + " has no owner link");
                continue;
            }
            String target = link.group(1).trim();
            if (target.matches("(?i)^(https?:|mailto:).*")) {
                row(violations, root, file, number, "feedback row " + id + " owner link must be a local project document");
                continue;
            }
            String pathPart = target.split("#", 2)[0];
            Path resolved = feedbackFolder.resolve(pathPart).normalize();
            if (!resolved.startsWith(root.normalize()) || !Files.isRegularFile(resolved)) {
                row(violations, root, file, number, "feedback row " + id + " owner link does not resolve: " + target);
            }
        }
        if (!foundHeader || !foundRows) row(violations, root, file, 1, "feedback ToDo table is missing or empty");
    }

    public static void inspectPriorities(List<String> lines, Path file, Path root, List<Violation> violations) {
        int heading = lines.indexOf("## Prioritized ToDos");
        if (heading < 0) heading = lines.indexOf("## ToDo-uri prioritizate");
        if (heading < 0) {
            row(violations, root, file, 1, "the prioritized feedback heading is missing");
            return;
        }
        var ids = new HashSet<String>();
        int previous = 100;
        int rows = 0;
        for (int index = heading + 1; index < lines.size() && !lines.get(index).startsWith("## "); index++) {
            var match = PRIORITY.matcher(lines.get(index));
            if (!match.matches()) continue;
            int score = Integer.parseInt(match.group(2));
            String id = match.group(1);
            if (!ids.add(id)) row(violations, root, file, index + 1, "duplicate feedback priority ID: " + id);
            if (score < 1 || score > 100)
                row(violations, root, file, index + 1, "priority score is outside 1–100: " + score);
            if (score > previous)
                row(violations, root, file, index + 1, "priority scores must be sorted highest first");
            previous = score;
            rows++;
        }
        if (rows < 10) row(violations, root, file, heading + 1, "the priority register omits the expected open work");
    }

    private static void row(List<Violation> violations, Path root, Path file, int line, String finding) {
        violations.add(violation("DOC-FEEDBACK-INDEX", root, file, line, finding,
                "feedback lists uniquely identified work with an ordered score and a local owner",
                "correct the priority or link the row to the active owner document"));
    }

    private FeedbackIndexRules() { }
}
