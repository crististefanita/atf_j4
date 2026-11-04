package atf.android.e2e.display.profile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ResponsiveDisplayProfile(int width, int height, int density) {
    static final String DEFAULTS =
            "720x1280@360,1080x1920@440,1080x2448@440,1200x1920@320";
    private static final Pattern PROFILE = Pattern.compile("(\\d{3,4})x(\\d{3,4})@(\\d{3})");

    public ResponsiveDisplayProfile {
        if (width < 480 || width > 1920 || height < 800 || height > 3200
                || width >= height || density < 160 || density > 640
                || width * 160 / density < 320) {
            throw new IllegalArgumentException("Unsupported portrait display profile: "
                    + width + "x" + height + "@" + density);
        }
    }

    public static List<ResponsiveDisplayProfile> parse(String value) {
        String configured = value == null || value.isBlank() ? DEFAULTS : value.trim();
        String[] entries = configured.split(",", -1);
        if (entries.length > 4) {
            throw new IllegalArgumentException("At most four display profiles are supported");
        }

        List<ResponsiveDisplayProfile> profiles = new ArrayList<>();
        HashSet<String> identities = new HashSet<>();
        for (String entry : entries) {
            Matcher match = PROFILE.matcher(entry.trim());
            if (!match.matches()) {
                throw new IllegalArgumentException(
                        "Expected comma-separated WIDTHxHEIGHT@DENSITY profiles: " + entry);
            }
            ResponsiveDisplayProfile profile = new ResponsiveDisplayProfile(
                    Integer.parseInt(match.group(1)),
                    Integer.parseInt(match.group(2)),
                    Integer.parseInt(match.group(3)));
            if (!identities.add(profile.id())) {
                throw new IllegalArgumentException("Duplicate display profile: " + profile.id());
            }
            profiles.add(profile);
        }
        return List.copyOf(profiles);
    }

    public static int handsToPlay(String value) {
        int hands;
        try {
            hands = Integer.parseInt(value == null || value.isBlank() ? "1" : value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Hands to play must be between 1 and 5", exception);
        }
        if (hands < 1 || hands > 5) {
            throw new IllegalArgumentException("Hands to play must be between 1 and 5");
        }
        return hands;
    }

    public String id() {
        return width + "x" + height + "-" + density + "dpi";
    }

    int logicalHeightDp() {
        return Math.round(height * 160f / density);
    }

    public int playedCardFaceHeightDp() {
        return logicalHeightDp() < 600 ? 96 : 104;
    }
}
