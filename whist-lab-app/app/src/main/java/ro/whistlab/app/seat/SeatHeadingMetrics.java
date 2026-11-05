package ro.whistlab.app.seat;

/** Layout decisions use measured glyph widths, independently of the Android renderer. */
final class SeatHeadingMetrics {
    record Plan(boolean inline, int nameWidthPx, int badgeWidthPx, int gapPx) { }

    static Plan arrange(int widthPx, int nameWidthPx, int badgeWidthPx, int gapPx,
            int minimumNameWidthPx) {
        if (widthPx < 1 || nameWidthPx < 0 || badgeWidthPx < 1 || gapPx < 0
                || minimumNameWidthPx < 1) throw new IllegalArgumentException("Invalid heading measurements");
        int nameMinimum = Math.min(nameWidthPx, minimumNameWidthPx);
        if (badgeWidthPx + gapPx + nameMinimum <= widthPx) {
            return new Plan(true, widthPx - badgeWidthPx - gapPx, badgeWidthPx, gapPx);
        }
        return new Plan(false, widthPx, widthPx, 0);
    }

    private SeatHeadingMetrics() { }
}
