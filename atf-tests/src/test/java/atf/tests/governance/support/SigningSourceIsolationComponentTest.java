package atf.tests.governance.support;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static atf.tests.governance.support.SourceInputFixture.*;

@AtfComponentTest
class SigningSourceIsolationComponentTest {
    @Test void gitReconstructionMatchesACheckoutContainingALocalKey(@TempDir Path scratch) throws Exception {
        Path root = fixture(scratch);
        Path script = scratch.resolve("reconstruct.ps1");
        Files.writeString(script, """
                param($Root,$Archive,$Rebuilt)
                $ErrorActionPreference='Stop'
                & git init --quiet $Root
                if ($LASTEXITCODE) { throw 'fixture init failed' }
                & git -C $Root config core.autocrlf false
                if ($LASTEXITCODE) { throw 'fixture byte policy failed' }
                & git -C $Root config core.eol lf
                if ($LASTEXITCODE) { throw 'fixture EOL policy failed' }
                & git -C $Root add .
                if ($LASTEXITCODE) { throw 'fixture index failed' }
                & git -C $Root -c user.name=Fixture -c user.email=fixture@example.invalid commit --quiet -m fixture
                if ($LASTEXITCODE) { throw 'fixture commit failed' }
                & git -C $Root archive --format=zip --output=$Archive HEAD
                if ($LASTEXITCODE) { throw 'fixture archive failed' }
                Expand-Archive -LiteralPath $Archive -DestinationPath $Rebuilt
                """);
        Path rebuilt = scratch.resolve("rebuilt");
        runSetup(script, List.of(root.toString(), scratch.resolve("source.zip").toString(), rebuilt.toString()), scratch);
        var gitSources = inventory(rebuilt, scratch);
        write(root, ".atf-signing/whist-android-test.keystore", "dummy private material");
        AuditedAssertions.assertEquals(gitSources, inventory(root, scratch), "The same Git source qualifies with or without a local key");
    }

    @Test void localKeysAndTheirRotationDoNotChangeThePublicSourceSnapshot(@TempDir Path scratch) throws Exception {
        Path root = fixture(scratch);
        var clean = inventory(root, scratch);
        for (String key : List.of("whist-android-test.keystore", "backup/key.p12", "local-password.txt")) {
            write(root, ".atf-signing/" + key, "dummy private material");
        }
        AuditedAssertions.assertEquals(clean, inventory(root, scratch), "A local key does not affect qualification identity");
        write(root, ".atf-signing/whist-android-test.keystore", "rotated dummy private material");
        var rotated = inventory(root, scratch);
        AuditedAssertions.assertEquals(clean, rotated, "Private key rotation is not a source change");
        Path stage = scratch.resolve("export");
        copy(root, stage, rotated, scratch, 0);
        AuditedAssertions.assertEquals("public certificate", Files.readString(stage.resolve(".atf-signing/whist-android-test.pem")));
        try (var files = Files.list(stage.resolve(".atf-signing"))) {
            AuditedAssertions.assertEquals(List.of(".gitignore", "README.md", "whist-android-test.fingerprint", "whist-android-test.pem"),
                    files.map(file -> file.getFileName().toString()).sorted().toList());
        }
        AuditedAssertions.assertEquals("rotated dummy private material", Files.readString(root.resolve(".atf-signing/whist-android-test.keystore")),
                "Qualification never alters the local key");
    }

    @Test void publicIdentityAndUnexpectedSourcesRemainFingerprintInputs(@TempDir Path scratch) throws Exception {
        Path root = fixture(scratch);
        var before = inventory(root, scratch);
        write(root, ".atf-signing/whist-android-test.fingerprint", "changed public identity");
        write(root, "unexpected-source.txt", "unreviewed input");
        var after = inventory(root, scratch);
        AuditedAssertions.assertEquals(hash("changed public identity"), after.get(".atf-signing/whist-android-test.fingerprint"));
        AuditedAssertions.assertNotEquals(before, after, "The private exclusion cannot hide other source changes");
        AuditedAssertions.assertEquals(hash("unreviewed input"), after.get("unexpected-source.txt"));
    }

