package atf.tests.reporting.aggregate;

import atf.testkit.AtfUnitTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class ComponentReportCoverageTest {
    @Test void identityEqualityRejectsMissingDuplicateUnexpectedAndChangedOutcomes() {
        var expected = List.of(new ComponentReportCoverage.Invocation("case-1", "PASS"));
        assertTrue(ComponentReportCoverage.violations(expected, receipt("case-1","PASS",1)).isEmpty());
        assertFalse(ComponentReportCoverage.violations(expected, "").isEmpty());
        assertFalse(ComponentReportCoverage.violations(expected, receipt("case-2","PASS",1)).isEmpty());
        assertFalse(ComponentReportCoverage.violations(expected, receipt("case-1","PASS",1).repeat(2)).isEmpty());
        assertFalse(ComponentReportCoverage.violations(expected, receipt("case-1","FAIL",1)).isEmpty());
        assertFalse(ComponentReportCoverage.violations(expected, receipt("case-1","PASS",0)).isEmpty());
        assertFalse(ComponentReportCoverage.violations(List.of(), "").isEmpty());
        assertFalse(ComponentReportCoverage.violations(List.of(expected.getFirst(),expected.getFirst()), receipt("case-1","PASS",1)).isEmpty());
    }
    @Test void malformedAndNestedReceiptsCannotBecomeEvidence() {
        var expected = List.of(new ComponentReportCoverage.Invocation("case-1", "PASS"));
        assertFalse(ComponentReportCoverage.violations(expected, "<details><summary>Component invocation receipt</summary><pre>invalid</pre></details>").isEmpty());
        assertFalse(ComponentReportCoverage.violations(expected, receipt("", "PASS",1)).isEmpty());
    }
    private static String receipt(String id, String outcome, int count) {
        return "<details><summary>Component invocation receipt</summary><pre>{\"schemaVersion\":1,\"invocationId\":\"" + id
                + "\",\"outcome\":\"" + outcome + "\",\"comparisons\":" + count + "}</pre></details>";
    }
}
