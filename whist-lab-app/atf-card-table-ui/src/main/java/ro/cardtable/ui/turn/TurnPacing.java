package ro.cardtable.ui.turn;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Waits until an automated actor is visible, then schedules exactly one current action. */
public final class TurnPacing {
    public static final long ACTION_DELAY_MILLIS = 550L;

    private TurnPacing() { }

    public static void afterVisibleFrame(boolean automatedTurn, boolean blocked,
            Consumer<Runnable> afterFirstFrame, BooleanSupplier sceneIsCurrent,
            BiConsumer<Runnable, Long> schedule, Runnable action) {
        Objects.requireNonNull(afterFirstFrame, "afterFirstFrame");
        Objects.requireNonNull(sceneIsCurrent, "sceneIsCurrent");
        Objects.requireNonNull(schedule, "schedule");
        Objects.requireNonNull(action, "action");
        if (!automatedTurn || blocked || !sceneIsCurrent.getAsBoolean()) return;
        afterFirstFrame.accept(() -> {
            if (!sceneIsCurrent.getAsBoolean()) return;
            schedule.accept(() -> {
                if (sceneIsCurrent.getAsBoolean()) action.run();
            }, ACTION_DELAY_MILLIS);
        });
    }
}
