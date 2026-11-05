package ro.whistlab.core.ai.simulation;

import java.util.concurrent.CancellationException;

/** Cooperative cancellation preserves the caller's interrupt status and the normal random stream. */
public final class SearchCancellation {
    private SearchCancellation() { }
    public static void check() {
        if (Thread.currentThread().isInterrupted()) {
            SearchWork.interrupted();
            throw new CancellationException("AI search interrupted");
        }
        SearchWork.checkpoint();
    }
}
