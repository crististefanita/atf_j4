package ro.whistlab.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ro.whistlab.core.ScoreHistory;

/** The rows read on screen or by a screen reader for the selected player. */
record ScoreHistoryColumn(String opening, List<Row> rows) {
    record Row(String hand, String bid, String points, String total, String description) { }

    static ScoreHistoryColumn forPlayer(ScoreHistory history, int seat, WhistUiText copy) {
        String opening = history.priorHands() == 0 ? ""
                : copy.earlierBalance(history.priorHands(), history.openingTotals().get(seat));
        List<Row> rows = new ArrayList<>();
        for (int hand = history.completedHands(); hand > history.priorHands(); hand--) {
            int points = history.points(hand, seat);
            String delta = points > 0 ? "+" + points : Integer.toString(points);
            String total = Integer.toString(history.total(hand, seat));
            Integer contract = history.bid(hand, seat);
            String bid = contract == null ? "—" : contract.toString();
            String description = copy.historyHand() + " " + hand + ", " + copy.bidLabel() + " "
                    + (contract == null ? copy.unknownBid() : bid) + ", " + copy.historyPoints() + " " + delta
                    + ", " + copy.scoreColumnTotal() + " " + total;
            rows.add(new Row(Integer.toString(hand), bid, delta, total, description));
        }
        return new ScoreHistoryColumn(opening, Collections.unmodifiableList(rows));
    }
}
