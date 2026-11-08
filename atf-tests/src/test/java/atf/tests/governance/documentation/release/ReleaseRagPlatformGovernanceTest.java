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
