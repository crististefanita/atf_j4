package ro.whistlab.app.score;

import static org.junit.Assert.*;
import java.util.List;
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

public class ScoreHistoryTableComponentTest {
    @Test public void allVariantsAndSeatsExposeTheWholeScheduleAndExactRecordedCells() {
        for (GameMode mode : GameMode.values()) for (int seats = 3; seats <= 6; seats++) {
            var game = GameSession.newGame(GameConfig.newMatch(seats, mode, AiDifficulty.NORMAL, 8, mode.isRomanian(), 10), "Cristi");
            for (HowToPlay.Language language : HowToPlay.Language.values()) {
                var copy = new WhistUiText(language);
                var initial = ScoreHistoryTable.forMatch(game.snapshot(), game.scoreHistory(), copy);
                assertEquals(GameRules.handSizes(mode, seats).size(), initial.rows().size());
                assertEquals(seats, initial.players().size());
                assertEquals("Cristi", initial.players().get(0));
                assertEquals(ScoreHistoryTable.Status.CURRENT, initial.rows().get(0).status());
                assertTrue(initial.rows().get(1).cells().stream().allMatch(cell -> cell.points().equals("—") && cell.total().equals("—")));
            }
            do {
                finish(game);
                var snapshot = game.snapshot();
                var checkpoint = game.saveState();
                var table = ScoreHistoryTable.forMatch(snapshot, game.scoreHistory(), new WhistUiText(HowToPlay.Language.RO));
                var row = table.rows().get(snapshot.handIndex());
                assertEquals(ScoreHistoryTable.Status.RECORDED, row.status());
                assertEquals(snapshot.handSize(), row.cards());
                for (int seat = 0; seat < seats; seat++) {
                    assertEquals(snapshot.players().get(seat).bid().toString(), row.cells().get(seat).bid());
                    assertEquals(Integer.toString(snapshot.players().get(seat).totalScore()), row.cells().get(seat).total());
                    assertEquals(Integer.parseInt(row.cells().get(seat).points().replace("+", "")), game.scoreHistory().points(snapshot.handNumber(), seat));
                }
                assertEquals(checkpoint, game.saveState());
                assertEquals(table, ScoreHistoryTable.forMatch(GameSession.restoreComplete(checkpoint).snapshot(), game.scoreHistory(), new WhistUiText(HowToPlay.Language.RO)));
                if (snapshot.finalHand()) break;
                game.startNextHand();
            } while (true);
        }
    }

    @Test public void currentBidsAreVisibleButUnfinishedPointsStayUnknown() {
        var game = GameSession.newGame(GameConfig.defaults(9));
        int bidder = game.snapshot().biddingSeat();
        game.submitBid(bidder, 0);
        var table = ScoreHistoryTable.forMatch(game.snapshot(), game.scoreHistory(), new WhistUiText(HowToPlay.Language.EN));
        assertEquals("0", table.rows().get(0).cells().get(bidder).bid());
        assertEquals("—", table.rows().get(0).cells().get(bidder).points());
        assertEquals("—", table.rows().get(0).cells().get(bidder).total());
    }

    @Test public void legacyBalancesDoNotInventMissingContractsOrHandScores() throws Exception {
        var game = GameSession.newGame(GameConfig.defaults(17)); finish(game); game.startNextHand();
        var json = GameSessionStore.encode(game.saveState());
        json.put("version", 3); json.remove("scores"); json.remove("streakPenaltyPoints");
        var migrated = GameSessionStore.fromJson(json.toString());
        var table = ScoreHistoryTable.forMatch(migrated.snapshot(), migrated.scoreHistory(), new WhistUiText(HowToPlay.Language.RO));
        var earlier = table.rows().get(0);
        assertEquals(ScoreHistoryTable.Status.EARLIER, earlier.status());
        for (int seat = 0; seat < 4; seat++) {
            assertEquals("—", earlier.cells().get(seat).bid()); assertEquals("—", earlier.cells().get(seat).points());
            assertEquals(migrated.snapshot().players().get(seat).totalScore(), Integer.parseInt(earlier.cells().get(seat).total()));
        }
    }

    @Test public void savedRuleVersionPreservesOldPenaltyAndRoundTripsTheNewPenalty() throws Exception {
        var game = GameSession.newGame(GameConfig.newMatch(4, GameMode.ROMANIAN_WHIST, AiDifficulty.HARD, 8, true, 10));
        var json = GameSessionStore.encode(game.saveState());
        assertEquals(6, json.getInt("version"));
        assertEquals(10, GameSessionStore.fromJson(json.toString()).config().streakPenaltyPoints());
        json.put("version", 5); json.remove("streakPenaltyPoints");
        var legacy = GameSessionStore.fromJson(json.toString());
        assertEquals(10, legacy.config().streakBonusPoints()); assertEquals(5, legacy.config().streakPenaltyPoints());
        assertEquals(5, GameSessionStore.fromJson(GameSessionStore.encode(legacy.saveState()).toString()).config().streakPenaltyPoints());
        json.put("version", 6);
        assertThrows(IllegalStateException.class, () -> GameSessionStore.fromJson(json.toString()));
    }

    private static void finish(GameSession game) {
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
