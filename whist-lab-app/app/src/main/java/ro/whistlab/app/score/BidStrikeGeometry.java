package ro.whistlab.app.score;

/** A diagonal crosses the bid glyphs and remains inside its own column. */
final class BidStrikeGeometry {
    record Line(float left, float bottom, float right, float top) { }

    static Line across(float left, float top, float right, float bottom, float width, float padding) {
        for (float value : new float[]{left, top, right, bottom, width, padding})
            if (!Float.isFinite(value)) throw new IllegalArgumentException("Non-finite strike bounds");
        if (right <= left || bottom <= top || padding < 0 || width <= 2 * padding)
            throw new IllegalArgumentException("Invalid strike bounds");
        float start = Math.max(padding, left - padding), end = Math.min(width - padding, right + padding);
        if (end <= start) throw new IllegalArgumentException("Strike glyphs are outside the column");
        float inset = (bottom - top) * 0.15f;
        return new Line(start, bottom - inset, end, top + inset);
    }

    private BidStrikeGeometry() { }
}
