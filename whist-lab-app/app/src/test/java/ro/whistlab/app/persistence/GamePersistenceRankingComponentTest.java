package ro.whistlab.app.persistence;

import static atf.gradle.audit.assertions.GradleAssertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.Test;
import ro.whistlab.app.highscores.HighScoreRepository;
import ro.whistlab.app.highscores.HighScores;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

public class GamePersistenceRankingComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void completedMatchKeepsCheckpointRankingAndReplayOrderAndIdentity() {
        var game = finishedMatch();
        var before = game.snapshot();
        var events = new ArrayList<String>();
        var persistence = new GamePersistence((saved, eligible, id) -> {

            assertSame("saved keeps the identity of game", game, saved);
            assertTrue("eligible must hold", eligible);
            assertEquals("id must equal \"stable-match\"", "stable-match", id);
            events.add("checkpoint");
        }, (id, saved, time) -> {
            assertEquals("id must equal \"stable-match\"", "stable-match", id);
            assertSame("saved keeps the identity of game", game, saved);
            assertEquals("time must equal 1234", 1234, time);
            events.add("ranking");
        }, () -> 1234, error -> fail(error.toString()));
        persistence.save(atf.gradle.audit.GradleInputs.created("Persistence or profile input before save", game), true, "stable-match", saved -> events.add("replay"));
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "events", events, "game", game, "persistence", persistence);
        assertEquals("events must equal List.of(\"checkpoint\", \"ranking\", \"replay\")", List.of("checkpoint", "ranking", "replay"), events);
        assertEquals("game.snapshot() must equal before", before, game.snapshot());
    }

    @Test public void excludedAndUnfinishedMatchesNeverReadTheRankingClock() {
        var persistence = new GamePersistence((game, eligible, id) -> { },
                (id, game, time) -> fail("Ranking"), () -> { throw new AssertionError("Clock"); },
                error -> fail(error.toString()));
        var finished = finishedMatch();
        persistence.save(atf.gradle.audit.GradleInputs.created("Persistence or profile input before save", finished), false, "fixture-match", game -> { });
        var unfinished = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", GameConfig.defaults(31)));
        persistence.save(atf.gradle.audit.GradleInputs.created("Persistence or profile input before save", unfinished), true, "fixture-match", game -> { });
        finishHand(unfinished);

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "finished", finished, "persistence", persistence, "unfinished", unfinished);
        assertFalse("unfinished.snapshot().finalHand() must remain false", unfinished.snapshot().finalHand());
        persistence.save(atf.gradle.audit.GradleInputs.created("Persistence or profile input before save", unfinished), true, "fixture-match", game -> { });
    }

    @Test public void aFailedFinalCheckpointCannotPublishTheRankingOrReplay() {
        var failure = new IOException("Checkpoint unavailable");
        var errors = new ArrayList<IOException>();
        var persistence = new GamePersistence((game, eligible, id) -> { throw failure; },
                (id, game, time) -> fail("Ranking"), () -> { throw new AssertionError("Clock"); }, errors::add);
        persistence.save(atf.gradle.audit.GradleInputs.created("Persistence or profile input before save", finishedMatch()), true, "stable-match", game -> fail("Replay"));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "errors", errors, "failure", failure, "persistence", persistence);
        assertEquals("errors must equal List.of(failure)", List.of(failure), errors);
    }

    @Test public void rankingIoFailureStillExportsReplayWithoutChangingOrRepeatingTheMatch() {
        var game = finishedMatch();
        var before = game.snapshot();
        var failure = new IOException("Ranking storage unavailable");
        var events = new ArrayList<String>();
        var persistence = new GamePersistence((saved, eligible, id) -> events.add("checkpoint"),
                (id, saved, time) -> { events.add("ranking"); throw failure; }, () -> 1234, error -> {

                    assertSame("error keeps the identity of failure", failure, error);
                    events.add("storage-error");
                });
        persistence.save(atf.gradle.audit.GradleInputs.created("Persistence or profile input before save", game), true, "stable-match", saved -> events.add("replay"));
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "events", events, "failure", failure, "game", game, "persistence", persistence);
        assertEquals("events must equal List.of(\"checkpoint\", \"ranking\", \"storage-error\", \"replay\")", List.of("checkpoint", "ranking", "storage-error", "replay"), events);
        assertEquals("game.snapshot() must equal before", before, game.snapshot());
    }

    @Test public void unexpectedRankingDefectPropagatesWithoutReplayOrIoTranslation() {
        var failure = new IllegalStateException("Ranking defect");
        var persistence = new GamePersistence((saved, eligible, id) -> { },
                (id, game, time) -> { throw failure; }, () -> 1234, error -> fail("IO diagnostic"));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "failure", failure, "persistence", persistence);
        assertSame("the declared component operation keeps the identity of failure", failure, assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class, () -> persistence.save(
                atf.gradle.audit.GradleInputs.created("Persistence or profile input before save", finishedMatch()), true, "stable-match", game -> fail("Replay"))));
    }

    @Test public void retryAndRestoredSessionRecordEachSeatOnceWithTheSameMatchIdentity() throws Exception {
        var disk = new MemoryStorage();
        var repository = new HighScoreRepository(disk, new Random(7));
        repository.boards();
        var errors = new ArrayList<IOException>();
        var persistence = new GamePersistence((game, eligible, id) -> { }, repository::record, () -> 1234, errors::add);
        var game = finishedMatch();
        var before = game.snapshot();
        var failure = new IOException("Ranking disk full");
        disk.failure = failure;
        persistence.save(atf.gradle.audit.GradleInputs.created("Persistence or profile input before save", game), true, "stable-match", saved -> { });

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "disk", disk, "errors", errors, "failure", failure, "game", game, "persistence", persistence, "repository", repository);
        assertEquals("errors must equal List.of(failure)", List.of(failure), errors);
        disk.failure = null;
        persistence.save(atf.gradle.audit.GradleInputs.created("Persistence or profile input before save", game), true, "stable-match", saved -> { });
        int writes = disk.writes;
        var restored = GameSession.restoreComplete(game.saveState());
        var reloaded = new HighScoreRepository(disk, new Random(99));
        var afterRestart = new GamePersistence((saved, eligible, id) -> { }, reloaded::record, () -> 5678, errors::add);
        afterRestart.save(atf.gradle.audit.GradleInputs.created("Persistence or profile input before save", restored), true, "stable-match", saved -> { });
        afterRestart.save(atf.gradle.audit.GradleInputs.created("Persistence or profile input before save", restored), true, "stable-match", saved -> { });
        assertEquals("disk.writes must equal writes", writes, disk.writes);
        var results = reloaded.board(HighScores.Profile.from(game.config())).entries().stream()
                .filter(entry -> !entry.sample()).toList();
        assertEquals("results.size() must equal game.config().playerCount()", game.config().playerCount(), results.size());
        for (var player : before.players()) {
            var entry = results.stream().filter(value -> value.seat() == player.seat()).findFirst().orElseThrow();
            assertEquals("entry.score() must equal player.totalScore()", player.totalScore(), entry.score());
            assertEquals("entry.recordedAt() must equal 1234", 1234, entry.recordedAt());
        }
        assertEquals("game.snapshot() must equal before", before, game.snapshot());
        assertEquals("restored.snapshot() must equal before", before, restored.snapshot());
        assertEquals("errors.size() must equal 1", 1, errors.size());
    }

    private static GameSession finishedMatch() {
        var game = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", GameConfig.newMatch(3, GameMode.ROMANIAN_WHIST,
                AiDifficulty.NORMAL, 31, true, 10)));
        while (true) {
            finishHand(game);
            if (game.snapshot().finalHand()) return game;
            game.startNextHand();
        }
    }

    private static void finishHand(GameSession game) {
        while (game.snapshot().phase() == GamePhase.BIDDING) {
            var state = game.snapshot();
            int bid = GameRules.allowedBids(state.handSize(), state.bidTotal(),
                    state.bidsSubmitted() == state.players().size() - 1).get(0);
            game.submitBid(state.biddingSeat(), bid);
        }
        while (game.snapshot().phase() == GamePhase.PLAYING) {
            var state = game.snapshot();
            var led = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
            var legal = GameRules.legalCards(state.players().get(state.currentSeat()).hand(), led,
                    state.trump(), state.mode());
            game.playCard(state.currentSeat(), legal.get(0));
        }
    }

    private static final class MemoryStorage implements HighScoreRepository.Storage {
        private String json;
        private int writes;
        private IOException failure;
        public String read() { return json; }
        public void write(String value) throws IOException {
            if (failure != null) throw failure;
            json = value;
            writes++;
        }
    }
}
