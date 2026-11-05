package atf.android.e2e.display.support;

import static atf.android.e2e.display.support.DisplayActions.runDisplaySetting;

import atf.android.AdbClient;
import atf.android.e2e.driver.AndroidUiDriver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Controlează profilul de display și verifică restaurarea stării observate. */
public final class GameplayDisplay {
    private GameplayDisplay() {}

    public static final Pattern OVERRIDE_DENSITY =
            Pattern.compile("(?m)^Override density:\\s*(\\d+)");

    public static final Pattern PHYSICAL_DENSITY =
            Pattern.compile("(?m)^Physical density:\\s*(\\d+)");

    public static final Pattern OVERRIDE_SIZE =
            Pattern.compile("(?m)^Override size:\\s*(\\d+)x(\\d+)");

    public static final Pattern PHYSICAL_SIZE =
            Pattern.compile("(?m)^Physical size:\\s*(\\d+)x(\\d+)");

    public static double effectiveDensityScale(String output) {
        Matcher override = OVERRIDE_DENSITY.matcher(output);
        if (override.find()) return Integer.parseInt(override.group(1)) / 160.0;
        Matcher physical = PHYSICAL_DENSITY.matcher(output);
        if (physical.find()) return Integer.parseInt(physical.group(1)) / 160.0;
        throw new IllegalStateException("Android display density was not reported: " + output);
    }

    public static void overrideDisplay(AdbClient adb, AndroidUiDriver ui, DisplayProfile profile)
            throws Exception {
        runDisplaySetting(
                adb,
                ui,
                "Set emulator display resolution",
                "Android display resolution",
                "shell",
                "wm",
                "size",
                profile.width() + "x" + profile.height());
        runDisplaySetting(
                adb,
                ui,
                "Set emulator display density",
                "Android display density",
                "shell",
                "wm",
                "density",
                Integer.toString(profile.density()));
        Thread.sleep(700);
    }

    public static void restoreDisplay(
            AdbClient adb, AndroidUiDriver ui, String sizeOverride, String densityOverride)
            throws Exception {
        if (sizeOverride == null) {
            runDisplaySetting(
                    adb,
                    ui,
                    "Restore emulator display resolution",
                    "Android display resolution",
                    "shell",
                    "wm",
                    "size",
                    "reset");
        } else {
            runDisplaySetting(
                    adb,
                    ui,
                    "Restore emulator display resolution",
                    "Android display resolution",
                    "shell",
                    "wm",
                    "size",
                    sizeOverride);
        }
        if (densityOverride == null) {
            runDisplaySetting(
                    adb,
                    ui,
                    "Restore emulator display density",
                    "Android display density",
                    "shell",
                    "wm",
                    "density",
                    "reset");
        } else {
            runDisplaySetting(
                    adb,
                    ui,
                    "Restore emulator display density",
                    "Android display density",
                    "shell",
                    "wm",
                    "density",
                    densityOverride);
        }
        Thread.sleep(700);
    }

    public static String effectiveDisplaySize(String commandOutput) {
        String override = overrideValue(OVERRIDE_SIZE, commandOutput);
        if (override != null) return override;
        String physical = overrideValue(PHYSICAL_SIZE, commandOutput);
        if (physical == null)
            throw new IllegalArgumentException("ADB did not report a display size");
        return physical;
    }

    public static String overrideValue(Pattern pattern, String commandOutput) {
        Matcher matcher = pattern.matcher(commandOutput);
        return matcher.find() ? matcher.group(1) + "x" + matcher.group(2) : null;
    }

    public static String overrideDensityValue(String commandOutput) {
        Matcher matcher = OVERRIDE_DENSITY.matcher(commandOutput);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static boolean matchesRestoredDisplay(
            AdbClient adb, String originalSize, String originalDensity) throws Exception {
        String currentSize = adb.run("shell", "wm", "size").output();
        String currentDensity = adb.run("shell", "wm", "density").output();
        return displayState(currentSize, currentDensity)
                .equals(displayState(originalSize, originalDensity));
    }

    public static String displayState(String sizeOutput, String densityOutput) {
        return "size="
                + firstState(OVERRIDE_SIZE, PHYSICAL_SIZE, sizeOutput, true)
                + ";density="
                + firstState(OVERRIDE_DENSITY, PHYSICAL_DENSITY, densityOutput, false);
    }

    public static String firstState(
            Pattern overridePattern, Pattern physicalPattern, String output, boolean dimensions) {
        Matcher override = overridePattern.matcher(output);
        if (override.find())
            return "override:" + override.group(1) + (dimensions ? "x" + override.group(2) : "");
        Matcher physical = physicalPattern.matcher(output);
        if (physical.find())
            return "physical:" + physical.group(1) + (dimensions ? "x" + physical.group(2) : "");
        return "unavailable";
    }

    public record DisplayProfile(String name, int width, int height, int density) {
        public String fileKey() {
            return width + "x" + height + "-" + density + "dpi";
        }
    }
}
