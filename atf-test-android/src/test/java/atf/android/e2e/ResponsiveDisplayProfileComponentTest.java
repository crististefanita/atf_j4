package atf.android.e2e;

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
    void configuredHandsCanReachTheFirstTwoCardDealWithoutPlayingTheFullMatch() {
        assertEquals(1, ResponsiveDisplayProfile.handsPerProfile("1"));
        assertEquals(2, ResponsiveDisplayProfile.handsPerProfile(null));
        assertEquals(5, ResponsiveDisplayProfile.handsPerProfile("5"));
        assertThrows(IllegalArgumentException.class,
                () -> ResponsiveDisplayProfile.handsPerProfile("0"));
        assertThrows(IllegalArgumentException.class,
                () -> ResponsiveDisplayProfile.handsPerProfile("6"));
        assertThrows(IllegalArgumentException.class,
                () -> ResponsiveDisplayProfile.handsPerProfile("many"));
    }
}
