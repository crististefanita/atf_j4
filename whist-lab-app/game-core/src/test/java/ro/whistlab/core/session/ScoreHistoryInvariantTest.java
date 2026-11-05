package ro.whistlab.core.session;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class ScoreHistoryInvariantTest {
    @Test public void invalidSeatsNeverBecomeUnknownContracts() {
        for (var history : List.of(ScoreHistory.newMatch(3).finish(1, List.of(5, -1, 5),
                List.of(0, 0, 0), List.of(0, 1, 0)),
                new ScoreHistory(0, List.of(0, 0, 0), List.of(List.of(5, -1, 5))))) {
            for (int seat : new int[]{-1, 3, Integer.MAX_VALUE}) {
                assertThrows(IllegalArgumentException.class, () -> history.bid(1, seat));
                assertThrows(IllegalArgumentException.class, () -> history.tricks(1, seat));
                assertThrows(IllegalArgumentException.class, () -> history.outcome(1, seat));
                assertThrows(IllegalArgumentException.class, () -> history.total(1, seat));
                assertThrows(IllegalArgumentException.class, () -> history.points(1, seat));
            }
        }
    }

    @Test public void suffixesRemainIndependentlyAlignedAfterAnUnknownPrefix() {
        var history = new ScoreHistory(2, List.of(10, 20, 30),
                List.of(List.of(15, 19, 35), List.of(20, 18, 40), List.of(19, 23, 45)),
                List.of(List.of(0, 0, 0), List.of(0, 0, 0)), List.of(List.of(1, 0, 0)));
        assertEquals(5, history.completedHands());
        assertNull(history.bid(3, 0));
        assertEquals(Integer.valueOf(0), history.bid(4, 0));
        assertNull(history.tricks(4, 0));
        assertEquals(ScoreHistory.Outcome.UNKNOWN, history.outcome(4, 0));
        assertEquals(ScoreHistory.Outcome.MISSED, history.outcome(5, 0));
        assertEquals(ScoreHistory.Outcome.EXACT, history.outcome(5, 1));
        assertEquals(-1, history.points(5, 0));
        for (int hand : new int[]{0, 2, 6}) assertThrows(IllegalArgumentException.class, () -> history.bid(hand, 0));
    }

    @Test public void appendedHandsPreserveTheirOriginAndDoNotInventEarlierTricks() {
        var old = new ScoreHistory(0, List.of(0, 0, 0), List.of(List.of(5, -1, 5)), List.of(List.of(0, 0, 0)));
        var next = old.finish(2, List.of(4, 4, 10), List.of(0, 0, 0), List.of(1, 0, 0));
        assertEquals(1, old.completedHands());
        assertNull(next.tricks(1, 0));
        assertEquals(Integer.valueOf(1), next.tricks(2, 0));
        assertEquals(ScoreHistory.Outcome.UNKNOWN, next.outcome(1, 0));
        assertEquals(ScoreHistory.Outcome.MISSED, next.outcome(2, 0));
        assertThrows(IllegalArgumentException.class, () -> old.finish(3, List.of(0, 0, 0), List.of(0, 0, 0), List.of(1, 0, 0)));
        assertThrows(IllegalArgumentException.class, () -> next.finish(3, List.of(0, 0, 0), List.of(0, 0, 0), null));
    }

    @Test public void constructorCopiesEveryMutableLevelAndExposesImmutableValues() {
        var opening = new ArrayList<>(List.of(0, 0, 0));
        var totals = new ArrayList<>(List.of(5, -1, 5));
        var bids = new ArrayList<>(List.of(0, 0, 0));
        var tricks = new ArrayList<>(List.of(0, 1, 0));
        var rows = new ArrayList<List<Integer>>(List.of(totals));
        var history = new ScoreHistory(0, opening, rows, List.of(bids), List.of(tricks));
        opening.set(0, 99); totals.set(0, 99); bids.set(0, 99); tricks.set(0, 99); rows.clear();
        assertEquals(5, history.total(1, 0));
        assertEquals(Integer.valueOf(0), history.bid(1, 0));
        assertEquals(Integer.valueOf(0), history.tricks(1, 0));
        assertThrows(UnsupportedOperationException.class, () -> history.latestTotals().set(0, 0));
        assertThrows(UnsupportedOperationException.class, () -> history.recordedTricks().get(0).set(0, 0));
        assertThrows(UnsupportedOperationException.class, () -> history.closingTotals().clear());
    }

    @Test public void malformedOriginsAndVectorsFailBeforeTheyCanBePublished() {
        assertThrows(IllegalArgumentException.class, () -> new ScoreHistory(Integer.MAX_VALUE,
                List.of(0), List.of(List.of(0))));
        assertThrows(IllegalArgumentException.class, () -> new ScoreHistory(0, List.of(0),
                List.of(List.of(0)), List.of(List.of(-1)), List.of(List.of(0))));
        assertThrows(IllegalArgumentException.class, () -> new ScoreHistory(0, null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ScoreHistory(0, List.of(0), null));
        assertThrows(IllegalArgumentException.class, () -> new ScoreHistory(0, List.of(0), List.of(List.of(0, 0))));
    }

    @Test public void signedBalancesStayLegalWhileArithmeticOverflowStaysExplicit() {
        var history = new ScoreHistory(0, List.of(-10), List.of(List.of(-12)));
        assertEquals(-2, history.points(1, 0));
        var huge = new ScoreHistory(0, List.of(Integer.MIN_VALUE), List.of(List.of(Integer.MAX_VALUE)));
        assertThrows(ArithmeticException.class, () -> huge.points(1, 0));
    }
}
