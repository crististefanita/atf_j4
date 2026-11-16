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

    public enum Progress { MATCH, WAIT, READ_AGAIN, RENEW_TIMEOUT }

    @FunctionalInterface public interface ProgressCondition {
        Progress observe(String hierarchy) throws Exception;
    }

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
        return awaitProgress(timeout, pollMillis,
                xml -> condition.matches(xml) ? Progress.MATCH : Progress.WAIT, failure);
    }

    public String awaitProgress(Duration timeout, long pollMillis, ProgressCondition condition,
            TimeoutMessage failure) throws Exception {
        long budget = timeout.toNanos();
        long started = nanoTime.getAsLong();
        String latest;
        do {
            latest = query.read();
            switch (condition.observe(latest)) {
                case MATCH -> { return latest; }
                case WAIT -> pause.sleep(pollMillis);
                case READ_AGAIN -> { }
                case RENEW_TIMEOUT -> started = nanoTime.getAsLong();
            }
        } while (nanoTime.getAsLong() - started < budget);
        throw new IllegalStateException(failure.describe(latest));
    }
}
