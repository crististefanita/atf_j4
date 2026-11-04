package atf.android.e2e.driver;

import static atf.android.e2e.driver.UiHierarchy.bounds;
import static atf.android.e2e.driver.UiHierarchy.nodes;

import atf.android.e2e.driver.AndroidUiDriver.Bounds;
import atf.android.e2e.driver.UiHierarchy.Node;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.*;

import javax.imageio.ImageIO;

/** Pure screenshot measurements and logical-to-physical coordinate mapping. */
final class ScreenshotAnalysis {
    private ScreenshotAnalysis() {}

    private static final Pattern OVERRIDE_SIZE =
            Pattern.compile("(?m)^Override size:\\s*(\\d+)x(\\d+)");

    private static final Pattern PHYSICAL_SIZE =
            Pattern.compile("(?m)^Physical size:\\s*(\\d+)x(\\d+)");

    static double buttonBottomGapFraction(String xml, Path screenshot, String text)
            throws Exception {
        List<Node> matches =
                nodes(xml).stream()
                        .filter(node -> node.className().equals("android.widget.Button"))
                        .filter(node -> node.text().equals(text))
                        .toList();
        if (matches.size() != 1) {
            throw new IllegalStateException(
                    "Expected one button labeled '" + text + "', got " + matches.size());
        }
        var image = ImageIO.read(screenshot.toFile());
        if (image == null)
            throw new IllegalStateException("Cannot decode screenshot: " + screenshot);
        int bottom = bounds(matches.getFirst().bounds())[3];
        return (image.getHeight() - bottom) / (double) image.getHeight();
    }

    static boolean sameBackgroundAcrossTableMargin(
            Path screenshot, double firstY, double secondY, String displaySize) throws Exception {
        var image = ImageIO.read(screenshot.toFile());
        if (image == null)
            throw new IllegalStateException("Cannot decode screenshot: " + screenshot);
        int[] viewport = displayDimensions(displaySize);
        // One percent stays beyond the oval stroke on both the narrow and wide viewport.
        int[] firstPoint =
                screenshotPoint(
                        image, viewport[0] * 0.01, viewport[1] * firstY, viewport[0], viewport[1]);
        int[] secondPoint =
                screenshotPoint(
                        image, viewport[0] * 0.01, viewport[1] * secondY, viewport[0], viewport[1]);
        int first = image.getRGB(firstPoint[0], firstPoint[1]);
        int second = image.getRGB(secondPoint[0], secondPoint[1]);
        return first == second;
    }

    static int sampleHandFace(Path screenshot, Bounds hand, String displaySize) throws IOException {
        var image = ImageIO.read(screenshot.toFile());
        if (image == null) throw new IOException("Cannot decode hand screenshot: " + screenshot);
        int[] viewport = displayDimensions(displaySize);
        double logicalX = (hand.left() + hand.right()) / 2.0;
        double logicalY = hand.bottom() - Math.max(8, (hand.bottom() - hand.top()) / 20);
        int[] point = screenshotPoint(image, logicalX, logicalY, viewport[0], viewport[1]);
        return image.getRGB(point[0], point[1]) & 0xFFFFFF;
    }

    static int[] displayDimensions(String displaySize) throws IOException {
        Matcher size = OVERRIDE_SIZE.matcher(displaySize);
        if (!size.find()) {
            size = PHYSICAL_SIZE.matcher(displaySize);
            if (!size.find())
                throw new IOException("Android display size is unavailable: " + displaySize);
        }
        int width = Integer.parseInt(size.group(1));
        int height = Integer.parseInt(size.group(2));
        if (width <= 0 || height <= 0)
            throw new IOException("Android reported invalid display size: " + displaySize);
        return new int[] {width, height};
    }

    static int[] screenshotPoint(
            java.awt.image.BufferedImage image,
            double logicalX,
            double logicalY,
            int viewportWidth,
            int viewportHeight) {
        double scale =
                Math.min(
                        image.getWidth() / (double) viewportWidth,
                        image.getHeight() / (double) viewportHeight);
        double offsetX = (image.getWidth() - viewportWidth * scale) / 2.0;
        double offsetY = (image.getHeight() - viewportHeight * scale) / 2.0;
        int x =
                Math.max(
                        0,
                        Math.min(
                                image.getWidth() - 1,
                                (int) Math.round(offsetX + logicalX * scale)));
        int y =
                Math.max(
                        0,
                        Math.min(
                                image.getHeight() - 1,
                                (int) Math.round(offsetY + logicalY * scale)));
        return new int[] {x, y};
    }

    static long countBrightNeutralPixels(Path image, double minVerticalFraction) throws Exception {
        var bitmap = ImageIO.read(image.toFile());
        if (bitmap == null) throw new IllegalStateException("Cannot decode screenshot: " + image);
        int firstRow = (int) (bitmap.getHeight() * minVerticalFraction);
        long count = 0;
        for (int y = firstRow; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                int rgb = bitmap.getRGB(x, y);
                int red = (rgb >>> 16) & 0xff;
                int green = (rgb >>> 8) & 0xff;
                int blue = rgb & 0xff;
                if (red >= 205
                        && green >= 205
                        && blue >= 205
                        && Math.max(red, Math.max(green, blue))
                                        - Math.min(red, Math.min(green, blue))
                                <= 28) count++;
            }
        }
        return count;
    }

    static long countChangedPixels(
            java.awt.image.BufferedImage left,
            java.awt.image.BufferedImage right,
            double firstRowFraction,
            double lastRowFraction) {
        if (left.getWidth() != right.getWidth() || left.getHeight() != right.getHeight())
            throw new IllegalStateException(
                    "Cannot compare gameplay screenshots with different dimensions");
        int width = left.getWidth(), height = left.getHeight();
        int firstRow = (int) (height * firstRowFraction);
        int lastRow = (int) (height * lastRowFraction);
        long count = 0;
        for (int y = firstRow; y < lastRow; y += 2) {
            for (int x = 0; x < width; x += 2) {
                int a = left.getRGB(x, y), b = right.getRGB(x, y);
                int difference =
                        Math.abs(((a >>> 16) & 0xff) - ((b >>> 16) & 0xff))
                                + Math.abs(((a >>> 8) & 0xff) - ((b >>> 8) & 0xff))
                                + Math.abs((a & 0xff) - (b & 0xff));
                if (difference > 48) count++;
            }
        }
        return count;
    }
}
