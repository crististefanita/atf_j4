package atf.tests.execution.components;

import atf.android.process.CommandProcess;
import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@AtfComponentTest
class TestChangeBatchComponentTest {
    private static final String FIRST = "FIRST-CONTROL";
    private static final String SECOND = "SECOND-CONTROL";
    @TempDir Path scratch;

    @Test void publishesBothControlsAfterCheckingTheSnapshot() throws Exception {
        var result = execute("success");
        AuditedAssertions.assertEquals(0, result.exitCode(), diagnostic(result));
        AuditedAssertions.assertEquals(List.of(FIRST, SECOND, "snapshot"), calls());
        for (String id : List.of(FIRST, SECOND)) {
            Path folder = scratch.resolve("proofs").resolve(id);
            AuditedAssertions.assertEquals(id, Files.readString(folder.resolve("proof.json")));
            AuditedAssertions.assertEquals(id, Files.readString(folder.resolve("component-report.html")));
        }
        AuditedAssertions.assertFalse(Files.exists(scratch.resolve("proofs.pending")));
    }

    @Test void failedControlDoesNotStopTheRemainingControlsOrPublishTheirProofs() throws Exception {
        assertUnpublished(execute("control"), "TEST-CHANGE-BATCH-CONTROL", List.of(FIRST, SECOND, "THIRD-CONTROL"));
        AuditedAssertions.assertTrue(Files.isRegularFile(scratch.resolve("proofs.pending/THIRD-CONTROL/proof.pending.json")));
    }

    @Test void changedSourcePreventsPublicationOfPassingControls() throws Exception {
        assertUnpublished(execute("snapshot"), "QUALIFICATION-SNAPSHOT-CHANGED", List.of(FIRST, SECOND, "snapshot"));
    }

    @Test void changedReviewPreventsPublicationOfPassingControls() throws Exception {
        assertUnpublished(execute("review"), "TEST-CHANGE-BATCH-PLAN", List.of(FIRST, SECOND, "snapshot"));
    }

    private void assertUnpublished(CommandProcess.Output result, String code, List<String> expected) throws Exception {
        AuditedAssertions.assertEquals(1, result.exitCode(), diagnostic(result));
        AuditedAssertions.assertTrue(diagnostic(result).contains(code), diagnostic(result));
        AuditedAssertions.assertEquals(expected, calls());
        AuditedAssertions.assertTrue(Files.isRegularFile(scratch.resolve("proofs.pending/" + FIRST + "/proof.pending.json")));
        AuditedAssertions.assertFalse(Files.exists(scratch.resolve("proofs")));
    }

    private List<String> calls() throws Exception {
        return Files.readAllLines(scratch.resolve("calls.txt"));
    }

    private CommandProcess.Output execute(String mode) throws Exception {
        Path launcher = scratch.resolve("batch.ps1");
        Files.writeString(launcher, """
                $ErrorActionPreference = 'Stop'
                . $env:ATF_TEST_BATCH_HELPER
                $root = $env:ATF_TEST_BATCH_ROOT
                $mode = $env:ATF_TEST_BATCH_MODE
                $calls = Join-Path $root 'calls.txt'
                $source = Join-Path $root 'input.txt'
                [IO.File]::WriteAllText($source, 'pinned')
                Invoke-AtfTestChangeBatch -RunRoot $root -ReadPlan {
                    'FIRST-CONTROL'
                    if ($mode -ne 'review' -or !(Test-Path -LiteralPath $calls)) { 'SECOND-CONTROL' }
                    if ($mode -eq 'control') { 'THIRD-CONTROL' }
                } -RunControl {
                    param($id,$output)
                    Add-Content -LiteralPath $calls -Value $id
                    if ($id -eq 'SECOND-CONTROL' -and $mode -eq 'control') { return 7 }
                    [IO.Directory]::CreateDirectory($output) | Out-Null
                    [IO.File]::WriteAllText((Join-Path $output 'proof.pending.json'), $id)
                    [IO.File]::WriteAllText((Join-Path $output 'component-report.html'), $id)
                    if ($id -eq 'SECOND-CONTROL' -and $mode -eq 'snapshot') { [IO.File]::WriteAllText($source, 'changed') }
                    0
                } -CheckSnapshot {
                    Add-Content -LiteralPath $calls -Value 'snapshot'
                    if ([IO.File]::ReadAllText($source) -cne 'pinned') { throw 'QUALIFICATION-SNAPSHOT-CHANGED: input.txt' }
                }
                """);
        var helper = GovernanceProject.locateProjectRoot().resolve("scripts/AtfTestChangeBatch.ps1");
        return CommandProcess.execute(List.of("pwsh", "-NoProfile", "-NonInteractive", "-File", launcher.toString()),
                Map.of("ATF_TEST_BATCH_HELPER", helper.toString(), "ATF_TEST_BATCH_ROOT", scratch.toString(),
                        "ATF_TEST_BATCH_MODE", mode), Duration.ofSeconds(45), true);
    }

    private static String diagnostic(CommandProcess.Output result) {
        return new String(result.stdout(), StandardCharsets.UTF_8);
    }
}
