package atf.gradle.audit.verification;

import atf.reporting.model.ReportScreenshot;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** The captured raster is the oracle; theme assets and another scenario cannot substitute its bytes. */
public final class CapturedPngVerifier {
    private CapturedPngVerifier() { }
    public static List<Map<String, Object>> inventory(List<ReportScreenshot> images) {
        return images.stream().map(image -> {
            byte[] bytes = Base64.getDecoder().decode(image.base64Png());
            var pixels = pixels(bytes);
            return Map.<String, Object>of("title", image.title(), "sha256", hash(bytes), "bytes", bytes.length,
                    "width", pixels.getWidth(), "height", pixels.getHeight());
        }).toList();
    }
    public static void verify(Document document, Element scenario, JsonNode expected) {
        if (!expected.isArray()) throw new IllegalStateException("Captured PNG inventory missing");
        var links = scenario.select("a.atf-ui-action-screenshot-link, a.atf-ui-evidence-screenshot-link");
        if (links.size() != expected.size()) throw new IllegalStateException("Owned PNG link inventory differs");
        for (int i = 0; i < links.size(); i++) {
            String target = links.get(i).attr("href");
            if (target.startsWith("#")) {
                String id = target.substring(1);
                var matches = document.getAllElements().stream().filter(e -> e.id().equals(id)).toList();
                if (matches.size() != 1) throw new IllegalStateException("Canonical inline PNG missing or duplicated");
                target = matches.get(0).attr("href");
            }
            String prefix = "data:image/png;base64,";
            if (!target.startsWith(prefix)) throw new IllegalStateException("Owned raster is not an inline PNG");
            byte[] bytes = Base64.getDecoder().decode(target.substring(prefix.length()));
            var pixels = pixels(bytes); var declared = expected.get(i);
            if (bytes.length != declared.required("bytes").asInt() || !hash(bytes).equals(declared.required("sha256").asText())
                    || pixels.getWidth() != declared.required("width").asInt() || pixels.getHeight() != declared.required("height").asInt())
                throw new IllegalStateException("Published PNG differs from captured raster: " + declared.required("title").asText());
        }
    }
    private static java.awt.image.BufferedImage pixels(byte[] bytes) {
        try {
            var image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            if (image == null) throw new IllegalStateException("PNG cannot be decoded");
            return image;
        } catch (java.io.IOException invalid) { throw new java.io.UncheckedIOException(invalid); }
    }
    private static String hash(byte[] bytes) {
        try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException unavailable) { throw new IllegalStateException(unavailable); }
    }
}
