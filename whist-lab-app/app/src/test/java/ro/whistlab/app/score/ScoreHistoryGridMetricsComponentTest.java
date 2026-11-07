package ro.whistlab.app.score;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import org.junit.Test;
import ro.whistlab.app.score.layout.BidStrikeGeometry;

public final class ScoreHistoryGridMetricsComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void playerHeadersAndSubcolumnsAlignWithoutClippingAtFractionalDensities() {
        for (int players = 3; players <= 6; players++) {
            for (float density : new float[]{1f, 1.5f, 2f, 2.25f, 2.625f, 2.75f, 3.5f}) {
                var grid = ScoreHistoryGridMetrics.forDensity(players, density);
                int header = grid.handWidthPx(), cells = header;
                for (int seat = 0; seat < players; seat++) {
                    header += grid.separatorWidthPx() + grid.playerWidthPx();
                    cells += grid.separatorWidthPx() + grid.bidWidthPx() + grid.pointsWidthPx();

                    atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "cells", cells, "density", density, "grid", grid, "header", header, "players", players, "seat", seat);
                    assertEquals("Player boundary at density " + density, header, cells);
                }
                assertEquals("cells + 2 * grid.outerPaddingPx() must equal grid.tableWidthPx()", grid.tableWidthPx(), cells + 2 * grid.outerPaddingPx());
                assertEquals("grid.separatorWidthPx() must equal Math.round(2.1F * density)", Math.round(2.1f * density), grid.separatorWidthPx());
            }
        }
    }

    @Test public void fractionalDensityKeepsTheExtraSubcolumnPixelRatherThanClippingIt() {
        var grid = ScoreHistoryGridMetrics.forDensity(4, 2.75f);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "grid", grid);
        assertEquals("grid.playerWidthPx() must equal 116", 116, grid.playerWidthPx());
        assertEquals("grid.tableWidthPx() must equal 4 * (116 + 6) + 82 + 40", 4 * (116 + 6) + 82 + 40, grid.tableWidthPx());
    }

    @Test public void preferredGridContainsOnlyBidAndPointsForEveryPlayer() {
        var grid = ScoreHistoryGridMetrics.forDensity(4, 1);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "grid", grid);
        assertEquals("grid.handWidthPx() must equal 30", 30, grid.handWidthPx());
        assertEquals("grid.bidWidthPx() must equal 19", 19, grid.bidWidthPx());
        assertEquals("grid.pointsWidthPx() must equal 23", 23, grid.pointsWidthPx());
        assertEquals("grid.playerWidthPx() must equal 42", 42, grid.playerWidthPx());
        assertEquals("grid.tableWidthPx() must equal 220", 220, grid.tableWidthPx());
        assertEquals("grid.separatorWidthPx() must equal 2", 2, grid.separatorWidthPx());
    }

    @Test public void measuredDigitsLargeTextAndNamesExpandWithoutChangingHeaderBoundaries() {
        for (int players = 3; players <= 6; players++) {
            var base = ScoreHistoryGridMetrics.forDensity(players, 2.75f);
            for (int measure : new int[]{0, 40, 90, 180}) {
                var fit = base.fitContent(measure, measure, measure, 4 * measure);

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "base", base, "fit", fit, "measure", measure, "players", players);
                assertTrue("fit.handWidthPx() >= measure must hold", fit.handWidthPx() >= measure);
                assertTrue("fit.bidWidthPx() >= measure must hold", fit.bidWidthPx() >= measure);
                assertTrue("fit.pointsWidthPx() >= measure must hold", fit.pointsWidthPx() >= measure);
                assertTrue("fit.playerWidthPx() >= 4 * measure must hold", fit.playerWidthPx() >= 4 * measure);
                assertEquals("fit.playerWidthPx() must equal fit.bidWidthPx() + fit.pointsWidthPx()", fit.bidWidthPx() + fit.pointsWidthPx(), fit.playerWidthPx());
                assertEquals("fit.separatorWidthPx() must equal base.separatorWidthPx()", base.separatorWidthPx(), fit.separatorWidthPx());
                assertEquals("base.fitContent(0, 0, 0, 0) must equal base", base, base.fitContent(0, 0, 0, 0));
            }
        }
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> ScoreHistoryGridMetrics.forDensity(4, 1).fitContent(-1, 0, 0, 0));
    }

    @Test public void diagonalStrikeCrossesZeroAndTwoDigitBidsWithoutEnteringTheirNeighbours() {
        for (float density : new float[]{1, 1.5f, 2.625f, 2.75f, 3.5f}) {
            for (float scale : new float[]{1, 1.15f, 1.3f, 1.5f}) {
                for (int digits : new int[]{1, 2}) {
                    float glyphWidth = digits * 7 * density * scale;
                    float width = Math.max(19 * density, glyphWidth + 4 * density);
                    float left = (width - glyphWidth) / 2;
                    var line = BidStrikeGeometry.across(left, 4, left + glyphWidth, 4 + 12 * density * scale, width, density);

                    atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "density", density, "digits", digits, "glyphWidth", glyphWidth, "left", left, "line", line, "scale", scale, "width", width);
                    assertTrue("line.left() <= left && line.right() >= left + glyphWidth must hold", line.left() <= left && line.right() >= left + glyphWidth);
                    assertTrue("line.left() >= density && line.right() <= width - density must hold", line.left() >= density && line.right() <= width - density);
                    assertTrue("line.bottom() > line.top() must hold", line.bottom() > line.top());
                    assertTrue("line.top() > 4 && line.bottom() < 4 + 12 * density * scale must hold", line.top() > 4 && line.bottom() < 4 + 12 * density * scale);
                }
            }
        }
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> BidStrikeGeometry.across(5, 0, 4, 8, 20, 1));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> BidStrikeGeometry.across(0, 0, 4, 8, 2, 1));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> BidStrikeGeometry.across(0, 0, Float.NaN, 8, 20, 1));
    }

    @Test public void visibleLabelsContainOnlyCardsWhileRepeatedHandsKeepDistinctIdentities() {
        var first = new ScoreHistoryTable.Row(1, 1, ScoreHistoryTable.Status.RECORDED, java.util.List.of());
        var second = new ScoreHistoryTable.Row(2, 1, ScoreHistoryTable.Status.CURRENT, java.util.List.of());

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "first", first, "second", second);
        assertEquals("first.cardLabel() must equal \"1\"", "1", first.cardLabel());
        assertEquals("second.cardLabel() must equal first.cardLabel()", first.cardLabel(), second.cardLabel());
        assertNotEquals("second.hand() must differ from first.hand()", first.hand(), second.hand());
        assertEquals("new ScoreHistoryTable.Row(12, 8, ScoreHistoryTable.Status.FUTURE, java.util.List.of()).cardLabel() must equal \"8\"", "8", new ScoreHistoryTable.Row(12, 8, ScoreHistoryTable.Status.FUTURE,
                java.util.List.of()).cardLabel());
    }

    @Test public void invalidInputsCannotInventAHistoryGeometry() {
        for (float density : new float[]{0, -1, Float.NaN, Float.POSITIVE_INFINITY}) {

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "density", density);
            assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> ScoreHistoryGridMetrics.forDensity(4, density));
        }
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> ScoreHistoryGridMetrics.forDensity(2, 1));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> ScoreHistoryGridMetrics.forDensity(7, 1));
    }

    @Test public void extremeDensityAndMeasurementsCannotProduceCollapsedOrOverflowedColumns() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 4, "declared constant 2", 1, "declared constant 3", 0);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> ScoreHistoryGridMetrics.forDensity(4, Float.MIN_VALUE));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> ScoreHistoryGridMetrics.forDensity(4, Float.MAX_VALUE));
        var base = ScoreHistoryGridMetrics.forDensity(4, 1);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> base.fitContent(0, Integer.MAX_VALUE, 0, 0));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> new ScoreHistoryGridMetrics(0, 1, 1, 1, 0, 4));
    }

    @Test public void spareWidthIsSharedByColumnsAndEveryHeaderKeepsItsBodyBoundary() {
        for (int seats = 3; seats <= 6; seats++) for (float density : new float[]{1, 1.5f, 2, 2.625f, 3.5f}) {
            for (float scale : new float[]{1, 1.3f, 1.5f}) {
                var minimum = ScoreHistoryGridMetrics.forDensity(seats, density)
                        .fitContent(Math.round(42 * density * scale), Math.round(18 * density * scale),
                                Math.round(38 * density * scale), Math.round(64 * density * scale));
                for (int width : new int[]{240, 320, 480, 644, 720, 1080, 1460, 2400}) {
                    var grid = minimum.fitViewport(width);

                    atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "density", density, "grid", grid, "minimum", minimum, "scale", scale, "seats", seats, "width", width);
                    assertEquals("grid.tableWidthPx() must equal Math.max(width, minimum.tableWidthPx())", Math.max(width, minimum.tableWidthPx()), grid.tableWidthPx());
                    assertTrue("grid.handWidthPx() >= minimum.handWidthPx() must hold", grid.handWidthPx() >= minimum.handWidthPx());
                    assertTrue("grid.bidWidthPx() >= minimum.bidWidthPx() must hold", grid.bidWidthPx() >= minimum.bidWidthPx());
                    assertTrue("grid.pointsWidthPx() >= minimum.pointsWidthPx() must hold", grid.pointsWidthPx() >= minimum.pointsWidthPx());
                    assertEquals("grid.separatorWidthPx() must equal minimum.separatorWidthPx()", minimum.separatorWidthPx(), grid.separatorWidthPx());
                    assertEquals("grid.outerPaddingPx() must equal minimum.outerPaddingPx()", minimum.outerPaddingPx(), grid.outerPaddingPx());
                    int header = grid.handWidthPx(), body = header;
                    for (int seat = 0; seat < seats; seat++) {
                        header += grid.separatorWidthPx() + grid.playerWidthPx();
                        body += grid.separatorWidthPx() + grid.bidWidthPx() + grid.pointsWidthPx();
                        assertEquals("body must equal header", header, body);
                    }
                    assertEquals("body + 2 * grid.outerPaddingPx() must equal grid.tableWidthPx()", grid.tableWidthPx(), body + 2 * grid.outerPaddingPx());
                }
            }
        }
    }

    @Test public void extraWidthGrowsBothNumericColumnsAndResizingDoesNotMutateTheMinimum() {
        var minimum = ScoreHistoryGridMetrics.forDensity(4, 2).fitContent(60, 40, 60, 80);
        var first = minimum.fitViewport(644);
        var wide = minimum.fitViewport(1460);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "first", first, "minimum", minimum, "wide", wide);
        assertEquals("first.tableWidthPx() must equal 644", 644, first.tableWidthPx());
        assertEquals("wide.tableWidthPx() must equal 1460", 1460, wide.tableWidthPx());
        assertTrue("wide.bidWidthPx() > first.bidWidthPx() must hold", wide.bidWidthPx() > first.bidWidthPx());
        assertTrue("wide.pointsWidthPx() > first.pointsWidthPx() must hold", wide.pointsWidthPx() > first.pointsWidthPx());
        assertTrue("wide.handWidthPx() > first.handWidthPx() must hold", wide.handWidthPx() > first.handWidthPx());
        assertEquals("minimum.fitViewport(644) must equal first", first, minimum.fitViewport(644));
        assertEquals("minimum.fitViewport(1) must equal minimum", minimum, minimum.fitViewport(1));
        assertEquals("minimum.fitViewport(0) must equal minimum", minimum, minimum.fitViewport(0));
    }

    @Test public void pixelRemaindersCannotCreateAnEmptyStripOrAnOverflow() {
        for (int seats = 3; seats <= 6; seats++) {
            var minimum = ScoreHistoryGridMetrics.forDensity(seats, 1);
            for (int width = minimum.tableWidthPx(); width < minimum.tableWidthPx() + 100; width++)
                {
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "minimum", minimum, "seats", seats, "width", width);
assertEquals("minimum.fitViewport(width).tableWidthPx() must equal width", width, minimum.fitViewport(width).tableWidthPx()); }
            assertEquals("minimum.fitViewport(Integer.MAX_VALUE).tableWidthPx() must equal Integer.MAX_VALUE", Integer.MAX_VALUE, minimum.fitViewport(Integer.MAX_VALUE).tableWidthPx());
            assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> minimum.fitViewport(-1));
        }
    }
}
