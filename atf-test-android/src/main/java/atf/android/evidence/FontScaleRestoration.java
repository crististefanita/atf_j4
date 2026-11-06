package atf.android.evidence;

/** Distinguishes an absent Android setting from a numeric scale; neither state is replaced with a default. */
public final class FontScaleRestoration {
    private FontScaleRestoration() { }
    public static String[] command(String observed) {
        if ("null".equals(observed)) return new String[] {"shell", "settings", "delete", "system", "font_scale"};
        if (observed == null || observed.isBlank()) throw new IllegalArgumentException("Original font scale was not observed");
        try {
            if (new java.math.BigDecimal(observed).signum() <= 0) throw new NumberFormatException("Nonpositive scale");
        } catch (NumberFormatException invalid) { throw new IllegalArgumentException("Invalid original font scale", invalid); }
        return new String[] {"shell", "settings", "put", "system", "font_scale", observed};
    }
}
