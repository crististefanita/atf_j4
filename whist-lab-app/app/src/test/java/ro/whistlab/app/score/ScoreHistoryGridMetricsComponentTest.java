package ro.whistlab.app.score;

import static org.junit.Assert.*;
import org.junit.Test;

public final class ScoreHistoryGridMetricsComponentTest {
    @Test public void playerHeadersAndSubcolumnsAlignWithoutClippingAtFractionalDensities() {
        for (int players = 3; players <= 6; players++) {
            for (float density : new float[]{1f, 1.5f, 2f, 2.25f, 2.625f, 2.75f, 3.5f}) {
                var grid = ScoreHistoryGridMetrics.forDensity(players, density);
                int header = grid.handWidthPx(), cells = header;
                for (int seat = 0; seat < players; seat++) {
                    header += grid.separatorWidthPx() + grid.playerWidthPx();
                    cells += grid.separatorWidthPx() + grid.bidWidthPx()
                            + grid.pointsWidthPx() + grid.totalWidthPx();
                    assertEquals("Player boundary at density " + density, header, cells);
                }
                assertEquals(grid.tableWidthPx(), cells + 2 * grid.outerPaddingPx());
                assertEquals(Math.round(3 * density), grid.separatorWidthPx());
            }
        }
    }

    @Test public void fractionalDensityKeepsTheExtraSubcolumnPixelRatherThanClippingIt() {
        var grid = ScoreHistoryGridMetrics.forDensity(4, 2.75f);
        assertEquals(397, grid.playerWidthPx());
        assertEquals(4 * (397 + 8) + 182 + 44, grid.tableWidthPx());
    }

    @Test public void visibleLabelsContainOnlyCardsWhileRepeatedHandsKeepDistinctIdentities() {
        var first = new ScoreHistoryTable.Row(1, 1, ScoreHistoryTable.Status.RECORDED, java.util.List.of());
        var second = new ScoreHistoryTable.Row(2, 1, ScoreHistoryTable.Status.CURRENT, java.util.List.of());
        assertEquals("1", first.cardLabel());
        assertEquals(first.cardLabel(), second.cardLabel());
        assertNotEquals(first.hand(), second.hand());
        assertEquals("8", new ScoreHistoryTable.Row(12, 8, ScoreHistoryTable.Status.FUTURE,
                java.util.List.of()).cardLabel());
    }

    @Test public void invalidInputsCannotInventAHistoryGeometry() {
        for (float density : new float[]{0, -1, Float.NaN, Float.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> ScoreHistoryGridMetrics.forDensity(4, density));
        }
        assertThrows(IllegalArgumentException.class, () -> ScoreHistoryGridMetrics.forDensity(2, 1));
        assertThrows(IllegalArgumentException.class, () -> ScoreHistoryGridMetrics.forDensity(7, 1));
    }
}
