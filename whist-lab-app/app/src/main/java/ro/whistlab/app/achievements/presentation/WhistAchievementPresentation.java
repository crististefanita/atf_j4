package ro.whistlab.app.achievements.presentation;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import ro.whistlab.app.achievements.WhistAchievementState;
import ro.whistlab.app.achievements.WhistAchievementCatalog;
import ro.whistlab.app.achievements.WhistAchievementCatalog.Goal;
import ro.whistlab.app.achievements.WhistAchievementCatalog.Group;
import ro.whistlab.app.localization.WhistUiText;

/** Builds the platform-independent text and ordering consumed by achievement dialogs. */
public final class WhistAchievementPresentation {
    public record GoalRow(String id, String marker, String level, String title, String progress,
            String description, boolean complete) { }

    public record GroupSection(int number, String heading, boolean recommended, List<GoalRow> goals) {
        public GroupSection { goals = List.copyOf(goals); }
    }

    public record Catalogue(String summary, List<GroupSection> groups) {
        public Catalogue { groups = List.copyOf(groups); }
    }

    public record Notice(String title, String summary, List<GoalRow> goals) {
        public Notice { goals = List.copyOf(goals); }
    }

    private WhistAchievementPresentation() { }

    public static Catalogue catalogue(WhistAchievementState state, WhistUiText copy) {
        Objects.requireNonNull(state);
        Objects.requireNonNull(copy);
        int recommended = state.recommendedGroup();
        var groups = WhistAchievementCatalog.groups().stream()
                .map(group -> section(group, state, copy, recommended))
                .collect(Collectors.toList());
        return new Catalogue(copy.achievementsSummary(state.earnedCount(), WhistAchievementCatalog.goals().size(),
                recommended), groups);
    }

    public static Notice notice(List<Goal> unlocked, WhistAchievementState state, int handNumber,
            boolean matchComplete, WhistUiText copy) {
        Objects.requireNonNull(unlocked);
        Objects.requireNonNull(state);
        Objects.requireNonNull(copy);
        if (unlocked.isEmpty() || handNumber < 1) throw new IllegalArgumentException("Invalid achievement notice");

        Set<String> selectedIds = new HashSet<>();
        for (Goal goal : unlocked) {
            if (goal == null || !selectedIds.add(WhistAchievementCatalog.goal(goal.id()).id()))
                throw new IllegalArgumentException("Achievement notice contains an invalid or duplicate goal");
        }
        List<Goal> ordered = WhistAchievementCatalog.goals().stream()
                .filter(goal -> selectedIds.contains(goal.id())).collect(Collectors.toList());
        if (ordered.size() != selectedIds.size()) throw new IllegalArgumentException("Unknown achievement notice goal");

        List<GoalRow> rows = ordered.stream().map(goal -> row(goal, state, copy, "★"))
                .collect(Collectors.toList());
        return new Notice(copy.achievementsUnlocked(rows.size()),
                copy.achievementNoticeSummary(rows.size(), state.earnedCount(), WhistAchievementCatalog.goals().size(),
                        handNumber, matchComplete, state.recommendedGroup()), rows);
    }

    private static GroupSection section(Group group, WhistAchievementState state, WhistUiText copy,
            int recommendedGroup) {
        int earned = state.earnedInGroup(group.number());
        boolean recommended = group.number() == recommendedGroup;
        String heading = group.number() + ". " + group.title(copy.romanian()) + " · "
                + copy.achievementGroupCount(earned, group.goals().size())
                + (recommended ? " · " + copy.recommended() : "");
        List<GoalRow> goals = group.goals().stream()
                .map(goal -> row(goal, state, copy, state.earned(goal) ? "✓" : "○"))
                .collect(Collectors.toList());
        return new GroupSection(group.number(), heading, recommended, goals);
    }

    private static GoalRow row(Goal goal, WhistAchievementState state, WhistUiText copy, String marker) {
        int progress = Math.min(state.progress(goal.id()), goal.target());
        boolean percentage = switch (goal.metric()) {
            case EXACT_SMALL_RATE, EXACT_MIDDLE_RATE, EXACT_LARGE_RATE, EXACT_MATCH_RATE -> true;
            default -> false;
        };
        String measure = percentage ? copy.achievementPercent(progress, goal.target())
                : copy.achievementProgress(progress, goal.target());
        return new GoalRow(goal.id(), marker, copy.achievementLevel(goal.level()), goal.title(copy.romanian()),
                measure, goal.description(copy.romanian()), state.earned(goal));
    }
}
