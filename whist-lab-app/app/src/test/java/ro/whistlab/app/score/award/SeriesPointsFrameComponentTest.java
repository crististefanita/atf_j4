package ro.whistlab.app.score.award;

import static org.junit.Assert.*;
import org.junit.Test;

public final class SeriesPointsFrameComponentTest {
    @Test public void theRectangleUsesOnlyThePointsWidthAndCentresItsIndependentHeight() {
        var rectangle = SeriesPointsFrame.inside(90, 60, 30, 2);
        assertEquals(new SeriesPointsFrame.Rectangle(1, 16, 89, 44), rectangle);
        assertNotEquals(rectangle.right() - rectangle.left(), rectangle.bottom() - rectangle.top(), 0);
    }

    @Test public void everySeatAndFractionalDensityKeepsItsRectangleOutOfBidsAndNeighbours() {
        for (int players = 3; players <= 6; players++) for (int seat = 0; seat < players; seat++)
            for (float density : new float[]{.75f, 1, 1.5f, 2.25f, 2.625f, 3.5f}) {
                int unit = Math.max(1, Math.round(density)), text = (int) Math.ceil(42 * density);
                int frameHeight = SeriesPointsFrame.requiredHeight((int) Math.ceil(18 * density), unit);
                int bidWidth = (int) Math.ceil(20 * density), pointsWidth = SeriesPointsFrame.requiredWidth(text, unit) + 9;
                int pointsStart = seat * (bidWidth + pointsWidth) + bidWidth;
                var rectangle = SeriesPointsFrame.inside(pointsWidth, frameHeight + 5, frameHeight, unit);
                assertNotNull(rectangle);
                assertEquals(pointsStart, pointsStart + rectangle.left() - unit / 2f, .0001f);
                assertEquals(pointsStart + pointsWidth, pointsStart + rectangle.right() + unit / 2f, .0001f);
                assertTrue(rectangle.top() - unit / 2f >= 0);
                assertTrue(rectangle.bottom() + unit / 2f <= frameHeight + 5);
                assertTrue((pointsWidth - text) / 2f - unit >= 2 * unit);
            }
    }

    @Test public void longTotalsAndEnlargedFontsReserveBothDimensionsBeforeDrawing() {
        assertEquals(38, SeriesPointsFrame.requiredWidth(32, 1));
        assertEquals(48, SeriesPointsFrame.requiredHeight(36, 2));
        assertEquals(22, SeriesPointsFrame.requiredWidth(10, 2));
        assertNull(SeriesPointsFrame.inside(60, 37, 38, 1));
        assertNotNull(SeriesPointsFrame.inside(38, 38, 38, 1));
    }

    @Test public void emptyOrConstrainedCellsCannotDrawIntoAnotherColumn() {
        assertNull(SeriesPointsFrame.inside(0, 40, 20, 1));
        assertNull(SeriesPointsFrame.inside(40, 0, 20, 1));
    }

    @Test public void invalidMeasurementsAndOverflowAreRejectedExplicitly() {
        assertThrows(IllegalArgumentException.class, () -> SeriesPointsFrame.requiredWidth(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> SeriesPointsFrame.requiredHeight(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> SeriesPointsFrame.requiredWidth(10, -1));
        assertThrows(IllegalArgumentException.class, () -> SeriesPointsFrame.requiredWidth(Integer.MAX_VALUE, 1));
        assertThrows(IllegalArgumentException.class, () -> SeriesPointsFrame.inside(-1, 40, 20, 1));
        assertThrows(IllegalArgumentException.class, () -> SeriesPointsFrame.inside(40, 40, 1, 1));
    }

    @Test public void penaltyInkDependsOnTheSeriesAdjustmentInsteadOfTheRunningTotal() {
        for (int points : new int[]{-5, -10}) assertEquals(0xff97462e, SeriesPointsFrame.color(points));
        for (int points : new int[]{5, 10}) assertEquals(0xff29362e, SeriesPointsFrame.color(points));
        assertThrows(IllegalArgumentException.class, () -> SeriesPointsFrame.color(0));
    }
}
