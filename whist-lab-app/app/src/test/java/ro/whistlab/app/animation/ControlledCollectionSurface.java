package ro.whistlab.app.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Explicit event queue: cancelled callbacks may still be delivered to exercise the lifetime fence. */
final class ControlledCollectionSurface implements TrickCollectionRun.Surface {
    boolean ready = true;
    boolean enabled = true;
    boolean finishInline;
    boolean cancelWhenWatching;
    boolean readyWhenWatching;
    int watches, animations, stopped, unwatched, released;
    boolean completed;
    RuntimeException renderFailure, stopFailure, unwatchFailure, releaseFailure;
    Runnable becameReady, invalidated, resized, finished;
    Consumer<Float> frame;
    Runnable onRender = () -> { };
    TrickCollectionMotion.Timing timing;
    TrickCollectionRun.Geometry geometry;
    final List<List<TrickCollectionMotion.Frame>> draws = new ArrayList<>();

    @Override public Runnable watch(Runnable ready, Runnable invalidated, Runnable resized) {
        watches++;
        becameReady = ready;
        this.invalidated = invalidated;
        this.resized = resized;
        if (cancelWhenWatching) invalidated.run();
        if (readyWhenWatching) ready.run();
        return () -> { unwatched++; if (unwatchFailure != null) throw unwatchFailure; };
    }

    @Override public boolean animationsEnabled() { return enabled; }

    @Override public TrickCollectionRun.Geometry measure(TrickCollectionRequest request) {
        if (!ready) return null;
        if (geometry == null) {
            var points = new ArrayList<TrickCollectionMotion.Point>();
            for (int seat : request.seats()) points.add(new TrickCollectionMotion.Point(50 + seat * 40, 300));
            geometry = new TrickCollectionRun.Geometry(points,
                    new TrickCollectionMotion.Point(80 + request.winner() * 40, request.winner() == 0 ? 600 : 100));
        }
        return geometry;
    }

    @Override public void render(List<TrickCollectionMotion.Frame> frames) {
        if (released != 0) throw new AssertionError("Frame delivered to a released scene");
        draws.add(frames);
        if (renderFailure != null) throw renderFailure;
        onRender.run();
    }

    @Override public Runnable animate(TrickCollectionMotion.Timing timing, Consumer<Float> frame, Runnable finished) {
        animations++;
        this.timing = timing;
        this.frame = frame;
        this.finished = finished;
        if (finishInline) finished.run();
        return () -> {
            stopped++;
            finished.run();
            if (stopFailure != null) throw stopFailure;
        };
    }

    @Override public void release(boolean completed) {
        released++;
        this.completed = completed;
        if (releaseFailure != null) throw releaseFailure;
    }
}
