package ro.whistlab.app.highscores;

import static org.junit.Assert.*;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.json.JSONObject;
import org.junit.Test;
import ro.whistlab.app.persistence.GameSessionStore;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

public class HighScoreRepositoryComponentTest {
    private static final HighScores.Profile PROFILE = new HighScores.Profile(GameMode.ROMANIAN_WHIST, 4, AiDifficulty.HARD, true, 10, 10);
    private static final class Memory implements HighScoreRepository.Storage {
        String json; int writes; boolean fail;
        public String read() { return json; }
        public void write(String value) throws IOException { if (fail) throw new IOException("disk full"); json = value; writes++; }
    }

    @Test public void everyGameTypeStartsWithTenMarkedSamplesAndReloadDoesNotReseed() throws Exception {
        var disk = new Memory(); var repository = new HighScoreRepository(disk, new Random(7));
        var first = repository.boards();
        assertEquals(GameMode.values().length, first.size());
        for (var board : first) {
            assertEquals(10, board.entries().size());
            assertTrue(board.entries().stream().allMatch(HighScores.Entry::sample));
            assertTrue(board.entries().stream().map(HighScores.Entry::score).distinct().count() > 1);
        }
        String bytes = disk.json; int writes = disk.writes;
        assertEquals(first, new HighScoreRepository(disk, new Random(999)).boards());
        assertEquals(bytes, disk.json); assertEquals(writes, disk.writes);
    }

    @Test public void rankingGrowsToTwentyThenRetainsTheBestScoresIncludingAiAndNegativeScores() {
        var data = new HighScores(); data.ensure(PROFILE, new Random(1));
        for (int game = 0; game < 8; game++) {
            var entries = new ArrayList<HighScores.Entry>();
            for (int seat = 0; seat < 4; seat++) entries.add(new HighScores.Entry("Ana", game * 100 + seat, seat != 0, false, "game-" + game, seat, game));
            assertTrue(data.record(PROFILE, "game-" + game, entries, new Random(2)));
            assertEquals(Math.min(10 + 4 * (game + 1), 20), data.boards.get(PROFILE.key()).entries().size());
        }
        var top = data.boards.get(PROFILE.key()).entries();
        assertEquals(703, top.get(0).score()); assertEquals(300, top.get(19).score());
        assertTrue(top.stream().anyMatch(HighScores.Entry::ai));
        assertTrue(top.stream().anyMatch(entry -> !entry.ai()));
        var empty = new HighScores();
        empty.record(PROFILE, "negative", List.of(new HighScores.Entry("Ana", -25, false, false, "negative", 0, 0)), new Random(1));
        assertTrue(empty.boards.get(PROFILE.key()).entries().stream().anyMatch(entry -> entry.score() == -25));
    }

    @Test public void onlyCompletedMatchesAreCommittedAndEachSeatIsRecordedExactlyOnceAcrossReload() throws Exception {
        var disk = new Memory(); var repository = new HighScoreRepository(disk, new Random(3));
        var game = GameSession.newGame(GameConfig.newMatch(3, GameMode.LONG_ROMANIAN, AiDifficulty.NORMAL, 17, true, 10), "Cristi");
        assertFalse(repository.record(game, 1)); assertNull(disk.json);
        finishHand(game); assertFalse(repository.record(game, 1)); assertNull(disk.json);
        while (!game.snapshot().finalHand() || game.snapshot().phase() != GamePhase.SCORE) {
            if (game.snapshot().phase() == GamePhase.SCORE) game.startNextHand();
            finishHand(game);
        }
        assertTrue(repository.record(game, 123));
        var board = repository.board(HighScores.Profile.from(game.config()));
        var actual = board.entries().stream().filter(entry -> !entry.sample()).toList();
        assertEquals(3, actual.size());
        for (var player : game.snapshot().players()) {
            var entry = actual.stream().filter(value -> value.seat() == player.seat()).findFirst().orElseThrow();
            assertEquals(player.totalScore(), entry.score()); assertEquals(player.name(), entry.name());
            assertEquals(!player.human(), entry.ai());
        }
        int writes = disk.writes;
        game.renameHuman("AltNume");
        assertFalse(new HighScoreRepository(disk, new Random(4)).record(GameSession.restoreComplete(game.saveState()), 456));
        assertEquals(writes, disk.writes); assertEquals(board, repository.board(HighScores.Profile.from(game.config())));
        var legacyJson = GameSessionStore.encode(game.saveState()).put("version",1);
        legacyJson.remove("history");
        var legacy = GameSessionStore.fromJson(legacyJson.toString());
        assertTrue(legacy.saveState().history().size() < legacy.snapshot().handSize() * legacy.config().playerCount());
        assertTrue(repository.record("legacy-match", legacy, 500));
    }

