package ro.whistlab.app.score.raster;

import static org.junit.Assert.*;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.Test;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.app.score.HistoryRasterProjection;
import ro.whistlab.app.score.series.AwardRasterFixture;
import ro.whistlab.core.rules.HowToPlay;

/** Declared phone profile; a resized attachment alone cannot establish the physical display density. */
public final class ScoreHistoryPhoneRasterComponentTest {
    @Test public void tallPhoneWithFractionalDensityKeepsFullRowsAndUsesTheAvailableWidth() throws Exception {
        for (int players = 3; players <= 6; players++) {
            var game = AwardRasterFixture.create(players, AwardRasterFixture.Pattern.BONUS_AND_PENALTY);
            for (var language : HowToPlay.Language.values()) for (float scale : new float[]{1, 1.3f, 1.5f}) {
                var copy = new WhistUiText(language);
                var source = HistoryRasterProjection.all(game.snapshot(), game.scoreHistory(), language, 2.75f, scale);
                var result = HistoryViewportRaster.render(source, 1080, 2448, 66, 132,
                        copy.scoreHistory(), copy.historyLegend(), copy.close());
                assertEquals(1080, result.image().getWidth());
                assertEquals(2448, result.image().getHeight());
                assertFalse(result.wholeScroll());
                assertTrue(result.dialog().contains(result.viewport()));
                assertTrue(result.dialog().contains(result.legend()));
                assertTrue(result.bodyHeight() >= source.fonts().rowHeight());
                assertEquals(Math.max(result.viewport().width, source.grid().width(players)), result.table().grid().width(players));
                assertEquals(result.closeTop(), result.legend().y + result.legend().height);
                String configured = System.getProperty("whist.historyRasterOutput", "");
                if (!configured.isBlank() && players == 4 && scale == 1.3f) {
                    Path output = Path.of(configured); Files.createDirectories(output);
                    assertTrue(ImageIO.write(result.image(), "png", output.resolve("history-1080x2448-4p-" + language + "-1.3.png").toFile()));
                }
            }
        }
    }
}
