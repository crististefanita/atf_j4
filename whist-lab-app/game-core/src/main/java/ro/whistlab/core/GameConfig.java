package ro.whistlab.core;

import java.util.Objects;

public record GameConfig(int playerCount, GameMode mode, AiDifficulty aiDifficulty, long seed) {
    public GameConfig {
        if (playerCount < 3 || playerCount > 6) {
            throw new IllegalArgumentException("Player count must be between 3 and 6");
        }
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(aiDifficulty, "aiDifficulty");
    }

    public static GameConfig defaults(long seed) {
        return new GameConfig(4, GameMode.OH_HELL, AiDifficulty.NORMAL, seed);
    }
}
