package atf.android.evidence;

import atf.reporting.model.ReportScreenshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;

/** Converts a device PNG into the shared ATF screenshot evidence contract. */
public final class AndroidScreenshotEvidence {
    private AndroidScreenshotEvidence() { }

    public static ReportScreenshot from(Path png) throws IOException {
        Objects.requireNonNull(png, "png");
        return from(png.getFileName().toString(), Files.readAllBytes(png));
    }

    public static ReportScreenshot from(String title, byte[] pngBytes) {
        Objects.requireNonNull(pngBytes, "pngBytes");
        String base64 = Base64.getEncoder().encodeToString(pngBytes);
        if (!ReportScreenshot.hasPngSignature(base64)) {
            throw new IllegalArgumentException("Android screenshot payload does not have a PNG signature");
        }
        return new ReportScreenshot(title, base64);
    }
}
