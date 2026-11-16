package ro.whistlab.app.profile;

import java.util.function.Supplier;
import ro.whistlab.core.player.PlayerNames;
import ro.whistlab.core.session.GameSession;

/** Keeps a dialog's save callback valid only while that edit is open. */
public final class PlayerProfileEdit {
    private final PlayerPreferences preferences;
    private final Supplier<GameSession> activeGame;
    private final Runnable afterSave;
    private boolean dismissed;

    public PlayerProfileEdit(PlayerPreferences preferences, Supplier<GameSession> activeGame, Runnable afterSave) {
        this.preferences = preferences;
        this.activeGame = activeGame;
        this.afterSave = afterSave;
    }

    public String currentName() { return preferences.name(); }

    public void save(String value) {
        if (dismissed) return;
        GameSession game = activeGame.get();
        String sessionName = value.isBlank() ? PlayerNames.DEFAULT_HUMAN : value;
        if (game != null) game.validateHumanName(sessionName);
        preferences.saveName(value);
        if (game != null) game.renameHuman(sessionName);
        afterSave.run();
    }

    public void dismiss() { dismissed = true; }
}