    @Test void aForgedManifestCannotCopyPrivateMaterialOrPartiallyExportPublicFiles(@TempDir Path scratch) throws Exception {
        Path root = fixture(scratch);
        write(root, ".atf-signing/whist-android-test.keystore", "dummy private material");
        for (String path : List.of(".atf-signing/whist-android-test.keystore",
                ".ATF-SIGNING/WHIST-ANDROID-TEST.KEYSTORE", "legal/../.atf-signing/whist-android-test.keystore",
                ".atf-signing\\whist-android-test.keystore", ".atf-signing/unknown.pem")) {
            Map<String, String> forged = new LinkedHashMap<>();
            forged.put(".atf-signing/README.md", hash("public signing contract"));
            forged.put(path, hash("dummy private material"));
            Path stage = scratch.resolve("refused-" + forged.hashCode());
            String output = copy(root, stage, forged, scratch, 1);
            AuditedAssertions.assertTrue(output.contains("SOURCE-STAGE-PRIVATE-SIGNING:"), "Every private alias is refused");
            AuditedAssertions.assertFalse(Files.exists(stage), "The whole manifest is checked before any copy");
            AuditedAssertions.assertFalse(output.contains("dummy private material"), "Diagnostics do not expose key contents");
        }
    }

    @Test void aPublicPathCannotFollowALinkToPrivateMaterial(@TempDir Path scratch) throws Exception {
        Path root = fixture(scratch);
        Path privateDirectory = Files.createDirectory(scratch.resolve("private"));
        write(privateDirectory, "key", "dummy private material");
        Path script = scratch.resolve("junction.ps1");
        Files.writeString(script, "param($Link,$Target)\nNew-Item -ItemType Junction -Path $Link -Target $Target -ErrorAction Stop | Out-Null\n");
        runSetup(script, List.of(root.resolve("linked").toString(), privateDirectory.toString()), scratch);
        String inventoryFailure = execute("Get-AtfSourceInputs.ps1", List.of("-SourceRoot", root.toString(), "-AsJson"), scratch, 1);
        AuditedAssertions.assertTrue(inventoryFailure.contains("linked"), "Inventory refuses an unaccounted link");
        Path stage = scratch.resolve("indirect-export");
        String copyFailure = copy(root, stage, Map.of("linked/key", hash("dummy private material")), scratch, 1);
        AuditedAssertions.assertTrue(copyFailure.contains("SOURCE-STAGE-INDIRECT:"));
        AuditedAssertions.assertFalse(Files.exists(stage), "A forged manifest cannot bypass link validation");
    }

    private static Path fixture(Path scratch) throws Exception {
        Path root = Files.createDirectory(scratch.resolve("checkout"));
        write(root, "config/atf-governance-policy.json", "{\"mavenModules\":[],\"additionalSourceRoots\":[]}");
        write(root, ".atf-signing/.gitignore", "*\n!.gitignore\n!README.md\n!whist-android-test.pem\n!whist-android-test.fingerprint\n");
        write(root, ".atf-signing/README.md", "public signing contract");
        write(root, ".atf-signing/whist-android-test.pem", "public certificate");
        write(root, ".atf-signing/whist-android-test.fingerprint", "public identity");
        return root;
    }

    private static void runSetup(Path script, List<String> arguments, Path scratch) throws Exception {
        var command = new java.util.ArrayList<>(List.of("pwsh", "-NoProfile", "-File", script.toString()));
        command.addAll(arguments);
        Path output = Files.createTempFile(scratch, "setup-", ".txt");
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            AuditedAssertions.assertTrue(process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS));
            AuditedAssertions.assertEquals(0, process.exitValue(), Files.readString(output));
        } finally {
            if (process.isAlive()) { process.destroyForcibly(); process.waitFor(); }
        }
    }
}
