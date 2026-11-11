package ro.cardtable.ui.seat;

/** A complete action line above a taller frame; no text height is discarded. */
public final class PlayerSeatMetrics {
    public static final int COMPACT_NAME_REGION_DP = 20;
    public static final int ROOMY_HEADING_RESERVE_DP = 16;
    public record Plan(int badgeHeightPx, int frameTopPx, int frameHeightPx, int heightPx) { }

    /** Preserve the frame's reference heading region when the action moves above its border. */
    public static int referenceFrameHeightPx(int bodyHeightPx, int nameHeightPx, int headingHeightPx) {
        if (bodyHeightPx < nameHeightPx || nameHeightPx < 1 || headingHeightPx < 1) {
            throw new IllegalArgumentException("Invalid frame heading measurements");
        }
        return bodyHeightPx + Math.max(0, headingHeightPx - nameHeightPx);
    }

    public static int frameHeightPx(int naturalHeightPx) {
        if (naturalHeightPx < 1) throw new IllegalArgumentException("Seat content must have a height");
        return Math.toIntExact((naturalHeightPx * 5L + 3) / 4);
    }

    /** One phase-independent badge slot sized for both measured action labels. */
    public static int stableBadgeHeightPx(int biddingBadgeHeightPx, int playingBadgeHeightPx) {
        if (biddingBadgeHeightPx < 1 || playingBadgeHeightPx < 1) {
            throw new IllegalArgumentException("Both phase badges must have positive measured heights");
        }
        return Math.max(biddingBadgeHeightPx, playingBadgeHeightPx);
    }

    public static Plan arrange(int naturalFrameHeightPx, int measuredBadgeHeightPx, int gapPx) {
        return arrange(naturalFrameHeightPx, measuredBadgeHeightPx, gapPx, 0);
    }

    public static Plan arrange(int naturalFrameHeightPx, int measuredBadgeHeightPx, int gapPx,
            int reservedHeightPx) {
        if (measuredBadgeHeightPx < 1 || gapPx < 1 || reservedHeightPx < 0) {
            throw new IllegalArgumentException("Invalid action measurements");
        }
        int frameTop = measuredBadgeHeightPx + gapPx;
        int frameHeight = Math.max(frameHeightPx(naturalFrameHeightPx), reservedHeightPx - frameTop);
        return new Plan(measuredBadgeHeightPx, frameTop, frameHeight, frameTop + frameHeight);
    }

    private PlayerSeatMetrics() { }
}
