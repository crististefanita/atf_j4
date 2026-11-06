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
    private boolean closed;

    public AndroidComponentRun(Path file, boolean fallback, ReportingSettings settings) {
        this.file = java.util.Objects.requireNonNull(file);
        this.fallback = fallback;
        this.settings = java.util.Objects.requireNonNull(settings);
    }
    public AssertionValueDisplay display() {
        return new AssertionValueDisplay(settings.assertionMaxDisplayCharacters(), redactor::redactField);
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
                .put(ReportEnvironmentField.SUITE, "Whist Android audited components")
                .put(ReportEnvironmentField.REPORT_ARTIFACT, "Captured inputs and comparisons; publication after execution").build();
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(file, settings)
                : new ExtentReportSession(file, settings, redactor, AtfConsoleSink.noOp(), environment)) {
            for (var invocation : invocations) publish(session, invocation);
        } finally { invocations.clear(); }
    }
    private void publish(AtfReportPorts.Session session, Invocation invocation) {
        var id = invocation.identity();
        try (var scenario = session.startScenario(id.testClass(), id.testPackage(), id.scenario(), id.tags())) {
            for (var observed : invocation.result().phases()) {
                try (var phase = scenario.startPhase(observed.phase().name().replace('_', ' '))) {
                    phase.detail("Test method", id.method());
                    observed.timing().ifPresent(time -> phase.executionTiming(new ReportPhaseTiming(time.elapsed(), time.intervals())));
                    if (observed.phase() == ComponentAssertionAudit.Phase.TEST_BODY) {
                        int ordinal = 0;
                        for (var input : invocation.inputs()) phase.observation(new ReportObservation("Component input: " + input.name(),
                                List.of(new ReportField("Input ordinal", Integer.toString(++ordinal)),
                                        new ReportField("Role", "Declared component fixture, not a native capture")),
                                List.of(new ReportObservation.Body("Full captured input", redactor.redactField(input.name(), input.value()),
                                        ReportObservation.Format.TEXT)), List.of()));
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
}
