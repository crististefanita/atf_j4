package atf.android.e2e.display.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

@AtfComponentTest
class ResponsiveDisplayProfileComponentTest {
    @Test
    void defaultsCoverCompactStandardRogAndTabletPortraits() {
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
        var profiles = ResponsiveDisplayProfile.parse("720x1280@360,1080x1920@440");
        assertEquals(569, profiles.get(0).logicalHeightDp());
        assertEquals(94, profiles.get(0).playedCardFaceHeightDp());
        assertEquals(16, profiles.get(0).playedCardLabelAndPaddingDp());
        assertEquals(698, profiles.get(1).logicalHeightDp());
        assertEquals(104, profiles.get(1).playedCardFaceHeightDp());
        assertEquals(18, profiles.get(1).playedCardLabelAndPaddingDp());
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
