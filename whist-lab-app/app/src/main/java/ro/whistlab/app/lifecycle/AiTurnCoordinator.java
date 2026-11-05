package ro.whistlab.app.lifecycle;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Computes off the owner thread; cancellation invalidates even an already queued delivery. */
public final class AiTurnCoordinator implements AutoCloseable {
    private final ExecutorService worker;
    private final Executor owner;
    private FutureTask<Void> pending;
    private long generation;
    private boolean closed;

    public AiTurnCoordinator(Executor owner) {
        this(Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "whist-ai");
            thread.setDaemon(true);
            return thread;
        }), owner);
    }

    /** The coordinator owns and closes the supplied worker. */
    public AiTurnCoordinator(ExecutorService worker, Executor owner) {
        this.worker = Objects.requireNonNull(worker);
        this.owner = Objects.requireNonNull(owner);
    }

    public <T> boolean submit(Supplier<T> calculation, Consumer<T> apply, Consumer<Throwable> failure) {
        Objects.requireNonNull(calculation);
        Objects.requireNonNull(apply);
        Objects.requireNonNull(failure);
        FutureTask<Void> task;
        synchronized (this) {
            if (closed) throw new IllegalStateException("AI coordinator is closed");
            if (pending != null) return false;
            long ticket = ++generation;
            task = new FutureTask<>(() -> {
                T result;
                try {
                    result = calculation.get();
                } catch (Throwable error) {
                    dispatch(ticket, () -> failure.accept(error));
                    return null;
                }
                dispatch(ticket, () -> apply.accept(result));
                return null;
            });
            pending = task;
        }
        try {
            worker.execute(task);
        } catch (RuntimeException rejected) {
            synchronized (this) {
                if (pending == task) { pending = null; generation++; }
            }
            task.cancel(true);
            throw rejected;
        }
        return true;
    }

    private void dispatch(long ticket, Runnable delivery) {
        synchronized (this) {
            if (closed || ticket != generation || pending == null) return;
        }
        try {
            owner.execute(() -> {
                synchronized (AiTurnCoordinator.this) {
                    if (closed || ticket != generation || pending == null) return;
                    pending = null;
                    // Serialize invalidation with application, including cancellation from another thread.
                    delivery.run();
                }
            });
        } catch (RuntimeException rejected) {
            synchronized (this) {
                if (ticket == generation) { pending = null; generation++; }
            }
            throw rejected;
        }
    }

    public synchronized boolean isPending() { return pending != null; }

    public synchronized void cancel() {
        generation++;
        if (pending != null) pending.cancel(true);
        pending = null;
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        cancel();
        worker.shutdownNow();
    }
}
