package atf.android.e2e.display.component;

import static org.junit.jupiter.api.Assertions.*;

import atf.android.e2e.display.support.ResponsiveGeometry;
import atf.android.e2e.display.support.GameplayGeometry;
import atf.android.e2e.display.support.ScreenGeometry;
import atf.android.e2e.driver.AndroidUiDriver.Bounds;
import atf.testkit.AtfComponentTest;

import org.junit.jupiter.api.Test;

import java.util.List;

@AtfComponentTest
class ScreenGeometryComponentTest {
    @Test
    void strictAndPixelRoundingOverlapContractsRemainDifferent() {
        var first = new Bounds(0, 0, 10, 10);
        var onePixel = new Bounds(9, 0, 19, 10);
        assertTrue(ResponsiveGeometry.overlaps(first, onePixel));
        assertFalse(GameplayGeometry.overlaps(first, onePixel));
        assertTrue(ScreenGeometry.overlaps(first, new Bounds(8, 0, 18, 10), 1));
        assertFalse(ScreenGeometry.overlaps(first, new Bounds(10, 0, 20, 10), 0));
    }

    @Test
    void centeringNeedsOneContainedCardAndAllowsOnlyTheDeclaredOffset() {
        var viewport = new Bounds(0, 0, 100, 100);
        var centered = new Bounds(40, 30, 60, 90);
        assertTrue(GameplayGeometry.singleCardCentered(viewport, List.of(centered)));
        assertFalse(
                GameplayGeometry.singleCardCentered(viewport, List.of(new Bounds(43, 30, 63, 90))));
        assertFalse(
                GameplayGeometry.singleCardCentered(
                        viewport, List.of(new Bounds(40, 30, 60, 101))));
        assertFalse(GameplayGeometry.singleCardCentered(viewport, List.of(centered, centered)));
        assertEquals(-1, GameplayGeometry.singleCardCenterDelta(viewport, List.of()));
    }
}
