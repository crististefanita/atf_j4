package atf.android;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import atf.testkit.AtfComponentTest;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

@AtfComponentTest
public class AdbClientContractTest {
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
}
