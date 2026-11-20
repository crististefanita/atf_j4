package ro.cardtable.ui.menu;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static atf.gradle.audit.assertions.GradleAssertions.*;

public final class TableMenuContentComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void descriptorsBindLabelsOrderAndOnlyTheSelectedPlatformAction() {
        var entries = List.of(new TableMenuContent.Entry("home", "Main menu"), new TableMenuContent.Entry("name", "Name"));
        var calls = new ArrayList<String>();
        atf.gradle.audit.GradleInputs.fixture("Shared menu descriptors", "entries", entries);
        var bound = TableMenuContent.bind(entries, java.util.Map.of("name", () -> calls.add("name"), "home", () -> calls.add("home")));
        assertEquals("Native descriptor order wins over callback map order", List.of("home", "name"), bound.stream().map(TableMenuContent.Item::id).toList());
        assertEquals("Localized labels are preserved", "Name", bound.get(1).label());
        assertTrue("Binding does not execute a route", calls.isEmpty());
        bound.get(1).action().run(); assertEquals("Only the selected route executes", List.of("name"), calls);
        assertThrows("Unknown callbacks are refused", IllegalArgumentException.class, () -> TableMenuContent.bind(entries, java.util.Map.of()));
        assertThrows("Duplicate descriptor identities are refused", IllegalArgumentException.class,
                () -> TableMenuContent.bind(List.of(entries.get(0), entries.get(0)), java.util.Map.of("home", () -> { })));
    }

    @Test public void historyIsDirectAndDispatchesOnlyItsOwnActionInBothLanguages() {
        for (String label : List.of("Score history", "Istoricul scorului")) {
            var calls = new ArrayList<String>();
            var items = TableMenuContent.historyFirst(label, () -> calls.add("history"),
                    List.of(new TableMenuContent.Item("stop-auto", "Stop", () -> calls.add("stop")),
                            new TableMenuContent.Item("home", "Home", () -> calls.add("home"))));
            atf.gradle.audit.GradleInputs.fixture("Public table menu routes", "label", label, "items", items);
            assertEquals("History has one stable entry before optional routes",
                    List.of("score-history", "stop-auto", "home"), items.stream().map(TableMenuContent.Item::id).toList());
            assertEquals("History remains localized", label, items.get(0).label());
            assertTrue("Constructing or reopening a menu does not navigate", calls.isEmpty());
            items.get(0).action().run();
            assertEquals("One selection emits only the history intention", List.of("history"), calls);
            assertThrows("The renderer cannot mutate the navigation plan", UnsupportedOperationException.class, items::clear);
        }
    }

    @Test public void duplicateHistoryAndMissingNavigationAreRejected() {
        var history = new TableMenuContent.Item(TableMenuContent.HISTORY, "History", () -> { });
        atf.gradle.audit.GradleInputs.fixture("Invalid menu plans", "duplicate", history);
        assertThrows("History cannot be added twice", IllegalArgumentException.class,
                () -> TableMenuContent.historyFirst("History", () -> { }, List.of(history)));
        assertThrows("A visible history entry requires an action", NullPointerException.class,
                () -> TableMenuContent.historyFirst("History", null, List.of()));
    }
}
