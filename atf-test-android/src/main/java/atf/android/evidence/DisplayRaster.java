package atf.android.evidence;

/** Native screencap may expose either the physical panel or its active logical raster. */
public final class DisplayRaster {
    private DisplayRaster() {}

    public enum Mode { PHYSICAL, LOGICAL, MISMATCH }

    public record Size(int width, int height) {
        public Size {
            if (width <= 0 || height <= 0) throw new IllegalArgumentException("Display dimensions must be positive");
        }

        @Override public String toString() { return width + "x" + height; }
    }

    public static Mode classify(Size capture, Size physical, Size logical) {
        if (capture.equals(physical)) return Mode.PHYSICAL;
        if (capture.equals(logical)) return Mode.LOGICAL;
        return Mode.MISMATCH;
    }
}
