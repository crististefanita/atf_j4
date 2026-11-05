package atf.android.e2e.support;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.android.evidence.timing.AndroidPhaseClock;
import atf.android.evidence.timing.AndroidPhaseTiming;
import atf.android.evidence.timing.AndroidPhaseTimingCodec;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Objects;

/** Persists ordered, renderer-neutral Android UI checkpoints for the final J4 Extent report. */
public final class AndroidUiFlowTrace {
    private final Path outputDirectory;
    private final Path manifest;
    private final String id;
    private final String name;
    private final String testClass;
    private final String testPackage;
    private final String appPackage;
    private final List<String> tags;
    private final AndroidPhaseClock clock;
    private final Map<String, List<Checkpoint>> phases = new LinkedHashMap<>();
    private final List<AtfUiActionRecord> pendingActions = new ArrayList<>();
    private final List<AtfUiActionRecord> failedActions = new ArrayList<>();
    private String activePhase = "";
    private String failurePhase = "";
    private int actionSequence;
    private AtfReportStatus status = AtfReportStatus.WARNING;
    private String failure = "";

    public AndroidUiFlowTrace(Path outputDirectory, String id, String name,
            String testClass, String testPackage, String appPackage, List<String> tags) throws IOException {
        this(outputDirectory, id, name, testClass, testPackage, appPackage, tags, System::nanoTime);
    }

    public AndroidUiFlowTrace(Path outputDirectory, String id, String name,
            String testClass, String testPackage, String appPackage, List<String> tags,
            java.util.function.LongSupplier ticks) throws IOException {
        this.outputDirectory = Objects.requireNonNull(outputDirectory).toAbsolutePath().normalize();
        Files.createDirectories(this.outputDirectory);
        this.id = required(id, "id");
        this.clock = new AndroidPhaseClock(this.id, ticks);
        this.name = required(name, "name");
        this.testClass = required(testClass, "testClass");
        this.testPackage = required(testPackage, "testPackage");
        this.appPackage = required(appPackage, "appPackage");
        this.tags = List.copyOf(tags);
        this.manifest = this.outputDirectory.resolve("ui-flow-" + id + ".properties");
        flush();
    }

    /** Must precede the first action of a phase; checkpoints never reconstruct its start. */
    public synchronized void beginPhase(String name) throws IOException {
        String phase = required(name, "phase");
        phases.computeIfAbsent(phase, ignored -> new ArrayList<>());
        clock.enter(phase, new ArrayList<>(phases.keySet()).indexOf(phase));
        activePhase = phase;
        flush();
    }

    public synchronized AndroidPhaseClock.Scope capture() { return clock.capture(); }
    public synchronized boolean hasOpenPhase() { return phases.keySet().stream().anyMatch(clock::running); }

    public synchronized int nextActionSequence() {
        return ++actionSequence;
    }

    /** Retains one measured interaction until the next ordered UI checkpoint. */
    public synchronized void recordAction(AtfUiActionRecord action) {
        Objects.requireNonNull(action, "action");
        pendingActions.add(new AtfUiActionRecord(++actionSequence, action.operation(), action.target(),
                action.expected(), action.observed(), action.durationMillis(), action.status(), action.source()));
    }

    public synchronized void checkpoint(String phase, String keyword, String title, String target,
            Path screenshot, List<AtfUiActionRecord> actions, List<AtfAssertion> assertions,
            AtfReportStatus checkpointStatus, List<Path> supportingArtifacts) throws IOException {
        String phaseName = required(phase, "phase");
        if (phases.keySet().stream().anyMatch(clock::running) && !clock.running(phaseName)) {
            throw new IllegalStateException("Checkpoint does not belong to the active Android phase: " + phaseName);
        }
        List<Checkpoint> steps = phases.computeIfAbsent(phaseName, ignored -> new ArrayList<>());
        String screenshotName = artifactName(screenshot);
        List<String> artifactNames = supportingArtifacts == null ? List.of()
                : supportingArtifacts.stream().map(this::artifactName).filter(value -> !value.isBlank()).toList();
        List<AtfUiActionRecord> checkpointActions = new ArrayList<>(pendingActions);
        pendingActions.clear();
        if (actions != null) checkpointActions.addAll(actions);
        steps.add(new Checkpoint(required(keyword, "keyword"), required(title, "title"),
                required(target, "target"), screenshotName, List.copyOf(checkpointActions),
                List.copyOf(assertions), Objects.requireNonNull(checkpointStatus), artifactNames));
        flush();
    }

