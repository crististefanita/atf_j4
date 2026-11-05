package atf.android.e2e.driver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/** Owns fresh hierarchy transfers and complete PNG retries, within the caller's capture scope. */
final class UiDeviceCapture {
    private static final String REMOTE_XML = "/sdcard/atf-j4-window.xml";
    private final AndroidUiDriver.Device adb;
    private final Path output;

    UiDeviceCapture(AndroidUiDriver.Device adb, Path output) {
        this.adb = adb;
        this.output = output;
    }

    String dump() throws Exception {
        Path local = output.resolve("current-window.xml");
        IOException lastFailure = null;
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                // uiautomator can exit 0 after "null root node" without rewriting the file.
                // Never pull a hierarchy left behind by a previous screen.
                adb.run("shell", "rm", "-f", REMOTE_XML).requireSuccess();
                var dump = adb.run("shell", "uiautomator", "dump", REMOTE_XML);
                if (dump.succeeded() && dump.output().contains("dumped to:")) {
                    var pull = adb.run("pull", REMOTE_XML, local.toString());
                    if (pull.succeeded() && Files.isRegularFile(local)) {
                        String xml = Files.readString(local);
                        if (xml.contains("<hierarchy")) return xml;
                    }
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw interrupted;
            } catch (IOException transferFailure) {
                lastFailure = transferFailure;
            }
            Thread.sleep(350L * (attempt + 1));
        }
        throw new IllegalStateException(
                "UI Automator did not produce a hierarchy: " + local, lastFailure);
    }

    List<Path> capture(String label, Supplier<? extends AutoCloseable> captureScopes) throws Exception {
        try (var capture = captureScopes.get()) {
            String xml = dump();
            Path xmlFile = output.resolve(label + ".xml");
            Path imageFile = output.resolve(label + ".png");
            Files.writeString(xmlFile, xml);
            capturePng(imageFile);
            return List.of(xmlFile, imageFile);
        }
    }

    Path captureImage(String label, Supplier<? extends AutoCloseable> captureScopes) throws Exception {
        try (var capture = captureScopes.get()) {
            Path image = output.resolve(label + ".png");
            capturePng(image);
            return image;
        }
    }

    private void capturePng(Path imageFile) throws Exception {
        IOException lastFailure = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                Files.deleteIfExists(imageFile);
                var capture = adb.runBinary("exec-out", "screencap", "-p");
                capture.requireSuccess();
                byte[] png = capture.output();
                if (png.length >= 10_000) {
                    Files.write(imageFile, png);
                    return;
                }
                lastFailure =
                        new IOException(
                                "Screenshot looks incomplete after ADB exec-out: " + imageFile);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw interrupted;
            } catch (IOException transferFailure) {
                lastFailure = transferFailure;
            }
            if (attempt < 2) Thread.sleep(250L * (attempt + 1));
        }
        throw lastFailure == null
                ? new IOException("Screenshot capture failed: " + imageFile)
                : lastFailure;
    }

}
