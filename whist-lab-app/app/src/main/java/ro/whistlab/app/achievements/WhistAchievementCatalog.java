package ro.whistlab.app.achievements;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Catalogul stabil al realizărilor Whist; nivelul descrie dificultatea obiectivului, nu AI-ul. */
public final class WhistAchievementCatalog {
    public enum Level { EASY, MEDIUM, HARD }
    public enum Aggregation { SUM, BEST_MATCH }
    public enum Metric {
        COMPLETED_MATCHES, EXACT_CONTRACTS, MATCH_WINS,
        EXACT_ONE_CARD, EXACT_ZERO_LARGE, EXACT_SMALL_RATE, EXACT_MIDDLE_RATE, EXACT_LARGE_RATE,
        EXACT_STREAK, EXACT_MATCH_RATE, EXACT_HAND_SIZE_COUNT, EXACT_FINAL_HAND,
        EXACT_FINAL_THREE, EXACT_FINAL_FIVE,
        SERIES_ENABLED_MATCHES, POSITIVE_SERIES_AWARDS, POSITIVE_SERIES_MATCHES,
        WIN_WITH_SERIES_AWARD,
        SHORT_ROMANIAN_MATCHES, LONG_ROMANIAN_MATCHES,
        SHORT_ROMANIAN_WINS, LONG_ROMANIAN_WINS, ZERO_GAME_MATCHES,
        PLAYER_COUNT_THREE, PLAYER_COUNT_SIX, TOP_TWO_AT_FIVE, TOP_TWO_AT_SIX, WINS_AT_SIX,
        HOLD_OR_IMPROVE_RANK, TOP_THREE_AT_FOUR, TOP_TWO_AT_FOUR, CLIMB_ONE_RANK, LAST_TO_TOP_TWO,
        EXACT_ZERO_MULTICARD, EXACT_ZERO_SHORT, EXACT_ZERO_MIDDLE, EXACT_ZERO_EIGHT, EXACT_ZERO_BANDS,
        EXACT_ONE_TRICK, EXACT_TWO_PLUS, EXACT_THREE_PLUS, EXACT_POSITIVE_LARGE, EXACT_POSITIVE_RATE,
        RECOVERY_AFTER_MISS, RECOVERY_AFTER_TWO_MISSES, RECOVERY_TWO_EXACT,
        RECOVERY_BOTH_MISS_TYPES, RECOVERY_THREE_EXACT;

        boolean isHandMetric() {
            return switch (this) {
                case EXACT_CONTRACTS, EXACT_ONE_CARD, EXACT_ZERO_LARGE, POSITIVE_SERIES_AWARDS, EXACT_STREAK,
                        EXACT_ZERO_MULTICARD, EXACT_ZERO_SHORT, EXACT_ZERO_MIDDLE, EXACT_ZERO_EIGHT,
                        EXACT_ONE_TRICK, EXACT_TWO_PLUS, EXACT_THREE_PLUS, EXACT_POSITIVE_LARGE -> true;
                default -> false;
            };
        }
    }

    public record Goal(String id, int group, Level level, int target, String titleRo, String titleEn,
            String descriptionRo, String descriptionEn, Metric metric, Aggregation aggregation) {
        public Goal {
            if (id == null || !id.matches("[a-z][a-z0-9_]*") || group < 1 || target < 1)
                throw new IllegalArgumentException("Invalid achievement definition");
            java.util.Objects.requireNonNull(level);
            java.util.Objects.requireNonNull(metric);
            java.util.Objects.requireNonNull(aggregation);
            if (titleRo == null || titleRo.isBlank() || titleEn == null || titleEn.isBlank()
                    || descriptionRo == null || descriptionRo.isBlank()
                    || descriptionEn == null || descriptionEn.isBlank())
                throw new IllegalArgumentException("Achievements require Romanian and English copy");
        }
        public String title(boolean romanian) { return romanian ? titleRo : titleEn; }
        public String description(boolean romanian) { return romanian ? descriptionRo : descriptionEn; }
    }

    public record Group(int number, String titleRo, String titleEn, List<Goal> goals) {
        public Group { goals = List.copyOf(goals); }
        public String title(boolean romanian) { return romanian ? titleRo : titleEn; }
    }

    private static final List<Goal> GOALS = WhistAchievementCatalogData.goals();
    private static final List<Group> GROUPS = WhistAchievementCatalogData.groups(GOALS);
    private static final Map<String, Goal> BY_ID = GOALS.stream().collect(Collectors.toUnmodifiableMap(Goal::id, goal -> goal));

    private WhistAchievementCatalog() { }
    public static List<Goal> goals() { return GOALS; }
    public static List<Group> groups() { return GROUPS; }
    public static Goal goal(String id) {
        Goal goal = BY_ID.get(id);
        if (goal == null) throw new IllegalArgumentException("Unknown achievement: " + id);
        return goal;
    }
}
