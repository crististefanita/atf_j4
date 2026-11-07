package ro.whistlab.app.presentation;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class PresentationRefreshComponentTest extends atf.gradle.audit.GradleAuditedTest {
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

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "drawn", drawn, "freezes", freezes, "queue", queue, "refresh", refresh, "viewport", viewport);
        assertEquals("freezes.get() must equal 1", 1, freezes.get());
        assertEquals("queue.size() must equal 1", 1, queue.size());
        queue.get(0).run();
        queue.get(0).run();
        assertEquals("drawn must equal List.of(\"compact-portrait\")", List.of("compact-portrait"), drawn);
    }

    @Test public void cancellationRejectsTheOldQueuedCallbackWithoutDroppingANewerRequest() {
        var queue = new ArrayList<Runnable>();
        var draws = new AtomicInteger();
        var refresh = new PresentationRefresh(() -> { }, draws::incrementAndGet, queue::add, () -> true);
        refresh.request();
        refresh.cancel();
        refresh.request();
        queue.get(0).run();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "draws", draws, "queue", queue, "refresh", refresh);
        assertEquals("draws.get() must equal 0", 0, draws.get());
        queue.get(1).run();
        assertEquals("draws.get() must equal 1", 1, draws.get());
        queue.get(0).run();
        assertEquals("draws.get() must equal 1", 1, draws.get());
    }

    @Test public void anUnavailablePresentationDoesNotRenderAndCannotLeaveTheQueueStuck() {
        var queue = new ArrayList<Runnable>();
        var draws = new AtomicInteger();
        var available = new AtomicReference<>(false);
        var refresh = new PresentationRefresh(() -> { }, draws::incrementAndGet, queue::add, available::get);
        refresh.request();
        queue.get(0).run();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "available", available, "draws", draws, "queue", queue, "refresh", refresh);
        assertEquals("draws.get() must equal 0", 0, draws.get());
        available.set(true);
        refresh.request();
        assertEquals("queue.size() must equal 2", 2, queue.size());
        queue.get(1).run();
        assertEquals("draws.get() must equal 1", 1, draws.get());
    }

    @Test public void navigationDuringSuspensionPreventsEnqueuingTheObsoleteRequest() {
        var owner = new AtomicReference<PresentationRefresh>();
        var queue = new ArrayList<Runnable>();
        owner.set(new PresentationRefresh(() -> owner.get().cancel(), () -> fail("Render"), queue::add, () -> true));
        owner.get().request();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "owner", owner, "queue", queue);
        assertTrue("queue.isEmpty() must hold", queue.isEmpty());
    }

    @Test public void navigationDuringAvailabilityCheckCannotRenderAfterItsLifetimeWasCancelled() {
        var owner = new AtomicReference<PresentationRefresh>();
        var queue = new ArrayList<Runnable>();
        var draws = new AtomicInteger();
        atf.gradle.audit.GradleInputs.fixture("Refresh cancelled during availability check", "initial queue size", queue.size(), "initial draws", draws.get());
        owner.set(new PresentationRefresh(() -> { }, draws::incrementAndGet, queue::add, () -> {
            owner.get().cancel();
            return true;
        }));
        owner.get().request();
        queue.get(0).run();
        assertEquals("Navigation inside availability checking cancels the queued render", 0, draws.get());
        assertEquals("The controlled refresh delivered one queued callback", 1, queue.size());
    }


    @Test public void synchronousDeliveryCanScheduleTheNextGeometryWithoutLosingIt() {
        var owner = new AtomicReference<PresentationRefresh>();
        var draws = new AtomicInteger();
        owner.set(new PresentationRefresh(() -> { }, () -> {
            if (draws.incrementAndGet() == 1) owner.get().request();
        }, Runnable::run, () -> true));
        owner.get().request();

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "draws", draws, "owner", owner);
        assertEquals("draws.get() must equal 2", 2, draws.get());
    }

    @Test public void failedSuspensionKeepsItsCauseAndAllowsANewAttempt() {
        var queue = new ArrayList<Runnable>();
        var failure = new IllegalStateException("Suspend failed");
        var attempts = new AtomicInteger();
        var refresh = new PresentationRefresh(() -> { if (attempts.getAndIncrement() == 0) throw failure; },
                () -> { }, queue::add, () -> true);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "attempts", attempts, "failure", failure, "queue", queue, "refresh", refresh);
        assertSame("assertThrows(IllegalStateException.class, refresh::request) keeps the identity of failure", failure, assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class, refresh::request));
        refresh.request();
        assertEquals("queue.size() must equal 1", 1, queue.size());
    }

    @Test public void failedQueueKeepsItsCauseAndDoesNotBlockTheNextRequest() {
        var failure = new IllegalStateException("Queue unavailable");
        var requests = new AtomicInteger();
        var queue = new ArrayList<Runnable>();
        var refresh = new PresentationRefresh(() -> { }, () -> { }, task -> {
            if (requests.getAndIncrement() == 0) throw failure;
            queue.add(task);
        }, () -> true);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "failure", failure, "queue", queue, "refresh", refresh, "requests", requests);
        assertSame("assertThrows(IllegalStateException.class, refresh::request) keeps the identity of failure", failure, assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class, refresh::request));
        refresh.request();
        assertEquals("queue.size() must equal 1", 1, queue.size());
    }
}
