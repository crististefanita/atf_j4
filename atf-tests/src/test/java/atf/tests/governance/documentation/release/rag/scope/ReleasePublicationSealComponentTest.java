package atf.tests.governance.documentation.release.rag.scope;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import org.junit.jupiter.api.Test;
import java.time.Duration;

import static atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts.runContracts;

@AtfComponentTest
class ReleasePublicationSealComponentTest {
    @Test void publicationSealsTheManifestBeforeObservingGithub() throws Exception {
        String output = runContracts(Duration.ofMinutes(2), "github-retention-seal.test.mjs");
        AuditedAssertions.assertTrue(output.contains("publication is sealed in Git before inventory and can resume without rebuilding artifacts"),
                "The real collector verifies published manifest bytes before inventory and retains immutable asset identities");
        AuditedAssertions.assertTrue(output.contains("uncommitted or unpushed manifest edits stop before any GitHub request"),
                "A local manifest change must fail before remote work");
        AuditedAssertions.assertTrue(output.contains("a changed local manifest during observation cannot become accepted evidence"),
                "The seal is checked again before an observation is returned");
    }

    @Test void unqualifiedLocalTagsStopBeforeSourceReconstruction() throws Exception {
        String output = runContracts(Duration.ofMinutes(2), "release-git-snapshot.test.mjs");
        AuditedAssertions.assertTrue(output.contains("a remote-only tag blocks before source reconstruction and resumes after local resolution"),
                "A remote reference cannot silently replace the local qualified tag");
        AuditedAssertions.assertTrue(output.contains("a tag on another commit is refused before reconstructing an invalid source"),
                "Tag mismatch is a preflight failure, not a late reconstruction failure");
        AuditedAssertions.assertTrue(output.contains("annotated qualified tags resolve to the exact source commit"),
                "Both lightweight and annotated tags must resolve to the manifest source identity");
        AuditedAssertions.assertTrue(output.contains("binds a mixed EOL recipe to the exact commit")
                        && output.contains("cleanup refuses a stale verdict or changed published report"),
                "This single reconstruction owner retains the existing source and published-payload regressions");
    }
}
