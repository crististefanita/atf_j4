package ro.cardtable.ui.score;

import java.util.Map;
import org.junit.Test;
import static atf.gradle.audit.assertions.GradleAssertions.*;

public final class HistoryAppearanceComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void readableGroupsExportTheExactNativePolicyToWebWithoutChangingLegacyDefaults() {
        atf.gradle.audit.GradleInputs.fixture("History appearance", "profiles", java.util.List.of(HistoryAppearance.LEGACY, HistoryAppearance.REGULAR_GROUPS));
        var regular = HistoryAppearance.REGULAR_GROUPS;
        assertFalse("Readable history does not emphasize headings", regular.boldPlayerNames());
        assertEquals("Player groups have a visible two-unit divider", 2, regular.separatorDp());
        assertEquals("Divider uses the Whist history reference color", 0xff71846c, regular.separatorColor());
        assertEquals("Browser consumes the exact native style", Map.of("--history-heading-weight", "400",
                "--history-divider-width", "2px", "--history-divider-color", "#71846c"), regular.cssTokens());
        assertTrue("Legacy consumers retain their bold headings", HistoryAppearance.LEGACY.boldPlayerNames());
        assertEquals("Legacy separator width is unchanged", 1, HistoryAppearance.LEGACY.separatorDp());
        assertThrows("Exported policy is immutable", UnsupportedOperationException.class, () -> regular.cssTokens().clear());
    }

    @Test public void invalidOrUnrepresentableSeparatorsAreRejected() {
        atf.gradle.audit.GradleInputs.fixture("Invalid history appearance", "widths", java.util.List.of(0, 5), "transparent", 0x0071846c);
        assertThrows("Zero divider is not visible", IllegalArgumentException.class, () -> new HistoryAppearance(false, 0, 0xff71846c));
        assertThrows("Oversized divider is refused", IllegalArgumentException.class, () -> new HistoryAppearance(false, 5, 0xff71846c));
        assertThrows("Native and CSS color must both be opaque", IllegalArgumentException.class, () -> new HistoryAppearance(false, 2, 0x0071846c));
    }
}
