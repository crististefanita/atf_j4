package atf.tests.governance.documentation.release.rag;

import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

record RagCandidateFixture(Path candidate, Path source, Path plan, Path output, Path project) {
    static final ObjectMapper JSON = new ObjectMapper();
    static final String VERSION = "0.21.4";
    static final String BASELINE = "0.21.3";
    static final String BASELINE_COMMIT = "c".repeat(40);

    static RagCandidateFixture create(Path temporary) throws Exception {
        return create(temporary, "# Whist Lab\n\n[Descarcă](release/0.21.4/WhistLab-0.21.4.apk)\n");
    }

    static RagCandidateFixture create(Path temporary, String readmeText) throws Exception {
        Path project = Files.createDirectories(temporary.resolve("project"));
        Path candidate = Files.createDirectories(temporary.resolve("candidate/release/" + VERSION));
        Path source = Files.createDirectories(temporary.resolve("source"));
        Path sourceFile = Files.createDirectories(source.resolve("src")).resolve("Game.java"); Files.writeString(sourceFile, "class Game {}\n");
        Path readmeFile = source.resolve("README.md"); Files.writeString(readmeFile, readmeText);
        String sourceHash = hash(sourceFile), readmeHash = hash(readmeFile);
        String baselineFingerprint = createBaseline(project, sourceHash);
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
        ObjectNode groups = RagComponentEvidenceFixture.create(candidate, project, readmeHash, sourceHash);
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
        ObjectNode candidatePlan = JSON.createObjectNode().put("schemaVersion", 2).put("releaseVersion", VERSION)
                .put("channel", "TEST").put("changeRisk", "maintenanceOrRefactor").put("userFlowChanged", false);
        candidatePlan.putObject("scope").put("baselineVersion", BASELINE).put("baselineCommit", BASELINE_COMMIT)
                .put("baselineFingerprint", baselineFingerprint).put("candidateFingerprint", fingerprint);
        candidatePlan.putArray("declaredPlatforms").add("android");
        var selectedGroups = candidatePlan.putArray("componentGroups");
        for (String module : RagComponentEvidenceFixture.GROUPS) selectedGroups.add(module);
        candidatePlan.set("e2e", JSON.createObjectNode().put("status", "NOT_RUN")
                .put("rationale", "RAG and governance-only changes; application E2E remains frozen."));
        JSON.writeValue(plan.toFile(), candidatePlan);
        Path outputDirectory = Files.createDirectories(temporary.resolve("verdict"));
        return new RagCandidateFixture(candidate, source, plan, outputDirectory.resolve("rag.json"), project);
    }

    private static String createBaseline(Path project, String sourceHash) throws Exception {
        Path config = Files.createDirectories(project.resolve("config")).resolve("release-platform-rag-policy.json");
        Files.copy(GovernanceProject.locateProjectRoot().resolve("config").resolve(config.getFileName()), config);
        Path baseline = Files.createDirectories(project.resolve("release/" + BASELINE));
        ObjectNode index = JSON.createObjectNode().put("schemaVersion", 2).put("currentVersion", BASELINE);
        index.putArray("versions").add(BASELINE);
        JSON.writeValue(project.resolve("release/index.json").toFile(), index);
        String readmeHash = hashText("# Published fixture\n");
        String fingerprint = hashText("README.md\t" + readmeHash + "\nsrc/Game.java\t" + sourceHash + "\n");
        ObjectNode inputs = JSON.createObjectNode().put("sourceCommit", BASELINE_COMMIT).put("fingerprint", fingerprint);
        var files = inputs.putArray("files");
        files.addObject().put("path", "README.md").put("sha256", readmeHash);
        files.addObject().put("path", "src/Game.java").put("sha256", sourceHash);
        JSON.writeValue(Files.createDirectories(baseline.resolve("reports")).resolve("apk-inputs.json").toFile(), inputs);
        ObjectNode manifest = JSON.createObjectNode();
        manifest.putObject("release").put("versionName", BASELINE).put("versionCode", 19);
        manifest.putObject("publication").put("state", "PUBLISHED");
        manifest.putObject("qualification").put("state", "PASS");
        manifest.putObject("source").put("sourceCommit", BASELINE_COMMIT).put("workingTreeFingerprint", fingerprint)
                .put("inputManifest", "reports/apk-inputs.json");
        capturePayload(baseline, manifest);
        JSON.writeValue(baseline.resolve("release-validation.json").toFile(), manifest);
        return fingerprint;
    }

    static void capturePayload(Path candidate, ObjectNode manifest) throws Exception {
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

    static ObjectNode gate(String receipt) {
        return JSON.createObjectNode().put("status", "PASS").put("receipt", receipt);
    }

    static void refreshPayload(Path candidate) throws Exception {
        ObjectNode manifest = (ObjectNode) JSON.readTree(candidate.resolve("release-validation.json").toFile());
        capturePayload(candidate, manifest);
        JSON.writerWithDefaultPrettyPrinter().writeValue(candidate.resolve("release-validation.json").toFile(), manifest);
    }

    static String hash(Path file) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))); }
    static String hashText(String value) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
}
