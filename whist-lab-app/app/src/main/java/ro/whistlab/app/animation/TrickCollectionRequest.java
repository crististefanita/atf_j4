package ro.whistlab.app.animation;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import ro.cardtable.ui.animation.CollectionTarget;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

/** Complete public trick and the engine's winner; never infers a winner from screen geometry. */
public final class TrickCollectionRequest {
    private TrickCollectionRequest() { }

    public static Optional<CollectionTarget> from(GameSession.Snapshot state, List<PlayedCard> cards) {
        Objects.requireNonNull(state);
        Objects.requireNonNull(cards);
        int count = state.players().size();
        int history = state.history().size();
        if (count < 3 || count > 6 || cards.size() != count || !cards.equals(state.lastTrick())
                || !state.currentTrick().isEmpty() || history < count || history % count != 0
                || !state.history().subList(history - count, history).equals(cards)
                || state.lastTrickWinnerSeat() < 0 || state.lastTrickWinnerSeat() >= count
                || state.phase() != GamePhase.PLAYING && state.phase() != GamePhase.SCORE) return Optional.empty();
        var seats = cards.stream().map(PlayedCard::seat).collect(java.util.stream.Collectors.toList());
        if (new HashSet<>(seats).size() != count || seats.stream().anyMatch(seat -> seat < 0 || seat >= count)
                || cards.stream().map(PlayedCard::card).distinct().count() != count) return Optional.empty();
        return Optional.of(new CollectionTarget(state.lastTrickWinnerSeat(), seats));
    }
}
