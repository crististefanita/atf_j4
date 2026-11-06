package ro.whistlab.app.score;

import java.util.Objects;
import java.util.function.ToIntFunction;
import ro.whistlab.app.score.award.SeriesPointsFrame;

/** Measures the full projection with injected glyph widths; Android supplies the font implementation. */
record ScoreHistoryMeasure(int hand, int bid, int points, int name) {
    record Labels(String cards, String bid, String points) { }

    static ScoreHistoryMeasure measure(ScoreHistoryTable table, Labels labels,
            ToIntFunction<String> values, ToIntFunction<String> captions, int sidePadding) {
        Objects.requireNonNull(table); Objects.requireNonNull(labels);
        Objects.requireNonNull(values); Objects.requireNonNull(captions);
        if (sidePadding < 0) throw new IllegalArgumentException("Text padding cannot be negative");
        try {
            int padding = Math.multiplyExact(2, sidePadding);
            int hand = width(captions, labels.cards(), padding);
            int bid = width(values, labels.bid(), padding);
            int points = width(values, labels.points(), padding);
            int name = 0;
            for (String player : table.players()) name = Math.max(name, width(values, player, padding));
            for (var row : table.rows()) {
                hand = Math.max(hand, width(values, row.cardLabel(), padding));
                for (var cell : row.cells()) {
                    bid = Math.max(bid, width(values, cell.bid(), Math.multiplyExact(2, padding)));
                    int numeric = width(values, cell.displayPoints(), 0);
                    int required = cell.boxPoints() ? SeriesPointsFrame.requiredWidth(numeric, sidePadding)
                            : Math.addExact(numeric, padding);
                    points = Math.max(points, required);
                }
            }
            return new ScoreHistoryMeasure(hand, bid, points, name);
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("Text measurements exceed the pixel range", overflow);
        }
    }

    ScoreHistoryGridMetrics fit(ScoreHistoryGridMetrics preferred) {
        return preferred.fitContent(hand, bid, points, name);
    }

    private static int width(ToIntFunction<String> measure, String text, int padding) {
        int width = measure.applyAsInt(Objects.requireNonNull(text));
        if (width < 0) throw new IllegalArgumentException("A glyph measurement cannot be negative");
        return Math.addExact(width, padding);
    }
}
