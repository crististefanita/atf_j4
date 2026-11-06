package ro.whistlab.app.score.layout;

/** A diagonal crosses the bid glyphs and remains inside its own column. */
public final class BidStrikeGeometry {
    public static final int COLOR = 0xffb3261e;
    public record Line(float left, float bottom, float right, float top) { }

    public static Line across(float left, float top, float right, float bottom, float width, float padding) {
        for (float value : new float[]{left, top, right, bottom, width, padding})
            if (!Float.isFinite(value)) throw new IllegalArgumentException("Non-finite strike bounds");
        if (right <= left || bottom <= top || padding < 0 || width <= 2 * padding)
            throw new IllegalArgumentException("Invalid strike bounds");
        float start = Math.max(padding, left - padding), end = Math.min(width - padding, right + padding);
        if (end <= start) throw new IllegalArgumentException("Strike glyphs are outside the column");
        float inset = (bottom - top) * 0.15f;
        return new Line(start, bottom - inset, end, top + inset);
    }

    public static float strokeWidth(float density) {
        if (!Float.isFinite(density) || density <= 0) throw new IllegalArgumentException("Invalid strike density");
        return Math.max(1.5f, 1.8f * density);
    }

    private BidStrikeGeometry() { }
}
