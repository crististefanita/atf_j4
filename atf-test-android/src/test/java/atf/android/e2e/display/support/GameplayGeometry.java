package atf.android.e2e.display.support;

import static atf.android.e2e.display.support.ScreenGeometry.centerX;
import static atf.android.e2e.display.support.ScreenGeometry.width;

import atf.android.e2e.driver.AndroidUiDriver;

import java.util.List;

/** Condițiile geometrice și de culoare ale scenariilor de prezentare. */
public final class GameplayGeometry {
    private GameplayGeometry() {}

    public static boolean isIvoryCardSurface(int rgb) {
        return ((rgb >>> 16) & 0xFF) >= 220 && ((rgb >>> 8) & 0xFF) >= 220 && (rgb & 0xFF) >= 210;
    }

    public static boolean overlaps(AndroidUiDriver.Bounds first, AndroidUiDriver.Bounds second) {
        return ScreenGeometry.overlaps(first, second, 1);
    }

    public static boolean singleCardCentered(
            AndroidUiDriver.Bounds viewport, List<AndroidUiDriver.Bounds> cardBounds) {
        return cardBounds.size() == 1
                && singleCardCenterDelta(viewport, cardBounds)
                        <= Math.max(1, Math.round(width(viewport) * 0.02))
                && cardBounds.getFirst().left() >= viewport.left()
                && cardBounds.getFirst().right() <= viewport.right()
                && cardBounds.getFirst().bottom() <= viewport.bottom();
    }

    public static int singleCardCenterDelta(
            AndroidUiDriver.Bounds viewport, List<AndroidUiDriver.Bounds> cardBounds) {
        return cardBounds.size() == 1
                ? Math.abs(centerX(viewport) - centerX(cardBounds.getFirst()))
                : -1;
    }
}
