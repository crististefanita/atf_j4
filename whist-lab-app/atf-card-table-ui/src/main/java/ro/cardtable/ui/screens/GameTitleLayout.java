package ro.cardtable.ui.screens;

/** Horizontal space allocation for a leading action, title and optional trailing action. */
record GameTitleLayout(int titleLeft, int titleWidth, boolean trailingOnTitleLine) {
    static GameTitleLayout measure(int width, int leadingWidth, int gap, int titleWidth, int trailingWidth) {
        int titleLeft = Math.min(width, leadingWidth + gap);
        int available = width - titleLeft;
        boolean inline = trailingWidth > 0 && titleWidth + gap + trailingWidth <= available;
        return new GameTitleLayout(titleLeft, inline ? available - gap - trailingWidth : available, inline);
    }
}
