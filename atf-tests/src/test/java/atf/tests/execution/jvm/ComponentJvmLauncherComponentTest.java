package atf.tests.execution.jvm;

import atf.android.process.CommandProcess;
import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@AtfComponentTest
class ComponentJvmLauncherComponentTest {
    @TempDir Path scratch;

    @Test void scopeBelongsToTheSuiteAndChildBytesStreamsAndExitRemainOriginal() throws Exception {
        Path output = scratch.resolve("probe.json");
        var run = probe("owner", output);
        AuditedAssertions.assertEquals(0, run.exitCode(), diagnostic(run));
        var observed = new ObjectMapper().readTree(output.toFile());
        AuditedAssertions.assertEquals("suite", observed.required("scope").asText());
        AuditedAssertions.assertEquals("8192", observed.required("partCharacters").asText());
        AuditedAssertions.assertEquals(scratch.resolve("checkout with spaces").toString(), observed.required("gitRoot").asText());
        AuditedAssertions.assertEquals(7, observed.required("childExit").asInt());
        AuditedAssertions.assertArrayEquals(new byte[] {(byte) 0xff, 0, 1, 13, 10},
                Base64.getDecoder().decode(observed.required("stdoutBase64").asText()));
        AuditedAssertions.assertEquals("separate-error", observed.required("stderr").asText());
    }

    @Test void theSuiteNonzeroExitIsNotConvertedToSuccess() throws Exception {
        var run = probe("failure", scratch.resolve("unused.json"));
        AuditedAssertions.assertEquals(23, run.exitCode());
        AuditedAssertions.assertTrue(diagnostic(run).contains("controlled-suite-failure"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS", "JDK_JAVA_OPTIONS"})
    void inheritedOptionsStopTheRealLauncherBeforeOutputPreparationOrMaven(String name) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        Path output = scratch.resolve("must-not-exist");
        Path entered = scratch.resolve("maven.entered");
        Path maven = scratch.resolve("maven.ps1");
        Files.writeString(maven, "[IO.File]::WriteAllText($env:ATF_MAVEN_ENTERED, 'entered')\n");
        var environment = Map.of(name, "-Dprivate.fixture=do-not-disclose", "ATF_MAVEN_ENTERED", entered.toString());
        var result = CommandProcess.execute(List.of("pwsh", "-NoProfile", "-NonInteractive", "-File",
                root.resolve("scripts/run-component-suites.ps1").toString(), "-SourceRoot", root.toString(),
                "-RunRoot", output.toString(), "-Maven", maven.toString()), environment, Duration.ofSeconds(45), false);
        String diagnostic = diagnostic(result);
        AuditedAssertions.assertEquals(1, result.exitCode(), diagnostic);
        AuditedAssertions.assertTrue(diagnostic.contains("COMPONENT-JVM-ENV") && diagnostic.contains(name), diagnostic);
        AuditedAssertions.assertFalse(diagnostic.contains("do-not-disclose"), "Report the variable name without its value");
        AuditedAssertions.assertFalse(Files.exists(output), "Reject before preparing the output or a workspace");
        AuditedAssertions.assertFalse(Files.exists(entered), "Reject before invoking Maven");
    }

    @Test void whitespaceGlobalOptionsAreStillRejected() throws Exception {
        Path script = helperProbe();
        var environment = environment();
        environment.put("JAVA_TOOL_OPTIONS", " ");
        var run = CommandProcess.execute(List.of("pwsh", "-NoProfile", "-NonInteractive", "-File", script.toString()),
                environment, Duration.ofSeconds(45), false);
        AuditedAssertions.assertEquals(1, run.exitCode());
        AuditedAssertions.assertTrue(diagnostic(run).contains("COMPONENT-JVM-ENV"));
    }

    private CommandProcess.Output probe(String mode, Path output) throws Exception {
        var environment = environment();
        environment.put("ATF_PROBE_MODE", mode);
        environment.put("ATF_PROBE_OUTPUT", output.toString());
        return CommandProcess.execute(List.of("pwsh", "-NoProfile", "-NonInteractive", "-File", helperProbe().toString()),
                environment, Duration.ofSeconds(45), false);
    }

    private Map<String, String> environment() {
        var environment = new HashMap<String, String>();
        environment.put("ATF_JVM_HELPER", GovernanceProject.locateProjectRoot().resolve("scripts/AtfComponentJvm.ps1").toString());
        environment.put("ATF_JAVA", Path.of(System.getProperty("java.home"), "bin", "java").toString());
        environment.put("ATF_CLASSPATH", System.getProperty("java.class.path"));
        environment.put("ATF_PROBE", ComponentJvmFixture.class.getName());
        environment.put("ATF_GIT_ROOT", scratch.resolve("checkout with spaces").toString());
        return environment;
    }

    private Path helperProbe() throws Exception {
        Path script = scratch.resolve("jvm-probe.ps1");
        Files.writeString(script, """
                $ErrorActionPreference = 'Stop'
                . $env:ATF_JVM_HELPER
                $before = @('JAVA_TOOL_OPTIONS','_JAVA_OPTIONS','JDK_JAVA_OPTIONS') | ForEach-Object { [Environment]::GetEnvironmentVariable($_,'Process') }
                Assert-AtfComponentJavaEnvironment
                $options = @(Get-AtfComponentJvmArguments -InputPartCharacters 8192 -SourceRoot $env:ATF_GIT_ROOT)
                & $env:ATF_JAVA @options -cp $env:ATF_CLASSPATH $env:ATF_PROBE $env:ATF_PROBE_MODE $env:ATF_PROBE_OUTPUT
                $result = $LASTEXITCODE
                $after = @('JAVA_TOOL_OPTIONS','_JAVA_OPTIONS','JDK_JAVA_OPTIONS') | ForEach-Object { [Environment]::GetEnvironmentVariable($_,'Process') }
                if (($before | ConvertTo-Json -Compress) -cne ($after | ConvertTo-Json -Compress)) { throw 'Global Java environment changed' }
                exit $result
                """);
        return script;
    }

    private static String diagnostic(CommandProcess.Output result) {
        return new String(result.stdout(), StandardCharsets.UTF_8) + result.stderr();
    }
}
