package ro.cardtable.ui.seat;

import static atf.gradle.audit.assertions.GradleAssertions.*;
import org.junit.Test;

/** Pure layout contract for phase-stable action badges in the shared table UI. */
public final class PlayerSeatMetricsComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void oneBadgeSlotUsesTheTallerMeasuredPhaseLabel() {
        atf.gradle.audit.GradleInputs.fixture("Measured phase badge heights", "bidding", 13, "playing", 24);
        assertEquals("The slot uses the taller playing label", 24,
                PlayerSeatMetrics.stableBadgeHeightPx(13, 24));
        assertEquals("The slot uses the taller bidding label", 24,
                PlayerSeatMetrics.stableBadgeHeightPx(24, 13));
        assertEquals("Equal labels keep their shared height", 24,
                PlayerSeatMetrics.stableBadgeHeightPx(24, 24));
    }

    @Test public void invalidPhaseMeasurementsCannotCreateAnEmptyActionSlot() {
        atf.gradle.audit.GradleInputs.fixture("Invalid phase badge heights", "bidding", 0, "playing", 24);
        assertThrows("A zero bidding label cannot create a badge slot", IllegalArgumentException.class,
                () -> PlayerSeatMetrics.stableBadgeHeightPx(0, 24));
        assertThrows("A zero playing label cannot create a badge slot", IllegalArgumentException.class,
                () -> PlayerSeatMetrics.stableBadgeHeightPx(24, 0));
    }

    @Test public void reservedBadgeHeightKeepsFrameCoordinatesEqual() {
        int sharedBadgeHeight = PlayerSeatMetrics.stableBadgeHeightPx(39, 26);
        var bidding = PlayerSeatMetrics.arrange(60, Math.max(39, sharedBadgeHeight), 3);
        var playing = PlayerSeatMetrics.arrange(60, Math.max(26, sharedBadgeHeight), 3);

        atf.gradle.audit.GradleInputs.fixture("The maximum measured badge drives both seat plans",
                "sharedBadgeHeight", sharedBadgeHeight, "bidding", bidding, "playing", playing);
        assertEquals("The entire plan remains unchanged between phases", bidding, playing);
        assertEquals("The framed player starts below the complete badge", 42, bidding.frameTopPx());
        assertEquals("Badge and frame remain one stable region", 117, bidding.heightPx());
    }
}
