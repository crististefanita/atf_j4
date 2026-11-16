package ro.whistlab.browser;

import java.util.ArrayList;
import java.util.List;
import ro.cardtable.browser.BrowserGame.History;
import ro.cardtable.browser.BrowserGame.HistoryCell;
import ro.cardtable.browser.BrowserGame.HistoryGrid;
import ro.cardtable.browser.BrowserGame.HistoryRow;
import ro.whistlab.app.score.ScoreHistoryTable;
import ro.whistlab.core.session.ScoreHistory;
import ro.cardtable.browser.BrowserGame.Score;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.core.session.GameSession;

final class WhistScores {
    private WhistScores() { }

    static Score hand(GameSession game, WhistUiText copy) {
        var state = game.snapshot();
        var scores = game.scoreHistory();
        var columns = List.of(copy.scoreColumnPlayer(), copy.scoreColumnBid(), copy.scoreColumnTricks(),
                copy.scoreColumnHand(), copy.scoreColumnTotal());
        var rows = state.players().stream().map(player -> {
            int points = scores.points(state.handNumber(), player.seat());
            return List.of(copy.playerName(player), String.valueOf(player.bid()), Integer.toString(player.tricksWon()),
                    (points > 0 ? "+" : "") + points, Integer.toString(state.matchScore(player.seat())));
        }).toList();
        return new Score(copy.scoreSheet(), new History(columns, rows));
    }

    static History history(GameSession game, WhistUiText copy) {
        var state = game.snapshot();
        var scores = game.scoreHistory();
        var labels = new ArrayList<String>();
        labels.add(copy.historyHand());
        state.players().forEach(player -> labels.add(copy.playerName(player) + " (" + copy.historyBidHeading() + " / " + copy.historyPointsHeading() + ")"));
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
        var table = ScoreHistoryTable.forMatch(state, scores, copy);
        var details = table.rows().stream().map(row -> new HistoryRow(row.cards(), row.status().name(),
                row.cells().stream().map(cell -> new HistoryCell(cell.bid(), cell.total(), cell.description(),
                        cell.outcome() == ScoreHistory.Outcome.MISSED, cell.seriesPoints())).toList())).toList();
        return new History(labels, rows, new HistoryGrid(table.players(), details, copy.cardsHeading(),
                copy.historyBidHeading(), copy.historyPointsHeading(), copy.historyLegend()));
    }
}
