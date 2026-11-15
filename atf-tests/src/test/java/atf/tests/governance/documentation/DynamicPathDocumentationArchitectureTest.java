package atf.tests.governance.documentation;

import atf.testkit.AtfComponentTest;
import atf.tests.documentation.DocumentationGovernance;
import atf.tests.governance.documentation.support.DocumentationAssertions;
import org.junit.jupiter.api.Test;

import static atf.tests.governance.support.GovernanceProject.locateProjectRoot;

@AtfComponentTest
public class DynamicPathDocumentationArchitectureTest {
    @Test
    void markdownAndReadmesDoNotPublishMachineSpecificAbsolutePaths() throws Exception {
        var result = DocumentationGovernance.scanProject(locateProjectRoot(), DocumentationGovernance.Rule.PATHS);
        DocumentationAssertions.assertNoViolations("documentation relocatability", result.violations());
    }

    @Test
    void markdownAndReadmesDoNotPublishConcreteGeneratedReportNames() throws Exception {
        var result = DocumentationGovernance.scanProject(locateProjectRoot(), DocumentationGovernance.Rule.ARTIFACT_NAMES);
        DocumentationAssertions.assertNoViolations("generated report names in documentation", result.violations());
    }
}
