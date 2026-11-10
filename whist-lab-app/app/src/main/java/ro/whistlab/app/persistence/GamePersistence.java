package ro.whistlab.app.persistence;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

/** Owner-thread persistence; completed-hand progress, ranking and replay keep separate failure boundaries. */
public final class GamePersistence {
    @FunctionalInterface
    public interface Checkpoints {
        void save(GameSession game, boolean rankingEligible, String matchId) throws IOException;
    }

    @FunctionalInterface
    public interface Rankings {
        void record(String matchId, GameSession game, long now) throws IOException;
    }

    @FunctionalInterface
    public interface Completions {
        void record(String matchId, GameSession game) throws IOException;
    }

    private final Checkpoints checkpoints;
    private final Rankings rankings;
    private final Completions completions;
    private final LongSupplier clock;
    private final Consumer<IOException> storageError;

    public GamePersistence(Checkpoints checkpoints, Rankings rankings, LongSupplier clock,
                           Consumer<IOException> storageError) {
        this(checkpoints, rankings, (matchId, game) -> { }, clock, storageError);
    }

    public GamePersistence(Checkpoints checkpoints, Rankings rankings, Completions completions,
                           LongSupplier clock, Consumer<IOException> storageError) {
        this.checkpoints = Objects.requireNonNull(checkpoints);
        this.rankings = Objects.requireNonNull(rankings);
        this.completions = Objects.requireNonNull(completions);
        this.clock = Objects.requireNonNull(clock);
        this.storageError = Objects.requireNonNull(storageError);
    }

    public void save(GameSession game, boolean rankingEligible, String matchId,
                     Consumer<GameSession> exportReplay) {
        Objects.requireNonNull(exportReplay);
        if (game == null) return;
        try {
            checkpoints.save(game, rankingEligible, matchId);
        } catch (IOException failure) {
            storageError.accept(failure);
            return;
        }
        recordProgressAndFinishedMatch(game, rankingEligible, matchId);
        exportReplay.accept(game);
    }

    private void recordProgressAndFinishedMatch(GameSession game, boolean rankingEligible, String matchId) {
        if (!rankingEligible) return;
        var state = game.snapshot();
        if (game.scoreHistory().completedHands() > 0) {
            try {
                completions.record(matchId, game);
            } catch (IOException failure) {
                storageError.accept(failure);
            }
        }
        if (state.phase() != GamePhase.SCORE || !state.finalHand()) return;
        try {
            rankings.record(matchId, game, clock.getAsLong());
        } catch (IOException failure) {
            storageError.accept(failure);
        }
    }
}
