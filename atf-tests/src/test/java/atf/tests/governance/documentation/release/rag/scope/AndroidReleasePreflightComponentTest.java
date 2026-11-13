package atf.tests.governance.documentation.release.rag.scope;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@AtfComponentTest
class AndroidReleasePreflightComponentTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String SERIAL = "127.0.0.1:15555";
    private static final String SOCKET = "tcp:127.0.0.1:15038";
    private static final String TARGET = "-s " + SERIAL + " ";
    private static final String CONNECT = "connect " + SERIAL;
    private static final String STATE = TARGET + "get-state";
    private static final String BOOT = TARGET + "shell getprop sys.boot_completed";
    private static final String API = TARGET + "shell getprop ro.build.version.sdk";
    private static final String SIZE = TARGET + "shell wm size";
    private static final String DENSITY = TARGET + "shell wm density";
    private static final List<String> FULL_PREFLIGHT = List.of(CONNECT, STATE, BOOT, API, SIZE, DENSITY);

    @TempDir static Path nativeTools;
    @TempDir Path scratch;

    @BeforeAll static void compileProcessDouble() throws Exception {
        Path compiler = Path.of(System.getenv("WINDIR"), "Microsoft.NET/Framework64/v4.0.30319/csc.exe");
        Path source = nativeTools.resolve("FakeAdb.cs");
        Files.writeString(source, """
                using System;
                using System.IO;
                class FakeAdb {
                    static int Main(string[] args) {
                        var command = string.Join(" ", args);
                        File.AppendAllText(Environment.GetEnvironmentVariable("ATF_FIXTURE_ADB_CALLS"), command + Environment.NewLine);
                        if (Environment.GetEnvironmentVariable("ADB_SERVER_SOCKET") != "tcp:127.0.0.1:15038") return 89;
                        var mode = Environment.GetEnvironmentVariable("ATF_FIXTURE_ADB_MODE");
                        switch (command) {
                            case "connect 127.0.0.1:15555":
                                if (mode == "refused") Console.Error.WriteLine("failed to connect: Connection refused");
                                else if (mode == "timeout") System.Threading.Thread.Sleep(8000);
                                else Console.WriteLine("already connected to 127.0.0.1:15555");
                                return 0;
                            case "-s 127.0.0.1:15555 get-state":
                                Console.WriteLine(mode == "offline" ? "offline" : "device");
                                return 0;
                            case "-s 127.0.0.1:15555 shell getprop sys.boot_completed":
                                Console.WriteLine(mode == "boot" ? "0" : "1");
                                return 0;
                            case "-s 127.0.0.1:15555 shell getprop ro.build.version.sdk":
                                Console.WriteLine(mode == "api" ? "35" : "28");
                                return 0;
                            case "-s 127.0.0.1:15555 shell wm size":
                                Console.WriteLine("Physical size: 1080x1920");
                                if (mode == "display") Console.WriteLine("Override size: 720x1280");
                                if (mode == "malformed") Console.WriteLine("Override size: invalid");
                                return 0;
                            case "-s 127.0.0.1:15555 shell wm density":
                                Console.WriteLine("Physical density: 440");
                                if (mode != "physical") Console.WriteLine("Override density: 420");
                                return 0;
                            default:
                                Console.Error.WriteLine("Unexpected ADB command: " + command);
                                return 90;
                        }
                    }
                }
                """);
        Path output = nativeTools.resolve("compile.log");
        Process process = new ProcessBuilder(compiler.toString(), "/nologo",
                "/out:" + nativeTools.resolve("adb.exe"), source.toString())
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("Mock ADB compiler timed out");
        }
        if (process.exitValue() != 0) throw new IllegalStateException(Files.readString(output));
    }

    @ParameterizedTest
    @CsvSource({
        "refused, ANDROID_PREFLIGHT_ADB, 1",
        "offline, ANDROID_PREFLIGHT_ADB, 2",
        "boot, ANDROID_PREFLIGHT_BOOT, 3",
        "api, ANDROID_PREFLIGHT_PROFILE, 4",
        "display, ANDROID_PREFLIGHT_PROFILE, 6",
        "malformed, ANDROID_PREFLIGHT_PROFILE, 5"
    })
    void theNativeLauncherBlocksBeforeStagingOrMaven(String mode, String code, int commands) throws Exception {
        Path output = scratch.resolve("run");
        Path lease = scratch.resolve("lease.json");
        Path maven = scratch.resolve("maven-probe.ps1");
        Files.writeString(maven, "[IO.File]::WriteAllText($env:ATF_FIXTURE_MAVEN_ENTERED, 'entered'); exit 19");
        Run run = invoke("run-whist-responsive-sanity.ps1", mode, List.of(
                "-RunRoot", output.toString(), "-LeasePath", lease.toString(), "-Maven", maven.toString(),
                "-MavenRepository", Files.createDirectory(scratch.resolve("cache")).toString(), "-ExpectedApi", "28", "-ExpectedDisplayProfile", "1080x1920@420"));
        AuditedAssertions.assertEquals(1, run.exit(), run.log());
        JsonNode state = read(output.resolve("WORKING_STATE.json"));
        AuditedAssertions.assertEquals("BLOCKED", state.required("status").asText());
        JsonNode preflight = read(output.resolve("android-preflight.json"));
        AuditedAssertions.assertEquals("BLOCKED", preflight.required("status").asText());
        AuditedAssertions.assertTrue(preflight.required("failure").asText().contains(code), run.log());
        AuditedAssertions.assertFalse(Files.exists(Path.of(state.required("buildRoot").asText())));
        AuditedAssertions.assertFalse(Files.exists(scratch.resolve("maven.entered")));
        AuditedAssertions.assertFalse(read(lease).required("active").asBoolean());
        AuditedAssertions.assertEquals(FULL_PREFLIGHT.subList(0, commands), calls());
    }

    @ParameterizedTest
    @CsvSource({"override, 1080x1920@420", "physical, 1080x1920@440"})
    void theSignedReleaseEntryRecordsTheEffectiveProfile(String mode, String profile) throws Exception {
        Path lease = activeLease(SERIAL);
        Path receipt = scratch.resolve("preflight.json");
        Run run = check(mode, lease, receipt, profile, "10");
        AuditedAssertions.assertEquals(0, run.exit(), run.log());
        JsonNode result = read(receipt);
        AuditedAssertions.assertEquals("PASS", result.required("status").asText());
        AuditedAssertions.assertEquals(28, result.at("/observed/api").asInt());
        AuditedAssertions.assertEquals(profile, result.at("/observed/displayProfile").asText());
        AuditedAssertions.assertTrue(result.at("/observed/bootCompleted").asBoolean());
        AuditedAssertions.assertEquals(FULL_PREFLIGHT, calls());
    }

    @Test void theNativeLauncherStartsMavenOnlyAfterAValidPreflight() throws Exception {
        Path output = scratch.resolve("run");
        Path lease = scratch.resolve("lease.json");
        Path maven = scratch.resolve("maven-probe.ps1");
        Files.writeString(maven, """
                $preflight = Get-Content -LiteralPath $env:ATF_FIXTURE_PREFLIGHT -Raw | ConvertFrom-Json
                if ($preflight.status -ne 'PASS' -or $preflight.observed.api -ne 28) { exit 18 }
                [IO.File]::WriteAllText($env:ATF_FIXTURE_MAVEN_ENTERED, 'preflight passed')
                exit 19
                """);
        Run run = invoke("run-whist-responsive-sanity.ps1", "override", List.of(
                "-RunRoot", output.toString(), "-LeasePath", lease.toString(), "-Maven", maven.toString(),
                "-MavenRepository", Files.createDirectory(scratch.resolve("cache")).toString(), "-ExpectedApi", "28", "-ExpectedDisplayProfile", "1080x1920@420"));
        AuditedAssertions.assertEquals(1, run.exit(), run.log());
        AuditedAssertions.assertEquals("preflight passed", Files.readString(scratch.resolve("maven.entered")));
        AuditedAssertions.assertEquals("FAIL", read(output.resolve("WORKING_STATE.json")).required("status").asText());
        AuditedAssertions.assertFalse(read(lease).required("active").asBoolean());
        AuditedAssertions.assertEquals(FULL_PREFLIGHT, calls());
    }

    @Test void aForeignLeaseBlocksBeforeCallingAdb() throws Exception {
        Path receipt = scratch.resolve("preflight.json");
        Run run = check("override", activeLease("different-device"), receipt, "1080x1920@420", "10");
        AuditedAssertions.assertEquals(1, run.exit(), run.log());
        AuditedAssertions.assertTrue(read(receipt).required("failure").asText().contains("ANDROID_PREFLIGHT_LEASE"));
        AuditedAssertions.assertTrue(calls().isEmpty(), "ADB must stay untouched under a foreign lease");
    }

    @Test void aTimedOutAdbProcessProducesABlockedReceipt() throws Exception {
        Path receipt = scratch.resolve("preflight.json");
        Run run = check("timeout", activeLease(SERIAL), receipt, "1080x1920@420", "1");
        AuditedAssertions.assertEquals(1, run.exit(), run.log());
        AuditedAssertions.assertTrue(read(receipt).required("failure").asText().contains("timed out"));
        AuditedAssertions.assertEquals(List.of(CONNECT), calls());
    }

    private Run check(String mode, Path lease, Path receipt, String profile, String timeout) throws Exception {
        return invoke("check-android-release-device.ps1", mode, List.of(
                "-LeasePath", lease.toString(), "-LeaseId", "fixture-lease", "-Receipt", receipt.toString(),
                "-ExpectedApi", "28", "-ExpectedDisplayProfile", profile, "-TimeoutSeconds", timeout));
    }

    private Path activeLease(String serial) throws Exception {
        return Files.writeString(scratch.resolve("lease.json"),
                JSON.createObjectNode().put("active", true).put("serial", serial).put("leaseId", "fixture-lease").toString());
    }

    private List<String> calls() throws Exception {
        Path calls = scratch.resolve("adb.calls");
        return Files.exists(calls) ? Files.readAllLines(calls) : List.of();
    }

    private static JsonNode read(Path path) throws Exception {
        return JSON.readTree(path.toFile());
    }

    private Run invoke(String script, String mode, List<String> arguments) throws Exception {
        Path runner = GovernanceProject.locateProjectRoot().resolve("scripts").resolve(script);
        var command = new ArrayList<>(List.of("pwsh", "-NoProfile", "-NonInteractive", "-File", runner.toString(),
                "-AdbExecutable", nativeTools.resolve("adb.exe").toString(), "-Serial", SERIAL, "-AdbServerSocket", SOCKET));
        command.addAll(arguments);
        Path output = scratch.resolve("process.log");
        var builder = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile());
        var environment = builder.environment();
        environment.put("TEMP", scratch.toString());
        environment.put("TMP", scratch.toString());
        environment.put("ATF_FIXTURE_ADB_MODE", mode);
        environment.put("ATF_FIXTURE_ADB_CALLS", scratch.resolve("adb.calls").toString());
        environment.put("ATF_FIXTURE_MAVEN_ENTERED", scratch.resolve("maven.entered").toString());
        environment.put("ATF_FIXTURE_PREFLIGHT", scratch.resolve("run/android-preflight.json").toString());
        Process process = builder.start();
        try {
            AuditedAssertions.assertTrue(process.waitFor(45, TimeUnit.SECONDS), "Android preflight launcher exceeded its deadline");
            return new Run(process.exitValue(), new String(Files.readAllBytes(output), StandardCharsets.UTF_8));
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }

    private record Run(int exit, String log) {}
}
