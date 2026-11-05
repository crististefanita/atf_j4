package ro.whistlab.app.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import java.util.ArrayList;
import java.util.List;

/** One layout-bound visual effect; detaching the board cancels all callbacks and animations. */
public final class TrickCollectionAnimator implements View.OnLayoutChangeListener, View.OnAttachStateChangeListener {
    private final ViewGroup board;
    private final int winner;
    private final List<Integer> seats;
    private final long remainingMillis;
    private final long createdAt = SystemClock.uptimeMillis();
    private boolean started;
    private boolean disposed;
    private ValueAnimator animator;

    private TrickCollectionAnimator(ViewGroup board, int winner, List<Integer> seats, long remainingMillis) {
        this.board = board;
        this.winner = winner;
        this.seats = List.copyOf(seats);
        this.remainingMillis = remainingMillis;
        board.addOnLayoutChangeListener(this);
        board.addOnAttachStateChangeListener(this);
        if (board.isLaidOut()) board.post(this::start);
    }

    public static TrickCollectionAnimator attach(ViewGroup board, int winner, List<Integer> seats, long remainingMillis) {
        return remainingMillis > 0 ? new TrickCollectionAnimator(board, winner, seats, remainingMillis) : null;
    }

    public void cancel() { dispose(); }

    private void start() {
        if (started || disposed || !board.isAttachedToWindow() || board.getWidth() == 0) return;
        started = true;
        board.removeOnLayoutChangeListener(this);
        var timing = TrickCollectionMotion.timing(remainingMillis, SystemClock.uptimeMillis() - createdAt);
        if (timing.durationMillis() == 0 || Build.VERSION.SDK_INT >= 26 && !ValueAnimator.areAnimatorsEnabled()) {
            dispose();
            return;
        }
        View target = board.findViewWithTag("table-seat:" + winner);
        if (target == null || target.getWidth() == 0 || target.getHeight() == 0) { dispose(); return; }
        var destination = center(target);
        var faces = new ArrayList<View>();
        var sources = new ArrayList<TrickCollectionMotion.Point>();
        for (int seat : seats) {
            View face = board.findViewWithTag("trick-face:" + seat);
            if (face == null || face.getWidth() == 0 || face.getHeight() == 0) { dispose(); return; }
            faces.add(face);
            sources.add(center(face));
            for (var parent = face.getParent(); parent instanceof ViewGroup group; parent = group.getParent()) {
                group.setClipChildren(false);
                group.setClipToPadding(false);
                if (group == board) break;
            }
        }
        apply(faces, sources, destination, timing.startFraction());
        animator = ValueAnimator.ofFloat(timing.startFraction(), 1);
        animator.setStartDelay(timing.delayMillis());
        animator.setDuration(timing.durationMillis());
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(value -> {
            if (!disposed) apply(faces, sources, destination, (Float) value.getAnimatedValue());
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) { animator = null; dispose(); }
        });
        animator.start();
    }

    private static TrickCollectionMotion.Point center(View view) {
        int[] position = new int[2];
        view.getLocationOnScreen(position);
        return new TrickCollectionMotion.Point(position[0] + view.getWidth() / 2f,
                position[1] + view.getHeight() / 2f);
    }

    private static void apply(List<View> faces, List<TrickCollectionMotion.Point> sources,
                              TrickCollectionMotion.Point target, float fraction) {
        for (int index = 0; index < faces.size(); index++) {
            View face = faces.get(index);
            var frame = TrickCollectionMotion.frame(sources.get(index), target, fraction);
            face.setTranslationX(frame.translationX());
            face.setTranslationY(frame.translationY());
            face.setScaleX(frame.scale());
            face.setScaleY(frame.scale());
            face.setAlpha(frame.alpha());
        }
    }

    private void dispose() {
        if (disposed) return;
        disposed = true;
        board.removeOnLayoutChangeListener(this);
        board.removeOnAttachStateChangeListener(this);
        if (animator != null) { animator.cancel(); animator = null; }
    }

    @Override public void onLayoutChange(View view, int left, int top, int right, int bottom,
            int oldLeft, int oldTop, int oldRight, int oldBottom) { start(); }
    @Override public void onViewAttachedToWindow(View view) { board.post(this::start); }
    @Override public void onViewDetachedFromWindow(View view) { dispose(); }
}
