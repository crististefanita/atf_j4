package atf.tests.governance.documentation.links;

import atf.testkit.AtfComponentTest;
import atf.tests.documentation.DocumentationGovernance;
import atf.tests.documentation.DocumentationGovernanceSupport.Violation;
import atf.tests.governance.documentation.support.DocumentationAssertions;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static atf.tests.governance.support.GovernanceProject.locateProjectRoot;

@AtfComponentTest
public class DocumentationLinkGovernanceTest {
    @Test
    void governedMarkdownKeepsResolvableLocalDestinationsAndFragments() throws Exception {
        var violations = new ArrayList<Violation>();
        inspect(locateProjectRoot(), violations);
        DocumentationAssertions.assertNoViolations("documentation links and fragments", violations);
    }

    public static void inspect(Path root, List<Violation> violations) throws IOException {
        violations.addAll(DocumentationGovernance.scanProject(root, DocumentationGovernance.Rule.LINKS).violations());
    }
}
