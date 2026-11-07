package ro.whistlab.app.theme;

import org.junit.Test;
import static atf.gradle.audit.assertions.GradleAssertions.*;

public class GameHeaderTypographyComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void bidSummaryGrowsByExactlyThirtyPercentWithoutWholeSpRounding() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", "The existing 9 sp summary grows by exactly 30%", "declared constant 2", 1.30f, "declared constant 3", 9f, "declared constant 4", 0.000001f, "declared constant 5", 11.7f, "declared constant 6", "Rounding to 12 sp would exceed the requested increase", "declared constant 7", 12f);
        assertEquals("The existing 9 sp summary grows by exactly 30%", 1.30f,
                GameHeaderTypography.BID_SUMMARY_SP / 9f, 0.000001f);
        assertEquals(11.7f, GameHeaderTypography.BID_SUMMARY_SP, 0.000001f);
        assertTrue("Rounding to 12 sp would exceed the requested increase",
                GameHeaderTypography.BID_SUMMARY_SP < 12f);
    }

    @Test public void theSummaryChangePreservesTheOtherHeaderRoles() {

        atf.gradle.audit.GradleInputs.fixture("Fixture before first assertion", "declared constant 1", 14f, "declared constant 2", 0f, "declared constant 3", 11f, "declared constant 4", "The game title remains the most prominent header role");
        assertEquals(14f, GameHeaderTypography.TITLE_SP, 0f);
        assertEquals(11f, GameHeaderTypography.PROGRESS_SP, 0f);
        assertTrue("The game title remains the most prominent header role",
                GameHeaderTypography.TITLE_SP > GameHeaderTypography.BID_SUMMARY_SP);
    }
}
