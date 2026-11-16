package ro.whistlab.app.score;

import java.util.List;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.session.ScoreHistory;

/** Localized columns and values consumed by both Android and browser score renderers. */
public final class ScoreRows {
    private ScoreRows() { }

    public static List<String> headings(WhistUiText copy) {
        return List.of(copy.scoreColumnPlayer(), copy.scoreColumnBid(), copy.scoreColumnTricks(),
                copy.scoreColumnHand(), copy.scoreColumnTotal());
    }

    public static List<String> player(WhistUiText copy, GameSession.PlayerSnapshot player,
                                      GameSession.Snapshot state, ScoreHistory history) {
        String hand = "—";
        if (state.phase() == GamePhase.SCORE) {
            int delta = history.points(state.handNumber(), player.seat());
            hand = (delta > 0 ? "+" : "") + delta;
        }
        return List.of(copy.playerName(player), player.bid() == null ? "—" : player.bid().toString(),
                Integer.toString(player.tricksWon()), hand, Integer.toString(state.matchScore(player.seat())));
    }
}
