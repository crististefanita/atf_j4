package atf.android.e2e.support;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
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
    private final Map<String, List<Checkpoint>> phases = new LinkedHashMap<>();
    private int actionSequence;
    private AtfReportStatus status = AtfReportStatus.WARNING;
    private String failure = "";

    public AndroidUiFlowTrace(Path outputDirectory, String id, String name,
            String testClass, String testPackage, String appPackage, List<String> tags) throws IOException {
        this.outputDirectory = Objects.requireNonNull(outputDirectory).toAbsolutePath().normalize();
        Files.createDirectories(this.outputDirectory);
        this.id = required(id, "id");
        this.name = required(name, "name");
        this.testClass = required(testClass, "testClass");
        this.testPackage = required(testPackage, "testPackage");
        this.appPackage = required(appPackage, "appPackage");
        this.tags = List.copyOf(tags);
        this.manifest = this.outputDirectory.resolve("ui-flow-" + id + ".properties");
        flush();
    }

    public synchronized int nextActionSequence() {
        return ++actionSequence;
    }

    public synchronized void checkpoint(String phase, String keyword, String title, String target,
            Path screenshot, List<AtfUiActionRecord> actions, List<AtfAssertion> assertions,
            AtfReportStatus checkpointStatus, List<Path> supportingArtifacts) throws IOException {
        String phaseName = required(phase, "phase");
        List<Checkpoint> steps = phases.computeIfAbsent(phaseName, ignored -> new ArrayList<>());
        String screenshotName = artifactName(screenshot);
        List<String> artifactNames = supportingArtifacts == null ? List.of()
                : supportingArtifacts.stream().map(this::artifactName).filter(value -> !value.isBlank()).toList();
        steps.add(new Checkpoint(required(keyword, "keyword"), required(title, "title"),
                required(target, "target"), screenshotName, List.copyOf(actions),
                List.copyOf(assertions), Objects.requireNonNull(checkpointStatus), artifactNames));
        flush();
    }

    public synchronized void finish(AtfReportStatus finalStatus) throws IOException {
        status = Objects.requireNonNull(finalStatus);
        flush();
    }

    public synchronized void fail(Throwable cause) throws IOException {
        status = AtfReportStatus.FAIL;
        String detail = cause == null ? "Unknown test failure" : cause.getClass().getSimpleName() + ": "
                + String.valueOf(cause.getMessage());
        failure = failure.isBlank() ? detail : failure + " | additional failure: " + detail;
        flush();
    }

    public Path manifest() { return manifest; }

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

    private void flush() throws IOException {
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
        properties.setProperty("flow.phaseCount", Integer.toString(phases.size()));
        int phaseIndex = 0;
        for (var entry : phases.entrySet()) {
            String phasePrefix = "phase." + phaseIndex++;
            properties.setProperty(phasePrefix + ".name", entry.getKey());
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
}
