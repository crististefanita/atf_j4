package atf.tests.governance.documentation;

import atf.testkit.AtfComponentTest;
import atf.tests.documentation.DocumentationGovernance;
import atf.tests.governance.documentation.support.DocumentationAssertions;
import org.junit.jupiter.api.Test;

import static atf.tests.governance.support.GovernanceProject.locateProjectRoot;

@AtfComponentTest
public class DocumentationDiagramGovernanceTest {
    @Test
    void nonRootReadmeDiagramsKeepTheDocumentedVisualContract() throws Exception {
        var result = DocumentationGovernance.scanProject(locateProjectRoot(), DocumentationGovernance.Rule.DIAGRAM);
        DocumentationAssertions.assertNoViolations("README diagram governance", result.violations());
    }
}
