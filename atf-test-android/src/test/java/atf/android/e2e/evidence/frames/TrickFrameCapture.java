package atf.android.e2e.evidence.frames;

import static atf.android.e2e.evidence.frames.TrickFrameAnalysis.TrickFrame;

import atf.android.AdbClient;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.evidence.frames.TrickFrameAnalysis.TrickFrame;
import atf.reporting.contract.AtfReportStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Colectează cadre Android și timpul lor de captură. */
public final class TrickFrameCapture {
    private TrickFrameCapture() {}

    public static FrameSampler startFrameSampler(AdbClient adb, AndroidUiDriver ui, Path artifacts)
            throws Exception {
        String remote = "/sdcard/atf-whist-trick-" + UUID.randomUUID().toString().replace("-", "");
        Path local =
                artifacts.resolve(
                        "device-trick-frames-" + remote.substring(remote.lastIndexOf('-') + 1));
        adb.run("shell", "mkdir", "-p", remote).requireSuccess();
        String loop =
                "i=0; while [ \"$i\" -lt 36 ]; do screencap -p "
                        + remote
                        + "/frame-$i.png; cat /proc/uptime > "
                        + remote
                        + "/frame-$i.time; "
                        + "i=$((i + 1)); sleep 0.06; done; touch "
                        + remote
                        + "/complete";
        long started = System.nanoTime();
        adb.run("shell", "nohup sh -c '" + loop + "' </dev/null >/dev/null 2>&1 &")
                .requireSuccess();
        ui.recordAction(
                "Start Android-side display sampling",
                remote,
                "Capture PNG frames without waiting for host-side ADB screenshots",
                "36-frame sampler launched on device",
                Duration.ofNanos(System.nanoTime() - started).toMillis(),
                AtfReportStatus.PASS);
        return new FrameSampler(remote, local);
    }

    public static List<TrickFrame> finishFrameSampler(AdbClient adb, FrameSampler sampler)
            throws Exception {
        try {
            long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
            while (!adb.run("shell", "test", "-f", sampler.remoteDirectory() + "/complete")
                    .succeeded()) {
                if (System.nanoTime() >= deadline) {
                    throw new IllegalStateException(
                            "Android-side screenshot sampler did not finish within 20 seconds");
                }
                Thread.sleep(250);
            }
            Files.createDirectories(sampler.localDirectory());
            adb.run("pull", sampler.remoteDirectory(), sampler.localDirectory().toString())
                    .requireSuccess();
            List<Path> images;
            try (var files = Files.walk(sampler.localDirectory())) {
                images =
                        files.filter(
                                        path ->
                                                path.getFileName()
                                                        .toString()
                                                        .matches("frame-\\d+\\.png"))
                                .sorted(Comparator.comparingInt(TrickFrameCapture::frameNumber))
                                .toList();
            }
            List<TrickFrame> frames = new ArrayList<>();
            for (Path image : images) {
                Path timeFile =
                        image.resolveSibling(
                                image.getFileName().toString().replace(".png", ".time"));
                String[] uptime = Files.readString(timeFile).trim().split("\\s+");
                frames.add(new TrickFrame(image, Double.parseDouble(uptime[0])));
            }
            return List.copyOf(frames);
        } finally {
            adb.run("shell", "rm", "-rf", sampler.remoteDirectory()).requireSuccess();
        }
    }

    public static int frameNumber(Path path) {
        String name = path.getFileName().toString();
        return Integer.parseInt(name.substring("frame-".length(), name.length() - ".png".length()));
    }

    public record FrameSampler(String remoteDirectory, Path localDirectory) {}
}
