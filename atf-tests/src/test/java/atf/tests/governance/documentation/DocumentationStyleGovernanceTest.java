package atf.tests.governance.documentation;

import atf.testkit.AtfComponentTest;
import atf.tests.documentation.DocumentationGovernance;
import atf.tests.governance.documentation.support.DocumentationAssertions;
import org.junit.jupiter.api.Test;

import static atf.tests.governance.support.GovernanceProject.locateProjectRoot;

@AtfComponentTest
public class DocumentationStyleGovernanceTest {
    @Test
    void everyOwnerReadmeUsesTheRendererNeutralAtfStyle() throws Exception {
        var result = DocumentationGovernance.scanProject(locateProjectRoot(), DocumentationGovernance.Rule.STYLE);
        DocumentationAssertions.assertNoViolations("README ATF style governance", result.violations());
    }
}
