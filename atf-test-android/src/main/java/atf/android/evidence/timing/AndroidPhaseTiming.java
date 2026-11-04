package atf.android.evidence.timing;

import atf.reporting.model.timing.ReportPhaseEvidenceTiming;
import atf.reporting.model.timing.ReportPhaseTiming;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Original monotonic intervals owned by one captured Android flow and phase. */
public record AndroidPhaseTiming(String flowId, String phaseName, int ordinal,
        List<Interval> intervals, boolean partial) {
    public AndroidPhaseTiming {
        if (flowId == null || flowId.isBlank() || phaseName == null || phaseName.isBlank() || ordinal < 0) {
            throw new IllegalArgumentException("Missing Android timing owner");
        }
        intervals = List.copyOf(intervals);
        if (intervals.isEmpty()) throw new IllegalArgumentException("A captured phase needs an interval");
        long previous = -1;
        for (var interval : intervals) {
            if (interval.startedNanos() < previous) throw new IllegalArgumentException("Overlapping Android intervals");
            previous = interval.endedNanos();
        }
    }

    public long elapsedNanos() { return sum(Interval::elapsedNanos); }
    public long captureNanos() { return sum(Interval::captureNanos); }
    public long journalNanos() { return sum(Interval::journalNanos); }
    public long executionNanos() { return sum(Interval::executionNanos); }

    /** Current evidence projection is supplied separately; original trace IO is never replayed as device work. */
    public ReportPhaseTiming reporting(Duration publication) {
        Objects.requireNonNull(publication, "publication");
        return new ReportPhaseTiming(Duration.ofNanos(executionNanos()), intervals.size(),
                Optional.of(new ReportPhaseEvidenceTiming(Duration.ofNanos(captureNanos()), publication)));
    }

    private long sum(java.util.function.ToLongFunction<Interval> value) {
        return intervals.stream().mapToLong(value).reduce(0L, Math::addExact);
    }

    public record Interval(long startedNanos, long endedNanos, long captureNanos, long journalNanos) {
        public Interval {
            if (startedNanos < 0 || endedNanos < startedNanos || captureNanos < 0 || journalNanos < 0
                    || Math.addExact(captureNanos, journalNanos) > endedNanos - startedNanos) {
                throw new IllegalArgumentException("Invalid Android phase interval or evidence axes");
            }
        }
        public long elapsedNanos() { return endedNanos - startedNanos; }
        public long executionNanos() { return elapsedNanos() - captureNanos - journalNanos; }
    }
}
