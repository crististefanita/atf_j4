package atf.tests.governance.documentation.release.download;

import atf.testkit.AtfComponentTest;
import atf.tests.distribution.ReleaseDownloadSelection;
import atf.tests.reporting.GameReportAudit;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest
class DownloadSelectionComponentTest {
    @RegisterExtension static final GameReportAudit AUDIT = new GameReportAudit();
    @Test void announcedCandidateCanDriveTheMainReadmeWithoutReplacingPublishedIdentity() throws Exception {
        var json = new ObjectMapper();
        var index = json.createObjectNode().put("currentVersion", "1.2.0").put("candidateVersion", "1.3.0");
        var manifest = json.createObjectNode();
        manifest.putObject("publication").put("state", "PREPARED").put("accessMode", "AUTHENTICATED")
                .put("targetUrl", "https://github.com/example/game/releases/download/v1.3.0/WhistLab-1.3.0.apk");
        manifest.putObject("release").put("versionName", "1.3.0").put("apkBytes", 43).put("apkSha256", "b".repeat(64));

        var advertised = ReleaseDownloadSelection.select("advertised", "example/game", index, manifest);
        assertEquals("1.3.0", advertised.version());
        assertEquals("https://github.com/example/game/releases/download/v1.3.0/WhistLab-1.3.0.apk", advertised.publicUrl());
        assertThrows(IOException.class, () -> ReleaseDownloadSelection.select("current", "example/game", index, manifest));
    }

    @Test void aPreparedCandidateCannotBorrowTheOlderPublishedVersionOrItsPassingDownload() throws Exception {
        var json = new ObjectMapper();var index = json.createObjectNode().put("currentVersion", "1.2.0");
        index.putObject("publishedDownload").put("version", "1.1.0").put("bytes", 42).put("sha256", "a".repeat(64))
                .put("url", "https://github.com/example/game/releases/download/v1.1.0/WhistLab-1.1.0.apk");
        var manifest = json.createObjectNode();manifest.putObject("publication").put("state", "PREPARED");
        manifest.putObject("release").put("versionName", "1.2.0").put("apkBytes", 43).put("apkSha256", "b".repeat(64));
        var current = ReleaseDownloadSelection.select("current", "example/game", index, manifest);
        assertEquals("1.2.0", current.version(), "The prepared current target uses its own identity");
        assertEquals("b".repeat(64), current.sha256(), "No fallback to the previous APK digest");
        assertEquals(43L, current.bytes(), "The complete-download expectation belongs to the current APK");
        var failure = assertThrows(IOException.class, () -> ReleaseDownloadSelection.select("published", "example/game", index, manifest));
        assertEquals("RELEASE-DOWNLOAD-SCOPE", failure.getMessage(), "An older release is never an alternative current acceptance target");
    }
    @Test void fractionalSizesAndDifferentCurrentIdentitiesRemainExplicitErrors() throws Exception {
        var json = new ObjectMapper();var index = json.createObjectNode().put("currentVersion", "1.2.0");
        var manifest = json.createObjectNode();manifest.putObject("publication").put("state", "PUBLISHED");
        var release = manifest.putObject("release").put("versionName", "1.1.0").put("apkBytes", 42).put("apkSha256", "a".repeat(64));
        var mismatch = assertThrows(IOException.class, () -> ReleaseDownloadSelection.select("current", "example/game", index, manifest));
        assertEquals("RELEASE-DOWNLOAD-CURRENT-IDENTITY", mismatch.getMessage(), "Wrong source version refused");
        release.put("versionName", "1.2.0").put("apkBytes", 42.75);
        var fractional = assertThrows(IOException.class, () -> ReleaseDownloadSelection.select("current", "example/game", index, manifest));
        assertEquals("RELEASE-DOWNLOAD-NUMBER-apkBytes", fractional.getMessage(), "No numeric truncation of the expected byte count");
    }
}
