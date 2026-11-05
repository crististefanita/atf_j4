package ro.whistlab.app.theme;

import org.junit.Test;
import static org.junit.Assert.*;

public class GameHeaderTypographyComponentTest {
    @Test public void bidSummaryGrowsByExactlyThirtyPercentWithoutWholeSpRounding() {
        assertEquals("The existing 9 sp summary grows by exactly 30%", 1.30f,
                GameHeaderTypography.BID_SUMMARY_SP / 9f, 0.000001f);
        assertEquals(11.7f, GameHeaderTypography.BID_SUMMARY_SP, 0.000001f);
        assertTrue("Rounding to 12 sp would exceed the requested increase",
                GameHeaderTypography.BID_SUMMARY_SP < 12f);
    }

    @Test public void theSummaryChangePreservesTheOtherHeaderRoles() {
        assertEquals(14f, GameHeaderTypography.TITLE_SP, 0f);
        assertEquals(11f, GameHeaderTypography.PROGRESS_SP, 0f);
        assertTrue("The game title remains the most prominent header role",
                GameHeaderTypography.TITLE_SP > GameHeaderTypography.BID_SUMMARY_SP);
    }
}
