package atf.tests.governance.support;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@AtfComponentTest
class LauncherSourceInventoryComponentTest {
    @Test void outputBoundariesKeepLegalPackagesAndDetectSourceChanges(@TempDir Path scratch) throws Exception {
        Path root = Files.createDirectory(scratch.resolve("surse cu spații"));
        write(root, "config/atf-governance-policy.json", """
                {"mavenModules":["module"],"additionalSourceRoots":[
                  {"module":"game/logic","path":"game/logic/custom","test":false}]}
                """);
        write(root, "module/pom.xml", """
                <project><build><testSourceDirectory>checks</testSourceDirectory></build></project>
                """);
        String release = "module/src/main/java/legal/release/A.java";
        String target = "module/src/test/java/legal/target/B.java";
        String build = "module/checks/legal/build/C.java";
        String custom = "game/logic/custom/release/D.java";
        String service = "module/src/test/resources/META-INF/services/extension";
        for (String path : Set.of(release, target, build, custom, service)) write(root, path, path);
        for (String path : Set.of("release/old.java", "build/noise", "module/target/noise",
                "game/logic/build/noise", "game/build/noise", ".git/noise")) write(root, path, "generated");
        Map<String, String> before = inventory(root, scratch);
        AuditedAssertions.assertEquals(before.keySet().stream().sorted().toList(), List.copyOf(before.keySet()),
                "Fingerprint inputs use deterministic ordinal path order, not host locale order");
        AuditedAssertions.assertEquals(Set.of("config/atf-governance-policy.json", "module/pom.xml",
                release, target, build, custom, service), before.keySet());
        AuditedAssertions.assertEquals(hash(release), before.get(release));
        Path staging = scratch.resolve("staging");
        copy(root, staging, before, scratch, 0);
        try (var files = Files.walk(staging)) {
            var copied = files.filter(Files::isRegularFile)
                    .map(file -> staging.relativize(file).toString().replace('\\', '/')).collect(java.util.stream.Collectors.toSet());
            AuditedAssertions.assertEquals(before.keySet(), copied, "Staging uses the captured inventory without additional exclusions");
        }

        write(root, release, "modified");
        Files.delete(root.resolve(target));
        String added = "module/src/main/java/legal/target/New.java";
        write(root, added, "new");
        Map<String, String> after = inventory(root, scratch);
        AuditedAssertions.assertEquals(hash("modified"), after.get(release));
        AuditedAssertions.assertFalse(after.containsKey(target));
        AuditedAssertions.assertEquals(hash("new"), after.get(added));
        AuditedAssertions.assertEquals(before.get(service), after.get(service));
        String refusal = copy(root, scratch.resolve("staging schimbat"), before, scratch, 1);
        AuditedAssertions.assertTrue(refusal.contains("SOURCE-STAGE-CHANGED:"), "A changed source is refused before compilation");
    }

    @Test void liveManifestCoversEveryDeclaredJavaSourceAndExtensionRegistration(@TempDir Path scratch) throws Exception {
        Path root = GovernanceProject.locateProjectRoot();
        Map<String, String> inputs = inventory(root, scratch);
        var missing = GovernanceProject.sources(root).stream()
                .map(source -> root.relativize(source.file()).toString().replace('\\', '/'))
                .filter(path -> !inputs.containsKey(path)).toList();
        AuditedAssertions.assertEquals(List.of(), missing, "Every declared Java source belongs to the launcher manifest");
        String service = "atf-tests/src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension";
        AuditedAssertions.assertTrue(inputs.containsKey(service), service);
    }
    @Test void alternateGradleEntriesExcludeOnlyTheirDeclaredGeneratedBoundaries(@TempDir Path scratch) throws Exception {
        Path root = Files.createDirectory(scratch.resolve("reactor JVM"));
        write(root, "config/atf-governance-policy.json", "{\"mavenModules\":[\"module\"],\"additionalSourceRoots\":[],\"buildRoots\":[\"jvm-entry\"]}");
        write(root, "module/pom.xml", "<project/>");
        write(root, "jvm-entry/settings.gradle", "rootProject.name='controlled'");
        write(root, "jvm-entry/build.gradle", "// controlled build");
        write(root, "jvm-entry/build/generated.txt", "generated");
        write(root, "jvm-entry/.gradle/cache", "generated");
        String legal = "jvm-entry/src/main/java/legal/build/Fixture.java";
        write(root, legal, "package legal.build; class Fixture {}");
        var inputs = inventory(root, scratch);
        AuditedAssertions.assertTrue(inputs.containsKey(legal), "A package named build remains source");
        AuditedAssertions.assertTrue(inputs.containsKey("jvm-entry/settings.gradle"), "The alternate reactor definition is an input");
        AuditedAssertions.assertFalse(inputs.containsKey("jvm-entry/build/generated.txt"), "Build outputs are excluded at the declared boundary");
        AuditedAssertions.assertFalse(inputs.containsKey("jvm-entry/.gradle/cache"), "The alternate Gradle cache is not source");
    }
    @Test void aDeclaredBuildRootWithoutItsSettingsIsRefused(@TempDir Path scratch) throws Exception {
        Path root = Files.createDirectory(scratch.resolve("incomplete JVM reactor"));
        write(root, "config/atf-governance-policy.json", "{\"mavenModules\":[],\"additionalSourceRoots\":[],\"buildRoots\":[\"jvm-entry\"]}");
        write(root, "jvm-entry/build.gradle", "// settings absent");
        String output = execute("Get-AtfSourceInputs.ps1", List.of("-SourceRoot", root.toString(), "-AsJson"), scratch, 1);
        AuditedAssertions.assertTrue(output.contains("settings"), "A missing reactor definition cannot produce a partial source fingerprint");
    }

