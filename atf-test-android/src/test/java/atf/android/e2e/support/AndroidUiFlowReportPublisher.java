package atf.android.e2e.support;

import atf.android.e2e.support.flow.AndroidUiFlowEvidence;

import static atf.android.e2e.support.AndroidUiFlowReportAudit.checkpointCount;
import static atf.android.e2e.support.AndroidUiFlowReportAudit.screenshotCount;
import static atf.android.e2e.support.AndroidUiFlowReportAudit.require;

import atf.android.evidence.AndroidScreenshotEvidence;
import atf.android.evidence.text.CapturedTextEvidence;
import atf.android.evidence.timing.AndroidPhaseReportTiming;
import atf.android.whist.evidence.WhistExtentFeedback;
import atf.reporting.configuration.AtfReportConfigurationLoader;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import atf.reporting.ui.UiExtentReportSession;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Publishes ordered Android checkpoints through the same public UI evidence API used by Playwright. */
public final class AndroidUiFlowReportPublisher {
    private AndroidUiFlowReportPublisher() { }

    public static Path publish(Path report, String installedPackage, String device, String androidApi,
            String summary, List<Path> files, AndroidUiFlowEvidence trace) throws Exception {
        return publish(report, installedPackage, device, androidApi, summary, files, trace, System::nanoTime);
    }

