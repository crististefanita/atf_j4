package ro.whistlab.core.config;

import ro.whistlab.core.rules.GameMode;

import java.util.Objects;

public record GameConfig(int playerCount, GameMode mode, AiDifficulty aiDifficulty, long seed,
        boolean streakBonuses, int streakBonusPoints, int streakPenaltyPoints, boolean allowZeroGame) {
    /** Historical profiles and benchmarks keep their original scoring. */
    public GameConfig(int playerCount, GameMode mode, AiDifficulty aiDifficulty, long seed,
            boolean streakBonuses, int streakBonusPoints, int streakPenaltyPoints) {
        this(playerCount, mode, aiDifficulty, seed, streakBonuses, streakBonusPoints, streakPenaltyPoints, false);
    }

    /** Compatibility for existing training protocols that declared only the positive reward. */
    public GameConfig(int playerCount, GameMode mode, AiDifficulty aiDifficulty, long seed,
            boolean streakBonuses, int streakBonusPoints) {
        this(playerCount, mode, aiDifficulty, seed, streakBonuses, streakBonusPoints, 5);
    }
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
        if (allowZeroGame && !mode.isRomanian()) {
            throw new IllegalArgumentException("Playing for zero applies only to Romanian Whist");
        }
        if (streakBonusPoints != 5 && streakBonusPoints != 10) {
            throw new IllegalArgumentException("The positive five-hand bonus must be 5 or 10 points");
        }
        if (streakPenaltyPoints != 5 && streakPenaltyPoints != 10) {
            throw new IllegalArgumentException("The five-hand penalty must be 5 or 10 points");
        }
    }

    public static GameConfig defaults(long seed) {
        return new GameConfig(4, GameMode.OH_HELL, AiDifficulty.NORMAL, seed);
    }

    /** Product configuration for a new match: equal five-hand reward and penalty. */
    public static GameConfig newMatch(int players, GameMode mode, AiDifficulty difficulty, long seed,
            boolean series, int points) {
        return newMatch(players, mode, difficulty, seed, series, points, mode.isRomanian());
    }

    public static GameConfig newMatch(int players, GameMode mode, AiDifficulty difficulty, long seed,
            boolean series, int points, boolean allowZeroGame) {
        return new GameConfig(players, mode, difficulty, seed, series, points, points, allowZeroGame);
    }
}
