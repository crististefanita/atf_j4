package atf.android.e2e;

import static org.junit.jupiter.api.Assertions.assertTrue;

import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Shared audit-step projection for Whist Lab UI acceptance tests. */
final class WhistTestEvidence {
    private WhistTestEvidence() { }

    static String csv(List<Integer> values) {
        return String.join(",", values.stream().map(String::valueOf).toList());
    }

    static void checkpoint(AndroidUiFlowTrace trace, Path output, String phase, String title,
            String label, String actual, List<AtfAssertion> assertions) throws Exception {
        checkpoint(trace, output, phase, title, label, actual, assertions, List.of());
    }

    static void checkpoint(AndroidUiFlowTrace trace, Path output, String phase, String title,
            String label, String actual, List<AtfAssertion> assertions,
            List<AtfUiActionRecord> inputActions) throws Exception {
        boolean passed = assertions.stream().allMatch(AtfAssertion::passed);
        AtfReportStatus status = passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
        List<Path> artifacts = new ArrayList<>();
        for (String suffix : List.of(".xml", ".save2", "-state.txt",
                "-lab-state.properties", "-lab-state.json")) {
            Path artifact = output.resolve(label + suffix);
            if (Files.exists(artifact)) {
                artifacts.add(artifact);
            }
        }
        AtfGherkinKeyword keyword = phase.equals("SETUP") ? AtfGherkinKeyword.GIVEN
                : title.contains("human turn") || title.contains("submit bid")
                        ? AtfGherkinKeyword.WHEN : AtfGherkinKeyword.THEN;
        List<AtfUiActionRecord> actions = new ArrayList<>(inputActions);
        actions.add(new AtfUiActionRecord(trace.nextActionSequence(),
                "Compare captured state with independent rules", "Whist Lab",
                "every move, winner and score agrees with the rules", actual, 0,
                status, AtfUiEvidenceSource.OBSERVE));
        trace.checkpoint(phase, keyword.text(), title, "Whist Lab",
                output.resolve(label + ".png"), actions, assertions, status, artifacts);
        assertTrue(passed, () -> "Rule mismatch: "
                + assertions.stream().filter(assertion -> !assertion.passed()).toList());
    }
}