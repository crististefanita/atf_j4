package ro.whistlab.app.lifecycle;

import java.util.List;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.model.PlayedCard;

/** Keeps a completed trick on the table before the next turn becomes interactive. */
public final class CompletedTrickPause {
    static final long DISPLAY_MILLIS = 1_500L;

    private int displayedHandIndex = -1;
    private int displayedHistorySize = -1;
    private long displayDeadline;
    private List<PlayedCard> cards = List.of();

    public DisplayState evaluate(GameSession.Snapshot state, long uptimeMillis) {
        int playerCount = state.players().size();
        int historySize = state.history().size();
        boolean completedTrick = playerCount > 0 && state.currentTrick().isEmpty()
                && state.lastTrick().size() == playerCount && historySize >= playerCount
                && historySize % playerCount == 0;
        if (completedTrick && (displayedHandIndex != state.handIndex()
                || displayedHistorySize != historySize)) {
            displayedHandIndex = state.handIndex();
            displayedHistorySize = historySize;
            cards = state.lastTrick();
            displayDeadline = uptimeMillis + DISPLAY_MILLIS;
        }
        if (!completedTrick || displayedHandIndex != state.handIndex()
                || displayedHistorySize != historySize) return DisplayState.hidden();
        long remaining = displayDeadline - uptimeMillis;
        return remaining > 0 ? new DisplayState(true, remaining, cards) : DisplayState.hidden();
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
