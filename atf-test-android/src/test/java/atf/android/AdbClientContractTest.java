package atf.android;

import atf.android.component.AndroidComponentAudit;
import org.junit.jupiter.api.extension.RegisterExtension;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertArrayEquals;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

import atf.testkit.AtfComponentTest;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

@AtfComponentTest
public class AdbClientContractTest {
    @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
    @Test
    void requiresAnExplicitNonBlankDeviceSerial() {
        assertThrows(IllegalArgumentException.class,
                () -> new AdbClient(Path.of("adb"), "  ", null, Duration.ofSeconds(1)));
    }

    @Test
    void rejectsUnboundedCommandTimeouts() {
        assertThrows(IllegalArgumentException.class,
                () -> new AdbClient(Path.of("adb"), "emulator-5554", null, Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new AdbClient(Path.of("adb"), "emulator-5554", null, Duration.ofSeconds(-1)));
    }

    @Test
    void resultExposesImmutableCommandAndExplicitExitSemantics() throws Exception {
        List<String> command = new ArrayList<>(List.of("adb", "-s", "emulator-5554", "version"));
        AdbClient.Result success = new AdbClient.Result(0, "Android Debug Bridge version", command);
        command.add("shell");

        assertTrue(success.succeeded());
        assertTrue(success.command().contains("version"));
        assertEquals(4, success.command().size());
        assertThrows(UnsupportedOperationException.class, () -> success.command().add("shell"));
        success.requireSuccess();

        AdbClient.Result failure = new AdbClient.Result(1, "offline", List.of("adb", "-s", "emulator-5554"));
        assertFalse(failure.succeeded());
        IOException error = assertThrows(IOException.class, failure::requireSuccess);
        assertTrue(error.getMessage().contains("offline"));
    }

    @Test
    void binaryResultPreservesScreenshotBytesAndDefensivelyCopiesPayload() throws Exception {
        byte[] pngSignature = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        AdbClient.BinaryResult result = new AdbClient.BinaryResult(0, pngSignature, "",
                List.of("adb", "-s", "emulator-5554", "exec-out", "screencap", "-p"));
        pngSignature[0] = 0;

        assertTrue(result.succeeded());
        assertArrayEquals(new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}, result.output());
        byte[] returned = result.output();
        returned[0] = 0;
        assertEquals((byte) 0x89, result.output()[0]);
        assertTrue(result.command().contains("screencap"));
    }
}
