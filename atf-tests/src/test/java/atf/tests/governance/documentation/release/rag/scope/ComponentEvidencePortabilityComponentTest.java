package atf.tests.governance.documentation.release.rag.scope;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@AtfComponentTest
class ComponentEvidencePortabilityComponentTest {
    @TempDir Path temporary;

    @Test void temporaryMachinePathsAreNormalizedAcrossCaseAndSlashVariants() throws Exception {
        Path source = Files.createDirectories(temporary.resolve("source tree with spaces"));
        Path stage = Files.createDirectories(temporary.resolve("build stage with spaces"));
        Path work = Files.createDirectories(temporary.resolve("work area with spaces"));
        Path evidence = Files.createDirectories(temporary.resolve("evidence run with spaces"));
        Path output = temporary.resolve("portable-diagnostic.txt");
        Path script = temporary.resolve("exercise-portability.ps1");
        Path helper = GovernanceProject.locateProjectRoot().resolve("scripts/AtfQualificationSnapshot.ps1");
        Files.writeString(script, """
                $ErrorActionPreference = 'Stop'
                . $env:ATF_PORTABILITY_HELPER
                $source = $env:ATF_PORTABILITY_SOURCE
                $text = @(
                    $source,
                    $source.Replace('\\', '/'),
                    $source.ToUpperInvariant(),
                    $env:ATF_PORTABILITY_STAGE,
                    $env:ATF_PORTABILITY_WORK,
                    $env:ATF_PORTABILITY_EVIDENCE
                ) -join '|'
                ConvertTo-AtfPortableDiagnostic $text $source $env:ATF_PORTABILITY_STAGE `
                    $env:ATF_PORTABILITY_WORK $env:ATF_PORTABILITY_EVIDENCE
                """);

        var processBuilder = new ProcessBuilder("pwsh", "-NoProfile", "-NonInteractive", "-File", script.toString())
                .redirectErrorStream(true).redirectOutput(output.toFile());
        var environment = processBuilder.environment();
        environment.put("ATF_PORTABILITY_HELPER", helper.toString());
        environment.put("ATF_PORTABILITY_SOURCE", source.toString());
        environment.put("ATF_PORTABILITY_STAGE", stage.toString());
        environment.put("ATF_PORTABILITY_WORK", work.toString());
        environment.put("ATF_PORTABILITY_EVIDENCE", evidence.toString());
        var process = processBuilder.start();
        try {
            AuditedAssertions.assertTrue(process.waitFor(20, TimeUnit.SECONDS), "Portable diagnostic helper timed out");
            AuditedAssertions.assertEquals(0, process.exitValue(), Files.readString(output));
            AuditedAssertions.assertEquals("<SOURCE>|<SOURCE>|<SOURCE>|<STAGE>|<WORK>|<EVIDENCE>",
                    Files.readString(output).trim());
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor();
            }
        }
    }

    @Test void evidenceAndSdkRootsResolveFromMachineConfiguration() throws Exception {
        Path evidence = Files.createDirectories(temporary.resolve("configured evidence"));
        Path sdk = Files.createDirectories(temporary.resolve("configured android sdk"));
        Path helper = GovernanceProject.locateProjectRoot().resolve("scripts/AtfLocalPaths.ps1");
        Path script = temporary.resolve("resolve-local-paths.ps1");
        Path output = temporary.resolve("resolved-local-paths.txt");
        Files.writeString(script, """
                $ErrorActionPreference = 'Stop'
                . $env:ATF_LOCAL_PATH_HELPER
                Get-AtfEvidenceRoot
                Get-AtfAndroidSdkRoot
                """);

        var processBuilder = new ProcessBuilder("pwsh", "-NoProfile", "-NonInteractive", "-File", script.toString())
                .redirectErrorStream(true).redirectOutput(output.toFile());
        var environment = processBuilder.environment();
        environment.put("ATF_LOCAL_PATH_HELPER", helper.toString());
        environment.put("WHIST_EVIDENCE_ROOT", evidence.toString());
        environment.put("ANDROID_SDK_ROOT", sdk.toString());
        environment.remove("ANDROID_HOME");
        var process = processBuilder.start();
        try {
            AuditedAssertions.assertTrue(process.waitFor(20, TimeUnit.SECONDS), "Configured path resolution timed out");
            AuditedAssertions.assertEquals(0, process.exitValue(), Files.readString(output));
            AuditedAssertions.assertEquals(evidence + System.lineSeparator() + sdk,
                    Files.readString(output).trim());
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor();
            }
        }
    }

    @Test void androidSdkDoesNotSilentlyFallBackWhenNoMachinePathIsConfigured() throws Exception {
        Path helper = GovernanceProject.locateProjectRoot().resolve("scripts/AtfLocalPaths.ps1");
        Path script = temporary.resolve("require-android-sdk.ps1");
        Path output = temporary.resolve("missing-android-sdk.txt");
        Files.writeString(script, ". $env:ATF_LOCAL_PATH_HELPER; Get-AtfAndroidSdkRoot");
        var processBuilder = new ProcessBuilder("pwsh", "-NoProfile", "-NonInteractive", "-File", script.toString())
                .redirectErrorStream(true).redirectOutput(output.toFile());
        processBuilder.environment().put("ATF_LOCAL_PATH_HELPER", helper.toString());
        processBuilder.environment().remove("ANDROID_SDK_ROOT");
        processBuilder.environment().remove("ANDROID_HOME");
        var process = processBuilder.start();
        try {
            AuditedAssertions.assertTrue(process.waitFor(20, TimeUnit.SECONDS), "Missing SDK check timed out");
            AuditedAssertions.assertNotEquals(0, process.exitValue(), Files.readString(output));
            AuditedAssertions.assertTrue(Files.readString(output).contains("Set ANDROID_SDK_ROOT or ANDROID_HOME"),
                    "The failure identifies the machine setting instead of a hard-coded path");
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor();
            }
        }
    }

    @Test void relativeEvidenceRootIsRejectedInsteadOfResolvingAgainstTheCheckout() throws Exception {
        Path helper = GovernanceProject.locateProjectRoot().resolve("scripts/AtfLocalPaths.ps1");
        Path script = temporary.resolve("reject-relative-evidence-root.ps1");
        Path output = temporary.resolve("relative-evidence-root.txt");
        Files.writeString(script, ". $env:ATF_LOCAL_PATH_HELPER; Get-AtfEvidenceRoot");
        var processBuilder = new ProcessBuilder("pwsh", "-NoProfile", "-NonInteractive", "-File", script.toString())
                .redirectErrorStream(true).redirectOutput(output.toFile());
        processBuilder.environment().put("ATF_LOCAL_PATH_HELPER", helper.toString());
        processBuilder.environment().put("WHIST_EVIDENCE_ROOT", ".\\generated-evidence");
        var process = processBuilder.start();
        try {
            AuditedAssertions.assertTrue(process.waitFor(20, TimeUnit.SECONDS), "Relative evidence root check timed out");
            AuditedAssertions.assertNotEquals(0, process.exitValue(), Files.readString(output));
            AuditedAssertions.assertTrue(Files.readString(output).contains("WHIST_EVIDENCE_ROOT must be an absolute"),
                    "Relative paths cannot silently point into the current checkout");
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor();
            }
        }
    }
}
