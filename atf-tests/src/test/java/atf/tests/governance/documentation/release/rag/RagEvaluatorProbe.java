package atf.tests.governance.documentation.release.rag;

import atf.tests.governance.support.GovernanceProject;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class RagEvaluatorProbe {
    static int evaluate(RagCandidateFixture fixture, Path output) throws Exception {
        Path root = GovernanceProject.locateProjectRoot();
        Path log = fixture.plan().resolveSibling("rag-evaluator.log");
        var command = new ArrayList<>(List.of("node", root.resolve("atf-tests/tools/evaluate-release-rag.mjs").toString(),
                "--project-root", fixture.project().toString(), "--candidate-dir", fixture.candidate().toString(),
                "--source-root", fixture.source().toString(), "--plan-file", fixture.plan().toString(), "--output", output.toString()));
        Path observation = fixture.output().resolveSibling("github-retention.json");
        if (Files.exists(observation)) command.addAll(List.of("--github-retention-file", observation.toString()));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!process.waitFor(45, TimeUnit.SECONDS)) { process.destroyForcibly(); throw new AssertionError("RAG evaluator exceeded 45 seconds"); }
        if (process.exitValue() == 1) throw new AssertionError(Files.readString(log));
        return process.exitValue();
    }

    static String check(JsonNode verdict, String code) {
        return findCheck(verdict, code).path("status").asText();
    }
    static List<String> details(JsonNode verdict, String code) {
        List<String> values = new ArrayList<>();
        for (JsonNode value : findCheck(verdict, code).path("details")) values.add(value.asText());
        return values;
    }
    static JsonNode findCheck(JsonNode verdict, String code) {
        for (JsonNode row : verdict.path("checks")) if (row.path("code").asText().equals(code)) return row;
        return com.fasterxml.jackson.databind.node.NullNode.getInstance();
    }

    private RagEvaluatorProbe() { }
}
