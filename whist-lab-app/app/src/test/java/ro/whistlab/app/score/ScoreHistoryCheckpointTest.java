package ro.whistlab.app.score;

import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.app.persistence.GameSessionStore;

import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.rules.HowToPlay;
import ro.whistlab.core.session.ScoreHistory;
import ro.whistlab.core.model.Suit;

import static org.junit.Assert.*;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;


public class ScoreHistoryCheckpointTest {
    @Test public void unsupportedSavedProfileHasItsOwnRecoverableLoadError() throws Exception {
        var payload = GameSessionStore.encode(newMatch().saveState());
        payload.put("mode", "UNAVAILABLE_PROFILE");
        assertThrows(GameSessionStore.UnsupportedSavedMode.class, () -> GameSessionStore.fromJson(payload.toString()));
    }

    @Test public void legacyMidHandCarriesOnlyKnownBalanceAndKeepsSubsequentRows() throws Exception {
        GameSession game = newMatch();
        finish(game);
        var first = game.scoreHistory().latestTotals();
        game.startNextHand();
        bidZero(game);
        playOne(game);
        for (int version = 1; version <= 3; version++) {
            GameSession migrated = legacy(game, version);
            assertEquals(1, migrated.scoreHistory().priorHands());
            assertEquals(first, migrated.scoreHistory().openingTotals());
            assertTrue(migrated.scoreHistory().closingTotals().isEmpty());
            finish(migrated);
            var restored = restore(migrated);
            for (int seat = 0; seat < 4; seat++) {
                int total = restored.snapshot().players().get(seat).totalScore();
                assertEquals(total - first.get(seat), restored.scoreHistory().points(2, seat));
                var column = ScoreHistoryColumn.forPlayer(restored.scoreHistory(), seat, new WhistUiText(HowToPlay.Language.EN));
                assertTrue(column.opening().contains("hand 1: " + first.get(seat)));
                assertEquals(1, column.rows().size());
                assertEquals("2", column.rows().get(0).hand());
            }
        }
    }

    @Test public void legacyScoreRecoversLastSettledHandExactlyOnce() throws Exception {
        GameSession game = newMatch();
        finish(game);
        game.startNextHand();
        finish(game);
        var complete = game.scoreHistory();
        for (int version = 1; version <= 3; version++) {
            GameSession restored = restore(legacy(game, version));
            var partial = restored.scoreHistory();
            assertEquals(1, partial.priorHands());
            for (int seat = 0; seat < 4; seat++) {
                assertEquals(complete.total(1, seat), (int) partial.openingTotals().get(seat));
                assertEquals(complete.points(2, seat), partial.points(2, seat));
            }
            restored.startNextHand();
            assertEquals(partial, restore(restored).scoreHistory());
            finish(restored);
            assertEquals(3, restore(restored).scoreHistory().completedHands());
        }
    }

    @Test public void damagedCurrentFormatFailsInsteadOfSilentlyDroppingHistory() throws Exception {
        GameSession game = newMatch();
        finish(game);
        game.startNextHand();
        finish(game);
        JSONObject state = GameSessionStore.encode(game.saveState());
        String good = state.toString();
        state.remove("scores");
        rejects(state);
        JSONObject scores = new JSONObject(good).getJSONObject("scores");
        JSONArray hands = scores.getJSONArray("hands");
        hands.getJSONArray(0).put(0, 500);
        rejects(new JSONObject(good).put("scores", scores));
        hands.getJSONArray(0).put(0, 0.5);
        rejects(new JSONObject(good).put("scores", scores));
        hands.put(0, new JSONArray().put(0));
        rejects(new JSONObject(good).put("scores", scores));
        scores = new JSONObject(good).getJSONObject("scores");
        scores.getJSONArray("hands").getJSONArray(1).put(1, -55);
        rejects(new JSONObject(good).put("scores", scores));
        scores = new JSONObject(good).getJSONObject("scores");
        scores.put("priorHands", 1);
        rejects(new JSONObject(good).put("scores", scores));
    }

