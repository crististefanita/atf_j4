package ro.whistlab.app.score;

import static org.junit.Assert.*;
import org.junit.Test;
import ro.whistlab.app.score.layout.BidStrikeGeometry;

public final class ScoreHistoryGridMetricsComponentTest {
    @Test public void playerHeadersAndSubcolumnsAlignWithoutClippingAtFractionalDensities() {
        for (int players = 3; players <= 6; players++) {
            for (float density : new float[]{1f, 1.5f, 2f, 2.25f, 2.625f, 2.75f, 3.5f}) {
                var grid = ScoreHistoryGridMetrics.forDensity(players, density);
                int header = grid.handWidthPx(), cells = header;
                for (int seat = 0; seat < players; seat++) {
                    header += grid.separatorWidthPx() + grid.playerWidthPx();
                    cells += grid.separatorWidthPx() + grid.bidWidthPx() + grid.pointsWidthPx();
                    assertEquals("Player boundary at density " + density, header, cells);
                }
                assertEquals(grid.tableWidthPx(), cells + 2 * grid.outerPaddingPx());
                assertEquals(Math.round(2.1f * density), grid.separatorWidthPx());
            }
        }
    }

    @Test public void fractionalDensityKeepsTheExtraSubcolumnPixelRatherThanClippingIt() {
        var grid = ScoreHistoryGridMetrics.forDensity(4, 2.75f);
        assertEquals(116, grid.playerWidthPx());
        assertEquals(4 * (116 + 6) + 82 + 40, grid.tableWidthPx());
    }

    @Test public void preferredGridContainsOnlyBidAndPointsForEveryPlayer() {
        var grid = ScoreHistoryGridMetrics.forDensity(4, 1);
        assertEquals(30, grid.handWidthPx());
        assertEquals(19, grid.bidWidthPx());
        assertEquals(23, grid.pointsWidthPx());
        assertEquals(42, grid.playerWidthPx());
        assertEquals(220, grid.tableWidthPx());
        assertEquals(2, grid.separatorWidthPx());
    }

    @Test public void measuredDigitsLargeTextAndNamesExpandWithoutChangingHeaderBoundaries() {
        for (int players = 3; players <= 6; players++) {
            var base = ScoreHistoryGridMetrics.forDensity(players, 2.75f);
            for (int measure : new int[]{0, 40, 90, 180}) {
                var fit = base.fitContent(measure, measure, measure, 4 * measure);
                assertTrue(fit.handWidthPx() >= measure);
                assertTrue(fit.bidWidthPx() >= measure);
                assertTrue(fit.pointsWidthPx() >= measure);
                assertTrue(fit.playerWidthPx() >= 4 * measure);
                assertEquals(fit.bidWidthPx() + fit.pointsWidthPx(), fit.playerWidthPx());
                assertEquals(base.separatorWidthPx(), fit.separatorWidthPx());
                assertEquals(base, base.fitContent(0, 0, 0, 0));
            }
        }
        assertThrows(IllegalArgumentException.class, () -> ScoreHistoryGridMetrics.forDensity(4, 1).fitContent(-1, 0, 0, 0));
    }

