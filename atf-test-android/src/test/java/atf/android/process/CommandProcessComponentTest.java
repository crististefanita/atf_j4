package atf.android.process;

import atf.android.component.AndroidComponentAudit;
import org.junit.jupiter.api.extension.RegisterExtension;

import static atf.testkit.assertions.AuditedAssertions.assertArrayEquals;
import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertInstanceOf;
import static atf.testkit.assertions.AuditedAssertions.assertSame;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

import atf.testkit.AtfComponentTest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

@AtfComponentTest
@Timeout(60)
public class CommandProcessComponentTest {
    @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
    @Test void aLateProcessStartIsBoundedAndItsEventualProcessIsCleaned() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch destroyed = new CountDownLatch(1);
        Process process = new StubProcess(new ByteArrayInputStream(new byte[0])) {
            @Override public Process destroyForcibly() { destroyed.countDown(); return this; }
        };
        try {
            assertThrows(IOException.class, () -> CommandProcess.start(() -> {
                try { release.await(); }
                catch (InterruptedException failure) { throw new IOException(failure); }
                return process;
            }, Duration.ofMillis(100), System.nanoTime(), TimeUnit.MILLISECONDS.toNanos(100)));
            release.countDown();
            assertTrue(destroyed.await(2, TimeUnit.SECONDS), "A late process must not escape after timeout");
        } finally { release.countDown(); }
    }
    @Test void separatesBinaryOutputAndKeepsNonzeroExit() throws Exception {
        var result = CommandProcess.execute(ProcessFixture.command("output"), Map.of(), Duration.ofSeconds(30), false);
        assertEquals(7, result.exitCode());
        assertArrayEquals(new byte[] {(byte) 0xff, 0, 1, 13, 10}, result.stdout());
        assertEquals("separate-error", result.stderr());
        result.stdout()[0] = 0;
        assertEquals((byte) 0xff, result.stdout()[0]);
    }

    @Test void drainsBothFullPipesWithoutDeadlock() throws Exception {
        var result = CommandProcess.execute(ProcessFixture.command("flood"), Map.of(), Duration.ofSeconds(30), false);
        assertEquals(0, result.exitCode());
        assertEquals(256 * 1024, result.stdout().length);
        assertEquals(256 * 1024, result.stderr().length());
    }

    @Test void mergedOutputUsesTheSameDeadline() throws Exception {
        var result = CommandProcess.execute(ProcessFixture.command("output"), Map.of(), Duration.ofSeconds(30), true);
        assertEquals(7, result.exitCode());
        assertTrue(new String(result.stdout(), java.nio.charset.StandardCharsets.UTF_8).contains("separate-error"));
        assertEquals("", result.stderr());
    }

    @Test void blockedDrainCannotTurnProcessExitIntoAnUnboundedJoin() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        StubProcess process = new StubProcess(new InputStream() {
            @Override public int read() throws IOException {
                try { release.await(); return -1; }
                catch (InterruptedException failure) { throw new IOException(failure); }
            }
            @Override public void close() { release.countDown(); }
        });
        long start = System.nanoTime();
        try {
            IOException failure = assertThrows(IOException.class, () -> CommandProcess.collect(
                    process, Duration.ofMillis(150), start, TimeUnit.MILLISECONDS.toNanos(150)));
            assertTrue(failure.getMessage().contains("timed out"));
            assertTrue(Duration.ofNanos(System.nanoTime()-start).toMillis() < 1500);
            assertTrue(process.destroyed);
        } finally { release.countDown(); }
    }

    @Test void readFailureIsNotSilentlyReportedAsSuccessAndCleanupCannotMaskIt() {
        IOException original = new IOException("controlled read failure");
        StubProcess process = new StubProcess(new InputStream() {
            @Override public int read() throws IOException { throw original; }
        });
        process.failDestroy = true;
        IOException failure = assertThrows(IOException.class, () -> CommandProcess.collect(process,
                Duration.ofSeconds(30), System.nanoTime(), TimeUnit.SECONDS.toNanos(30)));
        assertSame(original, failure.getCause());
        assertEquals("controlled cleanup failure", failure.getSuppressed()[0].getMessage());
    }

    @Test void aBlockedStdoutDoesNotHideTheOriginalStderrFailure() {
        CountDownLatch release = new CountDownLatch(1);
        IOException original = new IOException("controlled stderr failure");
        StubProcess process = new StubProcess(new InputStream() {
            @Override public int read() throws IOException {
                try { release.await(); return -1; }
                catch (InterruptedException failure) { throw new IOException(failure); }
            }
            @Override public void close() { release.countDown(); }
        }) {
            @Override public InputStream getErrorStream() {
                return new InputStream() { @Override public int read() throws IOException { throw original; } };
            }
        };
        try {
            IOException failure = assertThrows(IOException.class, () -> CommandProcess.collect(process,
                    Duration.ofSeconds(30), System.nanoTime(), TimeUnit.SECONDS.toNanos(30)));
            assertSame(original, failure.getCause());
            assertTrue(process.destroyed);
        } finally { release.countDown(); }
    }

    @Test void interruptionKillsTheOwnedProcessAndKeepsTheInterruptFlag() throws Exception {
        Process process = new ProcessBuilder(ProcessFixture.command("sleep")).start();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean interrupted = new AtomicBoolean();
        Thread caller = new Thread(() -> {
            try { CommandProcess.collect(process, Duration.ofSeconds(30), System.nanoTime(), TimeUnit.SECONDS.toNanos(30)); }
            catch (IOException | InterruptedException error) { failure.set(error); interrupted.set(Thread.currentThread().isInterrupted()); }
        });
        try {
            caller.start();
            caller.interrupt();
            caller.join(2000);
            assertFalse(caller.isAlive(), "Interrupted caller must return without waiting for output EOF");
            assertInstanceOf(InterruptedException.class, failure.get());
            assertTrue(interrupted.get());
            assertTrue(process.waitFor(2, TimeUnit.SECONDS), "Owned process must terminate");
        } finally { process.destroyForcibly(); caller.interrupt(); }
    }

    @Test void timedOutTreeKillsDescendantHoldingThePipeAfterParentExit(@TempDir Path directory) throws Exception {
        Path pidFile = directory.resolve("child.pid");
        Path release = directory.resolve("parent-can-exit");
        Process process = new ProcessBuilder(ProcessFixture.command("tree", pidFile.toString(), release.toString())).start();
        ProcessHandle child = null;
        try {
            long fixtureStarted = System.nanoTime();
            while (!Files.exists(pidFile) && process.isAlive()
                    && System.nanoTime()-fixtureStarted < TimeUnit.SECONDS.toNanos(30)) Thread.sleep(10);
            assertTrue(Files.exists(pidFile), "Fixture must actually create a descendant before measuring the deadline");
            child = ProcessHandle.of(Long.parseLong(Files.readString(pidFile))).orElseThrow();
            Process observed = ProcessFixture.exitAfterDescendantObservation(process, release);
            long start = System.nanoTime();
            assertThrows(IOException.class, () -> CommandProcess.collect(observed, Duration.ofSeconds(3),
                    start, TimeUnit.SECONDS.toNanos(3)));
            assertTrue(Duration.ofNanos(System.nanoTime()-start).toMillis() < 4500);
            assertTrue(Files.exists(pidFile), "Fixture must actually create a descendant");
            if (child != null) {
                child.onExit().get(2, TimeUnit.SECONDS);
                assertFalse(child.isAlive());
            }
            assertFalse(process.isAlive());
        } finally { process.destroyForcibly(); if (child != null) child.destroyForcibly(); }
    }

    private static class StubProcess extends Process {
        private final InputStream input;
        private boolean destroyed;
        private boolean failDestroy;
        StubProcess(InputStream input) { this.input = input; }
        @Override public OutputStream getOutputStream() { return new ByteArrayOutputStream(); }
        @Override public InputStream getInputStream() { return input; }
        @Override public InputStream getErrorStream() { return new ByteArrayInputStream(new byte[0]); }
        @Override public int waitFor() { return 0; }
        @Override public boolean waitFor(long time, TimeUnit unit) { return true; }
        @Override public int exitValue() { return 0; }
        @Override public void destroy() { destroyed = true; }
        @Override public Process destroyForcibly() {
            destroyed = true;
            if (failDestroy) throw new IllegalStateException("controlled cleanup failure");
            return this;
        }
        @Override public Stream<ProcessHandle> descendants() { return Stream.empty(); }
    }
}
