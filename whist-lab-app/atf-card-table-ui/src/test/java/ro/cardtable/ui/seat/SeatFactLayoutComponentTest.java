package ro.cardtable.ui.seat;

import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import java.util.List;
import org.junit.Test;
import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;

public final class SeatFactLayoutComponentTest extends GradleAuditedTest {
    private record Widths(int available, int label, int value, int reserved, boolean stacked) { }

    @Test public void fitsExactWidthAndStacksOnlyWhenContentNeedsMoreSpace() {
        var cases = List.of(new Widths(100, 54, 42, 0, false), new Widths(99, 54, 42, 0, true),
                new Widths(100, 40, 48, 0, false), new Widths(100, 59, 48, 0, true),
                new Widths(156, 81, 71, 0, false), new Widths(155, 81, 71, 0, true));
        for (var widths : cases) {
            GradleInputs.fixture("Measured label, value and available seat space", "widths", widths, "gap", 4);
            assertEquals("Stack without clipping when actual text exceeds the available width", widths.stacked(),
                    SeatFactLayout.stacks(widths.available(), widths.label(), widths.value(), widths.reserved(), 4));
        }
    }

    @Test public void keepsLayoutStableWhileTheReservedValueWidthCanStillFitLargerAmounts() {
        GradleInputs.fixture("One through four displayed digits", "available", 99, "label", 54,
                "reserved width", 42, "gap", 4);
        for (int value : List.of(11, 22, 33, 42)) {
            assertEquals("Changing an amount within its reserved range preserves a stacked row", true,
                    SeatFactLayout.stacks(99, 54, value, 42, 4));
        }
        assertEquals("The reserved range can fit at its exact boundary", false,
                SeatFactLayout.stacks(100, 54, 11, 42, 4));
        assertEquals("A longer value can exceed its reserved width", true,
                SeatFactLayout.stacks(100, 54, 43, 42, 4));
    }
}
