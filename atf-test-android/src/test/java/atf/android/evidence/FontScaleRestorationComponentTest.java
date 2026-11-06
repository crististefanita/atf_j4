package atf.android.evidence;

import atf.android.component.AndroidComponentAudit;
import atf.testkit.AtfComponentTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import static atf.testkit.assertions.AuditedAssertions.*;

@AtfComponentTest
public class FontScaleRestorationComponentTest {
    @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
    @Test void absentSettingIsDeletedInsteadOfWritingTheLiteralNull() {
        assertEquals(List.of("shell", "settings", "delete", "system", "font_scale"),
                List.of(FontScaleRestoration.command("null")), "Restore the original absence of the Android setting");
    }
    @Test void observedNumericValueIsRestoredExactly() {
        for (String scale : List.of("1.0", "1.3", "1.5")) assertEquals(
                List.of("shell", "settings", "put", "system", "font_scale", scale),
                List.of(FontScaleRestoration.command(scale)), "Restore the observed numeric font scale");
    }
    @Test void unknownOrInvalidOriginalValuesCannotSilentlyBecomeDefaults() {
        for (String scale : new String[] {null, "", "NaN", "Infinity", "0", "-1"})
            assertThrows(IllegalArgumentException.class, () -> FontScaleRestoration.command(scale), "Unknown original scale must stop restoration");
    }
}
