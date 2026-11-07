package ro.whistlab.app.score.award;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import org.junit.Test;

public final class SeriesPointsFrameComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void theRectangleUsesOnlyThePointsWidthAndCentresItsIndependentHeight() {
        var rectangle = SeriesPointsFrame.inside(90, 60, 30, 2);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "rectangle", rectangle);
        assertEquals("rectangle must equal new SeriesPointsFrame.Rectangle(1, 16, 89, 44)", new SeriesPointsFrame.Rectangle(1, 16, 89, 44), rectangle);
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

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "bidWidth", bidWidth, "density", density, "frameHeight", frameHeight, "players", players, "pointsStart", pointsStart, "pointsWidth", pointsWidth, "rectangle", rectangle, "seat", seat, "text", text, "unit", unit);
                assertNotNull("rectangle is available", rectangle);
                assertEquals(pointsStart, pointsStart + rectangle.left() - unit / 2f, .0001f);
                assertEquals(pointsStart + pointsWidth, pointsStart + rectangle.right() + unit / 2f, .0001f);
                assertTrue("rectangle.top() - unit / 2.0F >= 0 must hold", rectangle.top() - unit / 2f >= 0);
                assertTrue("rectangle.bottom() + unit / 2.0F <= frameHeight + 5 must hold", rectangle.bottom() + unit / 2f <= frameHeight + 5);
                assertTrue("(pointsWidth - text) / 2.0F - unit >= 2 * unit must hold", (pointsWidth - text) / 2f - unit >= 2 * unit);
            }
    }

    @Test public void longTotalsAndEnlargedFontsReserveBothDimensionsBeforeDrawing() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 38, "declared constant 2", 32, "declared constant 3", 1, "declared constant 4", 48, "declared constant 5", 36, "declared constant 6", 2, "declared constant 7", 22, "declared constant 8", 10, "declared constant 9", 60, "declared constant 10", 37);
        assertEquals("SeriesPointsFrame.requiredWidth(32, 1) must equal 38", 38, SeriesPointsFrame.requiredWidth(32, 1));
        assertEquals("SeriesPointsFrame.requiredHeight(36, 2) must equal 48", 48, SeriesPointsFrame.requiredHeight(36, 2));
        assertEquals("SeriesPointsFrame.requiredWidth(10, 2) must equal 22", 22, SeriesPointsFrame.requiredWidth(10, 2));
        assertNull("SeriesPointsFrame.inside(60, 37, 38, 1) remains absent", SeriesPointsFrame.inside(60, 37, 38, 1));
        assertNotNull("SeriesPointsFrame.inside(38, 38, 38, 1) is available", SeriesPointsFrame.inside(38, 38, 38, 1));
    }

    @Test public void emptyOrConstrainedCellsCannotDrawIntoAnotherColumn() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 0, "declared constant 2", 40, "declared constant 3", 20, "declared constant 4", 1);
        assertNull("SeriesPointsFrame.inside(0, 40, 20, 1) remains absent", SeriesPointsFrame.inside(0, 40, 20, 1));
        assertNull("SeriesPointsFrame.inside(40, 0, 20, 1) remains absent", SeriesPointsFrame.inside(40, 0, 20, 1));
    }

    @Test public void invalidMeasurementsAndOverflowAreRejectedExplicitly() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", -1, "declared constant 2", 1, "declared constant 3", 10, "declared constant 4", 40, "declared constant 5", 20);
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> SeriesPointsFrame.requiredWidth(-1, 1));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> SeriesPointsFrame.requiredHeight(-1, 1));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> SeriesPointsFrame.requiredWidth(10, -1));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> SeriesPointsFrame.requiredWidth(Integer.MAX_VALUE, 1));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> SeriesPointsFrame.inside(-1, 40, 20, 1));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> SeriesPointsFrame.inside(40, 40, 1, 1));
    }

    @Test public void penaltyInkDependsOnTheSeriesAdjustmentInsteadOfTheRunningTotal() {
        for (int points : new int[]{-5, -10}) {
atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "points", points);
assertEquals("SeriesPointsFrame.color(points) must equal -6863314", 0xff97462e, SeriesPointsFrame.color(points)); }
        for (int points : new int[]{5, 10}) assertEquals("SeriesPointsFrame.color(points) must equal -14076370", 0xff29362e, SeriesPointsFrame.color(points));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> SeriesPointsFrame.color(0));
    }
}
