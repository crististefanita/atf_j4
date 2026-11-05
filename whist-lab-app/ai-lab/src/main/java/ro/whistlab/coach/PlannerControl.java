package ro.whistlab.coach;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CancellationException;

/** Independent planner control; no engine, Android or ATF dependency. */
public final class PlannerControl {
    public enum Event { SAMPLE, WORLD, REJECTION, FEASIBILITY, ENUMERATION, ROLLOUT, PROOF, ENDGAME }
    public record Metrics(long workUnits, Map<Event, Long> events, String stopReason, long elapsedNanos) { }
    public static final class LimitExceeded extends RuntimeException {
        private LimitExceeded() { super("Independent work limit exceeded; no complete decision"); }
    }
    private static final ThreadLocal<Scope> ACTIVE = new ThreadLocal<>();
    private PlannerControl() { }

    public static Scope measure(long maximumWorkUnits) {
        if (maximumWorkUnits < 0) throw new IllegalArgumentException("Negative work limit");
        if (ACTIVE.get() != null) throw new IllegalStateException("Nested planner measurement");
        Scope scope = new Scope(maximumWorkUnits);
        ACTIVE.set(scope);
        return scope;
    }

    static void check() {
        Scope scope = ACTIVE.get();
        if (Thread.currentThread().isInterrupted()) {
            if (scope != null) scope.reason = "INTERRUPTED";
            throw new CancellationException("Independent search interrupted");
        }
        if (scope == null) return;
        if (scope.units == scope.limit) {
            scope.reason = "WORK_LIMIT";
            throw new LimitExceeded();
        }
        scope.units++;
    }

    static void event(Event event) {
        Scope scope = ACTIVE.get();
        if (scope != null) scope.events[event.ordinal()]++;
    }

    public static final class Scope implements AutoCloseable {
        private final Thread owner = Thread.currentThread();
        private final long started = System.nanoTime(), limit;
        private final long[] events = new long[Event.values().length];
        private long units, elapsed;
        private String reason = "NONE";
        private boolean closed;
        private Scope(long limit) { this.limit = limit; }

        public Metrics snapshot() {
            if (Thread.currentThread() != owner) throw new IllegalStateException("Measurement owner mismatch");
            Map<Event, Long> counts = new EnumMap<>(Event.class);
            for (Event event : Event.values()) counts.put(event, events[event.ordinal()]);
            return new Metrics(units, Map.copyOf(counts), reason, closed ? elapsed : System.nanoTime()-started);
        }

        @Override public void close() {
            if (Thread.currentThread() != owner) throw new IllegalStateException("Measurement owner mismatch");
            if (closed) return;
            if (ACTIVE.get() != this) throw new IllegalStateException("Measurement scope mismatch");
            elapsed = System.nanoTime()-started;
            closed = true;
            ACTIVE.remove();
        }
    }
}
