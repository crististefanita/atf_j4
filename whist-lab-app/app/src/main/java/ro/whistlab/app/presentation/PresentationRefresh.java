package ro.whistlab.app.presentation;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Owner-thread viewport refresh queue. Delivery reads current geometry through the render port.
 * The owner must cancel before discarding queued callbacks or replacing the presentation lifetime.
 */
public final class PresentationRefresh {
    private final Runnable suspend;
    private final Runnable render;
    private final Consumer<Runnable> queue;
    private final BooleanSupplier available;
    private long generation;
    private boolean pending;

    public PresentationRefresh(Runnable suspend, Runnable render, Consumer<Runnable> queue, BooleanSupplier available) {
        this.suspend = Objects.requireNonNull(suspend);
        this.render = Objects.requireNonNull(render);
        this.queue = Objects.requireNonNull(queue);
        this.available = Objects.requireNonNull(available);
    }

    public void request() {
        if (pending) return;
        long ticket = ++generation;
        pending = true;
        try {
            suspend.run();
            if (ticket != generation) return;
            queue.accept(() -> deliver(ticket));
        } catch (RuntimeException | Error failure) {
            if (ticket == generation) cancel();
            throw failure;
        }
    }

    private void deliver(long ticket) {
        if (!pending || ticket != generation) return;
        pending = false;
        boolean canRender = available.getAsBoolean();
        if (ticket == generation && canRender) render.run();
    }

    public void cancel() {
        generation++;
        pending = false;
    }
}
