package ro.whistlab.app.score.raster;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ro.whistlab.app.score.series.AwardRasterFixture;
import ro.whistlab.app.score.series.ScoredHandFixture;
import ro.whistlab.core.rules.GameMode;

public final class ScoreHistoryRecoveryRasterComponentTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    @Test public void savedBiddingPlayingAndSettlementKeepHistoricalRectanglesAndPixelsInBothLanguages() throws Exception {
        for (int players = 3; players <= 6; players++) {
            var game = AwardRasterFixture.create(players, AwardRasterFixture.Pattern.PENALTY_WITH_POSITIVE_TOTAL);
            var expected = HistoryRecoveryProof.Expected.current(game);
            assertEquals(-10, expected.series().get(0).intValue());
            assertTrue(expected.totals().get(0) > 0);
            String id = "resume-" + players + "p";
            game = HistoryRecoveryProof.verify(id + "-award", game, expected, temporary.getRoot().toPath());
            game.startNextHand();
            ScoredHandFixture.finish(game, 0, (stage, source) ->
                    HistoryRecoveryProof.verify(id + "-" + stage, source, expected, temporary.getRoot().toPath()));
        }
    }

    @Test public void finalHandsAndTheLastEligiblePaymentsRemainVisibleAfterFileRecoveryAtThreeToSixSeats() throws Exception {
        for (var mode : List.of(GameMode.ROMANIAN_WHIST, GameMode.LONG_ROMANIAN)) for (int players = 3; players <= 6; players++) {
            var fixture = AwardRasterFixture.completed(players, mode);
            var game = fixture.game();
            assertTrue(game.snapshot().finalHand());
            var finalExpected = HistoryRecoveryProof.Expected.current(game);
            if (mode == GameMode.ROMANIAN_WHIST) {
                assertEquals(1, game.snapshot().handSize());
                assertTrue(finalExpected.series().stream().allMatch(value -> value == 0));
            } else {
                assertEquals(-10, finalExpected.series().get(0).intValue());
                assertEquals(10, finalExpected.series().get(1).intValue());
            }
            String id = "final-" + mode.name().toLowerCase(java.util.Locale.ROOT) + "-" + players + "p";
            HistoryRecoveryProof.verify(id, game, finalExpected, temporary.getRoot().toPath());
            assertEquals(-10, fixture.eligibleSeries().get(0).intValue());
            assertEquals(10, fixture.eligibleSeries().get(1).intValue());
            if (mode == GameMode.ROMANIAN_WHIST)
                HistoryRecoveryProof.verify(id + "-last-eligible", game,
                        new HistoryRecoveryProof.Expected(fixture.lastEligibleIndex(), fixture.eligibleTotals(), fixture.eligibleSeries()),
                        temporary.getRoot().toPath());
        }
    }
}
