package atf.tests.governance.documentation.feedback;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import atf.tests.reporting.GameReportAudit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

/** Keeps the prioritized feedback index traceable, scored consistently and locally navigable. */
@AtfComponentTest
final class FeedbackIndexGovernanceTest {
    private static final Pattern LOCAL_LINK = Pattern.compile("\\[[^]]+]\\(([^)]+)\\)");

    @RegisterExtension
    static final GameReportAudit AUDIT = new GameReportAudit();

    @TempDir
    Path temporary;

    @Test
    void projectFeedbackIndexHasUniqueOrderedScoresAndExistingOwnerLinks() throws Exception {
        Path root = GovernanceProject.locateProjectRoot();
        Path index = root.resolve("docs/feedback/README.md");

        List<String> violations = validate(Files.readString(index), root.resolve("docs/feedback"), root);

        AuditedAssertions.assertTrue(violations.isEmpty(), String.join("\n", violations));
    }

    @Test
    void rejectsDuplicateIdsInvalidScoresWrongOrderAndBrokenOwnerLinks() throws Exception {
        Path root = temporary.resolve("project");
        Path feedback = root.resolve("docs/feedback");
        Files.createDirectories(feedback);
        Files.writeString(feedback.resolve("ATF_Owner.md"), "# Owner\n");
        String index = table(
                row("ACH-01", "P1", "90", "[Owner](ATF_Owner.md)"),
                row("ACH-01", "P1", "101", "[Owner](missing.md)"),
                row("AI-01", "P2", "95", "[Owner](ATF_Owner.md)"));

        List<String> violations = validate(index, feedback, root);

        AuditedAssertions.assertTrue(violations.stream().anyMatch(value -> value.contains("duplicate ID ACH-01")));
        AuditedAssertions.assertTrue(violations.stream().anyMatch(value -> value.contains("score must be between 1 and 100")));
        AuditedAssertions.assertTrue(violations.stream().anyMatch(value -> value.contains("scores must be in descending order")));
        AuditedAssertions.assertTrue(violations.stream().anyMatch(value -> value.contains("owner link does not resolve")));
    }

    @Test
    void requiresARecognizableTodoTableAndAResolvableLocalOwnerLink() throws Exception {
        Path root = temporary.resolve("empty-project");
        Path feedback = root.resolve("docs/feedback");
        Files.createDirectories(feedback);

        List<String> missingTable = validate("# Feedback\n", feedback, root);
        List<String> externalLink = validate(table(row("RAG-01", "P1", "98", "[Owner](https://example.invalid)")), feedback, root);

        AuditedAssertions.assertTrue(missingTable.stream().anyMatch(value -> value.contains("ToDo table is missing")));
        AuditedAssertions.assertTrue(externalLink.stream().anyMatch(value -> value.contains("owner link must be a local project document")));
    }

    private static List<String> validate(String markdown, Path feedbackFolder, Path root) {
        String[] lines = markdown.replace("\r\n", "\n").split("\n");
        List<String> violations = new ArrayList<>();
        boolean foundHeader = false;
        boolean foundRows = false;
        Integer previousScore = null;
        java.util.Set<String> ids = new java.util.HashSet<>();

        for (String line : lines) {
            if (!line.trim().startsWith("|")) {
                if (foundHeader && foundRows) break;
                continue;
            }
            String[] cells = line.trim().split("\\|", -1);
            if (cells.length < 6) continue;
            String idHeader = cells[1].trim();
            if (!foundHeader) {
                if (idHeader.equals("ID") && cells[3].trim().equals("Scor")) foundHeader = true;
                continue;
            }
            String id = cells[1].trim();
            if (id.matches(":?-{3,}:?")) continue;
            foundRows = true;
            if (!id.matches("[A-Z][A-Z0-9]*-[0-9]{2}")) {
                violations.add("feedback row has invalid ID: " + id);
                continue;
            }
            if (!ids.add(id)) violations.add("feedback row has duplicate ID " + id);
            String scoreText = cells[3].trim();
            int score;
            try {
                score = Integer.parseInt(scoreText);
            } catch (NumberFormatException error) {
                violations.add("feedback row " + id + " has a non-integer score");
                continue;
            }
            if (score < 1 || score > 100) violations.add("feedback row " + id + " score must be between 1 and 100");
            if (previousScore != null && score > previousScore) {
                violations.add("feedback scores must be in descending order at " + id);
            }
            previousScore = score;

            Matcher link = LOCAL_LINK.matcher(cells[4]);
            if (!link.find()) {
                violations.add("feedback row " + id + " has no owner link");
                continue;
            }
            String target = link.group(1).trim();
            if (target.matches("(?i)^(https?:|mailto:).*")) {
                violations.add("feedback row " + id + " owner link must be a local project document");
                continue;
            }
            String pathPart = target.split("#", 2)[0];
            Path resolved = feedbackFolder.resolve(pathPart).normalize();
            if (!resolved.startsWith(root.normalize()) || !Files.isRegularFile(resolved)) {
                violations.add("feedback row " + id + " owner link does not resolve: " + target);
            }
        }
        if (!foundHeader || !foundRows) violations.add("feedback ToDo table is missing or empty");
        return violations;
    }

    private static String table(String... rows) {
        return "| ID | Prioritate | Scor | ToDo rămas și criteriul scurt de închidere |\n"
                + "|---|---:|---:|---|\n" + String.join("\n", rows) + "\n";
    }

    private static String row(String id, String priority, String score, String ownerLink) {
        return "| " + id + " | " + priority + " | " + score + " | Criteriu " + ownerLink + " |";
    }
}
