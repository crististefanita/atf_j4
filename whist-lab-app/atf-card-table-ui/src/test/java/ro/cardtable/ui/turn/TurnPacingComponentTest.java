package ro.cardtable.ui.turn;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static atf.gradle.audit.assertions.GradleAssertions.*;

/** Consumer-neutral pacing: render first, wait one interval, then revalidate before acting. */
public final class TurnPacingComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void oneAutomatedActionWaitsForItsVisibleFrameAndConfiguredInterval() {
        var frame = new ManualFrame();
        var timer = new ManualTimer();
        var actions = new AtomicInteger();
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "frame", frame, "timer", timer,
                "actions", actions, "delay", TurnPacing.ACTION_DELAY_MILLIS);

        TurnPacing.afterVisibleFrame(true, false, frame::await, () -> true, timer::schedule, actions::incrementAndGet);

        assertNotNull("An automated actor waits for the first rendered frame", frame.pending);
        assertNull("No action is queued before the actor is visible", timer.pending);
        frame.draw();
        assertEquals("The action is scheduled at the shared interval", 550L, timer.due);
        timer.advanceTo(549);
        assertEquals("The action does not run early", 0, actions.get());
        timer.advanceTo(550);
        assertEquals("Exactly one action runs at the deadline", 1, actions.get());
    }

    @Test public void humanBlockedStaleAndReplacedScenesCannotRunAnAutomatedAction() {
        var frame = new ManualFrame();
        var timer = new ManualTimer();
        var current = new AtomicBoolean(true);
        var actions = new AtomicInteger();
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "frame", frame, "timer", timer,
                "current", current, "actions", actions);
        Runnable action = actions::incrementAndGet;

        TurnPacing.afterVisibleFrame(false, false, frame::await, current::get, timer::schedule, action);
        TurnPacing.afterVisibleFrame(true, true, frame::await, current::get, timer::schedule, action);
        TurnPacing.afterVisibleFrame(true, false, frame::await, () -> false, timer::schedule, action);
        assertNull("Human, blocked and stale scenes do not register a frame", frame.pending);

        TurnPacing.afterVisibleFrame(true, false, frame::await, current::get, timer::schedule, action);
        current.set(false);
        frame.draw();
        assertNull("A scene replaced before its first frame cannot schedule", timer.pending);

        current.set(true);
        TurnPacing.afterVisibleFrame(true, false, frame::await, current::get, timer::schedule, action);
        frame.draw();
        current.set(false);
        timer.advanceTo(550);
        assertEquals("A scene replaced during the delay cannot act", 0, actions.get());
    }

    private static final class ManualFrame {
        private Runnable pending;
        private void await(Runnable action) { assertNull("Only one first-frame callback is registered", pending); pending = action; }
        private void draw() { assertNotNull("A frame callback must be registered", pending); Runnable action = pending; pending = null; action.run(); }
    }

    private static final class ManualTimer {
        private long now, due;
        private Runnable pending;
        private void schedule(Runnable action, Long delay) { assertNull("Only one timed action is pending", pending); pending = action; due = now + delay; }
        private void advanceTo(long time) { now = time; if (pending != null && now >= due) { Runnable action = pending; pending = null; action.run(); } }
    }
}
