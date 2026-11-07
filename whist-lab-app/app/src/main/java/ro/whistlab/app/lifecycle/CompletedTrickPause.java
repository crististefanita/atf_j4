package ro.whistlab.app.lifecycle;

import java.util.List;
import java.util.Objects;
import ro.cardtable.ui.animation.TrickCollectionMotion;
import ro.whistlab.app.animation.TrickCollectionRequest;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.model.PlayedCard;

/** Counts visible time only; suspension never replays a trick or spends its remaining display budget. */
public final class CompletedTrickPause {
    static final long DISPLAY_MILLIS = TrickCollectionMotion.DISPLAY_MILLIS;

    private GameSession owner;
    private int displayedHandIndex = -1;
    private int displayedHistorySize = -1;
    private long lastUptime = -1;
    private long remainingMillis;
    private boolean suspended;
    private List<PlayedCard> cards = List.of();

    public void suspend(long uptimeMillis) {
        advance(uptimeMillis);
        suspended = true;
    }

    public void resume(long uptimeMillis) {
        advance(uptimeMillis);
        suspended = false;
    }

    public DisplayState evaluate(GameSession game, long uptimeMillis) {
        Objects.requireNonNull(game);
        validateClock(uptimeMillis);
        if (owner != game) {
            owner = game;
            displayedHandIndex = -1;
            displayedHistorySize = -1;
            remainingMillis = 0;
            cards = List.of();
        }
        return evaluate(game.snapshot(), uptimeMillis);
    }

    public DisplayState evaluate(GameSession.Snapshot state, long uptimeMillis) {
        Objects.requireNonNull(state);
        advance(uptimeMillis);
        if (!TrickCollectionRequest.from(state, state.lastTrick()).isPresent()) return DisplayState.hidden();
        int historySize = state.history().size();
        if (displayedHandIndex != state.handIndex() || displayedHistorySize != historySize) {
            displayedHandIndex = state.handIndex();
            displayedHistorySize = historySize;
            cards = state.lastTrick();
            remainingMillis = DISPLAY_MILLIS;
        }
        return remainingMillis > 0 ? new DisplayState(true, remainingMillis, cards) : DisplayState.hidden();
    }

    private void validateClock(long uptimeMillis) {
        if (uptimeMillis < 0 || lastUptime >= 0 && uptimeMillis < lastUptime)
            throw new IllegalArgumentException("Display time must be monotonic and nonnegative");
    }

    private void advance(long uptimeMillis) {
        validateClock(uptimeMillis);
        if (!suspended && lastUptime >= 0) remainingMillis = Math.max(0, remainingMillis - (uptimeMillis - lastUptime));
        lastUptime = uptimeMillis;
    }

    public record DisplayState(boolean active, long remainingMillis, List<PlayedCard> cards) {
        public DisplayState {
            cards = List.copyOf(cards);
        }

        private static DisplayState hidden() {
            return new DisplayState(false, 0, List.of());
        }
    }
}
