package atf.gradle.audit.matrix;

import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import org.junit.Test;
import static atf.gradle.audit.assertions.GradleAssertions.*;

public final class ExhaustiveMatrixChecksComponentTest extends GradleAuditedTest {
    @Test public void everyPairIsEvaluatedAndAHiddenMismatchCannotPassTheSummary() {
        GradleInputs.fixture("Controlled ledger pairs", "expected", "12", "matching", "12", "mismatching", "13");
        var checks = new ExhaustiveMatrixChecks();
        checks.at("RO / hand 5 / seat 2");
        checks.equal("cumulative points", "12", "12");
        checks.within("half pixel centering", 10.5, 10, .5);
        assertEquals("Every matching pair contributes to coverage", 2L, checks.checkedPairs());
        checks.publish();
        assertThrows("The first mismatching cell fails immediately", AssertionError.class, () -> checks.equal("cumulative points", "12", "13"));
        assertThrows("Numeric tolerance is enforced", AssertionError.class, () -> checks.within("half pixel centering", 10.5, 9, .5));
        assertEquals("Mismatches also contribute to coverage", 4L, checks.checkedPairs());
        assertThrows("Catching an earlier failure cannot turn the aggregate green", AssertionError.class, checks::publish);
    }
    @Test public void anEmptyScanCannotProducePassingCoverage() {
        GradleInputs.fixture("Controlled empty ledger scan", "pairs", 0);
        assertThrows("Coverage requires evaluated pairs", AssertionError.class, () -> new ExhaustiveMatrixChecks().publish());
    }
}
