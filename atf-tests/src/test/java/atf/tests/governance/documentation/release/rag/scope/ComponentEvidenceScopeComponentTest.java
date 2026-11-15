package atf.tests.governance.documentation.release.rag.scope;

import atf.android.process.CommandProcess;
import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@AtfComponentTest
class ComponentEvidenceScopeComponentTest {
    private static final Duration PROCESS_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration PUBLICATION_TIMEOUT = Duration.ofMinutes(2);

    @Test void nodeContractTestsKeepReuseClassificationInsideTheRealBuildBoundary() throws Exception {
        String output = runContracts(PROCESS_TIMEOUT, "prepare-component-reuse.test.mjs", "release-test-scope.test.mjs",
                "github-release-retention.test.mjs");
        AuditedAssertions.assertTrue(output.contains("documentation and separate test-harness changes reuse unaffected Gradle game components"),
                "The Extent component group must include the precise dependency-scope regression");
        AuditedAssertions.assertTrue(output.contains("a bidding change requires the native bidding and AutoGame selections"),
                "The Extent component group must include the executable RAG selection contract");
    }

    @Test void githubPublicationResumesByIdAndVerifiesDownloadsBeforeRotation() throws Exception {
        String output = runContracts(PUBLICATION_TIMEOUT, "publish-github-release.test.mjs");
        AuditedAssertions.assertTrue(output.contains("lost create response recovers from authenticated pages"),
                "The component report covers draft recovery without tag lookup.");
        AuditedAssertions.assertTrue(output.contains("rotation denies a stale README, missing download, wrong bytes or changed main"),
                "The component report covers the download gate before rotation.");
    }

    @Test void verboseNodeRunDrainsBothStreamsFromTheSelectedDirectory() throws Exception {
        Path directory = GovernanceProject.locateProjectRoot().resolve("atf-tests");
        String program = "process.stdout.write('x'.repeat(128 * 1024), "
                + "() => process.stderr.write('y'.repeat(128 * 1024), "
                + "() => process.stdout.write('\\n' + process.cwd())));";
        String output = runNode(List.of("node", "-e", program), directory, PROCESS_TIMEOUT);
        int separator = output.lastIndexOf('\n');
        AuditedAssertions.assertEquals(2 * 128 * 1024, separator, "The child can write beyond both pipe capacities");
        AuditedAssertions.assertEquals(directory.toRealPath(), Path.of(output.substring(separator + 1)).toRealPath(),
                "The probe starts in the requested checkout directory");
    }

    @Test void nonZeroNodeExitKeepsTheChildDiagnostic() throws Exception {
        Path directory = GovernanceProject.locateProjectRoot();
        var failure = AuditedAssertions.assertThrows(AssertionError.class, () -> runNode(List.of("node", "-e",
                "process.stderr.write('controlled Node failure'); process.exitCode = 7;"), directory, PROCESS_TIMEOUT));
        AuditedAssertions.assertTrue(failure.getMessage().contains("controlled Node failure"),
                "The failed contract preserves its original stderr");
    }

    private static String runContracts(Duration timeout, String... scripts) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        var args = new ArrayList<>(List.of("node", "--test"));
        for (String script : scripts) args.add(root.resolve("atf-tests/tools").resolve(script).toString());
        return runNode(args, root, timeout);
    }

    private static String runNode(List<String> args, Path directory, Duration timeout) throws Exception {
        var result = CommandProcess.execute(args, directory, Map.of(), timeout, true);
        String output = new String(result.stdout(), StandardCharsets.UTF_8);
        AuditedAssertions.assertEquals(0, result.exitCode(),
                result.exitCode() == 0 ? "The Node child completed successfully" : output);
        return output;
    }
}
