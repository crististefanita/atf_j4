package atf.android.e2e.display.flow.responsive;

import static atf.android.e2e.display.support.DisplayActions.runDisplayAction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import atf.android.AdbClient;
import atf.android.e2e.display.profile.ResponsiveDisplayProfile;
import atf.android.e2e.driver.AndroidUiDriver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Aplică și restaurează profilurile verificării responsive. */
public final class ResponsiveDisplayActions {
    private ResponsiveDisplayActions() {}

    public static void applyProfile(
            AdbClient adb, AndroidUiDriver ui, ResponsiveDisplayProfile profile) throws Exception {
        runDisplayAction(
                adb,
                ui,
                "Set emulator resolution",
                "Display resolution " + profile.id(),
                "shell",
                "wm",
                "size",
                profile.width() + "x" + profile.height());
        runDisplayAction(
                adb,
                ui,
                "Set emulator density",
                "Display density " + profile.id(),
                "shell",
                "wm",
                "density",
                Integer.toString(profile.density()));
    }

    public static void restoreDisplay(
            AdbClient adb, AndroidUiDriver ui, String originalSize, String originalDensity)
            throws Exception {
        restoreSetting(adb, ui, "size", originalSize);
        restoreSetting(adb, ui, "density", originalDensity);
        String afterSize = adb.run("shell", "wm", "size").output();
        String afterDensity = adb.run("shell", "wm", "density").output();
        assertEquals(
                displayState(originalSize, originalDensity),
                displayState(afterSize, afterDensity),
                "Emulator display overrides must be restored");
    }

    public static void restoreSetting(
            AdbClient adb, AndroidUiDriver ui, String property, String output) throws Exception {
        Matcher override =
                Pattern.compile("(?m)^Override " + property + ":\\s*(\\S+)").matcher(output);
        String[] command =
                override.find()
                        ? new String[] {"shell", "wm", property, override.group(1)}
                        : new String[] {"shell", "wm", property, "reset"};
        runDisplayAction(
                adb, ui, "Restore emulator " + property, "Original display " + property, command);
    }

    public static String displayState(String size, String density) {
        return state(size, "size") + ";" + state(density, "density");
    }

    public static String state(String output, String property) {
        Matcher override =
                Pattern.compile("(?m)^Override " + property + ":\\s*(\\S+)").matcher(output);
        if (override.find()) return "override=" + override.group(1);
        Matcher physical =
                Pattern.compile("(?m)^Physical " + property + ":\\s*(\\S+)").matcher(output);
        if (physical.find()) return "physical=" + physical.group(1);
        return "unavailable";
    }
}
