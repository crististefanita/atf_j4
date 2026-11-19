package ro.cardtable.ui.score;

import java.util.HashSet;
import java.util.List;
import org.junit.Test;
import static atf.gradle.audit.assertions.GradleAssertions.*;

public final class ScoreTypographyComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void nativeScoreRolesDefineTheWebScaleExactlyOnce() {
        var sizes = List.of(25, 12, 13, 16, 18, 14, 16, 14, 11, 13, 12, 20, 24, 21);
        atf.gradle.audit.GradleInputs.fixture("Score typography adapters", "native logical sp", sizes,
                "Web root pixels", List.of(16, 24, 32), "desktop scale", List.of(.75, 1.0, 1.25));
        var names = new HashSet<String>();
        int index = 0;
        for (var role : ScoreTypography.values()) {
            assertEquals("Existing native text role " + role, sizes.get(index++).intValue(), role.sp());
            assertTrue("Each role has a distinct CSS token " + role, names.add(role.cssName()));
            double rem = Double.parseDouble(role.rem().replace("rem", ""));
            for (int root : List.of(16, 24, 32)) {
                double nativeLogical = role.sp() * root / 16.0;
                assertEquals("Font enlargement once for " + role + " at " + root, nativeLogical, rem * root, .00001);
            }
        }
        assertEquals("All score roles are covered", sizes.size(), index);
    }

    @Test public void resultActionsEnlargeExactlyOnceInBothPlatformUnits() {
        atf.gradle.audit.GradleInputs.fixture("Result action typography", "baseline sizes", List.of(16, 14),
                "enlargement", 1.5, "accessible roots", List.of(16, 20, 24, 32));
        for (int root : List.of(16, 20, 24, 32)) {
            double scale = root / 16.0;
            assertEquals("Primary action gains 50% before accessibility scaling", 16 * 1.5 * scale,
                    ScoreTypography.ACTION_PRIMARY.sp() * scale, .00001);
            assertEquals("Secondary action gains 50% before accessibility scaling", 14 * 1.5 * scale,
                    Double.parseDouble(ScoreTypography.ACTION_SECONDARY.rem().replace("rem", "")) * root, .00001);
        }
    }

    @Test public void denseScoreCellsAndGenericDialogsKeepLegacyRoles() {
        atf.gradle.audit.GradleInputs.fixture("Independent score roles", "column/value/body/dialog/quiet",
                List.of(11, 13, 16, 20, 14));
        assertEquals("Compact column headings do not inherit action enlargement", 11, ScoreTypography.COLUMN.sp());
        assertEquals("Numeric cells retain their own role", 13, ScoreTypography.VALUE.sp());
        assertEquals("Generic body text is unchanged", 16, ScoreTypography.BODY.sp());
        assertEquals("Dialog headings are unchanged", 20, ScoreTypography.DIALOG_TITLE.sp());
        assertEquals("Existing quiet dialogs stay independent", 14, ScoreTypography.QUIET.sp());
    }
}
