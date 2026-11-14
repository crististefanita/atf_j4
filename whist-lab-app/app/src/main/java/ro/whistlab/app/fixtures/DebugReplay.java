package ro.whistlab.app.fixtures;

import ro.whistlab.core.model.Card;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.session.ScoreHistory;

/** Contract for a debug-only recorded match driver used by the normal session flow. */
public interface DebugReplay {
    GameSession startHand(int index, ScoreHistory scores);
    int bid(int seat);
    Card nextCard();
    void played(Card card);
    void export(GameSession game);
}