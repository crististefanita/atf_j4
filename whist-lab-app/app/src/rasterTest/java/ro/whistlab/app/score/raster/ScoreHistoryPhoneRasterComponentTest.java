package ro.whistlab.app.score.raster;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.Test;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.app.score.HistoryRasterProjection;
import ro.whistlab.app.score.series.AwardRasterFixture;
import ro.whistlab.core.rules.HowToPlay;

/** Declared phone profile; a resized attachment alone cannot establish the physical display density. */
public final class ScoreHistoryPhoneRasterComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void tallPhoneWithFractionalDensityKeepsFullRowsAndUsesTheAvailableWidth() throws Exception {
        for (int players = 3; players <= 6; players++) {
            var game = AwardRasterFixture.create(players, AwardRasterFixture.Pattern.BONUS_AND_PENALTY);
            for (var language : HowToPlay.Language.values()) for (float scale : new float[]{1, 1.3f, 1.5f}) {
                var copy = new WhistUiText(language);
                var source = HistoryRasterProjection.all(game.snapshot(), game.scoreHistory(), language, 2.75f, scale);
                var result = HistoryViewportRaster.render(source, 1080, 2448, 66, 132,
                        copy.scoreHistory(), copy.historyLegend(), copy.close());
                if (players == 4 && scale == 1.3f) atf.gradle.audit.GradleInputs.image(
                        "Synthetic tall-phone history / " + language + " / font 1.3", result.image());

                atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "copy", copy, "game", game, "language", language, "players", players, "result", result, "scale", scale, "source", source);
                assertEquals("result.image().getWidth() must equal 1080", 1080, result.image().getWidth());
                assertEquals("result.image().getHeight() must equal 2448", 2448, result.image().getHeight());
                assertFalse("result.wholeScroll() must remain false", result.wholeScroll());
                assertTrue("result.dialog().contains(result.viewport()) must hold", result.dialog().contains(result.viewport()));
                assertTrue("result.dialog().contains(result.legend()) must hold", result.dialog().contains(result.legend()));
                assertTrue("result.bodyHeight() >= source.fonts().rowHeight() must hold", result.bodyHeight() >= source.fonts().rowHeight());
                assertEquals("result.table().grid().width(players) must equal Math.max(result.viewport().width, source.grid().width(players))", Math.max(result.viewport().width, source.grid().width(players)), result.table().grid().width(players));
                assertEquals("result.legend().y + result.legend().height must equal result.closeTop()", result.closeTop(), result.legend().y + result.legend().height);
                String configured = System.getProperty("whist.historyRasterOutput", "");
                if (!configured.isBlank() && players == 4 && scale == 1.3f) {
                    Path output = Path.of(configured); Files.createDirectories(output);
                    assertTrue("ImageIO.write(result.image(), \"png\", output.resolve(\"history-1080x2448-4p-\" + language + \"-1.3.png\").toFile()) must hold", ImageIO.write(result.image(), "png", output.resolve("history-1080x2448-4p-" + language + "-1.3.png").toFile()));
                }
            }
        }
    }
}
