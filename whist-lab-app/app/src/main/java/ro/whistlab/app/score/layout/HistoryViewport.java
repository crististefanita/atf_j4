package ro.whistlab.app.score.layout;

/** Preserves measured headers and wrapped legend before allocating scrollable rows. */
public final class HistoryViewport {
    private HistoryViewport() { }

    public static int bodyHeight(int preferredBody, int naturalContent, int availableContent) {
        if (preferredBody < 0 || naturalContent < preferredBody || availableContent < 0)
            throw new IllegalArgumentException("History dimensions must be nonnegative and include the requested body");
        int fixedContent = naturalContent - preferredBody;
        return Math.max(0, availableContent - fixedContent);
    }

    public static boolean needsWholeScroll(int preferredBody, int naturalContent, int availableContent, int minimumRow) {
        if (minimumRow <= 0) throw new IllegalArgumentException("A readable row requires positive height");
        return bodyHeight(preferredBody, naturalContent, availableContent) < minimumRow;
    }

    public record DialogSize(int width, int height) { }

    /** Uses the usable window, with proportional phone margins capped on tablets. */
    public static DialogSize dialogSize(int width, int height, float density) {
        if (width <= 0 || height <= 0 || !Float.isFinite(density) || density <= 0)
            throw new IllegalArgumentException("A dialog requires positive window dimensions and density");
        int cap = Math.max(1, Math.round(24 * density));
        int horizontal = Math.min(cap, Math.max(1, Math.round(width * .025f)));
        int vertical = Math.min(cap, Math.max(1, Math.round(height * .02f)));
        return new DialogSize(Math.max(1, width - 2 * horizontal), Math.max(1, height - 2 * vertical));
    }
}
