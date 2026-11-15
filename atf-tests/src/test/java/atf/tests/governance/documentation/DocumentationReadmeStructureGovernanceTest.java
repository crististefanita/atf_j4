package atf.tests.governance.documentation;

import atf.testkit.AtfComponentTest;
import atf.tests.documentation.DocumentationGovernanceSupport;
import atf.tests.documentation.DocumentationGovernance;
import atf.tests.governance.documentation.support.DocumentationAssertions;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static atf.tests.governance.support.GovernanceProject.locateProjectRoot;

@AtfComponentTest
public class DocumentationReadmeStructureGovernanceTest {
    @Test
    void ownerDocumentsKeepAReadableStableShape() throws Exception {
        var result = DocumentationGovernance.scanProject(locateProjectRoot(), DocumentationGovernance.Rule.STRUCTURE);
        DocumentationAssertions.assertNoViolations("README structure governance", result.violations());
    }

    @Test
    void declaredReleaseReportIndexKeepsItsOriginalShape(@org.junit.jupiter.api.io.TempDir Path root) throws Exception {
        Path release = Files.createDirectories(root.resolve("release/1.2.3/reports/component"));
        Path evidence = Files.writeString(release.resolve("README.md"), "# Component report index\n");
        Path manifest = release.getParent().getParent().resolve("release-validation.json");
        Files.writeString(manifest, """
                {"integrity":{"files":[{"path":"reports/component/README.md"}]}}
                """);
        var violations = new ArrayList<DocumentationGovernanceSupport.Violation>();
        violations.addAll(DocumentationGovernance.scanFile(root, "release/1.2.3/reports/component/README.md",
                DocumentationGovernance.Rule.STRUCTURE).violations());
        DocumentationAssertions.assertNoViolations("Release evidence index", violations);

        Files.writeString(release.getParent().getParent().resolve("README.md"), "# Release owner\n");
        violations.addAll(DocumentationGovernance.scanFile(root, "release/1.2.3/README.md",
                DocumentationGovernance.Rule.STRUCTURE).violations());
        Files.writeString(manifest, """
                {"integrity":{"files":[]}}
                """);
        violations.addAll(DocumentationGovernance.scanFile(root, "release/1.2.3/reports/component/README.md",
                DocumentationGovernance.Rule.STRUCTURE).violations());
        atf.testkit.assertions.AuditedAssertions.assertEquals(
                List.of("DOC-README-OVERALL", "DOC-README-OVERALL"),
                violations.stream().map(DocumentationGovernanceSupport.Violation::code).toList());
    }
}
