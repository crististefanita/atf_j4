package ro.whistlab.app.score;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntUnaryOperator;
import org.junit.Test;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.app.persistence.GameSessionStore;
import ro.whistlab.app.score.series.ScoredHandFixture;
import atf.gradle.audit.matrix.ExhaustiveMatrixChecks;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.HowToPlay;
import ro.whistlab.core.session.GamePhase;
import ro.whistlab.core.session.GameSession;

public final class ScoreHistorySeriesComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void completeMatchesRetainEverySeatsTotalsAndBothSeriesPaymentsAcrossJsonRestarts() throws Exception {
        for (var mode : List.of(GameMode.ROMANIAN_WHIST, GameMode.LONG_ROMANIAN))
            for (int seats = 3; seats <= 6; seats++) for (int bonus : List.of(5, 10)) for (int penalty : List.of(5, 10))
                for (int missed = 0; missed < seats; missed++) {
                    int victim = missed;
                    var ledger = match(mode, seats, bonus, penalty, true, hand -> victim);
                    for (int seat = 0; seat < seats; seat++) {

                        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "bonus", bonus, "ledger", ledger, "missed", missed, "mode", mode, "penalty", penalty, "seat", seat, "seats", seats, "victim", victim);
                        assertTrue("Every seat must exercise its configured payment", seat == victim
                                ? ledger.negative[seat] > 0 : ledger.positive[seat] > 0);
                    }
                    if (mode == GameMode.LONG_ROMANIAN && seats == 4)
                        assertTrue("The final hand must include both bonus and penalty rectangles",
                                ledger.rows.get(ledger.rows.size() - 1).series().stream().allMatch(value -> value != 0));
                }
    }

    @Test public void changingContractsClearOppositeStreaksAndOneCardHandsCannotPayASeries() throws Exception {
        int resets = 0, oneCardResets = 0;
        for (var mode : List.of(GameMode.ROMANIAN_WHIST, GameMode.LONG_ROMANIAN)) for (int seats = 3; seats <= 6; seats++) {
            int count = seats;
            var ledger = match(mode, seats, 10, 5, true, hand -> ((hand - 1) / 3) % count);
            resets += ledger.resets;
            oneCardResets += ledger.oneCardResets;
        }

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "oneCardResets", oneCardResets, "resets", resets);
        assertTrue("The controlled contracts must exercise streak direction changes", resets > 0);
        assertTrue("One-card hands must exercise clearing an opposite pending streak", oneCardResets > 0);
    }

    @Test public void disabledSeriesKeepEveryRowUnboxedThroughTheLastHandAndReload() throws Exception {
        for (var mode : List.of(GameMode.ROMANIAN_WHIST, GameMode.LONG_ROMANIAN)) for (int seats = 3; seats <= 6; seats++) {
            var ledger = match(mode, seats, 5, 10, false, hand -> 0);

            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "ledger", ledger, "mode", mode, "seats", seats);
            assertTrue("ledger.rows.stream().flatMap((row)->row.series().stream()).allMatch((value)->value == 0) must hold", ledger.rows.stream().flatMap(row -> row.series().stream()).allMatch(value -> value == 0));
        }
    }

    private static Ledger match(GameMode mode, int seats, int bonus, int penalty, boolean series,
                                IntUnaryOperator missedSeat) throws Exception {
        var game = GameSession.newGame(atf.gradle.audit.GradleInputs.created("Game configuration before creation", new GameConfig(seats, mode, AiDifficulty.NORMAL, 401 + seats, series, bonus, penalty)));
        var ledger = new Ledger(seats, bonus, penalty, series);
        var checks = new ExhaustiveMatrixChecks();
        int[] checkpoints = {0};
        do {
            var result = ScoredHandFixture.finish(game, missedSeat.applyAsInt(game.snapshot().handNumber()), (stage, source) -> {
                var before = source.saveState();
                var restored = GameSessionStore.fromJson(GameSessionStore.encode(before).toString());
                checks.at(stage + " checkpoint / hand " + source.snapshot().handNumber());
                checks.equal("checkpoint preserves complete saved state", before, restored.saveState());
                if (restored.snapshot().phase() != GamePhase.SCORE) verify(restored, ledger, checks);
                checkpoints[0]++;
                return restored;
            });
            game = result.game();
            for (int seat = 0; seat < seats; seat++) {
                checks.at("settled hand " + game.snapshot().handNumber() + " / seat " + seat);
                checks.equal("tricks won", result.tricks().get(seat), game.snapshot().players().get(seat).tricksWon());
                checks.equal("contract bid", result.bids().get(seat), game.snapshot().players().get(seat).bid());
            }
            ledger.settle(game.snapshot().handSize(), result.bids(), result.tricks());
            verify(game, ledger, checks);
            if (game.snapshot().finalHand()) break;
            game.startNextHand();
            verify(game, ledger, checks);
        } while (true);
        assertEquals("checkpoints[0] must equal 3 * ledger.rows.size()", 3 * ledger.rows.size(), checkpoints[0]);
        assertEquals("ledger.rows.size() must equal game.scoreHistory().completedHands()", game.scoreHistory().completedHands(), ledger.rows.size());
        var finalTable = ScoreHistoryTable.forMatch(game.snapshot(), game.scoreHistory(), new WhistUiText(HowToPlay.Language.RO));
        assertEquals("The fixture completed the full configured hand schedule",
                ro.whistlab.core.rules.GameRules.handSizes(mode, seats).size(), ledger.rows.size());
        assertEquals("ledger.rows.size() must equal finalTable.rows().size()", finalTable.rows().size(), ledger.rows.size());
        verify(GameSessionStore.fromJson(GameSessionStore.encode(game.saveState()).toString()), ledger, checks);
        checks.publish();
        return ledger;
    }

    private static void verify(GameSession game, Ledger ledger, ExhaustiveMatrixChecks checks) {
        var before = game.saveState();
        for (var language : HowToPlay.Language.values()) {
            var copy = new WhistUiText(language);
            var table = ScoreHistoryTable.forMatch(game.snapshot(), game.scoreHistory(), copy);
            checks.at(language + " / completed hands");
            checks.equal("completed hands", ledger.rows.size(), game.scoreHistory().completedHands());
            for (int hand = 0; hand < table.rows().size(); hand++) for (int seat = 0; seat < ledger.total.length; seat++) {
                var cell = table.rows().get(hand).cells().get(seat);
                checks.at(language + " / hand " + (hand + 1) + " / seat " + seat);
                if (hand < ledger.rows.size()) {
                    var expected = ledger.rows.get(hand);
                    checks.equal("cumulative displayed points", expected.totals().get(seat).toString(), cell.displayPoints());
                    checks.equal("displayed contract", expected.bids().get(seat).toString(), cell.bid());
                    checks.equal("hand score delta", expected.delta().get(seat), Integer.parseInt(cell.points().replace("+", "")));
                    checks.equal("series adjustment", expected.series().get(seat), cell.seriesPoints());
                    checks.equal("series rectangle eligibility", expected.series().get(seat) != 0, cell.boxPoints());
                    if (cell.boxPoints()) checks.equal("localized adjustment description", true, cell.description().contains(copy.historySeriesAdjustment(expected.series().get(seat))));
                } else {
                    checks.equal("unplayed hand placeholder", "—", cell.displayPoints());
                    checks.equal("unplayed hand has no rectangle", false, cell.boxPoints());
                }
            }
            for (int seat = 0; seat < ledger.total.length; seat++) {
                var player = game.snapshot().players().get(seat);
                checks.at(language + " / player " + seat);
                checks.equal("player cumulative score", ledger.total[seat], player.totalScore());
                checks.equal("player pending streak", ledger.streak[seat], player.streak());
            }
        }
        checks.at("render is read-only");
        checks.equal("render preserves complete game state", before, game.saveState());
    }

    private record Expected(List<Integer> totals, List<Integer> delta, List<Integer> bids,
                            List<Integer> series) { }

    /** Independent arithmetic oracle; never delegates scoring or streak advancement to the engine. */
    private static final class Ledger {
        final int[] total, streak, positive, negative;
        final int bonus, penalty;
        final boolean enabled;
        final List<Expected> rows = new ArrayList<>();
        int resets, oneCardResets;
        Ledger(int seats, int bonus, int penalty, boolean enabled) {
            total = new int[seats]; streak = new int[seats]; positive = new int[seats]; negative = new int[seats];
            this.bonus = bonus; this.penalty = penalty; this.enabled = enabled;
        }
        void settle(int cards, List<Integer> bids, List<Integer> tricks) {
            int[] delta = new int[total.length], awards = new int[total.length];
            for (int seat = 0; seat < total.length; seat++) {
                int won = tricks.get(seat), bid = bids.get(seat);
                boolean exact = won == bid;
                delta[seat] = exact ? 5 + won : -Math.abs(won - bid);
                if (enabled) {
                    if (streak[seat] != 0 && exact != (streak[seat] > 0)) {
                        streak[seat] = 0; resets++; if (cards == 1) oneCardResets++;
                    }
                    if (cards > 1) streak[seat] += exact ? 1 : -1;
                    if (streak[seat] == 5) { awards[seat] = bonus; positive[seat]++; streak[seat] = 0; }
                    if (streak[seat] == -5) { awards[seat] = -penalty; negative[seat]++; streak[seat] = 0; }
                }
                delta[seat] += awards[seat];
                total[seat] += delta[seat];
            }
            rows.add(new Expected(integers(total), integers(delta), List.copyOf(bids), integers(awards)));
        }
        private static List<Integer> integers(int[] values) { return Arrays.stream(values).boxed().toList(); }
    }
}
