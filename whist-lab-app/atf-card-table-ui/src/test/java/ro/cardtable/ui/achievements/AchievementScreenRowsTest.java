package ro.cardtable.ui.achievements;

import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;
import static atf.gradle.audit.assertions.GradleAssertions.assertThrows;

public class AchievementScreenRowsTest extends GradleAuditedTest {
    @Test public void keepsCatalogOrderAndDefersEligibilityUntilOpened() {
        var reads = new AtomicInteger();
        var heading = AchievementScreen.Row.heading(-1, "Getting started");
        var goal = new AchievementScreen.Row(6, "first", "First match", "0/1", false, () -> {
            reads.incrementAndGet();
            return new AchievementScreen.Detail("0/1", List.of());
        });
        var source = new ArrayList<>(List.of(heading, goal));
        GradleInputs.fixture("Ordered public rows", "heading", heading.title(), "goal ID", goal.key());
        var rows = AchievementScreen.checkedRows(source);
        source.clear();
        assertEquals("Snapshot preserves row order", List.of(heading, goal), rows);
        assertEquals("List construction does not evaluate eligibility", 0, reads.get());
        assertEquals("Opening the row exposes its progress", "0/1", rows.get(1).detail().get().progress());
        assertEquals("Only the opened row evaluates its detail", 1, reads.get());
    }

    @Test public void rejectsAmbiguousRowsBeforeListRecyclingOrRestoration() {
        var first = goal(0, "first");
        GradleInputs.fixture("Ambiguous row identities", "duplicate numeric ID", 0, "duplicate key", "first");
        assertThrows("Reject duplicate adapter IDs", IllegalArgumentException.class,
                () -> AchievementScreen.checkedRows(List.of(first, goal(0, "second"))));
        assertThrows("Reject duplicate restoration keys", IllegalArgumentException.class,
                () -> AchievementScreen.checkedRows(List.of(first, goal(1, "first"))));
        assertThrows("Reject Android's invalid ID sentinel", IllegalArgumentException.class,
                () -> AchievementScreen.checkedRows(List.of(goal(Long.MIN_VALUE, "invalid"))));
    }

    private static AchievementScreen.Row goal(long id, String key) {
        return new AchievementScreen.Row(id, key, key, "", false,
                () -> new AchievementScreen.Detail("", List.of()));
    }
}
