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

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;


public class ScoreHistoryCheckpointTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void unsupportedSavedProfileHasItsOwnRecoverableLoadError() throws Exception {
        var payload = GameSessionStore.encode(newMatch().saveState());
        payload.put("mode", "UNAVAILABLE_PROFILE");

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "payload", payload);
        assertThrows("The operation rejects its input with GameSessionStore.UnsupportedSavedMode.class", GameSessionStore.UnsupportedSavedMode.class, () -> GameSessionStore.fromJson(payload.toString()));
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

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "first", first, "game", game, "migrated", migrated, "version", version);
            assertEquals("migrated.scoreHistory().priorHands() must equal 1", 1, migrated.scoreHistory().priorHands());
            assertEquals("migrated.scoreHistory().openingTotals() must equal first", first, migrated.scoreHistory().openingTotals());
            assertTrue("migrated.scoreHistory().closingTotals().isEmpty() must hold", migrated.scoreHistory().closingTotals().isEmpty());
            finish(migrated);
            var restored = restore(migrated);
            for (int seat = 0; seat < 4; seat++) {
                int total = restored.snapshot().players().get(seat).totalScore();
                assertEquals("restored.scoreHistory().points(2, seat) must equal total - first.get(seat)", total - first.get(seat), restored.scoreHistory().points(2, seat));
                var column = ScoreHistoryColumn.forPlayer(restored.scoreHistory(), seat, new WhistUiText(HowToPlay.Language.EN));
                assertTrue("column.opening().contains(\"hand 1: \" + first.get(seat)) must hold", column.opening().contains("hand 1: " + first.get(seat)));
                assertEquals("column.rows().size() must equal 1", 1, column.rows().size());
                assertEquals("column.rows().get(0).hand() must equal \"2\"", "2", column.rows().get(0).hand());
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

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "complete", complete, "game", game, "partial", partial, "restored", restored, "version", version);
            assertEquals("partial.priorHands() must equal 1", 1, partial.priorHands());
            for (int seat = 0; seat < 4; seat++) {
                assertEquals("(int)partial.openingTotals().get(seat) must equal complete.total(1, seat)", complete.total(1, seat), (int) partial.openingTotals().get(seat));
                assertEquals("partial.points(2, seat) must equal complete.points(2, seat)", complete.points(2, seat), partial.points(2, seat));
            }
            restored.startNextHand();
            assertEquals("restore(restored).scoreHistory() must equal partial", partial, restore(restored).scoreHistory());
            finish(restored);
            assertEquals("restore(restored).scoreHistory().completedHands() must equal 3", 3, restore(restored).scoreHistory().completedHands());
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

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "copy", copy, "history", history, "language", language, "mine", mine);
            assertEquals("mine.rows().stream().map(ScoreHistoryColumn.Row::points).toList() must equal List.of(\"+6\", \"0\")", List.of("+6", "0"), mine.rows().stream().map(ScoreHistoryColumn.Row::points).toList());
            var other = ScoreHistoryColumn.forPlayer(history, 2, copy).rows().get(0);
            assertEquals("other.points() must equal \"-1\"", "-1", other.points());
            assertEquals("other.total() must equal \"4\"", "4", other.total());
            assertEquals("other.bid() must equal \"\\u2014\"", "—", other.bid());
            assertTrue("other.description().contains(copy.bidLabel() + \" \" + copy.unknownBid()) must hold", other.description().contains(copy.bidLabel() + " " + copy.unknownBid()));
            assertTrue("other.description().endsWith(copy.historyPoints() + \" -1, Total 4\") must hold", other.description().endsWith(copy.historyPoints() + " -1, Total 4"));
        }
    }

    @Test public void everyModeKeepsEverySeatThroughTheLastHandAndAFreshMatchStartsEmpty() throws Exception {
        for (GameMode mode : GameMode.values()) {
            for (int seats : List.of(3, 6)) {
                GameConfig config = new GameConfig(seats, mode, AiDifficulty.NORMAL, 866);
                GameSession game = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", config));
                List<Integer> before = game.scoreHistory().openingTotals();
                for (int hand = 1; ; hand++) {
                    finish(game);
                    game = restore(game);
                    var history = game.scoreHistory();
                    var result = game.snapshot();

                    atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "before", before, "config", config, "game", game, "hand", hand, "history", history, "mode", mode, "result", result, "seats", seats);
                    assertEquals("history.completedHands() must equal hand", hand, history.completedHands());
                    for (var player : result.players()) {
                        int bid = player.bid(), won = player.tricksWon();
                        int earned = mode.isRomanian() ? (won == bid ? 5 + won : -Math.abs(won - bid))
                                : won + (won == bid ? 10 : 0);
                        assertEquals("history.points(hand, player.seat()) must equal earned", earned, history.points(hand, player.seat()));
                        assertEquals("history.total(hand, player.seat()) must equal before.get(player.seat()) + earned", before.get(player.seat()) + earned, history.total(hand, player.seat()));
                    }
                    if (result.finalHand()) break;
                    before = history.latestTotals();
                    game.startNextHand();
                    assertEquals("game.scoreHistory() must equal history", history, game.scoreHistory());
                }
                assertEquals("GameSession.newGame(atf.gradle.audit.GradleInputs.created(\"Game configuration before creation\", config)).scoreHistory().completedHands() must equal 0", 0, GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", config)).scoreHistory().completedHands());
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

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "loaded", loaded, "old", old, "player", player);
            assertNull("loaded.scoreHistory().bid(1, player.seat()) remains absent", loaded.scoreHistory().bid(1, player.seat()));
            assertEquals("loaded.scoreHistory().bid(2, player.seat()) must equal player.bid()", player.bid(), loaded.scoreHistory().bid(2, player.seat()));
        }
        loaded.startNextHand();
        finish(loaded);
        assertEquals("restore(loaded).scoreHistory() must equal loaded.scoreHistory()", loaded.scoreHistory(), restore(loaded).scoreHistory());
        JSONObject broken = GameSessionStore.encode(loaded.saveState());
        broken.getJSONObject("scores").getJSONArray("bids").getJSONArray(1).put(0, 1);
        rejects(broken);
        broken.getJSONObject("scores").remove("bids");
        rejects(broken);
    }

    private static void rejects(JSONObject state) {
        assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class, () -> GameSessionStore.fromJson(state.toString()));
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
        return GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(4, GameMode.ROMANIAN_WHIST, AiDifficulty.NORMAL, 401)));
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
