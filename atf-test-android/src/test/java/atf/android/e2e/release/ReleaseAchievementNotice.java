package atf.android.e2e.release;

import java.util.Set;
import java.util.Optional;

/** Exact localized title and dismissal action for the post-hand achievement notice. */
record ReleaseAchievementNotice(String title, String closeAction) {
    static Optional<ReleaseAchievementNotice> find(Set<String> visibleText) {
        if (visibleText.contains("Achievement unlocked"))
            return Optional.of(new ReleaseAchievementNotice("Achievement unlocked", "CLOSE"));
        if (visibleText.contains("Achievements unlocked"))
            return Optional.of(new ReleaseAchievementNotice("Achievements unlocked", "CLOSE"));
        if (visibleText.contains("Realizare nouă"))
            return Optional.of(new ReleaseAchievementNotice("Realizare nouă", "ÎNCHIDE"));
        if (visibleText.contains("Realizări noi"))
            return Optional.of(new ReleaseAchievementNotice("Realizări noi", "ÎNCHIDE"));
        return Optional.empty();
    }
}
