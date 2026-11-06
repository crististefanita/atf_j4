package atf.tests.governance.documentation.release.integrity.fixture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Tiny legal filesystem witness; its independent receipt can be refreshed to probe deeper oracles. */
public record ReleaseFixture(Path project, Path folder, ObjectNode manifest) {
    public static final String VERSION = "1.2.0", CASE_FILE = "TEST-example.Game.xml";
    private static final ObjectMapper JSON = new ObjectMapper();
    public static ReleaseFixture create(Path project) throws Exception {
        Path folder = Files.createDirectories(project.resolve("release/" + VERSION));
        var manifest = JSON.createObjectNode();
        manifest.putObject("release").put("versionName", VERSION).put("apkName", "WhistLab-" + VERSION + ".apk")
                .put("apkRelativePath", "WhistLab-" + VERSION + ".apk");
        manifest.putObject("source").putNull("sourceCommit").put("workingTreeFingerprint", "a".repeat(64))
                .put("inputManifest", "reports/apk-inputs.json");
        var group = manifest.putObject("qualification").put("state", "PASS").putObject("groups").putObject("component");
        group.put("tests", 1).put("failures", 0).put("errors", 0).put("skipped", 0);
        group.putArray("files").add(CASE_FILE).add("TEST-example.EmptySuite.xml");
        write(folder.resolve("README.md"), "# Ediție\n");write(folder.resolve("reports/README.md"), "# Rezultate\n");
        write(folder.resolve("reports/apk-inputs.json"), "{\"sourceCommit\":null,\"fingerprint\":\"" + "a".repeat(64)
                + "\",\"files\":[{\"path\":\"src/game.java\",\"sha256\":\"" + "b".repeat(64) + "\"}]}");
        var html = folder.resolve("reports/report.html");write(html, "<html><body>checkpoint</body></html>");
        manifest.putObject("evidenceRetention").put("canonicalReport", "reports/report.html").put("canonicalReportSha256", hash(html));
        var fixture = new ReleaseFixture(project, folder, manifest);
        fixture.xml(suite("<testcase classname=\"example.Game\" name=\"plays\"/>", 1, 0, 0, 0));
        write(fixture.result("TEST-example.EmptySuite.xml"), suite("", 0, 0, 0, 0));
        fixture.archive(Map.of("AndroidManifest.xml", "manifest", "classes.dex", "DEX-FIXTURE"));
        fixture.capture();return fixture;
    }
    public Path apk() { return folder.resolve("WhistLab-" + VERSION + ".apk"); }
    public Path result(String name) { return folder.resolve("reports/test-results/component/" + name); }
    public ObjectNode group() { return (ObjectNode) manifest.path("qualification").path("groups").path("component"); }
    public ObjectNode release() { return (ObjectNode) manifest.path("release"); }
    public void xml(String xml) throws IOException { write(result(CASE_FILE), xml); }
    public void save() throws IOException { JSON.writerWithDefaultPrettyPrinter().writeValue(folder.resolve("release-validation.json").toFile(), manifest); }
    public void capture() throws Exception {
        var integrity = manifest.putObject("integrity").put("schemaVersion", 1).put("kind", "OBSERVED_PAYLOAD_HASHES")
                .put("observedUtc", "2026-01-01T00:00:00Z");
        var files = integrity.putArray("files");
        try (var entries = Files.walk(folder)) {
            for (Path file : entries.filter(Files::isRegularFile).sorted().toList()) {
                if (file.equals(folder.resolve("release-validation.json"))) continue;
                files.addObject().put("path", folder.relativize(file).toString().replace('\\', '/'))
                        .put("bytes", Files.size(file)).put("sha256", hash(file));
            }
        }
        save();
    }
    public void archive(Map<String, String> entries) throws Exception {
        Files.createDirectories(apk().getParent());
        try (var output = new ZipOutputStream(Files.newOutputStream(apk()))) {
            for (var value : entries.entrySet()) {
                byte[] bytes = value.getValue().getBytes(StandardCharsets.UTF_8);var crc = new CRC32();crc.update(bytes);
                var entry = new ZipEntry(value.getKey());entry.setMethod(ZipEntry.STORED);
                entry.setSize(bytes.length);entry.setCompressedSize(bytes.length);entry.setCrc(crc.getValue());
                output.putNextEntry(entry);output.write(bytes);output.closeEntry();
            }
        }
        refreshApk();
    }
    public void refreshApk() throws Exception {
        release().put("apkBytes", Files.size(apk())).put("apkSha256", hash(apk()));
        write(apk().resolveSibling(apk().getFileName() + ".sha256"), hash(apk()) + "  " + apk().getFileName() + "\n");save();
    }
    public static String suite(String body, int tests, int failures, int errors, int skipped) {
        return "<testsuite name=\"example.Game\" tests=\"" + tests + "\" failures=\"" + failures
                + "\" errors=\"" + errors + "\" skipped=\"" + skipped + "\">" + body + "</testsuite>";
    }
    public static String hash(Path file) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }
    public static void write(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent());Files.writeString(file, text);
    }
}
