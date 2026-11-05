package ro.whistlab.app.score;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;
import ro.whistlab.core.session.ScoreHistory;

public final class ScoreHistoryMeasureComponentTest {
    private static final ScoreHistoryMeasure.Labels LABELS = new ScoreHistoryMeasure.Labels("Cards", "B", "P");

    @Test public void allDigitsSignsAndTheFullNameParticipateInWidthMeasurement() {
        var table = table(List.of("Al", "Dora", "Vlad"), List.of(
                cell("17", "-100", "1000"), cell("0", "+5", "9"), cell("—", "—", "—")));
        var size = ScoreHistoryMeasure.measure(table, LABELS, text -> text.length() * 10, text -> text.length() * 8, 2);
        assertEquals(44, size.hand());
        assertEquals(28, size.bid());
        assertEquals(44, size.points());
        assertEquals(44, size.name());
    }

    @Test public void theLastSeatAndLastRowCannotBeSkippedByAnOptimization() {
        var small = table(List.of("A", "B", "C"), List.of(cell("0", "5", "5"), cell("0", "5", "5"), cell("0", "5", "5")));
        var last = new ScoreHistoryTable.Row(24, 1, ScoreHistoryTable.Status.RECORDED,
                List.of(cell("0", "5", "5"), cell("0", "5", "5"), cell("17", "-1000", "-123456")));
        var table = new ScoreHistoryTable(small.players(), List.of(small.rows().get(0), last));
        var size = ScoreHistoryMeasure.measure(table, LABELS, String::length, String::length, 1);
        assertEquals(6, size.bid());
        assertEquals(7, size.points());
    }

    @Test public void captionAndNumericFontsHaveIndependentMeasurements() {
        var table = new ScoreHistoryTable(List.of("A", "B", "C"), List.of());
        var size = ScoreHistoryMeasure.measure(table, LABELS, text -> 2, text -> 70, 1);
        assertEquals(72, size.hand());
        assertEquals(4, size.bid());
        assertEquals(4, size.points());
    }

    @Test public void enlargedFontsFitAtEverySupportedFractionalDensity() {
        var table = table(List.of("LongName", "Dora", "Vlad"),
                List.of(cell("17", "-100", "1000"), cell("0", "+5", "9"), cell("—", "—", "—")));
        for (float density : new float[]{1, 1.5f, 2.25f, 2.625f, 2.75f, 3.5f}) {
            int previous = 0;
            for (float scale : new float[]{1, 1.15f, 1.3f, 1.5f}) {
                int padding = Math.round(density);
                var size = ScoreHistoryMeasure.measure(table, LABELS,
                        text -> (int) Math.ceil(text.length() * 7 * density * scale),
                        text -> (int) Math.ceil(text.length() * 6 * density * scale), padding);
                var grid = size.fit(ScoreHistoryGridMetrics.forDensity(3, density));
                assertTrue(grid.bidWidthPx() >= size.bid());
                assertTrue(grid.pointsWidthPx() >= size.points());
                assertTrue(grid.handWidthPx() >= size.hand());
                assertTrue(grid.playerWidthPx() >= size.name());
                assertTrue(grid.tableWidthPx() >= previous);
                previous = grid.tableWidthPx();
            }
        }
    }

    @Test public void aFontFailureKeepsItsOriginalCauseInsteadOfReturningZeroWidth() {
        var failure = new IllegalStateException("missing glyph metrics");
        var actual = assertThrows(IllegalStateException.class, () -> ScoreHistoryMeasure.measure(
                table(List.of("A", "B", "C"), List.of()), LABELS, text -> { throw failure; }, String::length, 1));
        assertSame(failure, actual);
    }

    @Test public void invalidMetricsAndOverflowFailBeforeLayout() {
        var table = table(List.of("A", "B", "C"), List.of());
        assertThrows(IllegalArgumentException.class, () -> ScoreHistoryMeasure.measure(table, LABELS, text -> -1, String::length, 1));
        assertThrows(IllegalArgumentException.class, () -> ScoreHistoryMeasure.measure(table, LABELS, String::length, text -> -1, 1));
        assertThrows(IllegalArgumentException.class, () -> ScoreHistoryMeasure.measure(table, LABELS, String::length, String::length, -1));
        assertThrows(IllegalArgumentException.class, () -> ScoreHistoryMeasure.measure(table, LABELS, text -> Integer.MAX_VALUE, String::length, 1));
    }

    private static ScoreHistoryTable table(List<String> players, List<ScoreHistoryTable.Cell> cells) {
        return new ScoreHistoryTable(players, cells.isEmpty() ? List.of() :
                List.of(new ScoreHistoryTable.Row(1, 8, ScoreHistoryTable.Status.RECORDED, cells)));
    }

    private static ScoreHistoryTable.Cell cell(String bid, String points, String total) {
        return new ScoreHistoryTable.Cell(bid, points, total, "fixture", ScoreHistory.Outcome.UNKNOWN, 0);
    }

    @Test public void hiddenTotalsNeverWidenTheTwoVisibleColumns() {
        var shortTotal = table(List.of("A", "B", "C"), List.of(cell("0", "+5", "5")));
        var longTotal = table(List.of("A", "B", "C"), List.of(cell("0", "+5", "-123456789")));
        assertEquals(ScoreHistoryMeasure.measure(shortTotal, LABELS, String::length, String::length, 1),
                ScoreHistoryMeasure.measure(longTotal, LABELS, String::length, String::length, 1));
    }
}