    @Test public void failedCommitCanBeRetriedAndCorruptionIsNotOverwritten() throws Exception {
        var disk = new Memory(); var repository = new HighScoreRepository(disk, new Random(3));
        repository.boards(); String original = disk.json;
        disk.fail = true;
        assertThrows(IOException.class, () -> repository.board(PROFILE));
        assertEquals(original, disk.json);
        disk.fail = false; assertEquals(10, repository.board(PROFILE).entries().size());
        disk.json = "{broken"; int writes = disk.writes;
        assertThrows(IOException.class, repository::boards);
        assertEquals("{broken", disk.json); assertEquals(writes, disk.writes);
    }

    @Test public void codecRejectsUnsupportedOrDuplicateDataAndSeparatesRuleProfiles() throws Exception {
        var data = HighScores.initial(new Random(2));
        String encoded = HighScoreCodec.encode(data);
        assertEquals(encoded, HighScoreCodec.encode(HighScoreCodec.decode(encoded)));
        var future = new JSONObject(encoded).put("schemaVersion", 99);
        assertThrows(IOException.class, () -> HighScoreCodec.decode(future.toString()));
        var duplicate = new JSONObject(encoded);
        duplicate.getJSONArray("profiles").put(duplicate.getJSONArray("profiles").getJSONObject(0));
        assertThrows(IOException.class, () -> HighScoreCodec.decode(duplicate.toString()));
        assertNotEquals(PROFILE.key(), new HighScores.Profile(PROFILE.mode(), 3, PROFILE.difficulty(), true, 10, 10).key());
        assertNotEquals(PROFILE.key(), new HighScores.Profile(PROFILE.mode(), 4, AiDifficulty.NORMAL, true, 10, 10).key());
        assertNotEquals(PROFILE.key(), new HighScores.Profile(PROFILE.mode(), 4, PROFILE.difficulty(), true, 5, 5).key());
        assertNotEquals(PROFILE.key(), new HighScores.Profile(PROFILE.mode(), 4, PROFILE.difficulty(), true, 10, 5).key());
    }

    @Test public void theLocalJsonFileCanBeReopenedWithoutAndroid() throws Exception {
        var folder = Files.createTempDirectory("whist-scores-"); var file = folder.resolve("highestScores.json");
        HighScoreRepository.Storage disk = new HighScoreRepository.Storage() {
            public String read() throws IOException { return Files.exists(file) ? new String(Files.readAllBytes(file), java.nio.charset.StandardCharsets.UTF_8) : null; }
            public void write(String json) throws IOException { Files.write(file, json.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
        };
        try {
            var before = new HighScoreRepository(disk, new Random(4)).boards();
            assertTrue(Files.size(file) > 0);
            assertEquals(before, new HighScoreRepository(disk, new Random(5)).boards());
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(folder); }
    }

    @Test public void failedFinalMatchWriteIsRetryableAndTheMatchIsNotMarkedProcessedPrematurely() throws Exception {
        var disk = new Memory(); var repository = new HighScoreRepository(disk, new Random(3));
        var game = GameSession.newGame(GameConfig.newMatch(3, GameMode.ROMANIAN_WHIST, AiDifficulty.NORMAL, 19, true, 5));
        do { finishHand(game); if (game.snapshot().finalHand()) break; game.startNextHand(); } while (true);
        var profile = HighScores.Profile.from(game.config());
        repository.board(profile);
        String before = disk.json; disk.fail = true;
        assertThrows(IOException.class, () -> repository.record("first-match", game, 1));
        assertEquals(before, disk.json);
        disk.fail = false;
        assertTrue(repository.record("first-match", game, 1));
        assertFalse(repository.record("first-match", game, 2));
        assertEquals(3, repository.board(profile).entries().stream().filter(entry -> !entry.sample()).count());
        assertTrue(repository.record("another-match-with-the-same-deal", game, 3));
        assertEquals(6, repository.board(profile).entries().stream().filter(entry -> !entry.sample()).count());
    }

    private static void finishHand(GameSession game) {
        while (game.snapshot().phase() == GamePhase.BIDDING) {
            var state = game.snapshot();
            game.submitBid(state.biddingSeat(), GameRules.allowedBids(state.handSize(), state.bidTotal(), state.bidsSubmitted() == state.players().size() - 1).get(0));
        }
        while (game.snapshot().phase() == GamePhase.PLAYING) {
            var state = game.snapshot(); var player = state.players().get(state.currentSeat());
            game.playCard(player.seat(), GameRules.legalCards(player.hand(), state.currentTrick().isEmpty() ? null
                    : state.currentTrick().get(0).card().suit(), state.trump(), state.mode()).get(0));
        }
    }
}
