package atf.tests.governance.documentation.release.rag;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.BASELINE_COMMIT;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.JSON;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.VERSION;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.create;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.refreshPayload;
import static atf.tests.governance.documentation.release.rag.RagEvaluatorProbe.check;
import static atf.tests.governance.documentation.release.rag.RagEvaluatorProbe.details;
import static atf.tests.governance.documentation.release.rag.RagEvaluatorProbe.evaluate;
import static atf.tests.governance.documentation.release.rag.RagGithubEvidenceFixture.selectAuthenticatedPublication;

@AtfComponentTest
class ReleaseRagGithubComponentTest {
    @TempDir Path temporary;

    @Test void publishedStageRequiresAReceiptForItsDeclaredAccessMode() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        Path manifestFile = fixture.candidate().resolve("release-validation.json");
        ObjectNode manifest = (ObjectNode) JSON.readTree(manifestFile.toFile());
        ((ObjectNode) manifest.path("publication")).put("state", "PUBLISHED_SITE");
        JSON.writerWithDefaultPrettyPrinter().writeValue(manifestFile.toFile(), manifest);
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "PLATFORM_GATES"));
    }

    @Test void authenticatedGithubApiDownloadCanSatisfyThePublishedGateWithoutPersistingCredentials() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        selectAuthenticatedPublication(fixture);
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(0, exit, JSON.writeValueAsString(verdict.path("checks")));
        AuditedAssertions.assertEquals("READY_FOR_HUMAN_REVIEW", verdict.path("verdict").asText());
        AuditedAssertions.assertEquals("PASS", check(verdict, "PLATFORM_GATES"));
    }

    @Test void authenticatedBrowserMcpDownloadCanSatisfyThePublishedGateWithoutPersistingCredentials() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        selectAuthenticatedPublication(fixture, "Browser MCP authenticated");
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(0, exit, JSON.writeValueAsString(verdict.path("checks")));
        AuditedAssertions.assertEquals("READY_FOR_HUMAN_REVIEW", verdict.path("verdict").asText());
        AuditedAssertions.assertEquals("PASS", check(verdict, "PLATFORM_GATES"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrongUrl", "wrongHash", "credentialLeak", "anonymousChannel"})
    void authenticatedGithubDownloadRejectsMismatchedOrSensitiveReceipts(String defect) throws Exception {
        RagCandidateFixture fixture = create(temporary);
        selectAuthenticatedPublication(fixture);
        Path receiptFile = fixture.candidate().resolve("reports/distribution-verification.json");
        ObjectNode receipt = (ObjectNode) JSON.readTree(receiptFile.toFile());
        ObjectNode download = (ObjectNode) receipt.path("authenticatedDownload");
        switch (defect) {
            case "wrongUrl" -> download.put("url", "https://github.com/other/repo/releases/download/v" + VERSION + "/WhistLab-" + VERSION + ".apk");
            case "wrongHash" -> download.put("sha256", "b".repeat(64));
            case "credentialLeak" -> download.put("token", "must-never-be-persisted");
            case "anonymousChannel" -> download.put("channel", "anonymous HTTPS GET");
            default -> throw new IllegalArgumentException(defect);
        }
        JSON.writeValue(receiptFile.toFile(), receipt);
        refreshPayload(fixture.candidate());
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "PLATFORM_GATES"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "missingTime", "expired", "extraDraft", "orphanTag", "wrongCommit", "wrongChecksum"})
    void publishedGithubReleaseRequiresMatchingRetentionEvidence(String defect) throws Exception {
        RagCandidateFixture fixture = create(temporary);
        selectAuthenticatedPublication(fixture);
        Path file = fixture.output().resolveSibling("github-retention.json");
        var observation = (ObjectNode) JSON.readTree(file.toFile());
        switch (defect) {
            case "missing" -> Files.delete(file);
            case "missingTime" -> observation.remove("observedAtUtc");
            case "expired" -> observation.put("observedAtUtc", "2000-01-01T00:00:00.000Z");
            case "extraDraft" -> ((com.fasterxml.jackson.databind.node.ArrayNode) observation.path("releases"))
                    .addObject().put("tag", "v1.0.0").put("draft", true);
            case "orphanTag" -> ((com.fasterxml.jackson.databind.node.ArrayNode) observation.path("tags"))
                    .addObject().put("tag", "v1.0.0").put("commit", BASELINE_COMMIT);
            case "wrongCommit" -> ((ObjectNode) observation.at("/tags/0")).put("commit", BASELINE_COMMIT);
            case "wrongChecksum" -> ((ObjectNode) observation.at("/releases/0/assets/1/download")).put("sha256", "0".repeat(64));
            default -> throw new IllegalArgumentException(defect);
        }
        if (Files.exists(file)) JSON.writeValue(file.toFile(), observation);
        AuditedAssertions.assertEquals(2, evaluate(fixture, fixture.output()));
        var verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals("PASS", check(verdict, "PLATFORM_GATES"));
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "GITHUB_RETENTION"));
        AuditedAssertions.assertTrue(details(verdict, "GITHUB_RETENTION").stream().anyMatch(value -> value.contains(switch (defect) {
            case "missing" -> "RETENTION_OBSERVATION_REQUIRED";
            case "missingTime" -> "RETENTION_OBSERVATION_TIME_REQUIRED";
            case "expired" -> "RETENTION_OBSERVATION_EXPIRED";
            case "extraDraft" -> "RELEASES: unexpected";
            case "orphanTag" -> "TAGS: unexpected";
            case "wrongCommit" -> "TAG_COMMIT";
            case "wrongChecksum" -> "ASSET_DOWNLOAD";
            default -> throw new IllegalArgumentException(defect);
        })));
    }
}
