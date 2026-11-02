package ro.whistlab.core;

import java.util.Objects;

public record GameConfig(int playerCount, GameMode mode, AiDifficulty aiDifficulty, long seed,
        boolean streakBonuses, int streakBonusPoints) {
    /** Existing saves and benchmarks use the explicitly retained classic profile. */
    public GameConfig(int playerCount, GameMode mode, AiDifficulty aiDifficulty, long seed) {
        this(playerCount, mode, aiDifficulty, seed, false, 5);
    }
    public GameConfig(int playerCount, GameMode mode, AiDifficulty aiDifficulty, long seed, boolean streakBonuses) {
        this(playerCount,mode,aiDifficulty,seed,streakBonuses,10);
    }
    public GameConfig {
        if (playerCount < 3 || playerCount > 6) {
            throw new IllegalArgumentException("Player count must be between 3 and 6");
        }
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(aiDifficulty, "aiDifficulty");
        if (streakBonuses && !mode.isRomanian()) {
            throw new IllegalArgumentException("Five-hand streaks apply only to Romanian Whist");
        }
        if (streakBonusPoints != 5 && streakBonusPoints != 10) {
            throw new IllegalArgumentException("The positive five-hand bonus must be 5 or 10 points");
        }
    }

    public static GameConfig defaults(long seed) {
        return new GameConfig(4, GameMode.OH_HELL, AiDifficulty.NORMAL, seed);
    }
}
