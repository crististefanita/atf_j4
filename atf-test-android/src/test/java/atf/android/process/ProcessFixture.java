package atf.android.process;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Local subprocess fixture. It never contacts Android or a server. */
public final class ProcessFixture {
    private ProcessFixture() { }

    static List<String> command(String... arguments) {
        java.util.ArrayList<String> command = new java.util.ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-XX:ActiveProcessorCount=1", "-Xmx32m", "-cp", System.getProperty("java.class.path"),
                ProcessFixture.class.getName()));
        command.addAll(List.of(arguments));
        return command;
    }

    public static void main(String[] arguments) throws Exception {
        switch (arguments[0]) {
            case "output" -> {
                System.out.write(new byte[] {(byte) 0xff, 0, 1, 13, 10});
                System.err.print("separate-error");
                System.exit(7);
            }
            case "flood" -> {
                byte[] bytes = new byte[256 * 1024];
                java.util.Arrays.fill(bytes, (byte) 'x');
                System.out.write(bytes);
                System.err.write(bytes);
            }
            case "tree" -> {
                Process child = new ProcessBuilder(command("sleep")).inheritIO().start();
                Path marker = Path.of(arguments[1]);
                Path pending = marker.resolveSibling("child.pid.pending");
                Files.writeString(pending, Long.toString(child.pid()));
                Files.move(pending, marker, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                while (!Files.exists(Path.of(arguments[2]))) Thread.sleep(10);
            }
            case "sleep" -> Thread.sleep(60000);
            default -> throw new IllegalArgumentException("Unknown local fixture mode");
        }
    }

    static Process exitAfterDescendantObservation(Process delegate, Path release) {
        return new Process() {
            private boolean released;
            @Override public java.io.OutputStream getOutputStream() { return delegate.getOutputStream(); }
            @Override public java.io.InputStream getInputStream() { return delegate.getInputStream(); }
            @Override public java.io.InputStream getErrorStream() { return delegate.getErrorStream(); }
            @Override public int waitFor() throws InterruptedException { return delegate.waitFor(); }
            @Override public boolean waitFor(long time, java.util.concurrent.TimeUnit unit) throws InterruptedException {
                return delegate.waitFor(time, unit);
            }
            @Override public int exitValue() { return delegate.exitValue(); }
            @Override public void destroy() { delegate.destroy(); }
            @Override public Process destroyForcibly() { delegate.destroyForcibly(); return this; }
            @Override public java.util.stream.Stream<ProcessHandle> descendants() {
                var observed = delegate.descendants().toList();
                if (!released && !observed.isEmpty()) {
                    try { Files.writeString(release, "observed"); }
                    catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
                    released = true;
                }
                return observed.stream();
            }
        };
    }
}
