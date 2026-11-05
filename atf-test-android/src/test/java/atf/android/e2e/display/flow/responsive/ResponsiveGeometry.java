package atf.android.e2e.display.flow.responsive;

import atf.android.e2e.display.profile.ResponsiveDisplayProfile;
import atf.android.e2e.display.support.ScreenGeometry;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.whist.evidence.HandIndexAudit;

import java.awt.image.BufferedImage;
import java.util.List;

/** Măsurători de geometrie și contrast pentru verificarea responsive. */
public final class ResponsiveGeometry {
    private ResponsiveGeometry() {}

    public static boolean noOverlap(List<AndroidUiDriver.Bounds> regions) {
        for (int first = 0; first < regions.size(); first++) {
            for (int second = first + 1; second < regions.size(); second++) {
                if (overlaps(regions.get(first), regions.get(second))) return false;
            }
        }
        return true;
    }

    public static boolean contains(AndroidUiDriver.Bounds parent, AndroidUiDriver.Bounds child) {
        return child.left() >= parent.left()
                && child.top() >= parent.top()
                && child.right() <= parent.right()
                && child.bottom() <= parent.bottom();
    }

    public static int sampleFrameFill(
            BufferedImage image, AndroidUiDriver.Bounds bounds, ResponsiveDisplayProfile profile) {
        double density = profile.density() / 160.0;
        int[] point =
                AndroidUiDriver.screenshotPoint(
                        image,
                        bounds.left() + 8 * density,
                        bounds.top() + (bounds.bottom() - bounds.top()) / 2.0,
                        profile.width(),
                        profile.height());
        return image.getRGB(point[0], point[1]) & 0x00FFFFFF;
    }

    public static int darkTextPixels(
            BufferedImage image, AndroidUiDriver.Bounds bounds, ResponsiveDisplayProfile profile) {
        int[] topLeft =
                AndroidUiDriver.screenshotPoint(
                        image, bounds.left(), bounds.top(), profile.width(), profile.height());
        int[] bottomRight =
                AndroidUiDriver.screenshotPoint(
                        image, bounds.right(), bounds.bottom(), profile.width(), profile.height());
        int left = Math.max(0, Math.min(topLeft[0], bottomRight[0]));
        int top = Math.max(0, Math.min(topLeft[1], bottomRight[1]));
        int right = Math.min(image.getWidth() - 1, Math.max(topLeft[0], bottomRight[0]));
        int bottom = Math.min(image.getHeight() - 1, Math.max(topLeft[1], bottomRight[1]));
        int dark = 0;
        for (int y = top; y <= bottom; y++) {
            for (int x = left; x <= right; x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >>> 16) & 0xFF;
                int green = (rgb >>> 8) & 0xFF;
                int blue = rgb & 0xFF;
                if (red < 110 && green < 125 && blue < 115) dark++;
            }
        }
        return dark;
    }

    public static boolean overlaps(AndroidUiDriver.Bounds left, AndroidUiDriver.Bounds right) {
        return ScreenGeometry.overlaps(left, right, 0);
    }

    public static HandIndexAudit.Box box(AndroidUiDriver.Bounds bounds) {
        return new HandIndexAudit.Box(bounds.left(), bounds.top(), bounds.right(), bounds.bottom());
    }
}