    public synchronized void finish(AtfReportStatus finalStatus) throws IOException {
        Objects.requireNonNull(finalStatus);
        clock.stop(false);
        boolean recordedFailure = status == AtfReportStatus.FAIL || !failure.isBlank()
                || pendingActions.stream().anyMatch(action -> action.status() == AtfReportStatus.FAIL)
                || phases.values().stream().flatMap(List::stream).anyMatch(checkpoint ->
                        checkpoint.status() == AtfReportStatus.FAIL
                                || checkpoint.actions().stream().anyMatch(action -> action.status() == AtfReportStatus.FAIL)
                                || checkpoint.assertions().stream().anyMatch(assertion -> assertion.resultStatus() == AtfReportStatus.FAIL));
        status = recordedFailure ? AtfReportStatus.FAIL : finalStatus;
        flush();
    }

    public synchronized void fail(Throwable cause) throws IOException {
        clock.stop(true);
        if (failurePhase.isBlank()) failurePhase = activePhase;
        failedActions.addAll(pendingActions);
        pendingActions.clear();
        status = AtfReportStatus.FAIL;
        String detail = cause == null ? "Unknown test failure" : cause.getClass().getSimpleName() + ": "
                + String.valueOf(cause.getMessage());
        failure = failure.isBlank() ? detail : failure + " | additional failure: " + detail;
        flush();
    }

    public Path manifest() { return manifest; }

    public String id() { return id; }
    public String name() { return name; }
    public synchronized String activePhaseName() { return required(activePhase,"active phase"); }
    public String testClass() { return testClass; }
    public String testPackage() { return testPackage; }
    public String appPackage() { return appPackage; }
    public List<String> tags() { return tags; }
    public synchronized AtfReportStatus status() { return status; }
    public synchronized String failure() { return failure; }
    public synchronized String failurePhase() { return failurePhase; }
    public synchronized List<AtfUiActionRecord> pendingActions() {
        var retained = new ArrayList<>(failedActions);
        retained.addAll(pendingActions);
        return List.copyOf(retained);
    }

    /** Immutable in-memory projection used to publish each checkpoint through the public UI report API. */
    public synchronized List<PhaseSnapshot> phaseSnapshots() {
        return phases.entrySet().stream()
                .map(entry -> new PhaseSnapshot(entry.getKey(), entry.getValue().stream()
                        .map(checkpoint -> new CheckpointSnapshot(checkpoint.keyword(), checkpoint.title(),
                                checkpoint.target(), resolveArtifact(checkpoint.screenshot()),
                                checkpoint.actions(), checkpoint.assertions(), checkpoint.status(),
                                checkpoint.artifacts().stream().map(this::resolveArtifact).toList()))
                        .toList(), clock.snapshot(entry.getKey())))
                .toList();
    }

    /** Returns the recorded comparisons in execution order with their checkpoint context. */
    public synchronized List<AtfAssertion> assertions() {
        return phases.entrySet().stream()
                .flatMap(phase -> phase.getValue().stream()
                        .flatMap(checkpoint -> checkpoint.assertions().stream()
                                .map(assertion -> new AtfAssertion(
                                        phase.getKey() + " · " + checkpoint.keyword() + " · "
                                                + checkpoint.title() + " · " + assertion.name(),
                                        assertion.expected(), assertion.actual(), assertion.passed(),
                                        assertion.resultStatus()))))
                .toList();
    }

    private String artifactName(Path file) {
        if (file == null) return "";
        Path normalized = file.toAbsolutePath().normalize();
        if (!normalized.startsWith(outputDirectory)) {
            throw new IllegalArgumentException("Android UI flow evidence must stay under " + outputDirectory + ": " + file);
        }
        return outputDirectory.relativize(normalized).toString().replace('\\', '/');
    }

    private Path resolveArtifact(String relative) {
        return relative == null || relative.isBlank() ? null
                : outputDirectory.resolve(relative).normalize();
    }

    private void flush() throws IOException {
        clock.journal(this::writeManifest);
    }

