package atf.tests.governance.documentation.release.rag.scope;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@AtfComponentTest
class QualificationLauncherComponentTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String VERSION = "1.2.3";
    private static final String APK = "WhistLab-" + VERSION + ".apk";
    private static final String RELEASE = "release/" + VERSION + "/";
    private static final String GOVERNANCE = "run-atf-governance.ps1";
    private static final String COMPONENTS = "run-component-suites.ps1";

    @TempDir Path scratch;

    @ParameterizedTest
    @CsvSource({
        "run-atf-governance.ps1, missing-apk, ENOENT",
        "run-atf-governance.ps1, corrupt-apk, APK_BYTES_OR_HASH",
        "run-component-suites.ps1, corrupt-checksum, APK_CHECKSUM",
        "run-component-suites.ps1, wrong-sidecar-size, RELEASE-PREFLIGHT-INVENTORY",
        "run-atf-governance.ps1, wrong-apk-name, RELEASE-PREFLIGHT-IDENTITY"
    })
    void anInvalidArtifactStopsTheRealLauncherBeforeMaven(String runner, String defect, String diagnostic) throws Exception {
        Path source = fixture();
        Path apk = source.resolve(RELEASE + APK);
        Path checksum = source.resolve(RELEASE + APK + ".sha256");
        Path manifest = source.resolve(RELEASE + "release-validation.json");
        switch (defect) {
            case "missing-apk" -> Files.delete(apk);
            case "corrupt-apk" -> Files.writeString(apk, "altered");
            case "corrupt-checksum" -> Files.writeString(checksum, "a".repeat(64) + "  " + APK);
            case "wrong-sidecar-size" -> Files.writeString(checksum, Files.readString(checksum) + "\n");
            case "wrong-apk-name" -> Files.writeString(manifest, Files.readString(manifest).replace(APK, "Other.apk"));
            default -> throw new AssertionError(defect);
        }
        Path output = scratch.resolve("blocked");
        var run = launch(runner, source, output, "", null);
        AuditedAssertions.assertEquals(1, run.exit(), run.log());
        AuditedAssertions.assertTrue(run.log().contains("RELEASE-PREFLIGHT") && run.log().contains(diagnostic), run.log());
        AuditedAssertions.assertFalse(Files.exists(scratch.resolve("maven.entered")), "Maven must never start for invalid release bytes");
    }

    @ParameterizedTest
    @ValueSource(strings = {GOVERNANCE, COMPONENTS})
    void aBuildEditAfterPlanningStopsBothLaunchers(String runner) throws Exception {
        Path source = fixture();
        Path pinned = scratch.resolve("plan-inputs.json");
        Path pom = source.resolve("pom.xml");
        // Capture the genuine launcher inventory so the only difference is one build input.
        var output = scratch.resolve("inventory");
        AuditedAssertions.assertEquals(0, launch(GOVERNANCE, source, output, "", null).exit());
        Files.copy(output.resolve("source-inputs.json"), pinned);
        Files.delete(scratch.resolve("maven.entered"));
        Files.writeString(pom, "<project><name>changed after pinning</name></project>");
        var run = launch(runner, source, scratch.resolve("changed"), "", pinned);
        AuditedAssertions.assertEquals(1, run.exit(), run.log());
        AuditedAssertions.assertTrue(run.log().contains("QUALIFICATION-SOURCE-DIFFERS-FROM-PLAN"), run.log());
        AuditedAssertions.assertFalse(Files.exists(scratch.resolve("maven.entered")), "A stale plan must stop before Maven");
    }

    @Test void governancePassesWithAnIsolatedRootAndThePinnedInputManifest() throws Exception {
        Path source = fixture(), output = scratch.resolve("healthy");
        var run = launch(GOVERNANCE, source, output, "", null);
        AuditedAssertions.assertEquals(0, run.exit(), run.log());
        JsonNode result = JSON.readTree(output.resolve("governance-result.json").toFile());
        Path stage = Path.of(result.required("stage").asText());
        AuditedAssertions.assertEquals(stage.toString(), Files.readString(scratch.resolve("maven.entered")));
        AuditedAssertions.assertFalse(stage.equals(source));
        AuditedAssertions.assertTrue(Files.isRegularFile(stage.resolve(RELEASE + APK)));
        AuditedAssertions.assertEquals(hash(output.resolve("release-inputs.json")), result.required("releaseFingerprint").asText());
        AuditedAssertions.assertEquals("PASS", JSON.readTree(output.resolve("release-preflight.log").toFile()).required("status").asText());
        AuditedAssertions.assertEquals(0, launch(GOVERNANCE, source, scratch.resolve("matching"), "",
                output.resolve("source-inputs.json")).exit());
    }

    @ParameterizedTest
    @CsvSource({"live-add, LIVE-RELEASE: release/added.json", "live-delete, LIVE-RELEASE: release/index.json",
            "stage-change, STAGED-RELEASE: release/index.json"})
    void driftInReleaseEvidenceInvalidatesAnOtherwisePassingRun(String mutation, String diagnostic) throws Exception {
        Path source = fixture(), output = scratch.resolve("drift");
        var run = launch(GOVERNANCE, source, output, mutation, null);
        AuditedAssertions.assertEquals(1, run.exit(), run.log());
        JsonNode result = JSON.readTree(output.resolve("governance-result.json").toFile());
        AuditedAssertions.assertEquals(0, result.required("testExit").asInt());
        AuditedAssertions.assertEquals("FAIL", result.required("status").asText());
        AuditedAssertions.assertTrue(result.required("snapshotDifferences").toString().contains(diagnostic));
    }

    private Path fixture() throws Exception {
        Path source = Files.createDirectory(scratch.resolve("surse fixate"));
        write(source, "config/atf-governance-policy.json", "{\"mavenModules\":[],\"additionalSourceRoots\":[]}");
        write(source, "pom.xml", "<project/>");
        Path real = GovernanceProject.locateProjectRoot();
        for (String file : List.of("check-release-artifacts.mjs", "evaluate-release-rag.mjs",
                "release-test-scope.mjs", "component-evidence-scope.mjs", "github-release-retention.mjs")) {
            String relative = "atf-tests/tools/" + file;
            Path target = source.resolve(relative);
            Files.createDirectories(target.getParent());
            Files.copy(real.resolve(relative), target);
        }
        write(source, "release/index.json", "{\"schemaVersion\":2,\"currentVersion\":\"" + VERSION + "\",\"versions\":[\"" + VERSION + "\"]}");
        Path apk = write(source, RELEASE + APK, "apk fixture");
        Path checksum = write(source, RELEASE + APK + ".sha256", hash(apk) + "  " + APK + "\n");
        var manifest = JSON.createObjectNode();
        manifest.putObject("release").put("artifactKind", "apk").put("versionName", VERSION)
                .put("apkName", APK).put("apkRelativePath", APK).put("apkBytes", Files.size(apk)).put("apkSha256", hash(apk));
        var files = manifest.putObject("integrity").putArray("files");
        for (Path file : List.of(apk, checksum)) files.addObject()
                .put("path", file.getFileName().toString()).put("bytes", Files.size(file)).put("sha256", hash(file));
        JSON.writeValue(source.resolve(RELEASE + "release-validation.json").toFile(), manifest);
        return source;
    }

    private Run launch(String runner, Path source, Path output, String mutation, Path pinned) throws Exception {
        Path tool = scratch.resolve("maven-probe.ps1");
        Files.writeString(tool, """
                $stage = (Get-Location).Path
                if ($stage -eq $env:ATF_FIXTURE_SOURCE -or -not ($args -contains "-Datf.governance.root=$stage") -or
                    -not (Test-Path -LiteralPath (Join-Path $stage 'release/index.json'))) { exit 19 }
                [IO.File]::WriteAllText($env:ATF_FIXTURE_ENTERED, $stage)
                switch ($env:ATF_FIXTURE_MUTATION) {
                    'live-add' { [IO.File]::WriteAllText((Join-Path $env:ATF_FIXTURE_SOURCE 'release/added.json'), '{}') }
                    'live-delete' { Remove-Item -LiteralPath (Join-Path $env:ATF_FIXTURE_SOURCE 'release/index.json') }
                    'stage-change' { [IO.File]::AppendAllText((Join-Path $stage 'release/index.json'), ' ') }
                }
                exit 0
                """);
        var command = new ArrayList<>(List.of("pwsh", "-NoProfile", "-NonInteractive", "-File",
                GovernanceProject.locateProjectRoot().resolve("scripts").resolve(runner).toString(),
                "-SourceRoot", source.toString(), "-RunRoot", output.toString(), "-Maven", tool.toString(),
                "-MavenCache", scratch.resolve("unused-cache").toString()));
        if (pinned != null) command.addAll(List.of("-PinnedSourceInputs", pinned.toString()));
        var builder = new ProcessBuilder(command);
        var environment = builder.environment();
        environment.put("TEMP", scratch.toString());
        environment.put("TMP", scratch.toString());
        environment.put("ATF_FIXTURE_SOURCE", source.toString());
        environment.put("ATF_FIXTURE_ENTERED", scratch.resolve("maven.entered").toString());
        environment.put("ATF_FIXTURE_MUTATION", mutation);
        Path log = Files.createTempFile(scratch, "launcher-", ".log");
        var process = builder.redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            AuditedAssertions.assertTrue(process.waitFor(60, TimeUnit.SECONDS), "Launcher timed out: " + log);
            return new Run(process.exitValue(), new String(Files.readAllBytes(log), java.nio.charset.StandardCharsets.UTF_8));
        } finally { if (process.isAlive()) process.destroyForcibly(); }
    }

    private static Path write(Path root, String relative, String text) throws Exception {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, text);
    }

    private static String hash(Path file) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }

    private record Run(int exit, String log) { }
}
