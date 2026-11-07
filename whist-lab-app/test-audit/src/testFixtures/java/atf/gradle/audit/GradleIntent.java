package atf.gradle.audit;

import atf.reporting.artifact.ReportArtifactProfile;
import atf.reporting.contract.AtfReportStatus;
import atf.testkit.assertions.ComponentAssertionAudit;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.runner.Description;

/** Caller-authored publication inventory from captured inputs/checks, never inferred from rendered rows. */
public final class GradleIntent {
    public static final String TITLE = "Captured publication intent";
    private GradleIntent() { }
    static ReportArtifactProfile.Scenario describe(Description description, GradleInputs input, ComponentAssertionAudit.Result result) {
        var phases = new ArrayList<ReportArtifactProfile.Phase>();
        for (var observed : result.phases()) {
            var evidence = new ArrayList<ReportArtifactProfile.Evidence>();
            if (observed.phase() == ComponentAssertionAudit.Phase.TEST_BODY) {
                evidence.add(observation("JUnit invocation receipt", "short receipt"));
                for (var entry : input.entries()) evidence.add(observation("Captured context: " + entry.name(), entry.value()));
            }
            if (!observed.checks().isEmpty()) evidence.add(new ReportArtifactProfile.Assertions("Component assertions", observed.checks().stream()
                    .map(check -> new ReportArtifactProfile.Comparison(label(check.name()), check.passed())).toList()));
            if (!observed.diagnostic().isBlank()) evidence.add(observation("Original execution diagnostic", observed.diagnostic()));
            phases.add(new ReportArtifactProfile.Phase(observed.phase().name().replace('_', ' '), status(observed.outcome()), evidence));
        }
        if (!input.images().isEmpty()) phases.add(new ReportArtifactProfile.Phase("COMPONENT RASTERS — SYNTHETIC JVM FIXTURES", AtfReportStatus.INFO,
                input.images().stream().map(image -> (ReportArtifactProfile.Evidence) new ReportArtifactProfile.Observation("THEN " + image.title(),
                        List.of(), List.of())).toList()));
        String owner = description.getTestClass().getName().substring(description.getTestClass().getPackageName().length() + 1);
        return new ReportArtifactProfile.Scenario(owner, label(description.getMethodName()), status(result.outcome()), phases);
    }
    public static ReportArtifactProfile.Scenario withMetadata(ReportArtifactProfile.Scenario source, String serialized) {
        var phases = new ArrayList<ReportArtifactProfile.Phase>();
        for (var phase : source.phases()) {
            var evidence = new ArrayList<>(phase.evidence());
            if (phase.name().equals("TEST BODY")) evidence.add(1, observation(TITLE, serialized));
            phases.add(new ReportArtifactProfile.Phase(phase.name(), phase.status(), evidence));
        }
        return new ReportArtifactProfile.Scenario(source.testClass(), source.name(), source.status(), phases);
    }
    private static ReportArtifactProfile.Observation observation(String name, String value) {
        var labels = new ArrayList<String>(); int at = 0;
        do { int end = Math.min(value.length(), at + 16384); if (end < value.length() && Character.isHighSurrogate(value.charAt(end - 1))) end--;
            labels.add("Captured value / part " + (labels.size() + 1)); at = end;
        } while (at < value.length());
        return new ReportArtifactProfile.Observation(label(name), List.of(), labels);
    }
    public static ReportArtifactProfile.Scenario read(JsonNode source) {
        var phases = new ArrayList<ReportArtifactProfile.Phase>();
        for (var phase : source.required("phases")) {
            var evidence = new ArrayList<ReportArtifactProfile.Evidence>();
            for (var item : phase.required("evidence")) {
                String name = item.required("title").asText();
                if (item.has("fieldNames")) evidence.add(new ReportArtifactProfile.Observation(name, strings(item.required("fieldNames")), strings(item.required("bodyLabels"))));
                else if (item.has("comparisons")) evidence.add(new ReportArtifactProfile.Assertions(name, comparisons(item.required("comparisons")), AtfReportStatus.valueOf(item.required("status").asText())));
                else evidence.add(new ReportArtifactProfile.HttpExchange(name, item.required("requestRequired").asBoolean(), item.required("responseRequired").asBoolean(),
                        strings(item.required("bodyLabels")), List.of(), AtfReportStatus.valueOf(item.required("status").asText())));
            }
            phases.add(new ReportArtifactProfile.Phase(phase.required("name").asText(), AtfReportStatus.valueOf(phase.required("status").asText()), evidence));
        }
        return new ReportArtifactProfile.Scenario(source.required("testClass").asText(), source.required("name").asText(), AtfReportStatus.valueOf(source.required("status").asText()), phases);
    }
    private static List<ReportArtifactProfile.Comparison> comparisons(JsonNode rows) {
        var result = new ArrayList<ReportArtifactProfile.Comparison>();
        for (var row : rows) result.add(new ReportArtifactProfile.Comparison(row.required("name").asText(), AtfReportStatus.valueOf(row.required("status").asText()))); return result;
    }
    private static List<String> strings(JsonNode values) { var result = new ArrayList<String>(); for (var value : values) result.add(value.asText()); return result; }
    private static String label(String value) { return value.replaceAll("[\\t\\n\\r\\f \\u00a0]+", " ").trim(); }
    private static AtfReportStatus status(ComponentAssertionAudit.Outcome outcome) { return outcome == ComponentAssertionAudit.Outcome.PASS ? AtfReportStatus.PASS : outcome == ComponentAssertionAudit.Outcome.ABORTED ? AtfReportStatus.SKIP : AtfReportStatus.FAIL; }
}
