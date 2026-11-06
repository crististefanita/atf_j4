package atf.android.e2e.display.profile;

import atf.android.component.AndroidComponentAudit;
import org.junit.jupiter.api.extension.RegisterExtension;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

@AtfComponentTest
class ResponsiveDisplayProfileComponentTest {
    @RegisterExtension static final AndroidComponentAudit AUDIT = AndroidComponentAudit.extension();
    @Test
    void defaultsCoverCompactStandardTallAndWidePortraits() {
        assertEquals(4, ResponsiveDisplayProfile.parse(null).size());
        assertEquals(4, ResponsiveDisplayProfile.parse("").size());
    }

    @Test
    void configuredProfilesRetainTheirOrderAndPhysicalParameters() {
        var profiles = ResponsiveDisplayProfile.parse("720x1280@360,1080x2448@440");
        assertEquals("720x1280-360dpi", profiles.getFirst().id());
        assertEquals(1080, profiles.get(1).width());
        assertEquals(2448, profiles.get(1).height());
        assertEquals(440, profiles.get(1).density());
    }

    @Test
    void compactProfilesDeclareTheResponsivePlayedCardHeightContract() {
        assertEquals(88, ResponsiveDisplayProfile.HAND_CARD_FACE_HEIGHT_DP);
        assertEquals(63, ResponsiveDisplayProfile.HAND_CARD_FACE_WIDTH_DP);
        var profiles = ResponsiveDisplayProfile.parse("720x1280@360,1080x1920@440");
        assertEquals(569, profiles.get(0).logicalHeightDp());
        assertEquals(64, profiles.get(0).playedCardFaceHeightDp());
        assertEquals(46, profiles.get(0).playedCardFaceWidthDp());
        assertEquals(66, profiles.get(0).minimumPlayedCardSlotHeightDp());
        assertEquals(698, profiles.get(1).logicalHeightDp());
        assertEquals(70, profiles.get(1).playedCardFaceHeightDp());
        assertEquals(50, profiles.get(1).playedCardFaceWidthDp());
        assertEquals(72, profiles.get(1).minimumPlayedCardSlotHeightDp());
    }

    @Test
    void duplicateMalformedLandscapeAndUnusableProfilesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ResponsiveDisplayProfile.parse("720x1280@360,720x1280@360"));
        assertThrows(IllegalArgumentException.class,
                () -> ResponsiveDisplayProfile.parse("1080x2448"));
        assertThrows(IllegalArgumentException.class,
                () -> ResponsiveDisplayProfile.parse("1920x1080@440"));
        assertThrows(IllegalArgumentException.class,
                () -> ResponsiveDisplayProfile.parse("480x800@640"));
        assertThrows(IllegalArgumentException.class,
                () -> ResponsiveDisplayProfile.parse("720x1280@360,720x1280@360,720x1280@360,720x1280@360,720x1280@360"));
    }

    @Test
    void configuredGameFlowHandsAreBoundedAndDefaultToOne() {
        assertEquals(1, ResponsiveDisplayProfile.handsToPlay("1"));
        assertEquals(1, ResponsiveDisplayProfile.handsToPlay(null));
        assertEquals(5, ResponsiveDisplayProfile.handsToPlay("5"));
        assertThrows(IllegalArgumentException.class,
                () -> ResponsiveDisplayProfile.handsToPlay("0"));
        assertThrows(IllegalArgumentException.class,
                () -> ResponsiveDisplayProfile.handsToPlay("6"));
        assertThrows(IllegalArgumentException.class,
                () -> ResponsiveDisplayProfile.handsToPlay("many"));
    }
}