    @Test void gitWorktreePointerDoesNotEnterTheSourceFingerprint(@TempDir Path scratch) throws Exception {
        Path root = Files.createDirectory(scratch.resolve("release worktree"));
        write(root, "config/atf-governance-policy.json", "{\"mavenModules\":[],\"additionalSourceRoots\":[]}");
        write(root, ".git", "gitdir: C:/private/repository/worktrees/release");
        write(root, ".gitattributes", "release/** -text");
        var inputs = inventory(root, scratch);
        AuditedAssertions.assertFalse(inputs.containsKey(".git"), "Worktree-local Git metadata is excluded");
        AuditedAssertions.assertTrue(inputs.containsKey(".gitattributes"), "Repository source configuration remains hashed");
    }

    private static Map<String, String> inventory(Path root, Path scratch) throws Exception {
        String output = execute("Get-AtfSourceInputs.ps1", List.of("-SourceRoot", root.toString(), "-AsJson"), scratch, 0);
        Map<String, String> result = new LinkedHashMap<>();
        var rows = new ObjectMapper().readTree(output);
        AuditedAssertions.assertTrue(rows.isArray(), "Source manifest is an array");
        for (var row : rows) {
            String path = row.required("path").asText();
            result.put(path, row.required("sha256").asText());
        }
        AuditedAssertions.assertEquals(rows.size(), result.size(), "Source paths are unique");
        return result;
    }

    private static String copy(Path root, Path stage, Map<String, String> inputs, Path scratch, int expectedExit) throws Exception {
        Path manifest = Files.createTempFile(scratch, "manifest-", ".json");
        new ObjectMapper().writeValue(manifest.toFile(), inputs.entrySet().stream()
                .map(entry -> Map.of("path", entry.getKey(), "sha256", entry.getValue())).toList());
        return execute("Copy-AtfSourceInputs.ps1", List.of("-SourceRoot", root.toString(), "-Destination", stage.toString(),
                "-InputManifest", manifest.toString()), scratch, expectedExit);
    }

    private static String execute(String name, List<String> arguments, Path scratch, int expectedExit) throws Exception {
        Path script = GovernanceProject.locateProjectRoot().resolve("scripts").resolve(name);
        Path output = Files.createTempFile(scratch, "process-", ".txt");
        var command = new ArrayList<>(List.of("pwsh", "-NoProfile", "-NonInteractive", "-File", script.toString()));
        command.addAll(arguments);
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            AuditedAssertions.assertTrue(process.waitFor(60, TimeUnit.SECONDS), "Source inventory exceeded its process deadline");
            AuditedAssertions.assertEquals(expectedExit, process.exitValue(), process.exitValue() == expectedExit
                    ? "Source input process completed with the expected exit" : Files.readString(output));
        } finally {
            if (process.isAlive()) { process.destroyForcibly(); process.waitFor(); }
        }
        return Files.readString(output);
    }

    private static String hash(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    private static void write(Path root, String relative, String text) throws Exception {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }
}
