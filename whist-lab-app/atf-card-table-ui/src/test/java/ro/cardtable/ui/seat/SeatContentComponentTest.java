package ro.cardtable.ui.seat;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static atf.gradle.audit.assertions.GradleAssertions.*;

/** The renderer receives facts; it has no interpretation of bids, points or chips. */
public final class SeatContentComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void orderedFactsAreImmutableAndIndependentOfTheirGameMeaning() {
        for (var labels : List.of(List.of("Bid:", "Won:"), List.of("Points:", "Total:"), List.of("Stack:", "Bet:"))) {
            var facts = new ArrayList<>(List.of(new SeatContent.Fact(labels.get(0), "−25"), new SeatContent.Fact(labels.get(1), "0")));
            var seat = new SeatContent(2, "Ana", facts, true, "Actor two");
            facts.clear();
            
            atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "facts", facts, "labels", labels, "seat", seat);
            assertEquals("seat.facts().stream().map(SeatContent.Fact::label).toList() must equal labels", labels, seat.facts().stream().map(SeatContent.Fact::label).toList());
            assertEquals("seat.facts().stream().map(SeatContent.Fact::value).toList() must equal List.of(\"\\u221225\", \"0\")", List.of("−25", "0"), seat.facts().stream().map(SeatContent.Fact::value).toList());
            assertEquals("seat.seat() must equal 2", 2, seat.seat());
            assertEquals("seat.name() must equal \"Ana\"", "Ana", seat.name());
            assertThrows("The operation rejects its input with UnsupportedOperationException.class", UnsupportedOperationException.class, () -> seat.facts().clear());
        }
    }

    @Test public void emptyFactsAreValidAndInvalidIdentityOrMissingTextIsRejected() {
        
        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 0, "declared constant 2", "You", "declared constant 3", false, "declared constant 4", "Waiting", "declared constant 5", -1, "declared constant 6", "Total:");
        assertTrue("new SeatContent(0, \"You\", List.of(), false, \"Waiting\").facts().isEmpty() must hold", new SeatContent(0, "You", List.of(), false, "Waiting").facts().isEmpty());
        assertThrows("The operation rejects its input with IllegalArgumentException.class", IllegalArgumentException.class, () -> new SeatContent(-1, "You", List.of(), false, "Waiting"));
        assertThrows("The operation rejects its input with NullPointerException.class", NullPointerException.class, () -> new SeatContent.Fact("Total:", null));
        assertThrows("The operation rejects its input with NullPointerException.class", NullPointerException.class, () -> new SeatContent(0, null, List.of(), false, "Waiting"));
    }
}
