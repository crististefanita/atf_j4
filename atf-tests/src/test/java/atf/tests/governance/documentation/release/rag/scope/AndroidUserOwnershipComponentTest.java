package atf.tests.governance.documentation.release.rag.scope;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.support.GovernanceProject;
import java.nio.file.Files;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import static atf.testkit.assertions.AuditedAssertions.assertTrue;

@AtfComponentTest
class AndroidUserOwnershipComponentTest {
    @Test void durableRecoveryPreservesOriginalAndForeignUsersWithMockTransport() throws Exception {
        String output = NodeReleaseContracts.runContracts(Duration.ofSeconds(100), "android-user-ownership.test.mjs");
        assertTrue(output.contains("16 durable ownership outcomes"), output);
        assertTrue(output.contains("Recovery adapter:"), output);
    }

    @Test void creationIntentAndReturnedIdentityPrecedePackageRegistration() throws Exception {
        String script = Files.readString(GovernanceProject.locateProjectRoot()
                .resolve("scripts/Invoke-AtfAndroidDisposableUser.ps1"));
        int intent = script.indexOf("$ownership = New-AtfUserOwnership");
        int creation = script.indexOf("$created = Invoke-DeviceShell @('pm','create-user'");
        int identity = script.indexOf("$ownership = Set-AtfOwnedUserId");
        int registration = script.indexOf("$registration = Invoke-AtfVerifiedPackageRegistration");
        assertTrue(intent >= 0 && creation > intent && identity > creation && registration > identity,
                "Persist ownership intent before creation and the returned ID before package registration");
        assertTrue(script.contains("$ownershipCreated -and"), "A pre-existing journal does not authorize cleanup on failed creation");
    }
}
