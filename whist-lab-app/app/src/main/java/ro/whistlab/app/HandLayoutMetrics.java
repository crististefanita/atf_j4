package ro.whistlab.app;

/** Compact portrait-hand rules shared by the renderer and its layout contract tests. */
final class HandLayoutMetrics {
    private static final int SINGLE_ROW_MIN_CARDS = 5;
    private static final int SINGLE_ROW_MAX_CARDS = 8;
    private static final int DEFAULT_COLUMNS = 4;

    private HandLayoutMetrics() { }

    static boolean usesSingleRow(int cardCount) {
        return cardCount >= SINGLE_ROW_MIN_CARDS && cardCount <= SINGLE_ROW_MAX_CARDS;
    }

    static int columnCount(int cardCount) {
        return usesSingleRow(cardCount) ? cardCount : DEFAULT_COLUMNS;
    }

    static int cardHeightDp(int cardCount) {
        return usesSingleRow(cardCount) ? 58 : 72;
    }

    static int horizontalMarginDp(int cardCount) {
        return usesSingleRow(cardCount) ? 1 : 2;
    }

    static int cardLabelSizeSp(boolean compactSingleRow) {
        return compactSingleRow ? 14 : 21;
    }
}
