package atf.tests.governance.documentation.release.rag;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.JSON;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.VERSION;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.hash;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.refreshPayload;

final class RagAndroidEvidenceFixture {
    static void selectPassingE2e(RagCandidateFixture fixture) throws Exception {
        selectPassingE2e(fixture, "android-e2e");
    }

    static void selectPassingE2e(RagCandidateFixture fixture, String... names) throws Exception {
        Path manifestFile = fixture.candidate().resolve("release-validation.json");
        ObjectNode manifest = (ObjectNode) JSON.readTree(manifestFile.toFile());
        ObjectNode groups = ((ObjectNode) manifest.path("qualification")).putObject("e2eGroups");
        for (String name : names) groups.set(name, passingE2eGroup(fixture.candidate(), name, manifest));
        JSON.writerWithDefaultPrettyPrinter().writeValue(manifestFile.toFile(), manifest);
        ObjectNode plan = (ObjectNode) JSON.readTree(fixture.plan().toFile());
        plan.put("userFlowChanged", true);
        var selected = plan.putObject("e2e").put("status", "PASS").putArray("groups");
        for (String name : names) selected.add(name);
        JSON.writeValue(fixture.plan().toFile(), plan);
        refreshPayload(fixture.candidate());
    }

    private static ObjectNode passingE2eGroup(Path candidate, String name, ObjectNode manifest) throws Exception {
        String directory = "reports/e2e/" + name + "/";
        Path report = candidate.resolve(directory + "report.html");
        Files.createDirectories(report.getParent());
        Files.writeString(report, "<!doctype html><html><body><img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=\"></body></html>");
        ObjectNode release = (ObjectNode) manifest.path("release");
        ObjectNode source = (ObjectNode) manifest.path("source");
        ObjectNode receipt = JSON.createObjectNode().put("schemaVersion", 1).put("kind", "android-signed-release-e2e")
                .put("status", "PASS").put("protocol", "ANDROID_E2E_FIXTURE")
                .put("tests", 1).put("failures", 0).put("errors", 0).put("skipped", 0)
                .put("releaseVersion", VERSION).put("installedApkSha256", release.path("apkSha256").asText())
                .put("sourceCommit", source.path("sourceCommit").asText())
                .put("sourceFingerprint", source.path("workingTreeFingerprint").asText())
                .put("inlinePngCount", 1).put("reportSha256", hash(report));
        JSON.writeValue(candidate.resolve(directory + "execution.json").toFile(), receipt);
        return JSON.createObjectNode().put("report", directory + "report.html")
                .put("executionReceipt", directory + "execution.json")
                .put("protocol", "ANDROID_E2E_FIXTURE").put("tests", 1).put("failures", 0).put("errors", 0).put("skipped", 0);
    }

    static void bindDisposableUserBoundary(RagCandidateFixture fixture, boolean successfulCleanup) throws Exception {
        Path candidate = fixture.candidate();
        Path readyFile = candidate.resolve("reports/e2e/data-isolation-ready.json");
        Path cleanupFile = candidate.resolve("reports/e2e/data-isolation-cleanup.json");
        Files.createDirectories(readyFile.getParent());
        ObjectNode manifest = (ObjectNode) JSON.readTree(candidate.resolve("release-validation.json").toFile());
        String apkSha = manifest.path("release").path("apkSha256").asText();
        ObjectNode ready = JSON.createObjectNode().put("schemaVersion", 1).put("status", "READY")
                .put("scope", "ANDROID_SECONDARY_USER").put("applicationId", "ro.whistlab.app")
                .put("originalUserId", 0).put("testUserId", 10).put("createdByRun", true)
                .put("packageWasInstalledBefore", false).put("originalPackageWasInstalledBefore", false)
                .put("screenOffTimeoutMs", 2147483647).put("screenUnlocked", true)
                .put("cleanupRequired", true)
                .put("accountsAdded", false).put("installedApkSha256", apkSha);
        JSON.writeValue(readyFile.toFile(), ready);
        String readySha = hash(readyFile);
        ObjectNode cleanup = JSON.createObjectNode().put("schemaVersion", 1)
                .put("status", successfulCleanup ? "PASS" : "FAIL")
                .put("scope", "ANDROID_SECONDARY_USER").put("applicationId", "ro.whistlab.app")
                .put("originalUserId", 0).put("removedUserId", 10).put("removedByRun", true)
                .put("originalPackageWasInstalledBefore", false).put("originalPackageRetained", false)
                .put("originalPackageStatePreserved", true).put("readyReceiptSha256", readySha);
        JSON.writeValue(cleanupFile.toFile(), cleanup);
        String cleanupSha = hash(cleanupFile);
        for (String name : List.of("signed-release-bidding", "signed-release-auto-game")) {
            Path receiptFile = candidate.resolve("reports/e2e").resolve(name).resolve("execution.json");
            ObjectNode receipt = (ObjectNode) JSON.readTree(receiptFile.toFile());
            receipt.putObject("dataBoundary").put("mode", "DISPOSABLE_SECONDARY_USER")
                    .put("readyReceipt", "reports/e2e/data-isolation-ready.json").put("readyReceiptSha256", readySha)
                    .put("cleanupReceipt", "reports/e2e/data-isolation-cleanup.json").put("cleanupReceiptSha256", cleanupSha)
                    .put("testUserId", 10);
            JSON.writeValue(receiptFile.toFile(), receipt);
        }
        refreshPayload(candidate);
    }

    private RagAndroidEvidenceFixture() { }
}
