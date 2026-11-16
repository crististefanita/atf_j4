package ro.whistlab.app.profile;

import android.content.SharedPreferences;
import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.player.PlayerNames;
import ro.whistlab.core.session.GameSession;

import static atf.gradle.audit.assertions.GradleAssertions.*;

public final class PlayerProfileEditComponentTest extends GradleAuditedTest {
    @Test public void saveCommitsTheCanonicalNameAndAReopenedProfileAndMatchUseIt() {
        var storage = new PreferenceMock("Cristi");
        var game = GameSession.newGame(GameConfig.defaults(71), "Cristi");
        int bidSeat = game.snapshot().biddingSeat();
        game.submitBid(bidSeat, 0);
        var before = game.snapshot();
        var originalHistory = game.scoreHistory();
        var persistedGames = new ArrayList<GameSession.SaveState>();
        var edit = new PlayerProfileEdit(storage.open(), () -> game, () -> persistedGames.add(game.saveState()));
        String entered = "  S\u0326tefan  ";
        GradleInputs.fixture("Profile save and restart input", "initial name", edit.currentName(),
                "draft", entered, "match before save", before, "commit accepted", true);

        edit.save(entered);

        assertEquals("Editing writes exactly one NFC-normalized name", List.of("Ștefan"), storage.attempts);
        var reopened = storage.open();
        assertEquals("An independent preferences adapter reads the saved name", "Ștefan", reopened.name());
        assertEquals("New tables receive the same custom name", "Ștefan", reopened.sessionName());
        assertEquals("The live match is checkpointed once after save", 1, persistedGames.size());
        var restoredGame = GameSession.restoreComplete(persistedGames.get(0));
        var reloaded = restoredGame.snapshot();
        assertEquals("The checkpoint contains the committed human name", "Ștefan", reloaded.players().get(0).name());
        assertEquals("The accepted bid survives the profile change", Integer.valueOf(0), reloaded.players().get(bidSeat).bid());
        assertEquals("Other seats stay intact", before.players().subList(1, 4), reloaded.players().subList(1, 4));
        assertEquals("The profile change preserves score history", originalHistory, restoredGame.scoreHistory());
    }

    @Test public void dismissIgnoresALateSaveAndReopeningShowsTheOriginalProfile() {
        var storage = new PreferenceMock("Cristi");
        var game = GameSession.newGame(GameConfig.defaults(71), "Cristi");
        var before = game.saveState();
        var edit = new PlayerProfileEdit(storage.open(), () -> game, () -> {
            throw new AssertionError("Dismissal must not save the game");
        });
        String draft = "Nume nou";
        GradleInputs.fixture("Cancelled edit with a delayed save action", "saved name", edit.currentName(), "draft", draft);

        edit.dismiss();
        edit.save(draft);

        assertEquals("There are no writes when the edit is dismissed", List.of(), storage.attempts);
        assertEquals("The live session remains identical", before, game.saveState());
        var reopened = new PlayerProfileEdit(storage.open(), () -> game, () -> { });
        assertEquals("The next dialog receives the original name", "Cristi", reopened.currentName());
    }

    @Test public void invalidNameAndRejectedStorageLeaveTheMatchUntouchedAndAllowRetry() {
        var storage = new PreferenceMock("Cristi");
        var game = GameSession.newGame(GameConfig.defaults(71), "Cristi");
        var before = game.saveState();
        var checkpoints = new ArrayList<GameSession.SaveState>();
        var edit = new PlayerProfileEdit(storage.open(), () -> game, () -> checkpoints.add(game.saveState()));
        String duplicate = game.snapshot().players().get(1).name();
        GradleInputs.fixture("Invalid or unpersisted edits before retry", "initial game", before,
                "invalid characters", "!?", "taken name", duplicate, "otherwise valid name", "Ștefan");

        assertThrows("A malformed name is rejected at validation", IllegalArgumentException.class, () -> edit.save("!?"));
        assertThrows("A name in use is rejected before a preference write", IllegalArgumentException.class, () -> edit.save(duplicate));
        assertEquals("No preference attempt precedes full validation", List.of(), storage.attempts);
        storage.rejectCommit = true;
        assertThrows("A refused durable write reaches the dialog as a storage error", IllegalStateException.class,
                () -> edit.save("Ștefan"));
        assertEquals("The previous durable profile is readable after the refused write", "Cristi", storage.open().name());
        assertEquals("A refused write keeps the live match intact", before, game.saveState());
        assertEquals("No match checkpoint is written on rejection", List.of(), checkpoints);

        storage.rejectCommit = false;
        edit.save("Ștefan");

        assertEquals("Retry commits the valid name", "Ștefan", storage.open().name());
        assertEquals("Only the accepted attempt checkpoints the match", 1, checkpoints.size());
    }

    @Test public void resetStoresAnEmptyProfileAndRestoresTheDefaultSeatName() {
        var storage = new PreferenceMock("Cristi");
        var game = GameSession.newGame(GameConfig.defaults(71), "Cristi");
        var edit = new PlayerProfileEdit(storage.open(), () -> game, () -> { });
        GradleInputs.fixture("Reset with an active match", "name before reset", edit.currentName(), "reset payload", "");

        edit.save("");

        assertEquals("Reset commits an explicit empty string", List.of(""), storage.attempts);
        assertEquals("The profile remains empty across reopen", "", storage.open().name());
        assertEquals("A new match uses the default seat name", PlayerNames.DEFAULT_HUMAN, storage.open().sessionName());
        assertEquals("The active human seat is reset too", PlayerNames.DEFAULT_HUMAN, game.snapshot().players().get(0).name());
    }

    private static final class PreferenceMock {
        private final List<String> attempts = new ArrayList<>();
        private String storedName;
        private boolean rejectCommit;

        PreferenceMock(String storedName) { this.storedName = storedName; }

        PlayerPreferences open() {
            return new PlayerPreferences((SharedPreferences) Proxy.newProxyInstance(
                    SharedPreferences.class.getClassLoader(), new Class<?>[]{SharedPreferences.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getString" -> "name".equals(args[0]) ? storedName : args[1];
                        case "edit" -> editor();
                        default -> throw new AssertionError(method);
                    }));
        }

        private SharedPreferences.Editor editor() {
            String[] draft = {null};
            return (SharedPreferences.Editor) Proxy.newProxyInstance(SharedPreferences.Editor.class.getClassLoader(),
                    new Class<?>[]{SharedPreferences.Editor.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "putString" -> {
                            if (!"name".equals(args[0])) throw new AssertionError("Unexpected preference " + args[0]);
                            draft[0] = (String) args[1];
                            yield proxy;
                        }
                        case "commit" -> {
                            attempts.add(draft[0]);
                            if (!rejectCommit) storedName = draft[0];
                            yield !rejectCommit;
                        }
                        default -> throw new AssertionError(method);
                    });
        }
    }
}
