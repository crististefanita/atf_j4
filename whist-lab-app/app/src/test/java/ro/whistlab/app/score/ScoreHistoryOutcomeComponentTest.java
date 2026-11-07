package ro.whistlab.app.score;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.List;
import org.json.JSONObject;
import org.junit.Test;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.app.persistence.GameSessionStore;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.GameRules;
import ro.whistlab.core.rules.HowToPlay;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.session.ScoreHistory;

public final class ScoreHistoryOutcomeComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void exactZeroMissedZeroAndPositivePointMissHaveIndependentOutcomes() {
        var history = new ScoreHistory(0, List.of(0, 0, 0), List.of(List.of(5, -1, 2)),
                List.of(List.of(0, 0, 1)), List.of(List.of(0, 1, 2)));

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "history", history);
        assertEquals("history.outcome(1, 0) must equal ScoreHistory.Outcome.EXACT", ScoreHistory.Outcome.EXACT, history.outcome(1, 0));
        assertEquals("history.outcome(1, 1) must equal ScoreHistory.Outcome.MISSED", ScoreHistory.Outcome.MISSED, history.outcome(1, 1));
        assertEquals("history.outcome(1, 2) must equal ScoreHistory.Outcome.MISSED", ScoreHistory.Outcome.MISSED, history.outcome(1, 2));
        for (var language : HowToPlay.Language.values()) {
            var copy = new WhistUiText(language);
            assertEquals("ScoreHistoryColumn.forPlayer(history, 1, copy).rows().get(0).bid() must equal \"0\"", "0", ScoreHistoryColumn.forPlayer(history, 1, copy).rows().get(0).bid());
            assertTrue("ScoreHistoryColumn.forPlayer(history, 2, copy).rows().get(0).description().contains(copy.historyContractOutcome(ScoreHistory.Outcome.MISSED)) must hold", ScoreHistoryColumn.forPlayer(history, 2, copy).rows().get(0).description()
                    .contains(copy.historyContractOutcome(ScoreHistory.Outcome.MISSED)));
        }
    }

    @Test public void legacyPointsAndKnownBidsNeverInventAnOutcome() {
        var history = new ScoreHistory(0, List.of(0, 0, 0), List.of(List.of(-1, 5, 2)), List.of(List.of(0, 0, 1)));
        for (int seat = 0; seat < 3; seat++) {

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "history", history, "seat", seat);
            assertNull("history.tricks(1, seat) remains absent", history.tricks(1, seat));
            assertEquals("history.outcome(1, seat) must equal ScoreHistory.Outcome.UNKNOWN", ScoreHistory.Outcome.UNKNOWN, history.outcome(1, seat));
        }
    }

    @Test public void v6MigrationRecoversOnlyTheLastSettledHandAndPreservesTheUnknownPrefix() throws Exception {
        var game = twoHands();
        JSONObject old = GameSessionStore.encode(game.saveState()).put("version", 6);
        old.getJSONObject("scores").remove("tricks");
        var loaded = GameSessionStore.fromJson(old.toString());
        for (int seat = 0; seat < 4; seat++) {

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "loaded", loaded, "old", old, "seat", seat);
            assertNull("loaded.scoreHistory().tricks(1, seat) remains absent", loaded.scoreHistory().tricks(1, seat));
            assertEquals("loaded.scoreHistory().outcome(1, seat) must equal ScoreHistory.Outcome.UNKNOWN", ScoreHistory.Outcome.UNKNOWN, loaded.scoreHistory().outcome(1, seat));
            assertEquals("loaded.scoreHistory().tricks(2, seat) must equal game.scoreHistory().tricks(2, seat)", game.scoreHistory().tricks(2, seat), loaded.scoreHistory().tricks(2, seat));
        }
        loaded.startNextHand(); finish(loaded);
        var roundTrip = GameSessionStore.fromJson(GameSessionStore.encode(loaded.saveState()).toString());
        assertEquals("roundTrip.scoreHistory() must equal loaded.scoreHistory()", loaded.scoreHistory(), roundTrip.scoreHistory());
        assertNull("roundTrip.scoreHistory().tricks(1, 0) remains absent", roundTrip.scoreHistory().tricks(1, 0));
        assertNotNull("roundTrip.scoreHistory().tricks(3, 0) is available", roundTrip.scoreHistory().tricks(3, 0));
    }

    @Test public void anOldUnfinishedHandDoesNotAttributeItsTricksToThePreviousRound() throws Exception {
        var game = twoHands(); game.startNextHand();
        JSONObject old = GameSessionStore.encode(game.saveState()).put("version", 6);
        old.getJSONObject("scores").remove("tricks");
        var loaded = GameSessionStore.fromJson(old.toString());

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "game", game, "loaded", loaded, "old", old);
        assertTrue("loaded.scoreHistory().recordedTricks().isEmpty() must hold", loaded.scoreHistory().recordedTricks().isEmpty());
        var table = ScoreHistoryTable.forMatch(loaded.snapshot(), loaded.scoreHistory(), new WhistUiText(HowToPlay.Language.RO));
        assertTrue("table.rows().get(2).cells().stream().allMatch((c)->c.outcome() == ScoreHistory.Outcome.UNKNOWN) must hold", table.rows().get(2).cells().stream().allMatch(c -> c.outcome() == ScoreHistory.Outcome.UNKNOWN));
    }

    @Test public void currentFormatRejectsMissingFractionalNegativeOrUnconservedTrickRows() throws Exception {
        var encoded = GameSessionStore.encode(twoHands().saveState());

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "encoded", encoded);
        assertEquals("encoded.getInt(\"version\") must equal 7", 7, encoded.getInt("version"));
        JSONObject bad = new JSONObject(encoded.toString()); bad.getJSONObject("scores").remove("tricks"); rejects(bad);
        bad = new JSONObject(encoded.toString()); bad.getJSONObject("scores").getJSONArray("tricks").getJSONArray(0).put(0, 0.5); rejects(bad);
        bad = new JSONObject(encoded.toString()); bad.getJSONObject("scores").getJSONArray("tricks").getJSONArray(0).put(0, -1); rejects(bad);
        bad = new JSONObject(encoded.toString());
        for (int seat = 0; seat < 4; seat++) bad.getJSONObject("scores").getJSONArray("tricks").getJSONArray(0).put(seat, 0);
        rejects(bad);
    }

    @Test public void aConservedButFalseLatestWinnerIsRejected() throws Exception {
        var game = twoHands();
        var encoded = GameSessionStore.encode(game.saveState());
        int winner = game.snapshot().players().stream().filter(p -> p.tricksWon() == 1).findFirst().orElseThrow().seat();
        var row = encoded.getJSONObject("scores").getJSONArray("tricks").getJSONArray(1);
        row.put(winner, 0); row.put((winner + 1) % 4, 1);
        rejects(encoded);
    }

    @Test public void legacyInMemoryCheckpointsNormalizeBeforeWritingTheCurrentFormat() throws Exception {
        var source = twoHands().saveState();
        var h = source.scoreHistory();
        var legacy = new ScoreHistory(h.priorHands(), h.openingTotals(), h.closingTotals(), h.recordedBids());
        var restored = GameSession.restoreComplete(new GameSession.SaveState(source.config(), source.phase(),
                source.handIndex(), source.handSize(), source.dealerSeat(), source.biddingSeat(), source.currentSeat(),
                source.bidsSubmitted(), source.bidTotal(), source.trump(), source.lastTrickWinnerSeat(), source.players(),
                source.currentTrick(), source.lastTrick(), source.history(), source.turnedCard(), legacy));
        var encoded = GameSessionStore.encode(restored.saveState());
        var loaded = GameSessionStore.fromJson(encoded.toString());

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "encoded", encoded, "h", h, "legacy", legacy, "loaded", loaded, "restored", restored, "source", source);
        assertEquals("loaded.scoreHistory() must equal restored.scoreHistory()", restored.scoreHistory(), loaded.scoreHistory());
        assertNull("loaded.scoreHistory().tricks(1, 0) remains absent", loaded.scoreHistory().tricks(1, 0));
        assertNotNull("loaded.scoreHistory().tricks(2, 0) is available", loaded.scoreHistory().tricks(2, 0));
    }

    private static GameSession twoHands() {
        var game = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(4, GameMode.ROMANIAN_WHIST, AiDifficulty.NORMAL, 401)));
        finish(game); game.startNextHand(); finish(game); return game;
    }
    private static void rejects(JSONObject state) {
        assertThrows("The operation rejects its input with IllegalStateException.class", IllegalStateException.class, () -> GameSessionStore.fromJson(state.toString()));
    }
    private static void finish(GameSession game) {
        while (game.snapshot().phase() == GamePhase.BIDDING) game.submitBid(game.snapshot().biddingSeat(), 0);
        while (game.snapshot().phase() == GamePhase.PLAYING) {
            var state = game.snapshot(); var player = state.players().get(state.currentSeat());
            game.playCard(player.seat(), GameRules.legalCards(player.hand(), state.currentTrick().isEmpty() ? null
                    : state.currentTrick().get(0).card().suit(), state.trump(), state.mode()).get(0));
        }
    }
}
