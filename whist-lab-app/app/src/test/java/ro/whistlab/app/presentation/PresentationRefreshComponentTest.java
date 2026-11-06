package ro.whistlab.app.presentation;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class PresentationRefreshComponentTest {
    @Test public void aBurstFreezesOnceAndReadsTheLatestViewportWhenTheQueueRuns() {
        var queue = new ArrayList<Runnable>();
        var freezes = new AtomicInteger();
        var viewport = new AtomicReference<>("portrait");
        var drawn = new ArrayList<String>();
        var refresh = new PresentationRefresh(freezes::incrementAndGet,
                () -> drawn.add(viewport.get()), queue::add, () -> true);
        refresh.request();
        viewport.set("landscape");
        refresh.request();
        viewport.set("compact-portrait");
        refresh.request();
        assertEquals(1, freezes.get());
        assertEquals(1, queue.size());
        queue.get(0).run();
        queue.get(0).run();
        assertEquals(List.of("compact-portrait"), drawn);
    }

    @Test public void cancellationRejectsTheOldQueuedCallbackWithoutDroppingANewerRequest() {
        var queue = new ArrayList<Runnable>();
        var draws = new AtomicInteger();
        var refresh = new PresentationRefresh(() -> { }, draws::incrementAndGet, queue::add, () -> true);
        refresh.request();
        refresh.cancel();
        refresh.request();
        queue.get(0).run();
        assertEquals(0, draws.get());
        queue.get(1).run();
        assertEquals(1, draws.get());
        queue.get(0).run();
        assertEquals(1, draws.get());
    }

    @Test public void anUnavailablePresentationDoesNotRenderAndCannotLeaveTheQueueStuck() {
        var queue = new ArrayList<Runnable>();
        var draws = new AtomicInteger();
        var available = new AtomicReference<>(false);
        var refresh = new PresentationRefresh(() -> { }, draws::incrementAndGet, queue::add, available::get);
        refresh.request();
        queue.get(0).run();
        assertEquals(0, draws.get());
        available.set(true);
        refresh.request();
        assertEquals(2, queue.size());
        queue.get(1).run();
        assertEquals(1, draws.get());
    }

    @Test public void navigationDuringSuspensionPreventsEnqueuingTheObsoleteRequest() {
        var owner = new AtomicReference<PresentationRefresh>();
        var queue = new ArrayList<Runnable>();
        owner.set(new PresentationRefresh(() -> owner.get().cancel(), () -> fail("Render"), queue::add, () -> true));
        owner.get().request();
        assertTrue(queue.isEmpty());
    }

    @Test public void navigationDuringAvailabilityCheckCannotRenderAfterItsLifetimeWasCancelled() {
        var owner = new AtomicReference<PresentationRefresh>();
        var queue = new ArrayList<Runnable>();
        owner.set(new PresentationRefresh(() -> { }, () -> fail("Obsolete render"), queue::add, () -> {
            owner.get().cancel();
            return true;
        }));
        owner.get().request();
        queue.get(0).run();
    }

    @Test public void synchronousDeliveryCanScheduleTheNextGeometryWithoutLosingIt() {
        var owner = new AtomicReference<PresentationRefresh>();
        var draws = new AtomicInteger();
        owner.set(new PresentationRefresh(() -> { }, () -> {
            if (draws.incrementAndGet() == 1) owner.get().request();
        }, Runnable::run, () -> true));
        owner.get().request();
        assertEquals(2, draws.get());
    }

    @Test public void failedSuspensionKeepsItsCauseAndAllowsANewAttempt() {
        var queue = new ArrayList<Runnable>();
        var failure = new IllegalStateException("Suspend failed");
        var attempts = new AtomicInteger();
        var refresh = new PresentationRefresh(() -> { if (attempts.getAndIncrement() == 0) throw failure; },
                () -> { }, queue::add, () -> true);
        assertSame(failure, assertThrows(IllegalStateException.class, refresh::request));
        refresh.request();
        assertEquals(1, queue.size());
    }

    @Test public void failedQueueKeepsItsCauseAndDoesNotBlockTheNextRequest() {
        var failure = new IllegalStateException("Queue unavailable");
        var requests = new AtomicInteger();
        var queue = new ArrayList<Runnable>();
        var refresh = new PresentationRefresh(() -> { }, () -> { }, task -> {
            if (requests.getAndIncrement() == 0) throw failure;
            queue.add(task);
        }, () -> true);
        assertSame(failure, assertThrows(IllegalStateException.class, refresh::request));
        refresh.request();
        assertEquals(1, queue.size());
    }
}
