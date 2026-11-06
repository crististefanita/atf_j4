package atf.android.e2e.records.legend;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import atf.android.component.AndroidComponentAudit;
import atf.android.evidence.component.ComponentInputs;
import org.junit.jupiter.api.extension.RegisterExtension;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest
class HistoryLegendAuditComponentTest {
    @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
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

    @Test void fixedWrappingCaptionHasIndependentOwnershipAndGeometry(ComponentInputs inputs) throws Exception {
        var state = inspect(xml(false), inputs);
        assertEquals(new HistoryLegendAudit.Box(20, 380, 300, 460), state.legend(), "Measured caption input");
        assertEquals(new HistoryLegendAudit.Box(20, 180, 300, 380), state.rows(), "Measured row viewport");
        assertTrue(state.checks().stream().allMatch(atf.reporting.contract.AtfAssertion::passed));
    }

    @Test void theOriginalHorizontalOwnershipFailsEvenWithFullAccessibleText(ComponentInputs inputs) throws Exception {
        var state = inspect(xml(true), inputs);
        assertFalse(state.outsideColumns());
        assertTrue(state.checks().stream().anyMatch(row -> !row.passed()));
    }

    @Test void clippedEmptyOverlappingAndOverwideCaptionsCannotPass(ComponentInputs inputs) throws Exception {
        for (String bounds : new String[]{"[20,380][600,460]", "[20,380][300,550]", "[20,380][300,380]", "[20,300][300,350]"}) {
            var state = inspect(xml(false).replace("[20,380][300,460]", bounds), inputs);
            assertTrue(state.checks().stream().anyMatch(row -> !row.passed()), bounds);
        }
        for (String[] region : new String[][]{{"[20,180][300,380]", "[20,180][300,180]"},
                {"[200,500][300,540]", "[200,500][300,500]"}})
            assertTrue(inspect(xml(false).replace(region[0], region[1]), inputs).checks().stream().anyMatch(row -> !row.passed()),
                    "Invalid rendered region: " + region[1]);
    }

    @Test void missingDuplicateOrMalformedEvidenceCannotProduceImplicitPass(ComponentInputs inputs) {
        assertThrows(IllegalStateException.class, () -> inspect(xml(false).replace(LEGEND, ""), inputs), "Missing legend");
        assertThrows(IllegalStateException.class, () -> inspect(xml(false).replace(LEGEND, LEGEND + LEGEND), inputs), "Duplicate legend");
        assertThrows(IllegalArgumentException.class, () -> inspect(xml(false).replace("[20,380][300,460]", "missing"), inputs), "Malformed bounds");
        assertThrows(org.xml.sax.SAXException.class,
                () -> inspect("<!DOCTYPE hierarchy SYSTEM 'file:///unavailable'><hierarchy/>", inputs), "External DTD refused");
    }

    private static HistoryLegendAudit.Snapshot inspect(String xml, ComponentInputs inputs) throws Exception {
        inputs.add("Android hierarchy", xml);
        return HistoryLegendAudit.inspect(xml, CAPTION, "Close", "HistUser");
    }

    private static String xml(boolean nested) {
        return "<hierarchy><node class=\"android.widget.LinearLayout\">" + TABLE.formatted(nested ? LEGEND : "")
                + (nested ? "" : LEGEND) + "<node class=\"android.widget.Button\" text=\"CLOSE\" bounds=\"[200,500][300,540]\"/>"
                + "</node></hierarchy>";
    }
}
