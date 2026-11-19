package ro.cardtable.ui.score;

import java.util.HashSet;
import java.util.List;
import org.junit.Test;
import static atf.gradle.audit.assertions.GradleAssertions.*;

public final class ScoreTypographyComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void nativeScoreRolesDefineTheWebScaleExactlyOnce() {
        var sizes = List.of(25, 12, 13, 16, 18, 14, 16, 14, 11, 13, 12, 20);
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
}