    @Test public void diagonalStrikeCrossesZeroAndTwoDigitBidsWithoutEnteringTheirNeighbours() {
        for (float density : new float[]{1, 1.5f, 2.625f, 2.75f, 3.5f}) {
            for (float scale : new float[]{1, 1.15f, 1.3f, 1.5f}) {
                for (int digits : new int[]{1, 2}) {
                    float glyphWidth = digits * 7 * density * scale;
                    float width = Math.max(19 * density, glyphWidth + 4 * density);
                    float left = (width - glyphWidth) / 2;
                    var line = BidStrikeGeometry.across(left, 4, left + glyphWidth, 4 + 12 * density * scale, width, density);
                    assertTrue(line.left() <= left && line.right() >= left + glyphWidth);
                    assertTrue(line.left() >= density && line.right() <= width - density);
                    assertTrue(line.bottom() > line.top());
                    assertTrue(line.top() > 4 && line.bottom() < 4 + 12 * density * scale);
                }
            }
        }
        assertThrows(IllegalArgumentException.class, () -> BidStrikeGeometry.across(5, 0, 4, 8, 20, 1));
        assertThrows(IllegalArgumentException.class, () -> BidStrikeGeometry.across(0, 0, 4, 8, 2, 1));
        assertThrows(IllegalArgumentException.class, () -> BidStrikeGeometry.across(0, 0, Float.NaN, 8, 20, 1));
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

    @Test public void extremeDensityAndMeasurementsCannotProduceCollapsedOrOverflowedColumns() {
        assertThrows(IllegalArgumentException.class, () -> ScoreHistoryGridMetrics.forDensity(4, Float.MIN_VALUE));
        assertThrows(IllegalArgumentException.class, () -> ScoreHistoryGridMetrics.forDensity(4, Float.MAX_VALUE));
        var base = ScoreHistoryGridMetrics.forDensity(4, 1);
        assertThrows(IllegalArgumentException.class, () -> base.fitContent(0, Integer.MAX_VALUE, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ScoreHistoryGridMetrics(0, 1, 1, 1, 0, 4));
    }

    @Test public void spareWidthIsSharedByColumnsAndEveryHeaderKeepsItsBodyBoundary() {
        for (int seats = 3; seats <= 6; seats++) for (float density : new float[]{1, 1.5f, 2, 2.625f, 3.5f}) {
            for (float scale : new float[]{1, 1.3f, 1.5f}) {
                var minimum = ScoreHistoryGridMetrics.forDensity(seats, density)
                        .fitContent(Math.round(42 * density * scale), Math.round(18 * density * scale),
                                Math.round(38 * density * scale), Math.round(64 * density * scale));
                for (int width : new int[]{240, 320, 480, 644, 720, 1080, 1460, 2400}) {
                    var grid = minimum.fitViewport(width);
                    assertEquals(Math.max(width, minimum.tableWidthPx()), grid.tableWidthPx());
                    assertTrue(grid.handWidthPx() >= minimum.handWidthPx());
                    assertTrue(grid.bidWidthPx() >= minimum.bidWidthPx());
                    assertTrue(grid.pointsWidthPx() >= minimum.pointsWidthPx());
                    assertEquals(minimum.separatorWidthPx(), grid.separatorWidthPx());
                    assertEquals(minimum.outerPaddingPx(), grid.outerPaddingPx());
                    int header = grid.handWidthPx(), body = header;
                    for (int seat = 0; seat < seats; seat++) {
                        header += grid.separatorWidthPx() + grid.playerWidthPx();
                        body += grid.separatorWidthPx() + grid.bidWidthPx() + grid.pointsWidthPx();
                        assertEquals(header, body);
                    }
                    assertEquals(grid.tableWidthPx(), body + 2 * grid.outerPaddingPx());
                }
            }
        }
    }

    @Test public void extraWidthGrowsBothNumericColumnsAndResizingDoesNotMutateTheMinimum() {
        var minimum = ScoreHistoryGridMetrics.forDensity(4, 2).fitContent(60, 40, 60, 80);
        var first = minimum.fitViewport(644);
        var wide = minimum.fitViewport(1460);
        assertEquals(644, first.tableWidthPx());
        assertEquals(1460, wide.tableWidthPx());
        assertTrue(wide.bidWidthPx() > first.bidWidthPx());
        assertTrue(wide.pointsWidthPx() > first.pointsWidthPx());
        assertTrue(wide.handWidthPx() > first.handWidthPx());
        assertEquals(first, minimum.fitViewport(644));
        assertEquals(minimum, minimum.fitViewport(1));
        assertEquals(minimum, minimum.fitViewport(0));
    }

    @Test public void pixelRemaindersCannotCreateAnEmptyStripOrAnOverflow() {
        for (int seats = 3; seats <= 6; seats++) {
            var minimum = ScoreHistoryGridMetrics.forDensity(seats, 1);
            for (int width = minimum.tableWidthPx(); width < minimum.tableWidthPx() + 100; width++)
                assertEquals(width, minimum.fitViewport(width).tableWidthPx());
            assertEquals(Integer.MAX_VALUE, minimum.fitViewport(Integer.MAX_VALUE).tableWidthPx());
            assertThrows(IllegalArgumentException.class, () -> minimum.fitViewport(-1));
        }
    }
}
