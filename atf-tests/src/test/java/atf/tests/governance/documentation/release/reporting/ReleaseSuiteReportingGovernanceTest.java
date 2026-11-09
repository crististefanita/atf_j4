package atf.tests.governance.documentation.release.reporting;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static atf.testkit.assertions.AuditedAssertions.*;

/** Verifies that active release suite reports have distinct, hash-bound owners and receipts. */
@AtfComponentTest
class ReleaseSuiteReportingGovernanceTest {
    @org.junit.jupiter.api.extension.RegisterExtension
    static final atf.tests.reporting.GameReportAudit AUDIT = new atf.tests.reporting.GameReportAudit();
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern INLINE_PNG = Pattern.compile("data:image/png;base64,");

    @Test void activeComponentAndE2eSuitesOwnDistinctHashBoundExtentReports() throws Exception {
        Path root = GovernanceProject.locateProjectRoot();
        String version = JSON.readTree(root.resolve("release/index.json").toFile()).required("currentVersion").asText();
        Path release = root.resolve("release").resolve(version);
        JsonNode manifest = JSON.readTree(release.resolve("release-validation.json").toFile());
        JsonNode policy = JSON.readTree(root.resolve("config/release-platform-rag-policy.json").toFile());
        JsonNode rag = policy.required("ragEvaluator"), reporting = policy.required("reportingContract");
        Map<String, String> payload = payloadHashes(manifest);
        Set<String> componentReports = new HashSet<>(), allReports = new HashSet<>();

        String componentPattern = reporting.required("componentReportPathPattern").asText();
        String componentReceiptPattern = rag.required("componentSourceBinding").required("executionReceiptPattern").asText();
        for (JsonNode required : rag.required("requiredComponentGroupsByPlatform").required("android")) {
            String name = required.asText();
            JsonNode group = manifest.required("qualification").required("auditedGroups").required(name);
            String report = group.required("report").asText();
            String expected = componentPattern.replace("{group}", name);
            assertEquals(expected, report, "Each component suite owns its canonical Extent path: " + name);
            assertTrue(componentReports.add(report), "Component reports must not alias: " + report);
            assertTrue(allReports.add(report), "Report path is globally unique: " + report);
            assertReport(release, report, payload);

            String receiptPath = componentReceiptPattern.replace("{group}", name);
            JsonNode receipt = readPayloadJson(release, receiptPath, payload);
            assertEquals(hash(release.resolve(report)), receipt.required("reportSha256").asText(),
                    "The execution receipt binds the exact Extent bytes: " + name);
        }

        String e2ePattern = reporting.required("e2eReportPathPattern").asText();
        String e2eReceiptPattern = rag.required("e2e").required("executionReceiptPattern").asText();
        JsonNode e2eGroups = manifest.required("qualification").required("e2eGroups");
        var names = e2eGroups.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            JsonNode group = e2eGroups.required(name);
            String report = group.required("report").asText();
            String suiteDirectory = Path.of(report).getParent().getFileName().toString();
            assertEquals(e2ePattern.replace("{suite}", suiteDirectory), report, "E2E report has its own canonical path");
            assertTrue(allReports.add(report), "E2E and component report paths remain distinct: " + report);
            assertReport(release, report, payload);

            String receiptPath = group.required("executionReceipt").asText();
            assertEquals(e2eReceiptPattern.replace("{suite}", suiteDirectory), receiptPath, "E2E receipt path is canonical");
            JsonNode receipt = readPayloadJson(release, receiptPath, payload);
            assertEquals("PASS", receipt.required("status").asText());
            assertEquals(group.required("protocol").asText(), receipt.required("protocol").asText());
            assertEquals(group.required("tests").asInt(), receipt.required("tests").asInt());
            assertEquals(group.required("failures").asInt(), receipt.required("failures").asInt());
            assertEquals(group.required("errors").asInt(), receipt.required("errors").asInt());
            assertEquals(group.required("skipped").asInt(), receipt.required("skipped").asInt());
            assertEquals(version, receipt.required("releaseVersion").asText());
            assertEquals(manifest.required("release").required("apkSha256").asText(), receipt.required("installedApkSha256").asText());
            assertEquals(manifest.required("source").required("sourceCommit").asText(), receipt.required("sourceCommit").asText());
            assertEquals(manifest.required("source").required("workingTreeFingerprint").asText(), receipt.required("sourceFingerprint").asText());
            assertEquals(hash(release.resolve(report)), receipt.required("reportSha256").asText());
            assertEquals(inlinePngCount(release.resolve(report)), receipt.required("inlinePngCount").asInt());
        }
    }

    private static Map<String, String> payloadHashes(JsonNode manifest) {
        Map<String, String> hashes = new HashMap<>();
        for (JsonNode file : manifest.required("integrity").required("files"))
            hashes.put(file.required("path").asText(), file.required("sha256").asText());
        return hashes;
    }

    private static JsonNode readPayloadJson(Path release, String relative, Map<String, String> payload) throws Exception {
        Path file = release.resolve(relative).normalize();
        assertTrue(file.startsWith(release) && Files.isRegularFile(file), "Receipt is a regular file inside the release payload: " + relative);
        assertEquals(hash(file), payload.get(relative), "Receipt bytes are hash-bound in the release manifest: " + relative);
        return JSON.readTree(file.toFile());
    }

    private static void assertReport(Path release, String relative, Map<String, String> payload) throws Exception {
        Path file = release.resolve(relative).normalize();
        assertTrue(file.startsWith(release) && Files.isRegularFile(file), "Extent is a regular file inside the release payload: " + relative);
        String html = Files.readString(file), visible = html.stripLeading();
        assertTrue(visible.regionMatches(true, 0, "<!doctype html>", 0, "<!doctype html>".length()) || visible.matches("(?is)^<html\\b.*"),
                "The suite artifact is rendered HTML: " + relative);
        assertTrue(inlinePngCount(file) > 0, "The canonical Extent includes inline PNG evidence: " + relative);
        assertEquals(hash(file), payload.get(relative), "Extent bytes are hash-bound in the release manifest: " + relative);
    }

    private static int inlinePngCount(Path report) throws Exception {
        return (int) INLINE_PNG.matcher(Files.readString(report)).results().count();
    }

    private static String hash(Path file) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }
}
