package atf.tests.governance.documentation.release;

import atf.testkit.AtfComponentTest;
import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static atf.testkit.assertions.AuditedAssertions.*;

/** Contract tests for platform-separated, read-only release evidence consumed by a future RAG adapter. */
@AtfComponentTest
class ReleaseRagPlatformGovernanceTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void policyKeepsAndroidAndIosArtifactsAndNativeGatesDistinct() throws Exception {
        JsonNode policy = policy();
        assertEquals(List.of("apk"), strings(policy.path("platforms").path("android").path("artifactKinds")));
        assertEquals(List.of("ipa", "xcarchive"), strings(policy.path("platforms").path("ios").path("artifactKinds")));
        assertTrue(strings(policy.path("platforms").path("android").path("requiredGates")).contains("apkBuild"));
        assertTrue(strings(policy.path("platforms").path("ios").path("requiredGates")).containsAll(
                List.of("xcodeSimulator", "iosArtifactIdentity", "iosDistribution")));
        assertFalse(policy.path("rules").path("androidEvidenceSatisfiesIos").asBoolean());
    }

    @Test void candidateLifecycleKeepsBuildAndPublicationOutsideRagAndRequiresAnExactMainSnapshot() throws Exception {
        JsonNode lifecycle = policy().path("candidateLifecycle");
        assertEquals(List.of("TEMP_SOURCE_PINNED", "TEMP_QUALIFIED", "READY_FOR_HUMAN_REVIEW", "PROMOTED_TO_MAIN", "PUBLISHED"),
                strings(lifecycle.path("states")));
        assertTrue(lifecycle.path("candidateArtifactsOutsideCheckoutUntilPromotion").asBoolean());
        assertTrue(strings(lifecycle.path("promotionRequires")).containsAll(List.of(
                "aggregateStatus=READY_FOR_HUMAN_REVIEW", "mainTreeSha256EqualsQualifiedSnapshotSha256")));
        assertTrue(strings(lifecycle.path("publicationRequires")).containsAll(List.of(
                "promotedCommitMatchesQualifiedSnapshot", "publishedApkSha256EqualsQualifiedApkSha256",
                "anonymousDownloadSha256EqualsPublishedApkSha256")));
        assertFalse(lifecycle.path("ragMayAdvanceLifecycle").asBoolean());
        assertTrue(lifecycle.path("ragOutputOutsideCheckout").asBoolean());
    }

    @Test void componentAndE2eReportsUseAtfExtentAndKeepJUnitOnlyAsATemporaryOracle() throws Exception {
        JsonNode reporting = policy().path("reportingContract");
        assertEquals("atf-reporting", reporting.path("owner").asText());
        assertEquals("Extent", reporting.path("renderer").asText());
        assertTrue(reporting.path("oneCanonicalHtmlPerExecutedSuiteSelection").asBoolean());
        assertTrue(reporting.path("componentAndE2eScopesRemainDistinct").asBoolean());
        assertTrue(reporting.path("exactJUnitIdentityAndOutcomeParity").asBoolean());
        assertEquals("temporary-independent-oracle", reporting.path("junitXmlRole").asText());
        assertFalse(reporting.path("duplicateJUnitXmlInFinalRelease").asBoolean());
        assertTrue(reporting.path("capturedImagesMustBeInlineBase64").asBoolean());
        assertTrue(reporting.path("reportBindsQualifiedSourceSnapshot").asBoolean());
    }

    @Test void apkSigningGateDistinguishesTestDistributionFromProduction() throws Exception {
        JsonNode signing = policy().path("signingChannels");
        assertTrue(signing.path("channelMustBeDeclared").asBoolean());
        assertTrue(signing.path("testDistributionMayUseDebugCertificate").asBoolean());
        assertTrue(signing.path("productionDistributionRequiresManagedKey").asBoolean());
        assertTrue(signing.path("allChannelsRequireCertificateFingerprintAndUpgradeCompatibility").asBoolean());
    }

    @Test void executableRagIsReadOnlyCitedAndKeepsUnchangedApplicationE2eFrozen() throws Exception {
        JsonNode evaluator = policy().path("ragEvaluator");
        assertEquals("atf-tests/tools/evaluate-release-rag.mjs", evaluator.path("entrypoint").asText());
        assertEquals("ATF_RELEASE_RAG_VERDICT", evaluator.path("outputSchema").asText());
        assertTrue(evaluator.path("outputMustBeOutsideInputs").asBoolean());
        assertEquals("atf-tests", evaluator.path("requiredComponentGroup").asText());
        assertEquals(List.of("atf-validation", "atf-test-android", "atf-tests",
                "whist-lab-app-bidding", "whist-lab-app-commands", "whist-lab-app-display", "whist-lab-app-history"),
                strings(evaluator.path("requiredComponentGroupsByPlatform").path("android")));
        assertEquals(List.of("androidComponents", "apkBuild", "apkIdentity", "signing"),
                strings(evaluator.path("platformGates").path("androidBeforeHumanReview")));
        assertEquals(List.of("publicDownload"), strings(evaluator.path("platformGates").path("androidAfterPublication")));
        assertTrue(evaluator.path("platformGates").path("gateReceiptMustBeHashBound").asBoolean());
        assertEquals("reports/component/source-inputs.json", evaluator.path("componentSourceBinding").path("inputManifest").asText());
        assertEquals("reports/component/{group}/execution.json", evaluator.path("componentSourceBinding").path("executionReceiptPattern").asText());
        assertTrue(evaluator.path("componentSourceBinding").path("sourceSetMustEqualReleaseInputManifest").asBoolean());
        assertTrue(evaluator.path("componentSourceBinding").path("eachExtentMustMatchExecutionReceipt").asBoolean());
        assertTrue(strings(evaluator.path("citationsRequire")).containsAll(List.of("path", "sha256", "jsonPointerAndLineForJson")));
        assertTrue(evaluator.path("doesNotInspectGitIndexOrExecuteGit").asBoolean());
        assertTrue(evaluator.path("doesNotPerformPromotionOrPublication").asBoolean());
        assertTrue(evaluator.path("e2e").path("notRunNeverMeansPass").asBoolean());
        assertTrue(evaluator.path("e2e").path("changedUserFlowRequiresPass").asBoolean());
        assertTrue(policy().path("suiteQualification").path("currentApplicationE2eMayBeFrozenWhenNoUserFlowChanges").asBoolean());
        Path root = GovernanceProject.locateProjectRoot();
        String cli = Files.readString(root.resolve(evaluator.path("entrypoint").asText()));
        assertFalse(cli.contains("node:child_process"));
        assertFalse(cli.contains("execSync("));
        assertFalse(cli.contains("spawn("));
    }

    @Test void changeRiskSelectsCoverageInsteadOfTheVersionLabel() throws Exception {
        JsonNode scope = policy().path("suiteQualification");
        assertTrue(scope.path("scopeComesFromChangedContractsAndRisk").asBoolean());
        assertFalse(scope.path("versionLabelMayReduceRequiredCoverage").asBoolean());
        assertTrue(scope.path("e2eNotSelectedRequiresRiskRationale").asBoolean());
        assertTrue(strings(scope.path("profiles").path("maintenanceOrRefactor")).containsAll(
                List.of("affectedUnitOrComponent", "documentationAndGovernance")));
        assertTrue(strings(scope.path("profiles").path("behaviorChange")).contains("targetedE2eWhenUserFlowChanges"));
        assertTrue(strings(scope.path("profiles").path("structuralOrReleaseChange")).containsAll(
                List.of("broadUnitAndComponent", "riskSelectedE2e", "releaseArtifactAndDownload")));
    }

    @Test void releaseGuideExplainsTempQualificationBeforePromotionAndCanonicalReporting() throws Exception {
        Path root = GovernanceProject.locateProjectRoot();
        String ragGuide = Files.readString(root.resolve("docs/feedback/ATF_Release_RAG.md"));
        String qualificationGuide = Files.readString(root.resolve("docs/testing/ATF_ReleaseQualification.md"));
        String evaluator = Files.readString(root.resolve("atf-tests/tools/evaluate-release-rag.mjs"));
        assertTrue(ragGuide.contains("## Ciclul candidatului"));
        assertTrue(ragGuide.contains("READY_FOR_HUMAN_REVIEW"));
        assertTrue(ragGuide.contains("snapshot-ul calificat"));
        assertTrue(ragGuide.contains("Fiecare selecție are propriul HTML Extent și receipt"));
        assertTrue(ragGuide.contains("Evaluatorul RAG este acum un CLI read-only"));
        assertTrue(ragGuide.contains("evaluate-release-rag.mjs"));
        assertTrue(ragGuide.contains("nu verifică indexul Git"));
        assertTrue(qualificationGuide.contains("ATF_Release_RAG.md"));
        assertTrue(qualificationGuide.contains("JUnit-ul temporar"));
        assertTrue(qualificationGuide.contains("--plan-file"));
        assertTrue(qualificationGuide.contains("`NOT_RUN`"));
        assertTrue(qualificationGuide.contains("nu devine rezultat pozitiv"));
        assertTrue(qualificationGuide.contains("`whist-lab-app-bidding`"));
        assertTrue(evaluator.contains("READY_FOR_HUMAN_REVIEW"));
    }

    @Test void androidPassCannotSatisfyAnExplicitIosClaim() throws Exception {
        JsonNode policy = policy();
        JsonNode candidate = candidate(List.of("android", "ios"), true, "apk", true, "PASS");
        assertEquals("PASS", platformStatus(candidate, policy, "android"));
        assertEquals("BLOCKED", platformStatus(candidate, policy, "ios"));
        assertEquals("BLOCKED", aggregateStatus(candidate, policy));
    }

    @Test void missingIosReceiptRemainsNotRunAndAbsentIosTargetRemainsNotImplemented() throws Exception {
        JsonNode policy = policy();
        JsonNode missingReceipt = candidate(List.of("ios"), true, "ipa", false, "PASS");
        JsonNode absentTarget = candidate(List.of("ios"), false, "ipa", false, "PASS");
        assertEquals("NOT_RUN", platformStatus(missingReceipt, policy, "ios"));
        assertEquals("NOT_IMPLEMENTED", platformStatus(absentTarget, policy, "ios"));
    }

    @Test void onlyAllDeclaredPlatformsCanReachHumanReviewAndNeverPublished() throws Exception {
        JsonNode policy = policy();
        JsonNode candidate = candidate(List.of("android", "ios"), true, "ipa", true, "PASS");
        assertEquals("READY_FOR_HUMAN_REVIEW", aggregateStatus(candidate, policy));
        assertTrue(strings(policy.path("rules").path("allowedAggregateStatuses")).contains("BLOCKED"));
        assertFalse(strings(policy.path("rules").path("allowedAggregateStatuses")).contains("PUBLISHED"));
        assertFalse(policy.path("rules").path("ragMayExecuteCommands").asBoolean());
        assertFalse(policy.path("rules").path("ragMayBuildOrSign").asBoolean());
        assertFalse(policy.path("rules").path("ragMayPublish").asBoolean());
        assertFalse(policy.path("rules").path("ragMayInferPassFromDocumentation").asBoolean());
    }

    @Test void legacyApkManifestDoesNotClaimIosSupport() throws Exception {
        JsonNode policy = policy();
        var legacy = (com.fasterxml.jackson.databind.node.ObjectNode)
                candidate(List.of("android"), true, "apk", true, "PASS");
        var release = (com.fasterxml.jackson.databind.node.ObjectNode) legacy.path("release");
        release.remove("platforms");
        release.put("artifactKind", "apk");
        assertEquals(List.of("android"), strings(policy.path("rules").path("legacyApkManifestPlatforms")));
        assertEquals("NOT_APPLICABLE", platformStatus(legacy, policy, "ios"));
    }

    @Test void anUnscopedCandidateCannotReachHumanReview() throws Exception {
        JsonNode policy = policy();
        JsonNode candidate = JSON.createObjectNode().set("release", JSON.createObjectNode());
        assertEquals("BLOCKED", aggregateStatus(candidate, policy));
    }

    @Test void catalogManifestAndPublicVersionConflictBlocksHumanReview() throws Exception {
        JsonNode policy = policy();
        var candidate = (com.fasterxml.jackson.databind.node.ObjectNode)
                candidate(List.of("android"), true, "apk", true, "PASS");
        ((com.fasterxml.jackson.databind.node.ObjectNode) candidate.path("release")).put("catalogConsistent", false);
        assertTrue(policy.path("rules").path("catalogMustMatchManifestAndPublicLinks").asBoolean());
        assertEquals("BLOCKED", aggregateStatus(candidate, policy));
    }

    private static JsonNode policy() throws Exception {
        Path file = GovernanceProject.locateProjectRoot().resolve("config/release-platform-rag-policy.json");
        return JSON.readTree(Files.readString(file));
    }

    private static List<String> strings(JsonNode node) {
        var result = new ArrayList<String>();
        node.forEach(value -> result.add(value.asText()));
        return List.copyOf(result);
    }

    private static JsonNode candidate(List<String> declared, boolean implemented, String artifactKind,
                                      boolean receiptsPresent, String receiptStatus) {
        var gates = JSON.createObjectNode();
        for (String gate : List.of("iosBridgeComponents", "xcodeSimulator", "iosArtifactIdentity", "signing", "iosDistribution"))
            if (receiptsPresent || gate.equals("iosBridgeComponents")) gates.put(gate, receiptStatus);
        var ios = JSON.createObjectNode().put("implemented", implemented).put("artifactKind", artifactKind);
        ios.set("gates", gates);
        var androidGates = JSON.createObjectNode();
        for (String gate : List.of("androidComponents", "apkBuild", "apkIdentity", "signing", "publicDownload"))
            androidGates.put(gate, "PASS");
        var android = JSON.createObjectNode().put("implemented", true).put("artifactKind", "apk");
        android.set("gates", androidGates);
        var platformReceipts = JSON.createObjectNode();
        platformReceipts.set("android", android);
        platformReceipts.set("ios", ios);
        var qualification = JSON.createObjectNode();
        qualification.set("platforms", platformReceipts);
        var release = JSON.createObjectNode().put("catalogConsistent", true);
        if (!declared.isEmpty()) {
            var platformList = JSON.createArrayNode();
            declared.forEach(platformList::add);
            release.set("platforms", platformList);
        }
        var candidate = JSON.createObjectNode();
        candidate.set("release", release);
        candidate.set("qualification", qualification);
        var shared = JSON.createObjectNode().put("sourceSnapshot", "PASS").put("gameCoreComponents", "PASS");
        qualification.set("sharedGates", shared);
        return candidate;
    }

    private static List<String> declaredPlatforms(JsonNode candidate, JsonNode policy) {
        JsonNode declared = candidate.path("release").path("platforms");
        if (declared.isArray()) return strings(declared);
        if (strings(policy.path("rules").path("legacyApkManifestPlatforms"))
                .contains(candidate.path("release").path("artifactKind").asText()))
            return strings(policy.path("rules").path("legacyApkManifestPlatforms"));
        return List.of();
    }

    private static String platformStatus(JsonNode candidate, JsonNode policy, String platform) {
        if (!declaredPlatforms(candidate, policy).contains(platform)) return "NOT_APPLICABLE";
        JsonNode contract = policy.path("platforms").path(platform);
        JsonNode receipt = candidate.path("qualification").path("platforms").path(platform);
        if (contract.isMissingNode() || !receipt.path("implemented").asBoolean(false)) return "NOT_IMPLEMENTED";
        if (!strings(contract.path("artifactKinds")).contains(receipt.path("artifactKind").asText())) return "BLOCKED";
        JsonNode shared = candidate.path("qualification").path("sharedGates");
        for (String gate : strings(policy.path("sharedGates"))) {
            String status = shared.path(gate).asText("NOT_RUN");
            if (!strings(policy.path("evidenceStatuses")).contains(status)) return "BLOCKED";
            if (!status.equals("PASS")) return status;
        }
        JsonNode gates = receipt.path("gates");
        for (String gate : strings(contract.path("requiredGates"))) {
            String status = gates.path(gate).asText("NOT_RUN");
            if (!strings(policy.path("evidenceStatuses")).contains(status)) return "BLOCKED";
            if (!status.equals("PASS")) return status;
        }
        return "PASS";
    }

    private static String aggregateStatus(JsonNode candidate, JsonNode policy) {
        if (!candidate.path("release").path("catalogConsistent").asBoolean(false)) return "BLOCKED";
        List<String> platforms = declaredPlatforms(candidate, policy);
        if (platforms.isEmpty()) return "BLOCKED";
        for (String platform : platforms)
            if (!platformStatus(candidate, policy, platform).equals("PASS")) return "BLOCKED";
        return "READY_FOR_HUMAN_REVIEW";
    }
}
