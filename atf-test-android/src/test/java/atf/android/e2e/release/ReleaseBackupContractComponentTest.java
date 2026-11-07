package atf.android.e2e.release;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
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
    private ObjectNode fixture() throws Exception {
        byte[] bytes = "component fixture: byte-identity contract, not TAR format qualification".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(run.resolve("app-data-before.tar"), bytes);
        return new ObjectMapper().createObjectNode().put("backupScopeValidated", true).put("restoreRequired", true)
                .put("backupSha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
    }
}
