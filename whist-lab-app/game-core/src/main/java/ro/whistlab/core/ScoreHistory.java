package ro.whistlab.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Closing balances by seat. A migrated checkpoint can have an opening balance after earlier hands. */
public record ScoreHistory(int priorHands, List<Integer> openingTotals, List<List<Integer>> closingTotals) {
    public ScoreHistory {
        if (priorHands < 0 || openingTotals.isEmpty()) throw new IllegalArgumentException("Invalid score history origin");
        openingTotals = balances(openingTotals, openingTotals.size());
        List<List<Integer>> rows = new ArrayList<>(closingTotals.size());
        for (List<Integer> row : closingTotals) rows.add(balances(row, openingTotals.size()));
        closingTotals = Collections.unmodifiableList(rows);
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

    ScoreHistory finish(int hand, List<Integer> totals) {
        if (hand != completedHands() + 1) throw new IllegalArgumentException("Score history must advance by one hand");
        List<List<Integer>> rows = new ArrayList<>(closingTotals);
        rows.add(totals);
        return new ScoreHistory(priorHands, openingTotals, rows);
    }

    private List<Integer> row(int hand) {
        if (hand <= priorHands || hand > completedHands()) throw new IllegalArgumentException("Hand was not recorded: " + hand);
        return closingTotals.get(hand - priorHands - 1);
    }

    private static List<Integer> balances(List<Integer> values, int seats) {
        if (values.size() != seats || values.stream().anyMatch(java.util.Objects::isNull)) throw new IllegalArgumentException("Invalid player score vector");
        return GameRules.immutableCopy(values);
    }
}
