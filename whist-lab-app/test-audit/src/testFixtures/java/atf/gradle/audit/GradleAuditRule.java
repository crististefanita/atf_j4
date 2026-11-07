package atf.gradle.audit;

import atf.testkit.assertions.AssertionValueDisplay;
import atf.testkit.assertions.AuditedAssertionScope;
import atf.testkit.assertions.ComponentAssertionAudit;
import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/** Owns one statement, including JUnit 4 before/after fixtures; original failures remain primary. */
public final class GradleAuditRule implements TestRule {
    private final Object instance;
    public GradleAuditRule(Object instance) { this.instance = java.util.Objects.requireNonNull(instance); }
    @Override public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override public void evaluate() throws Throwable { run(base, description); }
        };
    }
    private void run(Statement base, Description description) throws Throwable {
        var input = GradleInputs.open(instance);
        var display = new AssertionValueDisplay(4096, (name, value) -> value, projection -> {
            if (projection.truncated()) input.add("Full comparison value / " + projection.role() + " / " + projection.name(), projection.fullText());
        });
        var audit = new ComponentAssertionAudit(display, System::nanoTime, ComponentAssertionAudit.AssertionRequirement.TEST_BODY);
        Throwable failure = null;
        try (var ignored = audit.measure(ComponentAssertionAudit.Phase.TEST_BODY);
             var scope = AuditedAssertionScope.open(GradleAuditRule.class.getName(), "run", audit.recorder(ComponentAssertionAudit.Phase.TEST_BODY))) {
            input.captureFields(instance);
            base.evaluate();
            if (!input.hasFixture()) throw new AssertionError("Component invocation has no captured fixture context: " + description);
        } catch (Throwable error) {
            failure = error;
            var diagnostic = new java.io.StringWriter(); error.printStackTrace(new java.io.PrintWriter(diagnostic));
            input.add("Full original execution failure", diagnostic.toString());
            audit.failed(ComponentAssertionAudit.Phase.TEST_BODY, error instanceof AssumptionViolatedException
                    ? new org.opentest4j.TestAbortedException(error.getMessage(), error) : error);
        } finally { GradleInputs.close(input); }
        var result = audit.finish(failure instanceof AssumptionViolatedException
                ? new org.opentest4j.TestAbortedException(failure.getMessage(), failure) : failure);
        if (failure == null && result.propagate()) failure = result.failure();
        try { GradleReport.capture(description, input, result, failure); }
        catch (Throwable publication) { if (failure == null) failure = publication; else failure.addSuppressed(publication); }
        if (failure != null) throw failure;
    }
}
