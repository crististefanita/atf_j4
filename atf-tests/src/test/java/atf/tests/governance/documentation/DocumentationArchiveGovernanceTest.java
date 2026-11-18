package atf.tests.governance.documentation;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@AtfComponentTest
class DocumentationArchiveGovernanceTest {
    @Test void translatedReleaseGuidesRetainTheOriginalDocumentsAtTheirPinnedGitCommit() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        var gitRoot = System.getProperty("atf.governance.gitRoot", root.toString());
        var process = new ProcessBuilder("node", root.resolve("atf-tests/tools/release-documentation-provenance.mjs").toString(),
                root.toString(), gitRoot).redirectErrorStream(true).start();
        boolean finished = process.waitFor(45, TimeUnit.SECONDS);
        if (!finished) process.destroyForcibly();
        AuditedAssertions.assertTrue(finished, "Release documentation provenance must finish within its bounded budget");
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        AuditedAssertions.assertEquals(0, process.exitValue(), output);
        AuditedAssertions.assertTrue(output.contains("\"status\":\"PASS\""), output);
    }
}
