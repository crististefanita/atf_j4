package ro.whistlab.app.score.raster;

import static org.junit.Assert.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import ro.whistlab.app.persistence.GameSessionStore;
import ro.whistlab.app.score.HistoryRasterProjection;
import ro.whistlab.core.rules.HowToPlay;
import ro.whistlab.core.session.GameSession;

/** File-backed recovery and decoded PNG equality, with historical awards taken from the settled engine state. */
public final class HistoryRecoveryProof {
    public record Expected(int center, List<Integer> totals, List<Integer> series) {
        public Expected { totals = List.copyOf(totals); series = List.copyOf(series); }
        public static Expected current(GameSession game) {
            return new Expected(game.snapshot().handIndex(),
                    game.snapshot().players().stream().map(GameSession.PlayerSnapshot::totalScore).toList(),
                    game.snapshot().players().stream().map(GameSession.PlayerSnapshot::streakPoints).toList());
        }
    }

    public static GameSession verify(String id, GameSession source, Expected expected, Path temporary) throws Exception {
        var before = source.saveState();
        var file = Files.createTempFile(temporary, id + "-", ".json");
        Files.writeString(file, GameSessionStore.encode(before).toString());
        var restored = GameSessionStore.fromJson(Files.readString(file));
        assertEquals(id + " checkpoint", before, restored.saveState());
        for (var language : HowToPlay.Language.values()) for (float scale : new float[]{1, 1.5f}) {
            String profile = id + "-" + language.name().toLowerCase(Locale.ROOT) + "-" + scale;
            var first = HistoryRasterProjection.around(source, language, 2, scale, expected.center());
            var second = HistoryRasterProjection.around(restored, language, 2, scale, expected.center());
            assertEquals(profile, first.players(), second.players());
            assertEquals(profile, first.rows(), second.rows());
            assertEquals(profile, first.grid(), second.grid());
            var centre = second.rows().stream().filter(row -> row.hand() == expected.center() + 1).findFirst().orElseThrow();
            for (int seat = 0; seat < second.players().size(); seat++) {
                assertEquals(profile, expected.totals().get(seat).toString(), centre.cells().get(seat).points());
                assertEquals(profile, expected.series().get(seat).intValue(), centre.cells().get(seat).series());
            }
            for (var row : second.rows()) for (int seat = 0; seat < second.players().size(); seat++) {
                var cell = row.cells().get(seat);
                if (row.hand() <= restored.scoreHistory().completedHands())
                    assertEquals(profile, Integer.toString(restored.scoreHistory().total(row.hand(), seat)), cell.points());
                else { assertEquals(profile, "—", cell.points()); assertEquals(profile, 0, cell.series()); }
            }
            var baseline = HistoryRaster.render(first, false);
            var firstFile = Files.createTempFile(temporary, "before-", ".png");
            var secondFile = Files.createTempFile(temporary, "after-", ".png");
            assertTrue(ImageIO.write(HistoryRaster.render(first, true), "png", firstFile.toFile()));
            assertTrue(ImageIO.write(HistoryRaster.render(second, true), "png", secondFile.toFile()));
            var firstImage = ImageIO.read(firstFile.toFile());var secondImage = ImageIO.read(secondFile.toFile());
            HistoryRasterAudit.identicalPixels(profile, firstImage, secondImage);
            HistoryRasterAudit.rectanglesOnlyCoverTheirAwardedPointsCells(second, baseline, secondImage);
            HistoryRasterAudit.separatorPixels(second, secondImage, 4);
            String configured = System.getProperty("whist.historyRasterOutput", "");
            if (!configured.isBlank() && scale == 1.5f) {
                var output = Path.of(configured);Files.createDirectories(output);
                assertTrue(ImageIO.write(secondImage, "png", output.resolve(profile + ".png").toFile()));
                Files.writeString(output.resolve(profile + ".txt"), "CONTROLLED_JVM_RASTER_NOT_ANDROID_SCREENSHOT\n"
                        + "fileRecovery=PASS\npixelEquality=PASS\nphase=" + restored.snapshot().phase()
                        + "\nexpected=" + expected + "\nrows=" + second.rows() + "\n");
            }
        }
        assertEquals(id + " read-only projection", before, source.saveState());
        return restored;
    }
    private HistoryRecoveryProof() { }
}
