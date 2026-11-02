package atf.tests.reporting;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import atf.reporting.model.diagnostic.ReportPhaseDiagnostic;
import atf.reporting.model.timing.ReportPhaseTiming;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Required evidence always passes through; callers explicitly identify optional context. */
public final class GameReportPhase implements AtfReportPorts.Phase {
    private final AtfReportPorts.Phase delegate;
    private final GameReportPresentation presentation;
    private boolean optionalTouched, inventoried;
    GameReportPhase(AtfReportPorts.Phase delegate, GameReportPresentation presentation) {
        this.delegate = delegate; this.presentation = presentation;
    }
    public static void optional(AtfReportPorts.Phase phase, ReportObservation observation) {
        if (!(phase instanceof GameReportPhase managed)) throw new IllegalArgumentException("Optional evidence requires a game report session");
        managed.optionalTouched = true;
        boolean keep;
        try { keep = managed.presentation.retainOptional(observation); }
        catch (IllegalArgumentException failure) { managed.incomplete(failure); throw failure; }
        if (keep) managed.observation(observation);
    }
    /** The producer has already attached complete values to this exact checkpoint. */
    public static AtfReportPorts.AssertionStep withCapturedValues(AtfReportPorts.Phase phase, String title) {
        if (!(phase instanceof GameReportPhase managed)) throw new IllegalArgumentException("Captured values require a game report session");
        return managed.assertionStep(title, false);
    }
    @Override public boolean supportsObservations() { return delegate.supportsObservations(); }
    @Override public void observation(ReportObservation observation) {
        try { presentation.required(observation).forEach(delegate::observation); }
        catch (IllegalArgumentException failure) { incomplete(failure); throw failure; }
    }
    private void incomplete(IllegalArgumentException failure) {
        if (presentation.refusal == null) presentation.refusal = failure;
        delegate.observation(new ReportObservation("Audit completeness — INCOMPLETE", List.of(
                new ReportField("Audit acceptance", "INCOMPLETE"), new ReportField("Reason", failure.getMessage()),
                new ReportField("Business outcome", "The captured outcome is independent; publication refusal is not a new game result")), List.of(), List.of()));
    }
    @Override public AtfReportPorts.AssertionStep startAssertionStep(String title) { return assertionStep(title, true); }
    private AtfReportPorts.AssertionStep assertionStep(String title, boolean captureFullValues) {
        var first = delegate.startAssertionStep(title);
        return new AtfReportPorts.AssertionStep() {
            private final List<AtfAssertion> rows = new ArrayList<>();
            private boolean completed;
            @Override public void assertions(Collection<AtfAssertion> values) {
                if (completed) throw new IllegalStateException("Assertion group is complete");
                rows.addAll(List.copyOf(values));
            }
            @Override public void complete(AtfReportStatus status) {
                if (completed) throw new IllegalStateException("Assertion group is complete");
                int size = presentation.policy.rowsPerGroup();
                for (int start = 0; start < Math.max(1, rows.size()); start += size) {
                    int end = Math.min(rows.size(), start + size);
                    String label = rows.size() <= size ? title : title + " · rows " + (start + 1) + "–" + end + " of " + rows.size();
                    try (var step = start == 0 ? first : delegate.startAssertionStep(label)) {
                        var partRows = rows.subList(start, end);
                        var partStatus = partRows.stream().map(AtfAssertion::resultStatus)
                                .reduce(AtfReportStatus.INFO, AtfReportStatus::worst);
                        if (end == rows.size()) partStatus = AtfReportStatus.worst(partStatus, status);
                        step.assertions(partRows); step.complete(partStatus);
                    }
                }
                completed = true;
                if (captureFullValues) completeValues(title, rows);
            }
            @Override public void close() {
                if (!completed) complete(rows.stream().map(AtfAssertion::resultStatus).reduce(AtfReportStatus.INFO, AtfReportStatus::worst));
            }
        };
    }
    private void completeValues(String title, List<AtfAssertion> rows) {
        var bodies = new ArrayList<ReportObservation.Body>();
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i); int ordinal = i + 1;
            if (row.name().length() > 240) bodies.add(body("Comparison " + ordinal + " · name", row.name()));
            if (row.expected().length() > 240) bodies.add(body("Comparison " + ordinal + " · expected", row.expected()));
            if (row.actual().length() > 240) bodies.add(body("Comparison " + ordinal + " · actual", row.actual()));
        }
        if (!bodies.isEmpty()) observation(new ReportObservation("Complete comparison values — " + title,
                List.of(new ReportField("Original assertion group", title)), bodies, List.of()));
    }
    private static ReportObservation.Body body(String label, String value) {
        return new ReportObservation.Body(label, value, ReportObservation.Format.TEXT);
    }
    private void inventory() {
        if (optionalTouched && !inventoried) { observation(presentation.inventory()); inventoried = true; }
    }
    @Override public void executionTiming(ReportPhaseTiming timing) { delegate.executionTiming(timing); }
    @Override public AtfReportPorts.Request startRequest(String title, String method, String path) { return delegate.startRequest(title, method, path); }
    @Override public void detail(String name, String value) { delegate.detail(name, value); }
    @Override public void diagnostic(ReportPhaseDiagnostic.Kind kind, String message) { delegate.diagnostic(kind, message); }
    @Override public void complete(AtfReportStatus status) { inventory(); delegate.complete(status); }
    @Override public void close() { inventory(); delegate.close(); }
}
