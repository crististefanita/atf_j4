package ro.whistlab.app.persistence;

import static atf.gradle.audit.assertions.GradleAssertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import ro.whistlab.app.lifecycle.GameCommandFlow;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.session.GameSession;

public class GamePersistenceComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void checkpointReceivesTheSameSessionEligibilityAndIdentityBeforeReplay() {
        var game = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", GameConfig.defaults(31)));
        var events = new ArrayList<String>();
        var persistence = new GamePersistence((saved, eligible, id) -> {

            assertSame("saved keeps the identity of game", game, saved);
            assertFalse("eligible must remain false", eligible);
            assertEquals("id must equal \"fixture-match\"", "fixture-match", id);
            events.add("checkpoint");
        }, (id, saved, time) -> fail("Unfinished match"), () -> { throw new AssertionError("Clock"); },
                error -> fail(error.toString()));
        persistence.save(atf.gradle.audit.GradleInputs.created("Persistence or profile input before save", game), false, "fixture-match", saved -> {
            assertSame("saved keeps the identity of game", game, saved);
            events.add("replay");
        });
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "events", events, "game", game, "persistence", persistence);
        assertEquals("events must equal List.of(\"checkpoint\", \"replay\")", List.of("checkpoint", "replay"), events);
    }

    @Test public void noSessionHasNoPersistenceSideEffects() {
        var events = new ArrayList<String>();
        var persistence = new GamePersistence((game, eligible, id) -> events.add("checkpoint"),
                (id, game, time) -> events.add("ranking"), () -> { events.add("clock"); return 0; },
                error -> events.add("storage-error"));
        persistence.save(atf.gradle.audit.GradleInputs.created("Persistence or profile input before save", null), true, "fixture-match", game -> events.add("replay"));
        assertEquals("A null session invokes none of the persistence collaborators", List.of(), events);
    }

    @Test public void checkpointIoFailureIsReportedOnceAndStopsLaterPersistenceStages() {
        var failure = new IOException("Disk full");
        var failures = new ArrayList<IOException>();
        var persistence = new GamePersistence((game, eligible, id) -> { throw failure; },
                (id, game, time) -> fail("Ranking"), () -> 0, failures::add);
        persistence.save(atf.gradle.audit.GradleInputs.created("Persistence or profile input before save", GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", GameConfig.defaults(31)))), true, "fixture-match",
                game -> fail("Replay"));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "failure", failure, "failures", failures, "persistence", persistence);
        assertEquals("failures must equal List.of(failure)", List.of(failure), failures);
        assertSame("failures.get(0) keeps the identity of failure", failure, failures.get(0));
    }

    @Test public void unexpectedCheckpointDefectKeepsItsCauseAndIsNotAnIoDiagnostic() {
        var failure = new IllegalStateException("Codec defect");
        var persistence = new GamePersistence((game, eligible, id) -> { throw failure; },
                (id, game, time) -> fail("Ranking"), () -> 0, error -> fail("IO diagnostic"));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "failure", failure, "persistence", persistence);
        assertSame("the declared component operation keeps the identity of failure", failure, assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class, () -> persistence.save(
                atf.gradle.audit.GradleInputs.created("Persistence or profile input before save", GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", GameConfig.defaults(31)))), true, "fixture-match", game -> fail("Replay"))));
    }

    @Test public void replayFailureAfterCheckpointIsNotReclassifiedOrRetried() {
        var events = new ArrayList<String>();
        var failure = new IllegalStateException("Replay defect");
        var persistence = new GamePersistence((game, eligible, id) -> events.add("checkpoint"),
                (id, game, time) -> fail("Ranking"), () -> 0, error -> fail("IO diagnostic"));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "events", events, "failure", failure, "persistence", persistence);
        assertSame("the declared component operation keeps the identity of failure", failure, assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class, () -> persistence.save(
                atf.gradle.audit.GradleInputs.created("Persistence or profile input before save", GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", GameConfig.defaults(31)))), true, "fixture-match", game -> { throw failure; })));
        assertEquals("events must equal List.of(\"checkpoint\")", List.of("checkpoint"), events);
    }

    @Test public void storageDiagnosticDoesNotRejectOrReplayTheAcceptedCommand() {
        var game = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", GameConfig.defaults(31)));
        var before = game.snapshot();
        var events = new ArrayList<String>();
        var failure = new IOException("Storage unavailable");
        var persistence = new GamePersistence((saved, eligible, id) -> {

            assertEquals("saved.snapshot().bidsSubmitted() must equal before.bidsSubmitted() + 1", before.bidsSubmitted() + 1, saved.snapshot().bidsSubmitted());
            throw failure;
        }, (id, saved, time) -> fail("Ranking"), () -> 0, error -> {
            assertSame("error keeps the identity of failure", failure, error);
            events.add("storage-error");
        });
        var flow = new GameCommandFlow(() -> persistence.save(atf.gradle.audit.GradleInputs.created("Persistence or profile input before save", game), true, "fixture-match",
                saved -> fail("Replay after failed checkpoint")), () -> events.add("advance"));
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "events", events, "failure", failure, "flow", flow, "game", game, "persistence", persistence);
        assertTrue("flow.execute(()->game.submitBid(before.biddingSeat(), 0), ()->events.add(\"accepted\"), (refusal)->fail(\"An applied bid cannot be refused\")) must hold", flow.execute(() -> game.submitBid(before.biddingSeat(), 0), () -> events.add("accepted"),
                refusal -> fail("An applied bid cannot be refused")));
        assertEquals("events must equal List.of(\"accepted\", \"storage-error\", \"advance\")", List.of("accepted", "storage-error", "advance"), events);
        assertEquals("game.snapshot().bidsSubmitted() must equal before.bidsSubmitted() + 1", before.bidsSubmitted() + 1, game.snapshot().bidsSubmitted());
    }

    @Test public void delayedCheckpointPreventsReplayAndContinuationUntilItReturns() throws Exception {
        var game = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", GameConfig.defaults(31)));
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var replay = new CountDownLatch(1);
        var advanced = new CountDownLatch(1);
        var persistence = new GamePersistence((saved, eligible, id) -> {
            entered.countDown();
            try {
                if (!release.await(10, TimeUnit.SECONDS)) throw new IOException("Fixture was not released");
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IOException(interrupted);
            }
        }, (id, saved, time) -> fail("Unfinished match"), () -> 0, error -> fail(error.toString()));
        var worker = Executors.newSingleThreadExecutor();
        try {
            var result = worker.submit(() -> new GameCommandFlow(
                    () -> persistence.save(atf.gradle.audit.GradleInputs.created("Persistence or profile input before save", game), true, "fixture-match", saved -> replay.countDown()),
                    advanced::countDown).execute(() -> game.submitBid(game.snapshot().biddingSeat(), 0),
                    () -> { }, failure -> fail(failure.toString())));

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "advanced", advanced, "entered", entered, "game", game, "persistence", persistence, "release", release, "replay", replay, "result", result, "worker", worker);
            assertTrue("Controlled checkpoint started", entered.await(10, TimeUnit.SECONDS));
            assertEquals("replay.getCount() must equal 1", 1, replay.getCount());
            assertEquals("advanced.getCount() must equal 1", 1, advanced.getCount());
            assertFalse("result.isDone() must remain false", result.isDone());
            release.countDown();
            assertTrue("result.get(10, TimeUnit.SECONDS) must hold", result.get(10, TimeUnit.SECONDS));
            assertEquals("replay.getCount() must equal 0", 0, replay.getCount());
            assertEquals("advanced.getCount() must equal 0", 0, advanced.getCount());
        } finally {
            release.countDown();
            worker.shutdownNow();
            assertTrue("Fixture worker stopped", worker.awaitTermination(10, TimeUnit.SECONDS));
        }
    }
}
