package atf.android.e2e.records.legend;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class LegendProfilesComponentTest {
    @Test void defaultsPreserveTheFiveDeclaredNativeCasesAndBothLanguagesAtTheExtremes() {
        assertEquals(List.of("720x1280-360dpi-font-1.0-EN", "720x1280-360dpi-font-1.0-RO",
                "720x1280-360dpi-font-1.5-EN", "720x1280-360dpi-font-1.5-RO", "1080x2448-440dpi-font-1.3-EN"),
                LegendProfiles.parse(null).stream().map(LegendProfiles.Case::id).toList());
    }

    @Test void aFocusedSampleKeepsItsCanonicalIdentityAndPixelBudget() {
        var sample = LegendProfiles.parse(" 720x1280@360:1.50:ro ").getFirst();
        assertEquals("720x1280-360dpi-font-1.5-RO", sample.id());
        assertEquals("720x1280", sample.resolution());
        assertEquals(90, sample.minimumRowPixels());
        assertTrue(sample.romanian());assertTrue(sample.requiresColumnMovement());
        assertFalse(LegendProfiles.parse("1200x1920@320:1.5:EN").getFirst().requiresColumnMovement());
    }

    @Test void invalidProfilesScalesAndLanguagesCannotStartAnUnintendedRun() {
        for (String value : new String[]{"", " ", ",", "720x1280@360", "1280x720@360:1.0:EN", "720x1280@100:1.0:EN",
                "720x1280@360:NaN:EN", "720x1280@360:Infinity:EN", "720x1280@360:0.9:EN", "720x1280@360:1.6:EN",
                "720x1280@360:1.0:FR", "720x1280@360::EN", "720x1280@360:1.0:EN,"})
            assertThrows(IllegalArgumentException.class, () -> LegendProfiles.parse(value), value);
    }

    @Test void duplicatesAreRejectedAfterNormalizationAndTheRunHasAHardCardinalityBound() {
        assertThrows(IllegalArgumentException.class, () -> LegendProfiles.parse("720x1280@360:1.5:RO,720x1280@360:1.50:ro"));
        assertThrows(IllegalArgumentException.class, () -> LegendProfiles.parse(String.join(",", java.util.Collections.nCopies(9, "720x1280@360:1.0:EN"))));
    }

    @Test void aParsedPlanIsImmutableAndDoesNotLeakSelectionState() {
        var plan = LegendProfiles.parse(null);
        assertThrows(UnsupportedOperationException.class, plan::clear);
        assertEquals(1, LegendProfiles.parse("720x1280@360:1.5:RO").size());
        assertEquals(5, LegendProfiles.parse(null).size());
    }
}
