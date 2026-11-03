package atf.android.e2e.support;

import atf.android.evidence.AndroidScreenshotEvidence;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import atf.reporting.ui.UiExtentReportSession;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Publishes ordered Android checkpoints through the same public UI evidence API used by Playwright. */
public final class AndroidUiFlowReportPublisher {
    private AndroidUiFlowReportPublisher() { }

    public static Path publish(Path report, String installedPackage, String device, String androidApi,
            String summary, List<Path> files, AndroidUiFlowTrace trace) throws Exception {
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(trace, "trace");
        List<Path> evidenceFiles = files == null ? List.of() : List.copyOf(files);
        Path output = report.toAbsolutePath().normalize();
        Files.createDirectories(output.getParent());

        try (var session = new ExtentReportSession(output,
                AtfReportConfigurationLoader.load().reportingSettings())) {
            AtfReportPorts.Scenario raw = session.startScenario(
                    trace.testClass(), trace.testPackage(), trace.name(), trace.tags());
            publishRunReceipt(raw, trace, installedPackage, device, androidApi, summary, evidenceFiles);

            var ui = UiExtentReportSession.adaptScenario(raw,
                    ReportEvidenceProfile.forScreenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT),
                    trace.name());
            for (AndroidUiFlowTrace.PhaseSnapshot phaseSnapshot : trace.phaseSnapshots()) {
                try (var phase = ui.startPhase(phaseSnapshot.name())) {
                    for (AndroidUiFlowTrace.CheckpointSnapshot checkpoint : phaseSnapshot.checkpoints()) {
                        String screenshot = checkpoint.screenshot() == null ? ""
                                : AndroidScreenshotEvidence.from(checkpoint.screenshot()).base64Png();
                        phase.step(AtfGherkinKeyword.valueOf(checkpoint.keyword()),
                                checkpoint.title(), checkpoint.target(), screenshot,
                                checkpoint.assertions(), checkpoint.actions(),
                                ReportScreenshotPolicy.CapturePoint.CHECKPOINT, checkpoint.status());
                    }
                    phase.complete(phaseStatus(phaseSnapshot.checkpoints()));
                }
            }
            if (!trace.failure().isBlank()) {
                try (var failurePhase = ui.startPhase("EXECUTION DIAGNOSTICS")) {
                    failurePhase.failureEvidence("Android flow stopped after an uncaught failure",
                            trace.name(), trace.pendingActions(), new IllegalStateException(trace.failure()));
                    failurePhase.complete(AtfReportStatus.FAIL);
                }
            }
            raw.complete(trace.status());
        }

