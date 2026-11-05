package atf.android.process;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** One process and its output drains share one monotonic deadline. */
public final class CommandProcess {
    private CommandProcess() { }

    public static Output execute(List<String> command, Map<String, String> environment,
                                 Duration timeout, boolean mergeError) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(mergeError);
        Objects.requireNonNull(timeout);
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout must be positive");
        builder.environment().putAll(environment);
        long started = System.nanoTime();
        long budget;
        try { budget = timeout.toNanos(); }
        catch (ArithmeticException overflow) { budget = Long.MAX_VALUE; }
        return collect(start(builder::start, timeout, started, budget), timeout, started, budget);
    }

    @FunctionalInterface
    interface Starter { Process start() throws IOException; }

    static Process start(Starter starter, Duration timeout, long started, long budget)
            throws IOException, InterruptedException {
        CompletableFuture<Process> launched = new CompletableFuture<>();
        background("start", () -> {
            try { launched.complete(starter.start()); }
            catch (IOException | RuntimeException failure) { launched.completeExceptionally(failure); }
        });
        try { return launched.get(remaining(started, budget), TimeUnit.NANOSECONDS); }
        catch (TimeoutException expired) {
            IOException failure = new IOException("Command timed out after " + timeout, expired);
            launched.thenAccept(process -> background("late-cleanup", () -> cleanup(process, new LinkedHashMap<>(), failure)));
            throw failure;
        } catch (InterruptedException interrupted) {
            launched.thenAccept(process -> background("late-cleanup", () -> cleanup(process, new LinkedHashMap<>(), interrupted)));
            Thread.currentThread().interrupt();
            throw interrupted;
        } catch (ExecutionException failure) {
            if (failure.getCause() instanceof IOException io) throw io;
            throw new IOException("Cannot start command process", failure.getCause());
        }
    }

    static Output collect(Process process, Duration timeout, long started, long budget)
            throws IOException, InterruptedException {
        Map<Long, ProcessHandle> descendants = new LinkedHashMap<>();
        CompletableFuture<byte[]> out = new CompletableFuture<>();
        CompletableFuture<byte[]> err = new CompletableFuture<>();
        boolean complete = false;
        Throwable primary = null;
        try {
            drain(process.getInputStream(), out);
            drain(process.getErrorStream(), err);
            process.getOutputStream().close();
            while (true) {
                process.descendants().forEach(child -> descendants.put(child.pid(), child));
                checkFailure(out);
                checkFailure(err);
                long left = remaining(started, budget);
                if (process.waitFor(Math.min(left, TimeUnit.MILLISECONDS.toNanos(10)), TimeUnit.NANOSECONDS)) break;
            }
            // Inspect both readers while waiting: a blocked stdout must not hide a stderr failure.
            while (!out.isDone() || !err.isDone()) {
                checkFailure(out);
                checkFailure(err);
                TimeUnit.NANOSECONDS.sleep(Math.min(remaining(started, budget), TimeUnit.MILLISECONDS.toNanos(10)));
            }
            byte[] stdout = await(out, started, budget);
            byte[] stderr = await(err, started, budget);
            complete = true;
            return new Output(process.exitValue(), stdout, new String(stderr, StandardCharsets.UTF_8));
        } catch (TimeoutException expired) {
            IOException failure = observedFailure(out);
            if (failure == null) failure = observedFailure(err);
            if (failure == null) failure = new IOException("Command timed out after " + timeout, expired);
            primary = failure;
            throw failure;
        } catch (InterruptedException interrupted) {
            primary = interrupted;
            Thread.currentThread().interrupt();
            throw interrupted;
        } catch (IOException | RuntimeException | Error failure) {
            primary = failure;
            throw failure;
        } finally {
            if (!complete) {
                // No unbounded join or synchronous close: a pipe may be held by a descendant.
                cleanup(process, descendants, primary);
            }
        }
    }

    private static void cleanup(Process process, Map<Long, ProcessHandle> descendants, Throwable primary) {
        cleanupAction(() -> process.descendants().forEach(child -> descendants.put(child.pid(), child)), primary);
        for (ProcessHandle child : descendants.values()) {
            cleanupAction(child::destroyForcibly, primary);
        }
        cleanupAction(process::destroyForcibly, primary);
        cleanupAction(() -> closeLater(process.getInputStream()), primary);
        cleanupAction(() -> closeLater(process.getErrorStream()), primary);
    }

    private static void cleanupAction(Runnable action, Throwable primary) {
        try { action.run(); }
        catch (RuntimeException | Error failure) { primary.addSuppressed(failure); }
    }

    private static void drain(InputStream input, CompletableFuture<byte[]> result) {
        background("output", () -> {
            try { result.complete(input.readAllBytes()); }
            catch (IOException failure) { result.completeExceptionally(failure); }
            finally { closeLater(input); }
        });
    }

    private static void closeLater(InputStream input) {
        background("close", () -> {
            try { input.close(); }
            catch (IOException ignored) { /* the failed operation has already been reported */ }
        });
    }

    private static void background(String role, Runnable action) {
        // Blocking native pipe I/O must not depend on free virtual-thread carrier slots.
        Thread.ofPlatform().daemon(true).name("atf-command-" + role).start(action);
    }

    private static void checkFailure(CompletableFuture<byte[]> result) throws IOException {
        IOException failure = observedFailure(result);
        if (failure != null) throw failure;
    }

    private static IOException observedFailure(CompletableFuture<byte[]> result) {
        try { result.getNow(null); return null; }
        catch (CompletionException failure) { return new IOException("Cannot read command output", failure.getCause()); }
    }

    private static byte[] await(CompletableFuture<byte[]> result, long started, long budget)
            throws IOException, InterruptedException, TimeoutException {
        try { return result.get(remaining(started, budget), TimeUnit.NANOSECONDS); }
        catch (ExecutionException failure) { throw readFailure(failure); }
    }

    private static IOException readFailure(ExecutionException failure) {
        return new IOException("Cannot read command output", failure.getCause());
    }

    private static long remaining(long started, long budget) throws TimeoutException {
        long left = budget - (System.nanoTime() - started);
        if (left <= 0) throw new TimeoutException("Process and output deadline expired");
        return left;
    }

    public record Output(int exitCode, byte[] stdout, String stderr) {
        public Output { stdout = stdout.clone(); }
        @Override public byte[] stdout() { return stdout.clone(); }
    }
}
