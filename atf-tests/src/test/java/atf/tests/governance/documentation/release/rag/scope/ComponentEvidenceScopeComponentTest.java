package atf.tests.governance.documentation.release.rag.scope;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

import static atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts.*;

@AtfComponentTest
class ComponentEvidenceScopeComponentTest {
    private static final Duration PROCESS_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration PUBLICATION_TIMEOUT = Duration.ofMinutes(2);

    @Test void disposablePackageRegistrationReconcilesObservedBytesAndPreservesTheOriginalCause() throws Exception {
        String output = runContracts(PUBLICATION_TIMEOUT, "android-package-registration.test.mjs", "android-user-readiness.test.mjs", "documentation-editions.test.mjs");
        AuditedAssertions.assertTrue(output.contains("7 controlled package-registration outcomes"),
                "The isolated-user gate covers completion after timeout, missing registration and wrong APK bytes.");
        AuditedAssertions.assertTrue(output.contains("8 controlled Android user readiness outcomes"),
                "A hidden keyguard does not qualify BOOTING or credential-locked storage as app readiness.");
        AuditedAssertions.assertTrue(output.contains("an unchanged layout cannot hide unreviewed Romanian or English prose"),
                "A pinned bilingual pair remains immutable and the current pair still requires review after edits.");
    }

    @Test void nodeContractTestsKeepReuseClassificationInsideTheRealBuildBoundary() throws Exception {
        String output = runContracts(PROCESS_TIMEOUT, "release-test-scope.test.mjs", "github-release-retention.test.mjs");
        AuditedAssertions.assertTrue(output.contains("Gradle component checkpoint and audit changes keep qualification offline"),
                "A change limited to Gradle reporting must select offline qualification.");
        AuditedAssertions.assertTrue(output.contains("a bidding change requires the native bidding and AutoGame selections"),
                "The Extent component group must include the executable RAG selection contract");
        AuditedAssertions.assertTrue(output.contains("a changed app package reruns its declared owner while unchanged suites reuse hash-bound evidence"),
                "Ownership follows the declared app collection and retains evidence only for unaffected suites.");
        AuditedAssertions.assertTrue(output.contains("package ownership covers product, score and test sources and rejects an unmapped package conservatively"),
                "Product and test packages share one configured owner map, while unmapped packages fail closed.");
        AuditedAssertions.assertTrue(output.contains("shared Gradle dependencies invalidate their consumers and unknown paths fail closed"),
                "Shared dependency edges and unclassified paths cannot silently reuse evidence.");
        AuditedAssertions.assertTrue(output.contains("the version metadata exemption must reproduce the exact baseline build bytes"),
                "A release version-only diff is exempt only when the prior build bytes reconstruct exactly.");
        AuditedAssertions.assertTrue(output.contains("qualification path and UI export helpers require offline governance without native replay"),
                "Qualification-only helpers must not turn a documentation and governance release into seven Android E2E flows.");
    }

    @Test void sharedDependenciesAndUnknownInputsCannotReuseTheirConsumers() throws Exception {
        String output = runContracts(PROCESS_TIMEOUT, "release-test-scope.test.mjs");
        AuditedAssertions.assertTrue(output.contains("shared Gradle dependencies invalidate their consumers and unknown paths fail closed"),
                "Shared dependency and unowned path changes rerun all affected component selections.");
    }

    @Test void componentResumeImportsOnlyVerifiedEvidenceFromTheInterruptedSnapshot() throws Exception {
        String output = runContracts(PUBLICATION_TIMEOUT, "prepare-component-reuse.test.mjs", "qualification-temp-root.test.mjs");
        AuditedAssertions.assertTrue(output.contains("documentation and separate test-harness changes reuse unaffected Gradle game components"),
                "The Extent component group must include the precise dependency-scope regression");
        AuditedAssertions.assertTrue(output.contains("after interruption only the completed successful suites are imported"),
                "A partial run may contribute only complete passing suites.");
        AuditedAssertions.assertTrue(output.contains("a Gradle checkpoint skips the validated task and reruns failed or interrupted siblings"),
                "Gradle can resume from the last verified task without replaying its passing receipt.");
        AuditedAssertions.assertTrue(output.contains("the interrupted run cannot be resumed with changed source or release evidence"),
                "A changed snapshot invalidates resumption.");
        AuditedAssertions.assertTrue(output.contains("durable Maven preparation keeps compilation when classpath is interrupted and rejects changed classes"),
                "Maven preparation resumes from the verified compilation boundary.");
        AuditedAssertions.assertTrue(output.contains("Web-only artifacts preserve independent native and API evidence while critical or unknown changes still rerun"),
                "Browser delivery has no native/API compile edge; own sources, shared JVM libraries and unknown inputs still require execution.");
        AuditedAssertions.assertTrue(output.contains("the current published release admits reuse for an exact Android version-only build edit"),
                "Reuse checks the verified release baseline and exact app build reconstruction before skipping a suite.");
        AuditedAssertions.assertTrue(output.contains("a build configuration edit cannot borrow the version-only reuse exception"),
                "Other Android build-file changes select the affected app collections.");
        AuditedAssertions.assertTrue(output.contains("an explicit qualification temp root selects that machine path while validating OS temp variables too"),
                "External staging roots are configurable and still validated before work begins.");
    }

    @Test void githubPublicationResumesByIdAndVerifiesDownloadsBeforeRotation() throws Exception {
        String output = runContracts(PUBLICATION_TIMEOUT, "publish-github-release.test.mjs");
        AuditedAssertions.assertTrue(output.contains("lost create response recovers from authenticated pages"),
                "The component report covers draft recovery without tag lookup.");
        AuditedAssertions.assertTrue(output.contains("rotation denies a stale README, missing download, wrong bytes or changed main"),
                "The component report covers the download gate before rotation.");
        AuditedAssertions.assertTrue(output.contains("new release notes require a short summary and separate change bullets before any draft"),
                "Editorial preflight rejects dense or missing change lists before any GitHub mutation.");
    }

    @Test void localReleaseSyncKeepsOnlyQualifiedPublishedBytesAndRejectsLocalEdits() throws Exception {
        String output = runContracts(PUBLICATION_TIMEOUT, "local-release-sync.test.mjs");
        AuditedAssertions.assertTrue(output.contains("materializes only the current published release and preserves bytes"),
                "The local release copy preserves the current manifest and payload.");
        AuditedAssertions.assertTrue(output.contains("refuses unpublished proof, a modified candidate, and unknown local data"),
                "The local release copy rejects unqualified inputs and unrelated files.");
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

}
