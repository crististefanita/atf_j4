package ro.whistlab.app.lifecycle;

import static org.junit.Assert.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class AiTurnCoordinatorComponentTest {
    @Test public void calculationUsesWorkerAndApplicationWaitsForOwner() throws Exception {
        Thread ownerThread = Thread.currentThread();
        var deliveries = new LinkedBlockingQueue<Runnable>();
        var calculatedOn = new AtomicReference<Thread>();
        var appliedOn = new AtomicReference<Thread>();
        try (var coordinator = new AiTurnCoordinator(Executors.newSingleThreadExecutor(), deliveries::add)) {
            assertTrue(coordinator.submit(() -> { calculatedOn.set(Thread.currentThread()); return 17; },
                    value -> { assertEquals(17, value.intValue()); appliedOn.set(Thread.currentThread()); },
                    error -> { throw new AssertionError(error); }));
            Runnable delivery = deliveries.poll(5, TimeUnit.SECONDS);
            assertNotNull("Worker must complete without requiring the owner event loop", delivery);
            assertNotSame(ownerThread, calculatedOn.get());
            assertNull(appliedOn.get());
            assertTrue(coordinator.isPending());
            delivery.run();
            assertSame(ownerThread, appliedOn.get());
            assertFalse(coordinator.isPending());
        }
    }

    @Test public void duplicateSchedulingAndDuplicateDeliveryApplyOnlyOnce() {
        var worker = new ControlledWorker();
        var owner = new ArrayDeque<Runnable>();
        var applied = new ArrayList<Integer>();
        try (var coordinator = new AiTurnCoordinator(worker, owner::add)) {
            assertTrue(coordinator.submit(() -> 1, applied::add, error -> fail()));
            assertFalse(coordinator.submit(() -> 2, applied::add, error -> fail()));
            worker.runNext();
            Runnable delivery = owner.remove();
            delivery.run();
            delivery.run();
            assertEquals(List.of(1), applied);
            assertFalse(coordinator.isPending());
        }
    }

    @Test public void cancellationBeforeStartSuppressesCalculationAndAllowsANewTurn() {
        var worker = new ControlledWorker();
        var owner = new ArrayDeque<Runnable>();
        var evaluated = new AtomicBoolean();
        var applied = new ArrayList<Integer>();
        try (var coordinator = new AiTurnCoordinator(worker, owner::add)) {
            coordinator.submit(() -> { evaluated.set(true); return 1; }, applied::add, error -> fail());
            coordinator.cancel();
            worker.runNext();
            assertFalse(evaluated.get());
            assertTrue(owner.isEmpty());
            assertTrue(coordinator.submit(() -> 2, applied::add, error -> fail()));
            worker.runNext();
            owner.remove().run();
            assertEquals(List.of(2), applied);
        }
    }

    @Test public void cancellationAfterCalculationInvalidatesQueuedSuccessAndFailure() {
        for (boolean failCalculation : List.of(false, true)) {
            var worker = new ControlledWorker();
            var owner = new ArrayDeque<Runnable>();
            var delivered = new AtomicBoolean();
            try (var coordinator = new AiTurnCoordinator(worker, owner::add)) {
                coordinator.submit(() -> {
                    if (failCalculation) throw new IllegalArgumentException("controlled failure");
                    return 1;
                }, value -> delivered.set(true), error -> delivered.set(true));
                worker.runNext();
                assertEquals(1, owner.size());
                coordinator.cancel();
                owner.remove().run();
                assertFalse(delivered.get());
            }
        }
    }

    @Test public void cancellingARunningTaskInterruptsItAndRejectsItsLateResult() throws Exception {
        var started = new CountDownLatch(1);
        var interrupted = new CountDownLatch(1);
        var finished = new CountDownLatch(1);
        var delivered = new AtomicBoolean();
        var owner = new LinkedBlockingQueue<Runnable>();
        try (var coordinator = new AiTurnCoordinator(Executors.newSingleThreadExecutor(), owner::add)) {
            coordinator.submit(() -> {
                started.countDown();
                try { new CountDownLatch(1).await(); }
                catch (InterruptedException expected) { interrupted.countDown(); }
                finally { finished.countDown(); }
                return 1; // Deliberately ignores cancellation and returns a stale result.
            }, value -> delivered.set(true), error -> delivered.set(true));
            assertTrue(started.await(5, TimeUnit.SECONDS));
            coordinator.cancel();
            assertTrue(interrupted.await(5, TimeUnit.SECONDS));
            assertTrue(finished.await(5, TimeUnit.SECONDS));
            while (!owner.isEmpty()) owner.remove().run();
            assertFalse(delivered.get());
        }
    }

    @Test public void activeFailureKeepsItsOriginalCauseAndReleasesTheTurn() {
        var worker = new ControlledWorker();
        var owner = new ArrayDeque<Runnable>();
        var cause = new IllegalStateException("original calculation failure");
        var observed = new AtomicReference<Throwable>();
        try (var coordinator = new AiTurnCoordinator(worker, owner::add)) {
            coordinator.submit(() -> { throw cause; }, value -> fail(), observed::set);
            worker.runNext();
            owner.remove().run();
            assertSame(cause, observed.get());
            assertFalse(coordinator.isPending());
        }
    }

    @Test public void closeIsIdempotentAndRejectsPendingOrNewDeliveries() {
        var worker = new ControlledWorker();
        var owner = new ArrayDeque<Runnable>();
        var delivered = new AtomicBoolean();
        var coordinator = new AiTurnCoordinator(worker, owner::add);
        coordinator.submit(() -> 1, value -> delivered.set(true), error -> fail());
        worker.runNext();
        coordinator.close();
        coordinator.close();
        owner.remove().run();
        assertTrue(worker.isShutdown());
        assertFalse(delivered.get());
        assertThrows(IllegalStateException.class, () -> coordinator.submit(() -> 2, value -> fail(), error -> fail()));
    }

    @Test public void rejectedWorkerDoesNotLeaveTheCoordinatorBusy() {
        var worker = new ControlledWorker();
        worker.shutdown();
        try (var coordinator = new AiTurnCoordinator(worker, Runnable::run)) {
            assertThrows(RejectedExecutionException.class, () -> coordinator.submit(() -> 1, value -> fail(), error -> fail()));
            assertFalse(coordinator.isPending());
        }
    }

    static final class ControlledWorker extends AbstractExecutorService {
        private final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
        private boolean closed;
        void runNext() { tasks.remove().run(); }
        @Override public void execute(Runnable task) {
            if (closed) throw new RejectedExecutionException();
            tasks.add(task);
        }
        @Override public void shutdown() { closed = true; }
        @Override public List<Runnable> shutdownNow() { closed = true; var rest = List.copyOf(tasks); tasks.clear(); return rest; }
        @Override public boolean isShutdown() { return closed; }
        @Override public boolean isTerminated() { return closed && tasks.isEmpty(); }
        @Override public boolean awaitTermination(long timeout, TimeUnit unit) { return isTerminated(); }
    }
}
