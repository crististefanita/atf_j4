package ro.whistlab.app.fixtures;

import java.util.List;
import java.util.Objects;
import ro.whistlab.core.model.PlayedCard;
import ro.whistlab.core.session.GameSession;

/** Immutable startup instruction produced by the active build variant. */
public record DebugLaunch(
        GameSession game,
        DebugReplay replay,
        List<PlayedCard> renderedCards,
        boolean renderImmediately,
        boolean persistBeforeDrive) {
    public DebugLaunch {
        Objects.requireNonNull(game);
        renderedCards = List.copyOf(renderedCards);
        if (renderImmediately && renderedCards.isEmpty()) {
            throw new IllegalArgumentException("Immediate rendering requires captured trick cards");
        }
    }
}