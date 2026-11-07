package ro.whistlab.app.score.raster;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.Test;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.app.score.HistoryRasterProjection;
import ro.whistlab.app.score.series.AwardRasterFixture;
import ro.whistlab.core.rules.HowToPlay;

public final class ScoreHistoryResponsiveRasterComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void viewportsFillTheirWindowKeepTheLegendVisibleAndRetainEverySeat() throws Exception {
        int[][] profiles = {{320,568,1,24,32}, {678,1536,2,48,96}, {1080,2400,3,72,144},
                {1536,678,2,48,72}, {1600,2560,2,48,96}};
        for (int players = 3; players <= 6; players++) {
            var game = AwardRasterFixture.create(players, AwardRasterFixture.Pattern.BONUS_AND_PENALTY);
            for (var language : HowToPlay.Language.values()) for (float scale : new float[]{1, 1.5f}) {
                var copy = new WhistUiText(language);
                for (int[] profile : profiles) {
                    var source = HistoryRasterProjection.all(game.snapshot(), game.scoreHistory(), language, profile[2], scale);
                    var result = HistoryViewportRaster.render(source, profile[0], profile[1], profile[3], profile[4],
                            copy.scoreHistory(), copy.historyLegend(), copy.close());
                    var viewport = result.viewport();

                    atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "copy", copy, "game", game, "language", language, "players", players, "profile", profile, "profiles", profiles, "result", result, "scale", scale, "source", source, "viewport", viewport);
                    assertTrue("result.dialog().contains(viewport) must hold", result.dialog().contains(viewport));
                    assertTrue("The body must hold complete rows", result.bodyHeight() >= source.fonts().rowHeight());
                    var end = HistoryViewportRaster.render(source, profile[0], profile[1], profile[3], profile[4],
                            copy.scoreHistory(), copy.historyLegend(), copy.close(), true);
                    assertTrue("The complete legend is reachable", end.dialog().contains(end.legend()));
                    assertEquals("No fixed-height gap remains above Close", end.closeTop(), end.legend().y + end.legend().height);
                    if (result.wholeScroll()) {
                        assertTrue("result.maximumScroll() > 0 must hold", result.maximumScroll() > 0);
                        assertTrue("result.viewport().height >= 3 * source.fonts().rowHeight() must hold", result.viewport().height >= 3 * source.fonts().rowHeight());
                    } else assertTrue("result.dialog().contains(result.legend()) must hold", result.dialog().contains(result.legend()));
                    assertEquals("result.table().players().size() must equal players", players, result.table().players().size());
                    assertEquals("result.table().grid().width(players) must equal Math.max(viewport.width, source.grid().width(players))", Math.max(viewport.width, source.grid().width(players)), result.table().grid().width(players));
                    if (source.grid().width(players) <= viewport.width)
                        assertEquals("Header background reaches the right table edge", 0xffdfeada,
                                result.image().getRGB(viewport.x + viewport.width - 1, viewport.y + 1));
                    if ((players == 4 && profile[0] == 678) || (players == 6 && profile[0] == 1536))
                        publish("history-" + profile[0] + "x" + profile[1] + "-" + players + "p-" + language + "-" + scale, result.image());
                    if (players == 6 && profile[0] == 1536 && result.wholeScroll())
                        publish("history-" + profile[0] + "x" + profile[1] + "-" + players + "p-" + language + "-" + scale + "-end", end.image());
                }
            }
        }
    }

    @Test public void redDiagonalPixelsBelongOnlyToMissedBidsForEverySeatAndTextScale() throws Exception {
        atf.gradle.audit.GradleInputs.fixture("Missed-bid raster domain", "players inclusive", new int[]{3, 6},
                "missed seats", "every seat at each table", "languages", HowToPlay.Language.values(),
                "font scales", new float[]{1, 1.5f}, "density", 2, "fit width", 600,
                "bid variants", new String[]{"original", "zero"});
        var checks = new atf.gradle.audit.matrix.ExhaustiveMatrixChecks();
        int rasters = 0;
        for (int players = 3; players <= 6; players++) {
            for (int missedSeat = 0; missedSeat < players; missedSeat++) {
                var game = AwardRasterFixture.create(players, AwardRasterFixture.Pattern.BONUS_AND_PENALTY, missedSeat);
                for (var language : HowToPlay.Language.values()) for (float scale : new float[]{1, 1.5f}) {
                    var source = HistoryRasterProjection.all(game.snapshot(), game.scoreHistory(), language, 2, scale);
                    var spec = HistoryRasterProjection.fitWidth(source, 600);
                    String location = "players=" + players + ", missed seat=" + missedSeat + ", language=" + language + ", scale=" + scale;
                    verifyStrikes(spec, HistoryRaster.render(spec, true, false), HistoryRaster.render(spec, true, true), checks, location + ", original bids");
                    var zero = zeroBids(spec);
                    verifyStrikes(zero, HistoryRaster.render(zero, true, false), HistoryRaster.render(zero, true, true), checks, location + ", zero bids");
                    rasters += 2;
                }
            }
        }
        assertEquals("Every seat, language, text scale and bid variant was rendered", 144, rasters);
        checks.publish();
    }

    /** Isolated drawing input: zero remains a real glyph, rather than an unknown or empty bid. */
    private static HistoryRaster.Spec zeroBids(HistoryRaster.Spec spec) {
        var rows = spec.rows().stream().map(row -> new HistoryRaster.Row(row.hand(), row.cards(), row.status(),
                row.cells().stream().map(cell -> cell.missed()
                        ? new HistoryRaster.Cell("0", cell.points(), cell.handPoints(), cell.series(), true) : cell).toList())).toList();
        return new HistoryRaster.Spec(spec.players(), rows, spec.grid(), spec.fonts(),
                spec.cardsHeading(), spec.bidHeading(), spec.pointsHeading());
    }

    private static void verifyStrikes(HistoryRaster.Spec spec, BufferedImage before, BufferedImage after,
                                      atf.gradle.audit.matrix.ExhaustiveMatrixChecks checks, String location) {
        int rowHeight = spec.fonts().rowHeight(), seats = spec.players().size();
        int[][] count = new int[spec.rows().size()][seats];
        long[][] leftY = new long[count.length][seats], rightY = new long[count.length][seats];
        int[][] leftCount = new int[count.length][seats], rightCount = new int[count.length][seats];
        for (int y = 0; y < after.getHeight(); y++) for (int x = 0; x < after.getWidth(); x++) {
            if (before.getRGB(x, y) == after.getRGB(x, y)) continue;
            checks.at(location + ", pixel=" + x + "," + y);
            checks.equal("A missed bid gets an explicit red stroke", 0xffb3261e, after.getRGB(x, y));
            int row = y / rowHeight - 2;
            checks.equal("Strikes cannot affect the headers", true, row >= 0 && row < count.length);
            int owner = -1;
            for (int seat = 0; seat < seats; seat++) {
                int left = spec.grid().pointsLeft(seat) - spec.grid().bid();
                if (x >= left && x < left + spec.grid().bid()) owner = seat;
            }
            checks.equal("Strike cannot enter Points or another cell", true, owner >= 0);
            checks.equal("Only a missed contract is struck", true, spec.rows().get(row).cells().get(owner).missed());
            count[row][owner]++;
            int middle = spec.grid().pointsLeft(owner) - spec.grid().bid() / 2;
            if (x < middle) { leftY[row][owner] += y; leftCount[row][owner]++; }
            else { rightY[row][owner] += y; rightCount[row][owner]++; }
        }
        for (int row = 0; row < count.length; row++) for (int seat = 0; seat < seats; seat++) {
            checks.at(location + ", row=" + row + ", seat=" + seat);
            if (!spec.rows().get(row).cells().get(seat).missed()) { checks.equal("Unmissed bids remain unchanged", 0, count[row][seat]); continue; }
            checks.equal("Every player's missed bid has visible ink", true, count[row][seat] > 0);
            checks.equal("Both halves of the stroke are visible", true, leftCount[row][seat] > 0 && rightCount[row][seat] > 0);
            checks.equal("The stroke rises diagonally, rather than becoming a horizontal strikethrough", true,
                    leftY[row][seat] / (double) leftCount[row][seat] > rightY[row][seat] / (double) rightCount[row][seat] + 1);
        }
    }

    private static void publish(String name, BufferedImage image) throws Exception {
        String configured = System.getProperty("whist.historyRasterOutput", "");
        if (configured.isBlank()) return;
        Path output = Path.of(configured); Files.createDirectories(output);
        assertTrue("ImageIO.write(image, \"png\", output.resolve(name + \".png\").toFile()) must hold", ImageIO.write(image, "png", output.resolve(name + ".png").toFile()));
    }
}
