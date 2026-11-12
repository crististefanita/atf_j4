package ro.whistlab.app.achievements;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import ro.whistlab.core.session.GameSession;

/** Persistă progresul local pe mâini complete și finalizează obiectivele de partidă o singură dată. */
public final class WhistAchievementRepository {
    public interface Storage {
        String read() throws IOException;
        void write(String json) throws IOException;
    }

    private final Storage storage;
    public WhistAchievementRepository(Storage storage) { this.storage = Objects.requireNonNull(storage); }

    public synchronized WhistAchievementState state() throws IOException { return load(); }

    public synchronized boolean record(String matchId, GameSession game) throws IOException {
        if (matchId == null || matchId.isBlank() || matchId.length() > 128)
            throw new IllegalArgumentException("Invalid match identity");
        List<WhistHandFacts> hands = WhistHandFacts.from(game);
        var matchFacts = WhistMatchFacts.from(game, hands);
        if (hands.isEmpty() && matchFacts.isEmpty()) return false;
        WhistAchievementState before = load();
        WhistAchievementState after = before;
        for (WhistHandFacts hand : hands)
            after = after.recordHand(hand.eventId(matchId), hand);
        if (matchFacts.isPresent()) after = after.recordMatch(matchId, matchFacts.get());
        if (after == before) return false;
        try {
            storage.write(WhistAchievementCodec.encode(after));
        } catch (org.json.JSONException | IllegalStateException failure) {
            throw new IOException("Could not encode Whist achievements", failure);
        }
        return true;
    }

    public synchronized List<WhistAchievementCatalog.Goal> pendingNotifications() throws IOException {
        return load().pendingGoals();
    }

    public synchronized void acknowledgeNotifications(List<String> ids) throws IOException {
        WhistAchievementState before = load();
        WhistAchievementState after = before.acknowledgeNotifications(ids);
        if (after == before) return;
        try {
            storage.write(WhistAchievementCodec.encode(after));
        } catch (org.json.JSONException | IllegalStateException failure) {
            throw new IOException("Could not acknowledge Whist achievement notices", failure);
        }
    }

    private WhistAchievementState load() throws IOException {
        String encoded = storage.read();
        if (encoded == null) return WhistAchievementState.empty();
        try {
            return WhistAchievementCodec.decode(encoded);
        } catch (org.json.JSONException | IllegalArgumentException failure) {
            throw new IOException("Saved Whist achievements are invalid", failure);
        }
    }
}
