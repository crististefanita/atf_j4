package atf.tests.governance.documentation.release.versioning;

import atf.android.evidence.component.ComponentInputs;
import atf.testkit.AtfComponentTest;
import atf.tests.distribution.ApkDownloadTarget;
import atf.tests.distribution.ReleaseVersion;
import atf.tests.governance.documentation.release.rag.scope.NodeReleaseContracts;
import atf.tests.reporting.GameReportAudit;
import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.util.regex.Pattern;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest
class ReleaseVersionComponentTest {
    @org.junit.jupiter.api.extension.RegisterExtension
    static final GameReportAudit AUDIT = new GameReportAudit();

    @Test void theApplicationUsesTheDeclaredTwoPartReleaseConvention(ComponentInputs inputs) throws Exception {
        var root = GovernanceProject.locateProjectRoot();
        var policy = new ObjectMapper().readTree(root.resolve("config/release-platform-rag-policy.json").toFile())
                .required("releaseVersioning");
        var matcher = Pattern.compile("versionName\\s+\"([^\"]+)\"").matcher(
                Files.readString(root.resolve("whist-lab-app/app/build.gradle")));
        assertTrue(matcher.find(), "The application declares its delivery version");
        String version = matcher.group(1);
        inputs.add("Application version", version);
        inputs.add("Version policy", policy.toPrettyString());
        assertEquals("MAJOR.MINOR", policy.required("newReleaseFormat").asText());
        assertTrue(policy.required("retainHistoricalPatchNames").asBoolean());
        assertTrue(ReleaseVersion.atLeast(version, policy.required("sinceRelease").asText()));
        assertTrue(version.matches(policy.required("newReleasePattern").asText()), "New application releases use two numeric components");
    }

    @ParameterizedTest
    @CsvSource({"0.22,0.21.13,true", "0.21.13,0.22,false", "0.22,0.22.0,true",
            "0.22.0,0.22,true", "0.22,0.22.1,false", "0.23,0.22.99,true",
            "0.9,0.10,false", "1.0,0.99.99,true", "0.9007199254740993,0.9007199254740992,true"})
    void numericOrderPreservesHistoricalPatchPrecision(String version, String minimum, boolean expected,
                                                       ComponentInputs inputs) {
        inputs.add("Version comparison", version + " >= " + minimum);
        assertEquals(expected, ReleaseVersion.atLeast(version, minimum));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.022", "00.22", "0.22.00", "0.22.1.1", "../0.22", "0.22/asset", " 0.22"})
    void invalidIdentitiesCannotConstructDownloadPaths(String version, ComponentInputs inputs) {
        inputs.add("Rejected release identity", version);
        assertFalse(ReleaseVersion.isValid(version));
        assertThrows(IllegalArgumentException.class, () -> new ApkDownloadTarget("example/game", version, 1, "a".repeat(64)));
    }

    @Test void absentVersionCannotBecomeADownloadIdentity(ComponentInputs inputs) {
        inputs.add("Absent release name", "null");
        assertFalse(ReleaseVersion.atLeast(null, "0.22"));
        assertFalse(ReleaseVersion.atLeast("0.22", null));
        assertThrows(IllegalArgumentException.class, () -> new ApkDownloadTarget("example/game", null, 1, "a".repeat(64)));
    }

    @Test void releaseAdaptersAcceptTwoPartNamesAndRejectForeignAuditCommits(ComponentInputs inputs) throws Exception {
        var target = new ApkDownloadTarget("example/game", "0.22", 1, "a".repeat(64));
        assertEquals("https://github.com/example/game/releases/download/v0.22/WhistLab-0.22.apk", target.publicUrl());
        String output = NodeReleaseContracts.runContracts(Duration.ofMinutes(2),
                value -> inputs.add("Original release-adapter comparisons", value),
                "release-version.test.mjs", "publication-audit.test.mjs");
        assertTrue(output.contains("two-part publication keeps the exact APK name and checksum"), output);
        assertTrue(output.contains("a source-bound qualified audit is accepted before publication"), output);
    }
}
