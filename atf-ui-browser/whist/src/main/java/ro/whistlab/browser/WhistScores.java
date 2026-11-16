package ro.whistlab.browser;

import java.util.ArrayList;
import java.util.List;
import ro.cardtable.browser.BrowserGame.History;
import ro.whistlab.core.session.GameSession;

final class WhistScores {
    private WhistScores() { }

    static History history(GameSession game) {
        var state = game.snapshot();
        var scores = game.scoreHistory();
        var labels = new ArrayList<String>();
        labels.add("Mână");
        state.players().forEach(player -> labels.add(player.name() + " (L / P)"));
        var rows = new ArrayList<List<String>>();
        for (int hand = scores.priorHands() + 1; hand <= scores.completedHands(); hand++) {
            var row = new ArrayList<String>();
            row.add(Integer.toString(hand));
            for (var player : state.players()) {
                int seat = player.seat();
                row.add(scores.bid(hand, seat) + " / " + scores.total(hand, seat));
            }
            rows.add(row);
        }
        return new History(labels, rows);
    }
}
