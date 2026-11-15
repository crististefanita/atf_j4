package atf.tests.governance.documentation.release.rag;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.BASELINE;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.JSON;
import static atf.tests.governance.documentation.release.rag.RagCandidateFixture.hash;

final class RagComponentEvidenceFixture {
    static final List<String> GROUPS = List.of("atf-validation", "atf-test-android", "atf-tests",
            "whist-lab-app-bidding", "whist-lab-app-commands", "whist-lab-app-display", "whist-lab-app-history",
            "whist-lab-app-achievements", "whist-lab-app-atf-card-table-ui");

    static ObjectNode create(Path candidate, Path project, String readmeHash, String sourceHash) throws Exception {
        Path componentInputs = candidate.resolve("reports/component/source-inputs.json");
        Files.createDirectories(componentInputs.getParent());
        var componentRows = JSON.createArrayNode();
        componentRows.addObject().put("path", "README.md").put("sha256", readmeHash);
        componentRows.addObject().put("path", "src/Game.java").put("sha256", sourceHash);
        JSON.writeValue(componentInputs.toFile(), componentRows);
        String componentFingerprint = hash(componentInputs);
        Path releaseInputs = componentInputs.resolveSibling("release-inputs.json");
        var releaseRows = JSON.createArrayNode();
        for (String relative : List.of("release/" + BASELINE + "/release-validation.json",
                "release/" + BASELINE + "/reports/apk-inputs.json", "release/index.json")) {
            releaseRows.addObject().put("path", relative).put("sha256", hash(project.resolve(relative)));
        }
        JSON.writeValue(releaseInputs.toFile(), releaseRows);
        String releaseFingerprint = hash(releaseInputs);
        Path provenanceFile = candidate.resolve("reports/component/source-provenance.json");
        ObjectNode provenance = JSON.createObjectNode().put("schemaVersion", 1).put("kind", "live-source-components")
                .put("sourceFingerprint", componentFingerprint).put("inputCount", 2).put("inputManifest", "source-inputs.json")
                .put("releaseInputManifest", "release-inputs.json").put("releaseFingerprint", releaseFingerprint);
        JSON.writeValue(provenanceFile.toFile(), provenance);
        String provenanceFingerprint = hash(provenanceFile);
        ObjectNode run = JSON.createObjectNode().put("kind", "live-source-components").put("status", "PASS")
                .put("sourceStable", true).put("sourceFingerprint", componentFingerprint).put("inputCount", 2)
                .put("inputManifest", "source-inputs.json").put("provenanceReceiptSha256", provenanceFingerprint)
                .put("releaseInputManifest", "release-inputs.json").put("releaseFingerprint", releaseFingerprint);
        var groups = JSON.createObjectNode(); var runResults = run.putArray("results");
        for (String module : GROUPS) {
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
        return groups;
    }

    private RagComponentEvidenceFixture() { }
}
