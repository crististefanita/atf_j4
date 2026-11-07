package ro.cardtable.ui.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** Owner-thread collection lifetime, shared by the Android adapter and deterministic components. */
public final class TrickCollectionRun {
    public record Geometry(List<TrickCollectionMotion.Point> cards, TrickCollectionMotion.Point target) {
        public Geometry { cards = List.copyOf(cards); Objects.requireNonNull(target); }
    }

    public interface Surface {
        Runnable watch(Runnable ready, Runnable invalidated, Runnable resized);
        boolean animationsEnabled();
        Geometry measure(CollectionTarget request);
        void render(List<TrickCollectionMotion.Frame> frames);
        Runnable animate(TrickCollectionMotion.Timing timing, Consumer<Float> frame, Runnable finished);
        void release(boolean completed);
    }

    private enum State { NEW, WAITING, RUNNING, CANCELLED, FINISHED }
    private final CollectionTarget request;
    private final long remainingMillis;
    private final long createdAt;
    private final LongSupplier clock;
    private final Surface surface;
    private final Runnable resized;
    private State state = State.NEW;
    private Geometry geometry;
    private float lastFraction = -1;
    private Runnable stopWatching;
    private Runnable stopAnimation;

    public TrickCollectionRun(CollectionTarget request, long remainingMillis,
                              LongSupplier clock, Surface surface) {
        this(request, remainingMillis, clock, surface, () -> { });
    }

    public TrickCollectionRun(CollectionTarget request, long remainingMillis,
                              LongSupplier clock, Surface surface, Runnable resized) {
        this.request = Objects.requireNonNull(request);
        TrickCollectionMotion.timing(remainingMillis, 0);
        this.remainingMillis = remainingMillis;
        this.clock = Objects.requireNonNull(clock);
        this.surface = Objects.requireNonNull(surface);
        this.resized = Objects.requireNonNull(resized);
        createdAt = clock.getAsLong();
        if (createdAt < 0) throw new IllegalArgumentException("Invalid collection clock");
    }

    public void start() {
        if (state != State.NEW) return;
        state = State.WAITING;
        try {
            Runnable watch = Objects.requireNonNull(surface.watch(this::ready, this::cancel, this::resize));
            if (closed()) watch.run(); else stopWatching = watch;
            ready();
        } catch (RuntimeException | Error failure) { fail(failure); }
    }

    private void ready() {
        if (state != State.WAITING) return;
        try {
            if (!surface.animationsEnabled()) { cancel(); return; }
            Geometry measured = surface.measure(request);
            if (measured == null) return;
            if (measured.cards().size() != request.seats().size())
                throw new IllegalArgumentException("Every trick face needs a measured origin");
            long now = clock.getAsLong();
            if (now < createdAt) throw new IllegalArgumentException("Collection clock moved backwards");
            var timing = TrickCollectionMotion.timing(remainingMillis, now - createdAt);
            if (timing.durationMillis() == 0) { cancel(); return; }
            geometry = measured;
            state = State.RUNNING;
            frame(timing.startFraction());
            if (closed()) return;
            Runnable animation = Objects.requireNonNull(surface.animate(timing, this::frame, this::finish));
            if (closed()) animation.run(); else stopAnimation = animation;
        } catch (RuntimeException | Error failure) { fail(failure); }
    }

    private void frame(float fraction) {
        if (state != State.RUNNING) return;
        try {
            if (!Float.isFinite(fraction)) throw new IllegalArgumentException("Invalid collection frame");
            float progress = Math.max(0, Math.min(1, fraction));
            if (progress <= lastFraction) return;
            var frames = new ArrayList<TrickCollectionMotion.Frame>();
            for (var origin : geometry.cards()) frames.add(TrickCollectionMotion.frame(origin, geometry.target(), progress));
            lastFraction = progress;
            surface.render(List.copyOf(frames));
        } catch (RuntimeException | Error failure) { fail(failure); }
    }

    private void finish() {
        if (state != State.RUNNING) return;
        frame(1);
        if (!closed()) close(true, null);
    }

    public void cancel() {
        if (!closed()) close(false, null);
    }

    private void resize() {
        if (closed()) return;
        cancel();
        resized.run();
    }

    private boolean closed() { return state == State.CANCELLED || state == State.FINISHED; }

    private void fail(Throwable failure) {
        if (closed()) rethrow(failure);
        close(false, failure);
    }

    private void close(boolean completed, Throwable primary) {
        state = completed ? State.FINISHED : State.CANCELLED;
        Runnable animation = stopAnimation, watch = stopWatching;
        stopAnimation = null;
        stopWatching = null;
        geometry = null;
        Throwable failure = primary;
        for (Runnable action : new Runnable[]{animation, watch, () -> surface.release(completed)}) {
            if (action == null) continue;
            try { action.run(); }
            catch (RuntimeException | Error cleanup) {
                if (failure == null) failure = cleanup;
                else if (failure != cleanup) failure.addSuppressed(cleanup);
            }
        }
        if (failure != null) rethrow(failure);
    }

    private static void rethrow(Throwable failure) {
        if (failure instanceof RuntimeException runtime) throw runtime;
        throw (Error) failure;
    }
}
