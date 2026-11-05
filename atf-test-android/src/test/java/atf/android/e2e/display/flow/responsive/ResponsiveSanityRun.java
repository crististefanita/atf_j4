package atf.android.e2e.display.flow.responsive;

import static atf.android.e2e.display.flow.responsive.ResponsiveDisplayActions.applyProfile;
import static atf.android.e2e.display.flow.responsive.ResponsiveDisplayActions.restoreDisplay;
import static atf.android.e2e.display.flow.responsive.ResponsiveGameFlow.inspectPlayedCard;
import static atf.android.e2e.display.flow.responsive.ResponsiveGameFlow.playShortGame;
import static atf.android.e2e.display.flow.responsive.ResponsiveMenuChecks.inspectHardAiBid;
import static atf.android.e2e.display.flow.responsive.ResponsiveMenuChecks.inspectHomeAndSetup;
import static atf.android.e2e.display.flow.responsive.ResponsiveMenuChecks.inspectNineBidTable;
import static atf.android.e2e.display.flow.responsive.ResponsiveMenuChecks.inspectThirteenCardFan;
import static atf.android.e2e.display.flow.responsive.ResponsiveSettings.APP;
import static atf.android.e2e.display.flow.responsive.ResponsiveSettings.OUTPUT;

import static org.junit.jupiter.api.Assertions.assertEquals;

import atf.android.AdbClient;
import atf.android.e2e.display.profile.ResponsiveDisplayProfile;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.support.AndroidUiFlowReportPublisher;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfReportStatus;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Coordonează profilurile și selecția inspecțiilor responsive. */
public final class ResponsiveSanityRun {
    private ResponsiveSanityRun() {}

    public static void run(String ownerName) throws Exception {
        AdbClient adb = AdbClient.fromSystemProperties();
        List<ResponsiveDisplayProfile> profiles =
                ResponsiveDisplayProfile.parse(System.getProperty("atf.android.sanity.profiles"));
        int hands =
                ResponsiveDisplayProfile.handsToPlay(
                        System.getProperty("atf.android.sanity.hands", "1"));
        boolean includeGameFlow = Boolean.getBoolean("atf.android.sanity.includeGameFlow");
        String configured = System.getProperty(OUTPUT);
        Path root =
                Path.of(
                                configured == null || configured.isBlank()
                                        ? Path.of(
                                                        System.getProperty("java.io.tmpdir"),
                                                        "atf-j4-responsive-sanity")
                                                .toString()
                                        : configured)
                        .resolve("responsive-ui-sanity");
        AndroidUiDriver ui = new AndroidUiDriver(adb, root);
        AndroidUiFlowTrace trace =
                new AndroidUiFlowTrace(
                        root,
                        "responsive-ui-sanity",
                        "Whist Lab — responsive menu, table and card display checks",
                        ownerName,
                        "atf.android.e2e",
                        APP,
                        List.of("android", "e2e", "ui", "responsive", "sanity"));
        ui.recordCapturesAs(trace::capture);
        trace.beginPhase("SETUP");
        ui.recordActionsAs(trace::recordAction);
        List<Path> evidence = new ArrayList<>();
        AtfReportStatus outcome = AtfReportStatus.FAIL;
        Throwable failure = null;
        String originalSize = null;
        String originalDensity = null;
        boolean hardAiCompatibilityRequested =
                Boolean.getBoolean("atf.android.sanity.hardAiCompatibility");

        try {
            originalSize = adb.run("shell", "wm", "size").output();
            originalDensity = adb.run("shell", "wm", "density").output();
            trace.beginPhase("TEST BODY");
            boolean hardAiCompatibilityChecked = !hardAiCompatibilityRequested;
            boolean primaryProfile = true;
            for (ResponsiveDisplayProfile profile : profiles) {
                applyProfile(adb, ui, profile);
                Thread.sleep(700);
                if (!hardAiCompatibilityChecked) {
                    inspectHardAiBid(adb, ui, trace, evidence, profile);
                    hardAiCompatibilityChecked = true;
                }
                inspectPlayedCard(adb, ui, trace, evidence, profile);
                if (primaryProfile && includeGameFlow) {
                    playShortGame(adb, ui, trace, evidence, profile, hands);
                } else {
                    inspectHomeAndSetup(ui, adb, trace, evidence, profile);
                }
                inspectThirteenCardFan(adb, ui, trace, evidence, profile);
                inspectNineBidTable(adb, ui, trace, evidence, profile);
                primaryProfile = false;
            }
            outcome = AtfReportStatus.PASS;
        } catch (Throwable caught) {
            failure = caught;
            trace.fail(caught);
        }

        trace.beginPhase("TEARDOWN");
        try {
            if (originalSize != null && originalDensity != null)
                restoreDisplay(adb, ui, originalSize, originalDensity);
            adb.run("shell", "am", "force-stop", APP).requireSuccess();
        } catch (Throwable cleanup) {
            trace.fail(cleanup);
            if (failure == null) failure = cleanup;
            else failure.addSuppressed(cleanup);
        }

        if (failure != null) outcome = AtfReportStatus.FAIL;
        trace.finish(outcome);
        evidence.add(trace.manifest());
        try {
            AndroidUiFlowReportPublisher.publish(
                    root.resolve("responsive-ui-sanity.html"),
                    APP,
                    System.getProperty("atf.android.serial", System.getenv("ATF_ANDROID_SERIAL")),
                    System.getProperty("atf.android.api", "read from Android at runtime"),
                    "Configured portrait profiles use display fixtures; completed game flow runs once "
                            + "on the first profile only when enabled. Hands="
                            + hands
                            + "; game flow="
                            + includeGameFlow
                            + "; Hard-AI compatibility="
                            + hardAiCompatibilityRequested
                            + ".",
                    evidence,
                    trace);
        } catch (Throwable publishing) {
            if (failure == null) failure = publishing;
            else failure.addSuppressed(publishing);
        }
        if (failure instanceof Exception exception) throw exception;
        if (failure instanceof Error error) throw error;
        if (failure != null) throw new RuntimeException(failure);
        assertEquals(AtfReportStatus.PASS, outcome);
    }
}
