package atf.android.e2e.display.flow.responsive;

import java.time.Duration;
import java.util.regex.Pattern;

/** Contractele declarative ale verificării responsive. */
public final class ResponsiveSettings {
    private ResponsiveSettings() {}

    public static final String APP = "ro.whistlab.app";

    public static final Duration WAIT = Duration.ofSeconds(35);

    public static final int HAND_CARD_FACE_HEIGHT_DP = 98;

    public static final String OUTPUT = "atf.android.artifacts";

    public static final Pattern HAND_SIZE = Pattern.compile("Your hand, (\\d+) cards\\.");

    public static final Pattern DEAL_NUMBER = Pattern.compile("Hand (\\d+) / \\d+");
}
