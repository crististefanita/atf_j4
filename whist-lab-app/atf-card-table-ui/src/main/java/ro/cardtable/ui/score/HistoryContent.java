package ro.cardtable.ui.score;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Objects;

/** Pinned row labels and repeated player columns; column meanings belong to the consumer. */
public record HistoryContent(String corner, String legend, String detailsLabel, List<Player> players,
        List<String> columns, List<Row> rows) {
    public record Player(String id, String label) {
        public Player {
            if (Objects.requireNonNull(id).isBlank()) throw new IllegalArgumentException("Stable player identity required");
            Objects.requireNonNull(label);
        }
    }
    public record Row(String id, String label, String description, List<List<ScoreTableContent.Cell>> values) {
        public Row {
            if (Objects.requireNonNull(id).isBlank()) throw new IllegalArgumentException("Stable row identity required");
            Objects.requireNonNull(label); Objects.requireNonNull(description);
            values = List.copyOf(values.stream().map(List::copyOf).collect(Collectors.toList()));
        }
    }
    public HistoryContent {
        Objects.requireNonNull(corner); Objects.requireNonNull(legend); Objects.requireNonNull(detailsLabel);
        players = List.copyOf(players); columns = List.copyOf(columns); rows = List.copyOf(rows);
        if (players.isEmpty() || columns.isEmpty()) throw new IllegalArgumentException("Player and column groups required");
        var ids = new HashSet<String>();
        for (var player : players) if (!ids.add(player.id())) throw new IllegalArgumentException("Duplicate player identity");
        ids.clear();
        int columnCount = columns.size();
        for (var row : rows) {
            if (!ids.add(row.id()) || row.values().size() != players.size()
                    || row.values().stream().anyMatch(cells -> cells.size() != columnCount))
                throw new IllegalArgumentException("Unique history rows must match every player and column");
        }
    }
}