    static Path publish(Path report, String installedPackage, String device, String androidApi,
            String summary, List<Path> files, AndroidUiFlowEvidence trace, java.util.function.LongSupplier publicationTicks) throws Exception {
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(trace, "trace");
        if (trace.hasOpenPhase()) throw new IllegalStateException("Cannot publish an unfinished Android phase");
        var evidenceFiles = new java.util.LinkedHashSet<Path>();
        if (files != null) evidenceFiles.addAll(files);
        evidenceFiles.add(trace.manifest());
        trace.phaseSnapshots().stream().flatMap(phase -> phase.checkpoints().stream())
                .forEach(checkpoint -> evidenceFiles.addAll(checkpoint.supportingArtifacts()));
        Path output = report.toAbsolutePath().normalize();
        Files.createDirectories(output.getParent());
        ReportingSettings settings = AtfReportConfigurationLoader.load().reportingSettings();
        validateCheckpointContract(trace,
                settings.assertionMaxDisplayCharacters());
        List<ReportObservation.Body> supporting = supportingEvidence(List.copyOf(evidenceFiles),
                trace.manifest().toAbsolutePath().getParent(), settings.bodyMaxDisplayCharacters());

        try (var session = new ExtentReportSession(output, settings)) {
            AtfReportPorts.Scenario raw = session.startScenario(
                    trace.testClass(), trace.testPackage(), trace.name(), trace.tags());
            publishRunReceipt(raw, trace, installedPackage, device, androidApi, summary, supporting);

            var ui = UiExtentReportSession.adaptScenario(raw,
                    ReportEvidenceProfile.forScreenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT),
                    trace.name());
            int ordinal = 0;
            for (AndroidUiFlowTrace.PhaseSnapshot phaseSnapshot : trace.phaseSnapshots()) {
                try (var phase = ui.startPhase(phaseSnapshot.name())) {
                    long dispatch = phaseSnapshot.checkpoints().stream().flatMap(checkpoint -> checkpoint.actions().stream())
                            .filter(action -> action.source().name().equals("ACTION")).mapToLong(action -> action.durationMillis()).sum();
                    try (var timing = new AndroidPhaseReportTiming(phase, trace.id(), phaseSnapshot.name(), ordinal,
                            phaseSnapshot.timing(), dispatch, publicationTicks)) {
                    for (var checkpoint : phaseSnapshot.checkpoints()) {
                        publishCheckpoint(phase, checkpoint);
                    }
                    }
                    phase.complete(phaseSnapshot.timing().map(value -> value.partial()).orElse(false)
                            ? AtfReportStatus.FAIL : phaseStatus(phaseSnapshot.checkpoints()));
                }
                ordinal++;
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

        auditPublishedReport(output, trace, settings);
        try {
            WhistExtentFeedback.afterAudit(output, trace.appPackage(), trace.name(), trace.status().name());
        } catch (java.io.IOException | java.nio.file.InvalidPathException indexFailure) {
            System.getLogger(AndroidUiFlowReportPublisher.class.getName()).log(System.Logger.Level.WARNING,
                    "Android report was audited but its feedback link could not be updated: " + output, indexFailure);
        }
        return output;
    }

    private static void publishCheckpoint(UiExtentReportSession.UiPhase phase,
            AndroidUiFlowTrace.CheckpointSnapshot checkpoint) throws Exception {
        String screenshot = checkpoint.screenshot() == null ? ""
                : AndroidScreenshotEvidence.from(checkpoint.screenshot()).base64Png();
        phase.step(AtfGherkinKeyword.valueOf(checkpoint.keyword()),
                checkpoint.title(), checkpoint.target(), screenshot,
                checkpoint.assertions(), checkpoint.actions(),
                ReportScreenshotPolicy.CapturePoint.CHECKPOINT, checkpoint.status());
    }

    private static List<ReportObservation.Body> supportingEvidence(List<Path> files, Path origin, int bodyBudget)
            throws java.io.IOException {
        var bodies = new ArrayList<ReportObservation.Body>();
        var inventory = new StringBuilder();
        var retained = new java.util.HashSet<String>();
        var redactor = new atf.reporting.security.SensitiveDataRedactor();
        for (Path file : files) {
            if (file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")) continue;
            String name = origin.relativize(file.toAbsolutePath()).toString().replace('\\', '/');
            var evidence = CapturedTextEvidence.read(file, name);
            inventory.append(name).append("  sha256=").append(evidence.sha256()).append("  bytes=").append(evidence.bytes()).append('\n');
            if (retained.add(evidence.sha256())) {
                var parts = atf.android.evidence.text.EvidenceTextParts.split(redactor.redactField(name, evidence.content()), bodyBudget);
                for (int index = 0; index < parts.size(); index++) bodies.add(new ReportObservation.Body(
                        "Captured supporting evidence — " + name + " — " + evidence.sha256()
                                + " — part " + (index + 1) + "/" + parts.size(), parts.get(index), ReportObservation.Format.TEXT));
            }
        }
        if (!inventory.isEmpty()) {
            var parts = atf.android.evidence.text.EvidenceTextParts.split(inventory.toString(), bodyBudget);
            for (int index = parts.size() - 1; index >= 0; index--) bodies.add(0, new ReportObservation.Body(
                    "Supporting evidence inventory — original bytes read at publication — part " + (index + 1) + "/" + parts.size(),
                    parts.get(index), ReportObservation.Format.TEXT));
        }
        return List.copyOf(bodies);
    }

    private static void publishRunReceipt(AtfReportPorts.Scenario scenario, AndroidUiFlowEvidence trace,
            String installedPackage, String device, String androidApi, String summary,
            List<ReportObservation.Body> supporting) throws Exception {
        int checkpointCount = checkpointCount(trace);
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
                    supporting, List.of()));
            receipt.complete(trace.status());
        }
    }

    private static AtfReportStatus phaseStatus(List<AndroidUiFlowTrace.CheckpointSnapshot> checkpoints) {
        return checkpoints.stream().map(AndroidUiFlowTrace.CheckpointSnapshot::status)
                .reduce(AtfReportStatus.INFO, AtfReportStatus::worst);
    }

    static void auditPublishedReport(Path report, AndroidUiFlowEvidence trace,
            ReportingSettings settings) throws Exception {
        AndroidUiFlowReportAudit.auditPublishedReport(report, trace, settings);
    }

    private static void validateCheckpointContract(AndroidUiFlowEvidence trace, int assertionPreviewLimit) {
        List<AndroidUiFlowTrace.CheckpointSnapshot> checkpoints = trace.phaseSnapshots().stream()
                .flatMap(phase -> phase.checkpoints().stream()).toList();
        for (AndroidUiFlowTrace.CheckpointSnapshot checkpoint : checkpoints) {
            require(checkpoint.screenshot() != null && Files.isRegularFile(checkpoint.screenshot()),
                    "Android checkpoint has no captured PNG: " + checkpoint.title());
            require(checkpoint.actions().stream().anyMatch(action -> action.source().name().equals("OBSERVE")),
                    "Android checkpoint has no recorded UI observation: " + checkpoint.title());
            require(!checkpoint.assertions().isEmpty(),
                    "Android checkpoint has no evaluated assertions: " + checkpoint.title());
            for (var assertion : checkpoint.assertions()) {
                require(assertion.expected().length() <= assertionPreviewLimit,
                        "Assertion expected value exceeds Extent's configured " + assertionPreviewLimit
                                + " character preview: " + assertion.name());
                require(assertion.actual().length() <= assertionPreviewLimit,
                        "Assertion actual value exceeds Extent's configured " + assertionPreviewLimit
                                + " character preview: " + assertion.name()
                                + "; put complete diagnostic values in the expandable observation table");
            }
        }
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "not reported" : value;
    }

}
