package ro.whistlab.app.score;

import static atf.gradle.audit.assertions.GradleAssertions.*;

import java.util.List;
import org.junit.Test;
import ro.whistlab.core.session.ScoreHistory;

public final class ScoreHistoryMeasureComponentTest extends atf.gradle.audit.GradleAuditedTest {
    private static final ScoreHistoryMeasure.Labels LABELS = new ScoreHistoryMeasure.Labels("Cards", "B", "P");

    @Test public void allDigitsSignsAndTheFullNameParticipateInWidthMeasurement() {
        var table = table(List.of("Al", "Dora", "Vlad"), List.of(
                cell("17", "-100", "1000"), cell("0", "+5", "9"), cell("—", "—", "—")));
        var size = ScoreHistoryMeasure.measure(table, LABELS, text -> text.length() * 10, text -> text.length() * 8, 2);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "size", size, "table", table);
        assertEquals("size.hand() must equal 44", 44, size.hand());
        assertEquals("size.bid() must equal 28", 28, size.bid());
        assertEquals("size.points() must equal 44", 44, size.points());
        assertEquals("size.name() must equal 44", 44, size.name());
    }

    @Test public void theLastSeatAndLastRowCannotBeSkippedByAnOptimization() {
        var small = table(List.of("A", "B", "C"), List.of(cell("0", "5", "5"), cell("0", "5", "5"), cell("0", "5", "5")));
        var last = new ScoreHistoryTable.Row(24, 1, ScoreHistoryTable.Status.RECORDED,
                List.of(cell("0", "5", "5"), cell("0", "5", "5"), cell("17", "-1000", "-123456")));
        var table = new ScoreHistoryTable(small.players(), List.of(small.rows().get(0), last));
        var size = ScoreHistoryMeasure.measure(table, LABELS, String::length, String::length, 1);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "last", last, "size", size, "small", small, "table", table);
        assertEquals("size.bid() must equal 6", 6, size.bid());
        assertEquals("size.points() must equal 9", 9, size.points());
    }

    @Test public void captionAndNumericFontsHaveIndependentMeasurements() {
        var table = new ScoreHistoryTable(List.of("A", "B", "C"), List.of());
        var size = ScoreHistoryMeasure.measure(table, LABELS, text -> 2, text -> 70, 1);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "size", size, "table", table);
        assertEquals("size.hand() must equal 72", 72, size.hand());
        assertEquals("size.bid() must equal 4", 4, size.bid());
        assertEquals("size.points() must equal 4", 4, size.points());
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

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "density", density, "grid", grid, "padding", padding, "previous", previous, "scale", scale, "size", size, "table", table);
                assertTrue("grid.bidWidthPx() >= size.bid() must hold", grid.bidWidthPx() >= size.bid());
                assertTrue("grid.pointsWidthPx() >= size.points() must hold", grid.pointsWidthPx() >= size.points());
                assertTrue("grid.handWidthPx() >= size.hand() must hold", grid.handWidthPx() >= size.hand());
                assertTrue("grid.playerWidthPx() >= size.name() must hold", grid.playerWidthPx() >= size.name());
                assertTrue("grid.tableWidthPx() >= previous must hold", grid.tableWidthPx() >= previous);
                previous = grid.tableWidthPx();
            }
        }
    }

    @Test public void aFontFailureKeepsItsOriginalCauseInsteadOfReturningZeroWidth() {
        var failure = new IllegalStateException("missing glyph metrics");
        var actual = assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class, () -> ScoreHistoryMeasure.measure(
                table(List.of("A", "B", "C"), List.of()), LABELS, text -> { throw failure; }, String::length, 1));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "actual", actual, "failure", failure);
        assertSame("actual keeps the identity of failure", failure, actual);
    }

    @Test public void invalidMetricsAndOverflowFailBeforeLayout() {
        var table = table(List.of("A", "B", "C"), List.of());

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "table", table);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> ScoreHistoryMeasure.measure(table, LABELS, text -> -1, String::length, 1));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> ScoreHistoryMeasure.measure(table, LABELS, String::length, text -> -1, 1));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> ScoreHistoryMeasure.measure(table, LABELS, String::length, String::length, -1));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> ScoreHistoryMeasure.measure(table, LABELS, text -> Integer.MAX_VALUE, String::length, 1));
    }

    private static ScoreHistoryTable table(List<String> players, List<ScoreHistoryTable.Cell> cells) {
        return new ScoreHistoryTable(players, cells.isEmpty() ? List.of() :
                List.of(new ScoreHistoryTable.Row(1, 8, ScoreHistoryTable.Status.RECORDED, cells)));
    }

    private static ScoreHistoryTable.Cell cell(String bid, String points, String total) {
        return new ScoreHistoryTable.Cell(bid, points, total, "fixture", ScoreHistory.Outcome.UNKNOWN, 0);
    }

    @Test public void cumulativeTotalsDetermineTheVisiblePointsWidth() {
        var shortTotal = table(List.of("A", "B", "C"), List.of(cell("0", "+5", "5")));
        var longTotal = table(List.of("A", "B", "C"), List.of(cell("0", "+5", "-123456789")));
        var shortSize = ScoreHistoryMeasure.measure(shortTotal, LABELS, String::length, String::length, 1);
        var longSize = ScoreHistoryMeasure.measure(longTotal, LABELS, String::length, String::length, 1);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "longSize", longSize, "longTotal", longTotal, "shortSize", shortSize, "shortTotal", shortTotal);
        assertEquals("shortSize.points() must equal 3", 3, shortSize.points());
        assertEquals("longSize.points() must equal 12", 12, longSize.points());
        assertEquals("longSize.bid() must equal shortSize.bid()", shortSize.bid(), longSize.bid());
        assertEquals("longSize.hand() must equal shortSize.hand()", shortSize.hand(), longSize.hand());
        assertEquals("longSize.name() must equal shortSize.name()", shortSize.name(), longSize.name());
    }

    @Test public void aHiddenHandDeltaCannotWidenTheCumulativePointsColumn() {
        var shortDelta = table(List.of("A", "B", "C"), List.of(cell("0", "+5", "10")));
        var longDelta = table(List.of("A", "B", "C"), List.of(cell("0", "-123456789", "10")));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "longDelta", longDelta, "shortDelta", shortDelta);
        assertEquals("ScoreHistoryMeasure.measure(longDelta, LABELS, String::length, String::length, 1) must equal ScoreHistoryMeasure.measure(shortDelta, LABELS, String::length, String::length, 1)", ScoreHistoryMeasure.measure(shortDelta, LABELS, String::length, String::length, 1),
                ScoreHistoryMeasure.measure(longDelta, LABELS, String::length, String::length, 1));
    }

    @Test public void seriesRectanglesReserveSpaceOnlyInPointsForBonusAndPenaltyAtEverySeatCount() {
        for (int seats = 3; seats <= 6; seats++) for (int award : new int[]{-10, -5, 5, 10}) {
            var ordinary = cell("0", "+5", "-1000");
            var marked = new ScoreHistoryTable.Cell("0", "+5", "-1000", "fixture", ScoreHistory.Outcome.EXACT, award);
            var plain = table(java.util.Collections.nCopies(seats, "A"), java.util.Collections.nCopies(seats, ordinary));
            var boxed = table(plain.players(), java.util.Collections.nCopies(seats, marked));
            var baseline = ScoreHistoryMeasure.measure(plain, LABELS, String::length, String::length, 2);
            var size = ScoreHistoryMeasure.measure(boxed, LABELS, String::length, String::length, 2);

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "award", award, "baseline", baseline, "boxed", boxed, "marked", marked, "ordinary", ordinary, "plain", plain, "seats", seats, "size", size);
            assertEquals("size.points() must equal 17", 17, size.points());
            assertEquals("baseline.points() must equal 9", 9, baseline.points());
            assertEquals("size.bid() must equal baseline.bid()", baseline.bid(), size.bid());
            assertEquals("size.hand() must equal baseline.hand()", baseline.hand(), size.hand());
            assertEquals("size.name() must equal baseline.name()", baseline.name(), size.name());
        }
    }

    @Test public void seriesRectangleWidthIncludesTheLongestTotal() {
        var marked = new ScoreHistoryTable.Cell("0", "+5", "-123456789", "fixture", ScoreHistory.Outcome.EXACT, 10);
        var table = table(List.of("A", "B", "C"), List.of(marked));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "marked", marked, "table", table);
        assertEquals("ScoreHistoryMeasure.measure(table, LABELS, String::length, String::length, 1).points() must equal 16", 16, ScoreHistoryMeasure.measure(table, LABELS, String::length, String::length, 1).points());
    }
}