    @Test public void columnKeepsZeroPositiveAndNegativeDistinctInBothLanguages() {
        var history = new ScoreHistory(0, List.of(0, 0, 0), List.of(List.of(0, -1, 5), List.of(6, 4, 4)));
        for (var language : HowToPlay.Language.values()) {
            var copy = new WhistUiText(language);
            var mine = ScoreHistoryColumn.forPlayer(history, 0, copy);
            assertEquals(List.of("+6", "0"), mine.rows().stream().map(ScoreHistoryColumn.Row::points).toList());
            var other = ScoreHistoryColumn.forPlayer(history, 2, copy).rows().get(0);
            assertEquals("-1", other.points());
            assertEquals("4", other.total());
            assertEquals("—", other.bid());
            assertTrue(other.description().contains(copy.bidLabel() + " " + copy.unknownBid()));
            assertTrue(other.description().endsWith(copy.historyPoints() + " -1, Total 4"));
        }
    }

    @Test public void everyModeKeepsEverySeatThroughTheLastHandAndAFreshMatchStartsEmpty() throws Exception {
        for (GameMode mode : GameMode.values()) {
            for (int seats : List.of(3, 6)) {
                GameConfig config = new GameConfig(seats, mode, AiDifficulty.NORMAL, 866);
                GameSession game = GameSession.newGame(config);
                List<Integer> before = game.scoreHistory().openingTotals();
                for (int hand = 1; ; hand++) {
                    finish(game);
                    game = restore(game);
                    var history = game.scoreHistory();
                    var result = game.snapshot();
                    assertEquals(hand, history.completedHands());
                    for (var player : result.players()) {
                        int bid = player.bid(), won = player.tricksWon();
                        int earned = mode.isRomanian() ? (won == bid ? 5 + won : -Math.abs(won - bid))
                                : won + (won == bid ? 10 : 0);
                        assertEquals(earned, history.points(hand, player.seat()));
                        assertEquals(before.get(player.seat()) + earned, history.total(hand, player.seat()));
                    }
                    if (result.finalHand()) break;
                    before = history.latestTotals();
                    game.startNextHand();
                    assertEquals(history, game.scoreHistory());
                }
                assertEquals(0, GameSession.newGame(config).scoreHistory().completedHands());
            }
        }
    }

    @Test public void v4RecoversOnlyProvableBidsAndV5RejectsLostOrContradictoryContracts() throws Exception {
        GameSession game = newMatch();
        finish(game);
        game.startNextHand();
        finish(game);
        JSONObject old = GameSessionStore.encode(game.saveState()).put("version", 4);
        old.getJSONObject("scores").remove("bids");
        GameSession loaded = GameSessionStore.fromJson(old.toString());
        for (var player : loaded.snapshot().players()) {
            assertNull(loaded.scoreHistory().bid(1, player.seat()));
            assertEquals(player.bid(), loaded.scoreHistory().bid(2, player.seat()));
        }
        loaded.startNextHand();
        finish(loaded);
        assertEquals(loaded.scoreHistory(), restore(loaded).scoreHistory());
        JSONObject broken = GameSessionStore.encode(loaded.saveState());
        broken.getJSONObject("scores").getJSONArray("bids").getJSONArray(1).put(0, 1);
        rejects(broken);
        broken.getJSONObject("scores").remove("bids");
        rejects(broken);
    }

    private static void rejects(JSONObject state) {
        assertThrows(IllegalStateException.class, () -> GameSessionStore.fromJson(state.toString()));
    }

    private static GameSession legacy(GameSession game, int version) throws Exception {
        JSONObject state = GameSessionStore.encode(game.saveState());
        state.put("version", version);
        state.remove("scores");
        if (version == 1) state.remove("history");
        return GameSessionStore.fromJson(state.toString());
    }

    private static GameSession restore(GameSession game) throws Exception {
        return GameSessionStore.fromJson(GameSessionStore.encode(game.saveState()).toString());
    }

    private static GameSession newMatch() {
        return GameSession.newGame(new GameConfig(4, GameMode.ROMANIAN_WHIST, AiDifficulty.NORMAL, 401));
    }

    private static void finish(GameSession game) {
        bidZero(game);
        while (game.snapshot().phase() == GamePhase.PLAYING) playOne(game);
    }

    private static void bidZero(GameSession game) {
        while (game.snapshot().phase() == GamePhase.BIDDING) game.submitBid(game.snapshot().biddingSeat(), 0);
    }

    private static void playOne(GameSession game) {
        var state = game.snapshot();
        int seat = state.currentSeat();
        Suit led = state.currentTrick().isEmpty() ? null : state.currentTrick().get(0).card().suit();
        game.playCard(seat, GameRules.legalCards(state.players().get(seat).hand(), led, state.trump(), state.mode()).get(0));
    }
}
