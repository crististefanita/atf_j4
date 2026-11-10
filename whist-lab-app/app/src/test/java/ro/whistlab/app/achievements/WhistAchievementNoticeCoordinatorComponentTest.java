package ro.whistlab.app.achievements;

import static atf.gradle.audit.assertions.GradleAssertions.*;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import ro.whistlab.app.achievements.notice.WhistAchievementNoticeCoordinator;

public final class WhistAchievementNoticeCoordinatorComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void dismissedNoticeIsSingleFlightSuppressedUntilRestartAndClearedOnlyByAcknowledgement()
            throws Exception {
        var storage = storageWithPending("first_exact", "exact_one_card");
        var repository = new WhistAchievementRepository(storage);
        var current = new WhistAchievementNoticeCoordinator(repository);
        var notice = current.openNext(3, false).orElseThrow();
        var secondOpen = current.openNext(3, false);

        atf.gradle.audit.GradleInputs.fixture("Notice lifecycle inputs before first assertion", "notice", notice,
                "secondOpen", secondOpen, "repository", repository, "storage", storage, "current", current);
        assertThrows("notice consumers cannot mutate the pending goal order", UnsupportedOperationException.class,
                () -> notice.goals().clear());
        assertTrue("one visible notice blocks a duplicate dialog", secondOpen.isEmpty());
        current.dismiss(notice);
        assertTrue("an unacknowledged notice is suppressed in the same activity", current.openNext(3, false).isEmpty());
        assertEquals("dismissal does not erase durable pending goals", List.of("first_exact", "exact_one_card"),
                repository.pendingNotifications().stream().map(WhistAchievementCatalog.Goal::id).toList());

        var restarted = new WhistAchievementNoticeCoordinator(repository);
        var replayed = restarted.openNext(3, false).orElseThrow();
        restarted.acknowledge(replayed);
        assertTrue("acknowledgement does not dismiss the still-visible notice", restarted.openNext(3, false).isEmpty());
        restarted.dismiss(replayed);
        assertTrue("confirmed acknowledgement removes the durable notice", repository.pendingNotifications().isEmpty());
        assertTrue("a confirmed notice does not return after a second restart",
                new WhistAchievementNoticeCoordinator(repository).openNext(3, false).isEmpty());
    }

    @Test public void failedAcknowledgementKeepsTheNoticeDurableButDoesNotRepeatInTheCurrentProcess() throws Exception {
        var storage = storageWithPending("first_exact");
        var repository = new WhistAchievementRepository(storage);
        var current = new WhistAchievementNoticeCoordinator(repository);
        var notice = current.openNext(1, false).orElseThrow();
        storage.failure = new IOException("Notice state disk full");

        atf.gradle.audit.GradleInputs.fixture("Failed acknowledgement inputs before first assertion", "notice", notice,
                "repository", repository, "storage", storage, "current", current);
        assertThrows("storage failure remains visible", IOException.class, () -> current.acknowledge(notice));
        current.dismiss(notice);
        assertTrue("the current process suppresses the dismissed failed write",
                current.openNext(1, false).isEmpty());
        assertEquals("failed acknowledgement leaves the saved goal pending", List.of("first_exact"),
                repository.pendingNotifications().stream().map(WhistAchievementCatalog.Goal::id).toList());
        assertTrue("a new process can retry the durable notice",
                new WhistAchievementNoticeCoordinator(repository).openNext(1, false).isPresent());
    }

    @Test public void staleDismissCallbackCannotCloseOrAcknowledgeANewerNotice() throws Exception {
        var repository = new WhistAchievementRepository(storageWithPending("first_exact"));
        var firstProcess = new WhistAchievementNoticeCoordinator(repository);
        var oldNotice = firstProcess.openNext(1, false).orElseThrow();
        firstProcess.dismiss(oldNotice);

        var restartedProcess = new WhistAchievementNoticeCoordinator(repository);
        var currentNotice = restartedProcess.openNext(2, false).orElseThrow();
        restartedProcess.dismiss(oldNotice);

        atf.gradle.audit.GradleInputs.fixture("Stale callback inputs before first assertion",
                "oldNotice", oldNotice, "currentNotice", currentNotice, "repository", repository);
        assertThrows("stale acknowledgement is rejected", IllegalStateException.class,
                () -> restartedProcess.acknowledge(oldNotice));
        assertTrue("stale dismissal cannot release the current single-flight notice",
                restartedProcess.openNext(2, false).isEmpty());
        assertEquals("stale callbacks cannot clear the durable notification", List.of("first_exact"),
                repository.pendingNotifications().stream().map(WhistAchievementCatalog.Goal::id).toList());

        restartedProcess.acknowledge(currentNotice);
        restartedProcess.dismiss(currentNotice);
        assertTrue("the current notice can still be acknowledged and closed", repository.pendingNotifications().isEmpty());
    }

    private static MemoryStorage storageWithPending(String... ids) throws org.json.JSONException {
        var progress = new java.util.LinkedHashMap<String, Integer>();
        var pending = new LinkedHashSet<String>();
        for (String id : ids) {
            progress.put(id, WhistAchievementCatalog.goal(id).target());
            pending.add(id);
        }
        var state = new WhistAchievementState(progress, Set.of(), Set.of(), pending);
        var storage = new MemoryStorage();
        storage.value = WhistAchievementCodec.encode(state);
        return storage;
    }

    private static final class MemoryStorage implements WhistAchievementRepository.Storage {
        private String value;
        private IOException failure;

        @Override public String read() { return value; }

        @Override public void write(String json) throws IOException {
            if (failure != null) throw failure;
            value = json;
        }
    }
}
