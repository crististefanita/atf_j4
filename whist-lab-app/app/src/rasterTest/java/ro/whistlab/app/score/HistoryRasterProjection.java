package ro.whistlab.app.score;

import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.app.score.raster.HistoryRaster;
import ro.whistlab.core.rules.HowToPlay;
import ro.whistlab.core.session.GameSession;
import ro.whistlab.core.session.ScoreHistory;

/** Test-only bridge to the package-owned projection; raster tests do not widen the production API. */
public final class HistoryRasterProjection {
    public static HistoryRaster.Spec all(GameSession.Snapshot state, ScoreHistory history, HowToPlay.Language language,
                                         float density, float scale) {
        var copy = new WhistUiText(language);
        var model = ScoreHistoryTable.forMatch(state, history, copy);
        var fonts = new HistoryRaster.Fonts(density, scale);
        var measured = ScoreHistoryMeasure.measure(model, new ScoreHistoryMeasure.Labels(copy.cardsHeading(),
                copy.historyBidHeading(), copy.historyPointsHeading()), fonts::valueWidth, fonts::captionWidth, fonts.unit());
        var grid = measured.fit(ScoreHistoryGridMetrics.forDensity(model.players().size(), density));
        var rows = model.rows().stream().map(row -> new HistoryRaster.Row(row.hand(), row.cardLabel(), row.status().name(),
                row.cells().stream().map(cell -> new HistoryRaster.Cell(cell.bid(), cell.displayPoints(), cell.points(),
                        cell.seriesPoints(), cell.outcome() == ScoreHistory.Outcome.MISSED)).toList())).toList();
        return new HistoryRaster.Spec(model.players(), rows, new HistoryRaster.Grid(grid.handWidthPx(), grid.bidWidthPx(),
                grid.pointsWidthPx(), grid.separatorWidthPx(), grid.outerPaddingPx()), fonts,
                copy.cardsHeading(), copy.historyBidHeading(), copy.historyPointsHeading());
    }

    public static HistoryRaster.Spec around(GameSession game, HowToPlay.Language language, float density, float scale,
                                            int center) {
        var all = all(game.snapshot(), game.scoreHistory(), language, density, scale);
        if (center < 0 || center >= all.rows().size()) throw new IllegalArgumentException("History centre outside schedule");
        int first = Math.max(0, center - 1), last = Math.min(all.rows().size(), center + 2);
        return new HistoryRaster.Spec(all.players(), all.rows().subList(first, last), all.grid(), all.fonts(),
                all.cardsHeading(), all.bidHeading(), all.pointsHeading());
    }

    public static HistoryRaster.Spec fitWidth(HistoryRaster.Spec spec, int viewportWidth) {
        var original = spec.grid();
        var grid = new ScoreHistoryGridMetrics(original.hand(), original.bid(), original.points(), original.separator(),
                original.padding(), spec.players().size()).fitViewport(viewportWidth);
        return new HistoryRaster.Spec(spec.players(), spec.rows(), new HistoryRaster.Grid(grid.handWidthPx(), grid.bidWidthPx(),
                grid.pointsWidthPx(), grid.separatorWidthPx(), grid.outerPaddingPx()), spec.fonts(),
                spec.cardsHeading(), spec.bidHeading(), spec.pointsHeading());
    }
    private HistoryRasterProjection() { }
}
