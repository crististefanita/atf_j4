package atf.android.e2e.evidence.frames;

import static org.junit.jupiter.api.Assertions.*;

import atf.testkit.AtfComponentTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

/** Checks the calibrated observation oracle with independent positive and negative images. */
@AtfComponentTest
class TrickFrameAnalysisComponentTest {
    @Test
    void aPauseNeedsVisibleFacesStablePixelsAndTheAuditedTimeWindow(@TempDir Path directory)
            throws Exception {
        Path first = png(directory, "first.png", renderedCards(true));
        Path later = png(directory, "later.png", renderedCards(true));
        var origin = new TrickFrameAnalysis.TrickFrame(first, 10.0);
        assertNull(
                TrickFrameAnalysis.findRenderedPause(
                        List.of(origin, new TrickFrameAnalysis.TrickFrame(later, 10.899))));
        assertNotNull(
                TrickFrameAnalysis.findRenderedPause(
                        List.of(origin, new TrickFrameAnalysis.TrickFrame(later, 10.900))));
        assertNotNull(
                TrickFrameAnalysis.findRenderedPause(
                        List.of(origin, new TrickFrameAnalysis.TrickFrame(later, 11.400))));
        assertNull(
                TrickFrameAnalysis.findRenderedPause(
                        List.of(origin, new TrickFrameAnalysis.TrickFrame(later, 11.401))));
        assertEquals(
                2,
                TrickFrameAnalysis.countCardFaceFrames(
                        List.of(origin, new TrickFrameAnalysis.TrickFrame(later, 11.0))));
    }

    @Test
    void plainCardSurfacesCannotPretendThatTheirHeartMarksWereRendered(@TempDir Path directory)
            throws Exception {
        Path healthy = png(directory, "healthy.png", renderedCards(true));
        Path missing = png(directory, "without-marks.png", renderedCards(false));
        assertTrue(TrickFrameAnalysis.cardFacesAreRendered(healthy));
        assertTrue(TrickFrameAnalysis.cardFacesHaveHeartMarks(healthy));
        assertFalse(TrickFrameAnalysis.cardFacesAreRendered(missing));
        assertFalse(TrickFrameAnalysis.cardFacesHaveHeartMarks(missing));
        assertFalse(TrickFrameAnalysis.cardPixelsRemainEquivalent(healthy, missing));
        assertNull(
                TrickFrameAnalysis.findRenderedPause(
                        List.of(
                                new TrickFrameAnalysis.TrickFrame(missing, 0),
                                new TrickFrameAnalysis.TrickFrame(missing, 1))));
    }

    @Test
    void cardStabilityAndWholeViewportIdentityHaveDifferentScopes(@TempDir Path directory)
            throws Exception {
        BufferedImage original = renderedCards(true);
        Path first = png(directory, "first.png", original);
        original.setRGB(20, 20, Color.BLUE.getRGB());
        Path outside = png(directory, "outside-viewport.png", original);
        assertTrue(TrickFrameAnalysis.sameGameViewportPixels(first, outside));
        original.setRGB(20, 700, Color.BLUE.getRGB());
        Path board = png(directory, "changed-board.png", original);
        assertFalse(TrickFrameAnalysis.sameGameViewportPixels(first, board));
        assertTrue(TrickFrameAnalysis.cardPixelsRemainEquivalent(first, board));
    }

    private static BufferedImage renderedCards(boolean marks) {
        BufferedImage image = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB);
        var paint = image.createGraphics();
        try {
            paint.setColor(new Color(0x073F28));
            paint.fillRect(0, 0, 1000, 1000);
            for (int y : List.of(350, 450))
                for (int x : List.of(436, 564)) {
                    paint.setColor(new Color(0xFFF9ED));
                    paint.fillRect(x - 60, y - 42, 120, 85);
                    if (marks) {
                        paint.setColor(new Color(0xB00000));
                        paint.fillRect(x - 44, y - 27, 24, 20);
                    }
                }
        } finally {
            paint.dispose();
        }
        return image;
    }

    private static Path png(Path directory, String name, BufferedImage image) throws Exception {
        Path file = directory.resolve(name);
        assertTrue(ImageIO.write(image, "png", file.toFile()));
        return file;
    }
}
