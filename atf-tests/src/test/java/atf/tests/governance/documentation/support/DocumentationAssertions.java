package atf.tests.governance.documentation.support;

import atf.testkit.assertions.AuditedAssertions;
import atf.tests.documentation.DocumentationGovernanceSupport.Violation;
import java.util.List;

public final class DocumentationAssertions {
    public static void assertNoViolations(String area, List<Violation> violations) {
        AuditedAssertions.assertTrue(violations.isEmpty(), () -> area + " failed with " + violations.size()
                + " actionable violation(s)." + String.join("", violations.stream().map(Violation::toString).toList()));
    }

    private DocumentationAssertions() { }
}
