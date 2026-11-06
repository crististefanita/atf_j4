package ro.whistlab.app.score.raster;

import static org.junit.Assert.*;
import java.util.Collections;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.app.score.HistoryRasterProjection;
import ro.whistlab.app.score.raster.HistoryRaster;
import ro.whistlab.app.score.raster.HistoryRasterAudit;
import ro.whistlab.app.score.series.AwardRasterFixture;
import ro.whistlab.core.config.AiDifficulty;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.GameMode;
import ro.whistlab.core.rules.HowToPlay;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.session.ScoreHistory;

public final class ScoreHistoryCumulativeComponentTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();
    @Test public void everyHistoricalRowShowsItsOwnClosingBalanceForEverySeatAndLanguage() {
        var balances = List.of(List.of(12, -9, 0, 3, -7, 15),
                List.of(10, -7, -2, 7, -13, 0), List.of(17, -16, 2, 12, -10, -5));
        for (GameMode mode : GameMode.values()) for (int seats = 3; seats <= 6; seats++) {
            var game = newGame(mode, seats);
            int seatCount = seats;
            var totals = balances.stream().map(row -> row.subList(0, seatCount)).toList();
            var history = new ScoreHistory(0, Collections.nCopies(seats, 0), totals);
            for (var language : HowToPlay.Language.values()) {
                var table = HistoryRasterProjection.all(game.snapshot(), history, language, 1, 1);
                for (int hand = 0; hand < totals.size(); hand++) for (int seat = 0; seat < seats; seat++) {
                    var cell = table.rows().get(hand).cells().get(seat);
                    assertEquals(totals.get(hand).get(seat).toString(), cell.points());
                    assertEquals("RECORDED", table.rows().get(hand).status());
                    assertEquals(0, cell.series());
                }
                assertEquals("10", table.rows().get(1).cells().get(0).points());
                assertEquals("-2", table.rows().get(1).cells().get(0).handPoints());
                assertTrue(table.rows().subList(totals.size(), table.rows().size()).stream()
                        .flatMap(row -> row.cells().stream()).allMatch(cell -> cell.points().equals("—")));
            }
        }
    }

    @Test public void aLegacyPrefixDisplaysOnlyTheKnownBoundaryBalanceAndSubsequentTotals() {
        for (int seats = 3; seats <= 6; seats++) {
            var opening = List.of(10, -10, 0, 5, -5, 100).subList(0, seats);
            var closing = List.of(15, -11, -1, 10, 0, 99).subList(0, seats);
            var history = new ScoreHistory(2, opening, List.of(closing));
            for (var language : HowToPlay.Language.values()) {
                var table = HistoryRasterProjection.all(newGame(GameMode.ROMANIAN_WHIST, seats).snapshot(), history,
                        language, 1, 1);
                for (int seat = 0; seat < seats; seat++) {
                    assertEquals("—", table.rows().get(0).cells().get(seat).points());
                    var boundary = table.rows().get(1).cells().get(seat);
                    assertEquals(opening.get(seat).toString(), boundary.points());
                    assertEquals("—", boundary.bid());
                    assertEquals("—", boundary.handPoints());
                    assertEquals(closing.get(seat).toString(), table.rows().get(2).cells().get(seat).points());
                    assertEquals("—", table.rows().get(3).cells().get(seat).points());
                }
            }
        }
    }

    private static GameSession newGame(GameMode mode, int seats) {
        return GameSession.newGame(GameConfig.newMatch(seats, mode, AiDifficulty.NORMAL, 8, mode.isRomanian(), 10));
    }

    @Test public void rasterShowsRectanglesOnlyInAwardedPointsAndRedEvenForAPositiveRunningTotal() throws Exception {
        for (int players = 3; players <= 6; players++) for (var pattern : AwardRasterFixture.Pattern.values()) {
            var game = AwardRasterFixture.create(players, pattern);
            if (pattern == AwardRasterFixture.Pattern.PENALTY_WITH_POSITIVE_TOTAL) {
                assertEquals(-10, game.snapshot().players().get(0).streakPoints());
                assertTrue(game.snapshot().players().get(0).totalScore() > 0);
            }
            if (pattern == AwardRasterFixture.Pattern.ISOLATED_BONUS) {
                assertEquals(0, game.snapshot().players().get(0).streakPoints());
                assertEquals(0, game.snapshot().players().get(1).streakPoints());
                assertEquals(10, game.snapshot().players().get(2).streakPoints());
            }
            for (var language : HowToPlay.Language.values()) for (float scale : new float[]{1, 1.5f}) {
                var spec = raster(game, language, 2, scale);
                var baseline = HistoryRaster.render(spec, false);
                var image = HistoryRaster.render(spec, true);
                HistoryRasterAudit.rectanglesOnlyCoverTheirAwardedPointsCells(spec, baseline, image);
                HistoryRasterAudit.separatorPixels(spec, image, 4);
                var saved = temporary.newFile("raster-" + players + "-" + pattern + "-" + language + "-" + scale + ".png");
                assertTrue(ImageIO.write(image, "png", saved));
                var reloaded = ImageIO.read(saved);
                HistoryRasterAudit.rectanglesOnlyCoverTheirAwardedPointsCells(spec, baseline, reloaded);
                if (players == 4 && language == HowToPlay.Language.RO && scale == 1.5f)
                    publish(pattern.name().toLowerCase(Locale.ROOT), spec, reloaded);
            }
        }
    }

    @Test public void rasterSeparatorsUseTheThirtyPercentThinnerDpPolicyAtFractionalDensities() throws Exception {
        float[] density = {1, 1.5f, 2, 2.25f, 2.625f, 2.75f, 3.5f};
        int[] pixels = {2, 3, 4, 5, 6, 6, 7};
        for (int players = 3; players <= 6; players++) {
            var game = AwardRasterFixture.create(players, AwardRasterFixture.Pattern.ISOLATED_BONUS);
            for (int index = 0; index < density.length; index++) {
                var spec = raster(game, HowToPlay.Language.RO, density[index], 1);
                assertEquals(pixels[index], spec.grid().separator());
                HistoryRasterAudit.separatorPixels(spec, HistoryRaster.render(spec, true), pixels[index]);
            }
        }
    }

    private static HistoryRaster.Spec raster(GameSession game, HowToPlay.Language language, float density, float scale) {
        return HistoryRasterProjection.around(game, language, density, scale, game.snapshot().handIndex());
    }

    private static void publish(String name, HistoryRaster.Spec spec, java.awt.image.BufferedImage image) throws Exception {
        String configured = System.getProperty("whist.historyRasterOutput", "");
        if (configured.isBlank()) return;
        Path output = Path.of(configured);Files.createDirectories(output);
        assertTrue(ImageIO.write(image, "png", output.resolve("history-" + name + ".png").toFile()));
        Files.writeString(output.resolve("history-" + name + ".txt"),
                "Controlled JVM component raster; not an Android screenshot.\nplayers=" + spec.players()
                        + "\ngrid=" + spec.grid() + "\nrows=" + spec.rows() + "\n");
    }
}
