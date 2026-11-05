package atf.android.e2e.display.support;

import atf.android.e2e.driver.AndroidUiDriver;

/** Operații geometrice comune, cu toleranță explicită pentru suprapunere. */
public final class ScreenGeometry {
    private ScreenGeometry() {}

    public static int width(AndroidUiDriver.Bounds bounds) {
        return bounds.right() - bounds.left();
    }

    public static int height(AndroidUiDriver.Bounds bounds) {
        return bounds.bottom() - bounds.top();
    }

    public static int centerX(AndroidUiDriver.Bounds bounds) {
        return (bounds.left() + bounds.right()) / 2;
    }

    public static int centerY(AndroidUiDriver.Bounds bounds) {
        return (bounds.top() + bounds.bottom()) / 2;
    }

    public static boolean overlaps(
            AndroidUiDriver.Bounds first, AndroidUiDriver.Bounds second, int tolerance) {
        return first.left() < second.right() - tolerance
                && first.right() > second.left() + tolerance
                && first.top() < second.bottom() - tolerance
                && first.bottom() > second.top() + tolerance;
    }
}
