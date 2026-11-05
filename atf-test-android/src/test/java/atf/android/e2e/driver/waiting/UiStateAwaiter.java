package atf.android.e2e.driver.waiting;

import java.time.Duration;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Așteaptă o stare observată, cu termen monoton și pauze stabilite de apelant. */
public final class UiStateAwaiter {
    @FunctionalInterface public interface Query { String read() throws Exception; }
    @FunctionalInterface public interface Condition { boolean matches(String hierarchy) throws Exception; }
    @FunctionalInterface public interface Pause { void sleep(long millis) throws InterruptedException; }
    @FunctionalInterface public interface TimeoutMessage { String describe(String lastHierarchy) throws Exception; }

    private final Query query;
    private final LongSupplier nanoTime;
    private final Pause pause;

    public UiStateAwaiter(Query query, LongSupplier nanoTime, Pause pause) {
        this.query = Objects.requireNonNull(query);
        this.nanoTime = Objects.requireNonNull(nanoTime);
        this.pause = Objects.requireNonNull(pause);
    }

    public String await(Duration timeout, long pollMillis, Condition condition, TimeoutMessage failure)
            throws Exception {
        long deadline = nanoTime.getAsLong() + timeout.toNanos();
        String latest;
        do {
            latest = query.read();
            if (condition.matches(latest)) return latest;
            pause.sleep(pollMillis);
        } while (nanoTime.getAsLong() < deadline);
        throw new IllegalStateException(failure.describe(latest));
    }
}
