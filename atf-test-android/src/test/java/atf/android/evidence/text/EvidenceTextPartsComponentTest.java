package atf.android.evidence.text;

import atf.android.component.AndroidComponentAudit;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest
public class EvidenceTextPartsComponentTest {
    @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
    @Test void completeTextRemainsOrderedAtAndBeyondTheBudget() {
        assertEquals(java.util.List.of("abcd", "efgh", "i"), EvidenceTextParts.split("abcdefghi", 4), "Contiguous complete parts");
        for (int length : new int[]{0, 1, 7, 8, 9, 32768, 32769, 70000}) {
            String original = "x".repeat(length) + "tail";
            var parts = EvidenceTextParts.split(original, 8);
            assertEquals(original, String.join("", parts), "Complete input including the final sentinel: " + length);
            assertTrue(parts.stream().allMatch(value -> value.length() <= 8), "Every part fits the effective budget");
        }
    }
    @Test void unicodePairsCannotBeSeparatedOrSilentlyTruncated() {
        assertEquals(java.util.List.of("ab", "😀z"), EvidenceTextParts.split("ab😀z", 3), "Whole supplementary character");
        assertThrows(IllegalArgumentException.class, () -> EvidenceTextParts.split("😀", 1), "Explicit unrepresentable body budget");
        assertThrows(IllegalArgumentException.class, () -> EvidenceTextParts.split("input", -1), "Negative budget refused");
    }
    @Test void unboundedAndExactFitPoliciesPreserveTheOriginalContent() {
        assertEquals(java.util.List.of("abcdef"), EvidenceTextParts.split("abcdef", 0), "Zero disables only the body display limit");
        assertEquals(java.util.List.of("abcdef"), EvidenceTextParts.split("abcdef", 6), "Exact fit needs no fragmentation");
        assertEquals(java.util.List.of(""), EvidenceTextParts.split("", 1), "Empty body remains explicit");
    }
}
