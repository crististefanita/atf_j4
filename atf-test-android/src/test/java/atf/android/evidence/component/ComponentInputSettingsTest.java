package atf.android.evidence.component;

import atf.testkit.AtfUnitTest;
import atf.reporting.configuration.ReportingSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class ComponentInputSettingsTest {
    @Test void fullInputPartsChangeWithoutRelaxingAssertionPreviewsOrTheUnconfiguredPolicy() {
        var base = new ReportingSettings(240,100);
        assertSame(base,AndroidComponentReports.inputSettings(base,null));
        var aggregation = AndroidComponentReports.inputSettings(base,"16384");
        assertEquals(240,aggregation.assertionMaxDisplayCharacters());
        assertEquals(16384,aggregation.bodyMaxDisplayCharacters());
        assertEquals(100,base.bodyMaxDisplayCharacters());
    }
    @ParameterizedTest @ValueSource(strings={"0","1","-2","invalid","2147483648"})
    void invalidOrUnicodeSplittingInputBudgetsAreRejected(String value) {
        assertThrows(IllegalArgumentException.class,()->AndroidComponentReports.inputSettings(ReportingSettings.defaults(),value));
    }
}
