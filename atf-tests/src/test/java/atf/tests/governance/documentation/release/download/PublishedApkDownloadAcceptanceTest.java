package atf.tests.governance.documentation.release.download;

import atf.reporting.audit.AssertionAuditReport;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import atf.testkit.AtfIntegrationTest;
import atf.testkit.assertions.AssertionValueDisplay;
import atf.testkit.assertions.RecordedAssertions;
import atf.tests.distribution.GitHubApkDownload;
import atf.tests.distribution.ReleaseDownloadSelection;
import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

/** Opt-in network acceptance; the offline suite explicitly excludes this channel. */
@AtfIntegrationTest
@Tag("release-download-live")
@EnabledIfSystemProperty(named = "atf.release.download.enabled", matches = "true")
class PublishedApkDownloadAcceptanceTest {
    @TempDir Path temporary;
    @Test @Timeout(90) void theDeclaredPublishedApkCanBeDownloadedCompletely() throws Exception {
        Path root = GovernanceProject.locateProjectRoot();var json = new ObjectMapper();
        var index = json.readTree(root.resolve("release/index.json").toFile());String current = index.required("currentVersion").asText();
        var manifest = json.readTree(root.resolve("release/" + current + "/release-validation.json").toFile());
        String scope = System.getProperty("atf.release.download.scope", "current");
        Path directory = Path.of(System.getProperty("atf.release.download.report-directory",
                Path.of(System.getProperty("java.io.tmpdir"), "whist-release-download").toString()));
        if (!directory.isAbsolute()) throw new IllegalArgumentException("Download report directory must be absolute");
        Files.createDirectories(directory);Path report = directory.resolve("download-" + scope + "-" + java.util.UUID.randomUUID() + ".html");
        var settings = AtfReportConfigurationLoader.load().reportingSettings();var redactor = new atf.reporting.security.SensitiveDataRedactor();
        var rows = new RecordedAssertions(new AssertionValueDisplay(settings.assertionMaxDisplayCharacters(), redactor::redactField));
        try (var session = new ExtentReportSession(report, settings);
             var scenario = session.startScenario(getClass().getSimpleName(), getClass().getPackageName(),
                     "Complete GitHub APK download — " + scope, List.of("integration", "github", "network-live"));
             var phase = scenario.startPhase("TEST BODY")) {
            phase.detail("Requested scope", scope);phase.detail("Current local version", current);
            phase.detail("Local publication state", manifest.path("publication").path("state").asText());
            Throwable failure = null;
            try {
                String repository = GovernanceProject.policy(root).required("publicRepository").asText().replace("https://github.com/", "");
                var target = ReleaseDownloadSelection.select(scope, repository, index, manifest);
                String token = System.getenv("GH_TOKEN");if (token == null || token.isBlank()) token = System.getenv("GITHUB_TOKEN");
                phase.observation(new ReportObservation("Declared download identity", List.of(new ReportField("Scope", scope),
                        new ReportField("Version", target.version()), new ReportField("Public asset URL", target.publicUrl()),
                        new ReportField("Channel", token == null || token.isBlank() ? "GitHub API anonymous" : "GitHub API authenticated"),
                        new ReportField("Expected bytes", Long.toString(target.bytes())), new ReportField("Expected SHA-256", target.sha256())),
                        List.of(), List.of()));
                var proof = new GitHubApkDownload(token).download(target, temporary);
                rows.equals("Downloaded version", target.version(), proof.version());
                rows.equals("Actual completed bytes", target.bytes(), proof.bytes());
                rows.equals("Actual SHA-256", target.sha256(), proof.sha256());
                rows.equals("Every ZIP entry CRC", "PASS", proof.zipCrc());
                rows.equals("Binary response HTTP", 200, proof.httpStatus());
            } catch (Exception | AssertionError original) { failure = original; }
            try {
                if (failure != null) phase.diagnostic(atf.reporting.model.diagnostic.ReportPhaseDiagnostic.Kind.EXECUTION_FAILURE,
                        redactor.redactText(failure.getClass().getSimpleName() + ": " + failure.getMessage()));
                if (!rows.checks().isEmpty()) AssertionAuditReport.publishStep(phase, "Download assertions", rows.checks().stream()
                        .map(row -> new AtfAssertion(row.name(), row.expected(), row.actual(), row.passed())).toList(), AtfReportStatus.INFO);
                else if (failure == null) throw new IllegalStateException("Download acceptance has no captured comparisons");
                else phase.detail("Comparison state", "Download stopped before the declared identity could be compared");
                AtfReportStatus status = failure == null ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
                phase.complete(status);scenario.complete(status);
            } catch (Exception | AssertionError publication) {
                if (failure == null) failure = publication;else if (failure != publication) failure.addSuppressed(publication);
                try { phase.complete(AtfReportStatus.FAIL);scenario.complete(AtfReportStatus.FAIL); }
                catch (RuntimeException statusFailure) { if (failure != statusFailure) failure.addSuppressed(statusFailure); }
            }
            if (failure instanceof Exception original) throw original;
            if (failure instanceof AssertionError original) throw original;
        }
    }
}
