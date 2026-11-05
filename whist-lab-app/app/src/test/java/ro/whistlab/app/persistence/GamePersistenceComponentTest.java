package ro.whistlab.app.persistence;

import static org.junit.Assert.*;

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

public class GamePersistenceComponentTest {
    @Test public void checkpointReceivesTheSameSessionEligibilityAndIdentityBeforeReplay() {
        var game = GameSession.newGame(GameConfig.defaults(31));
        var events = new ArrayList<String>();
        var persistence = new GamePersistence((saved, eligible, id) -> {
            assertSame(game, saved);
            assertFalse(eligible);
            assertEquals("fixture-match", id);
            events.add("checkpoint");
        }, (id, saved, time) -> fail("Unfinished match"), () -> { throw new AssertionError("Clock"); },
                error -> fail(error.toString()));
        persistence.save(game, false, "fixture-match", saved -> {
            assertSame(game, saved);
            events.add("replay");
        });
        assertEquals(List.of("checkpoint", "replay"), events);
    }

    @Test public void noSessionHasNoPersistenceSideEffects() {
        var persistence = new GamePersistence((game, eligible, id) -> fail("Checkpoint"),
                (id, game, time) -> fail("Ranking"), () -> { throw new AssertionError("Clock"); },
                error -> fail("Storage error"));
        persistence.save(null, true, "fixture-match", game -> fail("Replay"));
    }

    @Test public void checkpointIoFailureIsReportedOnceAndStopsLaterPersistenceStages() {
        var failure = new IOException("Disk full");
        var failures = new ArrayList<IOException>();
        var persistence = new GamePersistence((game, eligible, id) -> { throw failure; },
                (id, game, time) -> fail("Ranking"), () -> 0, failures::add);
        persistence.save(GameSession.newGame(GameConfig.defaults(31)), true, "fixture-match",
                game -> fail("Replay"));
        assertEquals(List.of(failure), failures);
        assertSame(failure, failures.get(0));
    }

    @Test public void unexpectedCheckpointDefectKeepsItsCauseAndIsNotAnIoDiagnostic() {
        var failure = new IllegalStateException("Codec defect");
        var persistence = new GamePersistence((game, eligible, id) -> { throw failure; },
                (id, game, time) -> fail("Ranking"), () -> 0, error -> fail("IO diagnostic"));
        assertSame(failure, assertThrows(IllegalStateException.class, () -> persistence.save(
                GameSession.newGame(GameConfig.defaults(31)), true, "fixture-match", game -> fail("Replay"))));
    }

    @Test public void replayFailureAfterCheckpointIsNotReclassifiedOrRetried() {
        var events = new ArrayList<String>();
        var failure = new IllegalStateException("Replay defect");
        var persistence = new GamePersistence((game, eligible, id) -> events.add("checkpoint"),
                (id, game, time) -> fail("Ranking"), () -> 0, error -> fail("IO diagnostic"));
        assertSame(failure, assertThrows(IllegalStateException.class, () -> persistence.save(
                GameSession.newGame(GameConfig.defaults(31)), true, "fixture-match", game -> { throw failure; })));
        assertEquals(List.of("checkpoint"), events);
    }

    @Test public void storageDiagnosticDoesNotRejectOrReplayTheAcceptedCommand() {
        var game = GameSession.newGame(GameConfig.defaults(31));
        var before = game.snapshot();
        var events = new ArrayList<String>();
        var failure = new IOException("Storage unavailable");
        var persistence = new GamePersistence((saved, eligible, id) -> {
            assertEquals(before.bidsSubmitted() + 1, saved.snapshot().bidsSubmitted());
            throw failure;
        }, (id, saved, time) -> fail("Ranking"), () -> 0, error -> {
            assertSame(failure, error);
            events.add("storage-error");
        });
        var flow = new GameCommandFlow(() -> persistence.save(game, true, "fixture-match",
                saved -> fail("Replay after failed checkpoint")), () -> events.add("advance"));
        assertTrue(flow.execute(() -> game.submitBid(before.biddingSeat(), 0), () -> events.add("accepted"),
                refusal -> fail("An applied bid cannot be refused")));
        assertEquals(List.of("accepted", "storage-error", "advance"), events);
        assertEquals(before.bidsSubmitted() + 1, game.snapshot().bidsSubmitted());
    }

    @Test public void delayedCheckpointPreventsReplayAndContinuationUntilItReturns() throws Exception {
        var game = GameSession.newGame(GameConfig.defaults(31));
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
                    () -> persistence.save(game, true, "fixture-match", saved -> replay.countDown()),
                    advanced::countDown).execute(() -> game.submitBid(game.snapshot().biddingSeat(), 0),
                    () -> { }, failure -> fail(failure.toString())));
            assertTrue("Controlled checkpoint started", entered.await(10, TimeUnit.SECONDS));
            assertEquals(1, replay.getCount());
            assertEquals(1, advanced.getCount());
            assertFalse(result.isDone());
            release.countDown();
            assertTrue(result.get(10, TimeUnit.SECONDS));
            assertEquals(0, replay.getCount());
            assertEquals(0, advanced.getCount());
        } finally {
            release.countDown();
            worker.shutdownNow();
            assertTrue("Fixture worker stopped", worker.awaitTermination(10, TimeUnit.SECONDS));
        }
    }
}
