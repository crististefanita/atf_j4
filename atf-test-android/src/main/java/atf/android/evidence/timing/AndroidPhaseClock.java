package atf.android.evidence.timing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongSupplier;

/** Execution boundaries and exclusive evidence axes; the clock is injectable without a runtime fake mode. */
public final class AndroidPhaseClock {
    private final String flowId;
    private final LongSupplier ticks;
    private final long origin;
    private final Map<String, Phase> phases = new LinkedHashMap<>();
    private long last;
    private Active active;
    private boolean evidence;

    public AndroidPhaseClock(String flowId, LongSupplier ticks) {
        this.flowId = Objects.requireNonNull(flowId);
        this.ticks = Objects.requireNonNull(ticks);
        origin = ticks.getAsLong();
    }

    public void enter(String name) {
        enter(name, phases.containsKey(name) ? phases.get(name).ordinal : phases.size());
    }

    public void enter(String name, int ordinal) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Missing phase name");
        if (active != null && active.phase.name.equals(name)) return;
        stop(false);
        var phase = phases.computeIfAbsent(name, key -> new Phase(key, ordinal));
        if (phase.ordinal != ordinal) throw new IllegalArgumentException("Android phase ordinal changed");
        active = new Active(phase, now());
    }

    public void stop(boolean partial) {
        if (evidence) throw new IllegalStateException("Cannot close a phase inside evidence work");
        if (active == null) return;
        var interval = new AndroidPhaseTiming.Interval(active.started, now(), active.capture, active.journal);
        active.phase.intervals.add(interval);
        active.phase.partial |= partial;
        active = null;
    }

    public Scope capture() { return evidence(true); }

    public void journal(IoWork work) throws IOException {
        try (var ignored = evidence(false)) { work.run(); }
    }

    public boolean running(String phase) { return active != null && active.phase.name.equals(phase); }

    public Optional<AndroidPhaseTiming> snapshot(String name) {
        var phase = phases.get(name);
        if (phase == null || phase.intervals.isEmpty() || running(name)) return Optional.empty();
        return Optional.of(new AndroidPhaseTiming(flowId, name, phase.ordinal, phase.intervals, phase.partial));
    }

    private Scope evidence(boolean capture) {
        if (active == null) return () -> { };
        if (evidence) throw new IllegalStateException("Android evidence measurements must not overlap");
        Active owner = active;
        long started = now();
        evidence = true;
        return new Scope() {
            private boolean closed;
            public void close() {
                if (closed) return;
                closed = true;
                evidence = false;
                long elapsed = now() - started;
                if (active != owner) throw new IllegalStateException("Android evidence changed its phase owner");
                if (capture) owner.capture = Math.addExact(owner.capture, elapsed);
                else owner.journal = Math.addExact(owner.journal, elapsed);
            }
        };
    }

    private long now() {
        long value = ticks.getAsLong() - origin;
        if (value < last) throw new IllegalStateException("Android monotonic clock moved backwards");
        last = value;
        return value;
    }

    @FunctionalInterface public interface Scope extends AutoCloseable { @Override void close(); }
    @FunctionalInterface public interface IoWork { void run() throws IOException; }

    private static final class Phase {
        final String name;
        final int ordinal;
        final ArrayList<AndroidPhaseTiming.Interval> intervals = new ArrayList<>();
        boolean partial;
        Phase(String name, int ordinal) { this.name = name; this.ordinal = ordinal; }
    }
    private static final class Active {
        final Phase phase;
        final long started;
        long capture;
        long journal;
        Active(Phase phase, long started) { this.phase = phase; this.started = started; }
    }
}
