package atf.tests.governance.documentation.release.reporting;

import atf.android.process.CommandProcess;
import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;

/** The release guard uses the same native-source admission as the RAG evaluator. */
final class NativeEvidenceProbe {
    private NativeEvidenceProbe() { }
    static void verify(Path release, String group, JsonNode receipt, Path work) throws Exception {
        Files.createDirectories(work);
        var json = new ObjectMapper();
        var scenario = receipt.required("scenario");
        var profile = json.createObjectNode().put("api", scenario.required("api").asInt())
                .put("displayProfile", scenario.required("displayProfile").asText())
                .put("responsiveProfiles", scenario.path("responsiveProfiles").asText().isEmpty()
                        ? scenario.required("displayProfile").asText() : scenario.required("responsiveProfiles").asText());
        var request = json.createObjectNode().put("releaseVersion", receipt.required("releaseVersion").asText());
        request.putArray("groups").add(group);request.set("executionProfile", profile);
        json.writeValue(work.resolve("e2e-request.json").toFile(), request);
        var root = GovernanceProject.locateProjectRoot();
        var result = CommandProcess.execute(List.of("node", root.resolve("atf-tests/tools/prepare-android-e2e.mjs").toString(),
                "inspect", release.toString(), work.toString()), root, Map.of(), Duration.ofSeconds(90), true);
        assertEquals(0, result.exitCode(), new String(result.stdout(), StandardCharsets.UTF_8));
    }
}
