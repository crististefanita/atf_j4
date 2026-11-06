package atf.android.e2e.display.profile;

import atf.android.component.AndroidComponentAudit;
import org.junit.jupiter.api.extension.RegisterExtension;

import atf.android.e2e.gameplay.WhistLabGameplayE2eTest;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@AtfComponentTest
class WhistDisplayProfileComponentTest {
    @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
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
