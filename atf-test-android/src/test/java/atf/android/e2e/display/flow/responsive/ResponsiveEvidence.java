package atf.android.e2e.display.flow.responsive;

import static org.junit.jupiter.api.Assertions.assertTrue;

import atf.android.AdbClient;
import atf.android.evidence.DisplayRaster;
import atf.android.e2e.display.profile.ResponsiveDisplayProfile;
import atf.android.e2e.driver.AndroidUiDriver;
import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

/** Corelează capturile, măsurătorile și checkpoint-urile verificării responsive. */
public final class ResponsiveEvidence {
    private ResponsiveEvidence() {}

    public static void record(
            AndroidUiDriver ui,
            AdbClient adb,
            AndroidUiFlowTrace trace,
            List<Path> evidence,
            ResponsiveDisplayProfile profile,
            String title,
            List<AtfAssertion> checks)
            throws Exception {
        List<Path> capture = ui.capture(profile.id() + "-" + title.replace(' ', '-'));
        evidence.addAll(capture);
        List<AtfAssertion> recordedChecks = new ArrayList<>(checks);
        checkCaptureSize(adb, capture, profile, recordedChecks);
        publishCheckpoint(ui, trace, profile, title, capture, recordedChecks);
    }

    public static void checkCaptureSize(
            AdbClient adb,
            List<Path> capture,
            ResponsiveDisplayProfile profile,
            List<AtfAssertion> checks)
            throws Exception {
        String sizeOutput = adb.run("shell", "wm", "size").output();
        int[] logical = AndroidUiDriver.displayDimensions(sizeOutput);
        int[] physical = physicalDisplayDimensions(sizeOutput);
        BufferedImage image =
                ImageIO.read(
                        capture.stream()
                                .filter(path -> path.toString().endsWith(".png"))
                                .findFirst()
                                .orElseThrow()
                                .toFile());

        boolean profileApplied = logical[0] == profile.width() && logical[1] == profile.height();
        checks.add(
                check(
                        "Logical viewport matches configured profile",
                        profile.width() + "x" + profile.height(),
                        logical[0] + "x" + logical[1],
                        profileApplied));

        var physicalSize = new DisplayRaster.Size(physical[0], physical[1]);
        var logicalSize = new DisplayRaster.Size(logical[0], logical[1]);
        var mode = image == null ? DisplayRaster.Mode.MISMATCH : DisplayRaster.classify(
                new DisplayRaster.Size(image.getWidth(), image.getHeight()), physicalSize, logicalSize);
        checks.add(
                check(
                        "PNG captures a complete active display raster",
                        physicalSize + " physical or " + logicalSize + " logical",
                        image == null ? "invalid PNG" : image.getWidth() + "x" + image.getHeight() + " (" + mode + ")",
                        mode != DisplayRaster.Mode.MISMATCH));
    }

    public static int[] physicalDisplayDimensions(String sizeOutput) throws IOException {
        Matcher physical =
                Pattern.compile("(?m)^Physical size:\\s*(\\d+)x(\\d+)").matcher(sizeOutput);
        if (!physical.find())
            throw new IOException("Physical Android display size is unavailable: " + sizeOutput);
        return new int[] {Integer.parseInt(physical.group(1)), Integer.parseInt(physical.group(2))};
    }

    public static void publishCheckpoint(
            AndroidUiDriver ui,
            AndroidUiFlowTrace trace,
            ResponsiveDisplayProfile profile,
            String title,
            List<Path> capture,
            List<AtfAssertion> checks)
            throws Exception {
        boolean passed = checks.stream().allMatch(AtfAssertion::passed);
        trace.checkpoint(
                trace.activePhaseName(),
                trace.phaseSnapshots().stream().allMatch(value -> value.checkpoints().isEmpty())
                        ? "GIVEN"
                        : "AND",
                profile.id() + " — " + title,
                "Rendered Android screen",
                capture.stream()
                        .filter(path -> path.toString().endsWith(".png"))
                        .findFirst()
                        .orElseThrow(),
                List.of(
                        new AtfUiActionRecord(
                                trace.nextActionSequence(),
                                "Inspect rendered screen",
                                title,
                                "All named controls and game regions fit their display profile",
                                passed ? "All comparisons passed" : "A layout comparison failed",
                                0,
                                passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL,
                                AtfUiEvidenceSource.OBSERVE)),
                checks,
                passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL,
                capture.stream().filter(path -> path.toString().endsWith(".xml")).toList());
        for (AtfAssertion assertion : checks) {
            assertTrue(
                    assertion.passed(),
                    profile.id()
                            + " / "
                            + title
                            + " / "
                            + assertion.name()
                            + ": "
                            + assertion.actual());
        }
    }

    public static AtfAssertion check(String name, Object expected, Object actual) {
        return new AtfAssertion(
                name, String.valueOf(expected), String.valueOf(actual), expected.equals(actual));
    }

    public static AtfAssertion check(String name, String expected, boolean passed) {
        return new AtfAssertion(name, expected, Boolean.toString(passed), passed);
    }

    public static AtfAssertion check(String name, Object expected, Object actual, boolean passed) {
        return new AtfAssertion(name, String.valueOf(expected), String.valueOf(actual), passed);
    }
}
