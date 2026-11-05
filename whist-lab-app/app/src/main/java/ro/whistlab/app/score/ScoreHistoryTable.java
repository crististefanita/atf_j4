package ro.whistlab.app.score;

import java.util.ArrayList;
import java.util.List;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.session.ScoreHistory;

/** Complete chronological schedule, projected for every seat without inventing unfinished scores. */
record ScoreHistoryTable(List<String> players, List<Row> rows) {
    enum Status { RECORDED, EARLIER, CURRENT, FUTURE }
    record Cell(String bid, String points, String total, String description) { }
    record Row(int hand, int cards, Status status, List<Cell> cells) {
        String cardLabel() { return Integer.toString(cards); }
    }
    ScoreHistoryTable { players = List.copyOf(players); rows = List.copyOf(rows); }

    static ScoreHistoryTable forMatch(GameSession.Snapshot state, ScoreHistory history, WhistUiText copy) {
        if (history.openingTotals().size() != state.players().size()) throw new IllegalArgumentException("Seat mismatch");
        List<Integer> schedule = GameRules.handSizes(state.mode(), state.players().size());
        if (history.completedHands() > schedule.size()) throw new IllegalArgumentException("History exceeds schedule");
        var columns = new ArrayList<ScoreHistoryColumn>();
        for (int seat = 0; seat < state.players().size(); seat++) columns.add(ScoreHistoryColumn.forPlayer(history, seat, copy));
        var rows = new ArrayList<Row>();
        for (int hand = 1; hand <= schedule.size(); hand++) {
            Status status = hand <= history.priorHands() ? Status.EARLIER
                    : hand <= history.completedHands() ? Status.RECORDED
                    : hand == state.handNumber() ? Status.CURRENT : Status.FUTURE;
            var cells = new ArrayList<Cell>();
            for (int seat = 0; seat < state.players().size(); seat++) {
                if (status == Status.RECORDED) {
                    var value = columns.get(seat).rows().get(history.completedHands() - hand);
                    cells.add(new Cell(value.bid(), value.points(), value.total(), copy.playerName(state.players().get(seat)) + ", " + value.description()));
                } else {
                    Integer bid = status == Status.CURRENT ? state.players().get(seat).bid() : null;
                    String total = status == Status.EARLIER && hand == history.priorHands()
                            ? history.openingTotals().get(seat).toString() : "—";
                    String description = copy.playerName(state.players().get(seat)) + ", " + copy.historyHand() + " " + hand
                            + ", " + copy.bidLabel() + " " + (bid == null ? copy.unknownBid() : bid)
                            + ", " + copy.historyPoints() + " " + copy.unknownBid() + ", " + copy.scoreColumnTotal() + " " + total
                            + ", " + (status == Status.EARLIER ? copy.unknownBid() : copy.historyPending());
                    cells.add(new Cell(bid == null ? "—" : bid.toString(), "—", total, description));
                }
            }
            rows.add(new Row(hand, schedule.get(hand - 1), status, List.copyOf(cells)));
        }
        return new ScoreHistoryTable(state.players().stream().map(copy::playerName)
                .collect(java.util.stream.Collectors.toList()), rows);
    }
}
