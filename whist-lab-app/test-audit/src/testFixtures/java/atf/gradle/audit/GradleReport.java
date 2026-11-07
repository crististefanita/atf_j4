package atf.gradle.audit;

import atf.reporting.audit.AssertionAuditReport;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import atf.reporting.model.timing.ReportPhaseTiming;
import atf.reporting.security.SensitiveDataRedactor;
import atf.testkit.assertions.ComponentAssertionAudit;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.runner.Description;

/** One canonical Reporting session per Gradle task/worker; no independent HTML implementation. */
public final class GradleReport {
    private static final Map<Path, State> RUNS = new java.util.LinkedHashMap<>();
    static { Runtime.getRuntime().addShutdownHook(new Thread(() -> { try { close(); } finally { GradlePublication.shutdown(); } }, "whist-gradle-report-close")); }
    private record State(ExtentReportSession session, java.util.Set<String> identities,
                         java.util.List<atf.reporting.artifact.ReportArtifactProfile.Scenario> intent) { }
    private GradleReport() { }
    private static Path path() {
        String configured = System.getProperty("atf.gradle.audit.report", "");
        if (configured.isBlank() || !Path.of(configured).isAbsolute()) throw new IllegalStateException("An absolute atf.gradle.audit.report is required");
        return Path.of(configured).normalize();
    }
    private static State state() {
        return RUNS.computeIfAbsent(path(), file -> new State(new ExtentReportSession(file, new ReportingSettings(4096, 16384),
                new SensitiveDataRedactor(), AtfConsoleSink.noOp(), Map.of(
                "Scope", System.getProperty("atf.gradle.audit.scope", "controlled adapter qualification"),
                "Runtime", "JVM component/unit; no Android device",
                "Lifecycle", "JUnit 4 invocation statement includes before/after fixtures; publication timing is separate",
                "Source", System.getProperty("atf.gradle.audit.source", "not supplied"))), new java.util.LinkedHashSet<>(), new java.util.ArrayList<>()));
    }
    static void capture(Description description, GradleInputs input, ComponentAssertionAudit.Result result, Throwable failure) {
        GradlePublication.run(() -> publish(description, input, result, failure));
    }
    private static void publish(Description description, GradleInputs input, ComponentAssertionAudit.Result result, Throwable failure) {
        var run = state(); String id = description.getClassName() + "#" + description.getMethodName();
        if (!run.identities.add(id)) throw new IllegalStateException("Duplicate JUnit invocation: " + id);
        String layer = layer(description.getClassName());
        var status = status(result.outcome());
        var logicalIntent = GradleIntent.describe(description, input, result);
        final String serializedIntent;
        try { serializedIntent = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(logicalIntent); }
        catch (com.fasterxml.jackson.core.JsonProcessingException invalid) { throw new IllegalStateException(invalid); }
        String owner = description.getTestClass().getName().substring(description.getTestClass().getPackageName().length() + 1);
        try (var scenario = run.session.startScenario(owner, description.getTestClass().getPackageName(),
                description.getMethodName(), List.of(layer, "gradle", "offline"))) {
            for (var observed : result.phases()) {
                try (var phase = scenario.startPhase(observed.phase().name().replace('_', ' '))) {
                    observed.timing().ifPresent(time -> phase.executionTiming(new ReportPhaseTiming(time.elapsed(), time.intervals())));
                    if (observed.phase() == ComponentAssertionAudit.Phase.TEST_BODY) {
                        var receipt = new java.util.LinkedHashMap<String, Object>();
                        receipt.put("schemaVersion", 1); receipt.put("invocationId", id); receipt.put("layer", layer);
                        receipt.put("junitOutcome", failure == null ? "PASS" : failure instanceof org.junit.AssumptionViolatedException ? "SKIP"
                                : failure instanceof AssertionError ? "FAIL" : "ERROR");
                        receipt.put("comparisons", result.phases().stream().mapToInt(p -> p.checks().size()).sum());
                        receipt.put("fixtureCaptured", input.hasFixture());
                        receipt.put("images", atf.gradle.audit.verification.CapturedPngVerifier.inventory(input.images()));
                        try { phase.observation(body("JUnit invocation receipt", new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(receipt))); }
                        catch (com.fasterxml.jackson.core.JsonProcessingException invalid) { throw new IllegalStateException(invalid); }
                        phase.observation(body(GradleIntent.TITLE, serializedIntent));
                        for (var entry : input.entries()) phase.observation(body("Captured context: " + entry.name(), entry.value()));
                    }
                    if (!observed.checks().isEmpty()) AssertionAuditReport.publishStep(phase, "Component assertions", observed.checks().stream()
                            .map(check -> new AtfAssertion(check.name(), check.expected(), check.actual(), check.passed())).toList(), AtfReportStatus.INFO);
                    if (!observed.diagnostic().isBlank()) phase.observation(body("Original execution diagnostic", observed.diagnostic()));
                    phase.complete(status(observed.outcome()));
                }
            }
            if (!input.images().isEmpty()) {
                var visual = atf.reporting.ui.UiExtentReportSession.adaptScenario(scenario,
                        atf.reporting.evidence.profile.ReportEvidenceProfile.forScreenshotPolicy(atf.reporting.evidence.ReportScreenshotPolicy.EACH_CHECKPOINT), description.getDisplayName());
                try (var phase = visual.startPhase("COMPONENT RASTERS — SYNTHETIC JVM FIXTURES")) {
                    for (var image : input.images()) phase.uiValidationAsMedia(image.title(),
                            "Declared JVM renderer fixture; no Android activity or dispatched action", image.base64Png(),
                            List.of(), List.of(), AtfReportStatus.INFO);
                    phase.complete(AtfReportStatus.INFO);
                }
            }
            scenario.complete(status);
        }
        run.intent.add(GradleIntent.withMetadata(logicalIntent, serializedIntent));
    }
    private static String layer(String owner) {
        String configured = System.getProperty("atf.gradle.audit.catalog", "");
        if (configured.isBlank()) return "component"; // Controlled bridge tests supply their own classes.
        var properties = new java.util.Properties();
        try (var reader = java.nio.file.Files.newBufferedReader(Path.of(configured))) { properties.load(reader); }
        catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
        String value = properties.getProperty(owner);
        if (value == null || !List.of("unit", "component", "governance", "learning").contains(value))
            throw new IllegalStateException("Unclassified Gradle test: " + owner + " (add it to the audit collection catalog)");
        return value;
    }
    private static AtfReportStatus status(ComponentAssertionAudit.Outcome value) {
        return value == ComponentAssertionAudit.Outcome.PASS ? AtfReportStatus.PASS
                : value == ComponentAssertionAudit.Outcome.ABORTED ? AtfReportStatus.SKIP : AtfReportStatus.FAIL;
    }
    private static ReportObservation body(String title, String text) {
        var parts = new java.util.ArrayList<ReportObservation.Body>(); int at = 0;
        do {
            int end = Math.min(text.length(), at + 16384);
            if (end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))) end--;
            parts.add(new ReportObservation.Body("Captured value / part " + (parts.size() + 1), text.substring(at, end), ReportObservation.Format.TEXT)); at = end;
        } while (at < text.length());
        return new ReportObservation(title, List.of(), parts, List.of());
    }
    public static void flush() { GradlePublication.run(() -> { for (var run : RUNS.values()) run.session.flush(); }); }
    public static void closeCurrent() { Path selected = path(); GradlePublication.run(() -> { var run = RUNS.remove(selected); if (run != null) finish(selected, run); }); }
    public static void close() { GradlePublication.run(() -> { try { for (var entry : RUNS.entrySet()) finish(entry.getKey(), entry.getValue()); } finally { RUNS.clear(); } }); }
    private static void finish(Path file, State run) {
        run.session.close();
        if (!run.intent.isEmpty()) {
            var profile = new atf.reporting.artifact.ReportArtifactProfile(atf.reporting.artifact.ReportArtifactProfile.Renderer.EXTENT, run.intent);
            var inspection = atf.reporting.artifact.ReportArtifactInspector.inspect(file, profile);
            if (!inspection.violations().isEmpty()) throw new IllegalStateException("Captured publication intent differs: " + inspection.violations());
        }
    }
}
