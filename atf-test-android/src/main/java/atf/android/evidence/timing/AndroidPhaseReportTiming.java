package atf.android.evidence.timing;

import atf.reporting.duration.AtfDurationFormatter;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import atf.reporting.ui.UiExtentReportSession;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;

/** Consumer projection shared by immediate publication and captured-trace republication. */
public final class AndroidPhaseReportTiming implements AutoCloseable {
    private final UiExtentReportSession.UiPhase phase;
    private final Optional<AndroidPhaseTiming> original;
    private final long dispatchMillis;
    private final LongSupplier ticks;
    private final long started;
    private boolean closed;

    public AndroidPhaseReportTiming(UiExtentReportSession.UiPhase phase, String flowId, String name, int ordinal,
            Optional<AndroidPhaseTiming> original, long dispatchMillis, LongSupplier ticks) {
        this.phase = java.util.Objects.requireNonNull(phase);
        this.original = java.util.Objects.requireNonNull(original);
        this.dispatchMillis = dispatchMillis;
        this.ticks = java.util.Objects.requireNonNull(ticks);
        original.ifPresent(value -> {
            if (!value.flowId().equals(flowId) || !value.phaseName().equals(name) || value.ordinal() != ordinal) {
                throw new IllegalArgumentException("Android timing belongs to another flow/phase");
            }
        });
        started = ticks.getAsLong();
    }

    @Override public void close() {
        if (closed) return;
        long elapsed = ticks.getAsLong() - started;
        if (elapsed < 0) throw new IllegalStateException("Android publication clock moved backwards");
        var fields = new ArrayList<ReportField>();
        fields.add(new ReportField("ADB dispatch total", AtfDurationFormatter.formatMillis(dispatchMillis)));
        fields.add(new ReportField("Checkpoint projection before final HTML flush", duration(elapsed)));
        var bodies = new ArrayList<ReportObservation.Body>();
        if (original.isPresent()) {
            var timing = original.orElseThrow();
            fields.add(new ReportField("Original phase execution", duration(timing.executionNanos())));
            fields.add(new ReportField("Original phase elapsed", duration(timing.elapsedNanos())));
            fields.add(new ReportField("Original PNG and hierarchy capture", duration(timing.captureNanos())));
            fields.add(new ReportField("Original trace persistence", duration(timing.journalNanos())));
            fields.add(new ReportField("Original interval state", timing.partial() ? "PARTIAL" : "COMPLETE"));
            fields.add(new ReportField("Measured interval count", Integer.toString(timing.intervals().size())));
            String intervals = timing.intervals().stream().map(value -> "start=" + value.startedNanos()
                    + " ns; end=" + value.endedNanos() + " ns; capture=" + value.captureNanos()
                    + " ns; trace IO=" + value.journalNanos() + " ns")
                    .collect(java.util.stream.Collectors.joining("\n"));
            bodies.add(new ReportObservation.Body("Original monotonic offsets from this trace's origin", intervals, ReportObservation.Format.TEXT));
            phase.executionTiming(timing.reporting(Duration.ofNanos(elapsed)));
        } else {
            fields.add(new ReportField("Original phase execution", "UNAVAILABLE — legacy or unfinished capture"));
            fields.add(new ReportField("Legacy badge basis", "Captured dispatch/child durations; original phase execution unavailable"));
        }
        phase.observation(new ReportObservation("Android phase timing — separate measured axes", fields, bodies, List.of()));
        closed = true;
    }

    private static String duration(long nanos) {
        return AtfDurationFormatter.formatMillis(AtfDurationFormatter.elapsedMillis(Duration.ofNanos(nanos)));
    }
}
