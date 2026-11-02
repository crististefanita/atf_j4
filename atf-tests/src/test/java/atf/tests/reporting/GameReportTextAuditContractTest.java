package atf.tests.reporting;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class GameReportTextAuditContractTest {
    @Test void textAndLiteralEntitiesAreDecodedExactlyOnce() {
        var audit = new GameReportTextAudit("<pre>&#10;first&#13;&#10;last &amp; &amp;#13; &lt;x&gt;</pre>");
        assertTrue(audit.contains("\nfirst\r\nlast & &#13; <x>"));
        assertFalse(audit.contains("\nfirst\r\nlast & \r <x>"));
        assertFalse(audit.contains("first\r\nlast & &#13; <x>"));
    }
    @Test void jsonRequiresTheWholeTypedDocumentIncludingLargeNumbersAndLastScalar() {
        var audit = new GameReportTextAudit("<pre>{&quot;amount&quot;:9007199254740993.01,&quot;rows&quot;:[-7,2,3]}</pre>");
        assertTrue(audit.contains("{ \"amount\":9007199254740993.01, \"rows\":[-7, 2, 3] }"));
        assertFalse(audit.contains("{\"amount\":9007199254740993.00,\"rows\":[-7,2,3]}"));
        assertFalse(audit.contains("{\"amount\":9007199254740993.01,\"rows\":[-7,2]}"));
    }
    @Test void orderedTextFragmentsJoinOnlyInsideTheSameDisclosureAndNeedEveryPart() {
        String opening = "<details><summary>Captured context</summary>";
        String first = "<details><summary>State · part 1 of 2</summary><pre>first&#13;</pre></details>";
        String last = "<details><summary>State · part 2 of 2</summary><pre>&#10;last</pre></details>";
        assertTrue(new GameReportTextAudit(opening + first + last + "</details>").contains("first\r\nlast"));
        assertFalse(new GameReportTextAudit(opening + last + first + "</details>").contains("first\r\nlast"));
        assertFalse(new GameReportTextAudit(opening + first + "</details>" + opening + last + "</details>").contains("first\r\nlast"));
    }
}