    private void writeManifest() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("format.version", "1");
        properties.setProperty("flow.id", id);
        properties.setProperty("flow.name", name);
        properties.setProperty("flow.testClass", testClass);
        properties.setProperty("flow.testPackage", testPackage);
        properties.setProperty("flow.appPackage", appPackage);
        properties.setProperty("flow.tags", String.join(",", tags));
        properties.setProperty("flow.status", status.name());
        properties.setProperty("flow.failure", failure);
        properties.setProperty("flow.failurePhase", failurePhase);
        properties.setProperty("flow.phaseCount", Integer.toString(phases.size()));
        int phaseIndex = 0;
        for (var entry : phases.entrySet()) {
            int ordinal = phaseIndex++;
            String phasePrefix = "phase." + ordinal;
            properties.setProperty(phasePrefix + ".name", entry.getKey());
            clock.snapshot(entry.getKey()).ifPresent(timing -> AndroidPhaseTimingCodec.write(properties, phasePrefix + ".timing", timing));
            if (clock.running(entry.getKey())) AndroidPhaseTimingCodec.running(properties, phasePrefix + ".timing", id, entry.getKey(), ordinal);
            properties.setProperty(phasePrefix + ".stepCount", Integer.toString(entry.getValue().size()));
            for (int stepIndex = 0; stepIndex < entry.getValue().size(); stepIndex++) {
                Checkpoint checkpoint = entry.getValue().get(stepIndex);
                String stepPrefix = phasePrefix + ".step." + stepIndex;
                properties.setProperty(stepPrefix + ".keyword", checkpoint.keyword());
                properties.setProperty(stepPrefix + ".title", checkpoint.title());
                properties.setProperty(stepPrefix + ".target", checkpoint.target());
                properties.setProperty(stepPrefix + ".screenshot", checkpoint.screenshot());
                properties.setProperty(stepPrefix + ".status", checkpoint.status().name());
                properties.setProperty(stepPrefix + ".actionCount", Integer.toString(checkpoint.actions().size()));
                for (int actionIndex = 0; actionIndex < checkpoint.actions().size(); actionIndex++) {
                    AtfUiActionRecord action = checkpoint.actions().get(actionIndex);
                    String prefix = stepPrefix + ".action." + actionIndex;
                    properties.setProperty(prefix + ".sequence", Integer.toString(action.sequence()));
                    properties.setProperty(prefix + ".operation", action.operation());
                    properties.setProperty(prefix + ".target", action.target());
                    properties.setProperty(prefix + ".expected", action.expected());
                    properties.setProperty(prefix + ".observed", action.observed());
                    properties.setProperty(prefix + ".durationMillis", Long.toString(action.durationMillis()));
                    properties.setProperty(prefix + ".status", action.status().name());
                    properties.setProperty(prefix + ".source", action.source().name());
                }
                properties.setProperty(stepPrefix + ".assertionCount", Integer.toString(checkpoint.assertions().size()));
                for (int assertionIndex = 0; assertionIndex < checkpoint.assertions().size(); assertionIndex++) {
                    AtfAssertion assertion = checkpoint.assertions().get(assertionIndex);
                    String prefix = stepPrefix + ".assertion." + assertionIndex;
                    properties.setProperty(prefix + ".name", assertion.name());
                    properties.setProperty(prefix + ".expected", assertion.expected());
                    properties.setProperty(prefix + ".actual", assertion.actual());
                    properties.setProperty(prefix + ".passed", Boolean.toString(assertion.passed()));
                    properties.setProperty(prefix + ".status", assertion.resultStatus().name());
                }
                properties.setProperty(stepPrefix + ".artifactCount", Integer.toString(checkpoint.artifacts().size()));
                for (int artifactIndex = 0; artifactIndex < checkpoint.artifacts().size(); artifactIndex++) {
                    properties.setProperty(stepPrefix + ".artifact." + artifactIndex,
                            checkpoint.artifacts().get(artifactIndex));
                }
            }
        }
        List<AtfUiActionRecord> retainedPending = pendingActions();
        properties.setProperty("flow.pendingActionCount", Integer.toString(retainedPending.size()));
        for (int index = 0; index < retainedPending.size(); index++) {
            AtfUiActionRecord action = retainedPending.get(index);
            String prefix = "flow.pendingAction." + index;
            properties.setProperty(prefix + ".sequence", Integer.toString(action.sequence()));
            properties.setProperty(prefix + ".operation", action.operation());
            properties.setProperty(prefix + ".target", action.target());
            properties.setProperty(prefix + ".expected", action.expected());
            properties.setProperty(prefix + ".observed", action.observed());
            properties.setProperty(prefix + ".durationMillis", Long.toString(action.durationMillis()));
            properties.setProperty(prefix + ".status", action.status().name());
            properties.setProperty(prefix + ".source", action.source().name());
        }
        Path temporary = manifest.resolveSibling(manifest.getFileName() + ".tmp");
        try (OutputStream output = Files.newOutputStream(temporary)) {
            properties.store(output, "ATF J4 ordered Android UI flow evidence");
        }
        try {
            Files.move(temporary, manifest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, manifest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }

    private record Checkpoint(String keyword, String title, String target, String screenshot,
            List<AtfUiActionRecord> actions, List<AtfAssertion> assertions,
            AtfReportStatus status, List<String> artifacts) { }

    public record PhaseSnapshot(String name, List<CheckpointSnapshot> checkpoints, java.util.Optional<AndroidPhaseTiming> timing) {
        public PhaseSnapshot(String name, List<CheckpointSnapshot> checkpoints) { this(name, checkpoints, java.util.Optional.empty()); }
        public PhaseSnapshot { checkpoints = List.copyOf(checkpoints); }
    }

    public record CheckpointSnapshot(String keyword, String title, String target, Path screenshot,
            List<AtfUiActionRecord> actions, List<AtfAssertion> assertions,
            AtfReportStatus status, List<Path> supportingArtifacts) {
        public CheckpointSnapshot {
            actions = List.copyOf(actions);
            assertions = List.copyOf(assertions);
            supportingArtifacts = List.copyOf(supportingArtifacts);
        }
    }
}
