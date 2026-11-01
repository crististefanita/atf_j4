package atf.android;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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
        List<String> command = new ArrayList<>();
        command.add(executable);
        command.add("-s"); command.add(serial);
        command.addAll(List.of(arguments));
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        if (serverSocket != null) builder.environment().put("ADB_SERVER_SOCKET", serverSocket);
        Process process = builder.start();
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        Thread reader = Thread.startVirtualThread(() -> {
            try { process.getInputStream().transferTo(captured); }
            catch (IOException ignored) { /* the process may be forcibly stopped on timeout */ }
        });
        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            reader.join();
            throw new IOException("ADB command timed out after " + timeout + ": " + safeCommand(List.of(arguments)));
        }
        reader.join();
        String output = captured.toString(StandardCharsets.UTF_8);
        return new Result(process.exitValue(), output, List.copyOf(command));
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

    private static String safeCommand(List<String> args) { return String.join(" ", args); }

    public record Result(int exitCode, String output, List<String> command) {
        public Result { Objects.requireNonNull(output); command = List.copyOf(command); }
        public boolean succeeded() { return exitCode == 0; }
        public void requireSuccess() throws IOException {
            if (!succeeded()) throw new IOException("ADB exited " + exitCode + ": " + output);
        }
    }
}
