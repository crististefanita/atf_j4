package atf.android.e2e.records.legend;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import java.util.List;
import atf.android.component.AndroidComponentAudit;
import atf.android.evidence.component.ComponentInputs;
import org.junit.jupiter.api.extension.RegisterExtension;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest
class LegendProfilesComponentTest {
    @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
    @Test void defaultsPreserveTheFiveDeclaredNativeCasesAndBothLanguagesAtTheExtremes(ComponentInputs inputs) {
        assertEquals(List.of("720x1280-360dpi-font-1.0-EN", "720x1280-360dpi-font-1.0-RO",
                "720x1280-360dpi-font-1.5-EN", "720x1280-360dpi-font-1.5-RO", "1080x2448-440dpi-font-1.3-EN"),
                parse(null, inputs).stream().map(LegendProfiles.Case::id).toList());
    }

    @Test void aFocusedSampleKeepsItsCanonicalIdentityAndPixelBudget(ComponentInputs inputs) {
        var sample = parse(" 720x1280@360:1.50:ro ", inputs).getFirst();
        assertEquals("720x1280-360dpi-font-1.5-RO", sample.id());
        assertEquals("720x1280", sample.resolution());
        assertEquals(90, sample.minimumRowPixels());
        assertTrue(sample.romanian());assertTrue(sample.requiresColumnMovement());
        assertFalse(parse("1200x1920@320:1.5:EN", inputs).getFirst().requiresColumnMovement());
    }

    @Test void invalidProfilesScalesAndLanguagesCannotStartAnUnintendedRun(ComponentInputs inputs) {
        for (String value : new String[]{"", " ", ",", "720x1280@360", "1280x720@360:1.0:EN", "720x1280@100:1.0:EN",
                "720x1280@360:NaN:EN", "720x1280@360:Infinity:EN", "720x1280@360:0.9:EN", "720x1280@360:1.6:EN",
                "720x1280@360:1.0:FR", "720x1280@360::EN", "720x1280@360:1.0:EN,"})
            assertThrows(IllegalArgumentException.class, () -> parse(value, inputs), value);
    }

    @Test void duplicatesAreRejectedAfterNormalizationAndTheRunHasAHardCardinalityBound(ComponentInputs inputs) {
        assertThrows(IllegalArgumentException.class, () -> parse("720x1280@360:1.5:RO,720x1280@360:1.50:ro", inputs));
        assertThrows(IllegalArgumentException.class, () -> parse(String.join(",", java.util.Collections.nCopies(9, "720x1280@360:1.0:EN")), inputs));
    }

    @Test void aParsedPlanIsImmutableAndDoesNotLeakSelectionState(ComponentInputs inputs) {
        var plan = parse(null, inputs);
        assertThrows(UnsupportedOperationException.class, plan::clear);
        assertEquals(1, parse("720x1280@360:1.5:RO", inputs).size());
        assertEquals(5, parse(null, inputs).size());
    }

    private static List<LegendProfiles.Case> parse(String value, ComponentInputs inputs) {
        inputs.add("Profile selection", value == null ? "<unset: declared defaults>" : value);
        return LegendProfiles.parse(value);
    }
}
