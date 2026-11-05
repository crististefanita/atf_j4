package ro.whistlab.app.lifecycle;

import java.util.Objects;
import java.util.function.Consumer;

/** Only a domain command can be rejected; later failures never replay that command. */
public final class GameCommandFlow {
    private final Runnable persist;
    private final Runnable advance;

    public GameCommandFlow(Runnable persist, Runnable advance) {
        this.persist = Objects.requireNonNull(persist);
        this.advance = Objects.requireNonNull(advance);
    }

    public boolean execute(Runnable command, Runnable accepted, Consumer<RuntimeException> rejected) {
        Objects.requireNonNull(command);
        Objects.requireNonNull(accepted);
        Objects.requireNonNull(rejected);
        try {
            command.run();
        } catch (IllegalArgumentException | IllegalStateException refusal) {
            rejected.accept(refusal);
            return false;
        }
        accepted.run();
        persist.run();
        advance.run();
        return true;
    }
}
