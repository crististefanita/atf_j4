package atf.tests.governance.documentation.release;

import atf.testkit.assertions.AuditedAssertions;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.documentation.release.integrity.ReleaseIntegrity;
import atf.tests.governance.support.GovernanceProject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class ReleaseIntegrityGovernanceTest {
    @Test void registeredReleasesKeepTheirActualApkManifestPayloadAndSuccessfulJunitPopulation() throws Exception {
        var project = GovernanceProject.locateProjectRoot();
        var folders = ReleaseArtifactCatalog.folders(project);
        AuditedAssertions.assertFalse(folders.isEmpty());
        for (var folder : folders) AuditedAssertions.assertDoesNotThrow(() -> ReleaseIntegrity.verify(project, folder), folder.toString());
    }
}
