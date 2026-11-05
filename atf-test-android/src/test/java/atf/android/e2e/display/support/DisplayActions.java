package atf.android.e2e.display.support;

import atf.android.AdbClient;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.reporting.contract.AtfReportStatus;

import java.time.Duration;

/** Execută și trasează o comandă de configurare a display-ului. */
public final class DisplayActions {
    private DisplayActions() {}

    public static void runDisplaySetting(
            AdbClient adb, AndroidUiDriver ui, String operation, String target, String... arguments)
            throws Exception {
        long started = System.nanoTime();
        try {
            var result = adb.run(arguments);
            result.requireSuccess();
            ui.recordAction(
                    operation,
                    target,
                    String.join(" ", arguments),
                    result.output().isBlank()
                            ? "ADB accepted display setting"
                            : result.output().trim(),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    AtfReportStatus.PASS);
        } catch (Exception failure) {
            ui.recordAction(
                    operation,
                    target,
                    String.join(" ", arguments),
                    failure.toString(),
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    AtfReportStatus.FAIL);
            throw failure;
        }
    }

    public static void runDisplayAction(
            AdbClient adb, AndroidUiDriver ui, String operation, String target, String... command)
            throws Exception {
        runDisplaySetting(adb, ui, operation, target, command);
    }
}
