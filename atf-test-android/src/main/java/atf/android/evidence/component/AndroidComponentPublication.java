package atf.android.evidence.component;

import atf.reporting.audit.AssertionAuditReport;
import atf.testkit.assertions.AssertionValueDisplay;
import atf.testkit.assertions.ComponentAssertionAudit;
import atf.testkit.assertions.ComponentAuditExtension;

/** Projects already evaluated invocation data; no test execution, transport or synthetic API exchange. */
public final class AndroidComponentPublication implements ComponentAuditExtension.Publication {
    private final AndroidComponentRun owner;
    private final AssertionAuditReport.Identity identity;
    private final ComponentInputs inputs;
    private boolean published;

    public AndroidComponentPublication(AndroidComponentRun owner, AssertionAuditReport.Identity identity, ComponentInputs inputs) {
        this.owner = java.util.Objects.requireNonNull(owner);
        this.identity = java.util.Objects.requireNonNull(identity);
        this.inputs = java.util.Objects.requireNonNull(inputs);
    }
    @Override public AssertionValueDisplay display() { return owner.display(inputs); }
    @Override public ComponentAssertionAudit.AssertionRequirement assertionRequirement() {
        return ComponentAssertionAudit.AssertionRequirement.TEST_BODY;
    }
    @Override public void publish(ComponentAssertionAudit.Result result) {
        if (published) throw new IllegalStateException("Invocation audit cannot be published twice");
        owner.capture(identity, result, inputs);
        published = true;
    }
    @Override public void close() { }
}
