package atf.tests.reporting.replay;

import static atf.tests.reporting.replay.ReplayEvidenceFiles.loadTrace;

import static org.junit.jupiter.api.Assertions.assertTrue;

import atf.android.evidence.AndroidScreenshotEvidence;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Transformă un flux capturat în intrările tipizate ale raportării comune. */
public final class UiFlowReplay {
    private UiFlowReplay() {}

    public static void publishUiFlow(
            atf.reporting.contract.AtfReportPorts.Session session, Path manifest, Path evidenceDir)
            throws Exception {
        publishUiFlow(session, manifest, evidenceDir, false);
    }

    public static void publishUiFlow(
            atf.reporting.contract.AtfReportPorts.Session session,
            Path manifest,
            Path evidenceDir,
            boolean completeActionBodies)
            throws Exception {
        Properties trace = loadTrace(manifest);
        assertTrue(
                "1".equals(trace.getProperty("format.version")),
                "Unsupported Android UI flow manifest: " + manifest);
        String flowName = required(trace, "flow.name");
        String testClass = required(trace, "flow.testClass");
        String testPackage = required(trace, "flow.testPackage");
        String appPackage = required(trace, "flow.appPackage");
        AtfReportStatus flowStatus = AtfReportStatus.valueOf(required(trace, "flow.status"));
        List<String> tags = List.of(trace.getProperty("flow.tags", "android,e2e").split(","));
        var raw = session.startScenario(flowName, testPackage, appPackage, tags);
        var ui =
                UiExtentReportSession.adaptScenario(
                        raw,
                        ReportEvidenceProfile.forScreenshotPolicy(
                                ReportScreenshotPolicy.EACH_CHECKPOINT),
                        flowName);

        List<Path> retainedArtifacts = new ArrayList<>();
        int phaseCount = integer(trace, "flow.phaseCount");
        int checkpointCount = 0;
        for (int phase = 0; phase < phaseCount; phase++) {
            String phasePrefix = "phase." + phase;
            int stepCount = integer(trace, phasePrefix + ".stepCount");
            checkpointCount += stepCount;
            for (int step = 0; step < stepCount; step++) {
                String prefix = phasePrefix + ".step." + step;
                int artifactCount = integer(trace, prefix + ".artifactCount");
                for (int artifact = 0; artifact < artifactCount; artifact++) {
                    String name = trace.getProperty(prefix + ".artifact." + artifact, "");
                    if (!name.isBlank()) retainedArtifacts.add(resolveEvidence(evidenceDir, name));
                }
            }
        }
        try (var details = raw.startPhase("Android flow metadata and retained UI hierarchy")) {
            details.observation(
                    new ReportObservation(
                            "E2E flow identity and execution result",
                            List.of(
                                    new ReportField("Test class", testClass),
                                    new ReportField("App package", appPackage),
                                    new ReportField("Flow status", flowStatus.name()),
                                    new ReportField(
                                            "Ordered UI checkpoints",
                                            Integer.toString(checkpointCount)),
                                    new ReportField(
                                            "Failure", trace.getProperty("flow.failure", ""))),
                            List.of(),
                            retainedArtifacts.stream()
                                    .distinct()
                                    .map(ReplayEvidenceFiles::artifactUnchecked)
                                    .toList()));
            if (completeActionBodies) {
                for (int p = 0; p < phaseCount; p++)
                    for (int s = 0; s < integer(trace, "phase." + p + ".stepCount"); s++) {
                        String key = "phase." + p + ".step." + s;
                        List<ReportObservation.Body> payloads = new ArrayList<>();
                        for (int a = 0; a < integer(trace, key + ".actionCount"); a++) {
                            String action = key + ".action." + a;
                            String label =
                                    required(trace, action + ".source")
                                            + " #"
                                            + required(trace, action + ".sequence")
                                            + " "
                                            + required(trace, action + ".operation");
                            payloads.add(
                                    new ReportObservation.Body(
                                            label + " — target",
                                            required(trace, action + ".target"),
                                            ReportObservation.Format.TEXT));
                            payloads.add(
                                    new ReportObservation.Body(
                                            label + " — expected",
                                            trace.getProperty(action + ".expected", ""),
                                            ReportObservation.Format.TEXT));
                            payloads.add(
                                    new ReportObservation.Body(
                                            label + " — observed",
                                            trace.getProperty(action + ".observed", ""),
                                            ReportObservation.Format.TEXT));
                        }
                        for (int a = 0; a < integer(trace, key + ".assertionCount"); a++) {
                            String assertion = key + ".assertion." + a;
                            String expected = trace.getProperty(assertion + ".expected", ""),
                                    actual = trace.getProperty(assertion + ".actual", "");
                            if (expected.length() > 240 || actual.length() > 240) {
                                String label = "Assertion " + required(trace, assertion + ".name");
                                payloads.add(
                                        new ReportObservation.Body(
                                                label + " — full expected",
                                                expected,
                                                ReportObservation.Format.TEXT));
                                payloads.add(
                                        new ReportObservation.Body(
                                                label + " — full actual",
                                                actual,
                                                ReportObservation.Format.TEXT));
                            }
                        }
                        details.observation(
                                new ReportObservation(
                                        "Full state and action payload — "
                                                + required(trace, "phase." + p + ".name")
                                                + " / "
                                                + required(trace, key + ".title"),
                                        List.of(
                                                new ReportField("Checkpoint key", key),
                                                new ReportField(
                                                        "Presentation",
                                                        "The UI action table retains its short preview. These native TEXT bodies retain complete values with the same source and sequence.")),
                                        payloads,
                                        List.of()));
                    }
            }
            details.complete(flowStatus);
        }
        for (int phaseIndex = 0; phaseIndex < phaseCount; phaseIndex++) {
            String phasePrefix = "phase." + phaseIndex;
            String phaseName = required(trace, phasePrefix + ".name");
            int stepCount = integer(trace, phasePrefix + ".stepCount");
            var originalTiming =
                    atf.android.evidence.timing.AndroidPhaseTimingCodec.read(
                            trace,
                            phasePrefix + ".timing",
                            trace.getProperty("flow.id", ""),
                            phaseName,
                            phaseIndex);
            AtfReportStatus phaseStatus =
                    originalTiming.map(value -> value.partial()).orElse(false)
                            ? AtfReportStatus.FAIL
                            : AtfReportStatus.PASS;
            try (var phase = ui.startPhase(phaseName)) {
                try (var timing =
                        new atf.android.evidence.timing.AndroidPhaseReportTiming(
                                phase,
                                trace.getProperty("flow.id", ""),
                                phaseName,
                                phaseIndex,
                                originalTiming,
                                dispatchTotal(trace, phasePrefix, stepCount),
                                System::nanoTime)) {
                    for (int stepIndex = 0; stepIndex < stepCount; stepIndex++) {
                        String prefix = phasePrefix + ".step." + stepIndex;
                        String keyword = required(trace, prefix + ".keyword");
                        String title = required(trace, prefix + ".title");
                        String target = required(trace, prefix + ".target");
                        AtfReportStatus status =
                                AtfReportStatus.valueOf(required(trace, prefix + ".status"));
                        phaseStatus = AtfReportStatus.worst(phaseStatus, status);
                        List<AtfAssertion> assertions = new ArrayList<>();
                        for (int index = 0;
                                index < integer(trace, prefix + ".assertionCount");
                                index++) {
                            String assertion = prefix + ".assertion." + index;
                            assertions.add(
                                    new AtfAssertion(
                                            required(trace, assertion + ".name"),
                                            trace.getProperty(assertion + ".expected", ""),
                                            trace.getProperty(assertion + ".actual", ""),
                                            Boolean.parseBoolean(
                                                    trace.getProperty(
                                                            assertion + ".passed", "false")),
                                            AtfReportStatus.valueOf(
                                                    required(trace, assertion + ".status"))));
                        }
                        List<AtfUiActionRecord> actions = new ArrayList<>();
                        for (int index = 0;
                                index < integer(trace, prefix + ".actionCount");
                                index++) {
                            String action = prefix + ".action." + index;
                            actions.add(
                                    new AtfUiActionRecord(
                                            integer(trace, action + ".sequence"),
                                            required(trace, action + ".operation"),
                                            required(trace, action + ".target"),
                                            trace.getProperty(action + ".expected", ""),
                                            trace.getProperty(action + ".observed", ""),
                                            Long.parseLong(
                                                    required(trace, action + ".durationMillis")),
                                            AtfReportStatus.valueOf(
                                                    required(trace, action + ".status")),
                                            AtfUiEvidenceSource.valueOf(
                                                    required(trace, action + ".source"))));
                        }
                        String screenshotName = trace.getProperty(prefix + ".screenshot", "");
                        String base64Png =
                                screenshotName.isBlank()
                                        ? ""
                                        : AndroidScreenshotEvidence.from(
                                                        resolveEvidence(
                                                                evidenceDir, screenshotName))
                                                .base64Png();
                        if (assertions.isEmpty() && !base64Png.isBlank()) {
                            // A captured observation is media evidence, not an invented business
                            // assertion.
                            phase.uiValidationAsMedia(
                                    title, target, base64Png, assertions, actions, status);
                        } else
                            phase.step(
                                    AtfGherkinKeyword.valueOf(keyword),
                                    title,
                                    target,
                                    base64Png,
                                    assertions,
                                    actions,
                                    ReportScreenshotPolicy.CapturePoint.CHECKPOINT,
                                    status);
                    }
                }
                phase.complete(phaseStatus);
            }
        }
        AtfReportStatus publishedFlowStatus = flowStatus;
        raw.complete(publishedFlowStatus);
    }

    public static long dispatchTotal(Properties trace, String prefix, int steps) {
        long total = 0;
        for (int step = 0; step < steps; step++) {
            String checkpoint = prefix + ".step." + step;
            for (int index = 0; index < integer(trace, checkpoint + ".actionCount"); index++) {
                String action = checkpoint + ".action." + index;
                if (trace.getProperty(action + ".source", "ACTION").equals("ACTION")) {
                    total =
                            Math.addExact(
                                    total,
                                    Long.parseLong(required(trace, action + ".durationMillis")));
                }
            }
        }
        return total;
    }

    public static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Missing UI flow manifest field " + key);
        return value.trim();
    }

    public static int integer(Properties properties, String key) {
        return Integer.parseInt(required(properties, key));
    }

    public static Path resolveEvidence(Path root, String relativePath) {
        Path resolved = root.resolve(relativePath).toAbsolutePath().normalize();
        if (!resolved.startsWith(root.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException(
                    "UI flow evidence escapes the run evidence folder: " + relativePath);
        }
        assertTrue(Files.isRegularFile(resolved), "UI flow evidence file is missing: " + resolved);
        return resolved;
    }
}
