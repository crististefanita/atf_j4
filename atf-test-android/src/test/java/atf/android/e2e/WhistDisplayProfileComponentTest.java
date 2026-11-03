package atf.android.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@AtfComponentTest
class WhistDisplayProfileComponentTest {
    @ParameterizedTest
    @CsvSource({
            "'Physical size: 1080x1920',1080x1920",
            "'Physical size: 1080x1920\nOverride size: 720x1280',720x1280",
            "'Physical size: 1080x1920\r\nOverride size: 1080x2448',1080x2448"
    })
    void effectiveSizeUsesOverrideOrThePhysicalBaseline(String output, String expected) {
        assertEquals(expected, WhistLabGameplayE2eTest.effectiveDisplaySize(output));
    }

    @Test
    void absentSizeCannotCertifyTheRequestedProfile() {
        assertThrows(IllegalArgumentException.class,
                () -> WhistLabGameplayE2eTest.effectiveDisplaySize("ADB device offline"));
    }
}
