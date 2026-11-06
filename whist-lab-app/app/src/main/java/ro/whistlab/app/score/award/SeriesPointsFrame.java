package ro.whistlab.app.score.award;

/** A series rectangle stays inside the points cell, with clearance around the complete numeric text. */
public final class SeriesPointsFrame {
    public record Rectangle(float left, float top, float right, float bottom) { }
    public static final int BONUS_INK = 0xff29362e;
    public static final int PENALTY_INK = 0xff97462e;

    public static int requiredWidth(int textWidth, int unit) { return padded(textWidth, unit); }
    public static int requiredHeight(int textHeight, int unit) { return padded(textHeight, unit); }

    public static int color(int seriesPoints) {
        if (seriesPoints == 0) throw new IllegalArgumentException("A series adjustment is required");
        return seriesPoints < 0 ? PENALTY_INK : BONUS_INK;
    }

    private static int padded(int textExtent, int unit) {
        if (textExtent < 0 || unit < 0)
            throw new IllegalArgumentException("Invalid series text measurements");
        try { return Math.addExact(textExtent, Math.multiplyExact(6, unit)); }
        catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("Series frame exceeds the pixel range", overflow);
        }
    }

    /** Null means the parent constrained the cell below its measured requirement; never draw into a neighbour. */
    public static Rectangle inside(int width, int height, int frameHeight, int stroke) {
        if (width < 0 || height < 0 || stroke <= 0 || frameHeight <= stroke)
            throw new IllegalArgumentException("Invalid series frame bounds");
        if (width <= stroke || height < frameHeight) return null;
        float top = (height - frameHeight) / 2f + stroke / 2f;
        return new Rectangle(stroke / 2f, top, width - stroke / 2f, top + frameHeight - stroke);
    }

    private SeriesPointsFrame() { }
}
