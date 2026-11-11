package atf.tests.governance.documentation.release.rag;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@AtfComponentTest
class ReleaseRagEvaluatorComponentTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String VERSION = "1.2.0";
    @TempDir Path temporary;

    @Test void aPinnedComponentCandidateProducesCitedHumanReviewWithoutChangingInputsOrCallingE2e() throws Exception {
        Fixture fixture = fixture();
        String candidateBefore = treeHash(fixture.candidate());
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(0, exit);
        AuditedAssertions.assertEquals("READY_FOR_HUMAN_REVIEW", verdict.path("verdict").asText());
        AuditedAssertions.assertEquals("NOT_RUN", verdict.path("e2e").path("status").asText());
        AuditedAssertions.assertEquals("NONE", verdict.path("lifecycleEffect").asText());
        AuditedAssertions.assertEquals("PASS", check(verdict, "SOURCE_SNAPSHOT"));
        AuditedAssertions.assertEquals("PASS", check(verdict, "MAIN_README_SURFACE"));
        AuditedAssertions.assertEquals("PASS", check(verdict, "COMPONENT_EXTENT"));
        AuditedAssertions.assertTrue(verdict.path("checks").findValuesAsText("status").contains("PASS"));
        AuditedAssertions.assertEquals(candidateBefore, treeHash(fixture.candidate()), "Evaluator must not mutate the candidate");
        JsonNode citation = findCheck(verdict, "SOURCE_SNAPSHOT").path("evidence").get(0);
        AuditedAssertions.assertTrue(citation.path("sha256").asText().matches("[0-9a-f]{64}"));
        AuditedAssertions.assertTrue(citation.path("line").asInt() > 0);
        AuditedAssertions.assertEquals("/source", citation.path("jsonPointer").asText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"whist-lab-app-achievements", "whist-lab-app-atf-card-table-ui"})
    void aCollectionIntroducedForTheCandidateVersionCannotBeOmittedFromItsPlan(String collection) throws Exception {
        Fixture fixture = fixture();
        ObjectNode plan = (ObjectNode) JSON.readTree(fixture.plan().toFile());
        var groups = (com.fasterxml.jackson.databind.node.ArrayNode) plan.path("componentGroups");
        for (int index = groups.size() - 1; index >= 0; index--)
            if (groups.get(index).asText().equals(collection)) groups.remove(index);
        JSON.writeValue(fixture.plan().toFile(), plan);

        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("BLOCKED", verdict.path("verdict").asText());
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "COMPONENT_EXTENT"));
        AuditedAssertions.assertTrue(details(verdict, "COMPONENT_EXTENT").stream()
                .anyMatch(value -> value.contains(collection)));
    }

    @Test void changedSourceAndArtifactBytesBlockWithSeparateDiagnostics() throws Exception {
        Fixture fixture = fixture();
        Files.writeString(fixture.source().resolve("src/Game.java"), "class Changed {}\n");
        Files.writeString(fixture.candidate().resolve("WhistLab-" + VERSION + ".apk"), "tampered apk");
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("BLOCKED", verdict.path("verdict").asText());
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "SOURCE_SNAPSHOT"));
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "PAYLOAD_INTEGRITY"));
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "APK_IDENTITY"));
        AuditedAssertions.assertTrue(details(verdict, "SOURCE_SNAPSHOT").stream().anyMatch(value -> value.contains("src/Game.java") && value.contains("Hash diferit")));
        AuditedAssertions.assertTrue(details(verdict, "PAYLOAD_INTEGRITY").stream().anyMatch(value -> value.contains("PAYLOAD_MISMATCH")));
    }

    @Test void anApkDigestInThePinnedMainReadmeBlocksRagWithoutMaskingTheSourceCheck() throws Exception {
        Fixture fixture = fixture("# Whist Lab\nSHA-256: " + "a".repeat(64) + "\n");
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("PASS", check(verdict, "SOURCE_SNAPSHOT"));
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "MAIN_README_SURFACE"));
        AuditedAssertions.assertTrue(details(verdict, "MAIN_README_SURFACE").stream().anyMatch(value -> value.contains("linia 2")));
    }

    @Test void aProductionPlanCannotReuseTheDebugTestCertificate() throws Exception {
        Fixture fixture = fixture();
        ObjectNode plan = (ObjectNode) JSON.readTree(fixture.plan().toFile()); plan.put("channel", "PRODUCTION");
        JSON.writeValue(fixture.plan().toFile(), plan);
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "SIGNING_CHANNEL"));
    }

    @Test void componentReportsFromAnotherSourceInventoryCannotQualifyTheApk() throws Exception {
        Fixture fixture = fixture();
        Path inputs = fixture.candidate().resolve("reports/component/source-inputs.json");
        var componentRows = (com.fasterxml.jackson.databind.node.ArrayNode) JSON.readTree(inputs.toFile());
        for (JsonNode row : componentRows) {
            if (row.path("path").asText().equals("src/Game.java")) { ((ObjectNode) row).put("sha256", "b".repeat(64)); break; }
        }
        JSON.writeValue(inputs.toFile(), componentRows); refreshPayload(fixture.candidate());
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("PASS", check(verdict, "SOURCE_SNAPSHOT"));
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "COMPONENT_SOURCE_BINDING"));
        AuditedAssertions.assertEquals("INVALID", componentDecision(verdict, "atf-validation"));
    }

    @Test void reusesAnOriginalReceiptOnlyOutsideItsSuiteInputs() throws Exception {
        Fixture fixture = fixture();
        String added = "whist-lab-app/app/src/main/java/ro/whistlab/app/Screen.java";
        Path file = fixture.source().resolve(added);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "class Screen {}\n");
        Path manifestFile = fixture.candidate().resolve("release-validation.json");
        var manifest = (ObjectNode) JSON.readTree(manifestFile.toFile());
        Path inputsFile = fixture.candidate().resolve("reports/apk-inputs.json");
        var inputs = (ObjectNode) JSON.readTree(inputsFile.toFile());
        var rows = (com.fasterxml.jackson.databind.node.ArrayNode) inputs.path("files");
        rows.addObject().put("path", added).put("sha256", hash(file));
        var contents = new StringBuilder();
        rows.forEach(row -> contents.append(row.path("path").asText()).append('\t')
                .append(row.path("sha256").asText()).append('\n'));
        String fingerprint = hashText(contents.toString());
        inputs.put("fingerprint", fingerprint);
        ((ObjectNode) manifest.path("source")).put("workingTreeFingerprint", fingerprint);
        JSON.writeValue(inputsFile.toFile(), inputs);

        Path component = fixture.candidate().resolve("reports/component");
        Path original = Files.createDirectories(component.resolve("snapshots/original"));
        for (String name : List.of("source-inputs.json", "source-provenance.json", "source-component-run.json")) {
            Files.copy(component.resolve(name), original.resolve(name));
        }
        Path originalRunFile = original.resolve("source-component-run.json");
        var originalRun = (ObjectNode) JSON.readTree(originalRunFile.toFile());
        originalRun.put("status", "FAIL");
        originalRun.path("results").forEach(row -> {
            if (row.path("module").asText().equals("atf-tests")) ((ObjectNode) row).put("exitCode", 1);
        });
        JSON.writeValue(originalRunFile.toFile(), originalRun);
        var group = (ObjectNode) manifest.at("/qualification/auditedGroups/atf-validation");
        group.put("componentEvidenceOrigin", "original");
        capturePayload(fixture.candidate(), manifest);
        JSON.writeValue(manifestFile.toFile(), manifest);

        AuditedAssertions.assertEquals(2, evaluate(fixture, fixture.output()));
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals("PASS", check(verdict, "SOURCE_SNAPSHOT"));
        AuditedAssertions.assertEquals("REUSE", componentDecision(verdict, "atf-validation"));
        AuditedAssertions.assertEquals("REUSE", componentDecision(verdict, "atf-test-android"));
        AuditedAssertions.assertEquals("RUN", componentDecision(verdict, "whist-lab-app-display"));
        AuditedAssertions.assertEquals("RUN", componentDecision(verdict, "atf-tests"));
        AuditedAssertions.assertTrue(details(verdict, "COMPONENT_SOURCE_BINDING").stream()
                .anyMatch(value -> value.contains("APK-source-only: " + added)));
    }

    private static String componentDecision(JsonNode verdict, String group) {
        for (JsonNode entry : verdict.path("componentGroups")) {
            if (group.equals(entry.path("name").asText())) return entry.path("sourceEvidence").path("decision").asText();
        }
        throw new IllegalArgumentException("Missing group " + group);
    }

    @Test void aMissingApplicationExtentGroupCannotBeReplacedByTheGovernanceSuite() throws Exception {
        Fixture fixture = fixture();
        Path manifestFile = fixture.candidate().resolve("release-validation.json");
        ObjectNode manifest = (ObjectNode) JSON.readTree(manifestFile.toFile());
        ((ObjectNode) manifest.path("qualification").path("auditedGroups")).remove("whist-lab-app-history");
        JSON.writerWithDefaultPrettyPrinter().writeValue(manifestFile.toFile(), manifest);
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "COMPONENT_EXTENT"));
        AuditedAssertions.assertTrue(details(verdict, "COMPONENT_EXTENT").stream().anyMatch(value -> value.contains("whist-lab-app-history")));
    }

    @Test void provenanceReceiptMustBindTheSameComponentManifestAndAggregateRun() throws Exception {
        Fixture fixture = fixture();
        Path file = fixture.candidate().resolve("reports/component/source-provenance.json");
        ObjectNode provenance = (ObjectNode) JSON.readTree(file.toFile());
        provenance.put("sourceFingerprint", "b".repeat(64));
        JSON.writeValue(file.toFile(), provenance);
        refreshPayload(fixture.candidate());
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "COMPONENT_SOURCE_BINDING"));
        AuditedAssertions.assertTrue(details(verdict, "COMPONENT_SOURCE_BINDING").stream()
                .anyMatch(value -> value.contains("proveniență") && value.contains("manifestul")));
    }

    @Test void missingPrePublicationAndroidGateReceiptBlocksHumanReview() throws Exception {
        Fixture fixture = fixture();
        Path manifestFile = fixture.candidate().resolve("release-validation.json");
        ObjectNode manifest = (ObjectNode) JSON.readTree(manifestFile.toFile());
        ((ObjectNode) manifest.path("qualification").path("platforms").path("android").path("gates")).remove("apkIdentity");
        JSON.writerWithDefaultPrettyPrinter().writeValue(manifestFile.toFile(), manifest);
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "PLATFORM_GATES"));
        AuditedAssertions.assertTrue(details(verdict, "PLATFORM_GATES").stream().anyMatch(value -> value.contains("apkIdentity")));
    }

    @Test void publishedStageRequiresAReceiptForItsDeclaredAccessMode() throws Exception {
        Fixture fixture = fixture();
        Path manifestFile = fixture.candidate().resolve("release-validation.json");
        ObjectNode manifest = (ObjectNode) JSON.readTree(manifestFile.toFile());
        ((ObjectNode) manifest.path("publication")).put("state", "PUBLISHED_SITE");
        JSON.writerWithDefaultPrettyPrinter().writeValue(manifestFile.toFile(), manifest);
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "PLATFORM_GATES"));
    }

    @Test void authenticatedGithubDownloadCanSatisfyThePublishedGateWithoutPersistingCredentials() throws Exception {
        Fixture fixture = fixture();
        selectAuthenticatedPublication(fixture);
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(0, exit, JSON.writeValueAsString(verdict.path("checks")));
        AuditedAssertions.assertEquals("READY_FOR_HUMAN_REVIEW", verdict.path("verdict").asText());
        AuditedAssertions.assertEquals("PASS", check(verdict, "PLATFORM_GATES"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrongUrl", "wrongHash", "credentialLeak", "anonymousChannel"})
    void authenticatedGithubDownloadRejectsMismatchedOrSensitiveReceipts(String defect) throws Exception {
        Fixture fixture = fixture();
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

    @Test void changedUserFlowRequiresPassingE2eAndAnOmissionKeepsItsOwnStatus() throws Exception {
        Fixture fixture = fixture();
        ObjectNode plan = (ObjectNode) JSON.readTree(fixture.plan().toFile()); plan.put("userFlowChanged", true);
        JSON.writeValue(fixture.plan().toFile(), plan);
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "E2E_SCOPE"));
        AuditedAssertions.assertEquals("NOT_RUN", verdict.path("e2e").path("status").asText());
    }

    @Test void aPassingE2eReceiptBindsTheInstalledApkSourceOutcomesAndInlineExtentImages() throws Exception {
        Fixture fixture = fixture();
        selectPassingE2e(fixture);
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(0, exit, JSON.writeValueAsString(verdict.path("checks")));
        AuditedAssertions.assertEquals("READY_FOR_HUMAN_REVIEW", verdict.path("verdict").asText());
        AuditedAssertions.assertEquals("PASS", check(verdict, "E2E_SCOPE"));
        AuditedAssertions.assertTrue(findCheck(verdict, "E2E_SCOPE").path("evidence").size() >= 3);
    }

    @Test void aClaimedE2ePassNeedsAnExecutedExtentReceipt() throws Exception {
        Fixture fixture = fixture();
        selectPassingE2e(fixture);
        Path manifestFile = fixture.candidate().resolve("release-validation.json");
        ObjectNode manifest = (ObjectNode) JSON.readTree(manifestFile.toFile());
        ((ObjectNode) manifest.path("qualification").path("e2eGroups").path("android-e2e")).remove("executionReceipt");
        JSON.writerWithDefaultPrettyPrinter().writeValue(manifestFile.toFile(), manifest);
        refreshPayload(fixture.candidate());
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "E2E_SCOPE"));
        AuditedAssertions.assertFalse(details(verdict, "E2E_SCOPE").isEmpty(), details(verdict, "E2E_SCOPE").toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"installedApkSha256", "sourceCommit", "sourceFingerprint", "reportSha256", "inlinePngCount", "tests"})
    void anE2eReceiptCannotDriftFromItsApkSourceExtentOrResults(String field) throws Exception {
        Fixture fixture = fixture();
        selectPassingE2e(fixture);
        Path receiptFile = fixture.candidate().resolve("reports/e2e/android-e2e/execution.json");
        ObjectNode receipt = (ObjectNode) JSON.readTree(receiptFile.toFile());
        if (field.equals("inlinePngCount") || field.equals("tests")) receipt.put(field, receipt.path(field).asInt() + 1);
        else receipt.put(field, "f".repeat(64));
        JSON.writeValue(receiptFile.toFile(), receipt);
        refreshPayload(fixture.candidate());
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "E2E_SCOPE"));
        AuditedAssertions.assertFalse(details(verdict, "E2E_SCOPE").isEmpty(), details(verdict, "E2E_SCOPE").toString());
    }

    @Test void componentAndE2eSelectionsCannotReuseAnExtentPath() throws Exception {
        Fixture fixture = fixture();
        selectPassingE2e(fixture);
        Path manifestFile = fixture.candidate().resolve("release-validation.json");
        ObjectNode manifest = (ObjectNode) JSON.readTree(manifestFile.toFile());
        String componentReport = manifest.path("qualification").path("auditedGroups").path("atf-tests").path("report").asText();
        ((ObjectNode) manifest.path("qualification").path("e2eGroups").path("android-e2e")).put("report", componentReport);
        JSON.writerWithDefaultPrettyPrinter().writeValue(manifestFile.toFile(), manifest);
        refreshPayload(fixture.candidate());
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "E2E_SCOPE"));
        AuditedAssertions.assertFalse(details(verdict, "E2E_SCOPE").isEmpty(), details(verdict, "E2E_SCOPE").toString());
    }

    @Test void componentSelectionsCannotReuseOneCanonicalExtentForTwoSuites() throws Exception {
        Fixture fixture = fixture();
        Path manifestFile = fixture.candidate().resolve("release-validation.json");
        ObjectNode manifest = (ObjectNode) JSON.readTree(manifestFile.toFile());
        String sharedReport = manifest.path("qualification").path("auditedGroups").path("atf-tests").path("report").asText();
        ((ObjectNode) manifest.path("qualification").path("auditedGroups").path("whist-lab-app-history")).put("report", sharedReport);
        JSON.writerWithDefaultPrettyPrinter().writeValue(manifestFile.toFile(), manifest);
        refreshPayload(fixture.candidate());
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "COMPONENT_EXTENT"));
        AuditedAssertions.assertTrue(details(verdict, "COMPONENT_EXTENT").stream().anyMatch(value -> value.contains("cale Extent canonică/unică")));
    }

    @Test void evaluatorRefusesToWriteItsVerdictInsideAnyInputTree() throws Exception {
        Fixture fixture = fixture();
        Path forbidden = fixture.candidate().resolve("rag-verdict.json");
        AssertionError refusal = AuditedAssertions.assertThrows(AssertionError.class, () -> evaluate(fixture, forbidden));
        AuditedAssertions.assertTrue(refusal.getMessage().contains("OUTPUT_MUST_BE_OUTSIDE_INPUTS"));
        AuditedAssertions.assertFalse(Files.exists(forbidden));
    }

    private Fixture fixture() throws Exception { return fixture("# Whist Lab\n\n[Descarcă](release/1.2.0/WhistLab-1.2.0.apk)\n"); }

    private Fixture fixture(String readmeText) throws Exception {
        Path candidate = Files.createDirectories(temporary.resolve("candidate/release/" + VERSION));
        Path source = Files.createDirectories(temporary.resolve("source"));
        Path sourceFile = Files.createDirectories(source.resolve("src")).resolve("Game.java"); Files.writeString(sourceFile, "class Game {}\n");
        Path readmeFile = source.resolve("README.md"); Files.writeString(readmeFile, readmeText);
        String sourceHash = hash(sourceFile), readmeHash = hash(readmeFile);
        String fingerprint = hashText("README.md\t" + readmeHash + "\n" + "src/Game.java\t" + sourceHash + "\n");
        String commit = "d".repeat(40);
        Path apk = candidate.resolve("WhistLab-" + VERSION + ".apk"); Files.write(apk, "APK-FIXTURE".getBytes(StandardCharsets.UTF_8));
        String apkHash = hash(apk);
        Files.writeString(candidate.resolve(apk.getFileName() + ".sha256"), apkHash + "  " + apk.getFileName() + "\n");
        Files.createDirectories(candidate.resolve("reports"));
        Path inputManifest = candidate.resolve("reports/apk-inputs.json");
        ObjectNode inputs = JSON.createObjectNode().put("sourceCommit", commit).put("fingerprint", fingerprint);
        inputs.putArray("files").addObject().put("path", "README.md").put("sha256", readmeHash);
        ((com.fasterxml.jackson.databind.node.ArrayNode) inputs.path("files")).addObject().put("path", "src/Game.java").put("sha256", sourceHash);
        JSON.writeValue(inputManifest.toFile(), inputs);
        Path componentInputs = candidate.resolve("reports/component/source-inputs.json");
        Files.createDirectories(componentInputs.getParent());
        var componentRows = JSON.createArrayNode();
        componentRows.addObject().put("path", "README.md").put("sha256", readmeHash);
        componentRows.addObject().put("path", "src/Game.java").put("sha256", sourceHash);
        JSON.writeValue(componentInputs.toFile(), componentRows);
        String componentFingerprint = hash(componentInputs);
        Path provenanceFile = candidate.resolve("reports/component/source-provenance.json");
        ObjectNode provenance = JSON.createObjectNode().put("schemaVersion", 1).put("kind", "live-source-components")
                .put("sourceFingerprint", componentFingerprint).put("inputCount", 2).put("inputManifest", "source-inputs.json");
        JSON.writeValue(provenanceFile.toFile(), provenance);
        String provenanceFingerprint = hash(provenanceFile);
        ObjectNode run = JSON.createObjectNode().put("kind", "live-source-components").put("status", "PASS")
                .put("sourceStable", true).put("sourceFingerprint", componentFingerprint).put("inputCount", 2)
                .put("inputManifest", "source-inputs.json").put("provenanceReceiptSha256", provenanceFingerprint);
        var groups = JSON.createObjectNode(); var runResults = run.putArray("results");
        for (String module : List.of("atf-validation", "atf-test-android", "atf-tests",
                "whist-lab-app-bidding", "whist-lab-app-commands", "whist-lab-app-display", "whist-lab-app-history",
                "whist-lab-app-achievements", "whist-lab-app-atf-card-table-ui")) {
            int tests = module.equals("atf-tests") ? 3 : module.startsWith("whist-lab-app-") ? 4 : 2;
            Path groupFolder = Files.createDirectories(candidate.resolve("reports/component/" + module));
            Path report = groupFolder.resolve("component-report.html");
            Files.writeString(report, "<!doctype html><html><body>" + module
                    + " Extent component<img src=\"data:image/png;base64,iVBORw0KGgo=\"></body></html>");
            ObjectNode execution = JSON.createObjectNode().put("schemaVersion", 1).put("kind", "live-source-components")
                    .put("apkQualification", false).put("tests", tests).put("provenanceSha256", provenanceFingerprint)
                    .put("reportSha256", hash(report));
            execution.putObject("outcomes").put("PASS", tests); execution.putArray("coverageErrors"); execution.putArray("containerFailures");
            JSON.writeValue(groupFolder.resolve("execution.json").toFile(), execution);
            groups.set(module, JSON.createObjectNode().put("report", "reports/component/" + module + "/component-report.html")
                    .put("protocol", module.startsWith("whist-lab-app-") ? "GRADLE_COMPONENT_V1" : "ATF_EXTENT_COMPONENT_V1")
                    .put("tests", tests).put("failures", 0).put("errors", 0).put("skipped", 0));
            runResults.addObject().put("module", module).put("exitCode", 0).set("receipt", execution.deepCopy());
        }
        JSON.writeValue(candidate.resolve("reports/component/source-component-run.json").toFile(), run);
        Path diagnostics = Files.createDirectories(candidate.resolve("reports/diagnostics"));
        Files.writeString(diagnostics.resolve("build.log"), "BUILD SUCCESSFUL\n");
        JSON.writeValue(diagnostics.resolve("apk-identity.json").toFile(), JSON.createObjectNode().put("result", "PASS")
                .put("applicationId", "ro.whistlab.app").put("versionName", VERSION).put("versionCode", 20)
                .put("apkBytes", Files.size(apk)).put("apkSha256", apkHash));
        JSON.writeValue(diagnostics.resolve("signing.json").toFile(), JSON.createObjectNode().put("result", "PASS")
                .put("certificateSha256", "a".repeat(64)).put("upgradeCompatibility", "PASS"));

        ObjectNode release = JSON.createObjectNode().put("versionName", VERSION).put("artifactKind", "apk")
                .put("versionCode", 20).put("applicationId", "ro.whistlab.app")
                .put("apkName", apk.getFileName().toString()).put("apkRelativePath", apk.getFileName().toString())
                .put("apkBytes", Files.size(apk)).put("apkSha256", apkHash).put("certificateSha256", "a".repeat(64))
                .put("signingCertificate", "Android Debug").put("productionManagedKey", false);
        ObjectNode sourceReceipt = JSON.createObjectNode().put("sourceCommit", commit).put("workingTreeFingerprint", fingerprint)
                .put("inputManifest", "reports/apk-inputs.json")
                .put("fingerprintFormat", "SHA-256 of UTF-8 ordinal-sorted path TAB sha256 LF records");
        ObjectNode qualification = JSON.createObjectNode().put("state", "PASS"); qualification.set("auditedGroups", groups);
        ObjectNode platformGates = JSON.createObjectNode();
        platformGates.set("androidComponents", gate("reports/component/source-component-run.json"));
        platformGates.set("apkBuild", gate("reports/diagnostics/build.log"));
        platformGates.set("apkIdentity", gate("reports/diagnostics/apk-identity.json"));
        platformGates.set("signing", gate("reports/diagnostics/signing.json"));
        ObjectNode androidPlatform = JSON.createObjectNode().put("implemented", true).put("artifactKind", "apk");
        androidPlatform.set("gates", platformGates); qualification.putObject("platforms").set("android", androidPlatform);
        ObjectNode manifest = JSON.createObjectNode(); manifest.set("release", release); manifest.set("source", sourceReceipt);
        manifest.set("qualification", qualification);
        manifest.putObject("publication").put("state", "PREPARED");
        Path canonical = candidate.resolve("reports/component/atf-tests/component-report.html");
        manifest.putObject("evidenceRetention").put("canonicalReport", "reports/component/atf-tests/component-report.html")
                .put("canonicalReportSha256", hash(canonical));
        Path manifestFile = candidate.resolve("release-validation.json");
        capturePayload(candidate, manifest);
        JSON.writerWithDefaultPrettyPrinter().writeValue(manifestFile.toFile(), manifest);
        Path plan = temporary.resolve("plan.json");
        ObjectNode candidatePlan = JSON.createObjectNode().put("schemaVersion", 1).put("releaseVersion", VERSION)
                .put("channel", "TEST").put("changeRisk", "maintenanceOrRefactor").put("userFlowChanged", false);
        candidatePlan.putArray("declaredPlatforms").add("android");
        var selectedGroups = candidatePlan.putArray("componentGroups");
        for (String module : List.of("atf-validation", "atf-test-android", "atf-tests",
                "whist-lab-app-bidding", "whist-lab-app-commands", "whist-lab-app-display", "whist-lab-app-history",
                "whist-lab-app-achievements", "whist-lab-app-atf-card-table-ui")) selectedGroups.add(module);
        candidatePlan.set("e2e", JSON.createObjectNode().put("status", "NOT_RUN")
                .put("rationale", "RAG and governance-only changes; application E2E remains frozen."));
        JSON.writeValue(plan.toFile(), candidatePlan);
        Path outputDirectory = Files.createDirectories(temporary.resolve("verdict"));
        return new Fixture(candidate, source, plan, outputDirectory.resolve("rag.json"));
    }

    private static void capturePayload(Path candidate, ObjectNode manifest) throws Exception {
        ObjectNode receipt = manifest.putObject("integrity").put("schemaVersion", 1).put("kind", "OBSERVED_PAYLOAD_HASHES");
        var files = receipt.putArray("files");
        try (var paths = Files.walk(candidate)) {
            for (Path file : paths.filter(Files::isRegularFile).sorted().toList()) {
                if (file.getFileName().toString().equals("release-validation.json")) continue;
                files.addObject().put("path", candidate.relativize(file).toString().replace('\\', '/'))
                        .put("bytes", Files.size(file)).put("sha256", hash(file));
            }
        }
    }

    private static ObjectNode gate(String receipt) {
        return JSON.createObjectNode().put("status", "PASS").put("receipt", receipt);
    }

    private static void refreshPayload(Path candidate) throws Exception {
        ObjectNode manifest = (ObjectNode) JSON.readTree(candidate.resolve("release-validation.json").toFile());
        capturePayload(candidate, manifest);
        JSON.writerWithDefaultPrettyPrinter().writeValue(candidate.resolve("release-validation.json").toFile(), manifest);
    }

    private static void selectPassingE2e(Fixture fixture) throws Exception {
        Path report = fixture.candidate().resolve("reports/e2e/android-e2e/report.html");
        Files.createDirectories(report.getParent());
        Files.writeString(report, "<!doctype html><html><body><img src=\"data:image/png;base64,iVBORw0KGgo=\"></body></html>");
        ObjectNode manifest = (ObjectNode) JSON.readTree(fixture.candidate().resolve("release-validation.json").toFile());
        ObjectNode release = (ObjectNode) manifest.path("release");
        ObjectNode source = (ObjectNode) manifest.path("source");
        ObjectNode receipt = JSON.createObjectNode().put("schemaVersion", 1).put("kind", "android-signed-release-e2e")
                .put("status", "PASS").put("protocol", "ANDROID_E2E_FIXTURE")
                .put("tests", 1).put("failures", 0).put("errors", 0).put("skipped", 0)
                .put("releaseVersion", VERSION).put("installedApkSha256", release.path("apkSha256").asText())
                .put("sourceCommit", source.path("sourceCommit").asText())
                .put("sourceFingerprint", source.path("workingTreeFingerprint").asText())
                .put("inlinePngCount", 1).put("reportSha256", hash(report));
        JSON.writeValue(fixture.candidate().resolve("reports/e2e/android-e2e/execution.json").toFile(), receipt);
        ObjectNode e2eGroup = JSON.createObjectNode().put("report", "reports/e2e/android-e2e/report.html")
                .put("executionReceipt", "reports/e2e/android-e2e/execution.json")
                .put("protocol", "ANDROID_E2E_FIXTURE").put("tests", 1).put("failures", 0).put("errors", 0).put("skipped", 0);
        ((ObjectNode) manifest.path("qualification")).putObject("e2eGroups").set("android-e2e", e2eGroup);
        JSON.writerWithDefaultPrettyPrinter().writeValue(fixture.candidate().resolve("release-validation.json").toFile(), manifest);
        ObjectNode plan = (ObjectNode) JSON.readTree(fixture.plan().toFile());
        plan.put("userFlowChanged", true);
        ObjectNode e2ePlan = JSON.createObjectNode().put("status", "PASS");
        e2ePlan.putArray("groups").add("android-e2e");
        plan.set("e2e", e2ePlan);
        JSON.writeValue(fixture.plan().toFile(), plan);
        refreshPayload(fixture.candidate());
    }

    private static void selectAuthenticatedPublication(Fixture fixture) throws Exception {
        var manifestFile = fixture.candidate().resolve("release-validation.json");
        ObjectNode manifest = (ObjectNode) JSON.readTree(manifestFile.toFile());
        ObjectNode publication = (ObjectNode) manifest.path("publication");
        publication.put("state", "PUBLISHED").put("accessMode", "AUTHENTICATED").put("repository", "example/game");
        Path receiptFile = fixture.candidate().resolve("reports/distribution-verification.json");
        Files.createDirectories(receiptFile.getParent());
        String apkHash = manifest.path("release").path("apkSha256").asText();
        long apkBytes = manifest.path("release").path("apkBytes").asLong();
        String url = "https://github.com/example/game/releases/download/v" + VERSION + "/WhistLab-" + VERSION + ".apk";
        ObjectNode receipt = JSON.createObjectNode();
        receipt.putObject("authenticatedDownload").put("url", url).put("authenticated", true)
                .put("authMode", "GITHUB_ACCOUNT").put("httpStatus", 200).put("bytes", apkBytes)
                .put("sha256", apkHash).put("zipCrc", "PASS").put("channel", "GitHub API authenticated");
        JSON.writeValue(receiptFile.toFile(), receipt);
        ((ObjectNode) manifest.path("qualification").path("platforms").path("android").path("gates"))
                .set("distributionDownload", gate("reports/distribution-verification.json"));
        JSON.writerWithDefaultPrettyPrinter().writeValue(manifestFile.toFile(), manifest);
        refreshPayload(fixture.candidate());
    }

    private int evaluate(Fixture fixture, Path output) throws Exception {
        Path root = GovernanceProject.locateProjectRoot();
        Path log = temporary.resolve("rag-evaluator.log");
        Process process = new ProcessBuilder("node", root.resolve("atf-tests/tools/evaluate-release-rag.mjs").toString(),
                "--project-root", root.toString(), "--candidate-dir", fixture.candidate().toString(),
                "--source-root", fixture.source().toString(), "--plan-file", fixture.plan().toString(), "--output", output.toString())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!process.waitFor(45, TimeUnit.SECONDS)) { process.destroyForcibly(); throw new AssertionError("RAG evaluator exceeded 45 seconds"); }
        if (process.exitValue() == 1) throw new AssertionError(Files.readString(log));
        return process.exitValue();
    }

    private static String check(JsonNode verdict, String code) {
        return findCheck(verdict, code).path("status").asText();
    }
    private static List<String> details(JsonNode verdict, String code) {
        List<String> values = new java.util.ArrayList<>();
        for (JsonNode value : findCheck(verdict, code).path("details")) values.add(value.asText());
        return values;
    }
    private static JsonNode findCheck(JsonNode verdict, String code) {
        for (JsonNode row : verdict.path("checks")) if (row.path("code").asText().equals(code)) return row;
        return com.fasterxml.jackson.databind.node.NullNode.getInstance();
    }
    private static String treeHash(Path root) throws Exception {
        try (var files = Files.walk(root)) {
            String input = files.filter(Files::isRegularFile).sorted().map(file -> root.relativize(file).toString() + "\t" + hashUnchecked(file) + "\n").reduce("", String::concat);
            return hashText(input);
        }
    }
    private static String hashUnchecked(Path file) { try { return hash(file); } catch (Exception error) { throw new RuntimeException(error); } }
    private static String hash(Path file) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))); }
    private static String hashText(String value) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
    private record Fixture(Path candidate, Path source, Path plan, Path output) { }
}
