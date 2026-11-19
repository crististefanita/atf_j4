package ro.cardtable.ui.score;

import java.util.ArrayList;
import java.util.List;

/** Pixel measurements: numeric minima never shrink; overflow belongs to synchronized panes. */
public record HistoryGridMetrics(int label, List<Integer> columns, int separator, int players) {
    public HistoryGridMetrics {
        columns = List.copyOf(columns);
        if (label < 1 || columns.isEmpty() || columns.stream().anyMatch(n -> n < 1) || separator < 0 || players < 1)
            throw new IllegalArgumentException("Positive measured dimensions required");
    }
    public static HistoryGridMetrics fit(int viewport, int label, int name, List<Integer> minima, int separator, int players) {
        if (viewport < 2 || label < 1 || name < 0 || minima.isEmpty() || players < 1 || separator < 0
                || minima.stream().anyMatch(n -> n < 1)) throw new IllegalArgumentException("Invalid history measurement");
        int pinned = Math.min(label, Math.max(1, (int) (viewport * .4)));
        int minimum = minima.stream().reduce(0, Math::addExact);
        int available = (viewport - pinned) / players - separator;
        int group = Math.max(minimum, Math.max(Math.min(name, viewport - pinned - separator), available));
        int spare = group - minimum;
        var widths = new ArrayList<Integer>();
        for (int i = 0; i < minima.size(); i++) {
            widths.add(minima.get(i) + spare / minima.size() + (i >= minima.size() - spare % minima.size() ? 1 : 0));
        }
        return new HistoryGridMetrics(pinned, widths, separator, players);
    }
    public int playerWidth() { return Math.addExact(separator, columns.stream().reduce(0, Math::addExact)); }
    public int scoresWidth() { return Math.multiplyExact(players, playerWidth()); }
    public static int bodyHeight(int available, int header, int legend, int rows, int firstRow) {
        if (header < 0 || legend < 0 || rows < 0 || firstRow < 0) throw new IllegalArgumentException("Negative measured height");
        if (rows == 0) return 0;
        long remainder = (long) available - header - legend;
        return remainder < firstRow ? rows : (int) Math.min(rows, remainder);
    }
}
