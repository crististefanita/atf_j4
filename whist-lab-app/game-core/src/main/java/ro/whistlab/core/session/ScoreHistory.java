package ro.whistlab.core.session;

import ro.whistlab.core.model.ImmutableLists;



import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Closing balances by seat. A migrated checkpoint can have an opening balance after earlier hands. */
public record ScoreHistory(int priorHands, List<Integer> openingTotals, List<List<Integer>> closingTotals,
        List<List<Integer>> recordedBids) {
    /** Older balances may precede the first hand with recorded bids. */
    public ScoreHistory(int priorHands, List<Integer> openingTotals, List<List<Integer>> closingTotals) {
        this(priorHands, openingTotals, closingTotals, Collections.emptyList());
    }

    public ScoreHistory {
        if (priorHands < 0 || openingTotals.isEmpty()) throw new IllegalArgumentException("Invalid score history origin");
        openingTotals = balances(openingTotals, openingTotals.size());
        closingTotals = vectors(closingTotals, openingTotals.size());
        recordedBids = vectors(recordedBids, openingTotals.size());
        if (recordedBids.size() > closingTotals.size()) throw new IllegalArgumentException("Too many bid rows");
    }

    private static List<List<Integer>> vectors(List<List<Integer>> values, int seats) {
        List<List<Integer>> rows = new ArrayList<>(values.size());
        for (List<Integer> row : values) rows.add(balances(row, seats));
        return Collections.unmodifiableList(rows);
    }

    public static ScoreHistory newMatch(int seats) {
        return new ScoreHistory(0, Collections.nCopies(seats, 0), Collections.emptyList());
    }

    public int completedHands() { return priorHands + closingTotals.size(); }

    public List<Integer> latestTotals() {
        return closingTotals.isEmpty() ? openingTotals : closingTotals.get(closingTotals.size() - 1);
    }

    public int total(int hand, int seat) { return row(hand).get(seat); }

    public int points(int hand, int seat) {
        int previous = hand == priorHands + 1 ? openingTotals.get(seat) : total(hand - 1, seat);
        return Math.subtractExact(total(hand, seat), previous);
    }

    public Integer bid(int hand, int seat) {
        row(hand);
        int index = recordedBids.size() - (completedHands() - hand) - 1;
        return index < 0 ? null : recordedBids.get(index).get(seat);
    }

    ScoreHistory finish(int hand, List<Integer> totals, List<Integer> bids) {
        if (hand != completedHands() + 1) throw new IllegalArgumentException("Score history must advance by one hand");
        List<List<Integer>> rows = new ArrayList<>(closingTotals);
        rows.add(totals);
        List<List<Integer>> contracts = new ArrayList<>(recordedBids);
        contracts.add(bids);
        return new ScoreHistory(priorHands, openingTotals, rows, contracts);
    }

    private List<Integer> row(int hand) {
        if (hand <= priorHands || hand > completedHands()) throw new IllegalArgumentException("Hand was not recorded: " + hand);
        return closingTotals.get(hand - priorHands - 1);
    }

    private static List<Integer> balances(List<Integer> values, int seats) {
        if (values.size() != seats || values.stream().anyMatch(java.util.Objects::isNull)) throw new IllegalArgumentException("Invalid player score vector");
        return ImmutableLists.copy(values);
    }
}
