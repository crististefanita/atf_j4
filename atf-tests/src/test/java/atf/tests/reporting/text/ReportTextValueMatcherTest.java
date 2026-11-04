package atf.tests.reporting.text;

import static org.junit.jupiter.api.Assertions.*;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

@AtfComponentTest
class ReportTextValueMatcherTest {
    @Test void nativeCompactJsonPreservesEveryValueAndItsOrder() {
        assertTrue(ReportTextValueMatcher.containsCompleteValue("<td>[14,24,23,15]</td>","[14, 24, 23, 15]"));
        assertTrue(ReportTextValueMatcher.containsCompleteValue("<td>{&quot;cards&quot;:[1,2]}</td>","{ \"cards\": [1, 2] }"));
    }
    @Test void missingReorderedOrTruncatedJsonIsRejected() {
        for(String actual:new String[]{"[14,24,23]","[14,23,24,15]","[14,24,23,15,99]","[14,24…]"})
            assertFalse(ReportTextValueMatcher.containsCompleteValue("<td>"+actual+"</td>","[14, 24, 23, 15]"),actual);
    }
    @Test void arbitraryTextIsNotReformattedOrMatchedAsAPartialJsonPrefix() {
        assertFalse(ReportTextValueMatcher.containsCompleteValue("<td>legal=[1,2]</td>","legal=[1, 2]"));
        assertFalse(ReportTextValueMatcher.containsCompleteValue("<td>[1,2]</td>","[1, 2] missing tail"));
        assertTrue(ReportTextValueMatcher.containsCompleteValue("<td>a &lt; b</td>","a < b"));
    }
}
