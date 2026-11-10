package ro.whistlab.app.achievements;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ro.whistlab.app.achievements.WhistAchievementCatalog.Aggregation;
import ro.whistlab.app.achievements.WhistAchievementCatalog.Metric;

/** Immutable career progress, replay guards, and durable achievement notices awaiting dismissal. */
public record WhistAchievementState(Map<String, Integer> progress, Set<String> processedMatches,
        Set<String> processedHands, Set<String> pendingNotifications) {
    private static final Set<Metric> HAND_METRICS = Set.of(Metric.EXACT_CONTRACTS, Metric.EXACT_ONE_CARD,
            Metric.EXACT_ZERO_LARGE, Metric.POSITIVE_SERIES_AWARDS, Metric.EXACT_STREAK);

    public WhistAchievementState {
        if (progress == null || processedMatches == null || processedHands == null || pendingNotifications == null)
            throw new IllegalArgumentException("Missing achievement state");
        var safeProgress = new LinkedHashMap<String, Integer>();
        progress.forEach((id, value) -> {
            WhistAchievementCatalog.goal(id);
            if (value == null || value < 0) throw new IllegalArgumentException("Invalid achievement progress: " + id);
            if (value > 0) safeProgress.put(id, value);
        });
        progress = Collections.unmodifiableMap(safeProgress);
        processedMatches = immutableIds(processedMatches, 128, "match");
        processedHands = immutableIds(processedHands, 256, "hand");
        pendingNotifications = immutableAchievementIds(pendingNotifications);
    }

    /** Compatibility constructor for schema-v1 states and focused state tests. */
    public WhistAchievementState(Map<String, Integer> progress, Set<String> processedMatches) {
        this(progress, processedMatches, Set.of(), Set.of());
    }

    public static WhistAchievementState empty() {
        return new WhistAchievementState(Map.of(), Set.of(), Set.of(), Set.of());
    }

    public int progress(String goalId) { return progress.getOrDefault(goalId, 0); }
    public boolean earned(WhistAchievementCatalog.Goal goal) { return progress(goal.id()) >= goal.target(); }
    public int earnedCount() { return (int) WhistAchievementCatalog.goals().stream().filter(this::earned).count(); }
    public List<WhistAchievementCatalog.Goal> pendingGoals() {
        return pendingNotifications.stream().map(WhistAchievementCatalog::goal)
                .collect(java.util.stream.Collectors.toList());
    }
    public int earnedInGroup(int group) {
        if (group < 1 || group > WhistAchievementCatalog.groups().size())
            throw new IllegalArgumentException("Unknown achievement group: " + group);
        return (int) WhistAchievementCatalog.groups().get(group - 1).goals().stream().filter(this::earned).count();
    }
    /** The next group is guidance only; all goals remain visible and accrue from the first match. */
    public int recommendedGroup() {
        for (var group : WhistAchievementCatalog.groups()) if (earnedInGroup(group.number()) == 0) return group.number();
        return 0;
    }

    WhistAchievementState recordHand(String eventId, WhistHandFacts facts) {
        validateIdentity(eventId, 256, "hand");
        if (processedHands.contains(eventId)) return this;
        var nextProgress = new LinkedHashMap<>(progress);
        var nextPending = new LinkedHashSet<>(pendingNotifications);
        for (var goal : WhistAchievementCatalog.goals()) {
            int evidence = facts.measure(goal.metric());
            if (evidence > 0) apply(goal, evidence, nextProgress, nextPending);
        }
        var nextHands = new LinkedHashSet<>(processedHands);
        nextHands.add(eventId);
        return new WhistAchievementState(nextProgress, processedMatches, nextHands, nextPending);
    }

    WhistAchievementState recordMatch(String matchId, WhistMatchFacts facts) {
        validateIdentity(matchId, 128, "match");
        if (facts == null || processedMatches.contains(matchId)) return this;
        var nextProgress = new LinkedHashMap<>(progress);
        var nextPending = new LinkedHashSet<>(pendingNotifications);
        for (var goal : WhistAchievementCatalog.goals()) {
            if (HAND_METRICS.contains(goal.metric())) continue;
            int evidence = facts.measure(goal.metric());
            if (evidence > 0) apply(goal, evidence, nextProgress, nextPending);
        }
        var nextMatches = new LinkedHashSet<>(processedMatches);
        nextMatches.add(matchId);
        return new WhistAchievementState(nextProgress, nextMatches, processedHands, nextPending);
    }

    WhistAchievementState acknowledgeNotifications(List<String> ids) {
        if (ids == null || ids.isEmpty()) return this;
        var nextPending = new LinkedHashSet<>(pendingNotifications);
        ids.forEach(id -> {
            WhistAchievementCatalog.goal(id);
            nextPending.remove(id);
        });
        return nextPending.equals(pendingNotifications) ? this
                : new WhistAchievementState(progress, processedMatches, processedHands, nextPending);
    }

    private static void apply(WhistAchievementCatalog.Goal goal, int evidence,
            Map<String, Integer> progress, Set<String> pending) {
        int before = progress.getOrDefault(goal.id(), 0);
        int after = goal.aggregation() == Aggregation.BEST_MATCH
                ? Math.max(before, evidence) : Math.addExact(before, evidence);
        progress.put(goal.id(), after);
        if (before < goal.target() && after >= goal.target()) pending.add(goal.id());
    }

    private static Set<String> immutableIds(Set<String> values, int maxLength, String kind) {
        var copy = new LinkedHashSet<String>();
        for (String id : values) {
            validateIdentity(id, maxLength, kind);
            if (!copy.add(id)) throw new IllegalArgumentException("Duplicate processed " + kind + " identity");
        }
        return Collections.unmodifiableSet(copy);
    }

    private static Set<String> immutableAchievementIds(Set<String> values) {
        var copy = new LinkedHashSet<String>();
        for (String id : values) {
            WhistAchievementCatalog.goal(id);
            if (!copy.add(id)) throw new IllegalArgumentException("Duplicate pending achievement: " + id);
        }
        return Collections.unmodifiableSet(copy);
    }

    private static void validateIdentity(String id, int maxLength, String kind) {
        if (id == null || id.isBlank() || id.length() > maxLength)
            throw new IllegalArgumentException("Invalid processed " + kind + " identity");
    }
}
