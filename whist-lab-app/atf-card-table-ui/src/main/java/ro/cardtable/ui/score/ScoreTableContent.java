package ro.cardtable.ui.score;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Visible table data only: no scoring, sorting, player selection or persistence. */
public record ScoreTableContent(List<Column> columns, List<Row> rows) {
    public record Column(String label, float weight, boolean numeric) {
        public Column {
            Objects.requireNonNull(label);
            if (!Float.isFinite(weight) || weight <= 0) throw new IllegalArgumentException("Positive column weight required");
        }
    }
    public record Cell(String text, String description, boolean underlined, boolean emphasized) {
        public Cell { Objects.requireNonNull(text); Objects.requireNonNull(description); }
        public Cell(String text) { this(text, text, false, false); }
    }
    public record Row(String id, List<Cell> cells, boolean highlighted) {
        public Row {
            if (Objects.requireNonNull(id).isBlank()) throw new IllegalArgumentException("Stable row identity required");
            cells = List.copyOf(cells);
        }
    }
    public ScoreTableContent {
        columns = List.copyOf(columns); rows = List.copyOf(rows);
        if (columns.isEmpty()) throw new IllegalArgumentException("Columns required");
        var ids = new HashSet<String>();
        for (var row : rows) {
            if (row.cells().size() != columns.size() || !ids.add(row.id()))
                throw new IllegalArgumentException("Unique rows must match the column contract");
        }
    }
}
