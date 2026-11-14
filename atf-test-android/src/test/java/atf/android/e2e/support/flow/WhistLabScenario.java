package atf.android.e2e.support.flow;

import static org.junit.jupiter.api.Assertions.assertTrue;

import atf.android.AdbClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Ținta aplicației, verificarea instalării și metadatele păstrate pentru dovezi. */
public final class WhistLabScenario {
    private WhistLabScenario() {}

    public static final String PACKAGE = "ro.whistlab.app";

    public static Path artifacts(String flow) {
        Path root = configuredExternalPath("atf.android.artifacts");
        Path flowPath = Path.of(flow);
        if (flow.isBlank() || flowPath.getRoot() != null || flowPath.getNameCount() != 1
                || ".".equals(flow) || "..".equals(flow)) {
            throw new IllegalArgumentException("Android E2E flow output must be a single directory name.");
        }
        return externalPath(root.resolve(flowPath).toString(), "Android E2E flow output");
    }

    public static Path configuredExternalPath(String property) {
        return externalPath(System.getProperty(property), property);
    }

    public static Path externalPath(String configured, String purpose) {
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("Set " + purpose + " to a path outside the ATF checkout.");
        }
        Path path = realPath(Path.of(configured).toAbsolutePath().normalize());
        if (path.startsWith(realPath(checkoutRoot()))) {
            throw new IllegalStateException(purpose + " must stay outside the ATF checkout.");
        }
        return path;
    }

    private static Path realPath(Path path) {
        Path existing = path;
        while (existing != null && !Files.exists(existing)) existing = existing.getParent();
        if (existing == null) return path;
        try {
            return existing.toRealPath().resolve(existing.relativize(path)).normalize();
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot resolve " + path + "; refusing E2E file output.", failure);
        }
    }

    static Path checkoutRoot() {
        for (String property : new String[] {
                "atf.governance.gitRoot", "maven.multiModuleProjectDirectory", "user.dir"
        }) {
            String configured = System.getProperty(property);
            if (configured == null || configured.isBlank()) continue;
            Path start = Path.of(configured).toAbsolutePath().normalize();
            for (Path candidate = start; candidate != null; candidate = candidate.getParent()) {
                if (Files.isDirectory(candidate.resolve("atf-test-android"))
                        && Files.isDirectory(candidate.resolve("whist-lab-app"))) {
                    return candidate;
                }
            }
        }
        throw new IllegalStateException("Cannot locate the ATF checkout; refusing Android E2E artifact output.");
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
