package atf.tests.governance.documentation.release.rag.scope;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@AtfComponentTest
class ComponentEvidenceScopeComponentTest {
    @Test void nodeContractTestsKeepReuseClassificationInsideTheRealBuildBoundary() throws Exception {
        String output = runContracts("prepare-component-reuse.test.mjs", "release-test-scope.test.mjs",
                "github-release-retention.test.mjs");
        AuditedAssertions.assertTrue(output.contains("documentation and separate test-harness changes reuse unaffected Gradle game components"),
                "The Extent component group must include the precise dependency-scope regression");
        AuditedAssertions.assertTrue(output.contains("a bidding change requires the native bidding and AutoGame selections"),
                "The Extent component group must include the executable RAG selection contract");
    }

    @Test void githubPublicationResumesByIdAndVerifiesDownloadsBeforeRotation() throws Exception {
        String output = runContracts("publish-github-release.test.mjs");
        AuditedAssertions.assertTrue(output.contains("lost create response recovers from authenticated pages"),
                "The component report covers draft recovery without tag lookup.");
        AuditedAssertions.assertTrue(output.contains("rotation denies a stale README, missing download, wrong bytes or changed main"),
                "The component report covers the download gate before rotation.");
    }

    private static String runContracts(String... scripts) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        var args = new ArrayList<>(List.of("node", "--test"));
        for (String script : scripts) args.add(root.resolve("atf-tests/tools").resolve(script).toString());
        var command = new ProcessBuilder(args).directory(root.toFile()).redirectErrorStream(true).start();
        if (!command.waitFor(30, TimeUnit.SECONDS)) {
            command.destroyForcibly();
            throw new AssertionError("Node contract tests exceeded 30 seconds");
        }
        String output = new String(command.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        AuditedAssertions.assertEquals(0, command.exitValue(), output);
        return output;
    }
}