        auditPublishedReport(output, trace);
        return output;
    }

    private static void publishRunReceipt(AtfReportPorts.Scenario scenario, AndroidUiFlowTrace trace,
            String installedPackage, String device, String androidApi, String summary, List<Path> files)
            throws Exception {
        int checkpointCount = checkpointCount(trace);
        String fingerprints = files.stream()
                .filter(file -> !file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                .map(AndroidUiFlowReportPublisher::fingerprintLine)
                .collect(java.util.stream.Collectors.joining("\n"));
        try (var receipt = scenario.startPhase("Device, run receipt, and supporting Android evidence")) {
            receipt.observation(new ReportObservation("Android UI execution receipt",
                    List.of(new ReportField("Flow ID", trace.id()),
                            new ReportField("Device", valueOrUnknown(device)),
                            new ReportField("Android API", valueOrUnknown(androidApi)),
                            new ReportField("Application package", trace.appPackage()),
                            new ReportField("Installed APK", valueOrUnknown(installedPackage).trim()),
                            new ReportField("Checkpoint count", Integer.toString(checkpointCount)),
                            new ReportField("Inline checkpoint screenshots", Integer.toString(screenshotCount(trace))),
                            new ReportField("Screenshot policy", "One full-display PNG at every captured checkpoint"),
                            new ReportField("Observed journey", valueOrUnknown(summary)),
                            new ReportField("Execution failure", trace.failure().isBlank() ? "none" : trace.failure())),
                    fingerprints.isBlank() ? List.of() : List.of(new ReportObservation.Body(
                            "Supporting Android hierarchy/manifest SHA-256 fingerprints", fingerprints,
                            ReportObservation.Format.TEXT)), List.of()));
            receipt.complete(trace.status());
        }
    }

    private static AtfReportStatus phaseStatus(List<AndroidUiFlowTrace.CheckpointSnapshot> checkpoints) {
        return checkpoints.stream().map(AndroidUiFlowTrace.CheckpointSnapshot::status)
                .reduce(AtfReportStatus.INFO, AtfReportStatus::worst);
    }

    private static void auditPublishedReport(Path report, AndroidUiFlowTrace trace) throws Exception {
        require(Files.isRegularFile(report) && Files.size(report) > 2_000,
                "Android UI Extent report was not published: " + report);
        String html = Files.readString(report);
        require(html.contains("Device, run receipt, and supporting Android evidence"),
                "Extent omitted the Android execution receipt");
        require(containsText(html, trace.id())
                        && hasReceiptField(html, "CHECKPOINT COUNT", Integer.toString(checkpointCount(trace)))
                        && hasReceiptField(html, "INLINE CHECKPOINT SCREENSHOTS", Integer.toString(screenshotCount(trace))),
                "Extent omitted the run identity or checkpoint/screenshot audit counts");
        require(!html.toLowerCase(Locale.ROOT).contains("href=\"file:"),
                "Android UI report must not publish workstation-local file links");
        List<AndroidUiFlowTrace.CheckpointSnapshot> checkpoints = trace.phaseSnapshots().stream()
                .flatMap(phase -> phase.checkpoints().stream()).toList();
        long actionTableCount = checkpoints.stream().filter(checkpoint -> checkpoint.actions().stream()
                .anyMatch(action -> action.source().name().equals("ACTION"))).count();
        long observationTableCount = checkpoints.stream().filter(checkpoint -> checkpoint.actions().stream()
                .anyMatch(action -> action.source().name().equals("OBSERVE"))).count();
        long assertionTableCount = checkpoints.stream().filter(checkpoint -> !checkpoint.assertions().isEmpty()).count();
        if (!trace.failure().isBlank()) {
            if (trace.pendingActions().stream().anyMatch(action -> action.source().name().equals("ACTION"))) {
                actionTableCount++;
            }
            observationTableCount++;
            assertionTableCount++;
        }
        require(countOccurrences(html, "class=\"atf-ui-action-sequence\"") == actionTableCount,
                "Extent action-table count does not match the checkpoint owners");
        require(countOccurrences(html, "class=\"atf-ui-observations\"") == observationTableCount,
                "Extent observation-table count does not match the checkpoint owners");
        require(countOccurrences(html, "class=\"atf-ui-assertions\"") == assertionTableCount,
                "Extent assertion-table count does not match the checkpoint owners");
        require(!html.contains("<details class=\"atf-ui-action-sequence\" open")
                        && !html.contains("<details class=\"atf-ui-observations\" open")
                        && !html.contains("<details class=\"atf-ui-assertions\" open"),
                "UI evidence disclosures must be collapsed by default");

        java.util.Map<String, Integer> screenshotOccurrences = new java.util.HashMap<>();
        for (AndroidUiFlowTrace.PhaseSnapshot phase : trace.phaseSnapshots()) {
            for (AndroidUiFlowTrace.CheckpointSnapshot checkpoint : phase.checkpoints()) {
                require(containsText(html, checkpoint.title()), "Extent omitted checkpoint " + checkpoint.title());
                for (var action : checkpoint.actions()) {
                    boolean rowIsVisible = containsText(html, action.operation()) && containsText(html, action.target())
                            && containsText(html, action.expected()) && containsText(html, action.observed());
                    boolean measuredDispatchIsVisible = action.source().name().equals("OBSERVE")
                            || (containsText(html, action.durationText())
                                    && (containsText(html, action.status().displayValue())
                                            || containsText(html, action.status().displayLabel())));
                    require(rowIsVisible && measuredDispatchIsVisible,
                            "Extent omitted action/observation details from " + checkpoint.title());
                }
                for (var assertion : checkpoint.assertions()) {
                    require(containsText(html, assertion.name()),
                            "Extent omitted assertion " + assertion.name() + " from " + checkpoint.title());
                    require(containsText(html, assertion.expected()) && containsText(html, assertion.actual())
                                    && (containsText(html, assertion.resultStatus().displayValue())
                                            || containsText(html, assertion.resultStatus().displayLabel())),
                            "Extent omitted the evaluated expected/actual/result for " + assertion.name());
                }
                if (checkpoint.screenshot() != null) {
                    String payload = AndroidScreenshotEvidence.from(checkpoint.screenshot()).base64Png();
                    screenshotOccurrences.merge(payload, 1, Integer::sum);
                }
            }
        }
        for (var screenshot : screenshotOccurrences.entrySet()) {
            require(countOccurrences(html, "data:image/png;base64," + screenshot.getKey()) == screenshot.getValue(),
                    "Extent omitted or duplicated a checkpoint screenshot payload");
        }
        if (trace.status() == AtfReportStatus.PASS) {
            require(trace.phaseSnapshots().stream().mapToInt(phase -> phase.checkpoints().size()).sum() > 0,
                    "A PASS Android UI flow must contain at least one captured checkpoint");
        }
        if (!trace.failure().isBlank()) {
            require(html.contains("EXECUTION DIAGNOSTICS") && containsText(html, trace.failure()),
                    "Extent omitted the captured Android failure diagnostic");
            for (var action : trace.pendingActions()) {
                require(containsText(html, action.operation()) && containsText(html, action.target())
                                && containsText(html, action.expected()) && containsText(html, action.observed())
                                && containsText(html, Long.toString(action.durationMillis()))
                                && (containsText(html, action.status().displayValue())
                                        || containsText(html, action.status().displayLabel())),
                        "Extent omitted the uncheckpointed failure action/observation " + action.operation()
                                + " targeting " + action.target());
            }
        }
    }

    private static String fingerprintLine(Path file) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
            return file.getFileName() + "  sha256=" + HexFormat.of().formatHex(digest);
        } catch (Exception failure) {
            throw new IllegalStateException("Could not fingerprint Android UI evidence " + file, failure);
        }
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "not reported" : value;
    }

    private static int checkpointCount(AndroidUiFlowTrace trace) {
        return trace.phaseSnapshots().stream().mapToInt(phase -> phase.checkpoints().size()).sum();
    }

    private static int screenshotCount(AndroidUiFlowTrace trace) {
        return trace.phaseSnapshots().stream().flatMap(phase -> phase.checkpoints().stream())
                .mapToInt(checkpoint -> checkpoint.screenshot() == null ? 0 : 1).sum();
    }

    private static boolean hasReceiptField(String html, String label, String value) {
        return html.contains("<td>" + label.toUpperCase(Locale.ROOT) + "</td><td>" + value + "</td>");
    }

    private static int countOccurrences(String text, String token) {
        int count = 0;
        for (int index = 0; (index = text.indexOf(token, index)) >= 0; index += token.length()) count++;
        return count;
    }

    private static boolean containsText(String html, String value) {
        return html.contains(value) || html.contains(value.replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;"));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
