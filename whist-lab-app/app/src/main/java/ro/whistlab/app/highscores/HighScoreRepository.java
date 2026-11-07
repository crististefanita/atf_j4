package ro.whistlab.app.highscores;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

/** Commits each completed match once. A failed write remains retryable. */
public final class HighScoreRepository {
    public interface Storage {
        String read() throws IOException;
        void write(String json) throws IOException;
    }
    private final Storage storage;
    private final Random random;

    public HighScoreRepository(Storage storage, Random random) {
        this.storage = java.util.Objects.requireNonNull(storage);
        this.random = java.util.Objects.requireNonNull(random);
    }

    public synchronized List<HighScores.Board> boards() throws IOException {
        return List.copyOf(load().boards.values());
    }

    public synchronized HighScores.Board board(HighScores.Profile profile) throws IOException {
        HighScores data = load();
        boolean exists = data.boards.containsKey(profile.key());
        var board = data.ensure(profile, random);
        if (!exists) storage.write(HighScoreCodec.encode(data));
        return board;
    }

    public synchronized boolean record(GameSession game, long now) throws IOException {
        return record(Long.toUnsignedString(game.config().seed()), game, now);
    }

    public synchronized boolean record(String id, GameSession game, long now) throws IOException {
        if (id == null || id.isBlank() || id.length() > 128) throw new IllegalArgumentException("Invalid match identity");
        var state = game.snapshot();
        if (state.phase() != GamePhase.SCORE || !state.finalHand()) return false;
        // Validate the domain checkpoint, including explicit omissions retained by legacy saves.
        GameSession.restore(game.saveState());
        var profile = HighScores.Profile.from(game.config());
        String matchId = profile.key() + ":" + id;
        var results = new ArrayList<HighScores.Entry>();
        for (var player : state.players()) results.add(new HighScores.Entry(player.name(), state.matchScore(player.seat()),
                !player.human(), false, matchId, player.seat(), now));
        HighScores data = load();
        if (!data.record(profile, matchId, results, random)) return false;
        storage.write(HighScoreCodec.encode(data));
        return true;
    }

    private HighScores load() throws IOException {
        String json = storage.read();
        if (json != null) return HighScoreCodec.decode(json);
        HighScores data = HighScores.initial(random);
        storage.write(HighScoreCodec.encode(data));
        return data;
    }
}
