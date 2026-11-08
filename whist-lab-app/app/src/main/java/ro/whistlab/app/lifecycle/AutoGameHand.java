package ro.whistlab.app.lifecycle;

import java.util.Objects;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

/** A transient automation lease bounded to one hand; it never carries over the score boundary. */
public final class AutoGameHand {
    private int handIndex = -1;

    public boolean activeFor(GameSession.Snapshot state) {
        Objects.requireNonNull(state);
        if (handIndex < 0) return false;
        if (state.handIndex() != handIndex || state.phase() == GamePhase.SCORE) {
            stop();
            return false;
        }
        return true;
    }

    public void start(GameSession.Snapshot state) {
        Objects.requireNonNull(state);
        if (state.phase() != GamePhase.BIDDING && state.phase() != GamePhase.PLAYING)
            throw new IllegalStateException("Auto-game is available only during the current hand");
        handIndex = state.handIndex();
    }

    public void stop() { handIndex = -1; }
}
