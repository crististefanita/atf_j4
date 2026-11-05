package ro.whistlab.app.window;

/** Immutable base spacing keeps repeated inset dispatches from accumulating padding. */
record WindowPadding(int left, int top, int right, int bottom) {
    WindowPadding {
        if (left < 0 || top < 0 || right < 0 || bottom < 0) {
            throw new IllegalArgumentException("Window padding cannot be negative");
        }
    }

    WindowPadding plus(WindowPadding protection) {
        return new WindowPadding(Math.addExact(left, protection.left), Math.addExact(top, protection.top),
                Math.addExact(right, protection.right), Math.addExact(bottom, protection.bottom));
    }

    static int usableDp(int fullPixels, int before, int after, float density) {
        if (before < 0 || after < 0 || fullPixels <= (long) before + after
                || !Float.isFinite(density) || density <= 0) {
            throw new IllegalArgumentException("Window has no valid unobscured dimension");
        }
        return Math.max(1, Math.round((fullPixels - before - after) / density));
    }
}
