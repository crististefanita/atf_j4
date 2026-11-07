package ro.cardtable.ui.animation;

import android.os.SystemClock;
import android.view.ViewGroup;

/** Binds the tested collection lifetime to one Android board. */
public final class TrickCollectionAnimator {
    private final TrickCollectionRun run;

    private TrickCollectionAnimator(ViewGroup board, CollectionTarget request, long remainingMillis, Runnable resized) {
        run = new TrickCollectionRun(request, remainingMillis, SystemClock::uptimeMillis,
                new AndroidCollectionSurface(board), resized);
        run.start();
    }

    public static TrickCollectionAnimator attach(ViewGroup board, CollectionTarget request,
                                                 long remainingMillis, Runnable resized) {
        return remainingMillis > 0 ? new TrickCollectionAnimator(board, request, remainingMillis, resized) : null;
    }

    public void cancel() { run.cancel(); }
}
