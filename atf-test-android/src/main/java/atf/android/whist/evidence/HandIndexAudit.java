package atf.android.whist.evidence;

import java.awt.image.BufferedImage;

/** Checks the exposed rank/suit band, independently of accessibility-node counts. */
public final class HandIndexAudit {
    public record Box(int left, int top, int right, int bottom) {
        public boolean contains(Box other) {
            return left <= other.left && top <= other.top && right >= other.right && bottom >= other.bottom;
        }
    }
    public record Result(boolean contained, int rankPixels, int suitPixels, int edgePixels) {
        public boolean readable() { return contained && rankPixels >= 4 && suitPixels >= 4 && edgePixels == 0; }
    }

    private HandIndexAudit() { }

    public static Result inspect(BufferedImage image, int displayWidth, int displayHeight, Box index, Box exposed) {
        if (image == null || displayWidth <= 0 || displayHeight <= 0 || index.right <= index.left || index.bottom <= index.top) {
            throw new IllegalArgumentException("A decoded image, positive viewport and nonempty index are required");
        }
        double scale = Math.min(image.getWidth() / (double) displayWidth, image.getHeight() / (double) displayHeight);
        double offsetX = (image.getWidth() - displayWidth * scale) / 2;
        double offsetY = (image.getHeight() - displayHeight * scale) / 2;
        int left = (int) Math.ceil(offsetX + index.left * scale);
        int right = (int) Math.floor(offsetX + index.right * scale);
        int top = (int) Math.ceil(offsetY + index.top * scale);
        int bottom = (int) Math.floor(offsetY + index.bottom * scale);
        boolean contained = exposed.contains(index) && new Box(0, 0, displayWidth, displayHeight).contains(index)
                && left >= 0 && top >= 0 && right <= image.getWidth() && bottom <= image.getHeight();
        if (!contained) return new Result(false, 0, 0, 0);
        int rank = 0, suit = 0, edge = 0;
        // The top padding occupies about 1/7 of this two-line label. Split the actual glyph area.
        int middle = top + (bottom - top) * 4 / 7;
        int padding = Math.max(1, (right - left) / 20);
        for (int y = top + padding; y < bottom; y++) {
            for (int x = left + padding; x < right; x++) {
                if (!ink(image.getRGB(x, y))) continue;
                if (y < middle) rank++; else suit++;
                if (x >= right - padding) edge++;
            }
        }
        return new Result(true, rank, suit, edge);
    }

    private static boolean ink(int rgb) {
        int red = (rgb >>> 16) & 255, green = (rgb >>> 8) & 255, blue = rgb & 255;
        return (red < 110 && green < 125 && blue < 115) || (red > green * 1.5 && green < 110 && blue < 120);
    }
}
