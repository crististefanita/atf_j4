package atf.android.e2e.evidence.frames;

import static atf.android.e2e.display.support.GameplayGeometry.isIvoryCardSurface;
import static atf.android.e2e.display.support.ScreenGeometry.height;
import static atf.android.e2e.display.support.ScreenGeometry.width;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/** Verifică stabilitatea pixelilor și fereastra temporală a cadrelor capturate. */
public final class TrickFrameAnalysis {
    private TrickFrameAnalysis() {}

    public static TrickFramePair findRenderedPause(List<TrickFrame> frames) throws Exception {
        for (int firstIndex = 0; firstIndex < frames.size(); firstIndex++) {
            TrickFrame first = frames.get(firstIndex);
            if (!cardFacesAreRendered(first.image())) continue;
            for (int laterIndex = firstIndex + 1; laterIndex < frames.size(); laterIndex++) {
                TrickFrame later = frames.get(laterIndex);
                long elapsedMillis =
                        Math.round((later.uptimeSeconds() - first.uptimeSeconds()) * 1_000);
                if (elapsedMillis > 1_400) break;
                if (elapsedMillis >= 900
                        && cardFacesAreRendered(later.image())
                        && cardPixelsRemainEquivalent(first.image(), later.image())) {
                    return new TrickFramePair(first, later, elapsedMillis);
                }
            }
        }
        return null;
    }

    public static long countCardFaceFrames(List<TrickFrame> frames) throws Exception {
        long count = 0;
        for (TrickFrame frame : frames) if (cardFacesAreRendered(frame.image())) count++;
        return count;
    }

    public static boolean cardFacesAreRendered(Path imagePath) throws Exception {
        BufferedImage image = ImageIO.read(imagePath.toFile());
        if (image == null) return false;
        return cardFaceMetrics(image).stream()
                .allMatch(
                        metrics ->
                                metrics.ivoryFraction() >= 0.55
                                        && metrics.heartMarkFraction() >= 0.01);
    }

    public static boolean cardFacesHaveHeartMarks(Path imagePath) throws Exception {
        BufferedImage image = ImageIO.read(imagePath.toFile());
        return image != null
                && cardFaceMetrics(image).stream()
                        .allMatch(metrics -> metrics.heartMarkFraction() >= 0.01);
    }

    public static List<CardFaceMetrics> cardFaceMetrics(BufferedImage image) {
        int width = Math.max(1, (int) Math.round(image.getWidth() * 0.12));
        int height = Math.max(1, (int) Math.round(image.getHeight() * 0.085));
        List<CardFaceMetrics> result = new ArrayList<>();
        for (double centerY : List.of(0.35, 0.45)) {
            for (double centerX : List.of(0.436, 0.564)) {
                int centerXpx = (int) Math.round(image.getWidth() * centerX);
                int centerYpx = (int) Math.round(image.getHeight() * centerY);
                PixelRegion region =
                        new PixelRegion(
                                Math.max(0, centerXpx - width / 2),
                                Math.max(0, centerYpx - height / 2),
                                Math.min(image.getWidth(), centerXpx + (width + 1) / 2),
                                Math.min(image.getHeight(), centerYpx + (height + 1) / 2));
                long ivory = 0;
                long red = 0;
                long sampled = 0;
                for (int y = region.top(); y < region.bottom(); y += 3) {
                    for (int x = region.left(); x < region.right(); x += 3) {
                        int rgb = image.getRGB(x, y);
                        int r = (rgb >>> 16) & 0xFF;
                        int g = (rgb >>> 8) & 0xFF;
                        int b = rgb & 0xFF;
                        sampled++;
                        if (isIvoryCardSurface(rgb)) ivory++;
                        if (r >= 145 && g <= 115 && b <= 115 && r - g >= 45) red++;
                    }
                }
                result.add(
                        new CardFaceMetrics(
                                sampled == 0 ? 0 : ivory / (double) sampled,
                                sampled == 0 ? 0 : red / (double) sampled));
            }
        }
        return List.copyOf(result);
    }

    public static boolean cardPixelsRemainEquivalent(Path firstPath, Path laterPath)
            throws Exception {
        BufferedImage first = ImageIO.read(firstPath.toFile());
        BufferedImage later = ImageIO.read(laterPath.toFile());
        if (first == null
                || later == null
                || first.getWidth() != later.getWidth()
                || first.getHeight() != later.getHeight()) return false;
        for (PixelRegion region : cardFaceRegions(first)) {
            long matching = 0;
            long sampled = 0;
            for (int y = region.top(); y < region.bottom(); y += 3) {
                for (int x = region.left(); x < region.right(); x += 3) {
                    sampled++;
                    if (first.getRGB(x, y) == later.getRGB(x, y)) matching++;
                }
            }
            if (sampled == 0 || matching < Math.ceil(sampled * 0.98)) return false;
        }
        return true;
    }

    public static boolean sameGameViewportPixels(Path firstPath, Path laterPath) throws Exception {
        BufferedImage first = ImageIO.read(firstPath.toFile());
        BufferedImage later = ImageIO.read(laterPath.toFile());
        if (first == null
                || later == null
                || first.getWidth() != later.getWidth()
                || first.getHeight() != later.getHeight()) return false;
        int top = (int) Math.round(first.getHeight() * 0.05);
        int bottom = (int) Math.round(first.getHeight() * 0.95);
        for (int y = top; y < bottom; y++) {
            for (int x = 0; x < first.getWidth(); x++) {
                if (first.getRGB(x, y) != later.getRGB(x, y)) return false;
            }
        }
        return true;
    }

    public static List<PixelRegion> cardFaceRegions(BufferedImage image) {
        int width = Math.max(1, (int) Math.round(image.getWidth() * 0.12));
        int height = Math.max(1, (int) Math.round(image.getHeight() * 0.085));
        List<PixelRegion> result = new ArrayList<>();
        for (double centerY : List.of(0.35, 0.45)) {
            for (double centerX : List.of(0.436, 0.564)) {
                int x = (int) Math.round(image.getWidth() * centerX);
                int y = (int) Math.round(image.getHeight() * centerY);
                result.add(
                        new PixelRegion(
                                Math.max(0, x - width / 2),
                                Math.max(0, y - height / 2),
                                Math.min(image.getWidth(), x + (width + 1) / 2),
                                Math.min(image.getHeight(), y + (height + 1) / 2)));
            }
        }
        return List.copyOf(result);
    }

    public record TrickFrame(Path image, double uptimeSeconds) {}

    public record TrickFramePair(TrickFrame first, TrickFrame later, long elapsedMillis) {}

    public record CardFaceMetrics(double ivoryFraction, double heartMarkFraction) {}

    public record PixelRegion(int left, int top, int right, int bottom) {
        int width() {
            return right - left;
        }

        int height() {
            return bottom - top;
        }
    }
}
