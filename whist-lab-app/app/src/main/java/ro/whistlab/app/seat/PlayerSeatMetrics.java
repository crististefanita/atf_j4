package ro.whistlab.app.seat;

/** A complete action line above a taller frame; no text height is discarded. */
public final class PlayerSeatMetrics {
    public static final int COMPACT_NAME_REGION_DP = ro.cardtable.ui.seat.PlayerSeatMetrics.COMPACT_NAME_REGION_DP;
    public static final int ROOMY_HEADING_RESERVE_DP = ro.cardtable.ui.seat.PlayerSeatMetrics.ROOMY_HEADING_RESERVE_DP;
    public record Plan(int badgeHeightPx, int frameTopPx, int frameHeightPx, int heightPx) { }

    /** Preserve the frame's reference heading region when the action moves above its border. */
    public static int referenceFrameHeightPx(int bodyHeightPx, int nameHeightPx, int headingHeightPx) {
        return ro.cardtable.ui.seat.PlayerSeatMetrics.referenceFrameHeightPx(bodyHeightPx, nameHeightPx, headingHeightPx);
    }

    public static int frameHeightPx(int naturalHeightPx) {
        return ro.cardtable.ui.seat.PlayerSeatMetrics.frameHeightPx(naturalHeightPx);
    }

    public static Plan arrange(int naturalFrameHeightPx, int measuredBadgeHeightPx, int gapPx) {
        var plan = ro.cardtable.ui.seat.PlayerSeatMetrics.arrange(naturalFrameHeightPx, measuredBadgeHeightPx, gapPx);
        return new Plan(plan.badgeHeightPx(), plan.frameTopPx(), plan.frameHeightPx(), plan.heightPx());
    }

    public static Plan arrange(int naturalFrameHeightPx, int measuredBadgeHeightPx, int gapPx,
            int reservedHeightPx) {
        var plan = ro.cardtable.ui.seat.PlayerSeatMetrics.arrange(naturalFrameHeightPx, measuredBadgeHeightPx, gapPx, reservedHeightPx);
        return new Plan(plan.badgeHeightPx(), plan.frameTopPx(), plan.frameHeightPx(), plan.heightPx());
    }

    private PlayerSeatMetrics() { }
}
