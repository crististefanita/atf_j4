package atf.android.e2e.driver;

import atf.testkit.AtfComponentTest;

import static org.junit.jupiter.api.Assertions.*;
import atf.android.AdbClient;
import atf.android.evidence.timing.AndroidPhaseClock;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@AtfComponentTest
class AndroidUiDriverCaptureTimingTest {
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void fullPngAndOptionalHierarchyAreMeasuredAsCaptureRatherThanBusiness(boolean hierarchy, @TempDir Path output) throws Exception {
        byte[] png = wholeFixture(); var ticks = new AtomicLong();
        var clock = new AndroidPhaseClock("capture", ticks::get); clock.enter("TEST BODY");
        var driver = new AndroidUiDriver(device(ticks, png, false), output); driver.recordCapturesAs(clock::capture);
        ticks.addAndGet(100_000_000L);
        Path image = hierarchy ? driver.capture("whole").getLast() : driver.captureImage("whole");
        ticks.addAndGet(40_000_000L); clock.stop(false);
        var timing = clock.snapshot("TEST BODY").orElseThrow();
        assertEquals(140_000_000L, timing.executionNanos());
        assertEquals(hierarchy ? 45_000_000L : 30_000_000L, timing.captureNanos());
        assertArrayEquals(png, Files.readAllBytes(image));
        var decoded = ImageIO.read(image.toFile()); assertEquals(200, decoded.getWidth()); assertEquals(400, decoded.getHeight());
    }

    @Test void failedCaptureClosesItsMeasuredScopeAndRetainsTheOriginalFailure(@TempDir Path output) throws Exception {
        var ticks = new AtomicLong(); var clock = new AndroidPhaseClock("capture", ticks::get); clock.enter("TEST BODY");
        var driver = new AndroidUiDriver(device(ticks, new byte[0], true), output); driver.recordCapturesAs(clock::capture);
        var failure = assertThrows(UnsupportedOperationException.class, () -> driver.captureImage("failed"));
        clock.stop(true);
        assertEquals("Controlled failed capture", failure.getMessage());
        assertEquals(30_000_000L, clock.snapshot("TEST BODY").orElseThrow().captureNanos());
        assertTrue(clock.snapshot("TEST BODY").orElseThrow().partial());
    }

    private static AndroidUiDriver.Device device(AtomicLong ticks, byte[] png, boolean fail) {
        return new AndroidUiDriver.Device() {
            public AdbClient.Result run(String... args) throws java.io.IOException {
                ticks.addAndGet(5_000_000L);
                if (args[0].equals("pull")) Files.writeString(Path.of(args[2]), "<hierarchy><node text=\"Controlled fixture\"/></hierarchy>");
                return new AdbClient.Result(0, "UI hierarchy dumped to: fixture", List.of(args));
            }
            public AdbClient.BinaryResult runBinary(String... args) {
                ticks.addAndGet(30_000_000L);
                if (fail) throw new UnsupportedOperationException("Controlled failed capture");
                return new AdbClient.BinaryResult(0, png, "", List.of(args));
            }
        };
    }
    private static byte[] wholeFixture() throws Exception {
        var image = new BufferedImage(200, 400, BufferedImage.TYPE_INT_RGB); var random = new Random(105);
        for (int y = 0; y < 400; y++) for (int x = 0; x < 200; x++) image.setRGB(x, y, random.nextInt(0x1000000));
        var output = new ByteArrayOutputStream(); ImageIO.write(image, "png", output); return output.toByteArray();
    }
}
