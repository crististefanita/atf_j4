package atf.tests.governance.support;

import atf.testkit.assertions.AuditedAssertions;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

final class SourceInputFixture {
    private SourceInputFixture() { }
    static Map<String, String> inventory(Path root, Path scratch) throws Exception {
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

    static String copy(Path root, Path stage, Map<String, String> inputs, Path scratch, int expectedExit) throws Exception {
        Path manifest = Files.createTempFile(scratch, "manifest-", ".json");
        new ObjectMapper().writeValue(manifest.toFile(), inputs.entrySet().stream()
                .map(entry -> Map.of("path", entry.getKey(), "sha256", entry.getValue())).toList());
        return execute("Copy-AtfSourceInputs.ps1", List.of("-SourceRoot", root.toString(), "-Destination", stage.toString(),
                "-InputManifest", manifest.toString()), scratch, expectedExit);
    }

    static String execute(String name, List<String> arguments, Path scratch, int expectedExit) throws Exception {
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

    static String hash(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    static void write(Path root, String relative, String text) throws Exception {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }
}

