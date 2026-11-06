package atf.android.component;

import atf.android.evidence.component.AndroidComponentPublication;
import atf.android.evidence.component.AndroidComponentReports;
import atf.android.evidence.component.AndroidComponentRun;
import atf.android.evidence.component.ComponentInputs;
import atf.reporting.audit.AssertionAuditReport;
import atf.testkit.assertions.ComponentAuditAdapter;
import atf.testkit.assertions.ComponentAuditExtension;
import java.util.List;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;

/** JUnit metadata glue; shared owner acquisition and closure are delegated to Test Kit. */
public final class AndroidComponentAudit implements ComponentAuditAdapter {
    private static final ExtensionContext.Namespace INPUTS = ExtensionContext.Namespace.create(AndroidComponentAudit.class);
    private final ComponentAuditExtension audit = ComponentAuditExtension.scoped(AndroidComponentAudit.class, AndroidComponentRun.class,
                context -> AndroidComponentReports.open(), (run, context) -> {
                    var type = context.getRequiredTestClass();
                    String method = context.getTestMethod().map(java.lang.reflect.Method::getName).orElse("Class lifecycle");
                    var identity = new AssertionAuditReport.Identity(type.getSimpleName(), type.getPackageName(),
                            context.getDisplayName(), method, List.of("component", "android-library", "offline"));
                    var inputs = new ComponentInputs();
                    context.getStore(INPUTS).put(ComponentInputs.class, inputs);
                    return new AndroidComponentPublication(run, identity, inputs);
                });
    public static AndroidComponentAudit extension() { return new AndroidComponentAudit(); }
    @Override public ComponentAuditExtension auditExtension() { return audit; }
    @Override public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
        return parameter.getParameter().getType() == ComponentInputs.class || audit.supportsParameter(parameter, context);
    }
    @Override public Object resolveParameter(ParameterContext parameter, ExtensionContext context) {
        return parameter.getParameter().getType() == ComponentInputs.class
                ? java.util.Objects.requireNonNull(context.getStore(INPUTS).get(ComponentInputs.class, ComponentInputs.class))
                : audit.resolveParameter(parameter, context);
    }
}
