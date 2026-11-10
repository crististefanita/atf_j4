package ro.whistlab.app.achievements.notice;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import ro.whistlab.app.achievements.WhistAchievementRepository;
import ro.whistlab.app.achievements.WhistAchievementState;
import ro.whistlab.app.achievements.WhistAchievementCatalog.Goal;

/** Coordinates one visible notice, durable acknowledgement, and same-process dismissal suppression. */
public final class WhistAchievementNoticeCoordinator {
    public record Notice(List<Goal> goals, WhistAchievementState state, int handNumber, boolean matchComplete) {
        public Notice {
            goals = List.copyOf(goals);
            Objects.requireNonNull(state);
            if (goals.isEmpty() || handNumber < 1) throw new IllegalArgumentException("Invalid achievement notice");
        }
    }

    private final WhistAchievementRepository repository;
    private final Set<String> suppressedUntilRestart = new HashSet<>();
    private Notice active;
    private boolean acknowledged;

    public WhistAchievementNoticeCoordinator(WhistAchievementRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public synchronized Optional<Notice> openNext(int handNumber, boolean matchComplete) throws IOException {
        if (active != null) return Optional.empty();
        WhistAchievementState state = repository.state();
        List<Goal> visible = state.pendingGoals().stream()
                .filter(goal -> !suppressedUntilRestart.contains(goal.id())).collect(Collectors.toList());
        if (visible.isEmpty()) return Optional.empty();
        active = new Notice(visible, state, handNumber, matchComplete);
        acknowledged = false;
        return Optional.of(active);
    }

    public synchronized void acknowledge(Notice notice) throws IOException {
        requireActive(notice);
        repository.acknowledgeNotifications(notice.goals().stream().map(Goal::id).collect(Collectors.toList()));
        acknowledged = true;
    }

    /** A dismissed but unacknowledged notice remains durable and returns after process restart. */
    public synchronized void dismiss(Notice notice) {
        if (notice == null || active != notice) return;
        if (!acknowledged) notice.goals().forEach(goal -> suppressedUntilRestart.add(goal.id()));
        active = null;
        acknowledged = false;
    }

    private void requireActive(Notice notice) {
        if (notice == null || active != notice) throw new IllegalStateException("Achievement notice is not active");
    }
}
