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
import ro.whistlab.core.session.ScoreHistory;

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
                    assertEquals(Integer.toString(snapshot.players().get(seat).totalScore()), row.cells().get(seat).displayPoints());
                    assertEquals(Integer.parseInt(row.cells().get(seat).points().replace("+", "")), game.scoreHistory().points(snapshot.handNumber(), seat));
                    assertEquals(snapshot.players().get(seat).tricksWon(), (int) game.scoreHistory().tricks(snapshot.handNumber(), seat));
                    assertEquals(snapshot.players().get(seat).bid().equals(snapshot.players().get(seat).tricksWon())
                            ? ro.whistlab.core.session.ScoreHistory.Outcome.EXACT : ro.whistlab.core.session.ScoreHistory.Outcome.MISSED,
                            row.cells().get(seat).outcome());
                    assertEquals(snapshot.players().get(seat).streakPoints(), row.cells().get(seat).seriesPoints());
                    assertEquals(snapshot.players().get(seat).streakPoints() != 0, row.cells().get(seat).boxPoints());
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
        assertTrue(table.rows().stream().flatMap(row -> row.cells().stream()).allMatch(cell -> cell.displayPoints().equals("—")));
        assertTrue(table.rows().stream().flatMap(row -> row.cells().stream()).noneMatch(ScoreHistoryTable.Cell::boxPoints));
    }

    @Test public void onlyPositiveAndNegativeSeriesPaymentsRectangleTheRecordedTotal() {
        for (GameMode mode : List.of(GameMode.ROMANIAN_WHIST, GameMode.LONG_ROMANIAN)) {
            for (int bonus : List.of(5, 10)) for (int penalty : List.of(5, 10)) {
                var game = GameSession.newGame(new GameConfig(4, mode, AiDifficulty.NORMAL, 8, true, bonus, penalty));
                var history = awardHistory(mode, bonus, -penalty, true, true);
                for (var language : HowToPlay.Language.values()) {
                    var copy = new WhistUiText(language);
                    var table = ScoreHistoryTable.forMatch(game.snapshot(), history, copy);
                    var cells = table.rows().get(8).cells();
                    assertEquals(List.of(bonus, -penalty, 0, 0), cells.stream().map(ScoreHistoryTable.Cell::seriesPoints).toList());
                    assertEquals(List.of(true, true, false, false), cells.stream().map(ScoreHistoryTable.Cell::boxPoints).toList());
                    assertEquals("+" + (5 + bonus), cells.get(0).points());
                    assertEquals(Integer.toString(-1 - penalty), cells.get(1).points());
                    assertEquals("-1", cells.get(2).points());
                    assertEquals("+" + (4 + GameRules.handSizes(mode, 4).get(8)), cells.get(3).points());
                    assertTrue(cells.get(0).description().contains(copy.historySeriesAdjustment(bonus)));
                    assertTrue(cells.get(1).description().contains(copy.historySeriesAdjustment(-penalty)));
                    assertFalse(cells.get(3).description().contains(copy.historySeriesAdjustment(bonus)));
                    assertTrue(table.rows().get(7).cells().stream().noneMatch(ScoreHistoryTable.Cell::boxPoints));
                    assertTrue(table.rows().get(9).cells().stream().noneMatch(ScoreHistoryTable.Cell::boxPoints));
                }
            }
        }
    }

    @Test public void missingContractsOrTricksNeverInventASeriesPaymentFromItsSign() {
        var game = GameSession.newGame(GameConfig.newMatch(4, GameMode.ROMANIAN_WHIST, AiDifficulty.NORMAL, 8, true, 10));
        for (boolean bids : List.of(false, true)) for (boolean tricks : List.of(false, true)) {
            if (bids && tricks) continue;
            var table = ScoreHistoryTable.forMatch(game.snapshot(), awardHistory(GameMode.ROMANIAN_WHIST, 10, -10, bids, tricks),
                    new WhistUiText(HowToPlay.Language.EN));
            assertTrue(table.rows().stream().flatMap(row -> row.cells().stream()).noneMatch(ScoreHistoryTable.Cell::boxPoints));
        }
    }

    @Test public void disabledSeriesAndUnconfiguredOrOppositeAdjustmentsHaveNoRectangle() {
        for (GameMode mode : GameMode.values()) {
            var game = GameSession.newGame(GameConfig.newMatch(4, mode, AiDifficulty.NORMAL, 8, false, 10));
            var table = ScoreHistoryTable.forMatch(game.snapshot(), awardHistory(mode, 10, -10, true, true),
                    new WhistUiText(HowToPlay.Language.EN));
            assertTrue(table.rows().stream().flatMap(row -> row.cells().stream()).noneMatch(ScoreHistoryTable.Cell::boxPoints));
        }
        var game = GameSession.newGame(GameConfig.newMatch(4, GameMode.ROMANIAN_WHIST, AiDifficulty.NORMAL, 8, true, 10));
        for (var adjustments : List.of(List.of(5, -5), List.of(-10, 10), List.of(1, -1))) {
            var table = ScoreHistoryTable.forMatch(game.snapshot(), awardHistory(GameMode.ROMANIAN_WHIST, adjustments.get(0), adjustments.get(1), true, true),
                    new WhistUiText(HowToPlay.Language.RO));
            assertTrue(table.rows().get(8).cells().stream().noneMatch(ScoreHistoryTable.Cell::boxPoints));
        }
    }

    @Test public void aOneCardHandNeverReceivesTheSeriesRectangle() {
        var game = GameSession.newGame(GameConfig.newMatch(4, GameMode.ROMANIAN_WHIST, AiDifficulty.NORMAL, 8, true, 10));
        var firstHand = new ScoreHistory(0, List.of(0, 0, 0, 0), List.of(List.of(15, -11, -1, 5)),
                List.of(List.of(0, 1, 0, 0)), List.of(List.of(0, 0, 1, 0)));
        var table = ScoreHistoryTable.forMatch(game.snapshot(), firstHand, new WhistUiText(HowToPlay.Language.RO));
        assertTrue(table.rows().get(0).cells().stream().noneMatch(ScoreHistoryTable.Cell::boxPoints));
    }

    @Test public void settledSeriesMarkersSurviveJsonReloadForEveryHistoricalSeat() throws Exception {
        int positive = 0, negative = 0;
        for (int points : List.of(5, 10)) {
            var game = seriesPaymentGame(points);
            int firstRecordedIndex = game.snapshot().handIndex();
            var awards = new java.util.ArrayList<List<Integer>>();
            do {
                finish(game);
                awards.add(game.snapshot().players().stream().map(GameSession.PlayerSnapshot::streakPoints).toList());
                game = GameSessionStore.fromJson(GameSessionStore.encode(game.saveState()).toString());
                var table = ScoreHistoryTable.forMatch(game.snapshot(), game.scoreHistory(), new WhistUiText(HowToPlay.Language.RO));
                for (int hand = 0; hand < awards.size(); hand++) for (int seat = 0; seat < 4; seat++) {
                    var cell = table.rows().get(firstRecordedIndex + hand).cells().get(seat);
                    assertEquals((int) awards.get(hand).get(seat), cell.seriesPoints());
                    assertEquals(Integer.toString(game.scoreHistory().total(firstRecordedIndex + hand + 1, seat)), cell.displayPoints());
                }
                for (int award : awards.get(awards.size() - 1)) {
                    if (award > 0) positive++;
                    if (award < 0) negative++;
                }
                if (game.snapshot().finalHand()) break;
                game.startNextHand();
            } while (true);
        }
        assertTrue("Real engine hands must exercise a positive series payment", positive > 0);
        assertTrue("Real engine hands must exercise a negative series payment", negative > 0);
    }

    private static GameSession seriesPaymentGame(int points) {
        var config = GameConfig.newMatch(4, GameMode.ROMANIAN_WHIST, AiDifficulty.NORMAL, 8, true, points);
        var hands = List.of(List.of(card(ro.whistlab.core.model.Suit.SPADES, 14), card(ro.whistlab.core.model.Suit.SPADES, 13)),
                List.of(card(ro.whistlab.core.model.Suit.HEARTS, 12), card(ro.whistlab.core.model.Suit.HEARTS, 11)),
                List.of(card(ro.whistlab.core.model.Suit.HEARTS, 10), card(ro.whistlab.core.model.Suit.HEARTS, 9)),
                List.of(card(ro.whistlab.core.model.Suit.HEARTS, 8), card(ro.whistlab.core.model.Suit.HEARTS, 7)));
        var deal = GameSession.fromDeal(config, hands, ro.whistlab.core.model.Trump.SPADES, 3).saveState();
        var players = deal.players().stream().map(player -> new GameSession.PlayerState(player.seat(),
                player.name(), player.human(), player.hand(), null, 0, 0, player.seat() < 2 ? 4 : -4, 0)).toList();
        var game = GameSession.restoreComplete(new GameSession.SaveState(config, GamePhase.BIDDING, 19, 2,
                3, 0, 0, 0, 0, ro.whistlab.core.model.Trump.SPADES, -1, players, List.of(), List.of(), List.of(),
                card(ro.whistlab.core.model.Suit.SPADES, 7)));
        for (int seat = 0; seat < 4; seat++) game.submitBid(seat, new int[]{2, 0, 0, 1}[seat]);
        return game;
    }

    private static ro.whistlab.core.model.Card card(ro.whistlab.core.model.Suit suit, int rank) {
        return new ro.whistlab.core.model.Card(suit, rank);
    }

    private static ScoreHistory awardHistory(GameMode mode, int bonus, int penalty, boolean bids, boolean tricks) {
        // An unknown prefix is retained; the known score difference proves only this settled adjustment.
        int size = GameRules.handSizes(mode, 4).get(8);
        return new ScoreHistory(8, List.of(0, 0, 0, 0), List.of(List.of(
                GameRules.scoreDelta(0, 0, mode) + bonus, GameRules.scoreDelta(0, 1, mode) + penalty,
                GameRules.scoreDelta(1, 0, mode), GameRules.scoreDelta(size - 1, size - 1, mode))),
                bids ? List.of(List.of(0, 1, 0, size - 1)) : List.of(),
                tricks ? List.of(List.of(0, 0, 1, size - 1)) : List.of());
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
            assertEquals(earlier.cells().get(seat).total(), earlier.cells().get(seat).displayPoints());
        }
    }

    @Test public void savedRuleVersionPreservesOldPenaltyAndRoundTripsTheNewPenalty() throws Exception {
        var game = GameSession.newGame(GameConfig.newMatch(4, GameMode.ROMANIAN_WHIST, AiDifficulty.HARD, 8, true, 10));
        var json = GameSessionStore.encode(game.saveState());
        assertEquals(7, json.getInt("version"));
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
