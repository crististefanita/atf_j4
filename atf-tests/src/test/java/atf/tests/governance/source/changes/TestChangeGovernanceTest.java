package atf.tests.governance.source.changes;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.documentation.release.handoff.ReleaseCommitEvidence;
import atf.tests.governance.support.GovernanceProject;
import atf.tests.testchange.TestChangeReview;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;

@AtfComponentTest
class TestChangeGovernanceTest {
    @Test void protectedChangesRequireOneCurrentControlOnTheReviewedGitBase() throws Exception {
        Path checkout = Path.of(System.getProperty(ReleaseCommitEvidence.GIT_ROOT,
                GovernanceProject.locateProjectRoot().toString()));
        var result = TestChangeReview.inspect(checkout, System.getProperty(TestChangeReview.BASE_PROPERTY),
                System.getenv(), LocalDate.now(ZoneOffset.UTC));
        AuditedAssertions.assertEquals(java.util.List.of(), result.diagnostics(),
                "Git review base " + result.base() + ", protected paths checked " + result.protectedPaths());
    }
}
