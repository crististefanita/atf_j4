package atf.android;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import java.util.Objects;
import java.util.Map;
import atf.android.process.CommandProcess;

/** Runs a single explicitly selected Android device command with bounded execution time. */
public final class AdbClient {
    private final String executable;
    private final String serial;
    private final String serverSocket;
    private final Duration timeout;

    public AdbClient(Path executable, String serial, String serverSocket, Duration timeout) {
        Path normalized = Objects.requireNonNull(executable).normalize();
        this.executable = normalized.getParent() == null ? normalized.toString() : normalized.toAbsolutePath().toString();
        this.serial = requireText(serial, "serial");
        this.serverSocket = serverSocket == null || serverSocket.isBlank() ? null : serverSocket.trim();
        this.timeout = Objects.requireNonNull(timeout);
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout must be positive");
    }

    public static AdbClient fromSystemProperties() {
        String executable = setting("atf.android.adb", "ATF_ANDROID_ADB", "adb");
        String serial = setting("atf.android.serial", "ATF_ANDROID_SERIAL", "emulator-5554");
        String socket = setting("atf.android.adbServerSocket", "ADB_SERVER_SOCKET", "");
        long timeout = Long.parseLong(setting("atf.android.commandTimeoutSeconds", "ATF_ANDROID_TIMEOUT_SECONDS", "30"));
        return new AdbClient(Path.of(executable), serial, socket, Duration.ofSeconds(timeout));
    }

    public Result run(String... arguments) throws IOException, InterruptedException {
        List<String> command = command(arguments);
        CommandProcess.Output result = execute(command, true);
        return new Result(result.exitCode(), new String(result.stdout(), StandardCharsets.UTF_8), command);
    }

    /** Runs a command whose stdout is binary (for example {@code exec-out screencap -p}). */
    public BinaryResult runBinary(String... arguments) throws IOException, InterruptedException {
        List<String> command = command(arguments);
        CommandProcess.Output result = execute(command, false);
        return new BinaryResult(result.exitCode(), result.stdout(), result.stderr(), command);
    }

    private CommandProcess.Output execute(List<String> command, boolean mergeError)
            throws IOException, InterruptedException {
        Map<String, String> environment = serverSocket == null ? Map.of() : Map.of("ADB_SERVER_SOCKET", serverSocket);
        return CommandProcess.execute(command, environment, timeout, mergeError);
    }

    private List<String> command(String... arguments) {
        List<String> command = new ArrayList<>();
        command.add(executable);
        command.add("-s"); command.add(serial);
        command.addAll(List.of(arguments));
        return List.copyOf(command);
    }

    private static String setting(String property, String env, String fallback) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) value = System.getenv(env);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }

    public record Result(int exitCode, String output, List<String> command) {
        public Result { Objects.requireNonNull(output); command = List.copyOf(command); }
        public boolean succeeded() { return exitCode == 0; }
        public void requireSuccess() throws IOException {
            if (!succeeded()) throw new IOException("ADB exited " + exitCode + ": " + output);
        }
    }

    public record BinaryResult(int exitCode, byte[] output, String error, List<String> command) {
        public BinaryResult {
            output = Objects.requireNonNull(output).clone();
            error = Objects.requireNonNull(error);
            command = List.copyOf(command);
        }
        @Override public byte[] output() { return output.clone(); }
        public boolean succeeded() { return exitCode == 0; }
        public void requireSuccess() throws IOException {
            if (!succeeded()) throw new IOException("ADB exited " + exitCode + ": " + error);
        }
    }
}
