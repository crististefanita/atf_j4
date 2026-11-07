package ro.whistlab.app.screens;

import java.util.Objects;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;

/** Retains setup choices across rebuilding or translating a screen; it never changes a running match. */
public final class MatchSetup {
    private int playerCount = 4;
    private GameMode mode = GameMode.OH_HELL;
    private AiDifficulty difficulty = AiDifficulty.NORMAL;
    private boolean streaks = true;
    private int streakBonus = 10;
    private boolean allowZeroGame = true;

    public int playerCount() { return playerCount; }
    public GameMode mode() { return mode; }
    public AiDifficulty difficulty() { return difficulty; }
    public boolean streaks() { return streaks; }
    public int streakBonus() { return streakBonus; }
    public boolean allowZeroGame() { return allowZeroGame; }

    public void playerCount(int value) {
        if (value < 3 || value > 6) throw new IllegalArgumentException("Expected 3–6 players");
        playerCount = value;
    }
    public void mode(GameMode value) { mode = Objects.requireNonNull(value); }
    public void difficulty(AiDifficulty value) { difficulty = Objects.requireNonNull(value); }
    public void streaks(boolean value) { streaks = value; }
    public void allowZeroGame(boolean value) { allowZeroGame = value; }
    public void streakBonus(int value) {
        if (value != 5 && value != 10) throw new IllegalArgumentException("Expected a 5 or 10 point streak bonus");
        streakBonus = value;
    }

    public GameConfig config(long seed) {
        return GameConfig.newMatch(playerCount, mode, difficulty, seed, mode.isRomanian() && streaks,
                streakBonus, mode.isRomanian() && allowZeroGame);
    }
}
