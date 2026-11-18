package atf.tests.governance.documentation.language;

import atf.testkit.AtfComponentTest;
import atf.tests.documentation.DocumentationGovernance;
import atf.tests.governance.support.GovernanceProject;
import java.util.List;
import org.junit.jupiter.api.Test;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;

@AtfComponentTest
class DocumentationTerminologyGovernanceTest {
    @Test void englishOperationalTermsStayConsistentWithReviewedVocabulary() throws Exception {
        var findings = DocumentationGovernance.scanProject(GovernanceProject.locateProjectRoot(),
                DocumentationGovernance.Rule.TERMINOLOGY).violations();
        assertEquals(List.of(), findings, findings.toString());
    }
}
