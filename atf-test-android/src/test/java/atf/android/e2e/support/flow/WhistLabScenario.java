package atf.android.e2e.support.flow;

import static org.junit.jupiter.api.Assertions.assertTrue;

import atf.android.AdbClient;

import java.nio.file.Path;

/** Ținta aplicației, verificarea instalării și metadatele păstrate pentru dovezi. */
public final class WhistLabScenario {
    private WhistLabScenario() {}

    public static final String PACKAGE = "ro.whistlab.app";

    public static Path artifacts(String flow) {
        String configured = System.getProperty("atf.android.artifacts");
        Path root =
                configured == null || configured.isBlank()
                        ? Path.of(System.getProperty("java.io.tmpdir"), "atf-j4-android-artifacts")
                        : Path.of(configured);
        return root.resolve(flow);
    }

    public static String reportSetting(String property, String environment, String fallback) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) value = System.getenv(environment);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public static InstalledApp installedApp() throws Exception {
        AdbClient adb = AdbClient.fromSystemProperties();
        var packagePath = adb.run("shell", "pm", "path", PACKAGE);
        packagePath.requireSuccess();
        assertTrue(
                packagePath.output().contains("package:"),
                "Whist Lab is not installed: " + packagePath.output());
        return new InstalledApp(adb, packagePath.output());
    }

    public record InstalledApp(AdbClient adb, String packagePath) {}
}
