package atf.tests.governance.documentation.release.rag;

import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AuditedAssertions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.JSON;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.capturePayload;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.create;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.hash;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.hashText;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.refreshPayload;
import static atf.tests.governance.documentation.release.rag.RagEvaluatorProbe.check;
import static atf.tests.governance.documentation.release.rag.RagEvaluatorProbe.details;
import static atf.tests.governance.documentation.release.rag.RagEvaluatorProbe.evaluate;
import static atf.tests.governance.documentation.release.rag.RagEvaluatorProbe.findCheck;
import static atf.tests.governance.documentation.release.rag.RagAndroidEvidenceFixture.bindDisposableUserBoundary;
import static atf.tests.governance.documentation.release.rag.RagAndroidEvidenceFixture.selectPassingE2e;

@AtfComponentTest
class ReleaseRagAndroidComponentTest {
    @TempDir Path temporary;

    @Test void nativeRuntimeAndValidatorRevisionsRemainIndependentInTheRealEvaluator() throws Exception {
        var fixture = create(temporary);
        var root = atf.tests.governance.support.GovernanceProject.locateProjectRoot();
        var prepared = atf.android.process.CommandProcess.execute(List.of("node",
                root.resolve("atf-tests/tools/android-e2e-provenance-fixture.mjs").toString(), "bind-rag",
                fixture.candidate().toString(), fixture.source().toString(), fixture.plan().toString()),
                root, java.util.Map.of(), java.time.Duration.ofSeconds(60), true);
        AuditedAssertions.assertEquals(0, prepared.exitCode(), new String(prepared.stdout(), java.nio.charset.StandardCharsets.UTF_8)
                + prepared.stderr());
        refreshPayload(fixture.candidate());
        evaluate(fixture, fixture.output());
        var verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals("PASS", check(verdict, "SOURCE_SNAPSHOT"));
        AuditedAssertions.assertEquals("PASS", check(verdict, "E2E_SCOPE"));
        var decision = verdict.at("/e2e/decisions/0");
        AuditedAssertions.assertEquals("REUSE", decision.path("decision").asText());
        AuditedAssertions.assertTrue(decision.at("/provenance/validator/fingerprint").asText().matches("[a-f0-9]{64}"));

        Path sourceFile = fixture.candidate().resolve("reports/apk-inputs.json");
        var source = (ObjectNode) JSON.readTree(sourceFile.toFile());
        for (JsonNode row : source.withArray("files")) if (row.path("path").asText().equals("src/Game.java"))
            ((ObjectNode) row).put("sha256", "f".repeat(64));
        var records = new java.util.TreeMap<String, String>();
        source.withArray("files").forEach(row -> records.put(row.path("path").asText(), row.path("sha256").asText()));
        String fingerprint = hashText(records.entrySet().stream().map(row -> row.getKey() + "\t" + row.getValue() + "\n")
                .collect(java.util.stream.Collectors.joining()));
        source.put("fingerprint", fingerprint);JSON.writeValue(sourceFile.toFile(), source);
        Path manifestFile = fixture.candidate().resolve("release-validation.json");
        var manifest = (ObjectNode) JSON.readTree(manifestFile.toFile());
        ((ObjectNode) manifest.path("source")).put("workingTreeFingerprint", fingerprint);
        JSON.writeValue(manifestFile.toFile(), manifest);refreshPayload(fixture.candidate());
        evaluate(fixture, fixture.output());
        verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "E2E_SCOPE"));
        AuditedAssertions.assertEquals("ANDROID_E2E_RUNTIME_CHANGED", verdict.at("/e2e/decisions/0/reason").asText());
    }

    @Test void aDamagedReportRequestsOnlyItsOwnFlow() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        selectPassingE2e(fixture, "signed-release-bidding", "signed-release-auto-game");
        bindDisposableUserBoundary(fixture, true);
        Path report = fixture.candidate().resolve("reports/e2e/signed-release-bidding/report.html");
        Files.writeString(report, "<html><body><img src=\"data:image/png;base64,iVBORw0KGgo=\"></body></html>");
        Path receiptFile = report.resolveSibling("execution.json");
        ObjectNode receipt = (ObjectNode) JSON.readTree(receiptFile.toFile());
        receipt.put("reportSha256", hash(report));
        JSON.writeValue(receiptFile.toFile(), receipt);
        refreshPayload(fixture.candidate());

        AuditedAssertions.assertEquals(2, evaluate(fixture, fixture.output()));
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals("PASS", check(verdict, "PAYLOAD_INTEGRITY"));
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "E2E_SCOPE"));
        var decisions = verdict.at("/e2e/decisions");
        AuditedAssertions.assertEquals("RUN", decisions.get(0).path("decision").asText());
        AuditedAssertions.assertEquals("INVALID_PNG", decisions.get(0).path("reason").asText());
        AuditedAssertions.assertEquals("REUSE", decisions.get(1).path("decision").asText());
        AuditedAssertions.assertEquals("signed-release-auto-game", decisions.get(1).path("group").asText());
    }

    @Test void disposableAndroidUserRequiresHashBoundPreparationAndCleanupReceipts() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        selectPassingE2e(fixture, "signed-release-bidding", "signed-release-auto-game");
        bindDisposableUserBoundary(fixture, true);

        AuditedAssertions.assertEquals(0, evaluate(fixture, fixture.output()));
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals("PASS", check(verdict, "E2E_SCOPE"));
        AuditedAssertions.assertTrue(findCheck(verdict, "E2E_SCOPE").path("evidence")
                .findValuesAsText("path").contains("reports/e2e/data-isolation-cleanup.json"));

        Path biddingReceipt = fixture.candidate().resolve("reports/e2e/signed-release-bidding/execution.json");
        ObjectNode receipt = (ObjectNode) JSON.readTree(biddingReceipt.toFile());
        ((ObjectNode) receipt.path("dataBoundary")).put("testUserId", 11);
        JSON.writeValue(biddingReceipt.toFile(), receipt);
        refreshPayload(fixture.candidate());

        AuditedAssertions.assertEquals(2, evaluate(fixture, fixture.output()));
        verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "E2E_SCOPE"));
        AuditedAssertions.assertTrue(details(verdict, "E2E_SCOPE").stream()
                .anyMatch(value -> value.contains("eliminarea aceluiași utilizator")));
    }

    @Test void disposableAndroidUserCannotPassWithoutCleanupReceipt() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        selectPassingE2e(fixture, "signed-release-bidding", "signed-release-auto-game");
        bindDisposableUserBoundary(fixture, false);

        AuditedAssertions.assertEquals(2, evaluate(fixture, fixture.output()));
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "E2E_SCOPE"));
        AuditedAssertions.assertTrue(details(verdict, "E2E_SCOPE").stream()
                .anyMatch(value -> value.contains("nu dovedește eliminarea")));
    }

    @Test void resumedE2eBindsTheCleanupToItsOwnLease() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        selectPassingE2e(fixture, "signed-release-bidding", "signed-release-auto-game");
        bindDisposableUserBoundary(fixture, true);
        Path e2e = fixture.candidate().resolve("reports/e2e");
        String leaseId = "a".repeat(32);
        String prefix = "reports/e2e/isolation/" + leaseId + "/";
        Path receipts = Files.createDirectories(e2e.resolve("isolation").resolve(leaseId));
        Path ready = Files.copy(e2e.resolve("data-isolation-ready.json"), receipts.resolve("data-isolation-ready.json"));
        Path cleanup = receipts.resolve("data-isolation-cleanup.json");
        ObjectNode teardown = (ObjectNode) JSON.readTree(e2e.resolve(cleanup.getFileName()).toFile());
        teardown.put("leaseId", leaseId);
        JSON.writeValue(cleanup.toFile(), teardown);

        Path execution = e2e.resolve("signed-release-bidding/execution.json");
        ObjectNode receipt = (ObjectNode) JSON.readTree(execution.toFile());
        var boundary = (ObjectNode) receipt.path("dataBoundary");
        boundary.put("leaseId", leaseId)
                .put("readyReceipt", prefix + ready.getFileName()).put("readyReceiptSha256", hash(ready))
                .put("cleanupReceipt", prefix + cleanup.getFileName()).put("cleanupReceiptSha256", hash(cleanup));
        JSON.writeValue(execution.toFile(), receipt);
        refreshPayload(fixture.candidate());
        AuditedAssertions.assertEquals(0, evaluate(fixture, fixture.output()));

        teardown.put("leaseId", "b".repeat(32));
        JSON.writeValue(cleanup.toFile(), teardown);
        boundary.put("cleanupReceiptSha256", hash(cleanup));
        JSON.writeValue(execution.toFile(), receipt);
        refreshPayload(fixture.candidate());
        AuditedAssertions.assertEquals(2, evaluate(fixture, fixture.output()));
        AuditedAssertions.assertEquals("BLOCKED", check(JSON.readTree(fixture.output().toFile()), "E2E_SCOPE"));
    }

    @Test void disposableAndroidUserMustPreserveTheOriginalPackageInstallationState() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        selectPassingE2e(fixture, "signed-release-bidding", "signed-release-auto-game");
        bindDisposableUserBoundary(fixture, true);

        Path cleanup = fixture.candidate().resolve("reports/e2e/data-isolation-cleanup.json");
        ObjectNode receipt = (ObjectNode) JSON.readTree(cleanup.toFile());
        receipt.put("originalPackageStatePreserved", false);
        JSON.writeValue(cleanup.toFile(), receipt);
        for (String flow : List.of("signed-release-bidding", "signed-release-auto-game")) {
            Path execution = fixture.candidate().resolve("reports/e2e").resolve(flow).resolve("execution.json");
            ObjectNode document = (ObjectNode) JSON.readTree(execution.toFile());
            ((ObjectNode) document.path("dataBoundary")).put("cleanupReceiptSha256", hash(cleanup));
            JSON.writeValue(execution.toFile(), document);
        }
        refreshPayload(fixture.candidate());

        AuditedAssertions.assertEquals(2, evaluate(fixture, fixture.output()));
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "E2E_SCOPE"));
        AuditedAssertions.assertTrue(details(verdict, "E2E_SCOPE").stream()
                .anyMatch(value -> value.contains("stării inițiale a instalării originale")));
    }

    @Test void aProductionPlanCannotReuseTheDebugTestCertificate() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        ObjectNode plan = (ObjectNode) JSON.readTree(fixture.plan().toFile()); plan.put("channel", "PRODUCTION");
        JSON.writeValue(fixture.plan().toFile(), plan);
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "SIGNING_CHANNEL"));
    }

    @Test void missingPrePublicationAndroidGateReceiptBlocksHumanReview() throws Exception {
        RagCandidateFixture fixture = create(temporary);
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

    @Test void changedUserFlowRequiresPassingE2eAndAnOmissionKeepsItsOwnStatus() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        ObjectNode plan = (ObjectNode) JSON.readTree(fixture.plan().toFile()); plan.put("userFlowChanged", true);
        JSON.writeValue(fixture.plan().toFile(), plan);
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(2, exit);
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "E2E_SCOPE"));
        AuditedAssertions.assertEquals("NOT_RUN", verdict.path("e2e").path("status").asText());
    }

    @Test void aPassingE2eReceiptBindsTheInstalledApkSourceOutcomesAndInlineExtentImages() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        selectPassingE2e(fixture);
        int exit = evaluate(fixture, fixture.output());
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals(0, exit, JSON.writeValueAsString(verdict.path("checks")));
        AuditedAssertions.assertEquals("READY_FOR_HUMAN_REVIEW", verdict.path("verdict").asText());
        AuditedAssertions.assertEquals("PASS", check(verdict, "E2E_SCOPE"));
        AuditedAssertions.assertTrue(findCheck(verdict, "E2E_SCOPE").path("evidence").size() >= 3);
    }

    @Test void aClaimedE2ePassNeedsAnExecutedExtentReceipt() throws Exception {
        RagCandidateFixture fixture = create(temporary);
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
        RagCandidateFixture fixture = create(temporary);
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
        RagCandidateFixture fixture = create(temporary);
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

    @Test void aBiddingDiffBlocksAPlanThatDeclaresNoFlowChange() throws Exception {
        RagCandidateFixture fixture = create(temporary);
        String biddingPath = "whist-lab-app/app/src/main/java/ro/whistlab/app/bidding/BidPolicy.java";
        Path changed = fixture.source().resolve(biddingPath);
        Files.createDirectories(changed.getParent());
        Files.writeString(changed, "class BidPolicy {}\n");
        Path inputsFile = fixture.candidate().resolve("reports/apk-inputs.json");
        ObjectNode inputs = (ObjectNode) JSON.readTree(inputsFile.toFile());
        var rows = (com.fasterxml.jackson.databind.node.ArrayNode) inputs.path("files");
        String bidHash = hash(changed);
        rows.addObject().put("path", biddingPath).put("sha256", bidHash);
        String fingerprint = hashText("README.md\t" + rows.get(0).path("sha256").asText()
                + "\nsrc/Game.java\t" + rows.get(1).path("sha256").asText() + "\n" + biddingPath + "\t" + bidHash + "\n");
        inputs.put("fingerprint", fingerprint);
        JSON.writeValue(inputsFile.toFile(), inputs);
        Path manifestFile = fixture.candidate().resolve("release-validation.json");
        ObjectNode manifest = (ObjectNode) JSON.readTree(manifestFile.toFile());
        ((ObjectNode) manifest.path("source")).put("workingTreeFingerprint", fingerprint);
        capturePayload(fixture.candidate(), manifest);
        JSON.writeValue(manifestFile.toFile(), manifest);
        ObjectNode plan = (ObjectNode) JSON.readTree(fixture.plan().toFile());
        ((ObjectNode) plan.path("scope")).put("candidateFingerprint", fingerprint);
        JSON.writeValue(fixture.plan().toFile(), plan);

        AuditedAssertions.assertEquals(2, evaluate(fixture, fixture.output()));
        JsonNode verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals("PASS", check(verdict, "SOURCE_SNAPSHOT"));
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "DIFF_SCOPE"));
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "E2E_SCOPE"));
        AuditedAssertions.assertEquals(biddingPath, verdict.path("testScope").path("changes").get(1).path("path").asText());
        AuditedAssertions.assertTrue(details(verdict, "E2E_SCOPE").stream().anyMatch(value -> value.contains("signed-release-bidding")));

        selectPassingE2e(fixture);
        plan = (ObjectNode) JSON.readTree(fixture.plan().toFile());
        plan.put("changeRisk", "behaviorChange");
        JSON.writeValue(fixture.plan().toFile(), plan);
        AuditedAssertions.assertEquals(2, evaluate(fixture, fixture.output()));
        verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals("PASS", check(verdict, "DIFF_SCOPE"));
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "E2E_SCOPE"));
        AuditedAssertions.assertTrue(details(verdict, "E2E_SCOPE").stream().anyMatch(value -> value.contains("signed-release-auto-game")));

        selectPassingE2e(fixture, "signed-release-bidding", "signed-release-auto-game");
        AuditedAssertions.assertEquals(2, evaluate(fixture, fixture.output()));
        verdict = JSON.readTree(fixture.output().toFile());
        AuditedAssertions.assertEquals("PASS", check(verdict, "DIFF_SCOPE"));
        AuditedAssertions.assertEquals("PASS", check(verdict, "E2E_SCOPE"));
        AuditedAssertions.assertEquals("BLOCKED", check(verdict, "COMPONENT_SOURCE_BINDING"));
    }
}
