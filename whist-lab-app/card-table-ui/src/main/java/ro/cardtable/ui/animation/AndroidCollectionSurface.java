package ro.cardtable.ui.animation;

import android.animation.ValueAnimator;
import android.os.Build;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Android measurements and resources only; TrickCollectionRun owns the event ordering. */
final class AndroidCollectionSurface implements TrickCollectionRun.Surface {
    private record Face(View view, float x, float y, float scaleX, float scaleY, float alpha) { }
    private record Clip(boolean children, boolean padding) { }
    private final ViewGroup board;
    private final List<Face> faces = new ArrayList<>();
    private final Map<ViewGroup, Clip> clips = new IdentityHashMap<>();
    private boolean measured;
    private Runnable unwatch;

    AndroidCollectionSurface(ViewGroup board) { this.board = board; }

    @Override public Runnable watch(Runnable ready, Runnable invalidated, Runnable resized) {
        View.OnLayoutChangeListener layout = (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (!measured) ready.run();
            else if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) resized.run();
        };
        View.OnAttachStateChangeListener attachment = new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View view) { board.post(ready); }
            @Override public void onViewDetachedFromWindow(View view) { invalidated.run(); }
        };
        unwatch = new Runnable() {
            private boolean closed;
            @Override public void run() {
                if (closed) return;
                closed = true;
                board.removeCallbacks(ready);
                board.removeOnLayoutChangeListener(layout);
                board.removeOnAttachStateChangeListener(attachment);
            }
        };
        board.addOnLayoutChangeListener(layout);
        board.addOnAttachStateChangeListener(attachment);
        if (board.isLaidOut()) board.post(ready);
        return unwatch;
    }

    @Override public boolean animationsEnabled() {
        return Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled();
    }

    @Override public TrickCollectionRun.Geometry measure(CollectionTarget request) {
        if (!board.isAttachedToWindow() || board.getWidth() == 0 || board.getHeight() == 0) return null;
        View target = board.findViewWithTag("table-seat:" + request.winner());
        if (!hasSize(target)) return null;
        var measuredFaces = new ArrayList<Face>();
        var positions = new ArrayList<TrickCollectionMotion.Point>();
        for (int seat : request.seats()) {
            View face = board.findViewWithTag("trick-face:" + seat);
            if (!hasSize(face)) return null;
            measuredFaces.add(new Face(face, face.getTranslationX(), face.getTranslationY(),
                    face.getScaleX(), face.getScaleY(), face.getAlpha()));
            positions.add(center(face));
        }
        faces.addAll(measuredFaces);
        measured = true;
        return new TrickCollectionRun.Geometry(positions, center(target));
    }

    private static boolean hasSize(View view) {
        return view != null && view.getWidth() > 0 && view.getHeight() > 0;
    }

    private static TrickCollectionMotion.Point center(View view) {
        int[] position = new int[2];
        view.getLocationOnScreen(position);
        return new TrickCollectionMotion.Point(position[0] + view.getWidth() * view.getScaleX() / 2f,
                position[1] + view.getHeight() * view.getScaleY() / 2f);
    }

    @Override public void render(List<TrickCollectionMotion.Frame> frames) {
        for (int index = 0; index < faces.size(); index++) {
            Face face = faces.get(index);
            for (var parent = face.view().getParent(); parent instanceof ViewGroup group; parent = group.getParent()) {
                if (!clips.containsKey(group)) {
                    clips.put(group, new Clip(group.getClipChildren(), group.getClipToPadding()));
                    group.setClipChildren(false);
                    group.setClipToPadding(false);
                }
                if (group == board) break;
            }
            var frame = frames.get(index);
            face.view().setTranslationX(face.x() + frame.translationX());
            face.view().setTranslationY(face.y() + frame.translationY());
            face.view().setScaleX(face.scaleX() * frame.scale());
            face.view().setScaleY(face.scaleY() * frame.scale());
            face.view().setAlpha(face.alpha() * frame.alpha());
        }
    }

    @Override public Runnable animate(TrickCollectionMotion.Timing timing, Consumer<Float> frame, Runnable finished) {
        var choreographer = Choreographer.getInstance();
        long start = SystemClock.uptimeMillis() + timing.delayMillis();
        final class CollectionFrames implements Choreographer.FrameCallback {
            private boolean stopped;

            @Override public void doFrame(long ignored) {
                if (stopped) return;
                long now = SystemClock.uptimeMillis();
                float progress = TrickCollectionMotion.progress(timing, now - start);
                frame.accept(progress);
                if (stopped) return;
                if (progress >= 1f) finished.run();
                else choreographer.postFrameCallback(this);
            }

            void cancel() {
                stopped = true;
                choreographer.removeFrameCallback(this);
            }
        }
        var frames = new CollectionFrames();
        choreographer.postFrameCallbackDelayed(frames, timing.delayMillis());
        return frames::cancel;
    }

    @Override public void release(boolean completed) {
        if (unwatch != null) unwatch.run();
        if (!completed) for (Face face : faces) {
            face.view().setTranslationX(face.x());
            face.view().setTranslationY(face.y());
            face.view().setScaleX(face.scaleX());
            face.view().setScaleY(face.scaleY());
            face.view().setAlpha(face.alpha());
        }
        clips.forEach((view, clip) -> {
            view.setClipChildren(clip.children());
            view.setClipToPadding(clip.padding());
        });
        clips.clear();
        faces.clear();
        measured = false;
    }
}
