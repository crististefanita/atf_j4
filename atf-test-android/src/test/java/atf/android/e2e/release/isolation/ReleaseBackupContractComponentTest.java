package atf.android.e2e.release.isolation;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@AtfComponentTest
class ReleaseBackupContractComponentTest {
    @TempDir Path run;
    @Test void validatedReceiptAcceptsExactlyTheRetainedBytesWithoutChangingThem() throws Exception {
        var receipt = fixture(); var before = Files.readAllBytes(run.resolve("app-data-before.tar"));
        AuditedAssertions.assertDoesNotThrow(() -> ReleaseBackupContract.verify(run, receipt));
        AuditedAssertions.assertArrayEquals(before, Files.readAllBytes(run.resolve("app-data-before.tar")));
    }
    @Test void unknownFailedAndTextualScopeChecksCannotBeMistakenForAnEmptySuccessfulBackup() throws Exception {
        var good = fixture();
        for (String key : new String[] {"backupScopeValidated", "restoreRequired"}) {
            var missing = good.deepCopy(); missing.remove(key);
            AuditedAssertions.assertThrows(IllegalStateException.class, () -> ReleaseBackupContract.verify(run, missing));
            var failed = good.deepCopy().put(key, false);
            AuditedAssertions.assertThrows(IllegalStateException.class, () -> ReleaseBackupContract.verify(run, failed));
            var text = good.deepCopy().put(key, "true");
            AuditedAssertions.assertThrows(IllegalStateException.class, () -> ReleaseBackupContract.verify(run, text));
        }
    }
    @Test void changedBytesAndMalformedDigestsAreRejectedBeforeTheDeviceIsUsed() throws Exception {
        var receipt = fixture();
        for (String digest : new String[] {"", "bad", "0".repeat(64)}) {
            var invalid = receipt.deepCopy().put("backupSha256", digest);
            AuditedAssertions.assertThrows(IllegalStateException.class, () -> ReleaseBackupContract.verify(run, invalid));
        }
        Files.writeString(run.resolve("app-data-before.tar"), "changed bytes");
        AuditedAssertions.assertThrows(IllegalStateException.class, () -> ReleaseBackupContract.verify(run, receipt));
    }
    @Test void missingAndZeroLengthBackupsAreRejected() throws Exception {
        var receipt = fixture();
        Path absent = Files.createDirectory(run.resolve("no-backup"));
        AuditedAssertions.assertThrows(IllegalStateException.class, () -> ReleaseBackupContract.verify(absent, receipt));
        Files.write(run.resolve("app-data-before.tar"), new byte[0]);
        AuditedAssertions.assertThrows(IllegalStateException.class, () -> ReleaseBackupContract.verify(run, receipt));
    }
    @Test void disposableSecondaryUserRequiresFreshPackageScopeAndExactReceiptHash() throws Exception {
        var setup = disposableUserFixture();
        AuditedAssertions.assertDoesNotThrow(() -> ReleaseTestDataBoundary.verify(run, setup));

        for (String field : new String[] {"createdByRun", "cleanupRequired"}) {
            assertReceiptMutationRejected(setup, field, false);
        }
        assertReceiptMutationRejected(setup, "packageWasInstalledBefore", true);
        assertReceiptMutationRejected(setup, "originalPackageWasInstalledBefore", true);
        assertReceiptMutationRejected(setup, "screenUnlocked", false);
        assertReceiptMutationRejected(setup, "screenOffTimeoutMs", 30000);
        assertReceiptMutationRejected(setup, "accountsAdded", true);
        assertReceiptMutationRejected(setup, "testUserId", 0);
        assertReceiptMutationRejected(setup, "applicationId", "other.app");
        var wrongHash = setup.deepCopy().put("dataIsolationReceiptSha256", "0".repeat(64));
        AuditedAssertions.assertThrows(IllegalStateException.class, () -> ReleaseTestDataBoundary.verify(run, wrongHash));
        var claimsRestore = setup.deepCopy().put("restoreRequired", true);
        AuditedAssertions.assertThrows(IllegalStateException.class, () -> ReleaseTestDataBoundary.verify(run, claimsRestore));
        var unknown = setup.deepCopy().put("dataMode", "CLEAR_APP_DATA");
        AuditedAssertions.assertThrows(IllegalStateException.class, () -> ReleaseTestDataBoundary.verify(run, unknown));
    }

    @Test void disposableUserReceiptCannotBeMissingOrReplacedAfterSetup() throws Exception {
        var setup = disposableUserFixture();
        Path receipt = run.resolve("data-isolation-ready.json");
        Files.delete(receipt);
        AuditedAssertions.assertThrows(IllegalStateException.class, () -> ReleaseTestDataBoundary.verify(run, setup));
        Files.writeString(receipt, "{}");
        AuditedAssertions.assertThrows(IllegalStateException.class, () -> ReleaseTestDataBoundary.verify(run, setup));
    }

    private ObjectNode disposableUserFixture() throws Exception {
        byte[] bytes = new ObjectMapper().writeValueAsBytes(Map.ofEntries(
                Map.entry("schemaVersion", 1), Map.entry("status", "READY"),
                Map.entry("applicationId", ReleaseTestDataBoundary.PACKAGE),
                Map.entry("scope", "ANDROID_SECONDARY_USER"), Map.entry("originalUserId", 0),
                Map.entry("testUserId", 10), Map.entry("createdByRun", true),
                Map.entry("packageWasInstalledBefore", false),
                Map.entry("originalPackageWasInstalledBefore", false), Map.entry("cleanupRequired", true),
                Map.entry("screenOffTimeoutMs", 2147483647), Map.entry("screenUnlocked", true),
                Map.entry("accountsAdded", false)));
        Files.write(run.resolve("data-isolation-ready.json"), bytes);
        return new ObjectMapper().createObjectNode().put("dataMode", "DISPOSABLE_SECONDARY_USER")
                .put("dataIsolationReceipt", "data-isolation-ready.json")
                .put("dataIsolationReceiptSha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)))
                .put("backupScopeValidated", false).put("restoreRequired", false).put("testUserId", 10)
                .put("originalPackageWasInstalledBefore", false);
    }

    private void assertReceiptMutationRejected(ObjectNode setup, String field, Object replacement) throws Exception {
        var mapper = new ObjectMapper();
        ObjectNode isolation = (ObjectNode) mapper.readTree(Files.readAllBytes(run.resolve("data-isolation-ready.json")));
        isolation.set(field, mapper.valueToTree(replacement));
        byte[] changed = mapper.writeValueAsBytes(isolation);
        Files.write(run.resolve("data-isolation-ready.json"), changed);
        var matchingHash = setup.deepCopy().put("dataIsolationReceiptSha256",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(changed)));
        AuditedAssertions.assertThrows(IllegalStateException.class, () -> ReleaseTestDataBoundary.verify(run, matchingHash));
        disposableUserFixture();
    }
    private ObjectNode fixture() throws Exception {
        byte[] bytes = "component fixture: byte-identity contract, not TAR format qualification".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(run.resolve("app-data-before.tar"), bytes);
        return new ObjectMapper().createObjectNode().put("backupScopeValidated", true).put("restoreRequired", true)
                .put("backupSha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
    }
}
