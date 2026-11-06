package atf.android.e2e.support.replay;

import atf.android.e2e.support.AndroidUiFlowTrace;
import atf.android.e2e.support.flow.AndroidUiFlowEvidence;
import atf.android.evidence.timing.AndroidPhaseTimingCodec;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Reads captured verdicts and timing without executing ADB, assertions, clocks or trace writes. */
public record CapturedAndroidFlow(Path manifest, String id, String name, String testClass, String testPackage,
        String appPackage, List<String> tags, AtfReportStatus status, String failure,
        List<AtfUiActionRecord> pendingActions, List<AndroidUiFlowTrace.PhaseSnapshot> phaseSnapshots) implements AndroidUiFlowEvidence {
    public CapturedAndroidFlow {
        tags = List.copyOf(tags);pendingActions = List.copyOf(pendingActions);phaseSnapshots = List.copyOf(phaseSnapshots);
    }
    @Override public boolean hasOpenPhase() { return false; }
    public static CapturedAndroidFlow read(Path manifest) throws IOException {
        manifest = manifest.toAbsolutePath().normalize();var input = new Properties();
        try (var stream = Files.newInputStream(manifest)) { input.load(stream); }
        if (!required(input, "format.version").equals("1")) throw new IllegalArgumentException("Unsupported captured flow schema");
        String id = required(input, "flow.id");var phases = new ArrayList<AndroidUiFlowTrace.PhaseSnapshot>();
        for (int p = 0; p < count(input, "flow.phaseCount"); p++) {
            String prefix = "phase." + p, name = required(input, prefix + ".name");
            var checkpoints = new ArrayList<AndroidUiFlowTrace.CheckpointSnapshot>();
            for (int s = 0; s < count(input, prefix + ".stepCount"); s++) {
                String key = prefix + ".step." + s;var assertions = new ArrayList<AtfAssertion>();
                for (int a = 0; a < count(input, key + ".assertionCount"); a++) {
                    String at = key + ".assertion." + a, passed = required(input, at + ".passed");
                    if (!passed.equals("true") && !passed.equals("false")) throw new IllegalArgumentException("Invalid captured verdict: " + at);
                    assertions.add(new AtfAssertion(required(input, at + ".name"), value(input, at + ".expected"),
                            value(input, at + ".actual"), Boolean.parseBoolean(passed), AtfReportStatus.valueOf(required(input, at + ".status"))));
                }
                var artifacts = new ArrayList<Path>();
                for (int a = 0; a < count(input, key + ".artifactCount"); a++)
                    artifacts.add(artifact(manifest, required(input, key + ".artifact." + a)));
                checkpoints.add(new AndroidUiFlowTrace.CheckpointSnapshot(required(input, key + ".keyword"),
                        required(input, key + ".title"), required(input, key + ".target"), artifact(manifest, required(input, key + ".screenshot")),
                        actions(input, key + ".action", count(input, key + ".actionCount")), assertions,
                        AtfReportStatus.valueOf(required(input, key + ".status")), artifacts));
            }
            phases.add(new AndroidUiFlowTrace.PhaseSnapshot(name, checkpoints,
                    AndroidPhaseTimingCodec.read(input, prefix + ".timing", id, name, p)));
        }
        var tags = new ArrayList<>(List.of(required(input, "flow.tags").split(",")));tags.add("offline-republication");
        return new CapturedAndroidFlow(manifest, id, required(input, "flow.name"), required(input, "flow.testClass"),
                required(input, "flow.testPackage"), required(input, "flow.appPackage"), tags,
                AtfReportStatus.valueOf(required(input, "flow.status")), value(input, "flow.failure"),
                actions(input, "flow.pendingAction", count(input, "flow.pendingActionCount")), phases);
    }
    private static List<AtfUiActionRecord> actions(Properties input, String prefix, int count) {
        var values = new ArrayList<AtfUiActionRecord>();
        for (int index = 0; index < count; index++) {
            String key = prefix + "." + index;
            values.add(new AtfUiActionRecord(Math.toIntExact(number(input, key + ".sequence")), required(input, key + ".operation"),
                    value(input, key + ".target"), value(input, key + ".expected"), value(input, key + ".observed"),
                    number(input, key + ".durationMillis"), AtfReportStatus.valueOf(required(input, key + ".status")),
                    AtfUiEvidenceSource.valueOf(required(input, key + ".source"))));
        }
        return List.copyOf(values);
    }
    private static Path artifact(Path manifest, String name) throws IOException {
        Path root = manifest.getParent(), file = root.resolve(name).normalize();
        if (Path.of(name).isAbsolute() || !file.startsWith(root) || !Files.isRegularFile(file) || Files.isSymbolicLink(file)
                || !file.toRealPath().startsWith(root.toRealPath())) throw new IOException("Captured artifact escapes or is missing: " + name);
        return file;
    }
    private static int count(Properties input, String key) {
        long value = number(input, key);
        if (value > input.size()) throw new IllegalArgumentException("Invalid captured population: " + key);
        return Math.toIntExact(value);
    }
    private static long number(Properties input, String key) {
        String value = required(input, key);
        if (!value.matches("0|[1-9][0-9]*")) throw new IllegalArgumentException("Invalid captured number: " + key);
        return Long.parseLong(value);
    }
    private static String required(Properties input, String key) {
        String value = value(input, key);
        if (value.isBlank()) throw new IllegalArgumentException("Missing captured field: " + key);
        return value;
    }
    private static String value(Properties input, String key) {
        String value = input.getProperty(key);
        if (value == null) throw new IllegalArgumentException("Missing captured field: " + key);
        return value;
    }
}
