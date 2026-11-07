package ro.cardtable.ui.table;

import java.util.List;
import org.junit.Test;
import static atf.gradle.audit.assertions.GradleAssertions.*;

public final class TablePresentationComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void perimeterKeepsHumanBelowAndSeatOrderStableWithoutAssumingTheActor() {
        for (int count = 3; count <= 6; count++) {
            var plan = TableGeometry.arrangeDisplay(360, 500, count, 90, 60, 300, 180, 1, true,
                    TableGeometry.CenterAlignment.BALANCED);
            
            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "count", count, "plan", plan);
            assertEquals("plan.seats().size() must equal count", count, plan.seats().size());
            assertEquals("plan.seats().get(0).left() must equal 135", 135, plan.seats().get(0).left());
            assertEquals("plan.seats().get(0).bottom() must equal 500", 500, plan.seats().get(0).bottom());
            assertEquals("TableSeatLayout.positions(count)[0] must equal TableSeatLayout.Anchor.SOUTH", TableSeatLayout.Anchor.SOUTH, TableSeatLayout.positions(count)[0]);
            assertThrows("The operation rejects its input with UnsupportedOperationException.class", UnsupportedOperationException.class, () -> plan.seats().clear());
            for (var box : plan.seats()) {
                assertTrue("box.top() >= 0 && box.bottom() <= 500 must hold", box.top() >= 0 && box.bottom() <= 500);
                assertFalse("box.overlaps(plan.center()) must remain false", box.overlaps(plan.center()));
            }
        }
    }

    @Test public void publicCardIdentityAndCompactSizingAreSeparateFromAnyGameRules() {
        var play = new PlayedCardContent(2, "A♠", false, "Ana", "Ana played A♠");
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "play", play);
        assertEquals("play.seat() must equal 2", 2, play.seat());
        assertEquals("play.faceLabel() must equal \"A\\u2660\"", "A♠", play.faceLabel());
        for (int count = 3; count <= 6; count++) {
            var layout = PlayedCardMetrics.forTable(count, 360, 1f, new TableViewportPolicy(569));
            assertEquals("layout.cardHeightDp() must equal 64", 64, layout.cardHeightDp());
            assertEquals("layout.cardWidthDp() must equal 46", 46, layout.cardWidthDp());
            assertEquals("layout.slots().size() must equal count", count, layout.slots().size());
            for (int seat = 0; seat < count; seat++) assertEquals("layout.slotForSeat(seat).seat() must equal seat", seat, layout.slotForSeat(seat).seat());
        }
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> new PlayedCardContent(-1, "A♠", false, "Ana", ""));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> TableSeatLayout.positions(2));
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> TableGeometry.arrange(10, 10, 4, 90, 60, 300, 180, 1));
    }
}
