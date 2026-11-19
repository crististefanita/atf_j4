package ro.cardtable.ui.score;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static atf.gradle.audit.assertions.GradleAssertions.*;

public final class ScoreContentComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void columnRolesValuesAndRowIdentitiesBelongToEachAdapter() {
        atf.gradle.audit.GradleInputs.fixture("Consumer-owned score projections", "column families", List.of("Δ/Σ", "B/P", "Pot/Chips/Place"), "row identity", "hand:7");
        for (var captions : List.of(List.of("Δ", "Σ"), List.of("B", "P"), List.of("Pot", "Chips", "Place"))) {
            var cells = captions.stream().map(ScoreTableContent.Cell::new).toList();
            var groups = new ArrayList<List<ScoreTableContent.Cell>>(); groups.add(cells);
            var rows = new ArrayList<HistoryContent.Row>(); rows.add(new HistoryContent.Row("hand:7", "7 · game", "Details", groups));
            var model = new HistoryContent("Hand", "Adapter legend", "Open details",
                    List.of(new HistoryContent.Player("seat:0", "Alex")), captions, rows);
            groups.clear(); rows.clear();
            assertEquals("Column meanings survive " + captions, captions, model.columns());
            assertEquals("Stable hand identity survives " + captions, "hand:7", model.rows().get(0).id());
            assertEquals("Nested values are snapshots " + captions, cells, model.rows().get(0).values().get(0));
            assertThrows("No nested mutation " + captions, UnsupportedOperationException.class, () -> model.rows().get(0).values().get(0).clear());
        }
        var content = new ScoreTableContent(List.of(new ScoreTableContent.Column("Name", 2, false),
                new ScoreTableContent.Column("Δ", 1, true)), List.of(new ScoreTableContent.Row("seat:0",
                List.of(new ScoreTableContent.Cell("Ana"), new ScoreTableContent.Cell("-20", "minus twenty", true, false)), true)));
        assertEquals("Signed score is not interpreted or sorted", "-20", content.rows().get(0).cells().get(1).text());
        assertTrue("Underline is presentation supplied by the adapter", content.rows().get(0).cells().get(1).underlined());
        assertTrue("Human emphasis stays in the supplied row", content.rows().get(0).highlighted());
    }
    @Test public void malformedAndAmbiguousProjectionsFailBeforeRendering() {
        atf.gradle.audit.GradleInputs.fixture("Rejected projection shapes", "cases", List.of("duplicate row", "ragged cells", "NaN weight", "duplicate seat", "missing total"));
        var c = List.of(new ScoreTableContent.Column("Σ", 1, true));
        var row = new ScoreTableContent.Row("a", List.of(new ScoreTableContent.Cell("0")), false);
        assertThrows("Duplicate table identity", IllegalArgumentException.class, () -> new ScoreTableContent(c, List.of(row, row)));
        assertThrows("Ragged score row", IllegalArgumentException.class, () -> new ScoreTableContent(c, List.of(new ScoreTableContent.Row("b", List.of(), false))));
        assertThrows("Invalid column weight", IllegalArgumentException.class, () -> new ScoreTableContent.Column("Σ", Float.NaN, true));
        var player = new HistoryContent.Player("p", "Alex");
        assertThrows("Duplicate player identity even with valid names", IllegalArgumentException.class,
                () -> new HistoryContent("", "", "", List.of(player, player), List.of("Δ", "Σ"), List.of()));
        var wrong = new HistoryContent.Row("h", "", "", List.of(List.of(new ScoreTableContent.Cell("4"))));
        assertThrows("A player group cannot silently omit a column", IllegalArgumentException.class,
                () -> new HistoryContent("", "", "", List.of(player), List.of("Δ", "Σ"), List.of(wrong)));
        var empty = new HistoryContent("Hand", "No hands", "Open", List.of(player), List.of("Δ", "Σ"), List.of());
        assertEquals("An empty confirmed history is supported", List.of(), empty.rows());
    }
}
