package atf.tests.governance.documentation.release.rag.scope;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@AtfComponentTest
class ComponentEvidenceScopeComponentTest {
    @Test void nodeContractTestsKeepReuseClassificationInsideTheRealBuildBoundary() throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        var command = new ProcessBuilder("node", "--test",
                root.resolve("atf-tests/tools/prepare-component-reuse.test.mjs").toString())
                .directory(root.toFile()).redirectErrorStream(true).start();
        if (!command.waitFor(30, TimeUnit.SECONDS)) {
            command.destroyForcibly();
            throw new AssertionError("Component reuse contract tests exceeded 30 seconds");
        }
        String output = new String(command.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        AuditedAssertions.assertEquals(0, command.exitValue(), output);
        AuditedAssertions.assertTrue(output.contains("documentation and separate test-harness changes reuse unaffected Gradle game components"),
                "The Extent component group must include the precise dependency-scope regression");
    }
}
