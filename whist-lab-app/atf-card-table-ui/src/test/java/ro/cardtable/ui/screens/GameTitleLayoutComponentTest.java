package ro.cardtable.ui.screens;

import atf.gradle.audit.GradleAuditedTest;
import atf.gradle.audit.GradleInputs;
import java.util.List;
import org.junit.Test;
import static atf.gradle.audit.assertions.GradleAssertions.assertEquals;

public final class GameTitleLayoutComponentTest extends GradleAuditedTest {
    private record Profile(int width, int title, int toggle, int titleSpace, boolean inline) { }

    @Test public void positionsTheSameToggleUsingMeasuredLabels() {
        var profiles = List.of(new Profile(219, 172, 84, 163, false),
                new Profile(320, 128, 84, 172, true), new Profile(360, 218, 113, 183, true),
                new Profile(393, 218, 113, 216, true), new Profile(600, 240, 145, 391, true));
        for (var profile : profiles) {
            GradleInputs.fixture("Measured header labels", "profile", profile, "leading action", 48, "gap", 8);
            assertEquals("Keep the title readable and reserve a separate row when needed",
                    new GameTitleLayout(56, profile.titleSpace(), profile.inline()),
                    GameTitleLayout.measure(profile.width(), 48, 8, profile.title(), profile.toggle()));
        }
    }

    @Test public void wrapsTheTitleBeforeMovingTheControlAndKeepsTheExactWidthBoundary() {
        GradleInputs.fixture("Header placement boundary", "full row width", 320, "measured title", 172, "toggle", 84);
        assertEquals("An exact fit keeps the checkbox beside the title", new GameTitleLayout(56, 172, true),
                GameTitleLayout.measure(320, 48, 8, 172, 84));
        assertEquals("One less pixel keeps the checkbox inline while the wrapped title remains readable",
                new GameTitleLayout(56, 171, true),
                GameTitleLayout.measure(319, 48, 8, 172, 84));
        assertEquals("The checkbox moves below the title only when its useful width is too narrow",
                new GameTitleLayout(56, 163, false), GameTitleLayout.measure(219, 48, 8, 172, 84));
        assertEquals("The minimum useful wrapped width keeps the checkbox inline",
                new GameTitleLayout(56, 72, true), GameTitleLayout.measure(220, 48, 8, 172, 84));
        assertEquals("A settled hand keeps the full title width", new GameTitleLayout(56, 264, false),
                GameTitleLayout.measure(320, 48, 8, 172, 0));
    }
}
