package atf.android.e2e.records.legend;

import atf.android.e2e.display.profile.ResponsiveDisplayProfile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** A bounded native sample; invalid configuration is rejected without touching Android. */
public final class LegendProfiles {
    public static final String PROPERTY = "whist.history.legend.cases";
    private static final String DEFAULTS = "720x1280@360:1.0:EN,720x1280@360:1.0:RO,"
            + "720x1280@360:1.5:EN,720x1280@360:1.5:RO,1080x2448@440:1.3:EN";
    public enum Language { EN, RO }
    public record Case(ResponsiveDisplayProfile profile, float fontScale, Language language) {
        public Case {
            Objects.requireNonNull(profile);Objects.requireNonNull(language);
            if (!Float.isFinite(fontScale) || fontScale < 1 || fontScale > 1.5f)
                throw new IllegalArgumentException("Legend font scale must be between 1.0 and 1.5");
        }
        public String resolution() { return profile.width() + "x" + profile.height(); }
        public String id() { return profile.id() + "-font-" + fontScale + "-" + language; }
        public boolean romanian() { return language == Language.RO; }
        public boolean requiresColumnMovement() { return fontScale == 1.5f && profile.width() * 160f / profile.density() <= 360; }
        public int minimumRowPixels() { return Math.round(40 * profile.density() / 160f); }
    }
    private LegendProfiles() { }

    public static List<Case> parse(String configured) {
        String[] entries = (configured == null ? DEFAULTS : configured).split(",", -1);
        if (entries.length > 8) throw new IllegalArgumentException("At most eight native legend cases are supported");
        var result = new ArrayList<Case>();var identities = new HashSet<String>();
        for (String entry : entries) {
            String[] parts = entry.strip().split(":", -1);
            if (parts.length != 3) throw new IllegalArgumentException("Expected WIDTHxHEIGHT@DPI:SCALE:EN|RO: " + entry);
            var profiles = ResponsiveDisplayProfile.parse(parts[0].strip());
            if (profiles.size() != 1) throw new IllegalArgumentException("Each legend case requires exactly one viewport");
            var sample = new Case(profiles.get(0), Float.parseFloat(parts[1].strip()),
                    Language.valueOf(parts[2].strip().toUpperCase(Locale.ROOT)));
            if (!identities.add(sample.id())) throw new IllegalArgumentException("Duplicate legend case: " + sample.id());
            result.add(sample);
        }
        return List.copyOf(result);
    }
}
