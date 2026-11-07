package atf.gradle.audit.matrix;

import atf.gradle.audit.GradleInputs;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import static atf.gradle.audit.assertions.GradleAssertions.*;

/** Test input composition: evaluate every matrix pair, retain coverage and the exact first mismatch. */
public final class ExhaustiveMatrixChecks {
    private final Map<String, Long> evaluated = new LinkedHashMap<>();
    private String location = "match";
    private long mismatches;
    public long checkedPairs() { return evaluated.values().stream().mapToLong(Long::longValue).sum(); }
    public void at(String location) { this.location = location; }
    public void equal(String invariant, Object expected, Object actual) {
        evaluated.merge(invariant, 1L, Math::addExact);
        if (!Objects.deepEquals(expected, actual)) {
            mismatches++;
            GradleInputs.fixture("First matrix mismatch", "location", location, "invariant", invariant,
                    "expected", expected, "actual", actual, "evaluated before mismatch", evaluated);
            assertEquals(location + " / " + invariant, expected, actual);
        }
    }
    public void within(String invariant, double expected, double actual, double delta) {
        evaluated.merge(invariant, 1L, Math::addExact);
        if (!(Double.compare(expected, actual) == 0 || Math.abs(expected - actual) <= delta)) {
            mismatches++;
            GradleInputs.fixture("First numeric matrix mismatch", "location", location, "invariant", invariant,
                    "expected", expected, "actual", actual, "delta", delta, "evaluated before mismatch", evaluated);
            assertEquals(location + " / " + invariant, expected, actual, delta);
        }
    }
    public void publish() {
        GradleInputs.fixture("Exhaustive matrix coverage", "evaluated pairs by invariant", evaluated,
                "publication", "All pairs evaluated; passing pairs aggregated by invariant; first mismatch fails immediately");
        assertFalse("The exhaustive oracle evaluated at least one invariant", evaluated.isEmpty());
        assertEquals("No pair differs from the declared independent oracle", 0L, mismatches);
    }
}
