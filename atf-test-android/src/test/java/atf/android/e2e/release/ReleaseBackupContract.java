package atf.android.e2e.release;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Verifies the runner's immutable backup receipt before any match-changing UI action. */
final class ReleaseBackupContract {
    static void verify(Path run, JsonNode setup) throws Exception {
        Path backup = run.resolve("app-data-before.tar");
        for (String flag : new String[] {"backupScopeValidated", "restoreRequired"}) {
            if (!setup.path(flag).isBoolean() || !setup.path(flag).booleanValue())
                throw new IllegalStateException("Verified backup scope and restoration are required: " + flag);
        }
        if (Files.isSymbolicLink(backup) || !Files.isRegularFile(backup) || Files.size(backup) == 0)
            throw new IllegalStateException("A nonempty regular backup archive is required");
        String expected = setup.path("backupSha256").asText();
        String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(backup)));
        if (!expected.matches("[0-9a-f]{64}") || !expected.equals(actual))
            throw new IllegalStateException("Backup bytes differ from the verified runner receipt");
    }
    private ReleaseBackupContract() { }
}
