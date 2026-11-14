package atf.android.e2e.release.isolation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Verifies the data-isolation boundary before a signed-app test can change a match. */
public final class ReleaseTestDataBoundary {
    static final String PACKAGE = "ro.whistlab.app";

    public static void verify(Path run, JsonNode setup) throws Exception {
        String mode = setup.path("dataMode").asText("BACKUP_AND_RESTORE");
        if ("BACKUP_AND_RESTORE".equals(mode)) {
            ReleaseBackupContract.verify(run, setup);
            return;
        }
        if (!"DISPOSABLE_SECONDARY_USER".equals(mode))
            throw new IllegalStateException("Unknown signed-test data boundary: " + mode);

        if (setup.path("backupScopeValidated").asBoolean(false) || setup.path("restoreRequired").asBoolean(false))
            throw new IllegalStateException("Disposable-user isolation cannot claim backup restoration");
        if (!"data-isolation-ready.json".equals(setup.path("dataIsolationReceipt").asText()))
            throw new IllegalStateException("A canonical disposable-user setup receipt is required");

        Path receipt = run.resolve("data-isolation-ready.json");
        if (Files.isSymbolicLink(receipt) || !Files.isRegularFile(receipt))
            throw new IllegalStateException("Disposable-user setup receipt must be a regular file");
        byte[] bytes = Files.readAllBytes(receipt);
        String expectedHash = setup.path("dataIsolationReceiptSha256").asText();
        String actualHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        if (!expectedHash.matches("[0-9a-f]{64}") || !expectedHash.equals(actualHash))
            throw new IllegalStateException("Disposable-user receipt differs from the runner hash");

        JsonNode isolation = new ObjectMapper().readTree(bytes);
        int originalUser = isolation.path("originalUserId").asInt(-1);
        int testUser = isolation.path("testUserId").asInt(-1);
        if (isolation.path("schemaVersion").asInt() != 1
                || !"READY".equals(isolation.path("status").asText())
                || !PACKAGE.equals(isolation.path("applicationId").asText())
                || !"ANDROID_SECONDARY_USER".equals(isolation.path("scope").asText())
                || originalUser < 0 || testUser <= 0 || testUser == originalUser
                || setup.path("testUserId").asInt(-1) != testUser
                || !isolation.path("createdByRun").asBoolean(false)
                || !isolation.path("packageWasInstalledBefore").isBoolean()
                || isolation.path("packageWasInstalledBefore").asBoolean()
                || !isolation.path("originalPackageWasInstalledBefore").isBoolean()
                || !setup.path("originalPackageWasInstalledBefore").isBoolean()
                || setup.path("originalPackageWasInstalledBefore").asBoolean()
                        != isolation.path("originalPackageWasInstalledBefore").asBoolean()
                || isolation.path("screenOffTimeoutMs").asInt(-1) != 2147483647
                || !isolation.path("screenUnlocked").isBoolean()
                || !isolation.path("screenUnlocked").asBoolean()
                || !isolation.path("cleanupRequired").asBoolean(false)
                || !isolation.path("accountsAdded").isBoolean()
                || isolation.path("accountsAdded").asBoolean())
            throw new IllegalStateException("Disposable-user receipt does not prove a fresh, isolated package scope");
    }

    private ReleaseTestDataBoundary() { }
}
