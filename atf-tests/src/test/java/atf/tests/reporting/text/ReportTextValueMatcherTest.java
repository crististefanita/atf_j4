package atf.tests.reporting.text;

import atf.testkit.assertions.AuditedAssertions;

import static org.junit.jupiter.api.Assertions.*;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

@AtfComponentTest
class ReportTextValueMatcherTest {
    @Test void nativeCompactJsonPreservesEveryValueAndItsOrder() {
        AuditedAssertions.assertTrue(ReportTextValueMatcher.containsCompleteValue("<td>[14,24,23,15]</td>","[14, 24, 23, 15]"));
        AuditedAssertions.assertTrue(ReportTextValueMatcher.containsCompleteValue("<td>{&quot;cards&quot;:[1,2]}</td>","{ \"cards\": [1, 2] }"));
    }
    @Test void missingReorderedOrTruncatedJsonIsRejected() {
        for(String actual:new String[]{"[14,24,23]","[14,23,24,15]","[14,24,23,15,99]","[14,24…]"})
            AuditedAssertions.assertFalse(ReportTextValueMatcher.containsCompleteValue("<td>"+actual+"</td>","[14, 24, 23, 15]"),actual);
    }
    @Test void arbitraryTextIsNotReformattedOrMatchedAsAPartialJsonPrefix() {
        AuditedAssertions.assertFalse(ReportTextValueMatcher.containsCompleteValue("<td>legal=[1,2]</td>","legal=[1, 2]"));
        AuditedAssertions.assertFalse(ReportTextValueMatcher.containsCompleteValue("<td>[1,2]</td>","[1, 2] missing tail"));
        AuditedAssertions.assertTrue(ReportTextValueMatcher.containsCompleteValue("<td>a &lt; b</td>","a < b"));
    }
}
