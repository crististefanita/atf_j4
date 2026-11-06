package atf.android.e2e.evidence.frames;

import atf.testkit.assertions.AuditedAssertions;

import static org.junit.jupiter.api.Assertions.*;

import atf.testkit.AtfComponentTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOMetadataNode;

@AtfComponentTest
class PngUptimeMetadataComponentTest {
    @Test
    void uptimeMetadataKeepsEveryRenderedPixel(@TempDir Path directory) throws Exception {
        BufferedImage image = new BufferedImage(19, 23, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++)
            for (int x = 0; x < image.getWidth(); x++)
                image.setRGB(x, y, 0xFF000000 | ((x * 13) << 16) | ((y * 11) << 8) | (x + y));
        Path source = directory.resolve("source.png"), destination = directory.resolve("timed.png");
        AuditedAssertions.assertTrue(ImageIO.write(image, "png", source.toFile()));
        PngUptimeMetadata.reencodePngWithUptime(source, destination, 12.3456);
        AuditedAssertions.assertTrue(PngUptimeMetadata.sameImagePixels(image, ImageIO.read(destination.toFile())));
        try (var input = ImageIO.createImageInputStream(destination.toFile())) {
            var reader = ImageIO.getImageReaders(input).next();
            try {
                reader.setInput(input);
                var metadata = reader.getImageMetadata(0);
                var root =
                        (IIOMetadataNode)
                                metadata.getAsTree(metadata.getNativeMetadataFormatName());
                var entries = root.getElementsByTagName("tEXtEntry");
                String value = null;
                for (int i = 0; i < entries.getLength(); i++) {
                    var entry = (IIOMetadataNode) entries.item(i);
                    if (entry.getAttribute("keyword").equals("atfCaptureUptimeMs"))
                        value = entry.getAttribute("value");
                }
                AuditedAssertions.assertEquals("12346", value);
            } finally {
                reader.dispose();
            }
        }
    }

    @Test
    void anUndecodableSourceCannotProduceAnAuditedScreenshot(@TempDir Path directory)
            throws Exception {
        Path source = directory.resolve("not-an-image.png");
        Files.writeString(source, "not PNG data");
        AuditedAssertions.assertThrows(
                IOException.class,
                () ->
                        PngUptimeMetadata.reencodePngWithUptime(
                                source, directory.resolve("out.png"), 1));
    }

    @Test
    void imageIdentityRejectsMissingDifferentSizedAndChangedImages() {
        var image = new BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB);
        var same = new BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB);
        AuditedAssertions.assertTrue(PngUptimeMetadata.sameImagePixels(image, same));
        same.setRGB(1, 2, 0xFFFFFF);
        AuditedAssertions.assertFalse(PngUptimeMetadata.sameImagePixels(image, same));
        AuditedAssertions.assertFalse(
                PngUptimeMetadata.sameImagePixels(
                        image, new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB)));
        AuditedAssertions.assertFalse(PngUptimeMetadata.sameImagePixels(image, null));
    }
}
