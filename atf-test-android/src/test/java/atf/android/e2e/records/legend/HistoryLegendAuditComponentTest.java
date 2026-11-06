package atf.android.e2e.records.legend;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class HistoryLegendAuditComponentTest {
    private static final String CAPTION = "B Bid · P Points";
    private static final String TABLE = """
            <node class="android.widget.HorizontalScrollView" bounds="[20,100][300,380]">
              <node class="android.widget.LinearLayout" bounds="[20,100][600,380]">
                <node text="HistUser" bounds="[40,100][100,140]"/>
                <node class="android.widget.ScrollView" bounds="[20,180][300,380]"/>
                %s
              </node>
            </node>
            """;
    private static final String LEGEND = "<node text=\"" + CAPTION + "\" bounds=\"[20,380][300,460]\"/>";

    @Test void fixedWrappingCaptionHasIndependentOwnershipAndGeometry() throws Exception {
        assertTrue(inspect(xml(false)).checks().stream().allMatch(atf.reporting.contract.AtfAssertion::passed));
    }

    @Test void theOriginalHorizontalOwnershipFailsEvenWithFullAccessibleText() throws Exception {
        var state = inspect(xml(true));
        assertFalse(state.outsideColumns());
        assertTrue(state.checks().stream().anyMatch(row -> !row.passed()));
    }

    @Test void clippedEmptyOverlappingAndOverwideCaptionsCannotPass() throws Exception {
        for (String bounds : new String[]{"[20,380][600,460]", "[20,380][300,550]", "[20,380][300,380]", "[20,300][300,350]"}) {
            var state = inspect(xml(false).replace("[20,380][300,460]", bounds));
            assertTrue(state.checks().stream().anyMatch(row -> !row.passed()), bounds);
        }
        for (String[] region : new String[][]{{"[20,180][300,380]", "[20,180][300,180]"},
                {"[200,500][300,540]", "[200,500][300,500]"}})
            assertTrue(inspect(xml(false).replace(region[0], region[1])).checks().stream().anyMatch(row -> !row.passed()));
    }

    @Test void missingDuplicateOrMalformedEvidenceCannotProduceImplicitPass() {
        assertThrows(Exception.class, () -> inspect(xml(false).replace(LEGEND, "")));
        assertThrows(Exception.class, () -> inspect(xml(false).replace(LEGEND, LEGEND + LEGEND)));
        assertThrows(Exception.class, () -> inspect(xml(false).replace("[20,380][300,460]", "missing")));
        assertThrows(Exception.class, () -> inspect("<!DOCTYPE hierarchy SYSTEM 'file:///unavailable'><hierarchy/>"));
    }

    private static HistoryLegendAudit.Snapshot inspect(String xml) throws Exception {
        return HistoryLegendAudit.inspect(xml, CAPTION, "Close", "HistUser");
    }

    private static String xml(boolean nested) {
        return "<hierarchy><node class=\"android.widget.LinearLayout\">" + TABLE.formatted(nested ? LEGEND : "")
                + (nested ? "" : LEGEND) + "<node class=\"android.widget.Button\" text=\"CLOSE\" bounds=\"[200,500][300,540]\"/>"
                + "</node></hierarchy>";
    }
}
