package ro.whistlab.app.score.layout;

/** Preserves measured headers and wrapped legend before allocating scrollable rows. */
public final class HistoryViewport {
    private HistoryViewport() { }

    public static int bodyHeight(int preferredBody, int naturalContent, int availableContent) {
        if (preferredBody < 0 || naturalContent < preferredBody || availableContent < 0)
            throw new IllegalArgumentException("History dimensions must be nonnegative and include the requested body");
        int fixedContent = naturalContent - preferredBody;
        return Math.min(preferredBody, Math.max(0, availableContent - fixedContent));
    }
}
