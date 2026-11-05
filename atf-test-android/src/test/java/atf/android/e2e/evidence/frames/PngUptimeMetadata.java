package atf.android.e2e.evidence.frames;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;

/** Păstrează pixelii la înscrierea timpului capturii în metadatele PNG. */
public final class PngUptimeMetadata {
    private PngUptimeMetadata() {}

    public static void reencodePngWithUptime(Path source, Path destination, double uptimeSeconds)
            throws IOException {
        BufferedImage original = ImageIO.read(source.toFile());
        if (original == null) throw new IOException("Cannot decode sampled Android PNG: " + source);
        var writers = ImageIO.getImageWritersByFormatName("png");
        if (!writers.hasNext())
            throw new IOException("No PNG writer is available for audited screenshot metadata");
        ImageWriter writer = writers.next();
        try (var output = ImageIO.createImageOutputStream(destination.toFile())) {
            writer.setOutput(output);
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            IIOMetadata metadata =
                    writer.getDefaultImageMetadata(
                            ImageTypeSpecifier.createFromRenderedImage(original), parameters);
            String format = metadata.getNativeMetadataFormatName();
            if (!"javax_imageio_png_1.0".equals(format)) {
                throw new IOException("Unexpected PNG metadata format: " + format);
            }
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);
            IIOMetadataNode text = childNode(root, "tEXt");
            if (text == null) {
                text = new IIOMetadataNode("tEXt");
                root.appendChild(text);
            }
            IIOMetadataNode captureTime = new IIOMetadataNode("tEXtEntry");
            captureTime.setAttribute("keyword", "atfCaptureUptimeMs");
            captureTime.setAttribute("value", Long.toString(Math.round(uptimeSeconds * 1_000)));
            text.appendChild(captureTime);
            metadata.setFromTree(format, root);
            writer.write(null, new IIOImage(original, null, metadata), parameters);
        } finally {
            writer.dispose();
        }
        BufferedImage decoded = ImageIO.read(destination.toFile());
        if (!sameImagePixels(original, decoded)) {
            throw new IOException(
                    "Lossless screenshot re-encoding changed rendered pixels: " + destination);
        }
    }

    public static boolean sameImagePixels(BufferedImage first, BufferedImage second) {
        if (second == null
                || first.getWidth() != second.getWidth()
                || first.getHeight() != second.getHeight()) {
            return false;
        }
        for (int y = 0; y < first.getHeight(); y++) {
            for (int x = 0; x < first.getWidth(); x++) {
                if (first.getRGB(x, y) != second.getRGB(x, y)) return false;
            }
        }
        return true;
    }

    public static IIOMetadataNode childNode(IIOMetadataNode parent, String name) {
        for (int index = 0; index < parent.getLength(); index++) {
            org.w3c.dom.Node child = parent.item(index);
            if (child instanceof IIOMetadataNode metadataNode
                    && metadataNode.getNodeName().equals(name)) {
                return metadataNode;
            }
        }
        return null;
    }
}
