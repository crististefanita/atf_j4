package atf.android.evidence.component;

import atf.reporting.audit.AssertionAuditReport;
import atf.reporting.configuration.ReportEnvironmentField;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import atf.reporting.model.diagnostic.ReportPhaseDiagnostic;
import atf.reporting.model.timing.ReportPhaseTiming;
import atf.reporting.security.SensitiveDataRedactor;
import atf.testkit.assertions.AssertionValueDisplay;
import atf.testkit.assertions.ComponentAssertionAudit;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Captures invocations once, then publishes on the closing owner; execution timing remains original. */
public final class AndroidComponentRun implements AutoCloseable {
    private record Invocation(AssertionAuditReport.Identity identity, ComponentAssertionAudit.Result result,
                              List<ComponentInputs.Entry> inputs) { }
    private final Path file;
    private final boolean fallback;
    private final ReportingSettings settings;
    private final SensitiveDataRedactor redactor = new SensitiveDataRedactor();
    private final List<Invocation> invocations = new ArrayList<>();
    private final String provenance = System.getProperty(AndroidComponentReports.PROVENANCE, "");
    private boolean provenancePublished;
    private boolean closed;

    public AndroidComponentRun(Path file, boolean fallback, ReportingSettings settings) {
        this.file = java.util.Objects.requireNonNull(file);
        this.fallback = fallback;
        this.settings = java.util.Objects.requireNonNull(settings);
    }
    public AssertionValueDisplay display() {
        return new AssertionValueDisplay(settings.assertionMaxDisplayCharacters(), redactor::redactField);
    }
    public AssertionValueDisplay display(ComponentInputs inputs) {
        return new AssertionValueDisplay(settings.assertionMaxDisplayCharacters(), redactor::redactField, projection -> {
            if (projection.truncated()) inputs.add("Full comparison value — " + projection.role() + " — "
                    + redactor.redactText(String.valueOf(projection.name())), projection.fullText());
        });
    }
    synchronized void capture(AssertionAuditReport.Identity identity, ComponentAssertionAudit.Result result, ComponentInputs inputs) {
        if (closed) throw new IllegalStateException("Component report owner is closed");
        invocations.add(new Invocation(identity, result, inputs.snapshot()));
    }
    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        var environment = ReportEnvironmentSnapshot.builder()
                .put(ReportEnvironmentField.ENVIRONMENT, "COMPONENT — no Android device")
                .put(ReportEnvironmentField.SUITE, "Whist JVM component acceptance")
                .put(ReportEnvironmentField.REPORT_ARTIFACT, "Captured inputs and comparisons; publication after execution").build();
        var expectedProfile = invocations.isEmpty() ? null : profile();
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(file, settings)
                : new ExtentReportSession(file, settings, redactor, AtfConsoleSink.noOp(), environment)) {
            for (var invocation : invocations) publish(session, invocation);
        } finally { invocations.clear(); }
        if (expectedProfile != null) {
            var inspection = atf.reporting.artifact.ReportArtifactInspector.inspect(file, expectedProfile);
            if (!inspection.violations().isEmpty()) throw new IllegalStateException("Published component report differs from captured intent: " + inspection.violations());
        }
    }
    private atf.reporting.artifact.ReportArtifactProfile profile() {
        var scenarios = invocations.stream().map(invocation -> {
            var id = invocation.identity();
            var phases = invocation.result().phases().stream().map(phase -> {
                List<atf.reporting.artifact.ReportArtifactProfile.Evidence> evidence = phase.checks().isEmpty() ? List.of()
                        : List.of(new atf.reporting.artifact.ReportArtifactProfile.Assertions("Component assertions", phase.checks().stream()
                        .map(row -> new atf.reporting.artifact.ReportArtifactProfile.Comparison(redactor.redactText(row.name()), row.passed())).toList()));
                return new atf.reporting.artifact.ReportArtifactProfile.Phase(phase.phase().name().replace('_', ' '), status(phase.outcome()), evidence);
            }).toList();
            return new atf.reporting.artifact.ReportArtifactProfile.Scenario(id.testClass(), redactor.redactText(id.scenario()), status(invocation.result().outcome()), phases);
        }).toList();
        return new atf.reporting.artifact.ReportArtifactProfile(fallback ? atf.reporting.artifact.ReportArtifactProfile.Renderer.FALLBACK
                : atf.reporting.artifact.ReportArtifactProfile.Renderer.EXTENT, scenarios);
    }
    private void publish(AtfReportPorts.Session session, Invocation invocation) {
        var id = invocation.identity();
        try (var scenario = session.startScenario(id.testClass(), id.testPackage(), id.scenario(), id.tags())) {
            for (var observed : invocation.result().phases()) {
                try (var phase = scenario.startPhase(observed.phase().name().replace('_', ' '))) {
                    phase.detail("Test method", id.method());
                    if (observed == invocation.result().phases().getFirst()) {
                        var receipt = new java.util.LinkedHashMap<String, Object>();
                        receipt.put("schemaVersion", 1);
                        receipt.put("invocationId", invocation.inputs().stream().filter(i -> i.name().equals("JUnit invocation identity"))
                                .map(ComponentInputs.Entry::value).findFirst().orElse(""));
                        receipt.put("outcome", invocation.result().outcome().name());
                        receipt.put("comparisons", invocation.result().phases().stream().mapToInt(p -> p.checks().size()).sum());
                        phase.observation(new ReportObservation("Component invocation receipt", List.of(),
                                inputBodies(new ComponentInputs.Entry("Component invocation receipt",
                                        new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(receipt).toString())), List.of()));
                        if (!provenancePublished && !provenance.isBlank()) {
                            phase.observation(new ReportObservation("Live source execution provenance", List.of(),
                                    inputBodies(new ComponentInputs.Entry("Execution provenance", provenance)), List.of()));
                            provenancePublished = true;
                        }
                    }
                    observed.timing().ifPresent(time -> phase.executionTiming(new ReportPhaseTiming(time.elapsed(), time.intervals())));
                    if (observed.phase() == ComponentAssertionAudit.Phase.TEST_BODY) {
                        int ordinal = 0;
                        for (var input : invocation.inputs()) phase.observation(new ReportObservation("Component input: " + input.name(),
                                List.of(new ReportField("Input ordinal", Integer.toString(++ordinal)),
                                        new ReportField("Role", "Declared component input or comparison context, not a native capture")),
                                inputBodies(input), List.of()));
                    }
                    AssertionAuditReport.publishStep(phase, "Component assertions", observed.checks().stream()
                            .map(row -> new AtfAssertion(row.name(), row.expected(), row.actual(), row.passed())).toList(), AtfReportStatus.INFO);
                    if (!observed.diagnostic().isBlank()) phase.diagnostic(observed.outcome() == ComponentAssertionAudit.Outcome.ABORTED
                            ? ReportPhaseDiagnostic.Kind.EXECUTION_ABORTED : ReportPhaseDiagnostic.Kind.EXECUTION_FAILURE,
                            redactor.redactText(observed.diagnostic()));
                    phase.complete(status(observed.outcome()));
                }
            }
            scenario.complete(status(invocation.result().outcome()));
        }
    }
    private static AtfReportStatus status(ComponentAssertionAudit.Outcome outcome) {
        return switch (outcome) { case PASS -> AtfReportStatus.PASS; case FAIL -> AtfReportStatus.FAIL; case ABORTED -> AtfReportStatus.SKIP; };
    }
    private List<ReportObservation.Body> inputBodies(ComponentInputs.Entry input) {
        var parts = atf.android.evidence.text.EvidenceTextParts.split(
                redactor.redactField(input.name(), input.value()), settings.bodyMaxDisplayCharacters());
        var bodies = new ArrayList<ReportObservation.Body>();
        for (int index = 0; index < parts.size(); index++) bodies.add(new ReportObservation.Body(
                "Full captured input — part " + (index + 1) + "/" + parts.size(), parts.get(index), ReportObservation.Format.TEXT));
        return List.copyOf(bodies);
    }
}
