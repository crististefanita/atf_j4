package atf.tests.governance.documentation.release.integrity;

import atf.testkit.assertions.AuditedAssertions;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.documentation.release.integrity.fixture.ReleaseFixture;
import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class ManifestIntegrityComponentTest {
    @TempDir Path temporary;
    @ParameterizedTest @ValueSource(strings = {"missing-receipt", "schema", "kind", "time", "duplicate", "unregistered", "missing-file", "hash", "size", "self", "path"})
    void theReceiptMustDescribeExactlyTheExistingPayload(String defect) throws Exception {
        var fixture = ReleaseFixture.create(temporary);
        var receipt = (ObjectNode) fixture.manifest().path("integrity");
        var files = (ArrayNode) receipt.path("files");var first = (ObjectNode) files.get(0);
        String code;
        switch (defect) {
            case "missing-receipt" -> { fixture.manifest().remove("integrity");code = "NUMBER"; }
            case "schema" -> { receipt.put("schemaVersion", 2);code = "RECEIPT-SCHEMA"; }
            case "kind" -> { receipt.put("kind", "EXECUTED_TESTS");code = "RECEIPT-KIND"; }
            case "time" -> { receipt.put("observedUtc", "not-a-time");code = "RECEIPT-TIME"; }
            case "duplicate" -> { files.add(first.deepCopy());code = "RECEIPT-DUPLICATE"; }
            case "unregistered" -> { ReleaseFixture.write(fixture.folder().resolve("new-diagnostic.txt"), "new");code = "PAYLOAD-INVENTORY"; }
            case "missing-file" -> { Files.delete(fixture.folder().resolve(first.path("path").asText()));code = "FILE"; }
            case "hash" -> { first.put("sha256", "0".repeat(64));code = "PAYLOAD-HASH"; }
            case "size" -> { first.put("bytes", first.path("bytes").asLong() + 1);code = "PAYLOAD-SIZE"; }
            case "self" -> { first.put("path", "release-validation.json");code = "RECEIPT-SELF"; }
            case "path" -> { first.put("path", "../escape");code = "PATH"; }
            default -> throw new IllegalArgumentException(defect);
        }
        fixture.save();rejected(fixture, code);
    }
    @ParameterizedTest @ValueSource(strings = {"duplicate-json-key", "trailing-json", "empty-json"})
    void jsonSyntaxCannotSilentlyReplaceOrIgnoreDeclaredFields(String defect) throws Exception {
        var fixture = ReleaseFixture.create(temporary);var file = fixture.folder().resolve("release-validation.json");
        String original = Files.readString(file);
        String invalid = switch (defect) {
            case "duplicate-json-key" -> original.replaceFirst("\\{", "{\"qualification\":{},");
            case "trailing-json" -> original + " {}";
            case "empty-json" -> "";
            default -> throw new IllegalArgumentException(defect);
        };
        Files.writeString(file, invalid);rejected(fixture, "JSON");
    }
    @ParameterizedTest @ValueSource(strings = {"fingerprint", "duplicate-source", "empty-source", "source-hash", "source-path", "missing-commit", "invalid-commit", "report"})
    void refreshedReceiptsDoNotHideInconsistentSourceOrReportDeclarations(String defect) throws Exception {
        var fixture = ReleaseFixture.create(temporary);Path input = fixture.folder().resolve("reports/apk-inputs.json");
        var source = (ObjectNode) new ObjectMapper().readTree(input.toFile());var files = (ArrayNode) source.path("files");
        String code;
        switch (defect) {
            case "fingerprint" -> { source.put("fingerprint", "c".repeat(64));code = "SOURCE-FINGERPRINT"; }
            case "duplicate-source" -> { files.add(files.get(0).deepCopy());code = "SOURCE-DUPLICATE"; }
            case "empty-source" -> { files.removeAll();code = "SOURCE-FILES"; }
            case "source-hash" -> { ((ObjectNode) files.get(0)).put("sha256", "bad");code = "HASH-FORMAT"; }
            case "source-path" -> { ((ObjectNode) files.get(0)).put("path", "../escape");code = "PATH"; }
            case "missing-commit" -> { source.remove("sourceCommit");((ObjectNode) fixture.manifest().path("source")).remove("sourceCommit");code = "SOURCE-COMMIT"; }
            case "invalid-commit" -> { source.put("sourceCommit", 12);((ObjectNode) fixture.manifest().path("source")).put("sourceCommit", 12);code = "SOURCE-COMMIT"; }
            case "report" -> { Files.writeString(fixture.folder().resolve("reports/report.html"), "changed");code = "REPORT-HASH"; }
            default -> throw new IllegalArgumentException(defect);
        }
        new ObjectMapper().writeValue(input.toFile(), source);fixture.capture();rejected(fixture, code);
    }
    @Test void theActualRecorderCapturesOnlyHashesWithoutPromotingAQualification() throws Exception {
        var fixture = ReleaseFixture.create(temporary.resolve("spațiu pentru probă"));
        fixture.manifest().remove("integrity");fixture.save();
        var before = new ObjectMapper().readTree(fixture.folder().resolve("release-validation.json").toFile());
        AuditedAssertions.assertEquals(0, recorder(fixture.project(), ReleaseFixture.VERSION));
        var after = (ObjectNode) new ObjectMapper().readTree(fixture.folder().resolve("release-validation.json").toFile());
        AuditedAssertions.assertEquals("OBSERVED_PAYLOAD_HASHES", after.path("integrity").path("kind").asText());
        after.remove("integrity");AuditedAssertions.assertEquals(before, after);
        AuditedAssertions.assertDoesNotThrow(() -> ReleaseIntegrity.verify(fixture.project(), fixture.folder()));
    }
    @Test void anInvalidRecorderTargetLeavesTheManifestUnchanged() throws Exception {
        var fixture = ReleaseFixture.create(temporary);Path manifest = fixture.folder().resolve("release-validation.json");
        byte[] before = Files.readAllBytes(manifest);
        AuditedAssertions.assertNotEquals(0, recorder(temporary, "../outside"));
        AuditedAssertions.assertArrayEquals(before, Files.readAllBytes(manifest));
    }
    private int recorder(Path project, String version) throws Exception {
        Path script = GovernanceProject.locateProjectRoot().resolve("atf-tests/tools/record-release-integrity.mjs");
        var process = new ProcessBuilder("node", script.toString(), "--project", project.toString(), "--version", version)
                .redirectErrorStream(true).redirectOutput(temporary.resolve("recorder.log").toFile()).start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) { process.destroyForcibly();fail("The local recorder exceeded its deadline"); }
        return process.exitValue();
    }
    private static void rejected(ReleaseFixture fixture, String code) {
        var failure = AuditedAssertions.assertThrows(IOException.class, () -> ReleaseIntegrity.verify(fixture.project(), fixture.folder()));
        AuditedAssertions.assertTrue(failure.getMessage().startsWith("RELEASE-INTEGRITY-" + code), failure::toString);
    }
}
