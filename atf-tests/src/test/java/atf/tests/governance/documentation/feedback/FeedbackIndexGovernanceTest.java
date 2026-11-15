package atf.tests.governance.documentation.feedback;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import atf.tests.documentation.DocumentationGovernance;
import atf.tests.documentation.DocumentationGovernanceSupport.Violation;
import atf.tests.documentation.policy.FeedbackIndexRules;
import atf.tests.reporting.GameReportAudit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

/** Keeps the prioritized feedback index traceable, scored consistently and locally navigable. */
@AtfComponentTest
final class FeedbackIndexGovernanceTest {

    @RegisterExtension
    static final GameReportAudit AUDIT = new GameReportAudit();

    @TempDir
    Path temporary;

    @Test
    void projectFeedbackIndexHasUniqueOrderedScoresAndExistingOwnerLinks() throws Exception {
        Path root = GovernanceProject.locateProjectRoot();
        var result = DocumentationGovernance.scanFile(root, "docs/feedback/README.md", DocumentationGovernance.Rule.FEEDBACK_ROWS);
        AuditedAssertions.assertTrue(result.violations().isEmpty(), result.violations().toString());
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
        var violations = new ArrayList<Violation>();
        FeedbackIndexRules.inspectRows(markdown.lines().toList(), feedbackFolder.resolve("README.md"), root, violations);
        return violations.stream().map(Violation::finding).toList();
    }

    private static String table(String... rows) {
        return "| ID | Prioritate | Scor | ToDo rămas și criteriul scurt de închidere |\n"
                + "|---|---:|---:|---|\n" + String.join("\n", rows) + "\n";
    }

    private static String row(String id, String priority, String score, String ownerLink) {
        return "| " + id + " | " + priority + " | " + score + " | Criteriu " + ownerLink + " |";
    }
}
